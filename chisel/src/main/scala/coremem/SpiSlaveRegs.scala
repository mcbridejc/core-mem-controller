package coremem

import chisel3._
import chisel3.util._

/* Implements the registers and SPI control signals

This connects to the SpiSlaveIF module and provides simple outputs for the 
controller state machine to use. 
*/
class SpiSlaveRegs() extends Module {
    
  val io = IO(new Bundle {
    // Signals from SpiSlaveIF
    val WR = Input(Bool())
    val RD = Input(Bool())
    val WRDATA = Input(UInt(8.W))
    val RDDATA = Output(UInt(8.W))
    val ADDR = Input(UInt(7.W))

    // Outputs
    val SENSEDELAY = Output(UInt(8.W))
    val VTHRESH = Output(UInt(16.W))
    val CMD_DATA = Output(UInt(8.W))
    val CMD_ADDR = Output(UInt(8.W))
    val CMD_WRITE = Output(Bool())
    val CMD_READ = Output(Bool())
    val CMD_UPDATEPOT = Output(Bool())
  })

  val SenseDelayAddr = 0.U
  val CtrlAddr = 1.U
  val VThreshHAddr = 2.U
  val VThreshLAddr = 3.U
  val VDriveHAddr = 4.U
  val VDriveLAddr = 5.U
  val DataAddr = 6.U
  val AddrAddr = 7.U

  val SENSEDELAY_reg = RegInit(0xaa.U(8.W))
  val VTHRESH_reg = RegInit(0.U(16.W))
  val VDRIVE_reg = RegInit(0.U(16.W))
  val DATA_reg = RegInit(0.U(8.W))
  val ADDR_reg = RegInit(0.U(8.W))

  io.VTHRESH := VTHRESH_reg
  io.SENSEDELAY := SENSEDELAY_reg
  io.CMD_DATA := DATA_reg
  io.CMD_ADDR := ADDR_reg
  io.CMD_WRITE := false.B
  io.CMD_READ := false.B
  io.CMD_UPDATEPOT := false.B

  // Read process
  when(io.ADDR === SenseDelayAddr) {
    when(io.WR) {
      SENSEDELAY_reg := io.WRDATA
    }
    io.RDDATA := SENSEDELAY_reg
  }.elsewhen(io.ADDR === VThreshHAddr) {
    when(io.WR) {
      VTHRESH_reg := Cat(io.WRDATA, VTHRESH_reg(7, 0))
    }
    io.RDDATA := VTHRESH_reg(15, 8)
  }.elsewhen(io.ADDR === VThreshLAddr) {
    when(io.WR) {
      VTHRESH_reg := Cat(VTHRESH_reg(15, 8), io.WRDATA)
    }
    io.RDDATA := VTHRESH_reg(7, 0)
  }.elsewhen(io.ADDR === VDriveHAddr) {
    when(io.WR) {
      VDRIVE_reg := Cat(io.WRDATA, VDRIVE_reg(7, 0))
    }
    io.RDDATA := VDRIVE_reg(15, 8)
  }.elsewhen(io.ADDR === VDriveLAddr) {
    when(io.WR) {
      VDRIVE_reg := Cat(VDRIVE_reg(15, 8), io.WRDATA)
    }
    io.RDDATA := VDRIVE_reg(7, 0)
  }.elsewhen(io.ADDR === DataAddr) {
    when(io.WR) {
      DATA_reg := io.WRDATA
    }
    io.RDDATA := DATA_reg
  }.elsewhen(io.ADDR === AddrAddr) {
    when(io.WR) {
      ADDR_reg := io.WRDATA
    }
    io.RDDATA := ADDR_reg
  }.elsewhen(io.ADDR === CtrlAddr) {
    when(io.WR) {
      when(io.WRDATA(0)) {
        io.CMD_WRITE := true.B
      }.elsewhen(io.WRDATA(1)) {
        io.CMD_READ := true.B
      }.elsewhen(io.WRDATA(2)) {
        io.CMD_UPDATEPOT := true.B
      }
    }
    io.RDDATA := 0.U
  }.otherwise {
    io.RDDATA := 0xFF.U
  }

}

object SpiSlaveRegs extends App {
  chisel3.Driver.execute(args, () => new SpiSlaveRegs)
  // Alternate version if there are no args
  // chisel3.Driver.execute(Array[String](), () => new HelloWorld)
}