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

package com.taibaisoft.chip8.chipmunk;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.image.BufferStrategy;

import com.taibaisoft.chip8.processor.IScreen;

public class GraphicsUnit2 extends Canvas implements IScreen {
	private static final long serialVersionUID = -573533608293529475L;
	private int H = 0, V = 0;
	private int width = 0, height = 0;
	private double pux = 0, puy = 0;
	private volatile int[] data = null;
	private Color fc = Color.LIGHT_GRAY;
	boolean init = false;
	volatile boolean running = false;
	BufferStrategy bf = null;
	Thread renderThread = null;
	public GraphicsUnit2() {
		setBackground(Color.BLACK);	
	}
	public void init() {
		createBufferStrategy(2);
	
		init = true;
		// Starts the loop
		running = true;
		renderThread = new Thread (new Runnable() {
			@Override
			public void run() {
				bf = getBufferStrategy();
				while (running) {
					Graphics g = null;
					try {
						g = bf.getDrawGraphics();
						draw_(g);
						//Thread.sleep(1);
					} catch (Exception e) {
						if (g != null)
							g.dispose();
					}
					if (!bf.contentsLost()) {
						bf.show();
						Toolkit.getDefaultToolkit().sync();
					}
				}
			}
		});
		renderThread.start();
	}
	public void setPixelColor(Color clr){
		fc = clr;
	}
	public void setBackgroundColor(Color clr) {
		setBackground(clr);
	}
	
	public void draw(int[] buff, double d, int nx, int ny) {		
		if (buff==null) return;
		data = buff;
        H = nx;
        V = ny;

        width = getWidth(); 
        height = getHeight();
        
        pux = (double)width/nx *.5f;
        puy = (double)height/ny *.5f; // half size of Virtual PIXEL width and height
	}
	
	public void clearScreen() {
		data = null;
	}
	@Override
	public Dimension getPhysicalSize() {
		return getPreferredSize();
	}	
	protected void draw_(Graphics g) {
		if (!init) return;
		int dl = H * V;
		Dimension d = getSize();
		g.clearRect(0, 0, d.width, d.height);
		if (data != null) {
			g.setColor(fc);
			for (int i = 0; i < dl; ++i) {
				if (data[i] == 1) {
					g.fillRect((int) (((i % H) * 2) * pux),
							(int) (((i / H) * 2) * puy), (int) (2 * pux) + 1,
							(int) (2 * puy) + 1);
				}
			}
		}
		
	}
	@Override
	public void setPhysicalSize(Dimension dim) {
		setPreferredSize(dim);
	}


}
