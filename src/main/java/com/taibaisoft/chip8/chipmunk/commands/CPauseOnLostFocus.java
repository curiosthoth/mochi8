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

import com.taibaisoft.chip8.chipmunk.Board;
import com.taibaisoft.chip8.chipmunk.ConfigAndPrefs;
import com.taibaisoft.framework.UICommand;

public class CPauseOnLostFocus extends UICommand<Board> {

	@Override
	public String getText() {
		return "Pause on Lost Focus";
	}

	@Override
	public String getID() {
		return "EV_PAUSE_ON_LOST_FOCUS";
	}

	@Override
	public String getDescription() {
		return "Set if auto pause the program if the emulator window lost focus.";
	}
	public boolean isSelected() {
		return ConfigAndPrefs.getInstance().getPauseOnLostFocus();
	}
	@Override
	public void action(Board obj) {
		ConfigAndPrefs cap = ConfigAndPrefs.getInstance();
		boolean f = !cap.getPauseOnLostFocus();
		cap.setDefaultPauseOnLostFocus(f);
		obj.setPauseOnLostFocus(f);
	}

}
