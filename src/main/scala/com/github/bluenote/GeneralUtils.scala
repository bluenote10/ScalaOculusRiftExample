package com.github.bluenote

import java.io.PrintStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.File


object GeneralUtils {
  
  def loadFile(filename: String): String = {
    try {
      scala.io.Source.fromFile(filename, "utf-8").getLines.mkString("\n")
    } catch {
      case e: Throwable => println("Exception while reading file:\n" + e); ""
    }
  }
  
  // --------------------------------------------------
  // Output Stuff
  // --------------------------------------------------
  
  def outputStdOut: PrintStream = System.out
  
  def outputFile(file: File): PrintStream = new PrintStream(new FileOutputStream(file))
  
  def outputFile(filename: String): PrintStream = new PrintStream(new FileOutputStream(filename))
  
  def outputDummy: PrintStream = new PrintStream(new OutputStream() {
    override def close() {}
    override def flush() {}
    override def write(b: Array[Byte]) {}
    override def write(b: Array[Byte], off: Int, len: Int) {}
    override def write(b: Int) {}
  })  
}

