### ScalaOculusRiftExample

This project is a simple Oculus Rift example based on JOVR. A big thanks to [Brad Davis](https://github.com/jherico) for maintaining the Java bindings of LibOVR.

All Rift related code is in [this file](src/main/scala/com/github/bluenote/RiftExample.scala). The rest of the code mainly provides very basic shader + VBO handling, and GLM-like math. The demo scene is just a bunch of cubes hovering in front of the user. The code is not optimized but runs at ~500 fps on my system. Unfortunately, I get a very strong judder under both Linux and Windows -- I'm still trying to find out what may cause this.


#### Getting Started

Simply run `sbt run`. Should run with both SBT 0.12 and 0.13.

Screen Setup: I have only tested the code in "extended mode". Under Linux I currently assume that the Rift display is rotated (1920x1080 and not 1080x1920) like under Windows. I know that this is not in line with the recommendations of Oculus' Linux README but can hardly be the cause of the judder. 

Keyboard Shortcuts: To keep things simple, there is currently only very basic keybaord control. You can move the world/model with the curser keys (model moves up, down, left, right), page up/down (model moves forward/backward), and W/A/S/D (model rotates).


#### Getting Started for Non-Scala Users

If you want to try out the example but do not have/know Scala: No problem. All you need is a JVM and [SBT](http://www.scala-sbt.org/) (Simple Build Tool, the equivalent to Maven in the Scala world). You can simply download the SBT launcher (basically a JAR and a start script `sbt` which launches the JAR), run `sbt` in the project folder, and type `run` within the SBT shell. This will download and compile Scala, download the JOVR library, compile the project, and run it.
