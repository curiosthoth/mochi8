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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

/**
 * For expression evaluation.
 * 
 *   (  ; Start parentheses expression
 *   )  ; End of parentheses expression
 *   ----------------------------------
 *   +  ; Unary plus sign
 *   -  ; Unary minus sign
 *   ~  ; Bitwise NOT operator
 *   ----------------------------------
 *   !  ; Power of operator
 *   <  ; Shift left number of bits
 *   >  ; Shift right number of bits
 *   ----------------------------------
 *   *  ; Multiply
 *   /  ; Divide
 *   ----------------------------------
 *   +  ; Add
 *   -  ; Subtract
 *   ----------------------------------
 *   &  ; Bitwise AND operator
 *   |  ; Bitwise OR operator
 *   ^  ; Bitwise XOR operator
 *   ----------------------------------
 *   \  ; Low priority divide
 *   %  ; Modulus operator
 *    
 * @author jeffrey bian
 */
public class ExpressionParser {
	final static int MAX_TOKENS = 64;
	
	List<String> tokens_  = new ArrayList<String>();
	String expr_ = "";
	List<String> postfixExpr_ = new ArrayList<String>();
	HashMap<String, Integer> symbolTable_ = new HashMap<String,Integer>();
	HashSet<String> usedSymbolSet_ = new HashSet<String>();
	
	static char[] ops_ = new char[] {
			'=',  '_',  '~', // Use special chars for unary +, - ~
			'!',  '<',  '>',
			'*',  '/',  '\0',
			'+',  '-',  '\0',			
			'&',  '|',  '^',
			'\\', '%',  '\0',
	};

	public void init() {
		usedSymbolSet_.clear();
		usedSymbolSet_.add("?");
	}
	/**
	 * This algorithm involves two steps:
	 * 1. Transforms the expression into postfix notion, with variable replacement;
	 * 2. Evaluate the postfix expression to an integer.
	 * @param expr
	 * @param symbolTab
	 * @return 
	 */
	public Pair<Boolean, Integer> parse (String expr, /*IN, OUT*/ HashMap<String, Integer> symbolTab) {
		// 0. Initialize
		expr_ = expr;
		postfixExpr_.clear();
		tokens_.clear();
		
		if (symbolTab!=null) {
			symbolTable_ = symbolTab;
		}

		Pair<Boolean, Integer> p = new Pair<Boolean, Integer>(false, -1);
		// 0.
		if (symbolTable_.containsKey(expr)) {
			usedSymbolSet_.add(expr);
			p.first = true;
			p.second = symbolTable_.get(expr);
		} else {

			// 1. Tokenize
			tokenize();

			// 2. Convert to RPN with Shunting Yard Algorithm
			convertToRPN();

			// 3. Evaluate
			p = evaluate();
		}
		return p;
	}
	
	/**
	 * Side effect: postfixExpr_ is changed.
	 */
	protected void convertToRPN(){
		Stack<String> stack = new Stack<String>();
		String prev = "";
		for (String t : tokens_ ) {
			if (t==null) {
				break;
			}
			if (isOp(t)) {
				if (isOp(prev) || postfixExpr_.size()==0 || prev.compareTo("(")==0 || prev.length()==0) {
					// Unary
					String special = "";
					if (t.compareTo("+")==0) {
						special = "=";
					} else if (t.compareTo("-")==0 ) {
						special = "_";
					} else if (t.compareTo("~")==0) {
						special = "~";
					} else {
						// ERROR: Bad unary operator
						break;
					}
					stack.push(special);
				} else {
					while (!stack.empty() && isOp(stack.peek()) && opPriority(stack.peek(), t)>=0 ) {						
						postfixExpr_.add(stack.pop());
					}
					stack.push(t);	
				}
			} else if (t.compareTo("(")==0) {
				// A 'number' or variable
				stack.push(t);
			} else if (t.compareTo(")")==0) {
				String top = stack.pop();
				while(top.compareTo("(")!=0) {
					postfixExpr_.add(top);
					top = stack.pop();
				} 
			} else {
				postfixExpr_.add(t);
			}
			prev = t;
		}
		
		while(!stack.empty()) {
			postfixExpr_.add(stack.pop());
		}
	}
	
