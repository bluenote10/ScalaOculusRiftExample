package com.github.bluenote

import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30


trait Light{
  def posWorld: Vec3f
  def posCamera: Vec3f
}

case class MutableLight(var posWorld: Vec3f, var posCamera: Vec3f) extends Light {
  def updateCameraPos(worldToCamera: Mat4f) {
    posCamera = (worldToCamera * posWorld.toVec4f).toVec3f
  }
}



/**
 * Abstraction of a shader program that uses VNC data
 */
trait VNCProg {
  def attrLocPos: Int
  def attrLocNormal: Int
  def attrLocColor: Int
  def use()
  def setProjection(P: Mat4f, callUseProgram: Boolean = true)
  def setModelview(V: Mat4f, callUseProgram: Boolean = true)
  def setLights(lights: List[Light])
}


/**
 * Concrete shader program for Gaussian lighting
 */
class VNCProgGaussianLighting extends VNCProg {
  val prog = ShaderProgram("data/shaders/GaussianLighting")

  val attrLocPos    = prog.getAttributeLocation("position")
  val attrLocNormal = prog.getAttributeLocation("normal")
  val attrLocColor  = prog.getAttributeLocation("inDiffuseColor")
  
  val unifLocCameraToClipMatrix        = prog.getUniformLocation("cameraToClipMatrix")
  val unifLocModelToCameraMatrix       = prog.getUniformLocation("modelToCameraMatrix")
  val unifLocNormalModelToCameraMatrix = prog.getUniformLocation("normalModelToCameraMatrix")
  val unifLocCameraSpaceLightPos1      = prog.getUniformLocation("cameraSpaceLightPos")
  
  def use() = prog.use()
  
  def setProjection(P: Mat4f, callUseProgram: Boolean = true) {
    if (callUseProgram) prog.use() // must be active to set uniforms!!! otherwise GL error 1282...
    prog.setUniform(unifLocCameraToClipMatrix, P)
  }
  
  def setModelview(V: Mat4f, callUseProgram: Boolean = true) {
    if (callUseProgram) prog.use() // must be active to set uniforms!!! otherwise GL error 1282...
    prog.setUniform(unifLocModelToCameraMatrix, V)
    prog.setUniform(unifLocNormalModelToCameraMatrix, Mat3f.createFromMat4f(V).inverse().transpose())
  }
  
  def setLights(lights: List[Light]) {
    lights match {
      case l1 :: Nil => prog.setUniform(unifLocCameraSpaceLightPos1, l1.posCamera)
      case _ => 
    }
  }
}




trait VertexDescription {
  
}


trait VertexData {
  
  type VertexDesc <: VertexDescription
  
  val rawData: Array[Float]
  val primitiveType: Int
  val floatsPerVertex: Int
  
  // convenience functions derived from floatsPerVertex
  val numVertices = rawData.length / floatsPerVertex
  val strideInBytes = floatsPerVertex * 4
}

class VertexData3D_NC extends VertexData {
  
}


/**
 * A simple VBO wrapper that can take an array of vertices 
 * with the following per-vertex-information:
 * 
 * position (3 Floats) + normal (3 Floats) + color (4 Floats) 
 */
class SimpleVbo(arrayPNC: Array[Float], primitiveType: Int = GL11.GL_TRIANGLES) {
  
  val prog = ShaderProgram("shaders/GaussianLighting")
  
  val attribPos = prog.getAttributeLocation("position")
  val attribNrm = prog.getAttributeLocation("normal")
  val attribCol = prog.getAttributeLocation("inDiffuseColor")
  
  val floatsPerVertex = 3 + 3 + 4
  val numVertices = arrayPNC.length / floatsPerVertex
  val strideInBytes = floatsPerVertex * 4

  // initialize a VAO and bind it
  val vao = GL30.glGenVertexArrays()
  GL30.glBindVertexArray(vao)

