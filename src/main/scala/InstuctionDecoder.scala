import chisel3._
import chisel3.util._

class InstuctionDecoder() extends Module {
  val io = IO(new Bundle {
    val Instruction = Input(UInt(24.W))

    val Type = Output(UInt(3.W))
    val rs1 = Output(UInt(5.W))
    val rs2 = Output(UInt(5.W))

    val rd = Output(UInt(5.W))

    val AImmidiate = Output(UInt(13.W))
    val ASImmidiate = Output(SInt(13.W))

    val AOperation = Output(UInt(4.W))

    val MemOp = Output(UInt(1.W))
    val MemAdress = Output(UInt(15.W))

    val COperation = Output(UInt(2.W))
    val COffset = Output(SInt(9.W))
  })

  val VectorIO = IO(new Bundle {
    val Type = Output(UInt(3.W))
    val vrs1 = Output(UInt(3.W))
    val vrs2 = Output(UInt(3.W))

    val rs = Output(UInt(5.W))

    val vrd = Output(UInt(3.W))

    val AImmediate = Output(UInt(12.W))
    val ASImmediate = Output(SInt(12.W))

    val AOperation = Output(UInt(4.W))

    val MemOp = Output(UInt(2.W))
    val MemAddress = Output(UInt(16.W))
  })

  io.Type := io.Instruction(23,21)
  io.rs1 := 0.U
  io.rs2 := 0.U
  io.rd := 0.U
  io.AImmidiate := 0.U
  io.ASImmidiate := 0.S
  io.AOperation := 0.U
  io.MemOp := 0.U
  io.MemAdress := 0.U
  io.COperation := 0.U
  io.COffset := 0.S

  VectorIO.Type := io.Instruction(23,21)
  VectorIO.vrd := 0.U
  VectorIO.vrs1 := 0.U
  VectorIO.vrs2 := 0.U
  VectorIO.rs := 0.U
  VectorIO.AImmediate := 0.U
  VectorIO.ASImmediate := 0.S
  VectorIO.AOperation := 0.U
  VectorIO.MemOp := 0.U
  VectorIO.MemAddress := 0.U

  switch(io.Instruction(23,21)){
    is(0.U){
      io.AOperation := io.Instruction(20,17)
      io.rd := io.Instruction(16,12)
      io.rs1 := io.Instruction(11,7)
      io.rs2 := io.Instruction(6,2)
    }
    is(1.U){
      io.AOperation := io.Instruction(20,18)
      io.rd := io.Instruction(17,13)
      io.AImmidiate := io.Instruction(12,0)
      io.ASImmidiate := io.Instruction(12,0).asSInt
    }

    //TODO fix scalar mem-access

    is(2.U){
      io.MemOp := io.Instruction(20)
      io.rd := io.Instruction(19,15)
      io.MemAdress := io.Instruction(14,0)
    } 

    is(3.U){
      io.COperation := io.Instruction(20,19)
      io.rs1 := io.Instruction(18,14)
      io.rs2 := io.Instruction(13,9)
      io.COffset := io.Instruction(8,0).asSInt
    }

    // Vector operations

    is(4.U){
      VectorIO.AOperation := io.Instruction(20,17)
      VectorIO.vrd := io.Instruction(16,14)
      VectorIO.vrs1 := io.Instruction(13,11)
      VectorIO.vrs2 := io.Instruction(12,8)
    }
    is(5.U){
      VectorIO.AOperation := io.Instruction(20,17)
      VectorIO.vrd := io.Instruction(16,14)
      VectorIO.AImmediate := io.Instruction(13,2)
      VectorIO.ASImmediate := io.Instruction(13,2).asSInt
    }
    is(6.U){
      VectorIO.MemOp := io.Instruction(20,19)
      VectorIO.vrd := io.Instruction(18,16)
      VectorIO.MemAddress := io.Instruction(15,0)
    }
    is(7.U){
      VectorIO.AOperation := io.Instruction(20,17)
      VectorIO.vrd := io.Instruction(16,14)
      VectorIO.vrs1 := io.Instruction(13,11)
      //VectorIO.vrs2 := io.Instruction(12,8)
      VectorIO.rs := io.Instruction(10,6)
    }
  }
}