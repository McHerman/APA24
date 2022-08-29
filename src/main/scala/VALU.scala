import chisel3._
import chisel3.util._

// ALU module is used by the execute stage for ALU operations

class ALU_IO extends Bundle{
  val rs1 = Input(UInt(24.W))
  val rs2 = Input(UInt(24.W))
  val rd = Input(UInt(24.W))

  val Operation = Input(UInt(8.W))
  val Out = Output(UInt(24.W))
}

class VALU(Lanes: Int) extends Module {
  val io = IO(new Bundle {
    val vrs1 = Input(Vec(16,UInt(24.W)))
    val vrs2 = Input(Vec(16,UInt(24.W)))
    val vrd = Input(Vec(16,UInt(24.W)))

    val len = Input(UInt(4.W))

    val rs = Input(UInt(24.W))

    val Operation = Input(UInt(8.W))

    val Completed = Output(Bool())
    val Out = Output(Vec(16,UInt(24.W)))
  })


  //Defaults

  val LaneIO = Wire(Vec(Lanes, new ALU_IO))

  val OutReg = Reg(Vec(16,UInt(24.W)))

  io.Out := OutReg
  io.Completed := false.B

  for(i <- 0 until Lanes){
    val ALU = Module(new ALU)
    ALU.io <> LaneIO(i)

    LaneIO(i).rs1 := 0.U
    LaneIO(i).rs2 := 0.U
    LaneIO(i).rd := 0.U

    LaneIO(i).Operation := 0.U
  }


  val CntReg = RegInit(0.U(4.W))

  when(CntReg === (io.len - 1.U)){
    CntReg := 0.U
    io.Completed := true.B
  } 

  when(((io.len - 1.U) - CntReg) >= Lanes.U){
    for(i <- 0 until Lanes){
      LaneIO(i).rs1 := io.vrs1(i)
      LaneIO(i).rs2 := io.vrs2(i)
      OutReg(i.U + CntReg) := LaneIO(i).rd 

      LaneIO(i).Operation := io.Operation

      CntReg := CntReg + Lanes.U
    }
  }.otherwise{
    /*
    switch((io.len - CntReg)){
      for(i <- 0 until 15){
        is(i.U){
          for(j <- 0 until i){
            LaneIO(j).rs1 := io.vrs1(j)
            LaneIO(j).rs2 := io.vrs2(j)
            OutReg(i.U + CntReg) := LaneIO(j).rd  

            io.vrd(j) := LaneIO(j).rd

            LaneIO(i).Operation := io.Operation

            CntReg := CntReg + i.U

          }
        }
      }
    }
    */

    for(i <- 0 until Lanes){
      switch((io.len - 1.U) - CntReg){
        is(i.U){
          for(j <- 0 until i){
            LaneIO(j).rs1 := io.vrs1(j)
            LaneIO(j).rs2 := io.vrs2(j)
            OutReg(i.U + CntReg) := LaneIO(j).rd  

            LaneIO(i).Operation := io.Operation

            CntReg := CntReg + i.U

          }
        }
      }
    }


  } 
}