  // initialize the VBO and bind it
  val vbId = GL15.glGenBuffers()
  GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbId)

  // make the attribute bindings
  // that is where the association between the VAO and the current GL_ARRAY_BUFFER is evaluated and stored
  GL20.glEnableVertexAttribArray(attribPos)
  GL20.glEnableVertexAttribArray(attribNrm)
  GL20.glEnableVertexAttribArray(attribCol)
  GlWrapper.checkGlError("after enabling vertex attrib array")
  GL20.glVertexAttribPointer(attribPos, 3, GL_FLOAT, false, strideInBytes, 0)
  GL20.glVertexAttribPointer(attribNrm, 3, GL_FLOAT, false, strideInBytes, 12)
  GL20.glVertexAttribPointer(attribCol, 4, GL_FLOAT, false, strideInBytes, 24)
  GlWrapper.checkGlError("after setting the attrib pointers")
  
  // buffer the static vertex data
  GL15.glBufferData(GL15.GL_ARRAY_BUFFER, ScalaBufferUtils.convertToFloatBuffer(arrayPNC), GL15.GL_STATIC_DRAW)
  GlWrapper.checkGlError("after buffering the data")
  
  // unbind everything
  GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)
  GL30.glBindVertexArray(0)
  GlWrapper.checkGlError("after clean up")
  
  
  def setMatrices(P: Mat4f, V: Mat4f) {
    prog.use() // must be active to set uniforms!!! otherwise GL error 1282...
    prog.setUniform("cameraToClipMatrix", P)
    prog.setUniform("modelToCameraMatrix", V)
    prog.setUniform("normalModelToCameraMatrix", Mat3f.createFromMat4f(V).inverse().transpose())
    GlWrapper.checkGlError()
  }
  
  def render() {
    // bind
    //GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbId)
    GL30.glBindVertexArray(vao)
    GlWrapper.checkGlError("after binding VAO")
    
    prog.use()
    GlWrapper.checkGlError("after switching to shader")
    //prog.switchBackToFixedPipeline

    // draw
    GL11.glDrawArrays(primitiveType, 0, numVertices)
    GlWrapper.checkGlError("after drawing")
    
    // unbind
    GL30.glBindVertexArray(0)
    GlWrapper.checkGlError("finished rendering")
  }
  
}



/**
 * Collection vertex data generators for a few basic shapes
 * 
 * The returned vertex arrays have the following per-vertex-information:
 * 
 * position (3 Floats) + normal (3 Floats) + color (4 Floats) 
 */
object VertexDataGenVNC {
  
  /**
   * For convenience: Implicit conversion for transforming
   */
  implicit class TransformableArray(vertexData: Array[Float]) {

    def transformVNC(modMatrixPos: Mat4f, modMatrixNrm: Mat3f): Array[Float] = 
      VertexDataGenVNC.transform(vertexData, modMatrixPos, modMatrixNrm)
    
    def transformVNC(modMatrixPos: Mat4f): Array[Float] = 
      VertexDataGenVNC.transform(vertexData, modMatrixPos)
    
    def transfromSimpleVNC(modMatrixPos: Mat4f): Array[Float] = 
      VertexDataGenVNC.transformSimple(vertexData, modMatrixPos)
  }
  
  /**
   * Generic modification of an existing VNC data according to a transformation matrix
   * 
   * In general transforming VNC data requires to transform both position and normals.
   * In case of non-uniform scaling the transformation of positions and normals is not
   * the same, therefore the most general form requires two transformation matrices, 
   * which is obviously annoying. See below for alternatives.
   */
  def transform(vertexData: Array[Float], modMatrixPos: Mat4f, modMatrixNrm: Mat3f): Array[Float] = {
    val numEntriesPerVertex = 10
    assert(vertexData.length % numEntriesPerVertex == 0)

    val newVertexData = vertexData.clone()
    
    val numVertices = vertexData.length / numEntriesPerVertex
    for (ii <- Range(0, numVertices)) {
      val i = ii*numEntriesPerVertex
      val pos = new Vec4f(vertexData(i)  , vertexData(i+1), vertexData(i+2), 1f)
      val nrm = new Vec3f(vertexData(i+3), vertexData(i+4), vertexData(i+5))
      
      val newPos = modMatrixPos * pos
      val newNrm = modMatrixNrm * nrm
      
      newVertexData(i  ) = newPos.x
      newVertexData(i+1) = newPos.y
      newVertexData(i+2) = newPos.z
      newVertexData(i+3) = newNrm.x
      newVertexData(i+4) = newNrm.y
      newVertexData(i+5) = newNrm.z
    }
    
    return newVertexData
  }
  
