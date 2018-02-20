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

enum DasmLineType {
	NORMAL,
	CODE_JP,			   // For JP
	CODE_CONDITIONAL_SKIP, // For SKP, SKNP, SE, SNE, 
	CODE_CALL,             // For CALL
	CODE_RET,
	CODE_MEMLOAD_I,			// For LD I, NNN, loading address value into I
	DATA,
}

public class DasmLine {
	public String asm;
	public String data;
	public String data1;
	public int bin;
	public String label = "";
	public String label1 = "";	// Only makes sense for data 
	public int addr;
	public int addr1;			// Only makes sense for data 
	public String comment;
	public DasmLineType type = DasmLineType.NORMAL; 
}
