package com.github.bluenote

import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL12._
import org.lwjgl.opengl.GL14._
import org.lwjgl.opengl.GL20._
import org.lwjgl.opengl.GL30._


object FramebufferUtils {
  
  def checkFramebuffer() {
    val status = glCheckFramebufferStatus(GL_FRAMEBUFFER)
    val error = status match {
      case GL_FRAMEBUFFER_COMPLETE => None
      case GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT => Some("GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT")
      case GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER => Some("GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER")
      case GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT => Some("GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT")
      case GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE => Some("GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE")
      case GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER => Some("GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER")
      case GL_FRAMEBUFFER_UNSUPPORTED => Some("GL_FRAMEBUFFER_UNSUPPORTED")
      case GL_FRAMEBUFFER_UNDEFINED => Some("GL_FRAMEBUFFER_UNDEFINED")
      case x => Some(x.toString)
    }
    error.foreach{ msg =>
      println("Framebuffer status is not complete: " + msg)
    }
  }
  
}


class FramebufferTexture(val fboW: Int, val fboH: Int, useMipMap: Boolean = false) {

  GlWrapper.checkGlError("before FBO initialization", true)
  println(f"creating framebuffer texture of resolution: $fboW x $fboH")
  
  // create FBO
  val framebufferId = glGenFramebuffers()
  
  activate()
  
  // gen & bind texture (for colors)
  val textureId = glGenTextures()
  glBindTexture(GL_TEXTURE_2D, textureId)
  //glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST) 
  //glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST) // GL_NEAREST, GL_LINEAR, GL_LINEAR_MIPMAP_LINEAR
  glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR) 
  glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, if (!useMipMap) GL_LINEAR else GL_LINEAR_MIPMAP_LINEAR ) // GL_NEAREST, GL_LINEAR, GL_LINEAR_MIPMAP_LINEAR
  glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
  glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
  // Allocate space for the texture
  glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, fboW, fboH, 0, GL_RGBA, GL_UNSIGNED_BYTE, null.asInstanceOf[java.nio.ByteBuffer])
  
  // gen & bind renderbuffer (for depth)
  val depthBufferId = glGenRenderbuffers()
  glBindRenderbuffer(GL_RENDERBUFFER, depthBufferId)
  glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, fboW, fboH) // or GL_DEPTH_COMPONENT ? or 32 ? or 32F ? seems to improve z-fighting
  
  // attach texture to FBO
  glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, textureId, 0) // last parameter is the mipmap level
  glDrawBuffers(GL_COLOR_ATTACHMENT0)

  // attach renderbuffer to FBO
  glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthBufferId)
  
  // what does this?
  //glEnable(GL_TEXTURE_2D)
  
  deactivate()
  
  GlWrapper.checkGlError()
  
  def activate() {
    //glEnable(GL_TEXTURE_2D)
    glViewport(0, 0, fboW, fboH)
    glBindTexture(GL_TEXTURE_2D, 0)
    glBindFramebuffer(GL_FRAMEBUFFER, framebufferId)
  }
  
  def deactivate() {
    glBindFramebuffer(GL_FRAMEBUFFER, 0)
    if (useMipMap) {
      glBindTexture(GL_TEXTURE_2D, textureId)
      glEnable(GL_TEXTURE_2D)
      glGenerateMipmap(GL_TEXTURE_2D)
      glBindTexture(GL_TEXTURE_2D, 0)
      GlWrapper.checkGlError()
    }    
  }
}
