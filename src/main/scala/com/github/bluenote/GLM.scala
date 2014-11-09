package com.github.bluenote

import java.nio.FloatBuffer
import org.lwjgl.BufferUtils

case class MatCmpPrecision(precision: Double)

/**
 * Simple vector implementation
 */
case class Vec3f(var x: Float, var y: Float, var z: Float) {
  
  def /(a: Float) = Vec3f(x/a, y/a, z/a)
  def *(a: Float) = Vec3f(x*a, y*a, z*a)
  def +(that: Vec3f) = Vec3f(this.x+that.x, this.y+that.y, this.z+that.z)
  def -(that: Vec3f) = Vec3f(this.x-that.x, this.y-that.y, this.z-that.z)

  def *(that: Vec3f) = this.x*that.x + this.y*that.y + this.z*that.z
  
  def cross(that: Vec3f) = Vec3f(
    this.y*that.z - this.z*that.y,
    this.z*that.x - this.x*that.z,
    this.x*that.y - this.y*that.x
  )
  
  def mid(that: Vec3f) = Vec3f(0.5f*(this.x+that.x), 0.5f*(this.y+that.y), 0.5f*(this.z+that.z))
  
  def length = math.sqrt(x*x + y*y + z*z).toFloat
  
  def setLengthTo(a: Float): Vec3f = {
    val norm = this.length
    this * (a/norm)
  }
  def normalize(): Vec3f = {
    val norm = this.length
    this / norm
  }

  def negate() = Vec3f(-x, -y, -z)
  
  // convenience for VBO construction
  def arr = Array(x, y, z)
  def toVec4f = Vec4f(x, y, z, 1)
}
case class Vec4f(var x: Float, var y: Float, var z: Float, var w: Float) {
  
  def /(a: Float) = Vec4f(x/a, y/a, z/a, w/a)
  def *(a: Float) = Vec4f(x*a, y*a, z*a, w*a)
  def +(that: Vec4f) = Vec4f(this.x+that.x, this.y+that.y, this.z+that.z, this.w+that.w)
  def -(that: Vec4f) = Vec4f(this.x-that.x, this.y-that.y, this.z-that.z, this.w-that.w)
  
  def *(that: Vec4f) = this.x*that.x + this.y*that.y + this.z*that.z + this.w*that.w
  
  def mid(that: Vec4f) = Vec4f(0.5f*(this.x+that.x), 0.5f*(this.y+that.y), 0.5f*(this.z+that.z), 0.5f*(this.w+that.w))
  
  def length = math.sqrt(x*x + y*y + z*z + w*w).toFloat
  
  def setLengthTo(a: Float): Vec4f = {
    val norm = this.length
    this * (a/norm)
  }
  def normalize(): Vec4f = {
    val norm = this.length
    this / norm
  }

  def negate() = Vec4f(-x, -y, -z, -w)
  
  // convenience for VBO construction
  def arr = Array(x, y, z, w)
  def toVec3f = Vec3f(x, y, z)
}

/**
 * Quaternion implementation to handle rotations without gimbal lock
 */
class Quaternion(var x: Float, var y: Float, var z: Float, var w: Float) {
  def *(that: Quaternion): Quaternion = {
    new Quaternion(
      this.w*that.x + this.x*that.w + this.y*that.z - this.z*that.y,
      this.w*that.y + this.y*that.w + this.z*that.x - this.x*that.z,
      this.w*that.z + this.z*that.w + this.x*that.y - this.y*that.x,
      this.w*that.w - this.x*that.x - this.y*that.y - this.z*that.z
    )
  }
  
  def norm(): Quaternion = {
    val sum = math.sqrt(x*x+y*y+z*z+w*w).toFloat
    new Quaternion(x/sum, y/sum, z/sum, w/sum)
  }
  
