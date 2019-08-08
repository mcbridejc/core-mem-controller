package coremem

import chisel3._
import chisel3.util._

import scala.math.max

/* Generates a delayed pulse of a specified width

After start is asserted, idle will be immediately de-asserted indicating a
pulse is in progress. After `delay` cycles, pulse will be asserted, and it will
remain asserted for `duration` cycles, after which pulse is de-asserted. The idle 
signal will remain low for another `delay` cycles, and then it is asserted again 
indicating the sequence is complete. 

start __-_______________________________
idle  ---_____________________________--
pulse _______---------------------______
*/
class PulseGen(delay: Int, duration: Int) extends Module {
  val io = IO(new Bundle {
    val start = Input(Bool())
    val pulse = Output(Bool())
    val idle = Output(Bool())
  })
  val counterWidth = max(log2Ceil(delay), log2Ceil(duration))

  val counter = RegInit(delay.U(counterWidth.W))

  val sIdle :: sPre :: sPulse :: sPost :: Nil = Enum(4)
  val state = RegInit(sIdle)

  when(counter > 0.U) {
    counter := counter - 1.U
  }

  switch(state) {
    is(sIdle) {
      when(io.start) {
        state := sPre
        counter := delay.U
      }
    }
    
    is(sPre) {
      when(counter === 0.U) {
        state := sPulse
        counter := duration.U
      }
    }
    
    is(sPulse) {
      when(counter === 0.U) {
        state := sPost
        counter := delay.U
      }
    }

    is(sPost) {
      when(counter === 0.U) {
        state := sIdle
      }
    }
  }

  io.pulse := state === sPulse
  io.idle := state === sIdle
}