  /**
   * This provides a simplified interface of the above transformation.
   * The normal transformation matrix is calculated internally by taking the inverse transpose.
   */
  def transform(vertexData: Array[Float], modMatrixPos: Mat4f): Array[Float] = {
    val modMatrixNrm = Mat3f.createFromMat4f(modMatrixPos).inverse().transpose()
    transform(vertexData, modMatrixPos, modMatrixNrm)
  }

  /**
   * In case our transformations have uniform scale the above is overkill;
   * we can simply use the position transformation matrix for normals as well
   */
  def transformSimple(vertexData: Array[Float], modMatrixPos: Mat4f): Array[Float] = {
    transform(vertexData, modMatrixPos, Mat3f.createFromMat4f(modMatrixPos))
  }
  
  
  // some shorthand to simplify notation
  
  type V = Vec3f
  
  case class C(r: Float, g: Float, b: Float, a: Float) {
    def arr = Array(r, g, b, a)
  }
  
  
  /**
   * Generic cube
   */  
  def cubeVNC(x1: Float, x2: Float, y1: Float, y2: Float, z1: Float, z2: Float, color: Color): Array[Float] = {
    val p1 = Vec3f(x1 min x2, y1 min y2, z1 max z2)
    val p2 = Vec3f(x1 max x2, y1 min y2, z1 max z2)
    val p3 = Vec3f(x1 min x2, y1 max y2, z1 max z2)
    val p4 = Vec3f(x1 max x2, y1 max y2, z1 max z2)
    val p5 = Vec3f(x1 min x2, y1 min y2, z1 min z2)
    val p6 = Vec3f(x1 max x2, y1 min y2, z1 min z2)
    val p7 = Vec3f(x1 min x2, y1 max y2, z1 min z2)
    val p8 = Vec3f(x1 max x2, y1 max y2, z1 min z2)
    val carr = color.toArr
    val triangles =
      // front face
      p1.arr ++ Vec3f(0,0,+1).arr ++ carr    ++    p2.arr ++ Vec3f(0,0,+1).arr ++ carr    ++    p4.arr ++ Vec3f(0,0,+1).arr ++ carr ++
      p4.arr ++ Vec3f(0,0,+1).arr ++ carr    ++    p3.arr ++ Vec3f(0,0,+1).arr ++ carr    ++    p1.arr ++ Vec3f(0,0,+1).arr ++ carr ++
      // back face
      p5.arr ++ Vec3f(0,0,-1).arr ++ carr    ++    p7.arr ++ Vec3f(0,0,-1).arr ++ carr    ++    p8.arr ++ Vec3f(0,0,-1).arr ++ carr ++
      p8.arr ++ Vec3f(0,0,-1).arr ++ carr    ++    p6.arr ++ Vec3f(0,0,-1).arr ++ carr    ++    p5.arr ++ Vec3f(0,0,-1).arr ++ carr ++
      // right face
      p2.arr ++ Vec3f(+1,0,0).arr ++ carr    ++    p6.arr ++ Vec3f(+1,0,0).arr ++ carr    ++    p8.arr ++ Vec3f(+1,0,0).arr ++ carr ++
      p8.arr ++ Vec3f(+1,0,0).arr ++ carr    ++    p4.arr ++ Vec3f(+1,0,0).arr ++ carr    ++    p2.arr ++ Vec3f(+1,0,0).arr ++ carr ++
      // left face
      p1.arr ++ Vec3f(-1,0,0).arr ++ carr    ++    p3.arr ++ Vec3f(-1,0,0).arr ++ carr    ++    p7.arr ++ Vec3f(-1,0,0).arr ++ carr ++
      p7.arr ++ Vec3f(-1,0,0).arr ++ carr    ++    p5.arr ++ Vec3f(-1,0,0).arr ++ carr    ++    p1.arr ++ Vec3f(-1,0,0).arr ++ carr ++
      // top face
      p3.arr ++ Vec3f(0,+1,0).arr ++ carr    ++    p4.arr ++ Vec3f(0,+1,0).arr ++ carr    ++    p8.arr ++ Vec3f(0,+1,0).arr ++ carr ++
      p8.arr ++ Vec3f(0,+1,0).arr ++ carr    ++    p7.arr ++ Vec3f(0,+1,0).arr ++ carr    ++    p3.arr ++ Vec3f(0,+1,0).arr ++ carr ++
      // bottom face
      p1.arr ++ Vec3f(0,-1,0).arr ++ carr    ++    p5.arr ++ Vec3f(0,-1,0).arr ++ carr    ++    p6.arr ++ Vec3f(0,-1,0).arr ++ carr ++
      p6.arr ++ Vec3f(0,-1,0).arr ++ carr    ++    p2.arr ++ Vec3f(0,-1,0).arr ++ carr    ++    p1.arr ++ Vec3f(0,-1,0).arr ++ carr
    return triangles
  }
  
