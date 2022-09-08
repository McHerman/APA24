import chisel3._
import chisel3.util._

class DecodeStage extends Module {
  val io = IO(new Bundle{
    val Clear = Input(Bool())
    val Stall = Input(Bool())
    val MiniStall = Output(Bool())
  })
  val In = IO(new Bundle {
    val Instruction = Input(UInt(24.W))
  })
  val Out = IO(new Bundle {
    val Type = Output(UInt(3.W))
    val rs1 = Output(UInt(4.W))
    val rs2 = Output(UInt(4.W))
    val rd = Output(UInt(4.W))
    val AImmediate = Output(UInt(11.W))
    val ASImmediate = Output(SInt(11.W))
    val AOperation = Output(UInt(4.W))
    val MemOp = Output(UInt(1.W))
    val MemAddress = Output(UInt(11.W))
    val COperation = Output(UInt(2.W))
    val COffset = Output(SInt(6.W))
  })
  val VectorOut = IO(new Bundle {
    val Type = Output(UInt(3.W))
    val vrs1 = Output(UInt(3.W))
    val vrs2 = Output(UInt(3.W))
    val rs = Output(UInt(5.W))
    val vrd = Output(UInt(3.W))
    val AImmediate = Output(UInt(12.W))
    val ASImmediate = Output(SInt(12.W))
    val AOperation = Output(UInt(4.W))
    val MemOp = Output(UInt(2.W))
    val MemAddress = Output(UInt(15.W))
  })

  // Init

  io.MiniStall := false.B

  val InstDec = Module(new InstuctionDecoder())

  val AddressReg = RegInit(0.U(10.W))
  val TypeReg = RegInit(0.U(3.W))
  val rs1Reg = RegInit(0.U(5.W))
  val rs2Reg = RegInit(0.U(5.W))
  val rdReg = RegInit(0.U(5.W))

  val AImmediateReg = RegInit(0.U(11.W))
  val ASImmediateReg = RegInit(0.S(11.W))

  val AOperationReg = RegInit(0.U(4.W))

  val MemOpReg = RegInit(0.U(1.W))
  val MemAddressReg = RegInit(0.U(11.W))

  val COperationReg = RegInit(0.U(2.W))
  val COffsetReg = RegInit(0.S(6.W))

  // Vector 

  val VectorTypeReg = RegInit(0.U(3.W))
  val VectorRs1Reg = RegInit(0.U(4.W))
  val VectorRs2Reg = RegInit(0.U(4.W))
  val vr_rsReg = RegInit(0.U(5.W))
  val VectorRdReg = RegInit(0.U(4.W))
  val VectorAImmediateReg = RegInit(0.U(12.W))
  val VectorASImmidiateReg = RegInit(0.S(12.W))
  val VectorAOperationReg = RegInit(0.U(4.W))
  val VectorMemOpReg = RegInit(0.U(2.W))
  val VectorMemAddressReg = RegInit(0.U(15.W))


  // Logic

  InstDec.io.Instruction := In.Instruction

  Out.Type := TypeReg
  Out.rs1 := rs1Reg
  Out.rs2 := rs2Reg
  Out.rd := rdReg

  Out.AImmediate := AImmediateReg
  Out.ASImmediate:= ASImmediateReg

  Out.AOperation := AOperationReg

  Out.MemOp := MemOpReg
  Out.MemAddress := MemAddressReg

  Out.COperation := COperationReg
  Out.COffset := COffsetReg

  //Vector

  VectorOut.Type := VectorTypeReg
  VectorOut.vrs1 := VectorRs1Reg
  VectorOut.vrs2 := VectorRs2Reg
  VectorOut.rs := vr_rsReg
  VectorOut.vrd := VectorRdReg

  VectorOut.AImmediate := VectorAImmediateReg
  VectorOut.ASImmediate:= VectorASImmidiateReg

  VectorOut.AOperation := VectorAOperationReg

  VectorOut.MemOp := VectorMemOpReg
  VectorOut.MemAddress := VectorMemAddressReg

 
  when(!io.Stall){
    TypeReg := InstDec.io.Type
    rs1Reg := InstDec.io.rs1
    rs2Reg := InstDec.io.rs2
    rdReg := InstDec.io.rd

    AImmediateReg := InstDec.io.AImmidiate
    ASImmediateReg := InstDec.io.ASImmidiate

    AOperationReg := InstDec.io.AOperation

    MemOpReg := InstDec.io.MemOp
    MemAddressReg := InstDec.io.MemAdress

    COperationReg := InstDec.io.COperation
    COffsetReg := InstDec.io.COffset

    //Vector 

    VectorTypeReg := InstDec.VectorIO.Type
    VectorRs1Reg := InstDec.VectorIO.vrs1
    VectorRs2Reg := InstDec.VectorIO.vrs2
    vr_rsReg := InstDec.VectorIO.rs
    VectorRdReg := InstDec.VectorIO.vrd

    VectorAImmediateReg := InstDec.VectorIO.AImmediate
    VectorASImmidiateReg := InstDec.VectorIO.ASImmediate

    VectorAOperationReg := InstDec.VectorIO.AOperation

    VectorMemOpReg := InstDec.VectorIO.MemOp
    VectorMemAddressReg := InstDec.VectorIO.MemAddress
  }

  when(io.Clear){
    InstDec.io.Instruction := 0.U

    TypeReg := 0.U
    rs1Reg := 0.U
    rs2Reg := 0.U
    rdReg := 0.U

    AImmediateReg := 0.U
    ASImmediateReg := 0.S

    AOperationReg := 0.U

    MemOpReg := 0.U
    MemAddressReg := 0.U

    COperationReg := 0.U
    COffsetReg := 0.S

    Out.Type := 0.U
    Out.rs1 := 0.U
    Out.rs2 := 0.U
    Out.rd := 0.U

    Out.AImmediate := 0.U
    Out.ASImmediate:= 0.S

    Out.AOperation := 0.U

    Out.MemOp := 0.U
    Out.MemAddress := 0.U

    Out.COperation := 0.U
    Out.COffset := 0.S

    //Vector 

    VectorTypeReg := 0.U
    VectorRs1Reg := 0.U
    VectorRs2Reg := 0.U
    vr_rsReg := 0.U
    VectorRdReg := 0.U

    VectorAImmediateReg := 0.U
    VectorASImmidiateReg := 0.S

    VectorAOperationReg := 0.U

    VectorMemOpReg := 0.U
    VectorMemAddressReg := 0.U
  }

  // Inserts bubble in pipeline in case of memory access. 

  when(InstDec.io.AOperation === 9.U || InstDec.io.AOperation === 10.U || InstDec.io.Type === 3.U || (InstDec.io.Type > 3.U)){
    io.MiniStall := true.B
  }
  
}