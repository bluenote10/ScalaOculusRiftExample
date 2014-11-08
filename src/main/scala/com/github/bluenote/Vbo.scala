package com.github.bluenote

import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL30






/**
 * VBO wrapper for static vertex data
 */
class StaticVbo(vertexData: VertexData, shader: Shader) {
  
  // initialize a VAO and bind it
  val vao = GL30.glGenVertexArrays()
  GL30.glBindVertexArray(vao)

  // initialize the VBO and bind it
  val vbId = GL15.glGenBuffers()
  GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbId)

  // make the attribute bindings
  // that is where the association between the VAO and the current GL_ARRAY_BUFFER is evaluated and stored
  vertexData.setVertexAttribArrayAndPointer(shader)
  
  // buffer the static vertex data
  GL15.glBufferData(GL15.GL_ARRAY_BUFFER, ScalaBufferUtils.convertToFloatBuffer(vertexData.rawData), GL15.GL_STATIC_DRAW)
  GlWrapper.checkGlError(getClass + " -- after buffering the data")
  
  // unbind everything
  GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)
  GL30.glBindVertexArray(0)
  GlWrapper.checkGlError(getClass + " -- after clean up")
  
  
  def render() {
    // bind
    //GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbId)
    GL30.glBindVertexArray(vao)
    GlWrapper.checkGlError(getClass + " -- after binding VAO")
    
    shader.use()
    GlWrapper.checkGlError(getClass + " -- after switching to shader")
    //prog.switchBackToFixedPipeline

    // draw
    GL11.glDrawArrays(vertexData.primitiveType, 0, vertexData.numVertices)
    GlWrapper.checkGlError(getClass + " -- after drawing")
    
    // unbind
    GL30.glBindVertexArray(0)
    GlWrapper.checkGlError(getClass + " -- finished rendering")
  }
  
}











