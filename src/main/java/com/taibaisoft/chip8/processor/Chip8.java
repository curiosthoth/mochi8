/*
 * Copyright (c) 2014 Jeffrey Bian
 * 
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 */

package com.taibaisoft.chip8.processor;

import java.util.Arrays;

import com.taibaisoft.chip8.platform.Util;
import com.taibaisoft.framework.GenericCPU;
import com.taibaisoft.framework.IExceptionHandler;

/**
 * Reference:
 * 
 * http://devernay.free.fr/hacks/chip8/C8TECH10.HTM
 * http://devernay.free.fr/hacks/chip8/schip.txt
 * http://www.nic.funet.fi/pub/misc/hp48sx/chip/s_chip10.txt
 * Chip-8 Pack from http://chip8.com/?page=109 
 * 
 * David Winter's CHIP8.doc
 * David Winter's CHIPPER.doc (for V2.11)
 * 
 * Emulator tests : Fish'n'Chips
 *
 * 
 * @author jeffreybian
 *
 */
public class Chip8 extends GenericCPU {

	public enum Mode {
		CHIP_8,
		SCHIP,
	}
	public static final int MEMSIZE = 4096;
	public final static int DEFAULT_LOAD_ADDRESS = 0x200;
	public final static int CHIP_8_PIXELS_X = 64;
	public final static int CHIP_8_PIXELS_Y = 32;
	public final static int CHIP_8_HIRES_PIXELS_X = 64;
	public final static int CHIP_8_HIRES_PIXELS_Y = 64;
	public final static int SCHIP_PIXELS_X = 128;
	public final static int SCHIP_PIXELS_Y = 64;
	public final static int DEFAULT_CPU_HZ = 600;
	
	public final static int DEFAULT_RENDER_HZ = 30; 	// This must be lower than the DEFAULT_CPU_HZ above
	public final static int DEFAULT_TIMER_HZ = 60; 	// This must be lower than the DEFAULT_CPU_HZ above

	protected byte[] memory_ = null;
	/** For 5-byte sprites. */
	protected int digitSpriteStart_ = 0x040;
	protected int[] digitSprites_ = {
	/* 0 */0xF0, 0x90, 0x90, 0x90, 0xF0,
	/* 1 */0x20, 0x60, 0x20, 0x20, 0x70,
	/* 2 */0xF0, 0x10, 0xF0, 0x80, 0xF0,
	/* 3 */0xF0, 0x10, 0xF0, 0x10, 0xF0,
	/* 4 */0x90, 0x90, 0xF0, 0x10, 0x10,
	/* 5 */0xF0, 0x80, 0xF0, 0x10, 0xF0,
	/* 6 */0xF0, 0x80, 0xF0, 0x90, 0xF0,
	/* 7 */0xF0, 0x10, 0x20, 0x40, 0x40,
	/* 8 */0xF0, 0x90, 0xF0, 0x90, 0xF0,
	/* 9 */0xF0, 0x90, 0xF0, 0x10, 0xF0,
	/* A */0xF0, 0x90, 0xF0, 0x90, 0x90,
	/* B */0xE0, 0x90, 0xE0, 0x90, 0xE0,
	/* C */0xF0, 0x80, 0x80, 0x80, 0xF0,
	/* D */0xE0, 0x90, 0x90, 0x90, 0xE0,
	/* E */0xF0, 0x80, 0xF0, 0x80, 0xF0,
	/* F */0xF0, 0x80, 0xF0, 0x80, 0x80,
	};
	/** For 10-byte sprites, only available for SCHIP */
	protected int digit10SpriteStart_ = 0x0100;
	
