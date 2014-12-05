package com.github.bluenote

import org.lwjgl.system.glfw._
import org.lwjgl.system.glfw.GLFW._
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GLContext
import com.oculusvr.capi.Hmd
import com.oculusvr.capi.OvrLibrary
import com.oculusvr.capi.OvrLibrary.ovrDistortionCaps._
import com.oculusvr.capi.OvrLibrary.ovrTrackingCaps._
import com.oculusvr.capi.OvrLibrary.ovrHmdCaps._
import com.oculusvr.capi.OvrVector2i
import com.oculusvr.capi.OvrVector3f
import com.oculusvr.capi.Posef
import com.oculusvr.capi.RenderAPIConfig
import com.oculusvr.capi.Texture
import com.sun.jna.Structure
import org.lwjgl.system.linux.opengl.LinuxGLContext
import org.lwjgl.system.linux.GLFWLinux
import org.lwjgl.opengl.GL



object RiftExample {

  /**
   * Initializes libOVR and returns the Hmd instance
   */
  def initHmd(): Hmd = {

    // OvrLibrary.INSTANCE.ovr_Initialize() // is this actually still needed?
    Hmd.initialize()
    
    val hmd = 
      //Hmd.createDebug(ovrHmd_DK1)
      Hmd.create(0)
    if (hmd == null) {
      println("Oculus Rift HMD not found.")
      System.exit(-1)
    }
    
    // set hmd caps
    val hmdCaps = ovrHmdCap_LowPersistence | 
                  ovrHmdCap_NoVSync | 
                  ovrHmdCap_DynamicPrediction 
    hmd.setEnabledCaps(hmdCaps)
    
    hmd
  }
  
  
  /** Helper function used by initOpenGL */
  /*
  def setupContext(): ContextAttribs = {
    new ContextAttribs(3, 3)
    .withForwardCompatible(true)
    .withProfileCore(true)
    .withDebug(true)
  }
  
  /** Helper function used by initOpenGL */
 def setupDisplay(left: Int, top: Int, width: Int, height: Int) {
    Display.setDisplayMode(new DisplayMode(width, height));
    Display.setLocation(left, top)
    //Display.setLocation(0, 0)
    println(f"Creating window $width x $height @ x = $left, y = $top")
    //Display.setVSyncEnabled(true)
  }
  */
  
