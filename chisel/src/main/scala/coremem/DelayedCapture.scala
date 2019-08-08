package coremem

import chisel3._
import chisel3.util._

class DelayedCapture(max_delay: Int, data_width: Int = 8) extends Module {
    val delay_width = log2Ceil(max_delay)
    val io = IO(new Bundle {
        val capture_delay = Input(UInt(delay_width.W))
        val trigger = Input(Bool())
        val in = Input(UInt(data_width.W))
        val out = Output(UInt(data_width.W))
        val valid = Output(Bool())
    })
    
    val running = RegInit(false.B)
    val outReg = RegInit(0.U(data_width.W))
    val counter = RegInit(0.U(delay_width.W))
    val validReg = RegInit(false.B)
    
    io.out := outReg
    io.valid := validReg
    
    def risingedge(x: Bool) = x && !RegNext(x)
    
    when(risingedge(io.trigger)) {
        running := true.B
    }
    
    validReg := false.B
    
    when(running) {
        
        val ovf = counter === (io.capture_delay - 1.U)
        counter := counter + 1.U
        when(ovf) {
            counter := 0.U
            running := false.B
            validReg := true.B
            outReg := io.in
        }
    }
}

object DelayedCapture extends App {
  chisel3.Driver.execute(args, () => new DelayedCapture(1024, 8))
  // Alternate version if there are no args
  // chisel3.Driver.execute(Array[String](), () => new HelloWorld)
}