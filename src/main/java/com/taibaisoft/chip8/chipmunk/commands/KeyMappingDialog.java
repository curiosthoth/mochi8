package com.taibaisoft.chip8.chipmunk.commands;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

import com.taibaisoft.chip8.chipmunk.ConfigAndPrefs;

@SuppressWarnings("serial")
public class KeyMappingDialog extends JDialog implements ActionListener, KeyListener, FocusListener {
	
	private JToggleButton[] keys = new JToggleButton[16];
	private boolean firstTimeInfo_ = false;
	public static String[] keyboardPosKeyMap = {
		"1","2","3","C",
		"4","5","6","D",
		"7","8","9","E",
		"A","0","B","F",
	};
	
	private int[] keycodeMap_ = null;

	public KeyMappingDialog(JFrame parent, String title, int[] currentKeyMapping) {
		this(parent, title, currentKeyMapping, false);
	}
	public KeyMappingDialog(JFrame parent, String title, int[] currentKeyMapping, boolean firstTimeInfo) {
		super(parent, title, true);
		firstTimeInfo_ = firstTimeInfo;
		keycodeMap_ = currentKeyMapping;
		setupGUI(parent);
	}
	
	protected void setupGUI(JFrame parent) {
		
		if (parent != null) {
			Dimension parentSize = parent.getSize();
			Point p = parent.getLocation();
			setLocation(p.x + parentSize.width / 4, p.y + parentSize.height / 4);
		}
		JPanel msgPane = new JPanel();
		String tip = "Please click on a key button and then press a new key.";
		if ( firstTimeInfo_) {
			tip = "<html>This is your first time running Mochi8 Emulator.<br>"
					+ "Below listed the default key mapping.<br>"
					+ "Later you can access <b>Emulator->Edit Keys... </b> <br>"
					+ "to change the default key mapping.<br>"
					+ "You will not see this popup again. </html>";
			setTitle("Tip: Key Mapping");
		}
		JLabel lblTip = new JLabel(tip);
		msgPane.add(lblTip);
		getContentPane().add(msgPane, BorderLayout.NORTH);
		
		
		JPanel keysPane = new JPanel();
		
		keysPane.setLayout(new GridLayout(4,4));
		
		addKeyListener(this);
		
		ButtonGroup group = new ButtonGroup();
		
		for (int i= 0;i<16;++i) {
			JPanel jpn = new JPanel();
			keys[i] = new JToggleButton();
			keys[i].setBackground(Color.GRAY);			
			keys[i].setText(new String(new char[]{(char)keycodeMap_[i]}));
			keys[i].setActionCommand(""+i);
			keys[i].addActionListener(this);
			keys[i].addKeyListener(this);
			keys[i].setEnabled(!firstTimeInfo_);
			
			JLabel lbl = new JLabel(keyboardPosKeyMap[i]);
			lbl.setHorizontalAlignment(SwingConstants.CENTER);
			lbl.setForeground(Color.GRAY);
			lbl.setFont(Font.getFont("Arial Black"));
			
			
			jpn.setLayout(new GridLayout(2,1));
			jpn.add(keys[i]);
			jpn.add(lbl);
			
			group.add(keys[i]);
			
			keysPane.add(jpn);
		}	
		getContentPane().add(keysPane);
		
		
		JPanel buttonPane = new JPanel();
		
		JButton button = new JButton("OK");
		buttonPane.add(button);
		button.addActionListener(this);
		button.setActionCommand("OK");
		if (!firstTimeInfo_) {
			button = new JButton("Cancel");
			buttonPane.add(button);
			button.addActionListener(this);
			button.setActionCommand("Cancel");

			button = new JButton("Reset Default");
			buttonPane.add(button);
			button.addActionListener(this);
			button.setActionCommand("Reset");
		}
		getContentPane().add(buttonPane, BorderLayout.SOUTH);	
	}
	public void showMe() {
		setModalityType(ModalityType.APPLICATION_MODAL);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		pack();
		setVisible(true);		
	}

	public void actionPerformed(ActionEvent e) {
		// setVisible(false);
		String cmd = e.getActionCommand();
		if (cmd.compareTo("OK")==0) {
			setVisible(false);
			ConfigAndPrefs.getInstance().setKeyMappings(keycodeMap_);
			dispose();
		} else if (cmd.compareTo("Cancel")==0){
			setVisible(false);
			dispose();
		} else if (cmd.compareTo("Reset")==0) {
			byte[] a = ConfigAndPrefs.getInstance().getDefaultKeyMappings();
			int len = a.length;
			int i = 0;
			for (;i<len; ++i) {
				keycodeMap_[i] = (int)a[i];
				keys[i].setText(new String(new char[]{(char)a[i]}));
			}
			
		}
	}
	
	@Override
	public void keyPressed(KeyEvent arg0) {
	
	}

	@Override
	public void keyReleased(KeyEvent arg0) {
		if (firstTimeInfo_) {
			return;
		}
		JToggleButton btn = (JToggleButton)arg0.getSource();
		if (btn.isSelected()) {
			int index = Integer.parseInt(btn.getActionCommand()); 
			int kc = arg0.getKeyCode();
			keycodeMap_[index] =kc;		
			btn.setText(new String(new char[]{(char)kc}));
			btn.setSelected(false);
		} 
	}

	@Override
	public void keyTyped(KeyEvent arg0) {
	}

	@Override
	public void focusGained(FocusEvent e) {
		for (JToggleButton b : keys) {
			if (b!=null && b.isSelected()) {
				b.requestFocusInWindow();
				break;
			}
		}
		
	}

	@Override
	public void focusLost(FocusEvent e) {
		// TODO Auto-generated method stub
		
	}
	
}