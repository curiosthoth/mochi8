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
package com.taibaisoft.chip8.assemblers;
// TODO: Finish this in the future.
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.taibaisoft.chip8.platform.Util;


interface Chip8DasmProgressListener {
	void onDisamProgress(int percentage);
}

public class Disassembler {
	private int srCounter_ = 0;
	private int lbCounter_ = 0;
	private int dtCounter_ = 0;
	private byte[] memory_ = null;
	private List<DasmLine> lines_ = new ArrayList<DasmLine>();
	private int cursor_ = 0;
	private HashMap<Integer, String> subroutineMap_ = new HashMap<Integer,String>();
	private HashMap<Integer, String> jmpLabelMap_ = new HashMap<Integer, String>();
	private HashMap<Integer, String> dataLabelMap_ = new HashMap<Integer, String>();
	private int lastRet = -1;
	/**
	 * 
	 * @param loadedCode Should be the one directly in Chip8 memory.
	 * @param startAddress The starting address used to adjust memory access
	 * @param listener 
	 */
	public void disassemble(byte[] loadedCode, int startAddress, Chip8DasmProgressListener listener) {
		memory_ = loadedCode;
		cursor_ = startAddress;
		lastRet = -1;
		srCounter_ = 0;
		lbCounter_ = 0;
		dtCounter_ = 0;
		subroutineMap_.clear();
		jmpLabelMap_.clear();
		dataLabelMap_.clear();
		// 1st pass, basic dissassembly
		int len = memory_.length;
		for (; cursor_ < len; cursor_ += 2) {
			// If it is among data seg, don't bother
			// Suppose data always comes after code in a seprate data segment
			int opcode = ((memory_[cursor_] & 0xFF) << 8) | (memory_[cursor_ + 1] & 0xFF);
			lines_.add(dis(opcode));
		}
		// 2nd pass, find out data seg and makegood guessing of functions
		// At this point not all stuff in lines_ are valid instructions
		enhance();
		
	}
	private DasmLine dis(int opcode) {
		// Decode and run an opcode
		DasmLine srcLine = new DasmLine();
		String asmText = "";
		
		outmost:
		switch (opcode & 0xF000) {
		/** ---- 0x0000 ---- */
		case 0x0000: {
			if (opcode==0x00E0) {
				asmText = Mnemonics.CLS;
				break outmost;
			} else if (opcode==0x00EE) {
				asmText = Mnemonics.RET;
				// Possible dataStart, negate it later
				// dataStart = cursor;
				srcLine.type = DasmLineType.CODE_RET;
				lastRet = cursor_;
				break outmost;
			} else if (opcode==0x00FB) {
				asmText = Mnemonics.SCR;
				break outmost;
			} else if (opcode==0x00FC) {
				asmText = Mnemonics.SCL;
				break outmost;
			} else if (opcode==0x00FD) {
				asmText = Mnemonics.EXT;
				break outmost;
			} else if (opcode==0x00FE) {
				asmText = Mnemonics.LOW;
				break outmost;
			} else if (opcode==0x00FF) {
				asmText = Mnemonics.HIGH;
				break outmost;
			} else {
				if ( (opcode & 0x00F0)==0x00C0 ) {
					asmText = String.format("%s #%01x", Mnemonics.SCD, opcode & 0xF); 
					break outmost;
				} else {
					asmText = String.format("%s #%03x", Mnemonics.SYS, opcode & 0xFFF);
					break outmost;
				}
			}
		}
		/** ---- 0x1000 ---- */			
		case 0x1000: // JP NNN - Jumps to address NNN.
		{
			int addr = opcode & 0xFFF;
			String label = jmpLabelMap_.get(addr);
			if (label==null) {
				label = nextLabelName();
				jmpLabelMap_.put(addr, label);
			}
			asmText = String.format("%s %s", Mnemonics.JP, label);
			srcLine.type = DasmLineType.CODE_JP;
		}
		break;
		/** ---- 0x2000 ---- */
		case 0x2000: // CALL NNN - Calls subroutine at NNN.
		{
			int addr = opcode & 0xFFF;
			String subroutine = subroutineMap_.get(addr);
			if (subroutine==null) {
				subroutine = nextSubroutineName();
				subroutineMap_.put(addr, subroutine);
			}
			asmText = String.format("%s %s", Mnemonics.CALL, subroutine);
			srcLine.type = DasmLineType.CODE_CALL;
		}
		break;
		case 0x3000: // SE Vx, NN - Skips the next instruction if VX equals NN.
		{
			int index = (opcode & 0x0F00) >>> 8;	
			asmText = String.format("%s V%01X, #%02x", Mnemonics.SE, index, opcode & 0xFF);
			srcLine.type = DasmLineType.CODE_CONDITIONAL_SKIP;
		}
		break;
		case 0x4000: // SNE Vx, NN - Skips the next instruction if VX doesn't equal NN.
		{
			int index = (opcode & 0x0F00) >>> 8;	
			asmText = String.format("%s V%01X, #%02x", Mnemonics.SNE, index, opcode & 0xFF);
			srcLine.type = DasmLineType.CODE_CONDITIONAL_SKIP;
		}
			break;
		case 0x5000: // SE Vx, Vy - Skips the next instruction if VX equals VY.
		{
			int indx = (opcode & 0x0F00) >>> 8;
			int indy = (opcode & 0x00F0) >>> 4;
			asmText = String.format("%s V%01X, V%01X", Mnemonics.SE, indx, indy);
			srcLine.type = DasmLineType.CODE_CONDITIONAL_SKIP;
		}
			break;
		case 0x6000: // LD Vx, NN - Sets VX to NN.
		{
			int indx = (opcode & 0x0F00) >>> 8;
			asmText = String.format("%s V%01X, #%02x", Mnemonics.LD, indx, opcode & 0xFF);
		}
			break;
		case 0x7000: // ADD Vx, NN - Adds NN to VX.
		{
			int indx = (opcode & 0x0F00) >>> 8;
			asmText = String.format("%s V%01X, #%02x", Mnemonics.ADD, indx, (opcode & 0x00FF));
		}
			break;
		case 0x8000: {
			String format = "";
			int indx = (opcode & 0x0F00) >>> 8;
			int indy = (opcode & 0x00F0) >>> 4;
			switch ((opcode & 0x000F)) {
			case 0: // LD Vx, Vy - VX to the value of VY.
				format = Mnemonics.LD + " V%01X, V%01X";
				break;
			case 1: // OR Vx, Vy - Sets VX to VX or VY.
				format = Mnemonics.OR + " V%01X, V%01X";
				break;
			case 2: // AND Vx, Vy - Sets VX to VX and VY.
				format = Mnemonics.AND + " V%01X, V%01X";
				break;
			case 3: // XOR Vx, Vy - Sets VX to VX xor VY.
				format = Mnemonics.XOR + " V%01X, V%01X";
				break;
			case 4: // ADD Vx, Vy - Adds VY to VX. VF is set to 1 when there's a carry,
					// and to 0 when there isn't.
				format = Mnemonics.ADD + " V%01X, V%01X";
				break;
			case 5: // SUB Vx, Vy - VY is subtracted from VX. VF is set to 0 when
					// there's a borrow, and 1 when there isn't.
				format = Mnemonics.SUB + " V%01X, V%01X";
				break;
			case 6: // SHR Vx {,Vy} - 8XY6 Shifts VX right by one. VF is set to the value of
					// the least significant bit of VX before the shift.[2]
				format = Mnemonics.SHR + " V%01X {, V%01X}";
				break;
			case 7: // SUBN Vx, Vy - Sets VX to VY minus VX. VF is set to 0 when there's
					// a borrow, and 1 when there isn't.
				format = Mnemonics.SUBN + " V%01X, V%01X";
				break;
			case 0xE: // SHL Vx {, Vy} - 8XYE Shifts VX left by one. VF is set to the value of
						// the most significant bit of VX before the shift.[2]
				format = Mnemonics.SHL + " V%01X {, V%01X}";
				break;
			}
			asmText = String.format(format, indx, indy);
		}
			break;
		case 0x9000: // SNE Vx, Vy - Skips the next instruction if VX doesn't equal VY.
		{
			int indx = (opcode & 0x0F00) >>> 8;
			int indy = (opcode & 0x00F0) >>> 4;
			asmText = String.format("%s V%01X, V%01X", Mnemonics.SNE, indx, indy);
			srcLine.type = DasmLineType.CODE_CONDITIONAL_SKIP;
		}
			break;

		case 0xA000: // LD I, NNN - ANNN Set I to NNN
		{	
			int addr = opcode & 0x0FFF;
			srcLine.type = DasmLineType.CODE_MEMLOAD_I;
			String dlabel = nextDataName();
			dataLabelMap_.put(addr, dlabel);
			asmText = String.format("%s I, %s", Mnemonics.LD, dlabel);
		}
			break;
		case 0xB000: // JP V0, NNN - BNNN Jumps to the address NNN plus V0.
			asmText = String.format("%s V0, #%03x", Mnemonics.JP, (opcode & 0x0FFF));
			srcLine.type = DasmLineType.CODE_JP;
			break;
		case 0xC000: // RND Vx, NNN - CXNN Sets VX to a random number and NN.
		{
			int indx = (opcode & 0x0F00) >>> 8;
			asmText = String.format("%s V%01X, #%03x", Mnemonics.RND, indx, (opcode & 0x00FF));
		}
			break;
		case 0xD000: // DRW Vx, Vy, N - DXYN Sprites stored in memory at location in index
						// register (I), maximum 8bits wide. Wraps around the
						// screen. If when drawn, clears a pixel, register VF is
						// set to 1 otherwise it is zero. All drawing is XOR
						// drawing (e.g. it toggles the screen pixels)
		{
			int indx = (opcode & 0x0F00) >>> 8;
			int indy = (opcode & 0x00F0) >>> 4;
			asmText = String.format("%s V%01X, V%01X, #%01x", Mnemonics.DRW, indx, indy, (opcode & 0x000F));
		}
			break;
		case 0xE000: {  
			int indx = (opcode & 0x0F00) >>> 8;
			switch (opcode & 0x00FF) {
			case 0x9E:	// SKP Vx - Skips next instruction if key with the value of Vx is pressed.
				asmText = String.format("%s V%01X", Mnemonics.SKP, indx);
				srcLine.type = DasmLineType.CODE_CONDITIONAL_SKIP;
				break;
			case 0xA1: //  SKNP Vx - Skip next instruction if key with the value of Vx is not pressed.
				asmText = String.format("%s V%01X", Mnemonics.SKNP, indx);
				srcLine.type = DasmLineType.CODE_CONDITIONAL_SKIP;
				break;
			}
		}
			break;
		case 0xF000: 
		{
			int indx = (opcode & 0x0F00) >>> 8;
			switch (opcode & 0x00FF) {
			case 0x07: // LD Vx, DT - Sets VX to the value of the delay timer.
				asmText = String.format("%s V%01X, DT", Mnemonics.LD, indx);
				break;
			case 0x0A: // LD Vx, K - A key press is awaited, and then stored in VX.
				asmText = String.format("%s V%01X, K", Mnemonics.LD, indx);
				break;
			case 0x15: // LD DT, Vx - Sets the delay timer to Vx
				asmText = String.format("%s DT, V%01X", Mnemonics.LD, indx);
				break;
			case 0x18: // LD ST, Vx - Sets the sound timer to VX.
				asmText = String.format("%s ST, V%01X", Mnemonics.LD, indx);
				break;
			case 0x1E: // ADD I, Vx - Sets I = I + Vx. 
				asmText = String.format("%s I, V%01X", Mnemonics.ADD, indx);
				break;
			case 0x29: // LD F, Vx - Set I = location of sprite for digit Vx.
				asmText = String.format("%s F, V%01X", Mnemonics.LD, indx);
				break;
			case 0x30: // [S-CHIP] LD HF, Vx - Points I to 10-byte font sprite for digit VX (0..9)
				asmText = String.format("%s HF, V%01X", Mnemonics.LD, indx); 
				break;
			case 0x33: // LD B, Vx (Get BCD code for Vx)
				// The interpreter takes the decimal value of Vx, and places the
				// hundreds
				// digit in memory at location in I, the tens digit at location
				// I+1, and the
				// ones digit at location I+2
				asmText = String.format("%s B, V%01X", Mnemonics.LD, indx);
				break;
			case 0x55: // LD [I], Vx - The interpreter copies the values of registers V0
						// through Vx into memory, starting at the address in I.
				asmText = String.format("%s [I], V%01X", Mnemonics.LD, indx);
				break;
			case 0x65:  // LD Vx, [I] - Read registers V0 through Vx from memory starting at location I. 
				asmText = String.format("%s V%01X, [I]", Mnemonics.LD, indx);
				break;
			case 0x75:  // [S-CHIP] LD R, Vx - Stores V0..VX in RPL user flags (X <= 7), Maybe could be skipped?
				asmText = String.format("%s R, V%01X", Mnemonics.LD, indx);
				break;
			case 0x85:  // [S-CHIP] LD Vx, R - Read V0..VX from RPL user flags (X <= 7), Maybe could be skipped? 
				asmText = String.format("%s V%01X, R", Mnemonics.LD, indx);
				break;				
			}

		}
		break;		
		}
		
		srcLine.bin = opcode;
		srcLine.addr = cursor_;
		srcLine.addr1 = cursor_+1;
		srcLine.asm = asmText;
		return srcLine;
	}
	public List<DasmLine> getDisassembleResult() {
		return lines_;
	}
	public String getDisassembleResultAsString() {
		return getDisassembleResultAsString(false, false);
	}
	
