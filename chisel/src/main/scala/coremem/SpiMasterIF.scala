package coremem

import chisel3._
import chisel3.util._

/* Performs 16-bit word transactions as a SPI master

Supports writing only. Designed with an MCP42xxx digital pot in mind. 

When START is asserted, the value on DATAIN is latched, ACTIVE goes high, and a
transfer begins. When the transfer is complete ACTIVE goes low and the module is 
ready for a new START signal. 

*/
class SpiMasterIF(dataWidth: Int = 16, clkDiv: Int = 40) extends Module {
    
  val io = IO(new Bundle {
    val dataIn = Input(UInt(dataWidth.W))
    val start = Input(Bool())
    val active = Output(Bool())


    val csn = Output(Bool())
    val sclk = Output(Bool())
    val mosi = Output(Bool())
  })

  
  // Defines how long to hold CS asserted before and after first clock edges
  val PreCsClocks = 2
  val PostCsClocks = 2
  // How long after de-asserting CS before deasserting active
  // This can be used to ensure a minimum CS high time between transfers
  val ActiveDelayClocks = 2
  val ActiveTransactionLength = (dataWidth + PreCsClocks + PostCsClocks + ActiveDelayClocks) * 2 
  val TotalTransactionLength = ActiveTransactionLength + ActiveDelayClocks * 2
  val CounterWidth = log2Ceil(TotalTransactionLength)

  val dataReg = RegInit(0.U(dataWidth.W))
  val counterReg = RegInit(0.U(CounterWidth.W))
  val sclkReg = RegInit(false.B)
  val enableCounter = Counter(clkDiv)
  val enable = Wire(Bool())
  val startInternal = RegInit(false.B)

  // Enable divided clock logic when counter overflows
  
  when(enableCounter.value === 0.U) {
    enable := true.B
  } otherwise {
    enable := false.B
  }

  // Latch start signal and data word on any clock, and hold it until the next enabled cycle
  when(io.start) {
    startInternal := true.B
    dataReg := io.dataIn
  }.elsewhen(enable === true.B) {
    startInternal := false.B
  }
  
  when(counterReg === 0.U) {
    when(startInternal) {
      counterReg := 1.U
      sclkReg := false.B
    }
  } otherwise {
    counterReg := counterReg + 1.U
    when(counterReg >= PreCsClocks.U * 2.U && counterReg < (PreCsClocks + dataWidth).U * 2.U) {
      when(sclkReg) {
        // advance output to next bit in shift register
        dataReg := dataReg << 1
      }
      sclkReg := ~sclkReg
    }.elsewhen(counterReg >= TotalTransactionLength.U) {
      counterReg := 0.U
    }
  }

  // active should be asserted the clock after start is, even though the 
  // counterReg value won't update until the next divided clock cycle
  io.active := ~(counterReg === 0.U) || startInternal
  io.sclk := sclkReg
  io.csn := (counterReg === 0.U || counterReg >= ActiveTransactionLength.U)
  io.mosi := dataReg(dataWidth-1) // MSB of shift register on output
}
