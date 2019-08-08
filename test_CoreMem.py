import cocotb
from cocotb.triggers import Timer
from cocotb.result import TestFailure
import random

@cocotb.test()
def CoreMem_basic_test(dut):
    """Just exercise it and see if we can save waveforms"""
    yield Timer(2)
    
    for _ in range(100):
        dut.clock = 1
        yield Timer(1)
        dut.clock = 0
        yield Timer(1)
    
    yield Timer(2)
    
    dut.log.info("Ok!")
