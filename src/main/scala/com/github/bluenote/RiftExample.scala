package com.github.bluenote

import org.lwjgl.input.Keyboard
import org.lwjgl.input.Keyboard._
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.ContextAttribs
import org.lwjgl.opengl.Display
import org.lwjgl.opengl.DisplayMode
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GLContext
import org.lwjgl.opengl.PixelFormat

import com.oculusvr.capi.Hmd
import com.oculusvr.capi.OvrLibrary
import com.oculusvr.capi.OvrLibrary.ovrDistortionCaps._
import com.oculusvr.capi.OvrLibrary.ovrTrackingCaps._
import com.oculusvr.capi.OvrVector2i
import com.oculusvr.capi.OvrVector3f
import com.oculusvr.capi.Posef
import com.oculusvr.capi.RenderAPIConfig
import com.oculusvr.capi.Texture
import com.sun.jna.Structure

//import VertexDataGenVNC._


object RiftExample {

  def setupContext(): ContextAttribs = {
    new ContextAttribs(3, 3)
    .withForwardCompatible(true)
    .withProfileCore(true)
    .withDebug(true)
  }
  
 def setupDisplay(left: Int, top: Int, width: Int, height: Int) {
    Display.setDisplayMode(new DisplayMode(width, height));
    Display.setLocation(left, top)
    //Display.setLocation(0, 0)
    println(f"Creating window $width x $height @ x = $left, y = $top")
    //Display.setVSyncEnabled(true)
  }
 
  def setupDisplayOld() {
  }
 
