package coremem

import chisel3._
import chisel3.util._

/* Converts SPI transactions into parallel register access signals

RD is asserted after the address bits are clocked in, and the RDDATA input must be
immediately updated with the appropriate value to be clocked out to the master

WR/RD signals are positive-assert. ADDR and WRDATA are valid on the same clock as 
WR is asserted. 

clock input is assumed to be sufficiently faster than SLCK (what is sufficient? TBD)
*/
class SpiSlaveIF(addr_width: Int = 7, data_width: Int = 8) extends Module {
    
  val io = IO(new Bundle {
    val CSn = Input(Bool())
    val SCLK = Input(Bool())
    val MISO = Output(Bool())
    val MOSI = Input(Bool())

    val WR = Output(Bool())
    val RD = Output(Bool())
    val WRDATA = Output(UInt(data_width.W))
    val RDDATA = Input(UInt(data_width.W))
    val ADDR = Output(UInt(addr_width.W))
  })

  val bitCounter = RegInit(0.U((addr_width+data_width).W))
  val addrSR = RegInit(0.U(addr_width.W))
  val dataSR = RegInit(0.U(data_width.W))
  val outDataSR = RegInit(0.U(data_width.W))
  val rwSel = RegInit(false.B)
  val wrReg = RegInit(false.B)
  val rdReg = RegInit(false.B)
  val misoReg = RegInit(false.B)

  def risingedge(x: Bool) = x && !RegNext(x)
  def fallingedge(x: Bool) = !x && RegNext(x)

  io.WR := wrReg
  io.RD := rdReg
  io.WRDATA := dataSR
  io.ADDR := addrSR
  io.MISO := misoReg

  when(io.CSn) {
    bitCounter := 0.U
    wrReg := false.B
    rdReg := false.B
  }.otherwise {
    wrReg := false.B
    rdReg := false.B
    when(risingedge(io.SCLK)) {
      when(bitCounter === 0.U) {
        rwSel := io.MOSI
      }.elsewhen(bitCounter < (addr_width + 1).asUInt) {
        addrSR := Cat(addrSR(addr_width-2, 0), io.MOSI)
      }.elsewhen(bitCounter < (addr_width + 1 + data_width).asUInt) {
        dataSR := Cat(dataSR(data_width-2, 0), io.MOSI)
      }

      when(rwSel && bitCounter === addr_width.U + data_width.U) {
        // write op
        wrReg := true.B
      }
      when(!rwSel && bitCounter === addr_width.U) {
        rdReg := true.B
      }
      bitCounter := bitCounter + 1.U
    }

    when(fallingedge(io.SCLK)) {
      misoReg := outDataSR(data_width-1)
      outDataSR := Cat(outDataSR(data_width-2, 0), false.B)
    }
  }

  when(io.RD === true.B) {
    outDataSR := io.RDDATA
  }
}

object SpiSlaveIF extends App {
  chisel3.Driver.execute(args, () => new SpiSlaveIF)
}