  def castToOrientationMatrix(): Mat4f = {
    new Mat4f(
      1-2*y*y-2*z*z,   2*x*y-2*w*z,   2*x*z+2*w*y,   0,
        2*x*y+2*w*z, 1-2*x*x-2*z*z,   2*y*z-2*w*x,   0, 
        2*x*z-2*w*y,   2*y*z+2*w*x, 1-2*x*x-2*y*y,   0,
                  0,             0,             0,   1       
    )
  }
  def castToOrientationMatrixRH(): Mat4f = {
    new Mat4f(
      1-2*y*y-2*z*z,   2*x*y+2*w*z,   2*x*z-2*w*y,   0,
        2*x*y-2*w*z, 1-2*x*x-2*z*z,   2*y*z+2*w*x,   0, 
        2*x*z+2*w*y,   2*y*z-2*w*x, 1-2*x*x-2*y*y,   0,
                  0,             0,             0,   1       
    )
  }
  
  // inspired by: http://www.euclideanspace.com/maths/geometry/rotations/conversions/quaternionToEuler/
  def toEuler(): EulerAngles = {
    val test = x*y + z*w
    if (test > 0.499) { // singularity at north pole
      val yaw   = 2 * math.atan2(x, w)
      val roll  = Math.PI/2
      val pitch = 0f
      return EulerAngles(yaw.toFloat*180/Math.PI.toFloat, pitch.toFloat*180/Math.PI.toFloat, roll.toFloat*180/Math.PI.toFloat)
    }
    if (test < -0.499) { // singularity at south pole
      val yaw   = -2 * Math.atan2(x, w)
      val roll  = -Math.PI/2
      val pitch = 0
      return EulerAngles(yaw.toFloat*180/Math.PI.toFloat, pitch.toFloat*180/Math.PI.toFloat, roll.toFloat*180/Math.PI.toFloat)
    }
    val sqx = x * x
    val sqy = y * y
    val sqz = z * z
    val yaw   = Math.atan2(2*y*w-2*x*z, 1 - 2*sqy - 2*sqz)
    val roll  = Math.asin(2*test)
    val pitch = Math.atan2(2*x*w-2*y*z, 1 - 2*sqx - 2*sqz)
    return EulerAngles(yaw.toFloat*180/Math.PI.toFloat, pitch.toFloat*180/Math.PI.toFloat, roll.toFloat*180/Math.PI.toFloat)
  } 
  
  
}

object Quaternion {
  /**
   * theta is assumed to be in DEG.
   */
  def create(thetaDeg: Float, x: Float, y: Float, z: Float): Quaternion = {
    val thetaHalf = thetaDeg/2 * math.Pi/180
    val sinThetaHalf = math.sin(thetaHalf).toFloat
    val cosThetaHalf = math.cos(thetaHalf).toFloat
    new Quaternion(x*sinThetaHalf, y*sinThetaHalf, z*sinThetaHalf, cosThetaHalf)
  }
  
  /**
   * m must be a pure rotation (orthogonal), no scale/shear allowed
   * 
   * http://www.cs.princeton.edu/~gewang/projects/darth/stuff/quat_faq.html#Q55
   */
  def createFromRotationMatrix(m: Mat3f): Quaternion = {
    
    val trace = m.m00 + m.m11 + m.m22 + 1 
    
    if (trace > 0.0000001) {
      val s = math.sqrt(trace).toFloat * 2
      val x = (m.m12 - m.m21) / s
      val y = (m.m20 - m.m02) / s
      val z = (m.m01 - m.m10) / s
      val w = 0.25f * s
      new Quaternion(x, y, z, w)
    } else if (m.m00 > m.m11 && m.m00 > m.m22) {
      val s = math.sqrt(1f + m.m00 - m.m11 - m.m22).toFloat * 2
      val x = 0.25f * s
      val y = (m.m01 + m.m10) / s
      val z = (m.m20 + m.m02) / s
      val w = (m.m12 - m.m21) / s
      new Quaternion(x, y, z, w)
    } else if (m.m11 > m.m22) {
      val s = math.sqrt(1f + m.m11 - m.m00 - m.m22).toFloat * 2
      val x = (m.m01 + m.m10) / s
      val y = 0.25f * s
      val z = (m.m12 + m.m21) / s
      val w = (m.m20 - m.m02) / s
      new Quaternion(x, y, z, w)
    } else {
      val s = math.sqrt(1f + m.m22 - m.m00 - m.m11).toFloat * 2
      val x = (m.m20 + m.m02) / s
      val y = (m.m12 + m.m21) / s
      val z = 0.25f * s
      val w = (m.m01 - m.m10) / s
      new Quaternion(x, y, z, w)
    }    
  }
}