  /**
   * Generic cylinder
   * Convention:
   * x/z   corresponds to     rotation plane,
   * y     corresponds to     cylinder axis (with top a +h and bottom at -h)
   * 
   */
  def cylinderVNC(r: Float, h: Float, color: Color, slices: Int = 4, wallOnly: Boolean = false): Array[Float] = {
    val carr = color.toArr
    
    val circularIndices = Range(0, slices).toArray :+ 0                                             // eg 0,1,2,3,0
    val circularSlidingTuples = circularIndices.sliding(2).map{ case Array(i,j) => (i,j)}.toArray   // eg (0,1),(1,2),(2,3),(3,0)

    // precalculate sin/cos values for all indices
    val sinValues = circularIndices.map(i => r*math.sin(2*math.Pi * i / slices).toFloat)
    val cosValues = circularIndices.map(i => r*math.cos(2*math.Pi * i / slices).toFloat)

    // generate wall:
    val wallTriangles = circularSlidingTuples.flatMap{ case (i,j) =>
      val p1 = Vec3f(sinValues(i), -h, cosValues(i))
      val p2 = Vec3f(sinValues(j), -h, cosValues(j))
      val p3 = Vec3f(sinValues(j), +h, cosValues(j))
      val p4 = Vec3f(sinValues(i), +h, cosValues(i))
      val normalI = Vec3f(sinValues(i)/r, 0, cosValues(i)/r)
      val normalJ = Vec3f(sinValues(j)/r, 0, cosValues(j)/r)
      p1.arr ++ normalI.arr ++ carr    ++    p2.arr ++ normalJ.arr ++ carr    ++    p3.arr ++ normalJ.arr ++ carr ++
      p3.arr ++ normalJ.arr ++ carr    ++    p4.arr ++ normalI.arr ++ carr    ++    p1.arr ++ normalI.arr ++ carr
    }
    if (wallOnly) {
      return wallTriangles
    }
    
    // generate planes:
    val planes = for ((y,n) <- List((-h, Vec3f(0,-1,0)), (+h, Vec3f(0,+1,0)))) yield {
      val pc = Vec3f(0, y, 0)
      val triangles = circularSlidingTuples.flatMap{ case (i,j) =>
        val (ii, jj) = if (y > 0) (i,j) else (j,i) // change order depending on side
        val p1 = Vec3f(sinValues(ii), y, cosValues(ii))
        val p2 = Vec3f(sinValues(jj), y, cosValues(jj))
        p1.arr ++ n.arr ++ carr    ++    p2.arr ++ n.arr ++ carr    ++    pc.arr ++ n.arr ++ carr
      }
      triangles
    }

    wallTriangles ++ planes(0) ++ planes(1)
  }
  def cylinderTwoColorsVNC(r: Float, h: Float, colorBottom: Color, colorTop: Color, slices: Int = 4, wallOnly: Boolean = false): Array[Float] = {
    val carrB = colorBottom.toArr
    val carrT = colorTop.toArr
    
    val circularIndices = Range(0, slices).toArray :+ 0                                             // eg 0,1,2,3,0
    val circularSlidingTuples = circularIndices.sliding(2).map{ case Array(i,j) => (i,j)}.toArray   // eg (0,1),(1,2),(2,3),(3,0)

    // precalculate sin/cos values for all indices
    val sinValues = circularIndices.map(i => r*math.sin(2*math.Pi * i / slices).toFloat)
    val cosValues = circularIndices.map(i => r*math.cos(2*math.Pi * i / slices).toFloat)

    // generate wall:
    val wallTriangles = circularSlidingTuples.flatMap{ case (i,j) =>
      val p1 = Vec3f(sinValues(i), -h, cosValues(i))
      val p2 = Vec3f(sinValues(j), -h, cosValues(j))
      val p3 = Vec3f(sinValues(j), +h, cosValues(j))
      val p4 = Vec3f(sinValues(i), +h, cosValues(i))
      val normalI = Vec3f(sinValues(i)/r, 0, cosValues(i)/r)
      val normalJ = Vec3f(sinValues(j)/r, 0, cosValues(j)/r)
      p1.arr ++ normalI.arr ++ carrB    ++    p2.arr ++ normalJ.arr ++ carrB    ++    p3.arr ++ normalJ.arr ++ carrT ++
      p3.arr ++ normalJ.arr ++ carrT    ++    p4.arr ++ normalI.arr ++ carrT    ++    p1.arr ++ normalI.arr ++ carrB
    }
    if (wallOnly) {
      return wallTriangles
    }
    
    // generate planes:
    val planes = for ((y,n, carr) <- List((-h, Vec3f(0,-1,0), carrB), (+h, Vec3f(0,+1,0), carrT))) yield {
      val pc = Vec3f(0, y, 0)
      val triangles = circularSlidingTuples.flatMap{ case (i,j) =>
        val (ii, jj) = if (y > 0) (i,j) else (j,i) // change order depending on side
        val p1 = Vec3f(sinValues(ii), y, cosValues(ii))
        val p2 = Vec3f(sinValues(jj), y, cosValues(jj))
        p1.arr ++ n.arr ++ carr    ++    p2.arr ++ n.arr ++ carr    ++    pc.arr ++ n.arr ++ carr
      }
      triangles
    }

    wallTriangles ++ planes(0) ++ planes(1)
  }
  
