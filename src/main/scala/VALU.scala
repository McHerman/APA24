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

class VALU(Lanes: Int, VectorRegisterLength: Int) extends Module {
  val io = IO(new Bundle {
    val en = Input(Bool())
    val vrs1 = Input(Vec(VectorRegisterLength,UInt(24.W)))
    val vrs2 = Input(Vec(VectorRegisterLength,UInt(24.W)))
    val vrd = Input(Vec(VectorRegisterLength,UInt(24.W)))

    val len = Input(UInt(5.W))

    val rs = Input(UInt(24.W))

    val Operation = Input(UInt(8.W))

    val Completed = Output(Bool())
    val Out = Output(Vec(VectorRegisterLength,UInt(24.W)))
  })


  //Defaults

  val LaneIO = Wire(Vec(Lanes, new ALU_IO))

  val OutReg = Reg(Vec(VectorRegisterLength,UInt(24.W)))

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


  val CntReg = RegInit(0.U(6.W))

  when(CntReg === io.len){
    CntReg := 0.U
    io.Completed := true.B
  } 

  when(io.en){
    when((io.len - CntReg) >= Lanes.U){
      for(i <- 0 until Lanes){
        val index = Wire(UInt(24.W))
        index := CntReg + i.U

        LaneIO(i.U).rs1 := io.vrs1(index)
        LaneIO(i).rs2 := io.vrs2(index)
        OutReg(index) := LaneIO(i).Out

        LaneIO(i).Operation := io.Operation

        CntReg := CntReg + Lanes.U
      }
    }.otherwise{
      for(i <- 0 until Lanes){
        switch(io.len - CntReg){
          is(i.U){
            for(j <- 0 until i){
              LaneIO(j).rs1 := io.vrs1(CntReg + j.U)
              LaneIO(j).rs2 := io.vrs2(CntReg + j.U)
              OutReg(i.U + CntReg) := LaneIO(j).Out 

              LaneIO(i).Operation := io.Operation

              CntReg := CntReg + i.U

            }
          }
        }
      }
    } 

    /*

    for(i <- 0 until VectorRegisterLength){
      switch(io.len){
        is(i.U){
          for(j <- i until VectorRegisterLength){
              OutReg(j.U) := 0.U 
          }
        }
      }
    }

    */
  }  
}