case class EulerAngles(yaw: Float, pitch: Float, roll: Float)



/**
 * 4 dimensional Matrix implementation.
 * I know there are Java possibilities:
 * - https://github.com/jroyalty/jglm/blob/master/src/main/java/com/hackoeur/jglm/Mat4.java
 * - https://github.com/LWJGL/lwjgl/blob/master/src/java/org/lwjgl/util/vector/Matrix4f.java
 * But preferrable in Scala...
 */
class Mat4f(
  var m00: Float, var m10: Float, var m20: Float, var m30: Float,
  var m01: Float, var m11: Float, var m21: Float, var m31: Float,
  var m02: Float, var m12: Float, var m22: Float, var m32: Float,
  var m03: Float, var m13: Float, var m23: Float, var m33: Float
) {
  
  def +(that: Mat4f): Mat4f = {
    new Mat4f(
      this.m00+that.m00, this.m10+that.m10, this.m20+that.m20, this.m30+that.m30,
      this.m01+that.m01, this.m11+that.m11, this.m21+that.m21, this.m31+that.m31,
      this.m02+that.m02, this.m12+that.m12, this.m22+that.m22, this.m32+that.m32,
      this.m03+that.m03, this.m13+that.m13, this.m23+that.m23, this.m33+that.m33
    )
  }
  
  def *(that: Mat4f): Mat4f = {
    val nm00 = this.m00 * that.m00 + this.m10 * that.m01 + this.m20 * that.m02 + this.m30 * that.m03;
    val nm01 = this.m01 * that.m00 + this.m11 * that.m01 + this.m21 * that.m02 + this.m31 * that.m03;
    val nm02 = this.m02 * that.m00 + this.m12 * that.m01 + this.m22 * that.m02 + this.m32 * that.m03;
    val nm03 = this.m03 * that.m00 + this.m13 * that.m01 + this.m23 * that.m02 + this.m33 * that.m03;
    val nm10 = this.m00 * that.m10 + this.m10 * that.m11 + this.m20 * that.m12 + this.m30 * that.m13;
    val nm11 = this.m01 * that.m10 + this.m11 * that.m11 + this.m21 * that.m12 + this.m31 * that.m13;
    val nm12 = this.m02 * that.m10 + this.m12 * that.m11 + this.m22 * that.m12 + this.m32 * that.m13;
    val nm13 = this.m03 * that.m10 + this.m13 * that.m11 + this.m23 * that.m12 + this.m33 * that.m13;
    val nm20 = this.m00 * that.m20 + this.m10 * that.m21 + this.m20 * that.m22 + this.m30 * that.m23;
    val nm21 = this.m01 * that.m20 + this.m11 * that.m21 + this.m21 * that.m22 + this.m31 * that.m23;
    val nm22 = this.m02 * that.m20 + this.m12 * that.m21 + this.m22 * that.m22 + this.m32 * that.m23;
    val nm23 = this.m03 * that.m20 + this.m13 * that.m21 + this.m23 * that.m22 + this.m33 * that.m23;
    val nm30 = this.m00 * that.m30 + this.m10 * that.m31 + this.m20 * that.m32 + this.m30 * that.m33;
    val nm31 = this.m01 * that.m30 + this.m11 * that.m31 + this.m21 * that.m32 + this.m31 * that.m33;
    val nm32 = this.m02 * that.m30 + this.m12 * that.m31 + this.m22 * that.m32 + this.m32 * that.m33;
    val nm33 = this.m03 * that.m30 + this.m13 * that.m31 + this.m23 * that.m32 + this.m33 * that.m33;
    new Mat4f(
      nm00, nm10, nm20, nm30,
      nm01, nm11, nm21, nm31,
      nm02, nm12, nm22, nm32,
      nm03, nm13, nm23, nm33
    )
  }
  def *(v: Vec4f): Vec4f = {
    Vec4f(
      m00*v.x + m10*v.y + m20*v.z + m30*v.w,
      m01*v.x + m11*v.y + m21*v.z + m31*v.w,
      m02*v.x + m12*v.y + m22*v.z + m32*v.w,
      m03*v.x + m13*v.y + m23*v.z + m33*v.w
    )
  }
  def *(s: Float): Mat4f = { 
    new Mat4f(
      s*m00, s*m10, s*m20, s*m30,
      s*m01, s*m11, s*m21, s*m31,
      s*m02, s*m12, s*m22, s*m32,
      s*m03, s*m13, s*m23, s*m33
    )
  }
  
  def frobeniusDistance(that: Mat4f): Float = {
    math.sqrt(
      (this.m00-that.m00)*(this.m00-that.m00) + 
      (this.m01-that.m01)*(this.m01-that.m01) +
      (this.m02-that.m02)*(this.m02-that.m02) +
      (this.m03-that.m03)*(this.m03-that.m03) +
      (this.m10-that.m10)*(this.m10-that.m10) + 
      (this.m11-that.m11)*(this.m11-that.m11) +
      (this.m12-that.m12)*(this.m12-that.m12) +
      (this.m13-that.m13)*(this.m13-that.m13) +
      (this.m20-that.m20)*(this.m20-that.m20) + 
      (this.m21-that.m21)*(this.m21-that.m21) +
      (this.m22-that.m22)*(this.m22-that.m22) +
      (this.m23-that.m23)*(this.m23-that.m23) +
      (this.m30-that.m30)*(this.m30-that.m30) + 
      (this.m31-that.m31)*(this.m31-that.m31) +
      (this.m32-that.m32)*(this.m32-that.m32) +
      (this.m33-that.m33)*(this.m33-that.m33)
    ).toFloat
  }  
  
  def asFloatBuffer(): FloatBuffer = {
    val buf = BufferUtils.createFloatBuffer(16)
    store(buf, true)
    buf.flip()
    return buf
  }
  def asFloatBuffer(columnMajor: Boolean): FloatBuffer = {
    val buf = BufferUtils.createFloatBuffer(16)
    store(buf, columnMajor)
    buf.flip()
    return buf
  }
  
  def store(buf: FloatBuffer, columnMajor: Boolean = true) {
    if (columnMajor) {
      buf.put(m00);
      buf.put(m01);
      buf.put(m02);
      buf.put(m03);
      buf.put(m10);
      buf.put(m11);
      buf.put(m12);
      buf.put(m13);
      buf.put(m20);
      buf.put(m21);
      buf.put(m22);
      buf.put(m23);
      buf.put(m30);
      buf.put(m31);
      buf.put(m32);
      buf.put(m33);
    } else {
      buf.put(m00);
      buf.put(m10);
      buf.put(m20);
      buf.put(m30);
      buf.put(m01);
      buf.put(m11);
      buf.put(m21);
      buf.put(m31);
      buf.put(m02);
      buf.put(m12);
      buf.put(m22);
      buf.put(m32);
      buf.put(m03);
      buf.put(m13);
      buf.put(m23);
      buf.put(m33);
    }
  }
  
  def rotateYawPitchRoll(yaw: Float, pitch: Float, roll: Float, leftMultiply: Boolean = false): Mat4f = {
    if (!leftMultiply) {
      return this * Mat4f.rotateYawPitchRoll(yaw, pitch, roll)
    } else {
      return Mat4f.rotateYawPitchRoll(yaw, pitch, roll) * this
    }
  }
  def rotateYawPitchRollQuaternions(yaw: Float, pitch: Float, roll: Float, leftMultiply: Boolean = false): Mat4f = {
    if (!leftMultiply) {
      return this * Mat4f.rotateYawPitchRollQuaternions(yaw, pitch, roll)
    } else {
      return Mat4f.rotateYawPitchRollQuaternions(yaw, pitch, roll) * this
    }
  }  
  def rotate(angle: Float, x: Float, y: Float, z: Float, leftMultiply: Boolean = false): Mat4f = {
    if (!leftMultiply) {
      return this * Mat4f.rotate(angle, x, y, z)
    } else {
      return Mat4f.rotate(angle, x, y, z) * this
    }
  }  
  def translate(x: Float, y: Float, z: Float, leftMultiply: Boolean = false): Mat4f = {
    if (!leftMultiply) {
      return this * Mat4f.translate(x, y, z)
    } else {
      return Mat4f.translate(x, y, z) * this
    }    
  }
  def scale(x: Float, y: Float, z: Float): Mat4f = {
    return this * Mat4f.scale(x, y, z)
  }  
  
  
  def transpose(): Mat4f = {
    new Mat4f(
      m00, m01, m02, m03,
      m10, m11, m12, m13,
      m20, m21, m22, m23,
      m30, m31, m32, m33
    )
  }
  
  /**
   * Inversion of affine matrix
   *   http://stackoverflow.com/questions/2624422/efficient-4x4-matrix-inverse-affine-transform
   */
  def inverseAffine(): Mat4f = {
    val blockInv = Mat3f.createFromMat4f(this).inverse
    val posVec = blockInv * Vec3f(this.m30, this.m31, this.m32).negate
    Mat4f.createFromAffine(blockInv, posVec)
  }
  
  override def toString() = f"Mat4f(\n" +
    f"  $m00%8.3f, $m10%8.3f, $m20%8.3f, $m30%8.3f\n" +
    f"  $m01%8.3f, $m11%8.3f, $m21%8.3f, $m31%8.3f\n" +
    f"  $m02%8.3f, $m12%8.3f, $m22%8.3f, $m32%8.3f\n" +
    f"  $m03%8.3f, $m13%8.3f, $m23%8.3f, $m33%8.3f\n" +
    f")"
  
  def arr = Array(m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23, m30, m31, m32, m33)  
    
  def ~=(that: Mat4f)(implicit precision: MatCmpPrecision): Boolean = {
    val thisArr = this.arr
    val thatArr = that.arr
    val maxDiff = Range(0,16).map(i => math.abs(thisArr(i)-thatArr(i))).max
    return maxDiff < precision.precision
  }
}


