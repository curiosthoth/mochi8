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
// TODO: This is only a place holder for future "Development Mode" feature, which is an
// integrated environment enabling people to debug/trace CHIP-8 programs.
package com.taibaisoft.chip8.chipmunk;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

@SuppressWarnings("serial")
class ConsolePane extends JPanel {
	
}

@SuppressWarnings("serial")
class WatchPane extends JPanel {
	
}
@SuppressWarnings("serial")
class DasmPane extends JPanel {	
}
@SuppressWarnings("serial")
public class DevPane extends JFrame
{
	private JSplitPane vSplitPane1;
	private JSplitPane hSplitPane1;
	
	private ConsolePane consolePane;
	private DasmPane dasmPane;
	private WatchPane watchPane;
	
    public DevPane()
    {
        super("");
        setFocusableWindowState( false );
        setAlwaysOnTop(true);
        // setLayout( null );
        getContentPane().setBackground( Color.LIGHT_GRAY );
        setupGui();
    }
    private void setupGui () {
    	/**
    	 * +------+------+
    	 * + DASM + Regs +
    	 * +      +------+
    	 * +      + Vars +
    	 * +      +      +
    	 * +------+------+
    	 * +  Console    +
    	 * +             +
    	 * +-------------+
    	 * +Immediate    +
    	 * +-------------+
    	 */
    	consolePane = new ConsolePane();
    	dasmPane = new DasmPane();
    	watchPane = new WatchPane();
    	hSplitPane1 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
    				dasmPane, watchPane);
    	hSplitPane1.setOneTouchExpandable(true);
    	hSplitPane1.setDividerLocation(500);
    	
    	vSplitPane1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, 
    				hSplitPane1, consolePane);
    	
    	/* Set Layout*/
    	setLayout(new BorderLayout());
    	add(vSplitPane1, BorderLayout.CENTER);
    	
    }
    
    public static void main(String args[]) {
	       try {
	            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
	        } catch (UnsupportedLookAndFeelException ex) {
	            ex.printStackTrace();
	        } catch (IllegalAccessException ex) {
	            ex.printStackTrace();
	        } catch (InstantiationException ex) {
	            ex.printStackTrace();
	        } catch (ClassNotFoundException ex) {
	            ex.printStackTrace();
	        }
	        javax.swing.SwingUtilities.invokeLater(new Runnable() {
	            public void run() {
	                DevPane frame = new DevPane();
	                frame.setSize(new Dimension(640, 460));
	                frame.setVisible(true);
	            }
	            
	        });
    }
}