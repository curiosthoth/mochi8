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
import java.util.Random;


public class Util {
	public static String NEW_LINE = System.getProperty("line.separator");
	static Random rand = new Random(); 
	public static int genInt(int minIn, int maxEx) {
		return rand.nextInt(maxEx - minIn) + minIn;
	}
	public static void fill(byte[] buffer, byte val) {
		int len = buffer.length;
		for (int i = 0; i<len; ++i) {
			buffer[i] = val;
		}
	}
	/**
	 * @param b
	 * @return The promoted integer value which is always positive.
	 */
	public static int b2i(byte b) {
		return b >= 0 ? (int) b : (int) (256 + b);
	}
	/**
	 * 
	 * @param aa
	 * @param n
	 * @param cols
	 * @param width
	 */
	public static void shiftArray(int[] m, int n, int cols, int width) {
		if (m!=null && cols>0) {
			int len = m.length;
			int ac = cols%width;
			if(n==-1) {
				// Left
				for (int j =0;j<ac;++j) {
					for (int i = 0; i < len; ++i) {
						if ((i + 1) % width > 0) {
							m[i] = m[i + 1];
						} else {
							m[i] = 0;
						}
					}
				}
			} else if (n==1) {
				// Right
				for (int j=0;j<ac;++j) {
					for (int i = len-1; i >= 0; --i) {
						if ( i%width>0 ) {
							m[i] = m[i-1];
						} else {
							m[i] = 0;
						}
						
					}
				}				
			} else if (n==-2) {
				// Up
				for (int j=0;j<ac;++j) {
					for (int i = 0; i < len; ++i) {
						if ( i<len-width ) {
							m[i] = m[i+width];
						} else {
							m[i] = 0;
						}
						
					}
				}				
			} else if (n==2) {	
				// Down
				for (int j=0;j<ac;++j) {
					for (int i = len-1; i >= 0; --i) {
						if ( i>=width ) {
							m[i] = m[i-width];
						} else {
							m[i] = 0;
						}
						
					}
				}				
				
			} else {
				// Invalid direction
			}
		}
	}

	public static String getVersionString(String comp) {
		final String messageTemplate = "Mochi8 v%s \n" +
				"A CHIP-8/S-CHIP emulator & (dis)assembler.\n" +
				comp + "\n" +
				"by Jeffrey Bian (jeffreybian@gmail.com)";
		final Properties properties = new Properties();
		String message = "Unable to read versions.";

		try {
			properties.load(Util.class.getClassLoader().getResourceAsStream("project.properties"));
			String ver = properties.getProperty("version");
			message = String.format(messageTemplate, ver);
		} catch (Exception e) {
			message += e.toString();
		}
		return message;
	}
}
