import chisel3._
import chisel3.util._
import Core._

class ExecuteStage(Lanes: Int) extends Module {
  val io = IO(new Bundle {
    val x = Input(Vec(32,UInt(24.W)))
    val MemPort = new MemPort
    val Stall = Output(Bool())
    val Clear = Input(Bool())
  })
  val vio = IO(new Bundle {
    val vx = Input(Vec(8,Vec(16,UInt(24.W))))
    val len = Input(UInt(24.W))
  })
  val In = IO(new Bundle {
    val Type = Input(UInt(2.W))
    val rs1 = Input(UInt(5.W))
    val rs2 = Input(UInt(5.W))
    val rd = Input(UInt(5.W))
    val AImmediate = Input(UInt(11.W))
    val ASImmediate = Input(SInt(11.W))
    val AOperation = Input(UInt(4.W))
    val MemOp = Input(UInt(1.W))
    val MemAddress = Input(UInt(14.W))
    val COperation = Input(UInt(2.W))
    val COffset = Input(SInt(9.W))
  })
  val VectorIn = IO(new Bundle {
    val Type = Input(UInt(3.W))
    val vrs1 = Input(UInt(3.W))
    val vrs2 = Input(UInt(3.W))
    val rs = Input(UInt(5.W))
    val vrd = Input(UInt(3.W))
    val AImmediate = Input(UInt(12.W))
    val ASImmediate = Input(SInt(12.W))
    val AOperation = Input(UInt(4.W))
    val MemOp = Input(UInt(1.W))
    val MemAddress = Input(UInt(16.W))
  })
  val Out = IO(new Bundle {
    val WritebackMode = Output(UInt(4.W))
    val WritebackRegister = Output(UInt(5.W))
    val ALUOut = Output(UInt(24.W))
    val VALUOut = Output(Vec(16,UInt(24.W)))
    val JumpValue = Output(UInt(18.W))
  })


  // Init
  
  val ALU = Module(new ALU())
  val BranchComp = Module(new BranchComp())
  val VALU = Module(new VALU(Lanes))

  // Default

  io.MemPort.Enable := false.B
  io.MemPort.Address := 0.U
  io.MemPort.WriteEn := false.B
  io.MemPort.Len := 0.U

  for(i <- 0 until 16){
    io.MemPort.WriteData(i) := 0.U
  }

  io.Stall := false.B

  ALU.io.rs2 := 0.U
  ALU.io.rs1 := 0.U
  ALU.io.rd := 0.U
  ALU.io.Operation := 0.U

  for(i <- 0 until 16){
    VALU.io.vrs1(i) := 0.U
    VALU.io.vrs2(i) := 0.U
    VALU.io.vrd(i) := 0.U
  }

  VALU.io.len := vio.len
  VALU.io.Operation := 0.U
  VALU.io.rs := 0.U

  BranchComp.io.rs2 := 0.U
  BranchComp.io.rs1 := 0.U
  BranchComp.io.PC := 0.U
  BranchComp.io.Offset := 0.S
  BranchComp.io.Operation := 0.U

  val WritebackMode = RegInit(0.U(4.W))
  val WritebackRegister = RegInit(0.U(4.W))
  val ALUOutReg = RegInit(0.U(18.W))
  val COffsetReg = RegInit(0.U(6.W))

  //VALU Registers

  val VALUOutReg = Reg(Vec(16,UInt(24.W)))

  val DataHazard = RegInit(0.U(4.W))

  val rs1 = Wire(UInt(18.W))
  val rs2 = Wire(UInt(18.W))
  val rd = Wire(UInt(18.W))

  val swStall = RegInit(0.U(1.W))
  val lwStall = RegInit(0.U(1.W))

  // Data hazard protection

  rs1 := io.x(In.rs1)
  rs2 := io.x(In.rs2)
  rd := io.x(In.rd)

  // If the current instructions uses the result of a previous calculation, that isnt written to the register
  // the following code fetches the result from the internal pipeline registers

