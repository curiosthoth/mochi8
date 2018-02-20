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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JFrame;

import com.taibaisoft.framework.Tone.ToneType;

public class ToneGenerator {
	public static final int SR_FM = 11025;
	public static final int SR_HALF_CD = 22050;
	public static final int SR_CD = 44100;
	public static final int SR_DIGITAL_PRO = 48000;
	public static final int SR_DVD_AUDIO = 96000;
	
	public static final int BR_8 = 8;
	public static final int BR_16 = 16;
	
	private int defaultChannels_ = 1;
	private int defaultSampleRate_ = SR_HALF_CD;
	private int defaultBitRate_ = BR_8;
	private int defaultTempo_ = 60;	// Beat per minute. 
	private Vector<AudioFormat> supportedFormats_ = new Vector<>();
	
	public void setTempo(int t) {
		defaultTempo_ = t;
	}
	public Tone generateTone(double frequency, double beat) throws Exception{
		findSupportedFormats();
		int length = (int)(60000 * beat / defaultTempo_);
		return new Tone(frequency, defaultSampleRate_, length, ToneType.SQUARE, false);
	}
	private void findSupportedFormats() {
		if (supportedFormats_.isEmpty()) {
			Mixer mixer = AudioSystem.getMixer(null);
			try {
				mixer.open();
				for (Line.Info info : mixer.getSourceLineInfo()) {
					 if(SourceDataLine.class.isAssignableFrom(info.getLineClass())) {
						 SourceDataLine.Info supportedOutputInfo = (SourceDataLine.Info) info;
						 AudioFormat[]  formats = supportedOutputInfo.getFormats();
						 for (AudioFormat fmt : formats) {
							 supportedFormats_.add( fmt );
						 }
					 }
				}
			} catch (LineUnavailableException e) {
				e.printStackTrace();
			}
			// Change default value
			if (!supportedFormats_.isEmpty()) {
				defaultSampleRate_ = (int) supportedFormats_.get(0).getSampleRate();
			}
		}
	}
	public Clip generateAudioClipFromTones(List<Tone> tones) {
		Clip clip = null;
		try {
			ByteArrayOutputStream bo = new ByteArrayOutputStream();
			for (Tone t : tones) {
				bo.write(t.getBuffer());
			}
			
			clip = AudioSystem.getClip();

			AudioFormat format = new AudioFormat(defaultSampleRate_,
					defaultBitRate_, defaultChannels_, true, false);
			
			boolean foundMatch = false;
			for (AudioFormat fmt : supportedFormats_) {
				if ( format.matches(fmt) ) {
					foundMatch = true;
					format = fmt;
					break;
				}
			}
			if (!foundMatch) {
				format = supportedFormats_.firstElement();
			}
		
			byte[] bb = bo.toByteArray();
			AudioInputStream ais = new AudioInputStream(
					new ByteArrayInputStream(bb), format, bb.length);
			clip.open(ais);
		} catch (Exception e) {
			// e.printStackTrace();
			clip = null;
		}
		return clip;
	}


	public static void main(String[] args) {
		double[] freqs = {
				340.0
		};
		ToneGenerator tg = new ToneGenerator();
		List<Tone> tones = new ArrayList<>();
		for (double f : freqs) {
			try {
				tones.add(tg.generateTone(f, 0.0625));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		Clip clip = tg.generateAudioClipFromTones(tones);
		clip.loop(0);
		JFrame frame = new JFrame();
		frame.setVisible(true);
		
	}
}