	public String getDisassembleResultAsString(boolean showAddress, boolean showComment) {
		StringBuilder sb = new StringBuilder();
		for (DasmLine line : lines_) {
			if (line.label.length() > 0) {
				sb.append(line.label).append(":").append(Util.NEW_LINE);
			}
			if (showAddress) {
				sb.append(String.format("0x%03x", line.addr)).append(": ");
			}
			if (line.type != DasmLineType.DATA) {
				sb.append('\t').append(line.asm);
			} else {
				sb.append('\t').append(line.data).append(Util.NEW_LINE);
				if (line.label1.length() > 0) {
					sb.append(line.label1).append(":").append(Util.NEW_LINE);
				}
				if (showAddress) {
					sb.append(String.format("0x%03x", line.addr1)).append(": ");
				}
				sb.append('\t').append(line.data1);
			}
			if (showComment && line.comment != null) {
				sb.append(" ;; ").append(line.comment);
			}
			sb.append(Util.NEW_LINE);
		}
		sb.append("END").append(Util.NEW_LINE);
		return sb.toString();
	}
	
	
	private String nextSubroutineName() {
		return String.format("SUB%d", srCounter_++);
	}
	private String nextLabelName() {
		return String.format("LABEL%d", lbCounter_++);
	}
	private String nextDataName() {
		return String.format("DATA%d", dtCounter_++);
	}
	
