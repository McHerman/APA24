import chisel3._
import chisel3.util._
import chisel3.experimental.Analog
import chisel3.util.experimental.loadMemoryFromFile

class CacheMemory(Memports: Int, CacheSize: Int, VectorRegisterLength: Int) extends Module {
  val io = IO(new Bundle {
    val MemPort = Vec(Memports,Flipped(new MemPort(VectorRegisterLength)))
    val SPIMemPort = new MemPort(VectorRegisterLength)
    val Taken = Output(Bool())
  })

  // Module Definitions

  //val Memory = SyncReadMem(Memsize, UInt(24.W))

  val Cache = Module(new Cache(CacheSize, VectorRegisterLength))
  //val SPIArbiter = Module(new SPIArbiter(1))

  // Defaults

  for(i <- 0 until Memports){
    for(j <- 0 until VectorRegisterLength){
      io.MemPort(i).ReadData(j) := 0.U
    }

    io.MemPort(i).Completed := false.B
    io.MemPort(i).ReadValid := false.B
  
  }

  io.SPIMemPort <> Cache.io.EXT

  val Producer = Wire(UInt(2.W))
  val ProducerReg = RegInit(0.U(2.W))
  val Taken = RegInit(0.U(1.W))

  io.Taken := false.B

  when(Taken.asBool && Producer =/= 0.U){
    io.Taken := true.B
  }

  //io.Taken := Taken.asBool

  Producer := ProducerReg

  for(i <- 0 until Memports){
    when(!Taken.asBool && io.MemPort(i).Enable){
      Taken := true.B
      Producer := i.U
      ProducerReg := i.U
      //CntReg := 0.U
    }
  }

  io.MemPort(Producer) <> Cache.io.MemPort

}
