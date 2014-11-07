package com.github.bluenote


import org.lwjgl.BufferUtils
import java.nio.IntBuffer
import java.nio.FloatBuffer


object ScalaBufferUtils {
  
  def convertToIntBuffer(l: Seq[Int]): IntBuffer = {
    val buf = BufferUtils.createIntBuffer(l.length)
    l.foreach{ x => buf.put(x) }
    buf.flip()
    return buf
  }
  def convertToIntBuffer(l: Array[Int]): IntBuffer = {
    val buf = BufferUtils.createIntBuffer(l.length)
    buf.put(l)
    buf.flip()
    return buf
  }
  
  
  def convertToFloatBuffer(l: Seq[Float]): FloatBuffer = {
    val buf = BufferUtils.createFloatBuffer(l.length)
    l.foreach{ x => buf.put(x) }
    buf.flip()
    return buf
  }
  def convertToFloatBuffer(l: Array[Float]): FloatBuffer = {
    val buf = BufferUtils.createFloatBuffer(l.length)
    buf.put(l)
    buf.flip()
    return buf
  }
  
}





