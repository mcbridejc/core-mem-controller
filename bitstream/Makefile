
VERILOG=../verilog/top.v ../verilog/CoreMem.v
CoreMem.bin: CoreMem.asc
	icepack CoreMem.asc CoreMem.bin

CoreMem.asc : CoreMem.json
	nextpnr-ice40 --hx8k --package tq144:4k --json CoreMem.json --pcf CoreMem.pcf --asc CoreMem.asc > pnr.log

CoreMem.json: ${VERILOG}
	yosys  -p "synth_ice40 -json CoreMem.json" ${VERILOG} > synth.log

