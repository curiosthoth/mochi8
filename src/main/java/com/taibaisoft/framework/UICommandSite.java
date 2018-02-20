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

import java.awt.AWTKeyStroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;


class JDummySeparator extends JMenuItem {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
}
public class UICommandSite<T> extends JFrame implements ActionListener, MenuListener {

	/**
	 * Format of auto radio names:
	 * AUTO_RADIO_PREFIX + "_" + Master Command Index (2 digit decimal) + "_" + Radio Action Command;
	 */
	static int s_counter = 0;
	final String AUTO_RADIO_PREFIX = "__auto198214rad";
	HashMap<String, UICommand<T>> cmds = new HashMap<String, UICommand<T>>();
	
	List<String> configCmdNames = new ArrayList<String>();
	
	private static final long serialVersionUID = 1L;
	public UICommandSite() {
		super();
	}
	public UICommandSite(String title) {
		super(title);
	}	
	
	/**
	 * Subclasses should call this method to setup its menu from menu.cfg.
	 * This will reload the whole menu, resetting it to initial state.
	 */
	protected void reloadMenu() {
		try {
			InputStream p = Resources.getInstance().open("menu.cfg");
			if(p!=null) {
				BufferedReader bf = new BufferedReader(
						new InputStreamReader(p)
				);
		        StringBuilder out = new StringBuilder();
		        String line;
		        while ((line = bf.readLine()) != null) {
		            out.append(line);
		        }
				setupMenu(out.toString());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}
	/**
	 * Core method for dynamic menu generation. Config is the whole config string.
	 * Below lists the options at the beginning of each line,
	 * 
	 * '#' - Comment 
	 * 'i' - A normal menu item
	 * '+' - Start of an submenu
	 * '-' - End of a submenu
	 * 'r' - Aggregated radio menu item, expandable into N entries,
	 * 'c' - Check box menu item.
	 * 's' - Separator.
	 * 'p' - The package in which to search for classes.
	 * 
	 * Each line must end with ";".
	 * For most of the options, except for -, s and p, the parameters must follow the format  
	 * [option], [Class Name], [Mnemonic], [Key Access];
	 * E.g, i, CLoad, O, control L;
	 * 
	 * @param config
	 */
	protected void setupMenu(String config) {
		JMenuBar menuBar = getJMenuBar();
		s_counter = 0;
		if (menuBar==null) {
			menuBar = new JMenuBar();
		} else {
			menuBar.removeAll();
		}
		
		String[] lines = config.split(";");
		int len = lines.length;
		int cursor = 0;
		Stack<JMenu> menuStack = new Stack<JMenu>();
		String defaultPackagePrefix = "";
		for (;cursor<len;++cursor) {
			if (lines==null) continue;
			String cl = lines[cursor].trim();
			
			if (cl.length()==0) continue;
			
			char ct = cl.charAt(0);	// Line type

			UICommand<T> cmd = null;
			int mnemonic = -1;
			
			String[] items = cl.split(",");
			
			if (ct=='#') { // For comments
				continue; 
			}
			
			if (ct=='p') { // "p" to setup default package prefix.
				if (items.length>1) {
					defaultPackagePrefix = items[1].trim();
				} else {
					System.err.println("Missing default package names.");
				}
				continue;
			}

			if (items.length>2) {
				String i2=items[2].trim();
				if (i2.length()>0) {
					mnemonic = AWTKeyStroke.getAWTKeyStroke(i2.charAt(0)).getKeyCode();
				}
			} 
			if (items.length>1) {
				String clsName = items[1].trim();
				cmd = instantiateClass(
						defaultPackagePrefix + "." + clsName, 
						com.taibaisoft.framework.UICommand.class
						);
				if (cmd!=null) {
					cmd.setUICommandSite(this);
					cmd.setContainer(this);
					String masterCmdAction="";
					if (ct=='r') {
						s_counter++;
						masterCmdAction = generateRadioButtonGroupMasterAction(s_counter);
					} else {
						masterCmdAction = cmd.getID();
					}
					cmds.put(masterCmdAction, cmd);
				}
			}
			
			if (ct=='+') {
				if (cmd != null) {
					JMenu mnu = new JMenu(cmd.getText());
					mnu.setMnemonic(mnemonic);
					mnu.setActionCommand(cmd.getID());
					mnu.addMenuListener(this);
					if (menuStack.empty()) {
						menuBar.add(mnu);
					} else {
						menuStack.peek().add(mnu);
					}
					menuStack.push(mnu);
					mnu.setVisible(cmd.isVisible());
				} else {
					System.err.println("Error loading the menu at line " + cursor);
				}
			} else if (ct=='-') {
				menuStack.pop();
			} else {
				JMenuItem mi = null;
				if (ct=='i') {
					if (cmd==null) {
						System.err.println("Cannot create command object for line " + cursor);
					}
					if (mnemonic==-1) {
						mi = new JMenuItem(cmd.getText());
					} else {
						mi = new JMenuItem(cmd.getText(), mnemonic);
					}
					mi.getAccessibleContext().setAccessibleDescription(cmd.getDescription());
					mi.setActionCommand(cmd.getID());
					cmd.setRelatedMenuItem(mi);
					mi.addActionListener(this);
					menuStack.peek().add(mi);
					if ( items.length>3 ) {
						String i3 = items[3].trim();
						if (i3.length()>0) {
							mi.setAccelerator(KeyStroke.getKeyStroke(items[3]));
						}
					}
					mi.setVisible(cmd.isVisible());
				} else if (ct=='s'){
					menuStack.peek().addSeparator();
				} else if (ct=='c') { 
					if (cmd==null) {
						System.err.println("Cannot create command object for line " + cursor);
					}
					mi = new JCheckBoxMenuItem(cmd.getText());
					mi.setSelected(cmd.isSelected());
					mi.setActionCommand(cmd.getID());
					cmd.setRelatedMenuItem(mi);
					mi.addActionListener(this);
					menuStack.peek().add(mi);
					if ( items.length>3 ) {
						String i3 = items[3].trim();
						if (i3.length()>0) {
							mi.setAccelerator(KeyStroke.getKeyStroke(items[3]));
						}
					}
					mi.setVisible(cmd.isVisible());
				} else if (ct=='r') {
					// Generates radio group!
					if (cmd.getID()!=null && cmd.getText()!=null) {
						String[] allIds = cmd.getID().split(";");
						String[] allTexts = cmd.getText().split(";");
						int nn = 0;
						if ((nn=allIds.length)==allTexts.length) {
							cmd.setAllIDs(allIds);
							ButtonGroup bg = new ButtonGroup();
							for (int j = 0; j < nn; ++j ) {
								String subId = allIds[j];
								String txt = allTexts[j];
								String mid = generateRadioButtonGroupSubAction(s_counter, subId);
								JRadioButtonMenuItem mri = new JRadioButtonMenuItem(txt);
								mri.setActionCommand(mid);
								mri.addActionListener(this);
								
								bg.add(mri);
								menuStack.peek().add(mri);
								if (cmd.getDefaultRadioButton()==j) {
									mri.setSelected(true);
								} else {
									mri.setSelected(false);
								}
								mri.setVisible(cmd.isVisible());
							}
						} else {
							System.err.println("Command ID array length does not match text array length.");
						}
					} else {
						System.err.println("Empty Command ID or Text for command : " + cmd.getClass().toString());
					}
				} else {
					System.err.println("Not supported!");
				}
			}
		}
		setJMenuBar(menuBar);
	}
	
	protected String generateRadioButtonGroupMasterAction(int counter) {
		return String.format(AUTO_RADIO_PREFIX +"_%02d", counter);
	}
	protected String generateRadioButtonGroupSubAction(int counter, String subId) {
		return String.format(AUTO_RADIO_PREFIX +"_%02d_%s", counter, subId);
	}
	protected String[] extractRadioButtonGroupMasterAndSubAction(String s) {
		String master = s.substring(0, 18);
		String sub = s.substring(19);
		return new String[]{master,sub};
	}
	@SuppressWarnings("unchecked")
	protected UICommand<T> instantiateClass(final String name, @SuppressWarnings("rawtypes") final Class<UICommand> class1) {
		try {
			return class1.cast(Class.forName(name).newInstance());
		} catch (InstantiationException | IllegalAccessException
				| ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void actionPerformed(ActionEvent e) {
		String acmd = e.getActionCommand();
		if ( null!=acmd ) {
			String subCmd = null;
			if (acmd.startsWith(AUTO_RADIO_PREFIX)) {
				String[] as = extractRadioButtonGroupMasterAndSubAction(acmd);
				acmd = as[0];
				subCmd = as[1];
				
			}
			UICommand<T> cmd = cmds.get(acmd);
			if (cmd!=null) {
				if (subCmd!=null) {
					cmd.setRadioSelectionActionCommand(subCmd);
				}
				cmd.action((T)this);
			} else {
				System.err.println("No available UICommand object found.");
			}
		}
	}
	public UICommand<T> getCommandById(String id) {
		if (id!=null) {
			return cmds.get(id);
		} else {
			return null;
		}
	}
	@SuppressWarnings("unchecked")
	@Override
	public void menuSelected(MenuEvent e) {
		String acmd = ((JMenu)(e.getSource())).getActionCommand();
		if (acmd!=null) {
			UICommand<T> cmd = cmds.get(acmd);
			if (cmd!=null) {
				cmd.action((T)this);
			}
		}
	}
	@Override
	public void menuDeselected(MenuEvent e) {
		
	}
	@Override
	public void menuCanceled(MenuEvent e) {
		
	}

}
