import chisel3._
import chisel3.util._
import chisel3.experimental.Analog
import chisel3.util.experimental.loadMemoryFromFile

class DataMemory(Memports: Int, Memsize: Int, SPIRAM_Offset: Int) extends Module {
  val io = IO(new Bundle {
    val MemPort = Vec(Memports,Flipped(new MemPort))
    val SPIMemPort = new MemPort
  })

  // Module Definitions

  val Memory = SyncReadMem(Memsize, UInt(24.W))

  // Defaults

  val CompleteDelayInternal = RegInit(0.U(1.W))
  val CompleteDelayRegister = RegInit(0.U(1.W))
  //val CompleteDelayExternal = RegInit(0.U(1.W))

  for(i <- 0 until Memports){
    for(j <- 0 until 16){
      io.MemPort(i).ReadData(j) := 0.U
    }

    io.MemPort(i).Completed := false.B
  }

  io.SPIMemPort.Enable := false.B
  io.SPIMemPort.Address := 0.U

  for(i <- 0 until 16){
    io.SPIMemPort.WriteData(i) := 0.U
  }
  
  io.SPIMemPort.WriteEn := false.B
  io.SPIMemPort.Len := 0.U

  val Producer = Wire(UInt(2.W))
  val ProducerReg = RegInit(0.U(2.W))
  val Taken = RegInit(0.U(1.W))

  Producer := ProducerReg

  for(i <- 0 until Memports){
    when(!Taken.asBool && io.MemPort(i).Enable){
      Taken := true.B
      Producer := i.U
      ProducerReg := i.U
    }
  }

  when(CompleteDelayInternal.asBool || CompleteDelayRegister.asBool){
    CompleteDelayInternal := false.B
    CompleteDelayInternal := false.B
    io.MemPort(Producer).Completed := true.B
    Taken := 0.U
  }

  // Address space partition

  //ReadInputSample := Memory.read(SampleAdress)

  when(io.MemPort(Producer).Enable){
    when(io.MemPort(Producer).Address <= Memsize.U || CompleteDelayInternal.asBool){ // Internal data memory
      val rdwrPort = Memory(io.MemPort(Producer).Address)
      when (io.MemPort(Producer).WriteEn) {
        rdwrPort := io.MemPort(Producer).WriteData(0)
        CompleteDelayInternal := true.B
      }
      .otherwise{
        io.MemPort(Producer).ReadData(0) := rdwrPort(0)
        CompleteDelayInternal := true.B
      }
  
    }.otherwise{ // External Memory

      io.MemPort(Producer).ReadData := io.SPIMemPort.ReadData
      io.MemPort(Producer).Completed := io.SPIMemPort.Completed

      io.SPIMemPort.Address := io.MemPort(Producer).Address + Memsize.U
      io.SPIMemPort.WriteData := io.MemPort(Producer).WriteData
      io.SPIMemPort.Enable := io.MemPort(Producer).Enable
      io.SPIMemPort.WriteEn := io.MemPort(Producer).WriteEn
      io.SPIMemPort.Len := io.MemPort(Producer).Len

    }
  }
}
