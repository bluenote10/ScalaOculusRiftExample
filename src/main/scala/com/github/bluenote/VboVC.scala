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
trait ProgVC {
  def attrLocPos: Int
  def attrLocColor: Int
  def use()
  def setProjection(P: Mat4f, callUseProgram: Boolean = true)
  def setModelview(V: Mat4f, callUseProgram: Boolean = true)
}


/**
 * Concreate shader program for disabled lighting
 */
class ProgNoLightingVC extends ProgVC {
  val prog = ShaderProgram("data/shaders/NoLighting")

  val attrLocPos    = prog.getAttributeLocation("position")
  val attrLocColor  = prog.getAttributeLocation("color")
  
  val unifLocP      = prog.getUniformLocation("cameraToClipMatrix")
  val unifLocV      = prog.getUniformLocation("modelToCameraMatrix")
  
  def use() = prog.use()
  
  def setProjection(P: Mat4f, callUseProgram: Boolean = true) {
    if (callUseProgram) prog.use() // must be active to set uniforms!!! otherwise GL error 1282...
    prog.setUniform(unifLocP, P)
  }
  
  def setModelview(V: Mat4f, callUseProgram: Boolean = true) {
    if (callUseProgram) prog.use() // must be active to set uniforms!!! otherwise GL error 1282...
    prog.setUniform(unifLocV, V)
  }
}



class DynamicVboVC(prog: ProgVC) {
  
  val vbId = GL15.glGenBuffers()

  /**
   * Assumption: program is set!
   */
  def render(vertexData: Array[Float], primitiveType: Int = GL11.GL_TRIANGLES) {
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbId)
    GL15.glBufferData(GL15.GL_ARRAY_BUFFER, ScalaBufferUtils.convertToFloatBuffer(vertexData), GL15.GL_DYNAMIC_DRAW)
    
    val floatsPerVertex = 3 + 4
    val numVertices = vertexData.length / floatsPerVertex
    val strideInBytes = floatsPerVertex * 4

    // bind
    GL20.glEnableVertexAttribArray(prog.attrLocPos)
    GL20.glEnableVertexAttribArray(prog.attrLocColor)
    GL20.glVertexAttribPointer(prog.attrLocPos,    3, GL_FLOAT, false, strideInBytes, 0)
    GL20.glVertexAttribPointer(prog.attrLocColor,  4, GL_FLOAT, false, strideInBytes, 12)
    
    // draw
    GL11.glDrawArrays(primitiveType, 0, numVertices)  
    // make primitive type a parameter in VCProg? 
    // no! it should be part of the data.
    // since you may want to use a given prog for triangles, strips, fans, ...
    
    // unbind
    GL20.glDisableVertexAttribArray(prog.attrLocPos)
    GL20.glDisableVertexAttribArray(prog.attrLocColor)
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)
    GlWrapper.checkGlError()
  }
  
}


object VertexDataGenVC {
  
  def Frame(x1: Float, x2: Float, y1: Float, y2: Float, color: Color, z: Float = 0): Array[Float] = {
    val p1 = Vec3f(x1 min x2, y1 min y2, z)
    val p2 = Vec3f(x1 max x2, y1 min y2, z)
    val p3 = Vec3f(x1 min x2, y1 max y2, z)
    val p4 = Vec3f(x1 max x2, y1 max y2, z)
    val carr = color.toArr
    val triangles =
      p1.arr ++ carr    ++    p2.arr ++ carr    ++    p4.arr ++ carr ++
      p4.arr ++ carr    ++    p3.arr ++ carr    ++    p1.arr ++ carr
    return triangles
    
  }
  
  def Circle(x: Float, y: Float, r: Float, wPercent: Float, color: Color, z: Float = 0, slices: Int = 16): Array[Float] = {

    val carr = color.toArr
    val circularIndices = Range(0, slices).toArray :+ 0                                             // eg 0,1,2,3,0
    val circularSlidingTuples = circularIndices.sliding(2).map{ case Array(i,j) => (i,j)}.toArray   // eg (0,1),(1,2),(2,3),(3,0)

    // precalculate sin/cos values for all indices
    val sinValues = circularIndices.map(i => math.sin(2*math.Pi * i / slices).toFloat)
    val cosValues = circularIndices.map(i => math.cos(2*math.Pi * i / slices).toFloat)

    val triangles = circularSlidingTuples.flatMap{ case (i,j) =>
      val p1 = Vec3f(cosValues(i)*(r*(1f-wPercent)) + x, sinValues(i)*(r*(1f-wPercent)) + y, z)
      val p2 = Vec3f(cosValues(i)*(r*(1f+wPercent)) + x, sinValues(i)*(r*(1f+wPercent)) + y, z)
      val p3 = Vec3f(cosValues(j)*(r*(1f+wPercent)) + x, sinValues(j)*(r*(1f+wPercent)) + y, z)
      val p4 = Vec3f(cosValues(j)*(r*(1f-wPercent)) + x, sinValues(j)*(r*(1f-wPercent)) + y, z)
      val triangles =
        p1.arr ++ carr    ++    p2.arr ++ carr    ++    p3.arr ++ carr ++
        p3.arr ++ carr    ++    p4.arr ++ carr    ++    p1.arr ++ carr
      triangles
    }
    triangles
    
    
  }
  
}



*/


