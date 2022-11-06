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
  var Lanes = (xml \\ "Core" \\ "VALU" \\ "@lanes").text.toInt
  var HasCache = (xml \\ "Core" \\ "Memory" \\ "Cache" \\ "@hasCache").text.toBoolean
  var CacheSize = (xml \\ "Core" \\ "Memory" \\ "Cache" \\ "Size" \\"@bit").text.toInt

  var Memsize = (xml \\ "Core" \\ "Memory" \\ "BRAM" \\ "@size").text.toInt
  var SPIRAM_Offset = (xml \\ "Core" \\ "Memory" \\ "DRAM" \\ "@offset").text.toInt 
  

  val io = IO(new Bundle {
    //val Sub_IO = new CAP_IO
    val In = Input(UInt(24.W))
    val Out = Output(UInt(24.W))
  })
  val SPI_Out = IO(new Bundle{
    val SCLK = Output(Bool())
    val CE = Output(Bool())
    val SO = Input(Vec(4,Bool()))
    val SI = Output(Vec(4,Bool()))
    val Drive = Output(Bool())
  })

  val dedupBlock = WireInit(Program.hashCode.S)

  // Single Core

  replace_pseudo(Program);
  demangle_identifiers(Program);
  read_assembly(Program);


  val Core = Module(new Core("Programs/MachineCode/" + Program + ".mem", Lanes, Memsize))
  val SPI = Module(new SPIArbiter(1))


  if(HasCache){
    val Cache = Module(new CacheMemory(1,CacheSize))

    Core.io.MemPort <> Cache.io.MemPort(0)
    Core.io.MemTaken := false.B

    SPI.io.MemPort(0) <> Cache.io.SPIMemPort
  }else{
    val DataMemory = Module(new DataMemory(1, Memsize, SPIRAM_Offset))
    
    Core.io.MemPort <> DataMemory.io.MemPort(0)
    Core.io.MemTaken := DataMemory.io.Taken

    SPI.io.MemPort(0) <> DataMemory.io.SPIMemPort
  }

  // IO

  Core.io.WaveIn := io.In
  io.Out := Core.io.WaveOut

  // Interconnections

  SPI_Out <> SPI.SPI
  
}

// generate Verilog
object DSP extends App {

  //val Config = args(0)

  val Config = "config/APA24ex.xml"

  println(Text.name + "\n")

  val xml = XML.loadFile(Config)
  (new chisel3.stage.ChiselStage).emitVerilog(new APA24(100000000, xml))
}


