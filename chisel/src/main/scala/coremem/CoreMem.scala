package coremem

import chisel3._
import chisel3.util._

class DriveControlBundle extends Bundle {
    val SENSE = Input(UInt(8.W))
    val DIR = Output(Bool())
    val INHIBIT = Output(UInt(8.W))
    val XDRIVE = Output(UInt(2.W))
    val YDRIVE = Output(UInt(4.W))
}

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
        val drive = new DriveControlBundle
    })

    io.drive.DIR := false.B
    io.drive.INHIBIT := 0.U
    io.drive.XDRIVE := 0.U(2.W)
    io.drive.YDRIVE := 0.U

    // Create SPI slave interface submodules and connect it to the top-level SPI pins
    // Inputs are registered twice to avoid metastabillity
    val CPOL = false
    val CPHA = false
    val spi_if = Module(new SpiSlaveIF(7, 8, CPOL, CPHA))
    val spi_regs = Module(new SpiSlaveRegs)
    val drive_fsm = Module(new DriveFSM())
    val mcp42 = Module(new MCP42xxx())

    mcp42.io.write := drive_fsm.io.potStartUpdate
    drive_fsm.io.potAckUpdate := ~mcp42.io.busy
    mcp42.io.enable0 := true.B
    mcp42.io.enable1 := true.B
    mcp42.io.data0 := spi_regs.io.ctrl.VDRIVE
    mcp42.io.data1 := spi_regs.io.ctrl.VTHRESH
    io.POT_SCLK := mcp42.io.sclk
    io.POT_CSn := mcp42.io.csn
    io.POT_MOSI := mcp42.io.mosi

    spi_if.io.SCLK := RegNext(RegNext(io.SCLK))
    spi_if.io.CSn := RegNext(RegNext(io.CSn))
    spi_if.io.MOSI := RegNext(RegNext(io.MOSI))
    // MISO doubles as busy line when CSn is not asserted
    // MISO high means a transaction is in progress
    when(io.CSn) {
        io.MISO := drive_fsm.io.busy
    }otherwise{
        io.MISO := spi_if.io.MISO
    }

    spi_regs.io.SPI_WR := spi_if.io.WR
    spi_regs.io.SPI_RD := spi_if.io.RD
    spi_regs.io.SPI_ADDR := spi_if.io.ADDR
    spi_regs.io.SPI_WRDATA := spi_if.io.WRDATA
    spi_if.io.RDDATA := spi_regs.io.SPI_RDDATA
    spi_regs.io.ctrl.CMD_READ_ACK := false.B

    drive_fsm.io.ctrl <> spi_regs.io.ctrl
    io.drive <> drive_fsm.io.drive
    
}

object CoreMem extends App {
  chisel3.Driver.execute(args, () => new CoreMem)
}