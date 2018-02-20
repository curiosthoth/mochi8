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

public class AsmError {
	public enum AsmErrorLevel {
		ERROR, 
		WARNING,
	}
	
	public int lineNumber = -1;
	public int errorCode = 0;
	public String errorMessage = "";
	public AsmErrorLevel errorLevel = AsmErrorLevel.ERROR;
	
	public static final int E_WRONG_TYPE_OPERAND = 2;
	public static final int E_WRONG_NUM_ARGS = 3;
	public static final int E_WRONG_NUMERIC_VALUE = 4;
	public static final int E_WRONG_ARG_TYPE_NEED_V = 5;
	public static final int E_WRONG_VARIABLE_NAME = 6;
	public static final int E_UNDEFINED_VARIABLE = 7;
	public static final int E_DIRECTIVE_ERROR = 8;
	public static final int E_INSTRUCTION_NONCOMPLIANT = 9;
	public static final int E_DEPRECATED = 10;
	public static final int E_EXPRESSION_EVAL = 12;
	public static final int E_NUMBER_OUT_OF_RANGE = 13;
	public static final int E_RESERVED_KEYWORD = 14;
	public static final int E_FILE_NOT_FOUND = 15;
	public static final int E_FILE_IO_ERROR = 16;
	public static final int E_PREPROCESS_WRONG_DIRECTIVE = 20;
	public static final int E_PREPROCESS_UNBALANCED_IF = 21;
	public static final int E_PREPROCESS_SYMBOL = 22;
	public static final int E_UNUSED_SYMBOL = 23;
	public static final int E_SYNTAX = 24;
	
	private String[] msgs_ = {
	/*0*/   "", 
			"",
			"Wrong type of operand.",
			"Wrong number of arguments.",
			"The numeric argument is either malformatted or out of range.",
	/*5*/	"The argument type is wrong, should be VN.",
			"The variable/label name is not valid.",
			"Undefined variable.",
			"Directive value or format error.",
			"This instruction does not comply with current mode.",
	/*10*/	"This instruction is deprecated.",
			"The label name is invalid.",
			"Error evaluating expression.",
			"Number out of range.",
			"Use of reserved keywords as label or variable name.",
	/*15*/	"File not found.",
			"File IO error.",
			"",
			"",
			"",
	/*20*/	"Wrong preprocess directive.",
			"Unbalanced condition checks.",
			"Preprocessing symbole error.",
			"Unused symbol.",
			"Syntax error."
	};
	
	public AsmError(int code, String extra, int line) {
		this(code, extra, line, AsmErrorLevel.ERROR);
	}
	
	public AsmError(int code, String extra, int line, AsmErrorLevel level) {
		errorCode = code;
		errorMessage = msgs_[code] + " " + extra;
		lineNumber = line;
		errorLevel = level;
	}
	
	public String toString() {
		return String.format("[C%03d][Line %d] %s ", errorCode, lineNumber, errorMessage);
	}
}
