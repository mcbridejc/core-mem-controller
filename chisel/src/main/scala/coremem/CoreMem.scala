class CoreMem extends Module {
    val io = IO(new Bundle {
        // Control Bus
        val slv_addr = Input(UInt(3.W))
        val slv_rd = Input(Bool())
        val slv_wr = Input(Bool())
        val slv_data_o = Output(UInt(8.W))
        val slv_data_i = Input(UInt(8.W))
        val slv_data_t = Output(UInt(8.W))
        val slv_ack = Output(Bool())
        
        // Core driver signals
        val sense = Input(UInt(8.W))
        val sense_thresh = Output(Bool())
        val dir_f = Output(Bool())
        val dir_r = Output(Bool())
        val bit_drive_f = Output(UInt(8.W))
        val bit_drive_r = Output(UInt(8.W))
        val word_drive_f = Output(UInt(8.W))
        val word_drive_r = Output(UInt(8.W))
        val iset = Output(Bool())
    })
    
    io.slv_data_o := 0.U
    io.slv_data_t := 0xFF.U
    io.slv_ack := 0.U
    
    io.iset := 0.U
    io.sense_thresh := 0.U
    io.dir_f := 0.U
    io.dir_r := 0.U
    io.bit_drive_r := 0.U
    io.bit_drive_f := 0.U
    io.word_drive_r := 0.U
    io.word_drive_f := 0.U
    
//     def states = Map("idle" -> 0, "forward" -> 1, "forward-wait" -> 2,
//                      "reverse" -> 3, "reverse-wait" -> 4, "ack" -> 5)
    
    val idle :: forward :: forwardWait :: delay :: reverse :: reverseWait :: ack :: Nil = Enum(6)
    
    def PULSE_LENGTH = 100
    
    val next_state = UInt(3.W)
    val state = RegInit(idle)
    val drive_en = RegInit(false.B) 
    val drive_counter = RegInit(0.U(log2Ceil(PULSE_LENGTH).W))
    
    when(state === idle) {
        when(io.slv_wr || io.slv_rd) {
            next_state := forward
        }.otherwise {
            next_state := idle
        }
    }.elsewhen(state === forward) {
        next_state := forwardWait
    }.elsewhen(state === forwardWait) {
        when(!drive_en) {
            next_state := reverse
        }.otherwise {
            next_state := forwardWait
        }
    }.elsewhen(state === reverse) {
        next_state := reverseWait
    }.elsewhen(state === reverseWait) {
        when(!drive_en) {
            next_state := ack
        }.otherwise {
            next_state := reverseWait
        }
    }.elsewhen(state === ack) {
        // Hold ack until RD/WR are de-asserted by master
        when(!io.slv_rd && !io.slv_wr) {
            next_state := idle
        }.otherwise {
            next_state := ack
        }
    }.otherwise {
        next_state := idle
    }
    
    state := next_state
    
    val delayed_capture = Module(new DelayedCapture(10))
    
    when(next_state === forward || next_state === reverse) {
        drive_en := true.B
        drive_counter := PULSE_LENGTH.U
    }.otherwise {
        when(drive_counter > 0.U) {
            drive_counter := drive_counter - 1.U
        }.otherwise {
            drive_en := false.B
        }
    }
    
    
    val dir_is_forward = state === forward || state === forwardWait
    
    // Drive bit lines
    val write_value = UInt(8.W)
    val read_value = UInt(8.W)
    
    when(io.slv_wr) {
        // If writing, drive the data from the master bus
        write_value := io.slv_data_i
    }.otherwise {
        // When reading, drive the data we just read
        write_value := delayed_capture.io.out
    }
    
    
    io.bit_drive_f := 0.U
    io.bit_drive_r := 0.U
    when(drive_en) {
        when(dir_is_forward) {
            io.bit_drive_f := 0xff.U
        }.otherwise {
            io.bit_drive_r := write_value
        }
    }
    
    // Drive word line
    io.word_drive_f := 0.U
    io.word_drive_r := 0.U
    when(drive_en) {
        
        when(dir_is_forward) {
            io.word_drive_f := (1.U << io.slv_addr)
        }.otherwise {
            io.word_drive_r := (1.U << io.slv_addr)
        }
    }
    
    // Drive voltage thresholds
    val PWM_WIDTH = 11
    val PWM_CLKDIV = 1
    val iset_pwm = new PwmOutput(PWM_CLKDIV, PWM_WIDTH)
    val sense_thresh_pwm = new PwmOutput(PWM_CLKDIV, PWM_WIDTH)
    
    iset_pwm.io.duty := 0.U // TODO
    io.iset := iset_pwm.io.pwm
    sense_thresh_pwm.io.duty := 0.U // TODO
    io.sense_thresh := sense_thresh_pwm.io.pwm
    
}