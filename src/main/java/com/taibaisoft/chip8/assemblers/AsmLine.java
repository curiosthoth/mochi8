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


public class AsmLine {
	public static short UNINITIALIZED = -1;
	/** 
	 * Final translated binary code.
	 */
	public short bin = 0x00;		
	/** 
	 * Final translated binary data, if any. If this field is not NULL, it will 
	 * override the bin field and considered as data.
	 */
	public byte[] data = null;		
	/** 
	 * Final offset of this corresponding source line IN CODE BINARY, 
	 * without rebasing (without adding startAddress_). Leaves as -1 
	 * if this line is directive line with no actual code generation. 
	 */
	public int offset = UNINITIALIZED;	
	
	/** 
	 * If this line aligns at word boundary, only available when offset is NOT -1.
	 */
	public boolean align = true; 
	
	/**
	 * For storing broken-down tokens temporarily.
	 */
	public String[] tokens = null;
	
	/**
	 * Source code line number in source file. Used for tracking errors etc.
	 */
	public int srcLineNo = UNINITIALIZED; 		
	
	/** 
	 * If translated, assembleOnLine() method will simply ignore this line.
	 * Marks if this line is already generated the binary code.
	 */
	public boolean translated = false; 
	
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (data==null) {
			sb.append(String.format("(%03X): %04X ; ", offset, bin));
			for (String s : tokens) {
				sb.append(s).append(" ");
			}
			sb.append("(").append(srcLineNo).append(")");
		} else {
			sb.append(String.format("(%03X): ", offset));
			for (byte b : data) {
				sb.append(String.format("%02X ", b));
			}
			sb.append(" ; Data");
			sb.append(" (").append(srcLineNo).append(")");
		}
		return sb.toString();
	}
}
