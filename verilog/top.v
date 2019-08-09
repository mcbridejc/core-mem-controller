// Top-level file for instantiating the CoreMem module -- generated by chisel --
// into the FPGA, and setting up the PLL to provide an appropriate clock. 

module top (
    // CLOCK INPUT 
    input wire CLKIN,

    // SPI slave for control
    input wire SCLK,
    input wire CSn,
    input wire MOSI,
    output wire MISO,

    // SPI master for controlling digital pots
    output wire POT_SCLK,
    output wire POT_CSn,
    output wire POT_MOSI,
    
    // Core driver circuit interface
    input wire [7:0] SENSE,
    output wire DIR,
    output wire [7:0] INHIBIT,
    output wire X0_EN_F,
    output wire X0_EN_R,
    output wire X1_EN_F,
    output wire X1_EN_R,
    output wire Y0_EN_F,
    output wire Y0_EN_R,
    output wire Y1_EN_F,
    output wire Y1_EN_R,
    output wire Y2_EN_F,
    output wire Y2_EN_R,
    output wire Y3_EN_F,
    output wire Y3_EN_R
);

wire clk;
wire [1:0] XDRIVE;
wire [3:0] YDRIVE;

reg [7:0] resetb_counter = 0;
wire resetn = &resetn_counter;
wire pll_locked;

always @(posedge clk) begin
    if (!pll_locked) 
        resetn_counter <= 0;
    else if (!resetn)
        resetn_counter <= resetn_counter + 1;
end

SB_PLL40_CORE #(
    .FEEDBACK_PATH("SIMPLE"),
    .PLLOUT_SELECT("GENCLK"),
    .DIVR(4'b0000),
    .DIVF(7'd63),
    .DIVQ(3'd5),
    .FILTER_RANGE(3'b001)
) uut (
    .LOCK(pll_locked),
    .RESETB(1'b1),
    .BYPASS(1'b0),
    .REFERENCECLK(CLKIN),
    .PLLOUTCORE(clk)
);

CoreMem coremem (
    .clock(clk),
    .reset(~resetn),

    .io_SCLK(SCLK),
    .io_CSn(CSn),
    .io_MISO(MISO),
    
    .io_POT_SCLK(POT_SCLK),
    .io_POT_CSn(POT_CSn),
    .io_POT_MOSI(POT_MOSI),
    
    .io_drive_SENSE(SENSE),
    .io_drive_DIR(DIR),
    .io_drive_INHIBIT(INHIBIT),
    .io_drive_XDRIVE(XDRIVE),
    .io_drive_YDRIVE(YDRIVE)
);

assign X0_EN_F = !DIR & XDRIVE[0];
assign X0_EN_R =  DIR & XDRIVE[0];
assign X1_EN_F = !DIR & XDRIVE[1];
assign X1_EN_R =  DIR & XDRIVE[1];
assign Y0_EN_F = !DIR & YDRIVE[0];
assign Y0_EN_R =  DIR & YDRIVE[0];
assign Y1_EN_F = !DIR & YDRIVE[1];
assign Y1_EN_R =  DIR & YDRIVE[1];
assign Y2_EN_F = !DIR & YDRIVE[2];
assign Y2_EN_R =  DIR & YDRIVE[2];
assign Y3_EN_F = !DIR & YDRIVE[3];
assign Y3_EN_R =  DIR & YDRIVE[3];

endmodule