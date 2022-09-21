import chisel3._
import chisel3.util._
import chisel3.util.experimental.loadMemoryFromFileInline
import scala.io.Source
import java.io.File
import java.nio.file.{Files, Paths}

import chisel3._
import chisel3.util.experimental.loadMemoryFromFileInline
import chisel3.experimental.{annotate, ChiselAnnotation}
import firrtl.annotations.MemorySynthInit

class FetchStage(Program: String) extends Module {
  val io = IO(new Bundle{
    val Stall = Input(Bool())
    val Clear = Input(Bool())
    val WriteEn = Input(Bool())
    val WriteData = Input(UInt(24.W))
  })
  val In = IO(new Bundle{
    val PC = Input(UInt(24.W))
  })
  val Out = IO(new Bundle{
    val Instruction = Output(UInt(24.W))
  })

  // Init

  val ClearDelay = RegNext(io.Clear)

  /*

  val InstructionMem = Module(new InstuctionMemory(Program))

  doNotDedup(InstructionMem)

  InstructionMem.io.Address := 0.U
  InstructionMem.io.DataIn := 0.U
  InstructionMem.io.enable := 0.U
  InstructionMem.io.write := false.B

  // Logic

  InstructionMem.io.Address := In.PC
  InstructionMem.io.DataIn := 0.U
  InstructionMem.io.enable := true.B

  Out.Instruction := InstructionMem.io.Instruction

  when(io.Clear) {
    InstructionMem.io.enable := false.B
  }

  */

  val OutReg = RegInit(0.U(24.W))

  annotate(new ChiselAnnotation {
    override def toFirrtl =
      MemorySynthInit
  })

  val mem = Mem(1024, UInt(24.W))
  if (Program.trim().nonEmpty) {
    loadMemoryFromFileInline(mem, Program)
  }
  Out.Instruction := DontCare
  when(!io.Stall) {
    val rdwrPort = mem(In.PC)
    when (io.WriteEn) { rdwrPort := io.WriteData }
      .otherwise    { OutReg := rdwrPort }
  }

  Out.Instruction := OutReg  

}