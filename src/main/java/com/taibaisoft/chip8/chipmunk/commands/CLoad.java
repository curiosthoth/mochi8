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
import java.awt.FlowLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.taibaisoft.chip8.chipmunk.Board;
import com.taibaisoft.chip8.chipmunk.ConfigAndPrefs;
import com.taibaisoft.framework.UICommand;

class AddressAccessory extends JPanel implements PropertyChangeListener {
	private static final long serialVersionUID = 1L;
	private static final int PREFERRED_WIDTH = 125;
	private static final int PREFERRED_HEIGHT = 100;
	JTextField address = new JTextField(5);

	public AddressAccessory(JFileChooser chooser) {
		JLabel label = new JLabel("Base Addr (Hex)");
		label.setVerticalAlignment(JLabel.CENTER);
		label.setHorizontalAlignment(JLabel.LEFT);

		address.setText("200");
		chooser.addPropertyChangeListener(this);

		setLayout(new FlowLayout());
		add(label);
		add(address);
		setPreferredSize(new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT));
	}

	public void propertyChange(PropertyChangeEvent changeEvent) {

	}

	public int getAddress() {
		int i = 0x200;
		try {
			i = Integer.parseInt(address.getText(), 16);
		} catch (Exception e) {
			i = 0x200;
		}
		return i;
	}
}

public class CLoad extends UICommand<Board> {

	@Override
	public String getText() {
		return "Load";
	}

	@Override
	public String getID() {
		return "EV_LOAD";
	}

	@Override
	public String getDescription() {
		return "Loads CHIP8 or SCHIP program.";
	}

	@Override
	public void action(Board board) {
		ConfigAndPrefs cap = ConfigAndPrefs.getInstance();
		
		boolean debug = false;
		if (debug) {
	    	byte[] s = null;
	    	try {
				s = Files.readAllBytes(Paths.get(
			"D:\\workspace\\projects\\mochi8\\Chip-8 Pack\\Chip-8 Games\\Brix [Andreas Gustafsson, 1990].ch8"));
			} catch (IOException e) {
				alert(e.getMessage(), JOptionPane.ERROR_MESSAGE);
				e.printStackTrace();
			}
			board.setCurrentProgram(s, 0x200);
			board.getCPU().reset();
			if (cap.getAutoRunOnLoad()) {
				getOtherUICommand("EV_RUN").action(board);
			}
		} else {
			
			
			JFileChooser chooser = new JFileChooser(new File(cap.getLastVisitedPath()));
			chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			chooser.setMultiSelectionEnabled(false);		
		    FileNameExtensionFilter filter = new FileNameExtensionFilter(
		        "CHIP-8/CHIP-48/SCHIP", "ch8", "c8", "c48", "ch48", "sc", "c8x"
		        );
		    chooser.setFileFilter(filter);
		    
			AddressAccessory addressAcc = new AddressAccessory(chooser);
			chooser.setAccessory(addressAcc);
		    
		    int returnVal = chooser.showOpenDialog(this.getContainer());
		    if(returnVal == JFileChooser.APPROVE_OPTION) {
		    	File f = chooser.getSelectedFile();
		    	/* Updates the preferences */
		    	cap.setLastVisitedPath(f.getParentFile().getAbsolutePath());
		    	
		    	byte[] s = null;
		    	try {
		    		Path p = Paths.get(f.getAbsolutePath());
					s = Files.readAllBytes(p);
			    	if (s!=null) {
			    		board.setCurrentProgramPath(p.getFileName().toString());
						board.setCurrentProgram(s, addressAcc.getAddress());
						board.getCPU().reset();
						if (cap.getAutoRunOnLoad()) {
							getOtherUICommand("EV_RUN").action(board);
						}
			    	}
				} catch (IOException e) {
					e.printStackTrace();
					s = null;
					alert(e.getMessage(), JOptionPane.ERROR_MESSAGE);
				}

		    }
		}

	}

}