object Mat4f {
  
  /**
   * Translation
   */
  def translate(x: Float, y: Float, z: Float): Mat4f = {
    new Mat4f(
      1, 0, 0, x,
      0, 1, 0, y,
      0, 0, 1, z,
      0, 0, 0, 1
    )
  }

  /**
   * Scale
   */
  def scale(x: Float, y: Float, z: Float): Mat4f = {
    new Mat4f(
      x, 0, 0, 0,
      0, y, 0, 0,
      0, 0, z, 0,
      0, 0, 0, 1
    )
  }
  
  /**
   * Reimplementation of glRotate:
   *   https://www.opengl.org/sdk/docs/man2/xhtml/glRotate.xml
   * 
   * Convention: angles in DEG
   */
  def rotate(angle: Float, _x: Float, _y: Float, _z: Float): Mat4f = {
    val angleRAD = angle * math.Pi / 180
    val axis = Vec3f(_x, _y, _z).setLengthTo(1.0f)
    val x = axis.x
    val y = axis.y
    val z = axis.z
    val s = math.sin(angleRAD).toFloat
    val c = math.cos(angleRAD).toFloat
    
    new Mat4f(
      x*x*(1-c)+c,    x*y*(1-c)-z*s,  x*z*(1-c)+y*s,   0,
      y*x*(1-c)+z*s,  y*y*(1-c)+c,    y*z*(1-c)-x*s,   0, 
      x*z*(1-c)-y*s,  y*z*(1-c)+x*s,  z*z*(1-c)+c,     0,
                  0,              0,     0,            1
    )
  }
  