  def initGL() {
    glClearColor(78/255f, 115/255f, 151/255f, 0.0f)

    glClearDepth(1.0f);
    glEnable(GL_DEPTH_TEST);
    glDepthFunc(GL_LEQUAL);

    glEnable(GL_CULL_FACE)
    glCullFace(GL_BACK) // which side should be suppressed? typically the "back" side

    glEnable(GL_BLEND)
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

  }  
  
  
  def main(args: Array[String]) {
    
    // load Oculus Rift
    //OvrLibrary.INSTANCE.ovr_Initialize()
    Hmd.initialize()
    Thread.sleep(400);
    
    val hmd = 
      //Hmd.createDebug(ovrHmd_DK1)
      Hmd.create(0)
    if (hmd == null) {
      println("Oculus Rift HMD not found.")
      System.exit(-1)
    }
    
    hmd.setEnabledCaps(OvrLibrary.ovrHmdCaps.ovrHmdCap_LowPersistence | OvrLibrary.ovrHmdCaps.ovrHmdCap_NoVSync)
    //val hmdDesc = hmd.getDesc()

    // order copied from LwjglApp.run
    if (true) {
      val glContext = new GLContext()
      val contextAttribs = setupContext
      
      // the problem is not the width/height of the window, other values do work...
      setupDisplay(hmd.WindowsPos.x, hmd.WindowsPos.y, hmd.Resolution.w, hmd.Resolution.h)
      
      // the following makes the differes: passing contextAttribs solves the "invalid memory access" issue, not passing it crashes
      // Display.create(new PixelFormat(/*Alpha Bits*/8, /*Depth bits*/ 8, /*Stencil bits*/ 0, /*samples*/8))
      Display.create(new PixelFormat(/*Alpha Bits*/8, /*Depth bits*/ 8, /*Stencil bits*/ 0, /*samples*/8), contextAttribs)
      Display.setVSyncEnabled(false)
      
      // the following three things do not seem to be the cause, can be commented out
      GLContext.useContext(glContext, false)
      Mouse.create()
      Keyboard.create()    
    } else {
      // this is the old version that crashes with an "invalid memory access" in configureRendering
      Display.setDisplayMode(new DisplayMode(1280, 800))
      Display.setVSyncEnabled(true)
      Display.create(new PixelFormat(/*Alpha Bits*/8, /*Depth bits*/ 8, /*Stencil bits*/ 0, /*samples*/8))     
    }
    println(f"OpenGL version: ${GL11.glGetString(GL11.GL_VERSION)}")
    initGL()
    
    // start tracking
    // hmd.startSensor(ovrSensorCap_Orientation | ovrSensorCap_Position | ovrSensorCap_YawCorrection, ovrSensorCap_YawCorrection)
    //hmd.startSensor(ovrSensorCap_Orientation, 0)
    hmd.configureTracking(ovrTrackingCap_Orientation | ovrTrackingCap_Position, 0)
    
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
    /*Array.tabulate(2){ eye =>
      val texture = new Texture()
      val header = texture.Header
      header.TextureSize = hmd.getFovTextureSize(eye, fovPorts(eye), oversampling)
      header.RenderViewport.Size = header.TextureSize
      header.RenderViewport.Pos = new OvrVector2i(0, 0)
      header.API = OvrLibrary.ovrRenderAPIType.ovrRenderAPI_OpenGL
      texture
    }
    */
    
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
      //ovrDistortionCap_TimeWarp |
      ovrDistortionCap_Chromatic | 
      ovrDistortionCap_Vignette
    
    GlWrapper.checkGlError("before configureRendering")
    val eyeRenderDescs = hmd.configureRendering(rc, distortionCaps, fovPorts)
    GlWrapper.checkGlError("after configureRendering")
    
    
    def linspace(min: Float, max: Float, numSteps: Int) = {
      min to max by (max-min)/(numSteps-1)
    }      
    val numBlocks = 10
    val gridOfCubes = for {
      x <- linspace(-10, 10, numBlocks)
      y <- linspace(-10, 10, numBlocks)
      z <- linspace(-10, 10, numBlocks)
      if (!(math.abs(x) < 0.5 && math.abs(y) < 0.5 && math.abs(z) < 0.5))
    } yield {
      val ipd = 0.0325f
      VertexDataGen3D_NC.cube(-ipd, +ipd, -ipd, +ipd, +ipd, -ipd, Color.COLOR_CELESTIAL_BLUE).transformSimple(Mat4f.translate(x, y, z))
    }
    val dist = 3
    val cubeOfCubes = for {
      x <- linspace(-dist, dist, numBlocks)
      y <- linspace(-dist, dist, numBlocks)
      z <- linspace(-dist, dist, numBlocks)
      if ((math.abs(x) max math.abs(y) max math.abs(z)) > 0.99*dist)
    } yield {
      val ipd = 0.0325f * 2
      VertexDataGen3D_NC.cube(-ipd, +ipd, -ipd, +ipd, +ipd, -ipd, Color.COLOR_FERRARI_RED).transformSimple(Mat4f.translate(x, y, z))
    }
      
    
    val shader = new DefaultLightingShader()
    
    val vbo = 
      //new VboCube(0, 0, 0, 1, 1, 1)
      //new GenericVBOTrianglesFixedPipeline(GenericVBOTriangles.cubeVNC(-1, +1, -1, +1, +1, -1, Color.COLOR_CELESTIAL_BLUE))
      //new GenericVBOTrianglesFixedPipeline(GenericVBOTriangles.cylinderVNC(1, 1, Color.COLOR_FERRARI_RED, 32))
      //new GenericVBOTrianglesFixedPipeline(GenericVBOTriangles.sphereVNC(1, Color.COLOR_FERRARI_RED, 4))
      //new StaticVboGaussianShaderVNC(VertexDataGenVNC.sphereVNC(1, Color.COLOR_FERRARI_RED, 4))
      //new StaticVboGaussianShaderVNC(VertexDataGenVNC.roundedCubeVNC(1, 2, 3, 0.2f, Color.COLOR_FERRARI_RED, 8))
      new StaticVbo(cubeOfCubes.reduce(_ ++ _), shader)
    
    var renderMode = 'direct
    var modelR = Mat4f.createIdentity
    var modelS = Mat4f.createIdentity
    var modelT = Mat4f.translate(0, 0, -5)
    
    var numFrames = 0L
    val t1 = System.currentTimeMillis()
    var tL = t1


    
    def render(P: Mat4f, V: Mat4f) {
      //glClearColor(0.2f, 0.2f, 0.2f, 1)
      glClearColor(1f, 1f, 1f, 1)
      glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)
    
      //glDisable(GL_TEXTURE_2D)
      
      //glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)
      //glDisable(GL_CULL_FACE)
      
      //glTranslatef(0, 0, -5)
      //Color.glColorWrapper(Color.COLOR_FERRARI_RED)
      //drawBox(-1, +1, -1, +1, -1, +1)
      GlWrapper.checkGlError("before rendering of the cube")
      shader.use()
      shader.setProjection(P)
      shader.setModelview(V*modelT*modelR*modelS)
      vbo.render()
      
      //glPolygonMode(GL_FRONT_AND_BACK, GL_FILL)

      /*
      org.lwjgl.opengl.GL20.glUseProgram(0)
      glMatrixMode(GL_PROJECTION)
      glLoadMatrix(P.asFloatBuffer)

      glMatrixMode(GL_MODELVIEW);
      glLoadMatrix(V.asFloatBuffer)

      glMultMatrix((modelT*modelR).asFloatBuffer)
      val numblocks = 21
      for (x <- linspace(-10, 10, numblocks)) {
        for (y <- linspace(-10, 10, numblocks)) {
          for (z <- linspace(-10, 10, numblocks)) {
            if (!(math.abs(x) < 0.5 && math.abs(y) < 0.5 && math.abs(z) < 0.5)) {
              glPushMatrix()
              glTranslatef(x.toFloat, y.toFloat, z.toFloat)
              //glRotatef(time.toFloat / 1000 / 1000 / 10 * x.toFloat * y.toFloat + z.toFloat, 0.0f, 1.0f, 0.0f)
              glColor4f(100.0f/255, 50.0f/255, 20.0f/255, 1.0f)
              //glColor4f(Random.nextFloat, Random.nextFloat, Random.nextFloat, 1.0f)
              //new glu.Cylinder().draw(0.02f, 0.02f, 10, 4, 4)
              val ipd = 0.0325f
              drawBox(-ipd, +ipd, -ipd, +ipd, -ipd, +ipd)
              //cube.draw()
              glPopMatrix()
            }
          }
        }
      }
      */
      
    }

   
    while (!Display.isCloseRequested()) {
    
      val tN = System.currentTimeMillis()
      val dt = tN-tL
      tL = tN

      val ds = 0.001f * dt   //   1 m/s
      val da = 0.09f  * dt   //  90 °/s
      val dS = 0.001f * dt   //   1 m/s
      
      import Keyboard._
      while (Keyboard.next()) {
        val (isKeyPress, key, char) = (Keyboard.getEventKeyState(), Keyboard.getEventKey(), Keyboard.getEventCharacter())
        if (isKeyPress) {
          key match {
            case KEY_F1 => renderMode = 'direct
            case KEY_F2 => renderMode = 'framebuffer
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

      // get orientation quaternion
      /*
      oculusRift.poll();
      val yaw   = oculusRift.getYawDegrees_LH()
      val pitch = oculusRift.getPitchDegrees_LH()
      val roll  = oculusRift.getRollDegrees_LH()
      val V = Mat4f.rotateYawPitchRollQuaternions(yaw, pitch, roll)
      */
      
      GlWrapper.checkGlError()
      
      val frameTiming = hmd.beginFrame(numFrames.toInt)
      //val headPoses = for (i <- 0 until 2) yield {
      
      val hmdToEyeViewOffsets = new OvrVector3f().toArray(2).asInstanceOf[Array[OvrVector3f]]
      Range(0, 2).foreach { eye =>
        hmdToEyeViewOffsets(eye).x = eyeRenderDescs(eye).HmdToEyeViewOffset.x
        hmdToEyeViewOffsets(eye).y = eyeRenderDescs(eye).HmdToEyeViewOffset.y
        hmdToEyeViewOffsets(eye).z = eyeRenderDescs(eye).HmdToEyeViewOffset.z
      }
      /*
      val hmdToEyeViewOffsets = eyeRenderDescs.map{eyeRenderDesc =>
        val v = new OvrVector3f
        v.x = eyeRenderDesc.HmdToEyeViewOffset.x
        v.y = eyeRenderDesc.HmdToEyeViewOffset.y
        v.z = eyeRenderDesc.HmdToEyeViewOffset.z
        v
      }
      */
      //val headPoses = hmd.getEyePoses(numFrames.toInt, eyeRenderDescs.map(_.HmdToEyeViewOffset))
      val headPoses = hmd.getEyePoses(numFrames.toInt, hmdToEyeViewOffsets)
      val headPosesCont = new Posef().toArray(2).asInstanceOf[Array[Posef]]
      headPosesCont(0) = headPoses(0)
      headPosesCont(1) = headPoses(1)
      
      
      val eyeTexturesCont = new Texture().toArray(2).asInstanceOf[Array[Texture]]
      /*Range(0, 2).foreach { eye =>
        eyeTexturesCont(eye).TextureId = eyeTextures(eye).TextureId
        eyeTexturesCont(eye).Padding = eyeTextures(eye).
      }*/
      eyeTexturesCont(0) = eyeTextures(0)
      eyeTexturesCont(1) = eyeTextures(1)
      
      checkContiguous(hmdToEyeViewOffsets)
      checkContiguous(headPoses)
      checkContiguous(eyeTextures)
      checkContiguous(headPosesCont)
      checkContiguous(eyeTexturesCont)
      
      for (i <- 0 until 2) yield {
        val eye = hmd.EyeRenderOrder(i)
        val P = projections(eye)

        val pose = //hmd.getEyePose(eye)
          headPosesCont(eye)
        
        val matPos = Mat4f.translate(-pose.Position.x, -pose.Position.y, -pose.Position.z)
        val matOri = new Quaternion(-pose.Orientation.x, -pose.Orientation.y, -pose.Orientation.z, pose.Orientation.w).castToOrientationMatrix // RH
        val matEye = Mat4f.translate(eyeRenderDescs(eye).HmdToEyeViewOffset.x, eyeRenderDescs(eye).HmdToEyeViewOffset.y, eyeRenderDescs(eye).HmdToEyeViewOffset.z)
        val V = matEye * matOri * matPos
        //val V = matOri
        //val V = matEye
        
        framebuffers(eye).activate()
        render(P, V)
        framebuffers(eye).deactivate()

        //hmd.endEyeRender(eye, pose, eyeTextures(eye))
        GlWrapper.checkGlError("after hmd.endEyeRender()")
        
        //pose
      }
      
      GlWrapper.checkGlError("before hmd.endFrame()")
      hmd.endFrame(headPoses, eyeTexturesCont)
      GlWrapper.checkGlError("after hmd.endFrame()", true) // this seems to fail

      Display.update()
      //Display.swapBuffers()
      //Display.sync(120)
      numFrames += 1
    }

    val t2 = System.currentTimeMillis()
    println(f"\n *** average framerate: ${numFrames.toDouble / (t2-t1) * 1000}%.1f fps")

    //hmd.stopSensor()
    println("sensor stopped")
    hmd.destroy()
    //OvrLibrary.INSTANCE.ovr_Shutdown()
    println("hmd destroyed")
    Display.destroy()
    println("display destroyed")
    
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



