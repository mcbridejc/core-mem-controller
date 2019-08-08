# core-mem-controller

FPGA controller logic for core memory demo

# Architecture

The controller acts as a SPI slave to provide access to the core memory bits. 

## SPI Register Interface

All SPI transactions consist of 16 bits:

MOSI - | R/W | ADDR[6:0] | DATA[7:0] |
MISO - | DontCare[7:0]   | READDATA[7:0] |

The first byte written by the master indicates the type of transaction being performed. If the first bit is 0, then a read is performed. If it is 1, then a write is being performed. The following 7 bits specify the register address being read/written. For a write, the second byte contains the data to be written; in the case of a read this byte is ignored. 
The slave always writes zeroes for the first byte. During a read, the read value will be returned in the second byte; for a write the second byte will also be all zeros.

During memory access, the slave will hold the CS line low as a busy signal to the master. This will only occur after the START register is written, and should be used by the master as an indication of when the transaction result is completed.

Reg 0 - SENSEDELAY: Specifies the number of clock cycles of delay for latching SENSE data
Reg 1 - CTRL: Control register
        Bit 0 - WRITE: Writing a 1 to this bit initiates a write operation, storing the current value in DATA to the location stored in ADDR register
        Bit 1 - READ: Writing a 1 to this bit initiates a read operation, reading the value from the location stored in the ADDR register into the DATA register to be read by master
        Bit 2 - UPDATEPOT: Writing a 1 to this bit causes the VTHRESH and VDRIVE register values to be written to the digital pots
        Only one bit in this register should be written at a time. Undefined behavior will occur if more than one bit is set. This register always reads as zero. 
Reg 2 - VTHRESH_H: High byte of the VSENSE digital pot setting
Reg 3 - VTHRESH_L: Low byte of the VSENSE digital pot setting
Reg 4 - VDRIVE_H: High byte of the VDRIVE digital pot setting
Reg 5 - VDRIVE_L: Low byet of the VDRIVE digital pot setting
Reg 6 - DATA: Stores data for memory read/write
Reg 7 - ADDR: Stores the memory access address
        Bit 0: Selects X0 or X1 for drive
        Bit 2-1: Selects one of Y0-Y3 for drive

# Building

Hardware is defined with [Chisel](https://chisel.eecs.berkeley.edu/). There are some simple unit tests defined in chisel, but the bulk of testing is done on the generated verilog using cocotb. 

## Running chisel tests

From the `chisel` directory, run:

`sbt test`

## Building verilog output

From the `chisel directory, run:

`sbt 'runMain coremem.CoreMem --target-dir ../verilog'`