  /**
   * http://www.flipcode.com/documents/matrfaq.html#Q36
   * http://www.songho.ca/opengl/gl_anglestoaxes.html
   * 
         |  CE      -CF      -D   0 |
    M  = | -BDE+AF   BDF+AE  -BC  0 |
         |  ADE+BF  -ADF+BE   AC  0 |
         |  0        0        0   1 |
   *   where A,B are the cosine and sine of the X-axis rotation axis, (pitch)
   *         C,D are the cosine and sine of the Y-axis rotation axis, (yaw)
   *         E,F are the cosine and sine of the Z-axis rotation axis. (roll)
   * 
   * Convention: angles in DEG
   */
  def rotateYawPitchRoll(yaw: Float, pitch: Float, roll: Float): Mat4f = {
    val A = math.cos(pitch*math.Pi/180).toFloat
    val B = math.sin(pitch*math.Pi/180).toFloat
    val C = math.cos(yaw*math.Pi/180).toFloat
    val D = math.sin(yaw*math.Pi/180).toFloat
    val E = math.cos(roll*math.Pi/180).toFloat
    val F = math.sin(roll*math.Pi/180).toFloat
    new Mat4f(
             C*E,        -C*F,    -D,   0,
      -B*D*E+A*F,   B*D*F+A*E,  -B*C,   0, 
       A*D*E+B*F,  -A*D*F+B*E,   A*C,   0,
               0,           0,     0,   1
    )
  }
  
