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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;

import com.taibaisoft.chip8.platform.Util;

/**
 * @author Jeffrey Bian
 * A Christian Egeberg's CHIPPER 2.11 compliant assembler
 */

public class Assembler {

	enum OutputMode {
		BINARY,
		HEXSTRING,
	}
	
	public static final char TK_DELIM_01 = ' ';
	public static final char TK_DELIM_02 = '\t';
	public static final char TK_DELIM_03 = ',';
	public static final char CHAR_QUOTE = '\'';
	public static final char TK_DELIM_04 = '='; // Well! This is not really a delim
	public static final char CHAR_COMMENT_START = ';';
	private static final int MAX_PASSES = 2;
	private static final int FIRST_PASS = 0;
	private static final int SECOND_PASS = 1;
	private static final int MAX_SYMBOL_RESOLUTION_PASSES = 32;
	private static final int lastPass_ = MAX_PASSES - 1; 
	private static final int S_IF = 1;
	private static final int S_ELSE = 2;
	private static final int S_ENDIF = -1;
	private static final int MAX_MEM_SIZE = 4096;
	
	private static String[] reservedKeywords_ = new String[] {
		Mnemonics.CLS, Mnemonics.RET, Mnemonics.SCR, Mnemonics.SCL, Mnemonics.EXT, 
		Mnemonics.LOW, Mnemonics.HIGH, Mnemonics.SCD, Mnemonics.SYS, Mnemonics.JP, 
		Mnemonics.CALL, Mnemonics.SE, Mnemonics.SNE, Mnemonics.LD, Mnemonics.ADD, 
		Mnemonics.OR, Mnemonics.AND, Mnemonics.XOR, Mnemonics.SUB, Mnemonics.SHR, 
		Mnemonics.SUBN, Mnemonics.SHL, Mnemonics.RND, Mnemonics.DRW, Mnemonics.SKP, 
		Mnemonics.SKNP, Mnemonics.DT, Mnemonics.ST, Mnemonics.K, Mnemonics.I, Mnemonics.V, 
		
		Directives.ALIGN, Directives.DA, Directives.DB, Directives.DEFINE, Directives.DS,
		Directives.DW, Directives.ELSE, Directives.END, Directives.ENDIF, Directives.I_EQU,
		Directives.IFDEF, Directives.IFUND, Directives.INCLUDE, Directives.OPTION, 
		Directives.ORG, Directives.S_NO, Directives.S_YES, Directives.S_OFF, Directives.S_ON,
		Directives.UNDEF, Directives.USED, Directives.XREF,
		
		"V0", "V1", "V2", "V3", "V4", "V5", "V6", "V7", "V8", "V9", 
		"VA", "VB", "VC", "VD", "VE", "VF",		
	}; 
	
	@SuppressWarnings("unused")
	private int lineCounter_ = 0;
	private boolean wordAlign_ = true;
	private boolean skip_ = false;
	@SuppressWarnings("unused")
	private OutputMode outputMode_ = OutputMode.BINARY;
	private boolean warnUnusedSymbols_ = true; 
	private boolean autoUseSymbols_ = false;
			
	private Stack<Stack<Integer>> ifStackStack_ = new Stack<Stack<Integer>>();

	private HashMap<String, Integer> symbolTable_ = new HashMap<String, Integer>();
	private HashMap<String, Integer> symbolLineNumMap_ = new HashMap<String, Integer>();
	private List<AsmLine> lines_ = new ArrayList<AsmLine>();
	private int startAddress_ = 0x200;
	private int currentOffset_ = 0;	/* Offset pointer in bytes */
	private int currentPass_ = 0;
	private List<AsmError> errors_ = new ArrayList<AsmError>();
			
	private ExpressionParser ep_ = new ExpressionParser();
	private Tokenizer tk_ = new Tokenizer();
	
	/**
	 * A map for storing unresolved expressions, indexed by its corresponding source line number.
	 * Value is the expression string and the current assembly memory address value pair.
	 */
	private HashMap<String, String> unresolved_ = new HashMap<String, String>();
	
