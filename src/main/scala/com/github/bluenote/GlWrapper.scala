package com.github.bluenote

import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20


/**
 * Helper class to cache GL specific state (depth testing, ...).
 * Allows to minimize the state changes without having to query GL itself.
 */
trait GlStateWrapper[T] {
  private var lastState: Option[T] = None
  def set(state: T) {
    if (lastState != state) {
      applyStateChange(state)
      lastState = Some(state)
    }
  }
  def get() = lastState
  protected def applyStateChange(state: T)
}

class GlStateWrapperDepthTest extends GlStateWrapper[Boolean] {
  def applyStateChange(depthEnable: Boolean) {
    depthEnable match {
      case true  => GL11.glEnable(GL11.GL_DEPTH_TEST)
      case false => GL11.glDisable(GL11.GL_DEPTH_TEST)
    }
  }
}

class GlStateWrapperShaderProgram extends GlStateWrapper[Int] {
  def applyStateChange(shaderProg: Int) {
    GL20.glUseProgram(shaderProg)
  }
}

class GlStateWrapperClearColor extends GlStateWrapper[Color] {
  def applyStateChange(clearColor: Color) {
    GL11.glClearColor(clearColor.r, clearColor.g, clearColor.b, clearColor.a)
  }
}



object GlWrapper {
  
  /** various wrappers of GL states */
  val depthTest = new GlStateWrapperDepthTest
  val shaderProgram = new GlStateWrapperShaderProgram
  val clearColor = new GlStateWrapperClearColor
  
  /** basic GL error state checking */
  def checkGlError(codeLocation: String = "", ignoreError: Boolean = false): Int = {
    val error = GL11.glGetError()
    val errorMsg = error match {
      case 0 => None
      case GL11.GL_INVALID_ENUM      => Some("GL_INVALID_ENUM")
      case GL11.GL_INVALID_VALUE     => Some("GL_INVALID_VALUE")
      case GL11.GL_INVALID_OPERATION => Some("GL_INVALID_OPERATION")
      case GL11.GL_STACK_OVERFLOW    => Some("GL_STACK_OVERFLOW")
      case GL11.GL_STACK_UNDERFLOW   => Some("GL_STACK_UNDERFLOW")
      case GL11.GL_OUT_OF_MEMORY     => Some("GL_OUT_OF_MEMORY")
      case s => Some(s.toString)
    }
    errorMsg.foreach { msg =>
      val optCodeLocation = if (codeLocation=="") None else Some(codeLocation)
      val fullMsg = f"OpenGL error: $msg [$error] ${optCodeLocation.map(l => f" @ $l | ").getOrElse("| ")}" 
      println(fullMsg)
      if (!ignoreError) {
        throw new Exception(fullMsg)
      }
    }
    error
  }  

}