  /**
   * Though the above approach is mentioned frequently, it looks like is still has gimbal lock issues.
   * Therefore proper solution using quaternions:
   * - convert each euler angle to a quaternion
   * - multiply quaternions (in correct order!)
   * - convert the resulting quaternion to a rotation matrix.
   * Regarding the order of the multiplication:
   * This depends on the definition of the euler angles.
   * Here the order is "optimized" for Oculus Rift.
   * In case of trouble: trial & error...
   * 
   * Convention: angles in DEG
   */
  def rotateYawPitchRollQuaternions(yaw: Float, pitch: Float, roll: Float): Mat4f = {
    val quatPitch = Quaternion.create(pitch, 1, 0, 0)
    val quatYaw   = Quaternion.create(yaw,   0, 1, 0)
    val quatRoll  = Quaternion.create(roll,  0, 0, 1)
    // order that does not work: pitch * yaw * roll (roll gets inverted when yaw != 0)
    (quatRoll*quatPitch*quatYaw).norm().castToOrientationMatrix
  }

  
  /**
   * The above definitions are not prefixed with "create" in order to directly
   * resemble the operator API.
   * All remaining builders follow the create... convention.
   */
  
  def createIdentity() = new Mat4f(1, 0, 0, 0,    0, 1, 0, 0,    0, 0, 1, 0,    0, 0, 0, 1)
  def createZero()     = new Mat4f(0, 0, 0, 0,    0, 0, 0, 0,    0, 0, 0, 0,    0, 0, 0, 0)
  
  def createFromAffine(M: Mat3f, v: Vec3f) = new Mat4f(
    M.m00, M.m10, M.m20, v.x,
    M.m01, M.m11, M.m21, v.y,
    M.m02, M.m12, M.m22, v.z,
        0,     0,     0,   1
  )
  def createFromMat3f(that: Mat3f): Mat4f = {
    new Mat4f(
      that.m00, that.m10, that.m20, 0, 
      that.m01, that.m11, that.m21, 0,
      that.m02, that.m12, that.m22, 0,
             0,        0,        0, 1
    )    
  }
  
  def createFromFloatBuffer(buf: FloatBuffer): Mat4f = {
    val arr = new Array[Float](16)
    buf.get(arr)
    createFromColumnMajorArray(arr)
  }
  
  def createFromColumnMajorArray(arr: Array[Float]): Mat4f = {
    new Mat4f(
      arr(0), arr(4), arr( 8), arr(12),
      arr(1), arr(5), arr( 9), arr(13),
      arr(2), arr(6), arr(10), arr(14),
      arr(3), arr(7), arr(11), arr(15)
    )
  }

  def createFromRowMajorArray(arr: Array[Float]): Mat4f = {
    new Mat4f(
      arr( 0), arr( 1), arr( 2), arr( 3),
      arr( 4), arr( 5), arr( 6), arr( 7),
      arr( 8), arr( 9), arr(10), arr(11),
      arr(12), arr(13), arr(14), arr(15)
    )
  }
  
