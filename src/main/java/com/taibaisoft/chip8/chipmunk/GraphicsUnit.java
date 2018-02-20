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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.util.Arrays;

import javax.swing.JPanel;

import com.taibaisoft.chip8.processor.IScreen;

public class GraphicsUnit extends JPanel implements IScreen {
	private static boolean DIFF_UPDATE = false;
	private static final long serialVersionUID = -573533608293529475L;
	private int H = 0, V = 0, L = 0, R = 0, T = 0, B = 0;
	private int width = 0, height = 0;
	private double pux = 0, puy = 0;
	private volatile int[] data = null;
	private volatile int[] prev = null;
	@SuppressWarnings("unused")
	private double delta = 0;
	private Color fc = Color.LIGHT_GRAY;
	public GraphicsUnit() {
		//setPreferredSize(new Dimension(480, 360));
		setBackground(Color.BLACK);	
		setDoubleBuffered(true);
		setOpaque(true);
	}
	public void setPixelColor(Color clr){
		fc = clr;
		repaint();
	}
	public void setBackgroundColor(Color clr) {
		setBackground(clr);
		repaint();
	}
	
	public void draw(int[] buff, double d, int nx, int ny) {
		if (buff==null) return;
		data = buff;
		delta = d;
        H = nx;
        V = ny;
		
        Insets iss = getInsets();
        L = iss.left;
        R = iss.right;
        T = iss.top;
        B = iss.bottom;

        width = getWidth() - L - R; 
        height = getHeight() - T - B;
        
        pux = (double)width/nx *.5f;
        puy = (double)height/ny *.5f; // half size of Virtual PIXEL width and height
        int dl = H*V;
        
        if (DIFF_UPDATE) {
			if (prev != null) {
				int top = -1, left = -1, right = -1, bottom = -1;
				for (int i = 0; i < dl; ++i) {
					int x = i % H;
					int y = i / H;
					if (data[i] != prev[i]) {
						if (top > y || top == -1) {
							top = y;
						}
						if (left > x || left == -1) {
							left = x;
						}
					} else {
						if (right < x || right == -1) {
							right = x;
						}
						if (bottom < y || bottom == -1) {
							bottom = y;
						}

					}
				}
				repaint(L + (int) (left * 2 * pux) - 1, T
						+ (int) (top * 2 * puy) - 1,
						(int) (2 * (right - left + 1) * pux) + 1,
						(int) (2 * (bottom - top + 1) * puy) + 1);
			} else {
				repaint();
        }
        prev = Arrays.copyOf(data, data.length);
        } else {
        	repaint();
        }
	}
	@Override
	public void clearScreen() {
		data = null;
		prev = null;
		repaint();
	}
	@Override
	public void setPhysicalSize(Dimension dim) {
		setPreferredSize(dim);
	}
	@Override
	public Dimension getPhysicalSize() {
		return getPreferredSize();
	}	
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		int dl = H*V;
		Graphics2D g2 = (Graphics2D)g;
		g.setColor(fc);
        if (data!=null && data.length>=dl) {
        	for (int i = 0; i < dl; ++i) {
        		if (data[i]==1) {
        			g2.fillRect( L+ (int)( ((i%H) * 2 ) * pux), T + (int)( ((i/H) * 2) * puy), (int)(2*pux)+1, (int)(2*puy)+1 );
        		}
        	}
        }
 	}

}