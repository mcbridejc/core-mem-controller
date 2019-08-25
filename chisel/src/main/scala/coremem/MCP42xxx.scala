package coremem

import chisel3._
import chisel3.util._

class MCP42xxx extends Module {
  val io = IO(new Bundle {
    // Rising edge on write begins update sequence to potentiometer
    val write = Input(Bool())

    // Goes high on clock after write is asserted, and remains high until transfer is complete
    val busy = Output(Bool())

    val enable0 = Input(Bool())
    val enable1 = Input(Bool())
    val data0 = Input(UInt(8.W))
    val data1 = Input(UInt(8.W))

    val sclk = Output(Bool())
    val csn = Output(Bool())
    val mosi = Output(Bool())
  })

  val sIdle :: sWriteA :: sWaitA :: sWriteB :: sWaitB :: Nil = Enum(5)

  val state = RegInit(sIdle)

  val spiMaster = Module(new SpiMasterIF(16, 40))

  val cmdSelect = Wire(UInt(4.W))
  val chanSelect = Wire(UInt(4.W))
  
  // when(state === sWriteA) {
  //   chanSelect := 1.U
  //   when(io.enable0) {
  //     cmdSelect := 1.U
  //   } otherwise {
  //     cmdSelect := 2.U
  //   }
  //   spiMaster.io.dataIn := Cat(cmdSelect, chanSelect, io.data0)
  //   spiMaster.io.start := true.B
  // } elsewhen(state === sWaitA) {
  //   chanSelect := 2.U
  //   when(io.enable1) {
  //     cmdSelect := 1.U
  //   } otherwise {
  //     cmdSelect := 2.U
  //   }
  //   spiMaster.io.dataIn := Cat(cmdSelect, chanSelect, io.data1)
  //   spiMaster.io.start := true.B
  // } otherwise {
  //   spiMaster.io.dataIn := 0.U
  //   spiMaster.io.start := false.B
  // }


  chanSelect := 0.U
  cmdSelect := 0.U
  spiMaster.io.dataIn := 0.U
  spiMaster.io.start := false.B
  io.sclk := spiMaster.io.sclk 
  io.csn := spiMaster.io.csn
  io.mosi := spiMaster.io.mosi

  io.busy := (state =/= sIdle)

  switch(state) {

    is(sIdle) {
      when(io.write && ~RegNext(io.write)) {
        state := sWriteA
      } 
    }
    is(sWriteA) {
      state := sWaitA 
      chanSelect := 1.U
      when(io.enable0) {
        cmdSelect := 1.U
      } otherwise {
        cmdSelect := 2.U
      }
      spiMaster.io.dataIn := Cat(cmdSelect, chanSelect, io.data0)
      spiMaster.io.start := true.B
    }
    is(sWaitA) {
      when(~spiMaster.io.active) {
        state := sWriteB
      }
    }
    is(sWriteB) {
      state := sWaitB
      chanSelect := 2.U
      when(io.enable1) {
        cmdSelect := 1.U
      } otherwise {
        cmdSelect := 2.U
      }
      spiMaster.io.dataIn := Cat(cmdSelect, chanSelect, io.data1)
      spiMaster.io.start := true.B
    }
    is(sWaitB) {
      when(~spiMaster.io.active) {
        state := sIdle
      }
    }
  }
}
