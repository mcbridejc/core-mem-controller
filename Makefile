VERILOG_SOURCES = $(wildcard verilog/CoreMem.v)
TOPLEVEL=CoreMem  # the module name in your Verilog or VHDL file
MODULE=test_CoreMem,test_SpiSlaveIF  # the name of the Python test file
include $(COCOTB)/makefiles/Makefile.inc
include $(COCOTB)/makefiles/Makefile.sim