  when(In.rs1 === DataHazard){
    switch(WritebackMode){
      is(Arithmetic){
        rs1 := ALUOutReg
      }
      is(ImmidiateArithmetic){
        rs1 := ALUOutReg
      }
      is(MemoryI){
        rs1 := io.MemPort.ReadData(0)
      }
    }
  }

  when(In.rs2 === DataHazard){
    switch(WritebackMode){
      is(Arithmetic){
        rs2 := ALUOutReg
      }
      is(ImmidiateArithmetic){
        rs2 := ALUOutReg
      }
      is(MemoryI){
        rs2 := io.MemPort.ReadData(0)
      }
    }
  }

  when(In.rd === DataHazard){
    switch(WritebackMode){
      is(Arithmetic){
        rd := ALUOutReg
      }
      is(ImmidiateArithmetic){
        rd := ALUOutReg
      }
      is(MemoryI){
        rd := io.MemPort.ReadData(0)
      }
    }
  }

  Out.ALUOut := ALUOutReg
  Out.VALUOut := VALU.io.Out
  Out.WritebackMode := WritebackMode
  Out.WritebackRegister := WritebackRegister
  Out.JumpValue := COffsetReg

  ALUOutReg := ALU.io.Out

  // Clear overwrites registers

  when(io.Clear){
    ALUOutReg := 0.U
    WritebackMode := 0.U
    WritebackRegister := 0.U
    COffsetReg := 0.U
  }

  // Logic
  
