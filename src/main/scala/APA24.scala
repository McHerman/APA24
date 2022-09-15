import chisel3._
import chisel3.experimental._
import chisel3.util._
import scala.xml._
import java.io._
import java.io.File
import java.util.Arrays;
import Assembler._

class MemPort extends Bundle{
  val Address = Output(UInt(24.W))
  val WriteData = Output(Vec(16,UInt(24.W)))
  val Enable = Output(Bool())
  val Len = Output(UInt(5.W))
  val WriteEn = Output(Bool())

  val ReadData = Input(Vec(16,UInt(24.W)))
  val Completed = Input(Bool())
  val ReadValid = Input(Bool())
}

class CAP_IO extends Bundle{
  val In = Input(UInt(24.W))
  val Out = Output(UInt(24.W))
}

object Text{
    val name = """
               #      db      `7MMPPPMq.   db                          
               #     ;MM:       MM   `MM. ;MM:                         
               #    ,V^MM.      MM   ,M9 ,V^MM.     pd*"*b.      ,AM   
               #   ,M  `MM      MMmmdM9 ,M  `MM    (O)   j8     AVMM   
               #   AbmmmqMA     MM      AbmmmqMA       ,;j9   ,W' MM   
               #  A'     VML    MM     A'     VML   ,-='    ,W'   MM   
               #.AMA.   .AMMA..JMML. .AMA.   .AMMA.Ammmmmmm AmmmmmMMmm 
               #                                                  MM   
               #                                                  MM 
               #""".stripMargin('#')
}

class APA24(maxCount: Int, xml: scala.xml.Elem) extends Module {

  val Program = (xml \\ "Core" \ "Program").text
  var Memsize = (xml \\ "Core" \\ "BRAM" \\ "@size").text.toInt
  var SPIRAM_Offset = (xml \\ "Core" \\ "DRAM" \\ "@offset").text.toInt  
  var Lanes = (xml \\ "Core" \\ "VALU" \\ "@lanes").text.toInt



  val io = IO(new Bundle {
    //val Sub_IO = new CAP_IO
    val In = Input(UInt(24.W))
    val Out = Output(UInt(24.W))
  })
  val SPI = IO(new Bundle {
    val SPIMemPort = new MemPort
  })

  val dedupBlock = WireInit(Program.hashCode.S)

  // Single Core

  replace_pseudo(Program);
  demangle_identifiers(Program);
  read_assembly(Program);


  val Core = Module(new Core("Programs/MachineCode/" + Program + ".mem", Lanes, Memsize))
  val DataMemory = Module(new DataMemory(1, Memsize, SPIRAM_Offset))

  // IO

  Core.io.WaveIn := io.In
  io.Out := Core.io.WaveOut


  // Interconnections

  Core.io.MemPort <> DataMemory.io.MemPort(0)
  Core.io.MemTaken := DataMemory.io.Taken

  SPI.SPIMemPort <> DataMemory.io.SPIMemPort
}

// generate Verilog
object DSP extends App {

  //val Config = args(0)

  val Config = "config/APA24.xml"

  println(Text.name + "\n")

  val xml = XML.loadFile(Config)

  (new chisel3.stage.ChiselStage).emitVerilog(new APA24(100000000, xml))
}


