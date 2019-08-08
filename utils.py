import cocotb
import cocotb.clock
from cocotb.triggers import Timer
from cocotb.result import TestFailure, ReturnValue

SPI_SCLK_FREQ = 1e6
SPI_SCLK_PERIOD_PS = int(1e12 / SPI_SCLK_FREQ)

class RegAddr(object):
    SENSEDELAY = 0
    CTRL = 1
    DATA = 6
    ADDR = 7

@cocotb.coroutine
def spi_xfer(dut, send_data):
    """ Performs a SPI transfer to the DUT

    send_data : cocotb.binary.BinaryValue containing data to be sent

    Returns a BinaryValue of the same size with the returned MISO data
    """
    readVal = 0
    dut.io_SCLK = 1
    dut.io_CSn = 0
    #dut._log.info("xfer: %x", send_data.integer)
    yield Timer(round(SPI_SCLK_PERIOD_PS * 2))
    for bit in reversed(range(send_data.n_bits)):
        if send_data.integer & (1<<bit):
            dut.io_MOSI = 1
        else:
            dut.io_MOSI = 0
        dut.io_SCLK = 0
        yield Timer(int(SPI_SCLK_PERIOD_PS / 2))
        dut.io_SCLK = 1
        if dut.io_MISO == 1:
            readVal += (1<<bit)
        yield Timer(int(SPI_SCLK_PERIOD_PS / 2))
    dut.io_CSn = 1
    yield Timer(200, units='ns')
    raise ReturnValue(cocotb.binary.BinaryValue(readVal, bigEndian=False, n_bits=send_data.n_bits))

@cocotb.coroutine
def spi_read(dut, addr):
    writeVal = cocotb.binary.BinaryValue(addr<<8, bigEndian=False, n_bits=16)
    readVal = yield spi_xfer(dut, writeVal)
    raise ReturnValue(readVal.integer & 0xFF)

@cocotb.coroutine
def spi_write(dut, addr, data):
    writeVal = cocotb.binary.BinaryValue((1<<15) + (addr<<8) + data, bigEndian=False, n_bits=16)
    yield spi_xfer(dut, writeVal)