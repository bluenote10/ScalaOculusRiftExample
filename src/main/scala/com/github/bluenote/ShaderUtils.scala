package com.github.bluenote


import org.lwjgl.opengl.GL20


object ShaderUtils {
  
  val FRAGMENT_SHADER = GL20.GL_FRAGMENT_SHADER
  val VERTEX_SHADER   = GL20.GL_VERTEX_SHADER
    
  /**
   * Compile a shader
   */
  def compileShader(source: String, typ: Int): Option[Int] = {
    val shaderId = GL20.glCreateShader(typ)
    assert(shaderId != 0)
    
    GL20.glShaderSource(shaderId, source)
    GL20.glCompileShader(shaderId)

    val status = GL20.glGetShaderi(shaderId, GL20.GL_COMPILE_STATUS)
    if (status == 0) {
      val logLen = GL20.glGetShaderi(shaderId, GL20.GL_INFO_LOG_LENGTH)
      val error = GL20.glGetShaderInfoLog(shaderId, logLen)
      println(f"error compiling ${if (typ==VERTEX_SHADER) "vertex" else "fragment"} shader:\n" + error)  // TODO logging
      return None
    }
    
    return Some(shaderId)
  }
   
  /**
   * Link shaders to a program
   */
  def linkProgram(vsId: Int, fsId: Int): Option[Int] = {
    
    if (vsId == 0 || fsId == 0) 
      return None
    
    val progId = GL20.glCreateProgram()
    assert(progId != 0)
    GL20.glAttachShader(progId, vsId)
    GL20.glAttachShader(progId, fsId)
    
    // link
    GL20.glLinkProgram(progId)
    val statusLink = GL20.glGetProgrami(progId, GL20.GL_LINK_STATUS)
    if (statusLink == 0) {
      val logLen = GL20.glGetProgrami(progId, GL20.GL_INFO_LOG_LENGTH)
      val error = GL20.glGetProgramInfoLog(progId, logLen)
      println("error linking program:\n" + error)
      return None
    }
    
    // validate
    GL20.glValidateProgram(progId)
    val statusValid = GL20.glGetProgrami(progId, GL20.GL_VALIDATE_STATUS)
    if (statusValid == 0) {
      val logLen = GL20.glGetProgrami(progId, GL20.GL_INFO_LOG_LENGTH)
      val error = GL20.glGetProgramInfoLog(progId, logLen)
      println("error valdidating program:\n" + error)
      return None
    }
    
    return Some(progId)
  }
}



/**
 * Wrapper class for a shader program
 * Recompiles a shader automatically (on "use()") if the shader files have been modified.
 */
class ShaderProgram(vsFile: String, fsFile: String, automaticReloading: Boolean = true) {

  case class ProgramStatus(
    porperlyLoaded: Boolean,
    programId: Int,
    vsId: Int,
    fsId: Int,
    unifsMap: Map[String, Int],
    attrsMap: Map[String, Int],
    fsOldSource: String,
    vsOldSource: String
  )
  

  def load(): ProgramStatus = {
    println(f"loading shaders ['$vsFile', '$vsFile']...")
    val vsSource = GeneralUtils.loadFile(vsFile)
    val fsSource = GeneralUtils.loadFile(fsFile)

    // try to compile
    val vsId = ShaderUtils.compileShader(vsSource, GL20.GL_VERTEX_SHADER)
    val fsId = ShaderUtils.compileShader(fsSource, GL20.GL_FRAGMENT_SHADER)
    if (vsId.isEmpty || fsId.isEmpty) 
      return ProgramStatus(false, 0, vsId.getOrElse(0), fsId.getOrElse(0), Map(), Map(), fsSource, vsSource)
    
    // try to link
    val programId = ShaderUtils.linkProgram(vsId.get, fsId.get)
    if (programId.isEmpty) 
      return ProgramStatus(false, 0, vsId.getOrElse(0), fsId.getOrElse(0), Map(), Map(), fsSource, vsSource)
    
    GlWrapper.checkGlError()      
      
    // extract uniforms / attributes
    val numUnifs = GL20.glGetProgrami(programId.get, GL20.GL_ACTIVE_UNIFORMS)
    val numAttrs = GL20.glGetProgrami(programId.get, GL20.GL_ACTIVE_ATTRIBUTES)
    
    val unifsMap = Range(0, numUnifs).map{ i =>
      val name = GL20.glGetActiveUniform(programId.get, i, 8192)
      val loca = GL20.glGetUniformLocation(programId.get, name)
      println(f"Found uniform $name%-30s   at location $loca%3d")
      name -> loca
    } toMap
    val attrsMap = Range(0, numAttrs).map{ i =>
      val name = GL20.glGetActiveAttrib(programId.get, i, 8192)
      val loca = GL20.glGetAttribLocation(programId.get, name)
      println(f"Found attribute $name%-30s at location $loca%3d")
      name -> loca
    } toMap
    
    return ProgramStatus(true, programId.get, vsId.get, fsId.get, unifsMap, attrsMap, fsSource, vsSource)
  }
  
  
  var status = load()
  val changeNotification = new java.util.concurrent.atomic.AtomicBoolean(false) 
  
  
  // run a background thread to check for shader modification
  if (automaticReloading) {
    new Thread(new Runnable() {
      def run() {
        while (true) { 
          Thread.sleep(1000)
          val vsSource = GeneralUtils.loadFile(vsFile)
          val fsSource = GeneralUtils.loadFile(fsFile)
          if (vsSource != status.vsOldSource || fsSource != status.fsOldSource) {
            changeNotification.set(true)
            println("shader change detected")
          }
        }
      }
    }).start()
  }
  
