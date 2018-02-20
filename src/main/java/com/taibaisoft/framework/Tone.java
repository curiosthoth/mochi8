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

package com.taibaisoft.framework;

public class Tone {
	

	
	public enum ToneType {
		SINE, SQUARE, TRIANGLE,
	}
	
	

	private double frequency_ = 0;
	private int sampleRate_ = 1000;
	private int length_ = 200; // In milliseconds,
	private ToneType type_ = ToneType.SQUARE;
	private byte[] buffer_ = null;
	
	
	
	public Tone(double frequency, int sampleRate, int length, ToneType type, boolean harmony) throws Exception {
		frequency_ = frequency;
		if (frequency_<0 || frequency_>32767) {
			throw new Exception("Frequency out of range!");
		}
		length_ = length;
		if (length_<1) {
			throw new Exception("Length out of range!");
		}
		type_ = type;
		sampleRate_ = sampleRate;
		generateBuffer_();
	
	}

	public byte[] getBuffer() {
		return buffer_;
	}
	
	protected void generateBuffer_() {
		if (frequency_==0) {
			buffer_ = new byte[2 * sampleRate_ * length_ / 2000];
		} else {
			int wavelengths = (int)frequency_ * length_ / 2000;
			int framesPerWaveLen = (int)(sampleRate_/frequency_);
			int halfLen = framesPerWaveLen * wavelengths;
			buffer_ = new byte[2 * halfLen];
			for (int i = 0; i < halfLen; i++) {
				double rad = ((double) (i%framesPerWaveLen) / ((double) framesPerWaveLen))
						* (2*Math.PI);
				buffer_[i * 2] = shapeValue(rad);
				buffer_[i * 2 + 1] = buffer_[i * 2];
			}
		}
	}
	private byte shapeValue(double rad) {
		byte v = 0;
		int  max = 127;
		
		if (type_==ToneType.SINE) {
			v = (new Integer((int) Math.round(Math.sin(rad) * max)))
				.byteValue();
		} else if (type_==ToneType.SQUARE) {
			if (rad<Math.PI) {
				v = (byte)max;
			} else {
				v = 0;
			}
		} else if (type_==ToneType.TRIANGLE) {
			byte x = new Integer((int) Math.round(max * rad/Math.PI)).byteValue();
			if (rad>=0 && rad<Math.PI) {
				v = x;
			} else if (rad>Math.PI && v<2*Math.PI) {
				v = (byte) (max - x);
			}
			
		}
		return v;
	}
	public static void main(String[] args) {
		

	}

}