  switch(In.Type){
    is(0.U){
      when(In.AOperation <= 10.U){
        ALU.io.Operation := In.AOperation

        ALU.io.rs2 := rs2
        ALU.io.rs1 := rs1

        WritebackMode := Arithmetic
        WritebackRegister := In.rd

        DataHazard := In.rd
      }.elsewhen(In.AOperation === 11.U){

        // mac (multiply accumalate takes 3 values)

        ALU.io.Operation := In.AOperation

        ALU.io.rs2 := rs2
        ALU.io.rs1 := rs1
        ALU.io.rd := rd

        WritebackMode := Arithmetic
        WritebackRegister := In.rd

        DataHazard := In.rd
      }.elsewhen(In.AOperation === 12.U){

        // lw rd, rs1

        io.MemPort.Enable := true.B 
        io.MemPort.Len := 1.U

        //io.MemPort.Address := io.x(In.rs1)

        // Was causing combinational loop 

        when(In.rs1 === DataHazard && In.rs1 =/= 0.U){
          switch(WritebackMode){
            is(Arithmetic){
              io.MemPort.Address := ALUOutReg
            }
            is(ImmidiateArithmetic){
              io.MemPort.Address := ALUOutReg
            }
          }
        }.otherwise{
          io.MemPort.Address := io.x(In.rs1)
        }

        WritebackMode := MemoryI
        WritebackRegister := In.rd

        DataHazard := In.rd

        when(!io.MemPort.Completed){
          io.Stall := true.B
        }
      }.elsewhen(In.AOperation === 13.U){

        // sw rd, rs1
 
        io.MemPort.Enable := true.B
        io.MemPort.WriteEn := true.B

        when(In.rs1 === DataHazard){
          switch(WritebackMode){
            is(Arithmetic){
              io.MemPort.Address := ALUOutReg
            }
            is(ImmidiateArithmetic){
              io.MemPort.Address := ALUOutReg
            }
          }
        }.otherwise{
          io.MemPort.Address := io.x(In.rs1)
        }

        io.MemPort.WriteData(0) := rd
        io.MemPort.Len := 1.U

        WritebackMode := Nil

        when(!io.MemPort.Completed){
          io.Stall := true.B
        }
      }
    }
    is(1.U){
      when(In.AOperation === 1.U){

        // li 

        ALU.io.rs2 := 0.U
        ALU.io.rs1 := In.AImmediate
        ALU.io.Operation := 0.U
      }.elsewhen(In.AOperation === 2.U){

        // lui 

        ALU.io.rs2 := 0.U

        val upper = Wire(UInt(12.W))
        upper := In.AImmediate
        val lower = Wire(UInt(12.W))
        //lower := io.x(I n.rd)(8,0)
        lower := rd(11,0)
        val cat = Wire(UInt(24.W))
        cat := Cat(upper,lower)

        ALU.io.rs1 := cat
        ALU.io.Operation := 0.U
      }.otherwise{
        when(In.ASImmediate < 0.S){
          ALU.io.Operation := 1.U
          ALU.io.rs2 := (0.S - In.ASImmediate).asUInt
          ALU.io.rs1 := rd

        }.otherwise{
          ALU.io.rs2 := In.ASImmediate.asUInt
          ALU.io.rs1 := rd
          ALU.io.Operation := 0.U
        }
      }
      WritebackMode := Arithmetic
      WritebackRegister := In.rd

      DataHazard := In.rd
    }
    is(2.U){
      io.MemPort.Address := In.MemAddress
      io.MemPort.WriteData(0) := rd
      io.MemPort.Enable := true.B
      io.MemPort.WriteEn := In.MemOp
      io.MemPort.Len := 1.U

      switch(In.MemOp){
        is(0.U){
          WritebackMode := MemoryI
          DataHazard := In.rd
        }
        is(1.U){
          WritebackMode := Nil
        }
      }

      when(!io.MemPort.Completed){
        io.Stall := true.B
      }

      WritebackRegister := In.rd
    }
    is(3.U){
      BranchComp.io.rs2 := rs2
      BranchComp.io.rs1 := rs1

      BranchComp.io.Operation := In.COperation
      BranchComp.io.PC := io.x(1) - 2.U
      BranchComp.io.Offset := In.COffset

      COffsetReg := BranchComp.io.Out

      when(BranchComp.io.CondCheck){
        WritebackMode := Conditional
      }.otherwise{
        WritebackMode := Nil
      }

      when(io.Clear){
        WritebackMode := Nil
      }

      WritebackRegister := 0.U
    }

    // Vector

    is(4.U){
      VALU.io.vrs1 := vio.vx(VectorIn.vrs1)
      VALU.io.vrs2 := vio.vx(VectorIn.vrs2)

      VALU.io.Operation := In.AOperation

      WritebackMode := vArithmetic
      WritebackRegister := VectorIn.vrd

      DataHazard := VectorIn.vrd

      when(!VALU.io.Completed){
        io.Stall := true.B
      }
    }
    is(5.U){
      VALU.io.vrs1 := vio.vx(VectorIn.vrs1)
      //
      for(i <- 0 until 16){
        VALU.io.vrs2(i) := VectorIn.AImmediate
      }

      VALU.io.Operation := In.AOperation

      WritebackMode := vArithmetic
      WritebackRegister := VectorIn.vrd

      DataHazard := VectorIn.vrd

      when(!VALU.io.Completed){
        io.Stall := true.B
      }
    }
    is(6.U){
      io.MemPort.Address := VectorIn.MemAddress
      io.MemPort.WriteData := vio.vx(VectorIn.vrs1)
      io.MemPort.Enable := true.B
      io.MemPort.WriteEn := In.MemOp
      io.MemPort.Len := vio.len

      switch(VectorIn.MemOp){
        is(0.U){
          WritebackMode := vMemoryI
          DataHazard := In.rd
        }
        is(1.U){
          WritebackMode := Nil
        }
      }

      when(!io.MemPort.Completed){
        io.Stall := true.B
      }

      WritebackRegister := In.rd

    }
    is(7.U){
      VALU.io.vrs1 := vio.vx(VectorIn.vrs1)

      for(i <- 0 until 16){
        VALU.io.vrs2(i) := io.x(VectorIn.rs)
      }

      //VALU.io.vrs2 := vio.vx(VectorIn.vrs2)

      VALU.io.Operation := In.AOperation

      WritebackMode := vArithmetic
      WritebackRegister := VectorIn.vrd

      DataHazard := VectorIn.vrd

      when(!VALU.io.Completed){
        io.Stall := true.B
      }


    }
  }
}