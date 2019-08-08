package coremem

import chisel3._
import chisel3.util._

import scala.math.max

class DriveFSM extends Module {
  val io = IO(new Bundle {
    // Control signals from SPI interface
    val ctrl = Flipped(new SpiControlBundle)

    // Core driver signals
    val drive = new DriveControlBundle

    val busy = Output(Bool())
  })

  def risingedge(x: Bool) = x && !RegNext(x)

  val delayed_capture = Module(new DelayedCapture(255, 8))
  val pulseGen = Module(new PulseGen(100, 670))

  delayed_capture.io.capture_delay := io.ctrl.SENSEDELAY
  delayed_capture.io.trigger := risingedge(pulseGen.io.pulse)
  delayed_capture.io.in := io.drive.SENSE
  
  val sIdle :: sForward :: sForwardWait :: sReverse :: sReverseWait :: sUpdatePot :: Nil = Enum(6)

  val state = RegInit(sIdle)
  val cmdIsRead = RegInit(false.B)
  val pulseComplete = Wire(Bool())
  val updateComplete = RegInit(true.B)
  val dir = RegInit(false.B)
  val forwardReadValue = RegInit(0.U(8.W))

  switch(state) {
    is(sIdle) {
      dir := false.B
      when(io.ctrl.CMD_READ) {
        state := sForward
        cmdIsRead := true.B
      }.elsewhen(io.ctrl.CMD_WRITE) {
        state := sForward
        cmdIsRead := false.B
      }.elsewhen(io.ctrl.CMD_UPDATEPOT) {
        state := sUpdatePot
      }
    }

    is(sForward) {
      state := sForwardWait
      dir := false.B
    }

    is(sForwardWait) {
      when(pulseComplete) {
        state := sReverse
        forwardReadValue := delayed_capture.io.out
      }
    }

    is(sReverse) {
      state := sReverseWait
      dir := true.B
    }

    is(sReverseWait) {
      when(pulseComplete) {
        state := sIdle
      }
    }

    is(sUpdatePot) {
      when(updateComplete) {
        state := sIdle
      }
    }
    
  }

  // Generate drive outputs
  // It is important that DIR and INHIBIT lines be asserted some time before 
  // the XDRIVE/YDRIVE. To that end, the pulse gen module provides a pulse with 
  // some time delay before and after. 
  pulseGen.io.start := state === sForward | state === sReverse
  pulseComplete := pulseGen.io.idle

  io.drive.DIR := dir

  val writeValue = Wire(UInt(8.W))
  when(cmdIsRead) {
    writeValue := forwardReadValue
  } otherwise {
    writeValue := io.ctrl.DATA
  }

  when(state === sReverse | state === sReverseWait) {
    io.drive.INHIBIT := ~writeValue
  } otherwise {
    io.drive.INHIBIT := 0.U
  }

  when(pulseGen.io.pulse) {
    // ADDR[0] determines which X line to drive
    io.drive.XDRIVE := 1.U << io.ctrl.ADDR(0)
    // ADDR[2:1] deteremins which Y line to drive
    io.drive.YDRIVE := 1.U << io.ctrl.ADDR(2, 1)
  } otherwise {
    io.drive.XDRIVE := 0.U
    io.drive.YDRIVE := 0.U
  }

  // Provide read data storage back to SPI control regs
  io.ctrl.DATAIN := forwardReadValue
  io.ctrl.CMD_READ_ACK := (cmdIsRead && state === sIdle && RegNext(state) === sReverseWait)

  io.busy := !(state === sIdle)
}