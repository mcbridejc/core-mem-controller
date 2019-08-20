package coremem;

import chisel3._
import chisel3.iotesters.{ChiselFlatSpec, PeekPokeTester, Driver}


class SpiSlaveIFTester(c: SpiSlaveIF, CPOL: Boolean, CPHA: Boolean) extends PeekPokeTester(c) {

  def spi_xfer(CPOL: Boolean, CPHA: Boolean, bits: Array[Int]) : Array[Int] = {
    var bit = 0
    var readVal = new Array[Int](bits.length)
    var high = 1
    var low = 0
    if(CPOL) {
      high = 0
      low = 1 
    } 

    poke(c.io.SCLK, low)
    step(20)
    poke(c.io.CSn, 0)

    if(!CPHA) {
      poke(c.io.MOSI, bits(0))
    }
    step(20)

    while( bit < bits.length) {
      if(CPHA) {
        poke(c.io.MOSI, bits(bit))
      }
      if(!CPHA) {
        readVal(bit) = if (peek(c.io.MISO) == BigInt(0)) 0 else 1
      }
      bit += 1
      poke(c.io.SCLK, high)
      step(10)
      if(CPHA) {
        readVal(bit-1) =  if (peek(c.io.MISO) == BigInt(0)) 0 else 1
      }
      poke(c.io.SCLK, low)
      if(!CPHA && bit < bits.length) {
        poke(c.io.MOSI, bits(bit))
      }
      step(10)
    }
    poke(c.io.CSn, 1)
    for(i <- 0 to readVal.length-1) {
      System.out.printf(readVal(i).toString)
    }
    return readVal
  }

  poke(c.io.SCLK, 1)
  poke(c.io.CSn, 1)
  poke(c.io.MOSI, 1)

  step(20)

  val write_bits = Array(1, 0, 0, 0, 1, 0, 1, 0, 1, 0, 0, 1, 0, 0, 0, 1) // R/W(1), Addr(7), Data(8)
  val read_bits = Array(0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0)

  spi_xfer(CPOL, CPHA, write_bits)
  expect(c.io.WRDATA, 0x91.U) 

  poke(c.io.RDDATA, 0xa3.U)
  def groupBits(bit_array: Array[Int]) : UInt = {
    var x = 0
    for(i <- 0 to bit_array.length-1) {
      if(bit_array(i) != 0) {
        x += (1<<(bit_array.length - 1 - i))
      }
    }
    return x.U
  }
  val readVal = spi_xfer(CPOL, CPHA, read_bits)
  expect(groupBits(readVal), 0xa3.U)
  
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
  "SpiSlaveIF" should "assert correct signals after write mode 0" in {
    Driver.execute(Array("--generate-vcd-output", "on"), () => new SpiSlaveIF(7, 8, false, false)) { c =>
      new SpiSlaveIFTester(c, false, false)
    } should be(true)
  }
  "SpiSlaveIF" should "assert correct signals after write mode 3" in {
    Driver.execute(Array("--generate-vcd-output", "on"), () => new SpiSlaveIF(7, 8, true, true)) { c =>
      new SpiSlaveIFTester(c, true, true)
    } should be(true)
  }
}