	protected int[] digit10Sprites_ = {
	/* 0 */0x7c, 0xc6, 0xce, 0xde, 0xd6, 0xf6, 0xe6, 0xc6, 0x7c, 0x00,
	/* 1 */0x10, 0x30, 0xf0, 0x30, 0x30, 0x30, 0x30, 0x30, 0xfc, 0x00,
	/* 2 */0x78, 0xcc, 0xcc, 0x0c, 0x18, 0x30, 0x60, 0xc4, 0xfc, 0x00,
	/* 3 */0x78, 0xcc, 0x0c, 0x0c, 0x38, 0x0c, 0x0c, 0xcc, 0x78, 0x00,
	/* 4 */0x0c, 0x1c, 0x3c, 0x6c, 0xcc, 0xfe, 0x0c, 0x0c, 0x1e, 0x00,
	/* 5 */0xfc, 0xc0, 0xc0, 0xc0, 0xf8, 0x0c, 0x0c, 0xcc, 0x78, 0x00,
	/* 6 */0x38, 0x60, 0xc0, 0xc0, 0xf8, 0xcc, 0xcc, 0xcc, 0x78, 0x00,
	/* 7 */0xfc, 0xcc, 0x0c, 0x0c, 0x18, 0x30, 0x30, 0x30, 0x30, 0x00,
	/* 8 */0x78, 0xcc, 0xcc, 0xec, 0x78, 0xdc, 0xcc, 0xcc, 0x78, 0x00,
	/* 9 */0x7c, 0xc6, 0xc6, 0xc6, 0x7c, 0x18, 0x18, 0x30, 0x70, 0x00,
	/* a */0x30, 0x78, 0xcc, 0xcc, 0xcc, 0xfc, 0xcc, 0xcc, 0xcc, 0x00,
	/* b */0xfc, 0x66, 0x66, 0x66, 0x7c, 0x66, 0x66, 0x66, 0xfc, 0x00,
	/* c */0x3c, 0x66, 0xc6, 0xc0, 0xc0, 0xc0, 0xc6, 0x66, 0x3c, 0x00,
	/* d */0xf8, 0x6c, 0x66, 0x66, 0x66, 0x66, 0x66, 0x6c, 0xf8, 0x00,
	/* e */0xfe, 0x62, 0x60, 0x64, 0x7c, 0x64, 0x60, 0x62, 0xfe, 0x00,
	/* f */0x7f, 0x33, 0x31, 0x32, 0x3e, 0x32, 0x30, 0x30, 0x78, 0x00,	
	};
	/**
	 * Graphics memory emulator.
	 * Each element stands for 1 pixel, so either 0 or 1.
	 */
	protected volatile int[] graphicsBuffer_ = new int[SCHIP_PIXELS_X * SCHIP_PIXELS_Y];
	/**
	 * Keyboard memory emulator Pressed is 1 otherwise 0 Indices are [0x0, 0xF]
	 * 
	 * Layout 
	 * 1 2 3 C 
	 * 4 5 6 D 
	 * 7 8 9 E 
	 * A 0 B F
	 */
	protected byte[] kbdmem = new byte[16];
	

	protected int I = 0x0; // 16bit register, only right most 12 bits are used.
	protected byte[] v = new byte[16]; // v[0] -> v[F], 8bit registers
	/**
	 * delayTimer and soundTimer will automatically decrease by 1 at 60Hz
	 * frequency, if greater than 0.
	 */
	protected int delayTimer = 0x0;
	protected int soundTimer = 0x0;

	protected byte[] rplFlags = new byte[16]; // RPL flags, only for compatibility for SCHIP on HP48
	
	protected boolean awaitingKey_ = false;
	
		
	double prevTimerTick = 0;
	IScreen renderer = null;
	IBuzzer buzzer = null;
	IKeyboard keyboard = null;
	IExceptionHandler ehandler = null;
	Mode mode = Mode.CHIP_8; 
	/** This flag only matters when Mode== Mode.SCHIP */
	boolean isExtendedScreen = false;
	int ox = 0; // Origin 
	int oy = 0; // Origin
	int H = CHIP_8_PIXELS_X;
	
	int V = CHIP_8_PIXELS_Y;
	private int startAddress_ = DEFAULT_LOAD_ADDRESS;
	