	/**
	 * 
	 * @return
	 */
	protected Pair<Boolean, Integer> evaluate() {
		// Depends on postfixExpr_
		Pair<Boolean, Integer> p = new Pair<Boolean, Integer>(true, -1);
		Stack<String> stack = new Stack<String>();
		int i = 0;
		
		outter_loop:
		for(;i<postfixExpr_.size();++i) {
			String s = postfixExpr_.get(i);
			if (!isOp(s)) {
				stack.push(s);
			} else  {
				String first = null, second = null;
				if (!stack.empty()) {
					second = stack.pop();
				}
				if (!stack.empty() && s.compareTo("_")!=0 && s.compareTo("=")!=0 && s.compareTo("~")!=0) {
					first = stack.pop();
				}
				if (second!=null) {
					Pair<Boolean, Integer> p1 = new Pair<Boolean, Integer>(true, 0);
					Pair<Boolean, Integer> p2;
					if (first!=null) {
						p1 = getNumber(first);
					}
					p2 = getNumber(second);
					int tmp = 0;
					if (p1.first && p2.first) {
						switch (s.charAt(0)) 
						{
						case '=': tmp = p2.second; break;
						case '_': tmp = -p2.second; break;
						case '~': tmp = ~p2.second; break;
						case '!': tmp = (int)Math.pow(p1.second, p2.second); break;
						case '+': tmp = p1.second + p2.second; break;
						case '-': tmp = p1.second - p2.second; break;
						case '*': tmp = p1.second * p2.second; break;
						case '/': tmp = p1.second / p2.second; break;
						case '<': tmp = p1.second << p2.second; break;
						case '>': tmp = p1.second >>> p2.second; break;
						case '&': tmp = p1.second & p2.second; break;
						case '|': tmp = p1.second | p2.second; break;
						case '^': tmp = p1.second ^ p2.second; break;
						case '\\': tmp = p1.second / p2.second; break;
						case '%': tmp = p1.second % p2.second; break;
						default:
							return new Pair<Boolean, Integer>(true, -1);
						}
						
						// If good, push it to the stack
						stack.push(String.format("%d", tmp));
					} else {
						p.first = false;
						stack.clear();
						break outter_loop;
					}
				} else {
					// Second is the only one left
					stack.push(second);
				}
			}
		}
		
		if (!stack.empty()) {
			p = getNumber(stack.pop());
		}
		return p;
	}
	
	/**
	 * 
	 * @param c1
	 * @param c2
	 * @return 1 if c1 is higher priority than c2; 0 if same; -1 if lower.
	 */
	protected int opPriority(String c1, String c2) {
		int i1 =0 , i2= 0;
		char a1 = c1.charAt(0), a2 = c2.charAt(0);
		
		for (int j =0 ; j < ops_.length; ++j) {
			if (ops_[j]==a1) {
				i1 = j;
			}
			if (ops_[j]==a2) {
				i2 = j;
			}
		}		
		if (i1/3>i2/3) {
			return -1;
		} else if (i1/3==i2/3) {
			return 0;
		} else {
			return 1;
		}
	}
	protected boolean isOp(String s) {
		return s.length()==1 && isOp(s.charAt(0));
	}
	
	protected boolean isOp(char c) {
		boolean r = false;
		for (char o : ops_) {
			if (o==c) {
				r = true;
				break;
			}
		}
		return r;
	}
	protected Pair<Boolean, Integer> getNumber(String numberStr ) {
		Pair<Boolean, Integer> f = new Pair<Boolean, Integer>(false, -1);
		int number = -1;		
		do {
			if (numberStr == null || numberStr.length() < 1) {
				break;
			}
			int radix = 10;
			int sign = 1;
			int startIndex = 0;
			boolean isVar = false;
			char c = numberStr.charAt(0);
			if (c=='-') {
				sign = -1;
				c = numberStr.charAt(1);
				startIndex++;
			} else if (c=='+') {
				sign = 1;
				c = numberStr.charAt(1);
				startIndex++;
			}
			if (c=='$') {
				radix = 2;
				startIndex++;
			} else if (c=='#') {
				radix = 16;
				startIndex++;
			} else if (c=='@') {
				radix = 8;
				startIndex++;
			} else {
				if (Character.isDigit(c)) {
					radix = 10;
				} else {
					// Consider variable
					isVar = true;
				}
			}
			if (!isVar) {
				try {
					number = Integer.parseInt(
							numberStr.substring(startIndex),
							radix) * sign;
				} catch (NumberFormatException e) {
					break;
				}
			} else {
				//
				String vname = numberStr.substring(startIndex);
				if (symbolTable_.containsKey(vname)) {
					number = symbolTable_.get(vname)  * sign;
					usedSymbolSet_.add(vname);
				} else {
					break;
				}
			}
			f.first = true;
			f.second = number;
		} while(false);
		return f;	
	}
	
	protected void tokenize() {
		int len = expr_.length();
		StringBuilder[] sbs = new StringBuilder[MAX_TOKENS]; 
		char c = 0;
		int i = 0, pi = -1;
		int current = 0;
		for ( ;i<len; i++) {
			c = expr_.charAt(i);
			if ( c==' ' || c=='\t' || c==10 || c==13 ) {
				if (pi==0) {
					current++;
					pi = 1;
				}
				continue;
			}
			pi = 0;
			if (isOp(c) || c=='(' || c==')') {
				if (pi==0) {
					current++;
					pi = 1;
				}				
				if (sbs[current]==null) {
					sbs[current] = new StringBuilder();
				}
				sbs[current].append(c);
				current++;
			} else {
				if (c=='.') {
					c = '0';
				}
				if (sbs[current]==null) {
					sbs[current] = new StringBuilder();
				}
				sbs[current].append(c);
			}
		}		
		
		/* Fillup tokens */
		int j = 0;
		for (;j<MAX_TOKENS;++j) {
			if (sbs[j]!=null) {
				tokens_.add(sbs[j].toString());
			}
		}
		
	}
	public void pushSymbolAsUsed(String sym) {
		usedSymbolSet_.add(sym);
	}
	public HashSet<String> getUsedSymbolSet() {
		return usedSymbolSet_;
	}
	
}