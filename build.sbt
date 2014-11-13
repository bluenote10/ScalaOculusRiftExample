name := "ScalaOculusRiftExample"

version := "0.1"

scalaVersion := "2.10.4"

mainClass := Some("com.github.bluenote.RiftExample")


// --------------------------------
// Library dependencies:
// --------------------------------

libraryDependencies += "org.saintandreas" % "jovr" % "0.4.3.0"


// --------------------------------
// Fork settings:
// --------------------------------

fork in run := true

javaOptions in run += "-Djava.library.path=" + Seq({ if (System.getProperty("os.name").startsWith("Windows")) "lib\\native\\windows" else "lib/native/linux" } ).mkString(java.io.File.pathSeparator)

javaOptions in run += "-Dorg.lwjgl.opengl.Window.undecorated=true"

// disable sbt's prefix of output when forking: http://stackoverflow.com/questions/14504572/sbt-suppressing-logging-prefix-in-stdout
outputStrategy in run := Some(StdoutOutput)


// --------------------------------
// Experiments with GC settings
// --------------------------------

// javaOptions in run += "-XX:+UseParallelGC" // peaks less deterministic, sometimes 3 sec between peaks, but 50 ms frame delta

// javaOptions in run += "-XX:+UseParallelOldGC" // similar to -XX:+UseParallelGC

// javaOptions in run += "-XX:+UseSerialGC" // peaks slightly more frequent that with UseConcMarkSweepGC, but delta mainly below 10 ms

javaOptions in run += "-XX:+UseConcMarkSweepGC" 

javaOptions in run += "-XX:-UseParNewGC"