  /**
   * A "line" is a thin cylinder connecting two arbitrary points in space
   */
  def lineVNC(r: Float, p1: Vec3f, p2: Vec3f, color1: Color, color2: Color, slices: Int = 4, wallOnly: Boolean = false): Array[Float] = {
    
    val p1_to_p2 = p2 - p1
    val p1_to_p2_norm = p1_to_p2.normalize
    
    val mid = p1 mid p2
    val halfLength = p1_to_p2.length / 2
    
    val cylinder = cylinderTwoColorsVNC(r, halfLength, color1, color2, slices, wallOnly)
    
    val cylNorm = Vec3f(0, 1, 0)
    val rotAxis = p1_to_p2_norm cross cylNorm
    val rotAngl = math.acos(p1_to_p2_norm   *   cylNorm).toFloat
    
    //println(rotAngl, rotAngl*180/math.Pi.toFloat, rotAxis)
    
    cylinder.transformVNC(Mat4f.translate(mid.x, mid.y, mid.z).rotate(-rotAngl*180/math.Pi.toFloat, rotAxis.x, rotAxis.y, rotAxis.z))
  }
  
  /**
   * Generic disk
   * Convention: centered at y=0, with normal in +y direction
   */
  def discVNC(r: Float, color: Color, slices: Int = 16): Array[Float] = {
    val carr = color.toArr
    
    val circularIndices = Range(0, slices).toArray :+ 0                                             // eg 0,1,2,3,0
    val circularSlidingTuples = circularIndices.sliding(2).map{ case Array(i,j) => (i,j)}.toArray   // eg (0,1),(1,2),(2,3),(3,0)

    // precalculate sin/cos values for all indices
    val sinValues = circularIndices.map(i => r*math.sin(2*math.Pi * i / slices).toFloat)
    val cosValues = circularIndices.map(i => r*math.cos(2*math.Pi * i / slices).toFloat)

    // generate planes:
    val disc = {
      val pc = Vec3f(0,0,0)
      val n  = Vec3f(0,1,0)
      val triangles = circularSlidingTuples.flatMap{ case (i,j) =>
        val p1 = Vec3f(sinValues(i), 0, cosValues(i))
        val p2 = Vec3f(sinValues(j), 0, cosValues(j))
        p1.arr ++ n.arr ++ carr    ++    p2.arr ++ n.arr ++ carr    ++    pc.arr ++ n.arr ++ carr
      }
      triangles
    }
    disc
  }  
  