	public Assembler() {
		
	}
	public void init(int startAddress) {
		wordAlign_ = true;
		skip_ = false;
		ifStackStack_.clear();
		symbolTable_.clear();
		lines_.clear();
		unresolved_.clear();
		currentPass_ = 0;
		currentOffset_ = 0;
		lineCounter_ = 0;
		startAddress_ = startAddress;
		symbolLineNumMap_.clear();
		ep_.init();
		ifStackStack_.clear();
	}
	public byte[] assemble(String input, int startAddress) {
		Locale defaultLc = Locale.getDefault();
		Locale.setDefault(Locale.ENGLISH);
		
		byte[] binfinal = null;
		
		if (input==null) {
			return null;
		}
		
		try {
			// 0. Reinitialize 	
			init(startAddress);
		
			// 1. Preprocess,
			// - Tokenize
			// - Fillup lines_ arraylist 
			// - Deals with include preprocessor, which is special.
			preprocess(input);
			
			// 2. Iterates each asm line and try filling up bin field.
			int len = lines_.size();
			boolean stopError = false;
			do {
				boolean fulfilled = true;
				if (currentPass_==SECOND_PASS) {
					// Before second pass, try resolving all the unresolved.
					resolveAllIntermediateExpr();
				}
				for (int i = 0; i<len ; ++i) {
					AsmLine tl = lines_.get(i);
					if (!tl.translated) {
						assembleOneLine(tl);
						if ( fulfilled ) {
							fulfilled = false;
						}
					}
				}

				currentPass_++;
				/*
				// Find any stop errors
				if (errors_.size()>0) {
					for (AsmError e : errors_) {
						if (e.errorLevel==AsmError.AsmErrorLevel.ERROR) {
							stopError = true;
							break;
						}
					}
				}
				*/
				if (fulfilled || stopError) {
					break;
				}
			} while(currentPass_<MAX_PASSES);
			
			// 3. Collect further warnings, like unused symbols
			if (warnUnusedSymbols_) {
				for(String v : symbolTable_.keySet()) {
					if (!ep_.getUsedSymbolSet().contains(v) ) {
						int lineNo = -1;
						if ( symbolLineNumMap_.containsKey(v) ) {
							lineNo = symbolLineNumMap_.get(v);
						}
						errors_.add(new AsmError(AsmError.E_UNUSED_SYMBOL, v, lineNo, AsmError.AsmErrorLevel.WARNING));
					}
				}
				
			}
			
			for (AsmError e : errors_) {
				System.err.println(e.toString());
			}
			
			// 4. Encode memory addresses.
			if (!stopError) {
				// Note: BIG endian
				binfinal = encodeBinary();
				//printBinData(binfinal, false);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			Locale.setDefault(defaultLc);
		}
		return binfinal;
	}
	
	protected byte[] encodeBinary() throws Exception {
		ByteBuffer bb = ByteBuffer.allocate(4096);
		bb.order(ByteOrder.BIG_ENDIAN);
		
		for (AsmLine li : lines_) {
			if (li.offset!=-1) {
				if (li.offset<0 || li.offset>MAX_MEM_SIZE-2) {
					throw new Exception("Offset out of range");
				}				
				bb.position(li.offset);
				if (li.data==null) {
					bb.putShort(li.bin);
				} else {
					bb.put(li.data);
				}
			}
		}
		int p = bb.position();
		return Arrays.copyOf(bb.array(), p);
	}
	public void outputLastAssembledIntermediate() {
		for (AsmLine li : lines_) {
			if (li.offset!=-1)
				System.out.println(li.toString());
		}
	}
	
	public static void printBinData(byte[] data, boolean compact) {
		if (data!=null) {
			StringBuilder sb = new StringBuilder();
			int c = 0;
			for (byte b : data) {
				sb.append(String.format("%02X", b));				
				if (!compact) {
					c++;
					if (c%2==0) {
						sb.append(' ');
					}
					if (c==8) {
						sb.append(Util.NEW_LINE);
						c = 0;
					}
				}
			}
			System.out.println(sb.toString());
		}
	}
	/**
	 * Utility method used by assembleOneLine().
	 * @param int  
	 * @param int
	 * @return
	 */
	private boolean isNumberInRange(int number, int nPowerOfTwo) {
		return nPowerOfTwo >= 0 && nPowerOfTwo <= 32 && number >= 0 && number < (int) Math.pow(2, nPowerOfTwo);
				
	}
	
	/**
	 * Utility method used by assembleOneLine().
	 * Checks if the given parameter is in form VX. If so,
	 * returns boolean as first and extracts the X value in
	 * second field.
	 *  
	 * @param s
	 * @return
	 */
	private Pair<Boolean, Integer> isVxVy(String s) {
		Pair<Boolean, Integer> p = new Pair<Boolean, Integer>(false, -1);
		if (s!=null && s.length()==2) {
			char c = s.charAt(1);
			if ( s.charAt(0)=='V' && 
					(c=='1' || c=='2' || c=='3' ||
					c=='4' || c=='5' || c=='6' ||
					c=='7' || c=='8' || c=='9' ||
					c=='0' || c=='A' || c=='B' ||
					c=='C' || c=='D' || c=='E' || 
					c=='F')) {
					p.first = true;
					p.second = (c<58?c-48:c-55);
			}
		}
		return p;
	}
	
	private boolean checkReservedWord(String s, int lineNo) {
		// By pass null checks
		boolean r = false;
		for (String w : reservedKeywords_) {
			if (w.compareToIgnoreCase(s)==0) {
				r = true;
				break;
			}
		}
		if (r) {
			errors_.add(new AsmError(AsmError.E_RESERVED_KEYWORD, "(" + s + ")", lineNo));
		}
		return r;
	}
	/**
	 * Utility method used by assembleOneLine().
	 * @param vname
	 * @return
	 */
	private boolean checkVarNameValid(String vname, String additionalMsg, int lineNo) {
		boolean f = true;
		if (vname!=null) {			
			int len = vname.length();
			for (int i = 0;i < len; ++i) {
				char c = vname.charAt(i);
				if ( (c>=48 && c<= 57) || (c >= 65 && c<=90) || c=='_' ) {
					
				} else {
					f = false;
					break;
				}
			}
		} else {
			f = false;
		}
		if (!f) {
			errors_.add(new AsmError(AsmError.E_WRONG_VARIABLE_NAME, additionalMsg, lineNo));
		}
		return f;
	}
	/**
	 * Utility method used by assembleOneLine().
	 * @param oName
	 * @param hpos
	 * @param count
	 * @param min
	 * @param max
	 * @param lineNo
	 * @return
	 */
	private boolean checkNumOfArgs(String oName, int hpos, int count, int min, int max, int lineNo) {
		boolean f = true;
		if ( hpos<count-1-max || hpos>count-1-min ) {
			// Missing operands or too many operands
			errors_.add(new AsmError(AsmError.E_WRONG_NUM_ARGS, "("+oName+")", lineNo));
			f = false;
		}
		return f;
	}
	/**
	 * Tries to parse the given expr and if succeeded getting a number, test if the number is in range [0, 2^nPowerOfTwo).
	 * 
	 * @param oName
	 * @param expr
	 * @param nPowerOfTwo
	 * @param lineNo
	 * @return
	 */
	private Pair<Boolean, Integer> checkOperandAndRange(String oName, String expr, int nPowerOfTwo, int lineNo) {
		Pair<Boolean, Integer> p = ep_.parse(expr, symbolTable_);
		if (!p.first) {
			if (currentPass_ == lastPass_) {
				errors_.add(new AsmError(
						AsmError.E_UNDEFINED_VARIABLE, "(" +oName+ ")",
						lineNo));
			}
		} else {
			if (!isNumberInRange(p.second, nPowerOfTwo)) {
				errors_.add(new AsmError(AsmError.E_NUMBER_OUT_OF_RANGE, "(" +oName+ ")" + " " + p.second , lineNo));
				p.first = false;
			}
		}
		return p;
	}
	/*
	 * Worst O(n*n), not good.
	 */
	protected void resolveAllIntermediateExpr() {
		int x = 0;
		do {
			Iterator<Map.Entry<String,String>> iter = unresolved_.entrySet().iterator();
			while (iter.hasNext()) {
			    String uvar = iter.next().getKey();
			    if ( resolveIntermediateExpr(uvar) ) {
			    	iter.remove();
			    }
			}
			if (x>0) {
				break;
			}
			x++;
		} while( !unresolved_.entrySet().isEmpty() );
		/*
		if (!unresolved_.entrySet().isEmpty()) {
			StringBuilder sb = new StringBuilder("Unresolved expressions: ");
			for (String s : unresolved_.keySet()) {
				sb.append(s).append(", ");
			}
			System.err.println(sb.toString());
		}
		*/
	}
	protected boolean resolveIntermediateExpr(String i) {
		// To save some computations, first see if in symbolTable_
		if ( symbolTable_.containsKey(i) ) {
			return true;
		} else {
			String expr = unresolved_.get(i);
			if (expr!=null) {
				Pair<Boolean, Integer> p = ep_.parse(expr, symbolTable_);
				if (p.first) {
					symbolTable_.put(i, p.second);
				}
				return p.first;
			} else {
				return true;
			}
		}
	}
	
	private boolean checkIfBalance(int op, String msg, int lineNo) {
		boolean t = true;
		String po = "";
		if (ifStackStack_.empty() || op==S_IF) {
			ifStackStack_.push(new Stack<Integer>());
		} 
		
		Stack<Integer> ifStack = ifStackStack_.peek();

		if (ifStack.empty()) {
			if (op == S_IF) {
				ifStack.push(op);
			} else {
				t = false;
			}
		} else {
			if (op == S_ENDIF) {
				ifStack.pop();
				if (!ifStack.empty()) {
					t = false;
				} else {
					ifStackStack_.pop();
				}
			} else if (op == S_ELSE) {
				if (ifStack.peek() != S_IF) {
					t = false;
				}
			} else {
				// S_IF
				ifStack.push(op);
			}
		}
		
		if (!t) {
			errors_.add(new AsmError(AsmError.E_PREPROCESS_UNBALANCED_IF, msg + po, lineNo));
		}
		return t;
	}

	/**
	 * Preprocess side effect: lines_ will be populated after this method called.
	 * @param input
	 * @throws Exception 
	 */
	protected void preprocess(String input) throws Exception {
		// Fill up lines_
		// Note there will be BLANK lines, with a zero-lengthed String[] as tokens.
		List<TokenizedLine> srcTokenArrayList = tk_.tokenize(input);
		
		if (!tk_.getLastErrors().isEmpty()) {
			errors_.addAll(tk_.getLastErrors());
			return;
		}
		
		if (srcTokenArrayList!=null) {
			for (TokenizedLine p : srcTokenArrayList) {
				//++lineCounter_; // TODO: Use of line counter
				if (p.tokens!=null&&p.tokens.length>0) {
					AsmLine asmLine = new AsmLine();
					// srcLineNo is used for tracking the correct line number in THAT file.
					// lineCounter_ is used to track the combined final line number
					asmLine.srcLineNo = p.lineNo; 
					//TODO : Need to add an offset here for the line number for include
					//System.out.println(asmLine.srcLineNo + ": " + srcLine);
					asmLine.tokens = p.tokens;
					
					lines_.add(asmLine);
					// TODO:  If INCLUDE, don't put it in lines_ and insert file directly.
					// TODO:  NEED to test IFDEF/IFUND/ELSE/ENDIF too to correctly generate the line numbers!
					// Move these directives up here in preprocessing 
					//if (asmLine.tokens[0]) {
						
					//}
					
					if (asmLine.tokens.length>0) {
						// TODO: Use while() and hpos to detect any labels before these directives...
						// and split the label into a single new line.
						/*
						switch (asmLine.tokens[0]) {
						case Directives.INCLUDE:
							if (checkNumOfArgs(Directives.INCLUDE, hpos, count, 1, 1,
									asmLine.srcLineNo)) {
								asmLine.offset = -1;
								// TODO: Study this, do we want to preserver cases or use lower case only
								String fname = asmLine.tokens[hpos + 1].toLowerCase(); 
								Path file = Paths.get(fname);
								if (file.toFile().exists()) {
									boolean ioError = false;
									String input2 = "";
									try {
										input2 = new String(Files.readAllBytes(file),
												Charset.forName("ASCII"));
										asmLine.translated = true;
									} catch (IOException e) {
										errors_.add(new AsmError(
												AsmError.E_FILE_IO_ERROR,
												"Error reading " + fname,
												asmLine.srcLineNo));
										ioError = true;
									}
									if (!ioError) {
										preprocess(input2);
									}
								} else {
									errors_.add(new AsmError(AsmError.E_FILE_NOT_FOUND,
											"" + fname, asmLine.srcLineNo));
								}
							}
							break;
						default:
							lines_.add(asmLine);
						}
						*/
					}
				}
			}
		} else {
			throw new Exception("Empty input data.");
		}
	}
	
	private String minglePreprocessingDefName(String nameText) {
		return "p::" + nameText;
	}
	/**
	 * Core method of the assembler
	 * @param int line An AsmLine object.
	 * @param int pass Current pass indicator. 
	 * @return
	 */
	protected void assembleOneLine(/*IN, OUT*/AsmLine line) {
		
		int incr = 2; // Default increment is 2.
		// First pass specific handling.
		// 1. Only increment currentOffset_ and set it to line offset during first pass!
		// 2. Only put in current address ? symbol in table at first pass
		if ( currentPass_==FIRST_PASS ) {
			line.offset = currentOffset_;
			// Adjust symbol table for current address
			symbolTable_.put("?", currentOffset_ + startAddress_); 
			line.align =  wordAlign_;
		}
		
		
		String[] tokens = line.tokens; 
		int count = tokens.length;
		int hpos = 0;
		boolean doParse = true;
		lineProcessing:
		while( doParse ) {
			doParse = false;
			if (hpos==count) {
				// TODO: Study this conditional more carefully
				// If it is enough for "Label only"?
				line.translated = true;
				incr = 0;
				line.offset = AsmLine.UNINITIALIZED;
				break;
			}
			String head = tokens[hpos];
			

			if (head.compareTo(Directives.IFDEF)!=0 && 
					head.compareTo(Directives.IFUND)!=0 && 
					head.compareTo(Directives.ELSE)!=0 &&
					head.compareTo(Directives.ENDIF)!=0) {
					line.translated = skip_;
				}
			if (line.translated) {
				incr = 0;
				line.offset = -1;
				continue lineProcessing;
			}
			
			
			switch (head) 
			{
			// Java 7 and up, string switches
			// Mnemonics
			case Mnemonics.ADD :
				do {
					/**
					 * FX1E ADD I, VX ; Set I = I + VX 
					 * 7Xkk ADD VX, Byte ; Set VX = VX + Byte 
					 * 8XY4 ADD VX, VY
					 */
					if (!checkNumOfArgs(Mnemonics.ADD, hpos, count, 2, 2, line.srcLineNo)) break;
					Pair<Boolean, Integer> p = isVxVy(tokens[hpos+1]);
					if (p.first) {
						int x = p.second;
						p = isVxVy(tokens[hpos+2]);
						if (p.first) {
							int y = p.second;
							line.bin = (short)(0x8004 | x<<8 | y<<4);
						} else {
							p =  checkOperandAndRange(Mnemonics.ADD, tokens[hpos+2], 8, line.srcLineNo);
							if (!p.first) break;
							line.bin = (short)(0x7000 | x<<8 | (p.second & 0xFF) );
						}
					} else {
						if ( tokens[hpos+1].compareTo(Mnemonics.I)==0 ) {
							p = isVxVy(tokens[hpos+2]);
							if (!p.first) break;
							line.bin = (short)(0xF01E | p.second<<8);
						} else {
							errors_.add(new AsmError(AsmError.E_WRONG_TYPE_OPERAND, "LD I, Vx", line.srcLineNo));
							break;
						}
					}
					line.translated = true;
				} while(false);
				break;
			case Mnemonics.AND :
			case Mnemonics.OR :
			case Mnemonics.XOR :
				/** 
				 * 8xy2 - AND Vx, Vy 
				 * 8xy1 - OR Vx, Vy
				 * 8xy3 - XOR Vx, Vy
				 */
				do {
					if (!checkNumOfArgs(tokens[hpos], hpos, count, 2, 2, line.srcLineNo)) break;
					Pair<Boolean, Integer> p1 = isVxVy(tokens[hpos+1]);
					Pair<Boolean, Integer> p2 = isVxVy(tokens[hpos+2]);
					if ( !p1.first || !p2.first ) {
						errors_.add(new AsmError(AsmError.E_WRONG_TYPE_OPERAND, tokens[hpos] + " Vx, Vy", line.srcLineNo));
						break;
					}
					
					int xhead = 0x8002;
					if ( tokens[hpos].compareTo(Mnemonics.OR)==0 ) {
						xhead = 0x8001;
					} else if ( tokens[hpos].compareTo(Mnemonics.XOR)==0 ) {
						xhead = 0x8003;
					} 
					
					line.bin = (short)(xhead | p1.second<<8 | p2.second<<4);
					line.translated = true;
				} while(false);
				break;
			case Mnemonics.CALL :
				/** 2nnn - CALL nnn */
				do {
					if (!checkNumOfArgs(Mnemonics.CALL, hpos, count, 1, 1, line.srcLineNo)) break;
					Pair<Boolean, Integer> p = checkOperandAndRange(Mnemonics.CALL, tokens[hpos+1], 12, line.srcLineNo);
					if (!p.first) break;
					line.bin = (short)(0x2000 | (p.second & 0x0FFF));
					line.translated = true;
				} while(false);
				break;	
			case Mnemonics.CLS :
				/** 00E0 - CLS */
				line.bin = 0x00E0;
				line.translated = true;
				break;
			case Mnemonics.DRW:
				/**
				 * Dxyn - DRW Vx, Vy, Nibble/0  
				 */
				do {
					if (!checkNumOfArgs(Mnemonics.DRW, hpos, count, 3, 3, line.srcLineNo)) break;
					Pair<Boolean, Integer> p1 = isVxVy(tokens[hpos+1]);
					Pair<Boolean, Integer> p2 = isVxVy(tokens[hpos+2]);
					if ( !p1.first || !p2.first ) {
						errors_.add(new AsmError(AsmError.E_WRONG_TYPE_OPERAND, "DRW Vx, Vy, Nibble", line.srcLineNo));
						break;
					}
					Pair<Boolean, Integer> p = checkOperandAndRange(Mnemonics.DRW, tokens[hpos+3], 4, line.srcLineNo);
					if (!p.first) break;
					line.bin = (short)(0xD000 | p1.second<<8 | p2.second<<4 | (p.second & 0x000F));
					line.translated = true;
				} while(false);
				break;
			case Mnemonics.EXT :
				/** 00FD - EXT */
				line.bin = 0x00FD;
				line.translated = true;
				break;
			case Mnemonics.HIGH :
				/** 00FF - HIGH */
				line.bin = 0x00FF;
				line.translated = true;
				break;
			case Mnemonics.JP  :
				do {
					if (!checkNumOfArgs(Mnemonics.JP, hpos, count, 1, 2, line.srcLineNo)) break;
					if (hpos==count-2) {
						/**
						 * 1nnn - JP nnn
						 */
						Pair<Boolean, Integer> p = checkOperandAndRange(Mnemonics.JP, tokens[hpos+1], 12, line.srcLineNo);
						if (!p.first) {
							break;
						}
						line.bin = (short)(0x1000 | (p.second & 0x0FFF));
					} else {
						/**
						 * Bnnn - JP V0, nnn
						 */
						if (tokens[hpos+1].compareTo("V0")!=0) {
							errors_.add(new AsmError(AsmError.E_WRONG_TYPE_OPERAND, "JP V0, addr", line.srcLineNo));
							break;
						}
						Pair<Boolean, Integer> p = checkOperandAndRange(Mnemonics.JP, tokens[hpos+2], 12, line.srcLineNo);
						if (!p.first) break;
						line.bin = (short)(0xB000 | (p.second & 0x0FFF) );
					}
					line.translated = true;
				} while(false);
				break;	
			case Mnemonics.LD :
				do {
					if ( !checkNumOfArgs(Mnemonics.LD, hpos, count, 2, 2, line.srcLineNo) ) break;
					Pair<Boolean, Integer> p = isVxVy(tokens[hpos+1]);
					if (p.first) {
						/**
						 * 6xkk - LD Vx, byte 
						 * 8xy0 - LD Vx, Vy 
						 * Fx07 - LD Vx, DT
						 * Fx0A - LD Vx, K 
						 * Fx65 - LD Vx, [I]
						 * Fx85 - LD Vx, R
						 */
						int x = p.second;
						
						if ( tokens[hpos+2].compareTo(Mnemonics.DT)==0 ) {
							line.bin = (short)(0xF007 | x<<8);
						} else if ( tokens[hpos+2].compareTo(Mnemonics.K)==0 ) {
							line.bin = (short)(0xF00A | x<<8);
						} else if ( tokens[hpos+2].compareTo(Mnemonics.I_INDIRECT)==0) {
							line.bin = (short)(0xF065 | x<<8);
						} else if ( tokens[hpos+2].compareTo(Mnemonics.R)==0 ) {
							line.bin = (short)(0xF085 | x<<8);
						} else {
							p = isVxVy(tokens[hpos+2]);
							if (p.first) {
								int y = p.second;
								line.bin = (short)(0x8000 | x<<8 | y<<4);
							} else {
								p = checkOperandAndRange(Mnemonics.LD, tokens[hpos+2], 8, line.srcLineNo);
								if (!p.first) {
									break;
								}
								line.bin = (short)(0x6000 | x<<8 | (p.second & 0x00FF) );
							}
							
						}
					} else {
						/**
			            Annn - LD I, addr
			            Fx15 - LD DT, Vx
			            Fx18 - LD ST, Vx
			            Fx29 - LD (L)F, Vx
			            Fx30 - LD HF, Vx
			            Fx33 - LD B, Vx
			            Fx55 - LD [I], Vx
			            Fx75 - LD R, VX           
						 */
						if ( tokens[hpos+1].compareTo(Mnemonics.I)==0 ) {
							p = checkOperandAndRange(Mnemonics.LD,tokens[hpos+2], 12, line.srcLineNo);
							if (!p.first) {
								break;
							}
							line.bin = (short)(0xA000 | (p.second & 0x0FFF));
						} else if ( tokens[hpos+1].compareTo(Mnemonics.DT)==0 ) {
							p = isVxVy(tokens[hpos+2]);
							if (!p.first) {
								errors_.add(new AsmError(AsmError.E_WRONG_TYPE_OPERAND, "LD DT, Vx", line.srcLineNo));
								break;
							}
							line.bin = (short)(0xF015 | p.second<<8);
							
						} else if ( tokens[hpos+1].compareTo(Mnemonics.ST)==0 ) {
							p = isVxVy(tokens[hpos+2]);
							if (!p.first) {
								errors_.add(new AsmError(AsmError.E_WRONG_TYPE_OPERAND, "LD ST, Vx", line.srcLineNo));
								break;
							}
							line.bin = (short)(0xF018 | p.second<<8);
							
						} else if ( tokens[hpos+1].compareTo(Mnemonics.F)==0 || tokens[hpos+1].compareTo(Mnemonics.LF)==0 ) {
							p = isVxVy(tokens[hpos+2]);
							if (!p.first) {
								errors_.add(new AsmError(AsmError.E_WRONG_TYPE_OPERAND, "LD F, Vx", line.srcLineNo));
								break;
							}
							line.bin = (short)(0xF029 | p.second<<8);

						} else if ( tokens[hpos+1].compareTo(Mnemonics.B)==0 ) {
							p = isVxVy(tokens[hpos+2]);
							if (!p.first) {
								errors_.add(new AsmError(AsmError.E_WRONG_TYPE_OPERAND, "LD B, Vx", line.srcLineNo));
								break;
							}
							line.bin = (short)(0xF033 | p.second<<8);

						} else if ( tokens[hpos+1].compareTo(Mnemonics.I_INDIRECT)==0 ) {
							p = isVxVy(tokens[hpos+2]);
							if (!p.first) {
								errors_.add(new AsmError(AsmError.E_WRONG_TYPE_OPERAND, "LD [I], Vx", line.srcLineNo));
								break;
							}
							line.bin = (short)(0xF055 | p.second<<8);
						} else if ( tokens[hpos+1].compareTo(Mnemonics.R)==0 ) {
							p = isVxVy(tokens[hpos+2]);
							if (!p.first) {
								errors_.add(new AsmError(AsmError.E_WRONG_TYPE_OPERAND, "LD R, Vx", line.srcLineNo));
								break;
							}
							line.bin = (short)(0xF075 | p.second<<8);
						} else if ( tokens[hpos+1].compareTo(Mnemonics.HF)==0 ) {
							p = isVxVy(tokens[hpos+2]);
							if (!p.first) {
								errors_.add(new AsmError(AsmError.E_WRONG_TYPE_OPERAND, "LD HF, Vx", line.srcLineNo));
								break;
							}
							line.bin = (short)(0xF030 | p.second<<8);
						} else {
							errors_.add(new AsmError(AsmError.E_WRONG_TYPE_OPERAND, "LD ..., ...", line.srcLineNo));
							break;
						}
						
					} // End of else
			
					line.translated = true;
				} while(false);
				break;
			case Mnemonics.LOW :
				/** 00FE - LOW */
				line.bin = 0x00FE;
				line.translated = true;
				break;
			case Mnemonics.RET :
				/** 00EE - RET */
				line.bin = 0x00EE;
				line.translated = true;
				break;
			case Mnemonics.RND :
				/** CXKK - RND Vx , Byte */
				do {
					if ( !checkNumOfArgs(Mnemonics.RND, hpos, count, 2, 2, line.srcLineNo) ) break;
					Pair<Boolean, Integer> p = isVxVy(tokens[hpos+1]);
					if (!p.first) {
						errors_.add(new AsmError(AsmError.E_WRONG_TYPE_OPERAND, "RND Vx, Byte", line.srcLineNo));
						break;
					}
					int x = p.second;
					p = checkOperandAndRange(Mnemonics.RND, tokens[hpos+2], 8, line.srcLineNo);
					if (!p.first) break;
					line.bin = (short)(0xC000 | x<<8 | (p.second & 0xFF));
					line.translated = true;
				} while(false);
				break;
			case Mnemonics.SCD :
				/** 00Cn - SCD n */
				do {
					if (!checkNumOfArgs(Mnemonics.SCD, hpos, count, 1, 1, line.srcLineNo)) {
						break;
					}
					Pair<Boolean, Integer> p = checkOperandAndRange(Mnemonics.SCD, tokens[hpos+1], 4, line.srcLineNo);
					if (!p.first) {
						break;
					}
					line.bin = (short)(0x00C0 | (p.second & 0xF));
					line.translated = true;	
				} while(false);
				break;
			case Mnemonics.SCL :
				/** 00FC - SCL */
				line.bin = 0x00FC;
				line.translated = true;
				break;	
			case Mnemonics.SCR :
				/** 00FB - SCR */
				line.bin = 0x00FB;
				line.translated = true;
				break;
			case Mnemonics.SE :
			case Mnemonics.SNE:
				/**
				 * 3xkk - SE Vx, kk
				 * 5xy0 - SE Vx, Vy
				 * 4xkk - SNE Vx, kk
    			 * 9xy0 - SNE Vx, Vy          
				 */
				do {
					if ( !checkNumOfArgs(tokens[hpos], hpos, count, 2, 2, line.srcLineNo) ) break;
					Pair<Boolean,Integer> p = isVxVy(tokens[hpos+1]);
					if (!p.first) {
						errors_.add(new AsmError(AsmError.E_WRONG_ARG_TYPE_NEED_V, tokens[hpos] + " Vx, kk / " + tokens[hpos] + " Vx, Vy" , line.srcLineNo));
						break;
					}
					
					int x = p.second;
					p = isVxVy(tokens[hpos+2]);
					if (p.first) {
						// 5xy0 - SE Vx, Vy or 9xy0 - SNE Vx, Vy
						int xhead = 0x5000;
						if (tokens[hpos].compareTo(Mnemonics.SNE)==0) {
							xhead = 0x9000;
						}
						int y = p.second;
						line.bin = (short)(xhead | x<<8 | y<<4 );
					} else {
						// 3xkk - SE Vx, byte or 4xkk - SNE Vx, kk
						int xhead = 0x3000;
						if (tokens[hpos].compareTo(Mnemonics.SNE)==0) {
							xhead = 0x4000;
						}
						p = checkOperandAndRange(tokens[hpos], tokens[hpos+2], 8, line.srcLineNo);
						if (!p.first) {
							break;
						}
						line.bin = (short)(xhead | x<<8 | (p.second & 0x0FFF));
					}
					line.translated = true;
				} while(false);
				break;
			case Mnemonics.SHL :
			case Mnemonics.SHR :
				/** 8XYE - SHL Vx {, Vy} 
				 *  8XY6 - SHR Vx {, Vy} 
				 */
				do {
					if (!checkNumOfArgs(tokens[hpos], hpos, count, 1, 2, line.srcLineNo)) break;
					
					Pair<Boolean, Integer> p = isVxVy(tokens[hpos+1]);
					if ( !p.first ) {
						errors_.add(new AsmError(AsmError.E_WRONG_TYPE_OPERAND,  tokens[hpos] + " Vx {,Vy}", line.srcLineNo));
						break;
					}
					int x = p.second;
					int y = 0;
					if (hpos==count-3) {
						p = isVxVy(tokens[hpos+2]);
						if (!p.first) {
							errors_.add(new AsmError(AsmError.E_WRONG_TYPE_OPERAND, tokens[hpos] + " Vx {,Vy}", line.srcLineNo));
							break;
						}
						y = p.second;
					}
					int xhead = 0x8006;
					if (tokens[hpos].compareTo(Mnemonics.SHL)==0) {
						xhead = 0x800E;
					}
					line.bin = (short)(xhead | x<<8 | y<<4 );
					line.translated = true;
				} while(false);
				break;
			case Mnemonics.SKP:
			case Mnemonics.SKNP:
				/**
				 * Ex9E - SKP Vx
    		     * ExA1 - SKNP Vx        
				 */
				do {
					if (!checkNumOfArgs(tokens[hpos], hpos, count, 1, 1, line.srcLineNo)) break;
					Pair<Boolean, Integer> p = isVxVy(tokens[hpos+1]);
					if (!p.first) {
						errors_.add(new AsmError(AsmError.E_WRONG_TYPE_OPERAND, tokens[hpos] + " Vx", line.srcLineNo));
						break;
					}
					int xhead = 0xE09E;
					if (tokens[hpos].compareTo(Mnemonics.SKNP)==0) {
						xhead = 0xE0A1;
					}
					line.bin = (short)(xhead | p.second<<8 );
					line.translated = true;
				} while (false);
				break;
			case Mnemonics.SUB:
			case Mnemonics.SUBN:
				/**
				 * 8xy5 - SUB Vx, Vy
                 * 8xy7 - SUBN Vx, Vy  
				 */
				do {
					if (!checkNumOfArgs(tokens[hpos], hpos, count, 2, 2, line.srcLineNo)) break;
					Pair<Boolean, Integer> p1 = isVxVy(tokens[hpos+1]);
					Pair<Boolean, Integer> p2 = isVxVy(tokens[hpos+2]);
					if ( !p1.first || !p2.first ) {
						errors_.add(new AsmError(AsmError.E_WRONG_TYPE_OPERAND, tokens[hpos] + " Vx, Vy", line.srcLineNo));
						break;
					}
					int xhead = 0x8005;
					if ( tokens[hpos].compareTo(Mnemonics.SUBN)==0 ) {
						xhead = 0x8007;
					}
					line.bin = (short)(xhead | p1.second<<8 | p2.second<<4);
					line.translated = true;
				} while (false);
				break;
			case Mnemonics.SYS :
				/** 0nnn - SYS nnn */
				do {
					if ( !checkNumOfArgs(Mnemonics.SYS, hpos, count, 1, 1, line.srcLineNo)) break;
					Pair<Boolean,Integer> p = checkOperandAndRange(Mnemonics.SYS, tokens[hpos+1], 12, line.srcLineNo);
					if (!p.first) break;
					line.bin = new Integer(p.second).shortValue();
					line.translated = true;
				} while(false);
				break;
				
			/** ---- Directives ---- 
			 * For those directives not related to assembly layout, make sure to set line.offset to -1 and 
			 * incr to 0.
			 */
			case Directives.ALIGN:
				do {
					if ( !checkNumOfArgs(Directives.ALIGN, hpos, count, 1, 1, line.srcLineNo) ) break;
					line.offset = -1;
					incr = 0;
					if (line.tokens[hpos+1].compareTo(Directives.S_ON)==0) {
						wordAlign_ = true;
					} else if (line.tokens[hpos+1].compareTo(Directives.S_OFF)==0) {
						wordAlign_ = false;
					} else {
						errors_.add(new AsmError(AsmError.E_DIRECTIVE_ERROR, "[ALIGN] ON|OFF", line.srcLineNo));
					}
					line.translated = true;
				} while(false);
				break;
			case Directives.DEFINE:
				do {
					// TODO: Put define/und symbols into a different scope.
					if ( !checkNumOfArgs(Directives.DEFINE, hpos, count, 1, 1, line.srcLineNo)) break;
					line.offset = -1;
					incr = 0;
					String value = line.tokens[hpos+1];
					Boolean p = checkVarNameValid(value, "[DEFINE]", line.srcLineNo);
					if (!p) {
						errors_.add(new AsmError(AsmError.E_PREPROCESS_SYMBOL, "Not a valid condition name : " + value,
									line.srcLineNo));
						break;
					}
					symbolTable_.put(minglePreprocessingDefName(line.tokens[hpos+1]), Directives.S_TRUE_I);
					line.translated=true;
				} while(false);
				break;
			case Directives.UNDEF:
				do {
					if ( !checkNumOfArgs(Directives.UNDEF, hpos, count, 1, 1, line.srcLineNo)) break;
					line.offset = -1;
					incr = 0;
					String value = line.tokens[hpos+1];
					Boolean p = checkVarNameValid(value, "[UNDEF]", line.srcLineNo);
					if (!p) {
						errors_.add(new AsmError(AsmError.E_PREPROCESS_SYMBOL, "Not a valid condition name : " + value,
									line.srcLineNo));
						break;
					}
					symbolTable_.put(minglePreprocessingDefName(line.tokens[hpos+1]), Directives.S_FALSE_I);
					line.translated=true;
				} while(false);					
				break;
			case Directives.IFDEF:
				do {
					if ( !checkNumOfArgs(Directives.IFDEF, hpos, count, 1, 1, line.srcLineNo)) break;
					line.offset = -1;
					incr = 0;
					String value = line.tokens[hpos+1];
					Boolean p = checkVarNameValid(value, "[IFDEF]", line.srcLineNo);
					if (!p) {
						errors_.add(new AsmError(AsmError.E_PREPROCESS_SYMBOL, "Not a valid condition name : " + value,
									line.srcLineNo));
						break;
					}
					
					if ( !checkIfBalance(S_IF, "[IFDEF]", line.srcLineNo) ) break;
					
					String rname = minglePreprocessingDefName(value);
					skip_ = !(symbolTable_.containsKey(rname) && symbolTable_.get(rname) == Directives.S_TRUE_I);
					line.translated=true;
				} while(false);	
				
				break;
			case Directives.IFUND:
				do {
					if ( !checkNumOfArgs(Directives.IFUND, hpos, count, 1, 1, line.srcLineNo)) break;
					line.offset = -1;
					incr = 0;
					String value = line.tokens[hpos+1];
					Boolean p = checkVarNameValid(value, "[IFUND]", line.srcLineNo);
					if (!p) {
						errors_.add(new AsmError(AsmError.E_PREPROCESS_SYMBOL, "Not a valid condition name : " + value,
									line.srcLineNo));
						break;
					}
					
					if ( !checkIfBalance(S_IF, "[IFUND]", line.srcLineNo) ) break;
					String rname = minglePreprocessingDefName(value);
					skip_ = !(!symbolTable_.containsKey(rname) || symbolTable_.get(rname) == Directives.S_FALSE_I);
					line.translated=true;
				} while(false);	
				break;
			case Directives.ELSE:
				do {
					if ( !checkNumOfArgs(Directives.ELSE, hpos, count, 0, 0, line.srcLineNo)) break;
					line.offset = -1;
					incr = 0;
					if ( !checkIfBalance(S_ELSE, "[ELSE]", line.srcLineNo) ) break;
					skip_ = !skip_;
					line.translated=true;
				} while(false);	
				break;
			case Directives.ENDIF:
				do {
					if ( !checkNumOfArgs(Directives.ENDIF, hpos, count, 0, 0, line.srcLineNo)) break;
					line.offset = -1;
					incr = 0;
					if ( !checkIfBalance(S_ENDIF, "[ENDIF]", line.srcLineNo) ) break;
					skip_ = false;
					line.translated=true;
				} while(false);	
				break;
			case Directives.XREF:
				do {
					if ( !checkNumOfArgs(Directives.ENDIF, hpos, count, 1, 1, line.srcLineNo)) break;
					line.offset = -1;
					incr = 0;
					System.out.println("[XREF] Not implemented.");
					line.translated=true;
				} while(false);	
				break;			
			case Directives.OPTION:
				do {
					if ( !checkNumOfArgs(Directives.ENDIF, hpos, count, 1, 1, line.srcLineNo)) break;
					line.offset = -1;
					incr = 0;
					if (line.tokens[hpos+1].compareTo("BINARY")==0) {
						outputMode_ = OutputMode.BINARY;
					} else if (line.tokens[hpos+1].compareTo("STRING")==0) {
						outputMode_ = OutputMode.HEXSTRING;
					} else {
						/**
						 * TODO: Support these options in future, now just bypass.
						 * CHIP8 ; Specify Chip-8 as target mode
						 * CHIP48 ; Specify Chip-48 as target mode
						 * HPASC ; Select HP48 ASC-> output mode
						 * HPBIN ; Select HP48 binary output mode
						 * SCHIP10 ; Specify Super Chip-48 V1.0 mode
						 * SCHIP11 ; Specify Super Chip-48 V1.1 mode
						 */
						if ( line.tokens[hpos+1].compareTo("CHIP8")==0 ||
							line.tokens[hpos+1].compareTo("CHIP48")==0 ||
							line.tokens[hpos+1].compareTo("HPASC")==0 ||
							line.tokens[hpos+1].compareTo("HPBIN")==0 ||
							line.tokens[hpos+1].compareTo("SCHIP10")==0 ||
							line.tokens[hpos+1].compareTo("SCHIP11")==0) {
							// Supports in future
							// 
						} else {
							errors_.add(new AsmError(AsmError.E_PREPROCESS_WRONG_DIRECTIVE, "[OPTION]", line.srcLineNo));
							break;
						}
					}
					line.translated=true;
				} while(false);	
				break;
			case Directives.USED:
				do {
					if ( !checkNumOfArgs(Directives.USED, hpos, count, 1, 1, line.srcLineNo)) break;
					line.offset = -1;
					incr = 0;
					if (line.tokens[hpos+1].compareTo(Directives.S_NO)==0) {
						warnUnusedSymbols_ = true;
					} else if (line.tokens[hpos+1].compareTo(Directives.S_YES)==0) {
						warnUnusedSymbols_= false;
					} else if (line.tokens[hpos+1].compareTo(Directives.S_ON)==0){
						autoUseSymbols_ = true;
					} else if (line.tokens[hpos+1].compareTo(Directives.S_OFF)==0) {
						autoUseSymbols_ = false;
					} else {
						ep_.getUsedSymbolSet().add(line.tokens[hpos+1]);
					}
					line.translated=true;
				} while(false);	
				break;
			// Note that for Non-data allocation directives, set offest to -1 to disable generating binary code.	
			case Directives.ORG:
				// Rebase current address at operand in memory
				do {
					if ( !checkNumOfArgs(Directives.ORG, hpos, count, 1, 1, line.srcLineNo)) break;
					line.offset = -1;
					incr = 0;
					Pair<Boolean,Integer> p = checkOperandAndRange(Directives.ORG, tokens[hpos+1], 12, line.srcLineNo);
					if (!p.first) break;
					int newAddress = p.second;
					startAddress_  = newAddress  - currentOffset_;
					line.offset = -1;
					line.translated = true;
				} while (false);
				break;
			case Directives.DA:
				// String allocations, concats all the rest of tokens as a single string as data and get the length
				do {
					if ( !checkNumOfArgs(Directives.DA, hpos, count, 1, 1, line.srcLineNo)) break;
					byte[] ba = tokens[hpos+1].getBytes(Charset.forName("ASCII")); 
					incr = ba.length;
					if (line.align && incr%2==1) {
						incr+=1;
					}
					line.data = Arrays.copyOf(ba, incr);
					line.translated = true;
				} while (false);
				break;
			case Directives.DB:
				db_clause:
				do {
					// Maximum supports 1023 byte data.
					if ( !checkNumOfArgs(Directives.DB, hpos, count, 1, 1023, line.srcLineNo) ) break;
					
					incr = count-hpos-1;
					
					if (line.align && incr%2==1) {
						incr+=1;
					}
					
					line.data = new byte[incr];
					for (int i = hpos+1; i<count; ++i) {
						Pair<Boolean, Integer> p = checkOperandAndRange(Directives.DB, tokens[i], 8, line.srcLineNo);
						if (!p.first) break db_clause;
						line.data[i-hpos-1] = (byte)(p.second.intValue());
					}
					line.translated = true;
				} while(false);
				break;
			case Directives.DW:
				dw_clause:
				do {
					// Maximum supports 1023 byte data.
					if ( !checkNumOfArgs(Directives.DW, hpos, count, 1, 511, line.srcLineNo) ) break;
		
					incr = (count-hpos-1) * 2;
					line.data = new byte[incr];
					for (int i = hpos+1; i<count; ++i) {
						Pair<Boolean, Integer> p = checkOperandAndRange(Directives.DW, tokens[i], 16, line.srcLineNo);
						if (!p.first) break dw_clause;
						int value = p.second.intValue();
						line.data[2*(i-hpos-1)] = (byte)(value>>8);
						line.data[2*(i-hpos-1)+1] = (byte)value;
					}
					line.translated = true;
				} while(false);					
				break;
			case Directives.DS:
				do {
					if ( !checkNumOfArgs(Directives.DS, hpos, count, 1, 1, line.srcLineNo)) break;
					
					Pair<Boolean, Integer> p = checkOperandAndRange(Directives.DS,tokens[hpos+1], 8, line.srcLineNo);
					if (!p.first) break;
					incr = p.second;
					if (line.align && incr%2==1) {
						incr+=1;
					}
					line.data = new byte[incr];
					line.translated = true;
				} while (false);
				break;
			default:
			{
				// Possibly LABELS or VARIABLES
				if (tokens.length<2) {
					errors_.add(new AsmError(AsmError.E_WRONG_VARIABLE_NAME, "Not enough tokens on this line.", line.srcLineNo));
					break;
				}
				String symbolName = tokens[hpos];
				// Variable assignment
				if (tokens[hpos+1].compareTo(Directives.I_EQU)==0 || tokens[hpos+1].compareTo(Directives.I_EQU_SIGN)==0) {
					// Variables
					if ( !checkVarNameValid(symbolName, "Variable names contains non alpha-numeric values.", line.srcLineNo) ) {
						break;
					}
					// Check infix = / EQU
					if ( !checkNumOfArgs("EQU/=", hpos, count, 2, 2, line.srcLineNo) ) {
						break;
					}
					if (tokens[hpos+1].compareTo(Directives.I_EQU)!=0 && tokens[hpos+1].compareTo(Directives.I_EQU_SIGN)!=0) {
						errors_.add(new AsmError(AsmError.E_DIRECTIVE_ERROR, "Variable Assignment (=/EQU)", line.srcLineNo));
						break;	
					}
					/* Parse expression at hpos+2 */
					Pair<Boolean, Integer> p = ep_.parse(tokens[hpos+2], symbolTable_);
					if (!p.first) {
						if (currentPass_ == lastPass_) {
							errors_.add(new AsmError(
									AsmError.E_UNDEFINED_VARIABLE,
									"Can't resolve : " + tokens[hpos+2],
									line.srcLineNo));
						} else {
							unresolved_.put(symbolName, tokens[hpos+2]);
						}
						break;
					}
					symbolTable_.put(symbolName, p.second);
					symbolLineNumMap_.put(symbolName, line.srcLineNo);
					if (autoUseSymbols_) {
						ep_.getUsedSymbolSet().add(symbolName);
					}
					line.offset = -1;
					incr = 0;
					line.translated = true;
				} else if (tokens[hpos+1].compareTo(Tokenizer.S_LABEL_SUFFIX)==0) {
					// Labels
					if ( !checkVarNameValid(symbolName, "Lable names contains non alpha-numeric values.", line.srcLineNo) ) {
						break;
					}
					if ( checkReservedWord(symbolName, line.srcLineNo)) {						
						break;
					}
					symbolTable_.put(symbolName, currentOffset_ + startAddress_);
					symbolLineNumMap_.put(tokens[hpos], line.srcLineNo);
					if (autoUseSymbols_) {
						ep_.pushSymbolAsUsed(symbolName);
					}
					/* For labels do not increase currentOffset_ 
					 * Go back to parsing next token, set offset to -1 at that place (end chained labels.)
					 */
					hpos+=2;
					doParse = true;
				} else {
			
				} // End of Variable assignment
			}	// End of default case
			
			//////////////////////////////////////////
			} // End of Switch	
		} // End of while
		
		// Increment offset
		if (currentPass_==FIRST_PASS && incr!=0) {
			currentOffset_ += incr;
		}
	}
	public static String printStrArray(String[] a) {
		String str = "";
		for (String q : a) {
			str += (q+" | ");
		}
		return str;
	}
	
	public static void main (String[] args) {
		Assembler a = new Assembler();
		try {
			byte[] bytes = a.assemble(new String(Files.readAllBytes(Paths.get("CHIP8/SGAMES/SOURCES/BLINKY.SRC"))),0x200);
			//byte[] bytes = a.assemble(new String(Files.readAllBytes(Paths.get("tests/test05.asm"))),200);
			Files.write(Paths.get("tests/out.ch8"), bytes);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
