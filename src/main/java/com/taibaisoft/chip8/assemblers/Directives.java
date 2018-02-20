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

public class Directives {
	// Prefix directives
	public final static String ALIGN = "ALIGN";
	public final static String DA = "DA";
	public final static String DB = "DB";
	public final static String DEFINE = "DEFINE";
	public final static String DS = "DS";
	public final static String DW = "DW";
	public final static String ELSE = "ELSE";
	public final static String END = "END";
	public final static String ENDIF = "ENDIF";
	public final static String IFDEF = "IFDEF";
	public final static String IFUND = "IFUND";
	public final static String INCLUDE = "INCLUDE";
	public final static String OPTION = "OPTION";
	public final static String ORG = "ORG";
	public final static String UNDEF = "UNDEF";
	public final static String USED = "USED";
	public final static String XREF = "XREF";
	
	// Sub directive/values
	public final static String S_NO = "NO";
	public final static String S_YES = "YES";
	public final static String S_OFF = "OFF";
	public final static String S_ON = "ON";
	public final static int S_TRUE_I = 1;
	public final static int S_FALSE_I = 0;
	
	// Infix directives
	// Expression evaluation only happens to these two directives.
	public final static String I_EQU = "EQU";
	public final static String I_EQU_SIGN = "=";
	
}