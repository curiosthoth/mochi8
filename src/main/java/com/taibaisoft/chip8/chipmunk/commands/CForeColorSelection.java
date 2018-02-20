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

package com.taibaisoft.chip8.chipmunk.commands;

import java.awt.Color;

import com.taibaisoft.chip8.chipmunk.Board;
import com.taibaisoft.chip8.chipmunk.ConfigAndPrefs;
import com.taibaisoft.framework.UICommand;

public class CForeColorSelection extends UICommand<Board> {

	@Override
	public String getText() {
		return "Light Gray;Cyan;White;Light Green;Yellow;Dark Gray;Dark Blue";
	}

	@Override
	public String getID() {
		return "ffc0c0c0;ff00ffff;ffffffff;ff9edc9c;fffffda2;ff404040;ff00058d";
	}

	@Override
	public String getDescription() {
		return null;
	}

	public int getDefaultRadioButton() {
		return ConfigAndPrefs.getInstance().getForeColorSelection();
	}
	@Override
	public void action(Board obj) {
		ConfigAndPrefs cap = ConfigAndPrefs.getInstance();
		int index = getRadioSelectionIndex();
		String a = getRadioSelectionActionCommand();
		try {
			Color fc = new Color((int)Long.parseLong(a, 16));
			cap.setDefaultForeColor(fc);
			cap.setDefaultForeColorSelection(index);
			obj.getGraphicsUnit().setPixelColor(fc);
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}	
	}

}
