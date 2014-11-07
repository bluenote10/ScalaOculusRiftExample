package com.github.bluenote

import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30

/*
/**
 * Abstraction of a shader program that uses VC data
 */
trait ProgPT {
  def attrLocPos: Int
  def attrLocTCoord: Int
  def use()
  def setProjection(P: Mat4f, callUseProgram: Boolean = true)
  def setModelview(V: Mat4f, callUseProgram: Boolean = true)
  def setAlpha(alpha: Float, callUseProgram: Boolean = true)
}


/**
 * Concreate shader program for disabled lighting
 */
class ProgNoLightingTexturedPT extends ProgPT {
  val prog = ShaderProgram("data/shaders/NoLightingTextured")

  val attrLocPos     = prog.getAttributeLocation("position")
  val attrLocTCoord  = prog.getAttributeLocation("textureCoord")
  
  val unifLocP       = prog.getUniformLocation("cameraToClipMatrix")
  val unifLocV       = prog.getUniformLocation("modelToCameraMatrix")
  val unifAlpha      = prog.getUniformLocation("alpha")
  val unifLocSampler = prog.getUniformLocation("sampler")
  
  def use() = prog.use()
  
  def setProjection(P: Mat4f, callUseProgram: Boolean = true) {
    if (callUseProgram) prog.use() // must be active to set uniforms!!! otherwise GL error 1282...
    prog.setUniform(unifLocP, P)
  }
  
  def setModelview(V: Mat4f, callUseProgram: Boolean = true) {
    if (callUseProgram) prog.use() // must be active to set uniforms!!! otherwise GL error 1282...
    prog.setUniform(unifLocV, V)
  }
  
  def setAlpha(alpha: Float, callUseProgram: Boolean = true) {
    if (callUseProgram) prog.use() // must be active to set uniforms!!! otherwise GL error 1282...
    prog.setUniform(unifAlpha, alpha)
  }
}



class DynamicVboPT(prog: ProgPT) {
  
  val vbId = GL15.glGenBuffers()

  /**
   * Assumption: program is set!
   */
  def render(vertexData: Array[Float], primitiveType: Int = GL11.GL_TRIANGLES) {
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbId)
    GL15.glBufferData(GL15.GL_ARRAY_BUFFER, ScalaBufferUtils.convertToFloatBuffer(vertexData), GL15.GL_DYNAMIC_DRAW)
    
    val floatsPerVertex = 3 + 2
    val numVertices = vertexData.length / floatsPerVertex
    val strideInBytes = floatsPerVertex * 4

    // bind
    GL20.glEnableVertexAttribArray(prog.attrLocPos)
    GL20.glEnableVertexAttribArray(prog.attrLocTCoord)
    GL20.glVertexAttribPointer(prog.attrLocPos,    3, GL_FLOAT, false, strideInBytes, 0)
    GL20.glVertexAttribPointer(prog.attrLocTCoord, 2, GL_FLOAT, false, strideInBytes, 12)
    
    // draw
    GL11.glDrawArrays(primitiveType, 0, numVertices)  
    // make primitive type a parameter in VCProg? 
    // no! it should be part of the data.
    // since you may want to use a given prog for triangles, strips, fans, ...
    
    // unbind
    GL20.glDisableVertexAttribArray(prog.attrLocPos)
    GL20.glDisableVertexAttribArray(prog.attrLocTCoord)
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)
    GlWrapper.checkGlError()
  }
  
}


object VertexDataGenPT {
  
  def Frame(x1: Float, x2: Float, y1: Float, y2: Float, z: Float = 0): Array[Float] = {
    /**
     * My intuitive implementation used these texture coordinates:
     * 
    val p1 = Vec3f(x1 min x2, y1 min y2, z).arr ++ Array(0f, 0f)
    val p2 = Vec3f(x1 max x2, y1 min y2, z).arr ++ Array(1f, 0f)
    val p3 = Vec3f(x1 min x2, y1 max y2, z).arr ++ Array(0f, 1f)
    val p4 = Vec3f(x1 max x2, y1 max y2, z).arr ++ Array(1f, 1f)
     * 
     * But it looks like OpenGL internally assume an upside-down texture convention, there we exchange all 0 <-> 1 for y coordinates
     * Alternative would be to swap in the shader or in creation level?
     */
    val p1 = Vec3f(x1 min x2, y1 min y2, z).arr ++ Array(0f, 1f)
    val p2 = Vec3f(x1 max x2, y1 min y2, z).arr ++ Array(1f, 1f)
    val p3 = Vec3f(x1 min x2, y1 max y2, z).arr ++ Array(0f, 0f)
    val p4 = Vec3f(x1 max x2, y1 max y2, z).arr ++ Array(1f, 0f)
    val triangles =
      p1 ++ p2 ++ p4 ++
      p4 ++ p3 ++ p1
    return triangles
    
  }
  
  
}






*/