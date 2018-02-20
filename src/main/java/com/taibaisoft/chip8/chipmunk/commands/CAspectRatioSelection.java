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

import java.awt.Dimension;

import com.taibaisoft.chip8.chipmunk.Board;
import com.taibaisoft.chip8.chipmunk.ConfigAndPrefs;
import com.taibaisoft.framework.UICommand;

public class CAspectRatioSelection extends UICommand<Board> {

	@Override
	public String getText() {
		return "1:1;4:3;16:9;16:10;5:4;5:3;2:1";
	}

	@Override
	public String getID() {
		return "1:1;4:3;16:9;16:10;5:4;5:3;2:1";
	}

	@Override
	public String getDescription() {
		return "";
	}
	
	public int getDefaultRadioButton () {
		return ConfigAndPrefs.getInstance().getAspectRatioSelection();
	}

	@Override
	public void action(Board obj) {
		ConfigAndPrefs cap = ConfigAndPrefs.getInstance();
		String s = getRadioSelectionActionCommand();
		int index = getRadioSelectionIndex();
		
		try {
			String[] ss = s.split("\\:");

			int w = Integer.parseInt(ss[0]);
			int h = Integer.parseInt(ss[1]);
			float ar = w / (float) h;

			int ny = cap.getWindowSize();
			int nx = (int) (ny * ar);
			if (obj.getGraphicsUnit().getPhysicalSize().height != ny
					|| obj.getGraphicsUnit().getPhysicalSize().width != nx) {
				obj.getGraphicsUnit().setPhysicalSize(new Dimension(nx, ny));
				obj.pack();

				cap.setDefaultAspectRatio(ar);
				cap.setDefaultAspectRatioSelection(index);
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}

	}

}
