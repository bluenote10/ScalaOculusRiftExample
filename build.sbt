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

javaOptions in run += "-XX:+UseConcMarkSweepGC"

// disable sbt's prefix of output when forking: http://stackoverflow.com/questions/14504572/sbt-suppressing-logging-prefix-in-stdout
outputStrategy in run := Some(StdoutOutput)

