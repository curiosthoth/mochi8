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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

class TokenizedLine {
	public String[] tokens = null;
	public int lineNo = -1;
}

public class Tokenizer {

	public static final char CHAR_QUOTE = '\'';
	public static final char CHAR_COMMA = ',';
	public static final char CHAR_EQU_SIGN = '=';
	public static final char CHAR_COMMENT_START = ';';
	public static final char CHAR_LABEL_SUFFIX = ':';
	public static final String S_EQU = "EQU";
	public static final String S_LABEL_SUFFIX = ":";

	
	private List<AsmError> errors_ = new ArrayList<AsmError>();
	
	public List<TokenizedLine> tokenize(String input) {
		errors_.clear();
		if (input==null || input.length()==0) {
			return null;
		}
		
		// 1. Split into lines
		int lineNum = 0;
		String asciiStr = new String(input.getBytes(), Charset.forName("ASCII"));
		int srcLen = asciiStr.length();
		/** Splits at \r\n or single \r or single \n*/
		List<TokenizedLine> tokenizedLineList = new ArrayList<TokenizedLine>();
		StringBuilder tmp = new StringBuilder();
		for (int i = 0;i<srcLen;++i) {
			char c = asciiStr.charAt(i);
			if (i<srcLen-1) {
				if (c=='\r' && asciiStr.charAt(i+1)=='\n') {
					++i;
					lineNum++;
					
					TokenizedLine tline = new TokenizedLine();
					tline.lineNo = lineNum;
					tline.tokens = tokenizeOneLine(tmp, tline.lineNo);
					tokenizedLineList.add(tline);
					
					tmp = new StringBuilder();
					continue;
				}
			}
			if (c=='\r' || c=='\n') {
				lineNum++;
				
				TokenizedLine tline = new TokenizedLine();
				tline.lineNo = lineNum;
				tline.tokens = tokenizeOneLine(tmp, tline.lineNo);
				
				tokenizedLineList.add(tline);
				
				tmp = new StringBuilder();
				continue;
			}
			tmp.append(c);
		}
		
		if (tmp.length()>0) {
			lineNum++;
			TokenizedLine tline = new TokenizedLine();
			tline.lineNo = lineNum;
			tline.tokens = tokenizeOneLine(tmp, tline.lineNo);
			tokenizedLineList.add(tline);
		}
		return tokenizedLineList;
	}
	public List<AsmError> getLastErrors() {
		return errors_;
	}
	/**
	 * Line formats
	 * a) [LABEL :] INST/DIRECTIVE EXPR1, EXPR2, ... EXPRN
	 * b) [LABEL :] VAR EQU/= EXPR
	 * First token is either an Identifier, or a Label or an Instruction
	 * followed by delimeters =, :, (white chars) respectively.
	 * 
	 * Delimeters: WHITESPACE, = and ','
	 * @param sb
	 * @param lineNo
	 * @return
	 */
	protected String[] tokenizeOneLine(StringBuilder sb, int ln) {
		List<String> tokens = new ArrayList<String>();
		boolean inStr = false;
		boolean metWS = false;
		boolean labelEncountered = false;
		int posToStopWSDelim = 1;
		int i = 0;
		int len = sb.length();
		StringBuilder token = new StringBuilder();
		
		for ( ;i<len;++i){	
			char c = sb.charAt(i);
			
			if (i<len-1) {
				if ( isQuote(c) && isQuote(sb.charAt(i+1)) ) {
					token.append(CHAR_QUOTE);
					++i;
					continue;
				}
			}
			
			if (!inStr) {
				if (isCommentStart(c)) break;
				if (isWhitespace(c)) {
					// Met first whitespace after a non-whitespace sequence.
					if (!metWS) {
						metWS = true;
						if (labelEncountered) {
							posToStopWSDelim = 3;
							labelEncountered = false;
						}
						if (token.length()>0) {
							if ( ( tokens.size()<posToStopWSDelim && token.length() > 0) ) {
								tokens.add(token.toString());
								token = new StringBuilder();
							}
						}
					}
					// Whitespace never counts outside string literals
					continue;
				} else {
					metWS = false;
					if (isQuote(c)) {
						inStr = true;
						continue;
					}
					if (isLabelSuffix(c)) {
						if (token.length()>0) {
							tokens.add(token.toString());
							token = new StringBuilder();
						}
						tokens.add(S_LABEL_SUFFIX);
						labelEncountered = true;
						continue;
					}
					if (isEqualAssignmentSign(c)) {
						if (token.length()>0) {
							tokens.add(token.toString());
							token = new StringBuilder();
						}
						tokens.add(S_EQU);
						continue;
					}
					if (c==CHAR_COMMA) {
						if (token.length()>0) {
							tokens.add(token.toString());
							token = new StringBuilder();
						}
						continue;
					}
					if (c=='.') {
						c = '0';
					}
					if (Character.isAlphabetic(c)) {
						c = Character.toUpperCase(c);
					}
					token.append(c);
				}
			} else {
				if (c==CHAR_QUOTE) {
					inStr = false;
					tokens.add(token.toString());
					token = new StringBuilder();
					continue;
				}
				token.append(c);
			}
		}
		if (token.length()>0) {
			tokens.add(token.toString());
		}
		return tokens.toArray(new String[0]);
	}
	
	private boolean isWhitespace(char c) {
		return c==' ' || c=='\t';
	}
	private boolean isCommentStart(char c) {
		return c==CHAR_COMMENT_START;
	}
	private boolean isEqualAssignmentSign(char c) {
		return c==CHAR_EQU_SIGN;
	}
	private boolean isLabelSuffix(char c) {
		return c==CHAR_LABEL_SUFFIX;
	}
	private boolean isQuote(char c) {
		return c==CHAR_QUOTE;
	}
	
	public static void main(String[] args) {
		
		Tokenizer tk = new Tokenizer();
		String input;
		try {
			// input = new String(Files.readAllBytes(Paths.get("tests/test04.asm")) , Charset.forName("ASCII"));
			input = new String(Files.readAllBytes(Paths.get("tests/test05.asm")) , Charset.forName("ASCII"));
			// input = new String(Files.readAllBytes(Paths.get("CHIP8/SGAMES/SOURCES/BLINKY.SRC")) , Charset.forName("ASCII"));
			List<TokenizedLine> all = tk.tokenize(input);
			for (TokenizedLine p : all) {
				System.out.print(p.lineNo + "  --  ");
				for (String m : p.tokens) {
					System.out.print(m +";");
				}
				System.out.println();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

	}
}