	public Chip8(IScreen g, IKeyboard k, IBuzzer b, IExceptionHandler h) {
		renderer = g;
		buzzer = b;
		keyboard = k;
		ehandler = h;
		if (renderer == null || buzzer == null || keyboard == null) {
			throw new NullPointerException();
		}
		registerTimer(0, 1000000000/DEFAULT_TIMER_HZ);
		setRenderFrequency(DEFAULT_RENDER_HZ);
	}
	public byte[] getMemory() {
		return memory_;
	}
	public int getUnitX() {
		return H;
	}
	public int getUnitY() {
		return V;
	}
	public boolean loadProgram(byte[] prog) {
		return loadProgram(prog, Mode.CHIP_8, DEFAULT_LOAD_ADDRESS);
	}	
	public boolean loadProgram(byte[] prog, Mode mode) {
		return loadProgram(prog, mode, DEFAULT_LOAD_ADDRESS);
	}
	/**
	 * It is up to the caller to call reset() first if a previous program is running.
	 * This routine only does what its name implies: loading a program in to some mem location.
	 * @param prog
	 * @param mode Mode. The mode to run this program.
	 * @param startAddress_
	 */
	public boolean loadProgram(byte[] prog, Mode m, int start) {
		boolean r = false;
		if (prog != null) {
			init(m);
			startAddress_ = start;
			synchronized (memory_) {
				System.arraycopy(prog, 0, memory_, startAddress_, prog.length);
				pc=startAddress_;
			}
			r = true;
		} else {
			r = false;
		}
		return r;
	}

	/**
	 * The overall reset procedure, calls
	 * sub resets for each components.
	 * - Resets keyboard
	 * - Resets timers and all registers
	 * - Clears the whole memory
	 * - Clears the stack 
	 * - Clears the graphics memory
	 * - Set assistant variables such as prevTimerTick back to 0, paused to false.
	 * 
	 * This method should be called before loading a new program.
	 * This method is indempotent.
	 */
	public void reset() {
		stop();
		resetTimers();
		resetCallstack();
		resetMemoryAndRegisters();	
		resetKbd();
	}

