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

package com.taibaisoft.chip8.platform;

public class CmdArgs {

	private String[] args_ = null;
	private ArgDef[] defs_ = null;
	private int len_ = 0;
	private String lastError_ = "";
	
	public CmdArgs(ArgDef[] defs, String[] args) {
		defs_ = defs;
		args_ = args;
		if (args_!=null)
			len_ = args_.length;
	}

	private int ArgValueStart = 0;
	private ArgDef getMatchedDef(String arg, int start) {
		if (start==2) {
			for (ArgDef def : defs_) {
				if ( arg.substring(start).startsWith(def.name) ) {
					ArgValueStart = start+def.name.length();
					return def;
				}
			}
		} else {
			// start==1, short match
			for (ArgDef def : defs_) {
				if ( arg.substring(start).startsWith(def.alias) ) {
					ArgValueStart = start+def.alias.length();
					return def;
				}
			}	
		}
		return null;
	}
	public String getLastError() {
		return lastError_;
	}
	public Arg getNext(int index) {
		if (args_==null || index>=len_ || index <0) {
			return null;
		}
		Arg v = new Arg();
		
		String s = args_[index].trim();
		ArgDef md = null;

		if (s.startsWith("--")) {
			md = getMatchedDef(s, 2);
		} else if (s.startsWith("-")) {
			md = getMatchedDef(s, 1);
		}
		if (md != null) {
			v.argName = md.name;
			if (md.value > 0) {
				if (ArgValueStart == s.length()) {
					// Not written together, proceed to next arg
					if (index < len_ - 1) {
						index++;
						v.argVal = args_[index].trim();
					} else {
						lastError_ = "Option " + v.argName + " missing mandatory value.";
						v= null;
					}
				} else {
					v.argVal = s.substring(ArgValueStart);
				}
			}
		} else {
			// No matching
			v.argName = "";
			v.argVal = s;
		}
		if (index==len_-1) {
			v.nextIndex = -1;
		} else {
			v.nextIndex = index+1;
		}
		return v;
	}
	
}
