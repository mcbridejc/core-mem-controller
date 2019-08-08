package coremem;

import chisel3._
import chisel3.iotesters.{ChiselFlatSpec, PeekPokeTester, Driver}

class SpiSlaveIFTester(c: SpiSlaveIF) extends PeekPokeTester(c) {

    poke(c.io.SCLK, 1)
    poke(c.io.CSn, 1)
    poke(c.io.MOSI, 1)

    step(10)

    val write_bits = Array(1, 0, 0, 0, 1, 0, 1, 0, 1, 0, 0, 1, 0, 0, 0, 1) // R/W(1), Addr(7), Data(8)

    poke(c.io.CSn, 0)
    step(10)

    for( i <- 0 to 15) {
        poke(c.io.MOSI, write_bits(i))
        poke(c.io.SCLK, 0)
        step(5)
        poke(c.io.SCLK, 1)
        step(5)
    }
    poke(c.io.CSn, 1)
    
    expect(c.io.WRDATA, 0x91.U)
}

class SpiSlaveIFSpec extends ChiselFlatSpec {
  // behavior of "SpiSlaveIF"
  // backends foreach {backend =>
  //   it should s"correctly add randomly generated numbers $backend" in {
  //     Driver(() => new SpiSlaveIF(7, 8))(c => new SpiSlaveIFTester(c)) should be (true)
  //   }
    
  // }
  // it should "run while creating a vcd" in {
  //   Driver.execute(, () => new SpiSlaveIF(7, 8)) { c =>
  //     new SpiSlaveIFTester(c)
  //   }
  // }
  "SpiSlaveIF" should "assert correct signals after write" in {
    Driver.execute(Array("--generate-vcd-output", "on"), () => new SpiSlaveIF) { c =>
      new SpiSlaveIFTester(c)
    } should be(true)
  }
}