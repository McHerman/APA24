import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
//import chiseltest.experimental.TestOptionBuilder._
//import chiseltest.internal.WriteVcdAnnotation._
import scala.xml._
import org.scalatest.FlatSpec
//import Sounds._
//import Assembler._
import chisel3.experimental._
import chisel3.util._
//import PrintFiles._
import java.io.File
import java.util.Arrays;

class SampleTest_Verilator extends AnyFlatSpec with ChiselScalatestTester {

  val xml = XML.loadFile("config/APA24.xml")
  
  behavior of "APA24"

  /*

  it should "play" in {
    test(new APA24(100000000,xml)).withAnnotations(Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)) { dut =>
    //test(new DSP(100000000,xml)) { dut =>
      
      // no timeout, as a bunch of 0 samples would lead to a timeout.
      dut.clock.setTimeout(0)
      // Write the samples

      for(i <- 0 until 1000){
        dut.clock.step(1)
      }

      // Uncomment for direct playback
      //startPlayer
      //playArray(outSamples)      
      //stopPlayer

    }
  }

  */

  "Test " should "pass" in {
    test(new APA24(100000000,xml)).withAnnotations(Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)) { dut =>
      dut.clock.setTimeout(0)

      for(i <- 0 until 1000){
        dut.clock.step(1)
      }
    }
  }
}