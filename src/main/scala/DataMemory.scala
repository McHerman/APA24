import chisel3._
import chisel3.util._
import chisel3.experimental.Analog
import chisel3.util.experimental.loadMemoryFromFile

class DataMemory(Memports: Int, Memsize: Int, VectorRegisterLength: Int, SPIRAM_Offset: Int) extends Module {
  val io = IO(new Bundle {
    val MemPort = Vec(Memports,Flipped(new MemPort(VectorRegisterLength)))
    val SPIMemPort = new MemPort(VectorRegisterLength)
    val Taken = Output(Bool())
  })

  // Module Definitions

  val Memory = SyncReadMem(Memsize, UInt(24.W))

  // Defaults

  val CompleteDelayInternal = RegInit(0.U(1.W))
  val CompleteDelayRegister = RegInit(0.U(1.W))
  //val CompleteDelayExternal = RegInit(0.U(1.W))

  val CntReg = RegInit(0.U(5.W))
  val StorageReg = Reg(Vec(VectorRegisterLength,UInt(24.W)))

  for(i <- 0 until Memports){
    for(j <- 0 until VectorRegisterLength){
      io.MemPort(i).ReadData(j) := 0.U
    }

    io.MemPort(i).Completed := false.B
    io.MemPort(i).ReadValid := false.B
  
  }

  io.SPIMemPort.Enable := false.B
  io.SPIMemPort.Address := 0.U

  for(i <- 0 until VectorRegisterLength){
    io.SPIMemPort.WriteData(i) := 0.U
  }
  
  io.SPIMemPort.WriteEn := false.B
  io.SPIMemPort.Len := 0.U

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
      CntReg := 0.U
    }
  }

  val rdwrPort = Memory(io.MemPort(Producer).Address + CntReg)

  // Address space partition

  //ReadInputSample := Memory.read(SampleAdress)

  when(io.MemPort(Producer).Enable){
    when(io.MemPort(Producer).Address <= Memsize.U || CompleteDelayInternal.asBool){ // Internal data memory
      when (io.MemPort(Producer).WriteEn) {
        rdwrPort := io.MemPort(Producer).WriteData(CntReg)
        //CompleteDelayInternal := true.B
      }.otherwise{
        //io.MemPort(Producer).ReadData(0) := rdwrPort(0)

        when(CntReg > 0.U){
          StorageReg(CntReg - 1.U) := rdwrPort
        }

        //StorageReg(CntReg - 1.U) := rdwrPort
        //CompleteDelayInternal := true.
      }

      when(CntReg === (io.MemPort(Producer).Len - 1.U)){
        CompleteDelayInternal := true.B
      }.otherwise{
        CntReg := CntReg + 1.U
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


  when(CompleteDelayInternal.asBool || CompleteDelayRegister.asBool){
    CompleteDelayInternal := false.B
    CompleteDelayInternal := false.B
    io.MemPort(Producer).Completed := true.B
    io.MemPort(Producer).ReadValid := true.B

    CntReg := 0.U

    when(io.MemPort(Producer).Len > 1.U){
      for(i <- 0 to VectorRegisterLength){
        switch(io.MemPort(Producer).Len){
          is(i.U){
            for(j <- 0 until (i - 1)){
              io.MemPort(Producer).ReadData(j) := StorageReg(j)
            }

            io.MemPort(Producer).ReadData((i.U - 1.U)) := rdwrPort
          }
        }
      }
    }.otherwise(
      io.MemPort(Producer).ReadData(0) := rdwrPort
    )

    Taken := 0.U
  }
}