  /**
   * Activates the shader
   */
  def use() {
    if (automaticReloading && changeNotification.compareAndSet(true, false)) {
      status = load()
    }
    if (status.porperlyLoaded) {
      GL20.glUseProgram(status.programId)
    }
  }

  /**
   * Closes the shader
   */
  def close() {
    if (0 != status.programId) {
      GL20.glDeleteProgram(status.programId)
    }
    if (0 != status.vsId) {
      GL20.glDeleteShader(status.vsId)
    }
    if (0 != status.fsId) {
      GL20.glDeleteShader(status.fsId)
    }
    status = status.copy(programId = 0, fsId = 0, vsId = 0)
  }
  
  
  def getUniformLocation  (name: String) = status.unifsMap(name) // .getOrElse(name, -1)
  def getAttributeLocation(name: String) = status.attrsMap(name) // .getOrElse(name, -1)


  /**
   * Fast uniform setters by locations (external name lookup)
   */
  def setUniform(loc: Int, mat: Mat4f) {
    GL20.glUniformMatrix4(loc, false, mat.asFloatBuffer)  // 2. arg is transpose
  }
  def setUniform(loc: Int, mat: Mat3f) {
    GL20.glUniformMatrix3(loc, false, mat.asFloatBuffer)  // 2. arg is transpose
  }  
  def setUniform(loc: Int, a: Float, b: Float, c: Float, d: Float) {
    GL20.glUniform4f(loc, a, b, c, d)
  }
  def setUniform(loc: Int, a: Float, b: Float) {
    GL20.glUniform2f(loc, a, b)
  }
  def setUniform(loc: Int, a: Float) {
    GL20.glUniform1f(loc, a)
  }
  def setUniform(loc: Int, v: Vec3f) {
    GL20.glUniform3f(loc, v.x, v.y, v.z)
  }  
  def setUniform(loc: Int, vec: Array[Float]) {
    vec.length match {
      case 2 => setUniform(loc, vec(0), vec(1))
      case 4 => setUniform(loc, vec(0), vec(1), vec(2), vec(3))
    }
  }
  
  /**
   * Convenient uniform setters by names
   */
  def setUniform(name: String, mat: Mat4f) {
    GL20.glUniformMatrix4(getUniformLocation(name), false, mat.asFloatBuffer)
  }
  def setUniform(name: String, mat: Mat3f) {
    GL20.glUniformMatrix3(getUniformLocation(name), false, mat.asFloatBuffer)
  }
  def setUniform(name: String, a: Float, b: Float, c: Float, d: Float) {
    GL20.glUniform4f(getUniformLocation(name), a, b, c, d)
  }
  def setUniform(name: String, a: Float, b: Float) {
    GL20.glUniform2f(getUniformLocation(name), a, b)
  }
  def setUniform(name: String, a: Float) {
    GL20.glUniform1f(getUniformLocation(name), a)
  }
  def setUniform(name: String, v: Vec3f) {
    GL20.glUniform3f(getUniformLocation(name), v.x, v.y, v.z)
  }  
  def setUniform(name: String, vec: Array[Float]) {
    vec.length match {
      case 2 => setUniform(name, vec(0), vec(1))
      case 4 => setUniform(name, vec(0), vec(1), vec(2), vec(3))
    }
  }
  


  
}

object ShaderProgram {
  
  def apply(base: String) = new ShaderProgram(base + ".vs", base + ".fs")
  def apply(vsFile: String, fsFile: String) = new ShaderProgram(vsFile, fsFile)
  
}










