package coremem;

import chisel3._
import chisel3.iotesters.{ChiselFlatSpec, PeekPokeTester}

class DelayedCaptureTester(c: DelayedCapture) extends PeekPokeTester(c) {
    poke(c.io.capture_delay, 10)
    poke(c.io.in, 0)
    poke(c.io.trigger, 0)
    step(1)
    poke(c.io.trigger, 1)
    
    
    for(i <- 1 to 10) {
        expect(c.io.valid, false.B)
        step(1)
        poke(c.io.in, i)
    }
    
    step(1)
    
    expect(c.io.valid, true.B)
    expect(c.io.out, 10)
    
    step(1)
    expect(c.io.valid, false.B)
}

class DelayedCaptureSpec extends ChiselFlatSpec {
  "DelayedCapture" should "Assert its pulse after delay" in {
    chisel3.iotesters.Driver(() => new DelayedCapture(20, 8)) { c =>
      new DelayedCaptureTester(c)
    } should be(true)
  }
}