  /**
   * Generic sphere
   */
  def sphereVNC(r: Float, color: Color, numRecursions: Int = 4): Array[Float] = {
    val carr = color.toArr

    val p1 = Vec3f(0, -r, 0)
    val p2 = Vec3f(0, 0, +r)
    val p3 = Vec3f(+r, 0, 0)
    val p4 = Vec3f(0, 0, -r)
    val p5 = Vec3f(0, +r, 0)
    val p6 = Vec3f(-r, 0, 0)
    
    val triangles = 
      (p1, p3, p2) ::
      (p1, p4, p3) ::
      (p1, p6, p4) ::
      (p1, p2, p6) ::
      (p5, p2, p3) ::
      (p5, p3, p4) ::
      (p5, p4, p6) ::
      (p5, p6, p2) ::
      Nil

    def midPoint(p1: Vec3f, p2: Vec3f) = (p1 mid p2).setLengthTo(r)
      
    def recursiveRefinement(triangles: List[(Vec3f, Vec3f, Vec3f)], numRecursions: Int): List[(Vec3f, Vec3f, Vec3f)] = {
      if (numRecursions==0) {
        return triangles 
      } else {
        val refinedTriangles = triangles.flatMap{ case (p1, p2, p3) =>
          val p4 = midPoint(p1, p2)
          val p5 = midPoint(p2, p3)
          val p6 = midPoint(p3, p1)
          (p1, p4, p6) ::
          (p4, p2, p5) ::
          (p4, p5, p6) ::
          (p6, p5, p3) ::
          Nil          
        }
        return recursiveRefinement(refinedTriangles, numRecursions-1)
      }
    }
      
    val refinedTriangles = recursiveRefinement(triangles, numRecursions)
    
    def vecToNormal(p: Vec3f) = p / r

    refinedTriangles.toArray.flatMap{vertices => 
      vertices._1.arr ++ vecToNormal(vertices._1).arr ++ carr ++
      vertices._2.arr ++ vecToNormal(vertices._2).arr ++ carr ++
      vertices._3.arr ++ vecToNormal(vertices._3).arr ++ carr
    }
    
  }
  

  
  /**
   * Rounded cube 
   * Simplified over the generic cube in the sense that it is always centered at 0,
   * i.e., size is specified in "half width"
   */  
  def roundedCubeVNC(hwx: Float, hwy: Float, hwz: Float, r: Float, color: Color, detail: Int = 4): Array[Float] = {
    val p1 = Vec3f(-hwx, -hwy, +hwz)
    val p2 = Vec3f(+hwx, -hwy, +hwz)
    val p3 = Vec3f(-hwx, +hwy, +hwz)
    val p4 = Vec3f(+hwx, +hwy, +hwz)
    val p5 = Vec3f(-hwx, -hwy, -hwz)
    val p6 = Vec3f(+hwx, -hwy, -hwz)
    val p7 = Vec3f(-hwx, +hwy, -hwz)
    val p8 = Vec3f(+hwx, +hwy, -hwz)
    val carr = color.toArr
    val triangles =
      // front face
      (p1+Vec3f(+r,+r, 0)).arr ++ Vec3f(0,0,+1).arr ++ carr    ++    (p2+Vec3f(-r,+r, 0)).arr ++ Vec3f(0,0,+1).arr ++ carr    ++    (p4+Vec3f(-r,-r, 0)).arr ++ Vec3f(0,0,+1).arr ++ carr ++
      (p4+Vec3f(-r,-r, 0)).arr ++ Vec3f(0,0,+1).arr ++ carr    ++    (p3+Vec3f(+r,-r, 0)).arr ++ Vec3f(0,0,+1).arr ++ carr    ++    (p1+Vec3f(+r,+r, 0)).arr ++ Vec3f(0,0,+1).arr ++ carr ++
      // back face
      (p5+Vec3f(+r,+r, 0)).arr ++ Vec3f(0,0,-1).arr ++ carr    ++    (p7+Vec3f(+r,-r, 0)).arr ++ Vec3f(0,0,-1).arr ++ carr    ++    (p8+Vec3f(-r,-r, 0)).arr ++ Vec3f(0,0,-1).arr ++ carr ++
      (p8+Vec3f(-r,-r, 0)).arr ++ Vec3f(0,0,-1).arr ++ carr    ++    (p6+Vec3f(-r,+r, 0)).arr ++ Vec3f(0,0,-1).arr ++ carr    ++    (p5+Vec3f(+r,+r, 0)).arr ++ Vec3f(0,0,-1).arr ++ carr ++
      // right face
      (p2+Vec3f( 0,+r,-r)).arr ++ Vec3f(+1,0,0).arr ++ carr    ++    (p6+Vec3f( 0,+r,+r)).arr ++ Vec3f(+1,0,0).arr ++ carr    ++    (p8+Vec3f( 0,-r,+r)).arr ++ Vec3f(+1,0,0).arr ++ carr ++
      (p8+Vec3f( 0,-r,+r)).arr ++ Vec3f(+1,0,0).arr ++ carr    ++    (p4+Vec3f( 0,-r,-r)).arr ++ Vec3f(+1,0,0).arr ++ carr    ++    (p2+Vec3f( 0,+r,-r)).arr ++ Vec3f(+1,0,0).arr ++ carr ++
      // left face
      (p1+Vec3f( 0,+r,-r)).arr ++ Vec3f(-1,0,0).arr ++ carr    ++    (p3+Vec3f( 0,-r,-r)).arr ++ Vec3f(-1,0,0).arr ++ carr    ++    (p7+Vec3f( 0,-r,+r)).arr ++ Vec3f(-1,0,0).arr ++ carr ++
      (p7+Vec3f( 0,-r,+r)).arr ++ Vec3f(-1,0,0).arr ++ carr    ++    (p5+Vec3f( 0,+r,+r)).arr ++ Vec3f(-1,0,0).arr ++ carr    ++    (p1+Vec3f( 0,+r,-r)).arr ++ Vec3f(-1,0,0).arr ++ carr ++
      // top face
      (p3+Vec3f(+r, 0,-r)).arr ++ Vec3f(0,+1,0).arr ++ carr    ++    (p4+Vec3f(-r, 0,-r)).arr ++ Vec3f(0,+1,0).arr ++ carr    ++    (p8+Vec3f(-r, 0,+r)).arr ++ Vec3f(0,+1,0).arr ++ carr ++
      (p8+Vec3f(-r, 0,+r)).arr ++ Vec3f(0,+1,0).arr ++ carr    ++    (p7+Vec3f(+r, 0,+r)).arr ++ Vec3f(0,+1,0).arr ++ carr    ++    (p3+Vec3f(+r, 0,-r)).arr ++ Vec3f(0,+1,0).arr ++ carr ++
      // bottom face
      (p1+Vec3f(+r, 0,-r)).arr ++ Vec3f(0,-1,0).arr ++ carr    ++    (p5+Vec3f(+r, 0,+r)).arr ++ Vec3f(0,-1,0).arr ++ carr    ++    (p6+Vec3f(-r, 0,+r)).arr ++ Vec3f(0,-1,0).arr ++ carr ++
      (p6+Vec3f(-r, 0,+r)).arr ++ Vec3f(0,-1,0).arr ++ carr    ++    (p2+Vec3f(-r, 0,-r)).arr ++ Vec3f(0,-1,0).arr ++ carr    ++    (p1+Vec3f(+r, 0,-r)).arr ++ Vec3f(0,-1,0).arr ++ carr
    
    

    val hwxr = hwx-r
    val hwyr = hwy-r
    val hwzr = hwz-r

    val cylinderY = cylinderVNC(r, hwyr, color, detail*4, true)
    val lengthOfBlock = cylinderY.length / 4

    val cylinderYp2p4 = Array.tabulate(lengthOfBlock)(i => cylinderY(0*lengthOfBlock + i)).transfromSimpleVNC(Mat4f.translate(+hwxr, 0, +hwzr))
    val cylinderYp6p8 = Array.tabulate(lengthOfBlock)(i => cylinderY(1*lengthOfBlock + i)).transfromSimpleVNC(Mat4f.translate(+hwxr, 0, -hwzr))
    val cylinderYp5p7 = Array.tabulate(lengthOfBlock)(i => cylinderY(2*lengthOfBlock + i)).transfromSimpleVNC(Mat4f.translate(-hwxr, 0, -hwzr))
    val cylinderYp1p3 = Array.tabulate(lengthOfBlock)(i => cylinderY(3*lengthOfBlock + i)).transfromSimpleVNC(Mat4f.translate(-hwxr, 0, +hwzr))

    val cylinderX = cylinderVNC(r, hwxr, color, detail*4, true).transfromSimpleVNC(Mat4f.rotate(-90, 0, 0, 1))
    
    val cylinderXp1p2 = Array.tabulate(lengthOfBlock)(i => cylinderX(0*lengthOfBlock + i)).transfromSimpleVNC(Mat4f.translate(0, -hwyr, +hwzr))
    val cylinderXp5p6 = Array.tabulate(lengthOfBlock)(i => cylinderX(1*lengthOfBlock + i)).transfromSimpleVNC(Mat4f.translate(0, -hwyr, -hwzr))
    val cylinderXp7p8 = Array.tabulate(lengthOfBlock)(i => cylinderX(2*lengthOfBlock + i)).transfromSimpleVNC(Mat4f.translate(0, +hwyr, -hwzr))
    val cylinderXp3p4 = Array.tabulate(lengthOfBlock)(i => cylinderX(3*lengthOfBlock + i)).transfromSimpleVNC(Mat4f.translate(0, +hwyr, +hwzr))

    val cylinderZ = cylinderVNC(r, hwzr, color, detail*4, true).transfromSimpleVNC(Mat4f.rotate(90, 1, 0, 0))
    
    val cylinderZp2p6 = Array.tabulate(lengthOfBlock)(i => cylinderZ(0*lengthOfBlock + i)).transfromSimpleVNC(Mat4f.translate(+hwxr, -hwyr, 0))
    val cylinderZp4p8 = Array.tabulate(lengthOfBlock)(i => cylinderZ(1*lengthOfBlock + i)).transfromSimpleVNC(Mat4f.translate(+hwxr, +hwyr, 0))
    val cylinderZp3p7 = Array.tabulate(lengthOfBlock)(i => cylinderZ(2*lengthOfBlock + i)).transfromSimpleVNC(Mat4f.translate(-hwxr, +hwyr, 0))
    val cylinderZp1p5 = Array.tabulate(lengthOfBlock)(i => cylinderZ(3*lengthOfBlock + i)).transfromSimpleVNC(Mat4f.translate(-hwxr, -hwyr, 0))

    // the following ensures that the sphere uses just the right number of triangles
    // in order to math the number of faces used for the cylinder (at least for power-of-2 detail values)
    // otherwise there are visible gaps between the cylinder and the sphere parts
    val sphereDetail = (math.log(detail) / math.log(2)).toInt
    val sphere = sphereVNC(r, color, sphereDetail)
    val lengthOneEighth = sphere.length / 8
    
    val sphere1 = Array.tabulate(lengthOneEighth)(i => sphere(0*lengthOneEighth + i)).transfromSimpleVNC(Mat4f.translate(+hwxr, -hwyr, +hwzr))
    val sphere2 = Array.tabulate(lengthOneEighth)(i => sphere(1*lengthOneEighth + i)).transfromSimpleVNC(Mat4f.translate(+hwxr, -hwyr, -hwzr))
    val sphere3 = Array.tabulate(lengthOneEighth)(i => sphere(2*lengthOneEighth + i)).transfromSimpleVNC(Mat4f.translate(-hwxr, -hwyr, -hwzr))
    val sphere4 = Array.tabulate(lengthOneEighth)(i => sphere(3*lengthOneEighth + i)).transfromSimpleVNC(Mat4f.translate(-hwxr, -hwyr, +hwzr))
    val sphere5 = Array.tabulate(lengthOneEighth)(i => sphere(4*lengthOneEighth + i)).transfromSimpleVNC(Mat4f.translate(+hwxr, +hwyr, +hwzr))
    val sphere6 = Array.tabulate(lengthOneEighth)(i => sphere(5*lengthOneEighth + i)).transfromSimpleVNC(Mat4f.translate(+hwxr, +hwyr, -hwzr))
    val sphere7 = Array.tabulate(lengthOneEighth)(i => sphere(6*lengthOneEighth + i)).transfromSimpleVNC(Mat4f.translate(-hwxr, +hwyr, -hwzr))
    val sphere8 = Array.tabulate(lengthOneEighth)(i => sphere(7*lengthOneEighth + i)).transfromSimpleVNC(Mat4f.translate(-hwxr, +hwyr, +hwzr))
    
    return triangles ++ cylinderYp2p4 ++ cylinderYp6p8 ++ cylinderYp5p7 ++ cylinderYp1p3 ++
                        cylinderXp1p2 ++ cylinderXp5p6 ++ cylinderXp7p8 ++ cylinderXp3p4 ++
                        cylinderZp2p6 ++ cylinderZp4p8 ++ cylinderZp3p7 ++ cylinderZp1p5 ++
                        sphere1 ++ sphere2 ++ sphere3 ++ sphere4 ++ sphere5 ++ sphere6 ++ sphere7 ++ sphere8
  }

  
  
}




