	/**
	 * Side effect: lines_ are changed.
	 * Nothing to do now.
	 */
	private void enhance() {
		if (lines_!=null && lines_.size()>0) {
			int i = 0;
			int s = lines_.size();
			for ( ; i<s; ++i){
				DasmLine line = lines_.get(i);
				if (subroutineMap_.containsKey(line.addr)) {
					line.label = subroutineMap_.get(line.addr);
				} else if (jmpLabelMap_.containsKey(line.addr)) {
					line.label = jmpLabelMap_.get(line.addr);
				} else {
					
				}
				// For data
				if (line.addr <= lastRet) {
					if (dataLabelMap_.containsKey(line.addr)) {
						line.type = DasmLineType.DATA;
						line.label = dataLabelMap_.get(line.addr);
						line.data = String.format("DB #%02X",
								(line.bin >>> 8) & 0xFF);
						line.data1 = String.format("DB #%02X", line.bin & 0xFF);
						line.comment = "data?";

					}
					if (dataLabelMap_.containsKey(line.addr1)) {
						line.type = DasmLineType.DATA;
						line.label1 = dataLabelMap_.get(line.addr1);
						line.data = String.format("DB #%02X",
								(line.bin >>> 8) & 0xFF);
						line.data1 = String.format("DB #%02X", line.bin & 0xFF);
						line.comment = "data?";
					}
				} else {
					line.comment = "data?";
					line.type = DasmLineType.DATA;
					line.data = String.format("DB #%02X",
							(line.bin >>> 8) & 0xFF);
					line.data1 = String.format("DB #%02X", line.bin & 0xFF);
					if (dataLabelMap_.containsKey(line.addr)) {
						line.label = dataLabelMap_.get(line.addr);

					}
					if (dataLabelMap_.containsKey(line.addr1)) {
						line.label1 = dataLabelMap_.get(line.addr1);
					}
				}

			} // End of for loop
		}
	}
	
	
	public static void main(String[] args) {
		String ss = "C:/Users/jbian/workspace/mochi8/CHIP8/GAMES/15PUZZLE";
		//String ss = "D:/workspace/projects/mochi8/CHIP8/GAMES/UFO";
		Disassembler d = new Disassembler();
		int startAddress = 0x200;
		byte[] loadedCode;
		try {
			
			byte[] code = Files.readAllBytes(Paths.get(ss));
			int len = (code==null?0:code.length);
			loadedCode = new byte[len + startAddress];
			System.arraycopy(code, 0, loadedCode, startAddress, len);
			d.disassemble(loadedCode, startAddress, null);
			System.out.println( d.getDisassembleResultAsString(false, true) );
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}
}