  /**
   * Creates a projection matrix (according to Oculus Rift documentation):
   * @param yfov     vertical fov, in RAD
   * @param a        aspect ratio (width/height)
   * @param zn       near clipping plane
   * @param zf       far clipping plane
   */
  def createProjectionFrustumAccordingToOVRSDK(yfov: Float, a: Float, zn: Float, zf: Float): Mat4f = {
    val P = Mat4f.createZero()
    P.m00 =  1f / (a*math.tan(yfov/2).toFloat)
    P.m11 =  1f /   (math.tan(yfov/2).toFloat)
    P.m22 =  (zf-zn) / (zn-zf)             // numerator zf (like SDK docs) or zf-zn like in canonical OpenGL
    P.m23 = -1f
    P.m32 =  2f * (zf*zn) / (zn-zf)   // check: times 2? http://nykl.net/?page_id=175
    P
  }
  /**
   * Creates a projection matrix (according OpenGL canonical form):
   * Whether or not this gives different results in comparison to the formula
   * in the Oculus Rift documentation still unclear...
   *   http://nykl.net/?page_id=175
   * 
   * The obvious difference is the parameterization, since glFrustum uses l/r/b/t instead of fov/aspect
   * The parameterization here follows exactly the OpenGL "glFrustum" function:
   *   http://www.opengl.org/sdk/docs/man2/xhtml/glFrustum.xml

   * @param l         left clipping plane 
   * @param r         right clipping plane 
   * @param b         bottom clipping plane 
   * @param t         top clipping plane 
   * @param zn        near clipping plane
   * @param zf        far clipping plane
   */
  def createProjectionFrustumOpenGLCanonical(l: Float, r: Float, b: Float, t: Float, zn: Float, zf: Float): Mat4f = {
    val P = Mat4f.createZero()
    val A = (r+l) / (r-l)   // this terms is zero for symmetrical frustums
    val B = (t+b) / (t-b)   // this terms is zero for symmetrical frustums
    val C = - (zf+zn) / (zf-zn)
    val D = - 2f*zf*zn / (zf-zn)
    P.m00 =  2f*zn / (r-l)
    P.m11 =  2f*zn / (t-b)
    P.m20 =  A
    P.m21 =  B
    P.m22 =  C
    P.m23 = -1f
    P.m32 =  D 
    P
  }
  


  
  
}




