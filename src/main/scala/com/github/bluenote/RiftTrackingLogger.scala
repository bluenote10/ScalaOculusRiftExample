package com.github.bluenote

import com.oculusvr.capi.Hmd
import com.oculusvr.capi.FrameTiming
import com.oculusvr.capi.Posef


/**
 * Utility class to write the Rift's tracking data to CSV
 */
class RiftTrackingLogger() {

  val outputs = Map(
    "past"    -> GeneralUtils.outputFile("trackingLog1.csv"),
    "present" -> GeneralUtils.outputFile("trackingLog2.csv"),
    "future"  -> GeneralUtils.outputFile("trackingLog3.csv")
  )
  
  def writeTrackingState(hmd: Hmd, frameTiming: FrameTiming) {
    val predictionPoints = 
      (outputs("past"),    frameTiming.ThisFrameSeconds - 1) ::
      (outputs("present"), frameTiming.ThisFrameSeconds) :: 
      (outputs("future"),  frameTiming.ThisFrameSeconds + 0.05) ::
      Nil
    for ((output, predictionTimePoint) <- predictionPoints) {
      val trackingState = hmd.getSensorState(predictionTimePoint)
      val pose = trackingState.HeadPose.Pose
      
      //val matOri = new Quaternion(pose.Orientation.x, pose.Orientation.y, pose.Orientation.z, pose.Orientation.w).castToOrientationMatrix // LH
      val orientation = new Quaternion(pose.Orientation.x, pose.Orientation.y, pose.Orientation.z, pose.Orientation.w).toEuler
      output.println(f"${frameTiming.ThisFrameSeconds};${orientation.yaw}")
      
    }
  }
  
  def close() {
    outputs.values.foreach(_.close)
  }
  
}