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

import javax.swing.JOptionPane;

import com.taibaisoft.chip8.chipmunk.Board;
import com.taibaisoft.chip8.processor.Chip8;
import com.taibaisoft.chip8.processor.Chip8.Mode;
import com.taibaisoft.framework.UICommand;

public class CRun extends UICommand<Board> {

	@Override
	public String getText() {
		return "Run";
	}

	@Override
	public String getID() {
		return "EV_RUN";
	}

	@Override
	public String getDescription() {
		return "Run/Reset current loaded program.";
	}

	@Override
	public void action(Board board) {
		Chip8 chip = board.getCPU();
		if (chip.hasStartedRunning()) {
			chip.reset();
			board.getGraphicsUnit().clearScreen();
			updateTextAndIcon("Run", null);
			getOtherUICommand("EV_PAUSE").updateTextAndIcon("Pause", null);
			board.setTitlePostfix(Board.ST_STOPPED);
		} else {
			if (board.getCurrentProgram()==null) {
				getOtherUICommand("EV_LOAD").action(board);
			}
			
			try {
				chip.loadProgram(board.getCurrentProgram(), Mode.CHIP_8, board.getLoadingAddress());
				chip.run();
				if (chip.hasStartedRunning()) {
					updateTextAndIcon("Reset", null);
					board.setTitlePostfix(Board.ST_RUNNING);
				}
			} catch (Exception e) {
				alert("Error running/loading program.", JOptionPane.ERROR_MESSAGE);
			}
		}
		
	}

}
