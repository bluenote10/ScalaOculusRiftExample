package com.github.bluenote


sealed trait VertexAttributes
trait HasVrtxAttrPos3D  extends VertexAttributes { val locPos3D: Int }
trait HasVrtxAttrNormal extends VertexAttributes { val locNormal: Int }
trait HasVrtxAttrColor  extends VertexAttributes { val locColor: Int }


trait Shader {
  
  val prog: ShaderProgram
  
  def use() = prog.use()
  
  val vertexAttributes: VertexAttributes

  def setProjection(P: Mat4f)
  def setModelview(V: Mat4f, V_inv: Option[Mat4f] = None)
  
}


/**
 * Concrete shader program for Gaussian lighting
 */
class DefaultLightingShader extends Shader {
  
  val prog = ShaderProgram("data/shaders/GaussianLighting")

  val vertexAttributes = new VertexAttributes with HasVrtxAttrPos3D with HasVrtxAttrNormal with HasVrtxAttrColor {
    val locPos3D  = prog.getAttributeLocation("position")
    val locNormal = prog.getAttributeLocation("normal")
    val locColor  = prog.getAttributeLocation("inDiffuseColor")
  }
  
  val unifLocCameraToClipMatrix        = prog.getUniformLocation("cameraToClipMatrix")
  val unifLocModelToCameraMatrix       = prog.getUniformLocation("modelToCameraMatrix")
  val unifLocNormalModelToCameraMatrix = prog.getUniformLocation("normalModelToCameraMatrix")
  val unifLocCameraSpaceLightPos1      = prog.getUniformLocation("cameraSpaceLightPos")
  
  def setProjection(P: Mat4f) {
    prog.use() // must be active to set uniforms! otherwise GL error 1282...
    prog.setUniform(unifLocCameraToClipMatrix, P)
  }
  
  def setModelview(V: Mat4f, V_inv: Option[Mat4f]) {
    prog.use() // must be active to set uniforms! otherwise GL error 1282...
    prog.setUniform(unifLocModelToCameraMatrix, V)
    prog.setUniform(unifLocNormalModelToCameraMatrix, V_inv.map(Mat3f.createFromMat4f(_)) getOrElse Mat3f.createFromMat4f(V).inverse().transpose())
  }
  
}



