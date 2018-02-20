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

import java.util.Properties;

public class Platforms {

	public enum OsType {
		MAC_OS_X, WINDOWS, OTHER,
	}
	
	private static String osname_ = System.getProperty("os.name").toLowerCase();
	public static OsType getOSType() {
		if (osname_.startsWith("mac os x")) {
			return OsType.MAC_OS_X;
		} else if (osname_.startsWith("win")) {
			return OsType.WINDOWS;
		} else {
			return OsType.OTHER;
		}
	}
	public static boolean isJavaVersionOK() {
		String supported = "1.8";
		String ver = System.getProperty("java.version");
		
		if (ver!=null && ver.length()==0) {
			ver = System.getProperty("java.runtime.version");
		}
		
		if (ver!=null) {
			ver = ver.substring(0,3);
			return supported.compareTo(ver) <= 0;
		} else {
			return false;
		}
	}

}
