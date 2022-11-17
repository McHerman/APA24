import chisel3._
import chisel3.util._
import chisel3.experimental.Analog
import chisel3.util.experimental.loadMemoryFromFile

class SPIArbiter(Memports: Int, VectorRegisterLength: Int) extends Module {
  val io = IO(new Bundle {
    val MemPort = Vec(Memports,Flipped(new MemPort(VectorRegisterLength)))
  })
  val SPI = IO(new Bundle{
    val SCLK = Output(Bool())
    val CE = Output(Bool())
    val SO = Input(Vec(4,Bool()))
    val SI = Output(Vec(4,Bool()))
    val Drive = Output(Bool())
  })

  // Defaults

  val ExternalMemory = Module(new MemoryController(0,VectorRegisterLength))

  for(i <- 0 until Memports){
    for(j <- 0 until VectorRegisterLength){
      io.MemPort(i).ReadData(j) := 0.U
    }

    io.MemPort(i).Completed := false.B
    io.MemPort(i).ReadValid := false.B
    io.MemPort(i).Ready := false.B

    //io.MemPort(i).Ready := ExternalMemory.io.Ready
  }

  for(i <- 0 until VectorRegisterLength){
    ExternalMemory.io.MemPort.WriteData(i) := 0.U  
  }

  ExternalMemory.io.MemPort.Enable := false.B
  ExternalMemory.io.MemPort.WriteEn := false.B
  ExternalMemory.io.MemPort.Address := 0.U
  ExternalMemory.io.MemPort.Len := 0.U
  ExternalMemory.SPI <> SPI

  val Producer = Wire(UInt(2.W))
  val ProducerReg = RegInit(0.U(2.W))
  val Taken = RegInit(0.U(1.W))

  Producer := ProducerReg

  // Arbiter 

  for(i <- 0 until Memports){
    when(!Taken.asBool && io.MemPort(i).Enable){
      Taken := true.B
      Producer := i.U
      ProducerReg := i.U
    }
  }

  when(ExternalMemory.io.MemPort.Completed === true.B){
    Taken := 0.U
  }

  // SPI module controller

  when(io.MemPort(Producer).Enable){
    SPI <> io.MemPort(Producer)
  }

  /*

  when(io.MemPort(Producer).Enable){
    ExternalMemory.io.Address := io.MemPort(Producer).Address

    when(io.MemPort(Producer).WriteEn){
      when(ExternalMemory.io.Ready){
        ExternalMemory.io.WriteEnable := true.B
        ExternalMemory.io.WriteData := io.MemPort(Producer).WriteData
        ExternalMemory.io.Len := io.MemPort(Producer).Len
      }

      io.MemPort(Producer).Completed := ExternalMemory.io.Completed
    }.otherwise{
      when(ExternalMemory.io.Ready){
        ExternalMemory.io.ReadEnable := true.B
      }

      ExternalMemory.io.Len := io.MemPort(Producer).Len

      io.MemPort(Producer).Completed := ExternalMemory.io.Completed
      io.MemPort(Producer).ReadData := ExternalMemory.io.ReadData
      io.MemPort(Producer).ReadValid := ExternalMemory.io.Completed
    }
  }

  */
}