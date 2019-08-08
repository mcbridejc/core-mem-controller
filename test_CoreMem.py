import cocotb
from cocotb.triggers import Timer, FallingEdge, Edge, Combine
from cocotb.result import TestFailure
from cocotb.utils import get_sim_time
from cocotb.monitors import Monitor
import numpy as np
import random

from utils import spi_write, spi_read, RegAddr

MAIN_CLK_FREQ = 13.333e6
MAIN_CLK_PERIOD_PS = int(1e12 / MAIN_CLK_FREQ / 2) * 2

def count_bits(binval):
    count = 0
    for i in range(binval.n_bits):
        if binval.integer & (1<<i):
            count += 1
    return count

def one_hot_to_index(one_hot):
    if one_hot == 0:
        return None
    for i in range(32):
        if one_hot & (1<<i):
            return i

class SignalMonitor(Monitor):
    """Observes a single-bit input or output of DUT."""
    def __init__(self, name, signal, callback=None, event=None):
        self.name = name
        self.signal = signal
        self.last_change = 0
        Monitor.__init__(self, callback, event)
        
    @cocotb.coroutine
    def _monitor_recv(self):
        while True:
            # Capture signal at rising edge of clock
            yield Edge(self.signal)
            self.last_change = get_sim_time('ps')
            vec = self.signal.value
            self._recv(vec)

    def stable_time(self):
        """Return the number of ps since the last change of this signal"""
        cur_time = get_sim_time('ps')
        return cur_time - self.last_change

class ArrayModel():
    def __init__(self):
        self._state = np.zeros((8,8), dtype=bool)
    
    def get_word(self, addr):
        word = 0
        for i in range(8):
            if self._state[addr][i]:
                word += 1<<i
        return word

    def drive(self, dir, inhibit, xdrive, ydrive):
        """Given new wire conditions, update internal memory state, and return 
        the expected SENSE value
        """
        xsel = one_hot_to_index(xdrive)
        ysel = one_hot_to_index(ydrive)
        if xsel is None or ysel is None:
            # Unless a XDRIVE and YDRIVE are driven simultaneously, we can't
            # flip any bits
            return 0
        
        word_select = xsel + (ysel << 1)
        sense_value = 0
        if dir == 0: # Forward
            # All bits are set to 0; bits which were one will generate a sense pulse
            for i in range(8):
                if self._state[word_select][i]:
                    sense_value += (1<<i)
            self._state[word_select] = np.zeros((1, 8))
        else: # reverse
            # Bits which are not inhibited are set to 1. Nothing is read on SENSE,
            # as even bits which are flipped will create a negative pulse
            for i in range(8):
                if not inhibit & (1<<i):
                    self._state[word_select][i] = True

        return sense_value
    

class ArrayMock():
    SENSEDELAY = 2000
    SENSEWIDTH = 1500

    def __init__(self, dut):
        self._dut = dut
        self._model = ArrayModel()
    
    def get_word(self, addr):
        return self._model.get_word(addr)

    @cocotb.coroutine
    def send_sense_pulse(self, value):
        yield Timer(self.SENSEDELAY, 'ns')
        self._dut.io_drive_SENSE = value
        yield Timer(self.SENSEWIDTH, 'ns')
        self._dut.io_drive_SENSE = 0

    @cocotb.coroutine
    def start(self):
        self._dut.io_drive_SENSE = 0
        yield Timer(20)
        while True:
            yield Combine(Edge(self._dut.io_drive_XDRIVE), Edge(self._dut.io_drive_YDRIVE))
            
            xdrive = self._dut.io_drive_XDRIVE.value.integer
            ydrive = self._dut.io_drive_YDRIVE.value.integer
            dir = self._dut.io_drive_DIR.value.integer
            inhibit = self._dut.io_drive_INHIBIT.value.integer

            if count_bits(self._dut.io_drive_XDRIVE.value) > 1 or count_bits(self._dut.io_drive_YDRIVE.value) > 1:
                raise TestFailure("More than one DRIVE lane asserted at once")
            
            sense = self._model.drive(dir, inhibit, xdrive, ydrive)
            
            if sense != 0:
                self._dut._log.info("Writing back sense=%x" % (sense))
                cocotb.fork(self.send_sense_pulse(sense))

@cocotb.coroutine
def mem_write(dut, addr, data):
    yield spi_write(dut, RegAddr.DATA, data)
    yield spi_write(dut, RegAddr.ADDR, addr)
    yield spi_write(dut, RegAddr.CTRL, (1<<0))

    # Falling edge of MISO indicates transaction is finished
    yield FallingEdge(dut.io_MISO)


@cocotb.coroutine
def mem_read(dut, addr):
    yield spi_write(dut, RegAddr.ADDR, addr)
    yield spi_write(dut, RegAddr.CTRL, (1<<1))

    # Falling edge of MISO indicates transaction is finished
    yield FallingEdge(dut.io_MISO)

    readVal = yield spi_read(dut, RegAddr.DATA)
    return readVal

@cocotb.test()
def CoreMem_read_write_transaction(dut):
    
    # init IOs
    dut.io_SCLK = 1
    dut.io_CSn = 1
    dut.io_MOSI = 1
    dut.reset = 1

    # Start the main device clock
    print("Main clock period: " + str(MAIN_CLK_PERIOD_PS))
    clock = cocotb.clock.Clock(dut.clock, MAIN_CLK_PERIOD_PS, 'ps')
    cocotb.fork(clock.start())

    # Create a mock array
    mock = ArrayMock(dut)
    cocotb.fork(mock.start())

    yield Timer(20, 'ns')
    dut.reset = 0

    yield Timer(200, 'ns')

    # Compute a value for SENSEDELAY register
    sensedelay_clks = int(round((ArrayMock.SENSEDELAY + ArrayMock.SENSEWIDTH/2)*1e-9 * MAIN_CLK_FREQ))
    yield spi_write(dut, RegAddr.SENSEDELAY, sensedelay_clks)


    write_pairs = [
        [3, 0x49],
        [0, 0x9f],
        [7, 0x64],
        [4, 0x24],
        [6, 0x23]
    ]
    # Initiate a memory write for each addr/value in list
    for wp in write_pairs:
        yield mem_write(dut, wp[0], wp[1])
        # Check that the write is properly reflected in the mock's memory
        if mock.get_word(wp[0]) != wp[1]:
            dut._log.error("Invalid memory value: %x" % (mock.get_word(3)))

    # Read back all the values
    for wp in write_pairs:
        readVal = yield mem_read(dut, wp[0])
        if readVal != wp[1]:
            dut._log.error("Invalid readback value :%x" % (readVal))
            raise TestFailure("Bad readback on first try")

    # Read back the values again (to make sure memory value is properly restored
    # after destructive read)
    for wp in write_pairs:
        readVal = yield mem_read(dut, wp[0])
        if readVal != wp[1]:
            dut._log.error("Invalid readback value :%x" % (readVal))
            raise TestFailure("Bad readback on first try")
    
    yield Timer(200, 'ns')
    