class Mat3f(
  var m00: Float, var m10: Float, var m20: Float, 
  var m01: Float, var m11: Float, var m21: Float, 
  var m02: Float, var m12: Float, var m22: Float 
) {

  def +(that: Mat3f): Mat3f = {
    new Mat3f(
      this.m00+that.m00, this.m10+that.m10, this.m20+that.m20,
      this.m01+that.m01, this.m11+that.m11, this.m21+that.m21,
      this.m02+that.m02, this.m12+that.m12, this.m22+that.m22
    )
  }
  
  def *(that: Mat3f): Mat3f = {
    val nm00 = this.m00 * that.m00 + this.m10 * that.m01 + this.m20 * that.m02
    val nm01 = this.m01 * that.m00 + this.m11 * that.m01 + this.m21 * that.m02
    val nm02 = this.m02 * that.m00 + this.m12 * that.m01 + this.m22 * that.m02
    val nm10 = this.m00 * that.m10 + this.m10 * that.m11 + this.m20 * that.m12
    val nm11 = this.m01 * that.m10 + this.m11 * that.m11 + this.m21 * that.m12
    val nm12 = this.m02 * that.m10 + this.m12 * that.m11 + this.m22 * that.m12
    val nm20 = this.m00 * that.m20 + this.m10 * that.m21 + this.m20 * that.m22
    val nm21 = this.m01 * that.m20 + this.m11 * that.m21 + this.m21 * that.m22
    val nm22 = this.m02 * that.m20 + this.m12 * that.m21 + this.m22 * that.m22
    new Mat3f(
      nm00, nm10, nm20, 
      nm01, nm11, nm21, 
      nm02, nm12, nm22 
    )
  }
  def *(v: Vec3f): Vec3f = {
    Vec3f(
      m00*v.x + m10*v.y + m20*v.z, 
      m01*v.x + m11*v.y + m21*v.z,
      m02*v.x + m12*v.y + m22*v.z
    )
  }
  def *(s: Float): Mat3f = { 
    new Mat3f(
      s*m00, s*m10, s*m20,
      s*m01, s*m11, s*m21,
      s*m02, s*m12, s*m22
    )
  }
  
  def frobeniusDistance(that: Mat3f): Float = {
    math.sqrt(
      (this.m00-that.m00)*(this.m00-that.m00) + 
      (this.m01-that.m01)*(this.m01-that.m01) +
      (this.m02-that.m02)*(this.m02-that.m02) +
      (this.m10-that.m10)*(this.m10-that.m10) + 
      (this.m11-that.m11)*(this.m11-that.m11) +
      (this.m12-that.m12)*(this.m12-that.m12) +
      (this.m20-that.m20)*(this.m20-that.m20) + 
      (this.m21-that.m21)*(this.m21-that.m21) +
      (this.m22-that.m22)*(this.m22-that.m22)
    ).toFloat
  } 
  
  def asFloatBuffer(): FloatBuffer = {
    val buf = BufferUtils.createFloatBuffer(16)
    store(buf, true)
    buf.flip()
    return buf
  }
  def asFloatBuffer(columnMajor: Boolean): FloatBuffer = {
    val buf = BufferUtils.createFloatBuffer(16)
    store(buf, columnMajor)
    buf.flip()
    return buf
  }
  
  def store(buf: FloatBuffer, columnMajor: Boolean = true) {
    if (columnMajor) {
      buf.put(m00);
      buf.put(m01);
      buf.put(m02);
      buf.put(m10);
      buf.put(m11);
      buf.put(m12);
      buf.put(m20);
      buf.put(m21);
      buf.put(m22);
    } else {
      buf.put(m00);
      buf.put(m10);
      buf.put(m20);
      buf.put(m01);
      buf.put(m11);
      buf.put(m21);
      buf.put(m02);
      buf.put(m12);
      buf.put(m22);
    }
  }
  
  /*
  def rotate(angleX: Float, angleY: Float, angleZ: Float, leftMultiply: Boolean = false): Mat4f = {
    if (!leftMultiply) {
      return this * Mat4f.createRotationFromYawPitchRoll(angleZ, angleX, angleY)
    } else {
      return Mat4f.createRotationFromYawPitchRoll(angleZ, angleX, angleY) * this
    }
  }
  def translate(x: Float, y: Float, z: Float): Mat4f = {
    return this * Mat4f.createTranslation(x, y, z)
  }
  */
  
  def transpose(): Mat3f = {
    new Mat3f(
      m00, m01, m02,
      m10, m11, m12,
      m20, m21, m22
    )
  }
  /**
   * https://github.com/LWJGL/lwjgl/blob/master/src/java/org/lwjgl/util/vector/Matrix3f.java
   * http://ardoris.wordpress.com/2008/07/18/general-formula-for-the-inverse-of-a-3x3-matrix/
   */
  def inverse(): Mat3f = {
    val a = m00
    val b = m10
    val c = m20
    val d = m01
    val e = m11
    val f = m21
    val g = m02
    val h = m12
    val i = m22
    val det = 1f / (a*(e*i-f*h) - b*(d*i-f*g) + c*(d*h-e*g))
    new Mat3f(
      det*(e*i-f*h), det*(c*h-b*i), det*(b*f-c*e),
      det*(f*g-d*i), det*(a*i-c*g), det*(c*d-a*f),
      det*(d*h-e*g), det*(b*g-a*h), det*(a*e-b*d)
    )
  }
  
  override def toString() = f"Mat3f(\n" +
    f"  $m00%8.3f, $m10%8.3f, $m20%8.3f\n" +
    f"  $m01%8.3f, $m11%8.3f, $m21%8.3f\n" +
    f"  $m02%8.3f, $m12%8.3f, $m22%8.3f\n" +
    f")"

  def arr = Array(m00, m01, m02, m10, m11, m12, m20, m21, m22)  
    
  def ~=(that: Mat3f)(implicit precision: MatCmpPrecision): Boolean = {
    val thisArr = this.arr
    val thatArr = that.arr
    val maxDiff = Range(0,9).map(i => math.abs(thisArr(i)-thatArr(i))).max
    return maxDiff < precision.precision
  }

}


object Mat3f {
  
  def createIdentity(): Mat3f = {
    new Mat3f(
      1, 0, 0, 
      0, 1, 0, 
      0, 0, 1 
    )    
  } 
  
  def createFromMat4f(that: Mat4f): Mat3f = {
    new Mat3f(
      that.m00, that.m10, that.m20, 
      that.m01, that.m11, that.m21, 
      that.m02, that.m12, that.m22 
    )    
  }
  
}


