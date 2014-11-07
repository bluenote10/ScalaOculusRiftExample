package com.github.bluenote


object GeneralUtils {
  
  def loadFile(filename: String): String = {
    try {
      scala.io.Source.fromFile(filename, "utf-8").getLines.mkString("\n")
    } catch {
      case e: Throwable => println("Exception while reading file:\n" + e); ""
    }
  }
  
}

