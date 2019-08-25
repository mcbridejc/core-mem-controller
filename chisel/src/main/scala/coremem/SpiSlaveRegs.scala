package coremem

import chisel3._
import chisel3.util._

/* Implements the registers and SPI control signals

This connects to the SpiSlaveIF module and provides simple outputs for the 
controller state machine to use. 
*/
class SpiControlBundle extends Bundle {
  val SENSEDELAY = Output(UInt(8.W))
  val VTHRESH = Output(UInt(8.W))
  val VDRIVE = Output(UInt(8.W))
  val DATA = Output(UInt(8.W))
  val ADDR = Output(UInt(8.W))
  val CMD_WRITE = Output(Bool())
  val CMD_READ = Output(Bool())
  val CMD_UPDATEPOT = Output(Bool())
  val CMD_READ_ACK = Input(Bool())
  val DATAIN = Input(UInt(8.W))
}

class SpiSlaveRegs() extends Module {
    
  val io = IO(new Bundle {
    // Signals from SpiSlaveIF
    val SPI_WR = Input(Bool())
    val SPI_RD = Input(Bool())
    val SPI_WRDATA = Input(UInt(8.W))
    val SPI_RDDATA = Output(UInt(8.W))
    val SPI_ADDR = Input(UInt(7.W))

    // Outputs
    val ctrl = new SpiControlBundle()
  })

  val SenseDelayAddr = 0.U
  val CtrlAddr = 1.U
  val VThreshAddr = 2.U
  val VDriveAddr = 4.U
  val DataAddr = 6.U
  val AddrAddr = 7.U

  val SENSEDELAY_reg = RegInit(0xaa.U(8.W))
  val VTHRESH_reg = RegInit(0.U(8.W))
  val VDRIVE_reg = RegInit(0.U(8.W))
  val DATA_reg = RegInit(0.U(8.W))
  val ADDR_reg = RegInit(0.U(8.W))

  io.ctrl.VTHRESH := VTHRESH_reg
  io.ctrl.VDRIVE := VDRIVE_reg
  io.ctrl.SENSEDELAY := SENSEDELAY_reg
  io.ctrl.DATA := DATA_reg
  io.ctrl.ADDR := ADDR_reg
  io.ctrl.CMD_WRITE := false.B
  io.ctrl.CMD_READ := false.B
  io.ctrl.CMD_UPDATEPOT := false.B

  // Read process
  when(io.SPI_ADDR === SenseDelayAddr) {
    when(io.SPI_WR) {
      SENSEDELAY_reg := io.SPI_WRDATA
    }
    io.SPI_RDDATA := SENSEDELAY_reg
  }.elsewhen(io.SPI_ADDR === VThreshAddr) {
    when(io.SPI_WR) {
      VTHRESH_reg := io.SPI_WRDATA
    }
    io.SPI_RDDATA := VTHRESH_reg
  }.elsewhen(io.SPI_ADDR === VDriveAddr) {
    when(io.SPI_WR) {
      VDRIVE_reg := io.SPI_WRDATA
    }
    io.SPI_RDDATA := VDRIVE_reg
  }.elsewhen(io.SPI_ADDR === DataAddr) {
    when(io.SPI_WR) {
      DATA_reg := io.SPI_WRDATA
    }
    io.SPI_RDDATA := DATA_reg
  }.elsewhen(io.SPI_ADDR === AddrAddr) {
    when(io.SPI_WR) {
      ADDR_reg := io.SPI_WRDATA
    }
    io.SPI_RDDATA := ADDR_reg
  }.elsewhen(io.SPI_ADDR === CtrlAddr) {
    when(io.SPI_WR) {
      when(io.SPI_WRDATA(0)) {
        io.ctrl.CMD_WRITE := true.B
      }.elsewhen(io.SPI_WRDATA(1)) {
        io.ctrl.CMD_READ := true.B
      }.elsewhen(io.SPI_WRDATA(2)) {
        io.ctrl.CMD_UPDATEPOT := true.B
      }
    }
    io.SPI_RDDATA := 0.U
  }.otherwise {
    io.SPI_RDDATA := 0xFF.U
  }

  // The data register can be updated by SPI write, or by the hardware
  // (whenever a read is performed, the read value is stored here)
  when(io.ctrl.CMD_READ_ACK) {
    DATA_reg := io.ctrl.DATAIN;
  }
}

object SpiSlaveRegs extends App {
  chisel3.Driver.execute(args, () => new SpiSlaveRegs)
  // Alternate version if there are no args
  // chisel3.Driver.execute(Array[String](), () => new HelloWorld)
}