  /**
   * Initializes OpenGL
   * I first ran into some issues with an "invalid memory access" in configureRendering 
   * depending on how I initialize OpenGL (probably a context issue, but this was with the old SDK). 
   * To solve the issue I now initialize OpenGL similar to LwjglApp.run. 
   */  
  def initOpenGL(hmd: Hmd): Long = {
    
    glfwSetErrorCallback(ErrorCallback.Util.getDefault())
    
    if (glfwInit() != GL11.GL_TRUE) {
      throw new IllegalStateException("Unable to initialize GLFW")
    }
    
    glfwDefaultWindowHints()
    //glfwWindowHint(GLFW_VISIBLE, GL_FALSE);
    //glfwWindowHint(GLFW_RESIZABLE, GL_TRUE);
    
    val window = glfwCreateWindow(hmd.Resolution.w, hmd.Resolution.h, "Rift Example", NULL, NULL)
    if (window == NULL) {
      throw new RuntimeException("Failed to create the GLFW window")
    } 
    
    glfwMakeContextCurrent(window)
    
    /*
    //val display1 = GL.getFunctionProvider().getFunctionAddress("glXGetCurrentDisplay");
    val display2 = GLFWLinux.glfwGetX11Display()
    //val caps = GL.getCapabilities()
    //println(f"Display1: $display1")
    println(f"Display2: $display2")
    LinuxGLContext.createFromCurrent(display2)
    */
    
    glfwSwapInterval(1)
    glfwSetWindowPos(window, hmd.WindowsPos.x, hmd.WindowsPos.y)
    
    glfwShowWindow(window)
    /*
    // new initialization:
    if (true) {
      val glContext = new GLContext()
      val contextAttribs = setupContext
      
      // the problem is not the width/height of the window, other values do work...
      setupDisplay(hmd.WindowsPos.x, hmd.WindowsPos.y, hmd.Resolution.w, hmd.Resolution.h)
      
      // the following makes the difference: passing contextAttribs solves the "invalid memory access" issue, not passing it crashes
      // Display.create(new PixelFormat(/*Alpha Bits*/8, /*Depth bits*/ 8, /*Stencil bits*/ 0, /*samples*/8))
      Display.create(new PixelFormat(/*Alpha Bits*/8, /*Depth bits*/ 8, /*Stencil bits*/ 0, /*samples*/8), contextAttribs)
      Display.setVSyncEnabled(false)
      
      // the following three things do not seem to be the cause, can be commented out?
      GLContext.useContext(glContext, false)
      Mouse.create()
      Keyboard.create()    
    } else {
      // this is the old version that crashes (? or crashed) with an "invalid memory access" in configureRendering
      Display.setDisplayMode(new DisplayMode(1280, 800))
      Display.setVSyncEnabled(true)
      Display.create(new PixelFormat(/*Alpha Bits*/8, /*Depth bits*/ 8, /*Stencil bits*/ 0, /*samples*/8))     
    }
    println(f"OpenGL version: ${GL11.glGetString(GL11.GL_VERSION)}")
  */
    window
  }
  
  /**
   * Some general OpenGL state settings
   */
  def configureOpenGL() {
    GLContext.createFromCurrent()
    glClearColor(78/255f, 115/255f, 151/255f, 0.0f)

    glClearDepth(1.0f)
    glEnable(GL_DEPTH_TEST)
    glDepthFunc(GL_LEQUAL)

    glEnable(GL_CULL_FACE)
    glCullFace(GL_BACK) // which side should be suppressed? typically the "back" side

    glEnable(GL_BLEND)
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

    // for wire-frame:
    //glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)
    //glDisable(GL_CULL_FACE)
  }  
  

  /**
   * Generates the vertex data of the scene (multiple variants)
   */
  def generateSceneVertexData(scene: Int = 0): VertexData = {
    val approxHalfIpd = 0.064f / 2
    def linspace(min: Float, max: Float, numSteps: Int) = {
      min to max by (max-min)/(numSteps-1)
    }      
    scene match {
      case 0 => {
        val numBlocks = 10
        val dist = 1.5f
        val cubeOfCubes = for {
          x <- linspace(-dist, dist, numBlocks)
          y <- linspace(-dist, dist, numBlocks)
          z <- linspace(-dist, dist, numBlocks)
          if ((math.abs(x) max math.abs(y) max math.abs(z)) > 0.99*dist)
        } yield {
          val h = approxHalfIpd
          VertexDataGen3D_NC.cube(-h, +h, -h, +h, +h, -h, Color.COLOR_FERRARI_RED).transformSimple(Mat4f.translate(x, y, z))
        }
        cubeOfCubes.reduce(_ ++ _)
      }
      case 1 => {
        val numBlocks = 10
        val gridOfCubes = for {
          x <- linspace(-10, 10, numBlocks)
          y <- linspace(-10, 10, numBlocks)
          z <- linspace(-10, 10, numBlocks)
          if (!(math.abs(x) < 0.5 && math.abs(y) < 0.5 && math.abs(z) < 0.5))
        } yield {
          val h = approxHalfIpd
          VertexDataGen3D_NC.cube(-h, +h, -h, +h, +h, -h, Color.COLOR_CELESTIAL_BLUE).transformSimple(Mat4f.translate(x, y, z))
        }
        gridOfCubes.reduce(_ ++ _)
      }
    }
    
  }

  /**
   * Main
   */
  def main(args: Array[String]) {
    
    // initialize the Oculus Rift
    val hmd = initHmd() 
    
    // initialize and configure OpenGL
    val window = initOpenGL(hmd)
    configureOpenGL()
    
    // start tracking
    hmd.configureTracking(ovrTrackingCap_Orientation | ovrTrackingCap_Position | ovrTrackingCap_MagYawCorrection, 0)
    
    // prepare fovports
    val fovPorts = Array.tabulate(2)(eye => hmd.DefaultEyeFov(eye))
    val projections = Array.tabulate(2)(eye => Mat4f.createFromRowMajorArray(Hmd.getPerspectiveProjection(fovPorts(eye), 0.0001f, 10000f, true).M))
    
    val oversampling = 1.0f
    
    val eyeTextures = new Texture().toArray(2).asInstanceOf[Array[Texture]]
    Range(0, 2).foreach{ eye =>
      val header = eyeTextures(eye).Header
      header.TextureSize = hmd.getFovTextureSize(eye, fovPorts(eye), oversampling)
      header.RenderViewport.Size = header.TextureSize
      header.RenderViewport.Pos = new OvrVector2i(0, 0)
      header.API = OvrLibrary.ovrRenderAPIType.ovrRenderAPI_OpenGL
    }
    // the eyeTextures must be contiguous, since they are passed to endFrame
    checkContiguous(eyeTextures)
    
    
    val framebuffers = Array.tabulate(2){eye => 
      new FramebufferTexture(eyeTextures(eye).Header.TextureSize.w, eyeTextures(eye).Header.TextureSize.h)
      //new MultisampleFramebufferTexture(eyeTextures(eye).Header.TextureSize.w, eyeTextures(eye).Header.TextureSize.h, 4)
    }
    
    for (eye <- Range(0, 2)) {
      eyeTextures(eye).TextureId = framebuffers(eye).textureId
      println(f"Texture ID of eye $eye: ${eyeTextures(eye).TextureId}")
    }

    val rc = new RenderAPIConfig()
    rc.Header.API = OvrLibrary.ovrRenderAPIType.ovrRenderAPI_OpenGL
    rc.Header.RTSize = hmd.Resolution
    rc.Header.Multisample = 1 // does not seem to have any effect
    
    val distortionCaps = 
      //ovrDistortionCap_NoSwapBuffers |
      //ovrDistortionCap_FlipInput |
      //ovrDistortionCap_TimeWarp |
      ovrDistortionCap_Overdrive |
      ovrDistortionCap_HqDistortion |
      ovrDistortionCap_Chromatic | 
      ovrDistortionCap_Vignette
    
    // configure rendering
    GlWrapper.checkGlError("before configureRendering")
    val eyeRenderDescs = hmd.configureRendering(rc, distortionCaps, fovPorts)
    GlWrapper.checkGlError("after configureRendering")
    
    // hmdToEyeViewOffset is an Array[OvrVector3f] and is needed in the GetEyePoses call
    // we can prepare this here. Note: must be a contiguous structure
    val hmdToEyeViewOffsets = new OvrVector3f().toArray(2).asInstanceOf[Array[OvrVector3f]]
    Range(0, 2).foreach { eye =>
      hmdToEyeViewOffsets(eye).x = eyeRenderDescs(eye).HmdToEyeViewOffset.x
      hmdToEyeViewOffsets(eye).y = eyeRenderDescs(eye).HmdToEyeViewOffset.y
      hmdToEyeViewOffsets(eye).z = eyeRenderDescs(eye).HmdToEyeViewOffset.z
    }
    checkContiguous(hmdToEyeViewOffsets)
    
    
    // create vertex data + shader + VBO
    val vertexData = generateSceneVertexData(scene = 0)
    val shader = new DefaultLightingShader()
    
    val vbo = new StaticVbo(vertexData, shader)
    
    
    // mutable model/world transformation
    var modelR = Mat4f.createIdentity
    var modelS = Mat4f.createIdentity
    var modelT = Mat4f.translate(0, 0, -2)
    

    // nested function for handling a few keyboard controls
    def handleKeyboardInput(dt: Float) {
      val ds = 0.001f * dt   //   1 m/s
      val da = 0.09f  * dt   //  90 °/s
      val dS = 0.001f * dt   //   1 m/s
      /*
      import Keyboard._
      while (Keyboard.next()) {
        val (isKeyPress, key, char) = (Keyboard.getEventKeyState(), Keyboard.getEventKey(), Keyboard.getEventCharacter())
        if (isKeyPress) {
          key match {
            case KEY_F1 => // currently nothing
            case _ => {}
          }
        }
      } 
      if (Keyboard.isKeyDown(KEY_LEFT))   modelT = modelT.translate(-ds, 0, 0)
      if (Keyboard.isKeyDown(KEY_RIGHT))  modelT = modelT.translate(+ds, 0, 0)
      if (Keyboard.isKeyDown(KEY_UP))     modelT = modelT.translate(0, +ds, 0)
      if (Keyboard.isKeyDown(KEY_DOWN))   modelT = modelT.translate(0, -ds, 0)
      if (Keyboard.isKeyDown(KEY_PRIOR))  modelT = modelT.translate(0, 0, -ds)
      if (Keyboard.isKeyDown(KEY_NEXT))   modelT = modelT.translate(0, 0, +ds)

      if (Keyboard.isKeyDown(KEY_W))      modelR = modelR.rotateYawPitchRollQuaternions(-da, 0, 0, false)
      if (Keyboard.isKeyDown(KEY_S))      modelR = modelR.rotateYawPitchRollQuaternions(+da, 0, 0, false)
      if (Keyboard.isKeyDown(KEY_A))      modelR = modelR.rotateYawPitchRollQuaternions(0, 0, +da, false)
      if (Keyboard.isKeyDown(KEY_D))      modelR = modelR.rotateYawPitchRollQuaternions(0, 0, -da, false)
      if (Keyboard.isKeyDown(KEY_Q))      modelR = modelR.rotateYawPitchRollQuaternions(0, +da, 0, false)
      if (Keyboard.isKeyDown(KEY_E))      modelR = modelR.rotateYawPitchRollQuaternions(0, -da, 0, false)

      if (Keyboard.isKeyDown(KEY_U))      modelS = modelS.scale(1, 1, 1f+dS)
      if (Keyboard.isKeyDown(KEY_J))      modelS = modelS.scale(1, 1, 1f-dS)
      if (Keyboard.isKeyDown(KEY_H))      modelS = modelS.scale(1f+dS, 1, 1)
      if (Keyboard.isKeyDown(KEY_K))      modelS = modelS.scale(1f-dS, 1, 1)
      if (Keyboard.isKeyDown(KEY_Z))      modelS = modelS.scale(1, 1f+dS, 1)
      if (Keyboard.isKeyDown(KEY_I))      modelS = modelS.scale(1, 1f-dS, 1)
      */
    }
    
    // nested render function
    def render(P: Mat4f, V: Mat4f) {
      GlWrapper.clearColor.set(Color(1f, 1f, 1f, 1f))
      glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)
      
      GlWrapper.checkGlError("render -- before rendering the VBO")
      shader.use()
      shader.setProjection(P)
      shader.setModelview(V*modelT*modelR*modelS)
      vbo.render()
      
      GlWrapper.checkGlError("render -- finished")
    }


    // frame timing vars
    var numFrames = 0L
    val t1 = System.currentTimeMillis()
    var tL = t1
    
    val trackingLogger: Option[RiftTrackingLogger] = None // Some(new RiftTrackingLogger)
    
    // main loop:  
    while (glfwWindowShouldClose(window) == GL_FALSE) {
    
      val tN = System.currentTimeMillis()
      val dt = tN-tL
      tL = tN
      
      handleKeyboardInput(dt)

      GlWrapper.checkGlError("beginning of main loop")
      
      // deal with HSW
      val hswState = hmd.getHSWDisplayState()
      if (hswState.Displayed != 0) {
        hmd.dismissHSWDisplay()
      }
      
      // start frame timing
      val frameTiming = hmd.beginFrame(0 /*numFrames.toInt*/)
      
      trackingLogger.map(_.writeTrackingState(hmd, frameTiming))
      
      // get tracking by getEyePoses
      val headPoses = hmd.getEyePoses(0 /*numFrames.toInt*/, hmdToEyeViewOffsets)
      checkContiguous(headPoses)

      // get tracking manually
      val predictionTimePoint = frameTiming.ScanoutMidpointSeconds // + 0.002 // frameTiming.ScanoutMidpointSeconds
      val trackingState = hmd.getSensorState(predictionTimePoint)
      val manualHeadPoses = {
        val pose = trackingState.HeadPose.Pose
        val matPos = Mat4f.translate(pose.Position.x, pose.Position.y, pose.Position.z)
        val matOri = new Quaternion(pose.Orientation.x, pose.Orientation.y, pose.Orientation.z, pose.Orientation.w).castToOrientationMatrix // LH
        val euler = new Quaternion(pose.Orientation.x, pose.Orientation.y, pose.Orientation.z, pose.Orientation.w).toEuler()
        println(f"yaw = ${euler.yaw}%12.6f    pitch = ${euler.pitch}%12.6f    roll = ${euler.roll}%12.6f")
        val headPoses = new Posef().toArray(2).asInstanceOf[Array[Posef]]
        for (eye <- 0 until 2) {
          val matEye = Mat4f.translate(-eyeRenderDescs(eye).HmdToEyeViewOffset.x, -eyeRenderDescs(eye).HmdToEyeViewOffset.y, -eyeRenderDescs(eye).HmdToEyeViewOffset.z)
          val V = matPos * matOri * matEye // reverse transformation
          val origin = V * Vec4f(0,0,0,1)
          headPoses(eye).Position.x = origin.x
          headPoses(eye).Position.y = origin.y
          headPoses(eye).Position.z = origin.z
          headPoses(eye).Orientation.x = pose.Orientation.x
          headPoses(eye).Orientation.y = pose.Orientation.y
          headPoses(eye).Orientation.z = pose.Orientation.z
          headPoses(eye).Orientation.w = pose.Orientation.w
          //println(f"$eye    orig x = ${pose.Position.x}   new x = ${headPoses(eye).Position.x}    orig y = ${pose.Position.y}   new y = ${headPoses(eye).Position.y}    orig z = ${pose.Position.z}   new z = ${headPoses(eye).Position.z}")
        }
        headPoses
      }
      checkContiguous(manualHeadPoses)
      
      val headPosesToUse = manualHeadPoses
      
      val nextFrameDelta = (frameTiming.NextFrameSeconds-frameTiming.ThisFrameSeconds)*1000
      val scanoutMidpointDelta = (frameTiming.ScanoutMidpointSeconds-frameTiming.ThisFrameSeconds)*1000
      val timewarpDelta = (frameTiming.TimewarpPointSeconds-frameTiming.ThisFrameSeconds)*1000
      //println(f"delta = ${frameTiming.DeltaSeconds*1000}%9.3f thisFrame = ${frameTiming.ThisFrameSeconds*1000}%9.3f    nextFrameΔ = ${nextFrameDelta}%9.3f    timewarpΔ =  ${timewarpDelta}%9.3f    scanoutMidpointΔ = ${scanoutMidpointDelta}%9.3f")

      // now iterate eyes
      for (i <- 0 until 2) {
        val eye = hmd.EyeRenderOrder(i)
        val P = projections(eye)

        val pose = headPosesToUse(eye)
        
        //println(f"tracking position: x = ${pose.Position.x}%8.3f    y = ${pose.Position.y}%8.3f    z = ${pose.Position.z}%8.3f")

        //val trackingScale = 0.1f
        val matPos = Mat4f.translate(-pose.Position.x, -pose.Position.y, -pose.Position.z) //.scale(trackingScale, trackingScale, trackingScale)
        val matOri = new Quaternion(-pose.Orientation.x, -pose.Orientation.y, -pose.Orientation.z, pose.Orientation.w).castToOrientationMatrix // RH
        val V = matOri * matPos 
        
        // the old transformation was: matEye * matOri * matPos
        // the matEye correction is no longer needed, since the eye offset is now incorporated into pose.position
        // val matEye = Mat4f.translate(eyeRenderDescs(eye).HmdToEyeViewOffset.x, eyeRenderDescs(eye).HmdToEyeViewOffset.y, eyeRenderDescs(eye).HmdToEyeViewOffset.z)
        
        framebuffers(eye).activate()
        render(P, V)
        framebuffers(eye).deactivate()

        GlWrapper.checkGlError("after hmd.endEyeRender()")
      }
      
      GlWrapper.checkGlError("before hmd.endFrame()")
      hmd.endFrame(headPosesToUse, eyeTextures)
      GlWrapper.checkGlError("after hmd.endFrame()")

      glfwSwapBuffers(window);
      glfwPollEvents();
      numFrames += 1
    }

    val t2 = System.currentTimeMillis()
    println(f"\n *** average framerate: ${numFrames.toDouble / (t2-t1) * 1000}%.1f fps")

    trackingLogger.map(_.close())
    
    // destroy Hmd
    hmd.destroy()
    // OvrLibrary.INSTANCE.ovr_Shutdown() // apparently no longer required, causes buffer overflow
    println("Hmd destroyed")
    
    // destroy display
    glfwDestroyWindow(window)
    glfwTerminate()
    println("Display destroyed")
    
    System.exit(0)
  }
  
  
  private def checkContiguous[T <: Structure](ts: Array[T]) {
    val first = ts(0).getPointer
    val size = ts(0).size
    val secondCalc = first.getPointer(size)
    val secondActual = ts(1).getPointer.getPointer(0)
    assert(secondCalc == secondActual, "array must be contiguous in memory.")
  }
 
}



