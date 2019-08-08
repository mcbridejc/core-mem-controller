package coremem

import chisel3._
import chisel3.util._

class CoreMem extends Module {
    val io = IO(new Bundle {
        // Control Bus
        val SCLK = Input(Bool())
        val CSn = Input(Bool())
        val MISO = Output(Bool())
        val MOSI = Input(Bool())

        // Digital pot control bus
        val POT_SCLK = Output(Bool())
        val POT_CSn = Output(Bool())
        val POT_MOSI = Output(Bool())

        // Core driver signals
        val SENSE = Input(UInt(10.W))
        val DIR = Output(Bool())
        val INHIBIT = Output(Bool())
        val XDRIVE = Output(UInt(2.W))
        val YDRIVE = Output(UInt(4.W))
    })
    
    io.POT_SCLK := false.B
    io.POT_CSn := false.B
    io.POT_MOSI := false.B

    io.DIR := false.B
    io.INHIBIT := false.B
    io.XDRIVE := 0.U(2.W)
    io.YDRIVE := 0.U
//     def states = Map("idle" -> 0, "forward" -> 1, "forward-wait" -> 2,
//                      "reverse" -> 3, "reverse-wait" -> 4, "ack" -> 5)
    
    //val idle :: forward :: forwardWait :: delay :: reverse :: reverseWait :: ack :: Nil = Enum(6)
    
    // def PULSE_LENGTH = 100
    
    // val next_state = UInt(3.W)
    // val state = RegInit(idle)
    // val drive_en = RegInit(false.B) 
    // val drive_counter = RegInit(0.U(log2Ceil(PULSE_LENGTH).W))

    // Create SPI slave interface submodules and connect it to the top-level SPI pins
    // Inputs are registered twice to avoid metastabillity
    val spi_if = Module(new SpiSlaveIF(7, 8))
    val spi_regs = Module(new SpiSlaveRegs)
    spi_if.io.SCLK := RegNext(RegNext(io.SCLK))
    spi_if.io.CSn := RegNext(RegNext(io.CSn))
    spi_if.io.MOSI := RegNext(RegNext(io.MOSI))
    io.MISO := spi_if.io.MISO

    spi_regs.io.WR := spi_if.io.WR
    spi_regs.io.RD := spi_if.io.RD
    spi_regs.io.ADDR := spi_if.io.ADDR
    spi_regs.io.WRDATA := spi_if.io.WRDATA
    spi_if.io.RDDATA := spi_regs.io.RDDATA
    
}

object CoreMem extends App {
  chisel3.Driver.execute(args, () => new CoreMem)
  // Alternate version if there are no args
  // chisel3.Driver.execute(Array[String](), () => new HelloWorld)
}