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

import java.awt.Container;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

public abstract class UICommand<T> {
	
	private Container con = null;
	private JMenuItem relatedMenuItem = null;
	protected String radioCmd_ = "";
	protected String[] radioAllIds_ = null;
	protected UICommandSite<T> commandSite_ = null;
	
	public void setUICommandSite( UICommandSite<T> csref) {
		commandSite_ = csref;
	}
	
	public void setRelatedMenuItem(JMenuItem m) {
		relatedMenuItem = m;
	}
	public JMenuItem getRelatedMenuItem() {
		return relatedMenuItem;
	}
	
	public void setContainer(Container c) {
		con = c;
	}
	public void updateTextAndIcon(String newText, String newIcon) {
		relatedMenuItem.setText(newText);
		if (newIcon!=null) {
			relatedMenuItem.setIcon(null);
		}

	}
	public UICommand<T> getOtherUICommand(String commandId) {
		return commandSite_.getCommandById(commandId);
	}
	
	public Container getContainer() {
		return con;
	}

	/**
	 * For checkbox menu item only;
	 * @return
	 */
	public boolean isSelected() {
		return false;
	}
	// ---- For Radio button group ----
	/**
	 * Used internally. Not for end users.
	 * @param allIds
	 */
	public void setAllIDs(String[] allIds) {
		radioAllIds_ = allIds;
	}
	/**
	 * 
	 * @return The index of the default selected radio button.
	 */
	public int getDefaultRadioButton() {
		return 0;
	}
	public void setRadioSelectionActionCommand(String s) {
		radioCmd_ = s;
	}
	/**
	 * To be used in action() method. Gets the currently selected radio option whenever 
	 * the selection changed.
	 * @return
	 */
	protected String getRadioSelectionActionCommand() {
		return radioCmd_;
	}
	/**
	 * To be used in action() method. Gets the currently selected radio option index.
	 * @return
	 */
	protected int getRadioSelectionIndex() {
		int i = 0;
		for (; i < radioAllIds_.length; ++i) {
			if ( radioCmd_.equalsIgnoreCase(radioAllIds_[i]) ) {
				break;
			}
		}
		return i;
	}
	
	public void alert(String message, int messageType) {
		JOptionPane.showMessageDialog(con, message, "Chipmunk", messageType);
	}
	public boolean isVisible() {
		return true;
	}
	
	abstract public String getText();
	abstract public String getID();
	abstract public String getDescription();
	public String getIconPath() {
		return null;
	}
	abstract public void action(T obj);
	

	public static void main(String[] args) {


	}
	
	

}