	/**
	 * 
	 * @param x X coord;
	 * @param y Y coord;
	 * @param memStart Start of the memory to blit.
	 * @param memEnd Exclusive the end of the memory.
	 * @param 
	 */
	private void blitSprite(int sx, int sy, int memStart, int memEnd) {
		int inc = 1;
		if (memEnd-memStart==32) {
			// Draws 16x16 sprites.
			inc = 2;
		}	
		// Extracts each bit to a byte and XOR into gfx
		int cy = sy % V;
		v[0xF] = 0;
		for (int j = memStart; j < memEnd; j+=inc) {
			for (int q = inc-1;q>=0;q--) {
				for (int k = 0; k < 8; ++k) {
					int x = (sx + k + q*8) % H;
					int cindex = x + (cy*H);
					int pixel = ((Util.b2i(memory_[j+q]) >>> (7-k)) & 0x1) ^ (graphicsBuffer_[cindex] & 0x1);
					/* The correct logic : if the pixel originally at the cindex is NOT zero
					 * AND now it has become zero after XOR, set v[0xf]!!
					 * Previously this puzzled me for some time but finally I was able to fix it!e
					 * */
					if (v[0xF] ==0 && pixel == 0 && (graphicsBuffer_[cindex] & 0x1)==0x1) {
						v[0xF] = 1;
					}
					graphicsBuffer_[cindex] = pixel;
				}
			}
			cy = (cy+1) % V;
		}
	}
	/**
	 * This routine is called at cpuFrequency.
	 * @param delta
	 * @throws Exception 
	 * @throws AbnormalStopException 
	 */
	@Override
	public void oneCycle() throws Exception{
		// Fetch and run an opcode at PC, 16 bit, Big endian
		if (pc<MEMSIZE-2) {
			int opcode = ((memory_[pc] & 0xFF) << 8) | (memory_[pc + 1] & 0xFF); 
			// Intercepts for HiRes
			if ( pc==startAddress_ && (opcode==0x1260) ) {
				H = CHIP_8_HIRES_PIXELS_X;
				V = CHIP_8_HIRES_PIXELS_Y;
				opcode = 0x12C0;
			}
			runOpcode(opcode);
		} else {
			throw new Exception("Reached the end of program.");
		}
	}
	/**
	 * 
	 */
	private void init(Mode m) {
		mode = m;
		memory_ = new byte[MEMSIZE];
		int len = digitSprites_.length;
		for (int i = 0; i < len; ++i) {
			memory_[digitSpriteStart_ + i] = (byte) digitSprites_[i];
		}
		len = digit10Sprites_.length;
		for (int i = 0; i < len; ++i) {
			memory_[digit10SpriteStart_ + i] = (byte) digit10Sprites_[i];
		}
		H = CHIP_8_PIXELS_X;
		V = CHIP_8_PIXELS_Y;
		isExtendedScreen = false;
	}
	private void resetCallstack() {
		if (callStack != null) {
			int len = callStack.length;
			for (int i = 0; i < len; ++i) {
				callStack[i] = 0;
			}
		}
		sp =0;
	}
	/**
	 * Clears the kbd states
	 */
	private void resetKbd() {
		int len = kbdmem.length;
		for (int i = 0; i < len; ++i) {
			kbdmem[i] = 0;
		}
	}
	private void resetMemoryAndRegisters() {
		pc = 0;
		I = 0;
		if (memory_ != null)
			Util.fill(memory_, (byte)0);
		if (graphicsBuffer_!=null)
			Arrays.fill(graphicsBuffer_, 0);
		if (v != null)
			Util.fill(v, (byte)0);
		soundTimer = 0;
		delayTimer = 0;
		startAddress_ = 0;
	}	
	private void resetTimers() {
		prevTimerTick = 0;
		delayTimer = 0;
		soundTimer = 0;
	}

