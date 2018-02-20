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

public class CBackColorSelection extends UICommand<Board> {

	@Override
	public String getText() {
		return "Black;Dark Blue;Dark Gray;Dark Green;Light Gray;Cyan";
	}

	@Override
	public String getID() {
		return "ff000000;ff00058d;ff404040;ff035500;ffc0c0c0;ff00ffff";
	}

	@Override
	public String getDescription() {
		return "";
	}
	
	public int getDefaultRadioButton() {
		return ConfigAndPrefs.getInstance().getBackColorSelection();
	}

	@Override
	public void action(Board obj) {
		ConfigAndPrefs cap = ConfigAndPrefs.getInstance();
		int index = getRadioSelectionIndex();
		String a = getRadioSelectionActionCommand();
		try {
			Color bg = new Color((int)Long.parseLong(a, 16));
			cap.setDefaultBackColor(bg);
			cap.setDefaultBackColorSelection(index);

			obj.getGraphicsUnit().setBackgroundColor(bg);
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}	
	}
	public static void main (String[] args) {
		System.out.println(Long.parseLong("FF00058D", 16));
	}

}
