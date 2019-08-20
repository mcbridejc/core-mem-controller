import cocotb
import cocotb.clock
from cocotb.triggers import Timer
from cocotb.result import TestFailure, ReturnValue
import random

MAIN_CLK_FREQ = 13.333e6
MAIN_CLK_PERIOD_PS = int(1e12 / MAIN_CLK_FREQ / 2) * 2



@cocotb.test()
def SpiSlaveIF_register_readback(dut):
    """Write and readback from the registers that support this"""

    # init IOs
    dut.io_SCLK = 0
    dut.io_CSn = 1
    dut.io_MOSI = 1
    dut.reset = 1

    # Start the main device clock
    print("Main clock period: " + str(MAIN_CLK_PERIOD_PS))
    clock = cocotb.clock.Clock(dut.clock, MAIN_CLK_PERIOD_PS, 'ps')
    cocotb.fork(clock.start())

    # release reset
    yield Timer(200, 'ns')
    dut.reset = 0

    yield Timer(200, 'ns')
    
    @cocotb.coroutine
    def test_readback(addr, register_name):
        writeVal = random.randint(0, 255)
        yield spi_write(dut, addr, writeVal)
        readVal = yield spi_read(dut, addr)
        if readVal != writeVal:
            dut._log.error("Error on register %d (%s) readback: Got %x, expected %x" % (addr, register_name, readVal, writeVal))
            raise TestFailure("Bad readback value for %s" % register_name)

    yield test_readback(0, "SENSEDELAY")
    yield test_readback(2, "VTHRESH_H")
    yield test_readback(3, "VTHRESH_L")
    yield test_readback(4, "VDRIVE_H")
    yield test_readback(5, "VDRIVE_L")
    yield test_readback(6, "DATA")
    yield test_readback(7, "ADDR")

    yield Timer(int(SPI_SCLK_PERIOD_PS * 10), 'ps')
    