	private void runOpcode(int opcode) throws ExitException {
		// Decode and run an opcode
		switch (opcode & 0xF000) {
		/** ---- 0x0000 ---- */
		case 0x0000: {
			if (opcode==0x0230) {
				// [Hi-RES] 64x64 CLS
				for (int i = 0; i < graphicsBuffer_.length; ++i) {
					graphicsBuffer_[i] = 0;
				}
				pc += 2;
			} else if (opcode==0x00E0) {
				// CLS - Clears the screen
				for (int i = 0; i < graphicsBuffer_.length; ++i) {
					graphicsBuffer_[i] = 0;
				}
				pc += 2;				
			} else if (opcode==0x00EE) {
				// RET - Returns from subroutine call
				sp--;
				pc = callStack[sp];
				pc += 2;	
			} else if (opcode==0x00FB) {
				// [S-CHIP]
				// SCR - Scrolls screen 4 pixels right
				Util.shiftArray(graphicsBuffer_, 1, 4, H);
				pc+=2;
			} else if (opcode==0x00FC) {
				// [S-CHIP]
				// SCL - Scrolls screen 4 pixels left
				Util.shiftArray(graphicsBuffer_, -1, 4, H);
				pc+=2;
			} else if (opcode==0x00FD) {
				// [S-CHIP]
				// EXT Exit interpreter.
				// TODO: Make this better, notify board and other.
				// running_ = false;
				throw new ExitException();
			} else if (opcode==0x00FE) {
				// [S-CHIP]
				// LOW - Disables the extended screen mode
				if (isExtendedScreen) {
					isExtendedScreen = false;
					H = CHIP_8_PIXELS_X;
					V = CHIP_8_PIXELS_Y;
				}
				pc+=2;
			} else if (opcode==0x00FF) {
				// [S-CHIP]
				// HIGH - Enables the extended screen mode (128x64)
				if ( !isExtendedScreen ) {
					isExtendedScreen = true;
					H = SCHIP_PIXELS_X;
					V = SCHIP_PIXELS_Y;
				}
				pc+=2;
			} else {
				if ( (opcode & 0x00F0)==0x00C0 ) {
					// [S-CHIP]
					// SCD N - Scrolls the screen down X lines
					Util.shiftArray(graphicsBuffer_, 2, (opcode&0x000F), H);
					pc+=2;
				} else {
					// Obsolete
					// SYS NNN - Execute RCA1802 instructions at address NNN
					// Should simply ignore
					pc+=2;
				}
			}
		}
		break;
		/** ---- 0x1000 ---- */			
		case 0x1000: // JP NNN - Jumps to address NNN.
			pc = opcode & 0x0FFF;
		break;
		/** ---- 0x2000 ---- */
		case 0x2000: // CALL NNN - Calls subroutine at NNN.
			callStack[sp] = pc;
			++sp;
			pc = opcode & 0x0FFF;
		break;

		case 0x3000: // SE Vx, NN - Skips the next instruction if VX equals NN.
		{
			int index = (opcode & 0x0F00) >>> 8;
			if ((Util.b2i(v[index]) & 0xFF) == (opcode & 0xFF)) {
				pc += 2;
			}
		}
			pc += 2;
		break;
		case 0x4000: // SNE Vx, NN - Skips the next instruction if VX doesn't equal NN.
		{
			int index = (opcode & 0x0F00) >>> 8;
			if ((Util.b2i(v[index]) & 0xFF) != (opcode & 0xFF)) {
				pc += 2;
			}
		}
			pc += 2;
			break;
		case 0x5000: // SE Vx, Vy - Skips the next instruction if VX equals VY.
		{
			int indx = (opcode & 0x0F00) >>> 8;
			int indy = (opcode & 0x00F0) >>> 4;

			if ((Util.b2i(v[indx]) & 0xFF) == (v[indy] & 0xFF)) {
				pc += 2;
			}
		}
			pc += 2;
			break;
		case 0x6000: // LD Vx, NN - Sets VX to NN.
		{
			int indx = (opcode & 0x0F00) >>> 8;
			v[indx] = (byte) (opcode & 0xFF);
		}
			pc += 2;
			break;
		case 0x7000: // ADD Vx, NN - Adds NN to VX.
		{
			int indx = (opcode & 0x0F00) >>> 8;
			int r = (Util.b2i(v[indx]) + (opcode & 0x00FF));
			if (r>0xFF) {
				v[0xF] = 0x1;
			}
			v[indx] = (byte) r;
		}
			pc += 2;
			break;
		case 0x8000: {
			int indx = (opcode & 0x0F00) >>> 8;
			int indy = (opcode & 0x00F0) >>> 4;
			switch ((opcode & 0x000F)) {
			case 0: // LD Vx, Vy - VX to the value of VY.
				v[indx] = v[indy];
				break;
			case 1: // OR Vx, Vy - Sets VX to VX or VY.
				v[indx] = (byte) (v[indx] | v[indy]);
				break;
			case 2: // AND Vx, Vy - Sets VX to VX and VY.
				v[indx] = (byte) (v[indx] & v[indy]);
				break;
			case 3: // XOR Vx, Vy - Sets VX to VX xor VY.
				v[indx] = (byte) (v[indx] ^ v[indy]);
				break;
			case 4: // ADD Vx, Vy - Adds VY to VX. VF is set to 1 when there's a carry,
					// and to 0 when there isn't.
			{
				int s = Util.b2i(v[indx]) + Util.b2i(v[indy]);
				if (s > 0xFF) {
					v[0xF] = 0x1;
				} else {
					v[0xF] = 0x0;
				}
				v[indx] = (byte) s;
			}
				break;
			case 5: // SUB Vx, Vy - VY is subtracted from VX. VF is set to 0 when
					// there's a borrow, and 1 when there isn't.
			{
				int s = 0;
				int s1 = Util.b2i(v[indx]);
				int s2 = Util.b2i(v[indy]); 
				if ( s1>= s2) {
					s = s1 - s2;
					v[0xF] = 0x1;
				} else {
					s = 0x100+s1 - s2;
					v[0xF] = 0x0;
				}
				v[indx] = (byte) s;
			}
				break;
			case 6: // SHR Vx {,Vy} - 8XY6 Shifts VX right by one. VF is set to the value of
					// the least significant bit of VX before the shift.[2]
				v[0xF] = (byte)(Util.b2i(v[indx])%2);
				v[indx] = (byte) (Util.b2i(v[indx])/2);
				break;
			case 7: // SUBN Vx, Vy - Sets VX to VY minus VX. VF is set to 0 when there's
					// a borrow, and 1 when there isn't.
			{
				int s = 0;
				int s1 = Util.b2i(v[indy]);
				int s2 = Util.b2i(v[indx]);
				if ( s1 >= s2) {
					s = s1-s2;
					v[0xF] = 0x1;
				} else {
					s = 0x100+s1-s2;
					v[0xF] = 0x0;
				}
				v[indx] = (byte) s;
			}
				break;
			case 0xE: // SHL Vx {, Vy} - 8XYE Shifts VX left by one. VF is set to the value of
						// the most significant bit of VX before the shift.[2]
			{
				v[0xF] = (byte)( (Util.b2i(v[indx]) >>> 7) & 0x1 );

				v[indx] = (byte) (Util.b2i(v[indx]) << 1);
			}
				break;
			}
		}
			pc += 2;
			break;
		case 0x9000: // SNE Vx, Vy - Skips the next instruction if VX doesn't equal VY.
		{
			int indx = (opcode & 0x0F00) >>> 8;
			int indy = (opcode & 0x00F0) >>> 4;
			if (v[indx] != v[indy]) {
				pc += 2;
			}
		}
			pc += 2;
			break;

		case 0xA000: // LD I, NNN - ANNN Set I to NNN
			I = (opcode & 0x0FFF);
			pc += 2;
			break;
		case 0xB000: // JP V0, NNN - BNNN Jumps to the address NNN plus V0.
			pc = (opcode & 0x0FFF) + Util.b2i(v[0]);
			break;
		case 0xC000: // RND Vx, NNN - CXNN Sets VX to a random number and NN.
		{
			int indx = (opcode & 0x0F00) >>> 8;
			v[indx] = (byte) (Util.genInt(0, 256) & (opcode & 0x00FF));
		}
			
			pc += 2;
			break;
		case 0xD000: // DRW Vx, Vy, N - DXYN Sprites stored in memory at location in index
						// register (I), maximum 8bits wide. Wraps around the
						// screen. If when drawn, clears a pixel, register VF is
						// set to 1 otherwise it is zero. All drawing is XOR
						// drawing (e.g. it toggles the screen pixels)
		{
			int indx = (opcode & 0x0F00) >>> 8;
			int indy = (opcode & 0x00F0) >>> 4;
			
			int n = (opcode & 0x000F);
			if (n>0) {
				blitSprite(Util.b2i(v[indx]), Util.b2i(v[indy]), I, I+n);
			} else { // n==0
				// [S-CHIP] draws 16x16 sprites
				if (isExtendedScreen) {
					blitSprite(Util.b2i(v[indx]), Util.b2i(v[indy]), I, I+32);
				}
			}
		}
			pc += 2;
			break;
		case 0xE000: {  
			int indx = (opcode & 0x0F00) >>> 8;
			switch (opcode & 0x00FF) {
			case 0x9E:	// SKP Vx - Skips next instruction if key with the value of Vx is pressed.
				if (keyboard.getKeyboardState()[v[indx]] == 1) { // Pressed
					pc += 2;
				}
				break;
			case 0xA1: //  SKNP Vx - Skip next instruction if key with the value of Vx is not pressed.
				if (keyboard.getKeyboardState()[v[indx]] == 0) { // Not pressed
					pc += 2;
				}
				break;
			}
		}
			pc += 2;
			break;
		case 0xF000: 
		{
			int indx = (opcode & 0x0F00) >>> 8;
			switch (opcode & 0x00FF) {
			case 0x07: // LD Vx, DT - Sets VX to the value of the delay timer.
				v[indx] = (byte)delayTimer;
				break;
			case 0x0A: // LD Vx, K - A key press is awaited, and then stored in VX.
			{
				boolean pressed = false;
				int j = 0;
				if (!awaitingKey_) {
					awaitingKey_ = true;
					kbdmem=keyboard.getKeyboardState();
					pc-=2;
				} else {
					for (; j < kbdmem.length; ++j) {
						if (kbdmem[j] == 1) {
							pressed = true;
							break;
						}
					}
					if (!pressed) {
						pc -= 2;
					} else {
						v[indx] = (byte) (j);
						awaitingKey_ = false;
						resetKbd();
					}		
				}
			}
				break;
			case 0x15: // LD DT, Vx - Sets the delay timer to Vx
				delayTimer = Util.b2i(v[indx]);
				break;
			case 0x18: // LD ST, Vx - Sets the sound timer to VX.
				soundTimer = Util.b2i(v[indx]);
				break;
			case 0x1E: // ADD I, Vx - Sets I = I + Vx. 
				I = (I + Util.b2i(v[indx])) & 0x0FFF;
				break;
			case 0x29: // LD F, Vx - Set I = location of sprite for digit Vx.
				I = digitSpriteStart_+Util.b2i(v[indx])*5;
				break;
			case 0x30: // [S-CHIP] LD HF, Vx - Points I to 10-byte font sprite for digit VX (0..9)
				I = digit10SpriteStart_ +Util.b2i(v[indx])*10; // indx ranges [0,9] 
				break;
			case 0x33: // LD B, Vx
				// The interpreter takes the decimal value of Vx, and places the
				// hundreds
				// digit in memory at location in I, the tens digit at location
				// I+1, and the
				// ones digit at location I+2
			{
				int n = Util.b2i(v[indx]);
				memory_[I + 2] = (byte) ((n % 10) & 0xff);
				n /= 10;
				memory_[I + 1] = (byte) ((n % 10) & 0xff);
				n /= 10;
				memory_[I] = (byte) ((n % 10) & 0xff);
			}
				break;
			case 0x55: // LD [I], Vx - The interpreter copies the values of registers V0
						// through Vx into memory, starting at the address in I.
			{
				for (int j = 0; j <= indx; ++j) {
					memory_[I + j] = v[j];
				}
			}
				break;
			case 0x65: { // LD Vx, [I] - Read registers V0 through Vx from memory starting at location I. 
				for (int j = 0; j <= indx; ++j) {
					v[j] = memory_[I + j];
				}
			}
				break;
			case 0x75: { // [S-CHIP] LD R, Vx - Stores V0..VX in RPL user flags (X <= 7), Maybe could be skipped?
				for (int j = 0; j <= indx; ++j) {
					rplFlags[j] = v[j];
				}
			}
				break;
			case 0x85: { // [S-CHIP] LD Vx, R - Read V0..VX from RPL user flags (X <= 7), Maybe could be skipped? 
				for (int j = 0; j <= indx; ++j) {
					v[j] = rplFlags[j];
				}
			}
				break;				
			}

		}
			pc += 2;
			break;
		}
	}
	@Override
	public void timerCallback(int timerId) {
		// Ignoring timerId 
		if (delayTimer > 0) {
			delayTimer--;
		}
		if (soundTimer > 0) {
			soundTimer--;
			buzzer.buzz();
		}
	}
	@Override
	public void blitGraphics(double delta) {
		if (renderer!=null)
			renderer.draw(graphicsBuffer_, delta, H, V);
	}

}
