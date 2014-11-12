package com.github.bluenote

import com.oculusvr.capi.Hmd
import com.oculusvr.capi.FrameTiming
import com.oculusvr.capi.Posef
import scala.collection.immutable.ListMap


/**
 * Utility class to write the Rift's tracking data to CSV
 */
class RiftTrackingLogger() {

  
  
  val predictionPoints = ListMap[String, FrameTiming => Double](
    "past"                -> (frameTiming => frameTiming.ThisFrameSeconds - 1.0),
    "thisFrame"           -> (frameTiming => frameTiming.ThisFrameSeconds),
    "thisFrame + 20 ms"   -> (frameTiming => frameTiming.ThisFrameSeconds + 0.02),
    "thisFrame + 100 ms"  -> (frameTiming => frameTiming.ThisFrameSeconds + 0.1),
    "ScanoutMidpoint"     -> (frameTiming => frameTiming.ScanoutMidpointSeconds)
  )

  val output = GeneralUtils.outputFile("trackingLog.csv")
  
  output.println(("time" +: predictionPoints.keys.toList).mkString(";"))
  
  def writeTrackingState(hmd: Hmd, frameTiming: FrameTiming) {
    
    val orientations = for (predictionTimePoint <- predictionPoints.values) yield {
      val trackingState = hmd.getSensorState(predictionTimePoint(frameTiming))
      val pose = trackingState.HeadPose.Pose
      
      //val matOri = new Quaternion(pose.Orientation.x, pose.Orientation.y, pose.Orientation.z, pose.Orientation.w).castToOrientationMatrix // LH
      val orientation = new Quaternion(pose.Orientation.x, pose.Orientation.y, pose.Orientation.z, pose.Orientation.w).toEuler
      orientation
    }
    
    output.println(f"${frameTiming.ThisFrameSeconds};${orientations.map(_.yaw).mkString(";")}")
  }
  
  def close() {
    output.close
  }
  
}