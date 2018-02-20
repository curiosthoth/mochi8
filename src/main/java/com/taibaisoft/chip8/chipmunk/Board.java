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

package com.taibaisoft.chip8.chipmunk;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Properties;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.taibaisoft.chip8.assemblers.AsmMain;
import com.taibaisoft.chip8.chipmunk.commands.CExit;
import com.taibaisoft.chip8.chipmunk.commands.KeyMappingDialog;
import com.taibaisoft.chip8.platform.OSXAdapter;
import com.taibaisoft.chip8.platform.Platforms;
import com.taibaisoft.chip8.platform.Util;
import com.taibaisoft.chip8.processor.Chip8;
import com.taibaisoft.chip8.processor.ExitException;
import com.taibaisoft.chip8.processor.IBuzzer;
import com.taibaisoft.chip8.processor.IKeyboard;
import com.taibaisoft.chip8.processor.IScreen;
import com.taibaisoft.framework.IExceptionHandler;
import com.taibaisoft.framework.Resources;
import com.taibaisoft.framework.Tone;
import com.taibaisoft.framework.ToneGenerator;
import com.taibaisoft.framework.UICommandSite;

@SuppressWarnings("serial")
public class Board extends UICommandSite<Board> implements IKeyboard, IBuzzer, IExceptionHandler ,KeyListener, FocusListener {

	private static int DEFAULT_LOADING_ADDRESS=0x200;

	private byte[] keystates_ = new byte[16];
	private boolean muted_ = false;
	private boolean pauseOnLostFocus_ = false;
	
	private boolean passivePaused = false;
	private String currentProgramPath_ = "";
	private byte[] currentProgram_ = null;
	
	
	private int loadingAddress_ = DEFAULT_LOADING_ADDRESS;
	public static final String ST_PAUSED = "PAUSED";
	
	
	public static final String ST_RUNNING = "RUNNING";
	
	public static final String ST_STOPPED = "STOPPED";
	private int[] keycodeMap_ = new int[] {
			KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4,
			KeyEvent.VK_Q, KeyEvent.VK_W, KeyEvent.VK_E, KeyEvent.VK_R,
			KeyEvent.VK_A, KeyEvent.VK_S, KeyEvent.VK_D, KeyEvent.VK_F,	
			KeyEvent.VK_Z, KeyEvent.VK_X, KeyEvent.VK_C, KeyEvent.VK_V,	
	};
	private int[] posKeyMap_ = new int[] {
			1,  2,  3,0xC,
			4,  5,  6,0xD,
			7,  8,  9,0xE,
			0xA,0,0xB,0xF
	};
	
	private Chip8 chip = null;
	private IScreen screen = null;
	private AudioInputStream audioStream;
	private Clip audioClip = null;
	
	private static final String TITLE_PRE = "Mochi8";
	private static Board frame = null;
	public static void start() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			if (Platforms.getOSType()==Platforms.OsType.MAC_OS_X) {
				System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Mochi8 Emulator");
				System.setProperty("com.apple.mrj.application.apple.menu.about.version", "0.95");
				System.setProperty("apple.laf.useScreenMenuBar", "true");
			}
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
                create();
            }
            
        });  		    	
    }
	
	/**
	 * Entry point.
	 */
    private static void create() {
        frame = new Board();
       
        frame.init();
        
        frame.pack();
        frame.placeMe();
        frame.setVisible(true);
        
        if (Platforms.getOSType()==Platforms.OsType.MAC_OS_X) {
        	try {
				OSXAdapter.setAboutHandler(frame, frame.getClass().getDeclaredMethod("about", (Class[])null));
				OSXAdapter.setQuitHandler(frame, frame.getClass().getDeclaredMethod("quit", (Class[])null));
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        // Loading ok, first time running test now.
        if ( ConfigAndPrefs.getInstance().isFirstTime() ) {
        	new KeyMappingDialog(frame, "", frame.keycodeMap_, true).showMe();
        }
    }
	
	private Board() {
		super(TITLE_PRE);
	}
	public void about() {

		JOptionPane.showMessageDialog(this, Util.getVersionString("Emulator"),
				"About Mochi8 Emulator", JOptionPane.PLAIN_MESSAGE);
	}

	@Override
	public void buzz() {
		if (!muted_ && audioClip != null) {
			if (!audioClip.isRunning()) {
				audioClip.stop();
				audioClip.setFramePosition(0);
				audioClip.start();
			}
		}
	}
    @Override
	public void focusGained(FocusEvent arg0) {
		/**
		 * Ignores focus events from parents.
		 */
		if ( arg0.getOppositeComponent() instanceof JRootPane ) {
			return;
		}
		if (pauseOnLostFocus_) {
			if (passivePaused && chip.isPaused()) {
				passivePaused = false;
				chip.resume();
				setTitlePostfix(ST_RUNNING);
			}
		}
	}

	@Override
	public void focusLost(FocusEvent arg0) {
		/**
		 * Ignores focus events from parents.
		 */
		if ( arg0.getOppositeComponent() instanceof JRootPane ) {
			return;
		}
		if (pauseOnLostFocus_) {
			if (!chip.isPaused()) {
				passivePaused = true;
				chip.pause();
				setTitlePostfix(ST_PAUSED);
			}
		}
	}
    public Chip8 getCPU() {
    	return chip;
    }
    
    public byte[] getCurrentProgram() {
    	return currentProgram_;
    }
    public IScreen getGraphicsUnit() {
    	return screen;
    }
    @Override
	public byte[] getKeyboardState() {
		return keystates_;
	}
    public int[] getKeycodeMap() {
		return keycodeMap_;
	}
    public int getLoadingAddress() {
    	return loadingAddress_;
    }
    @Override
	public void keyPressed(KeyEvent arg0) {
		int key = arg0.getKeyCode();
		for (int i = 0; i < 16; ++i) {
			if (key==keycodeMap_[i]) {
				keystates_[posKeyMap_[i]] = 1;
			}
		}
	}
    @Override
	public void keyReleased(KeyEvent arg0) {
		int key = arg0.getKeyCode();
		for (int i = 0; i < 16; ++i) {
			if (key==keycodeMap_[i]) {
				keystates_[posKeyMap_[i]] = 0;
			}
		}
	}
    @Override
	public void keyTyped(KeyEvent arg0) {
	}
    @Override
	public boolean onException(Exception e) {
		if (!(e instanceof ExitException)) {
			getGraphicsUnit().clearScreen();
			String msg = e.getMessage();
			if (e instanceof ArrayIndexOutOfBoundsException) {
				msg = "Access addresses out of bounds.";
			}
			setCurrentProgramPath("");
			setCurrentProgram(null, DEFAULT_LOADING_ADDRESS);
			setTitlePostfix("");

			JOptionPane.showMessageDialog(this, msg, "Exception!",
					JOptionPane.ERROR_MESSAGE);
			reloadMenu();
			return false;
		}
		return true;
	}
	public void quit() {
		CExit quit = new CExit();
		quit.action(this);
	}

	public void setCurrentProgram(byte[] cp, int loadingAddress) {
    	currentProgram_ = cp;
    	loadingAddress_ = loadingAddress;
    }

	public void setCurrentProgramPath(String path) {
    	currentProgramPath_ = path;
    }
	
	public void setKeycodeMap(int[] km) {
		keycodeMap_ = km;
	}

	public void setMuted(boolean f) {
    	muted_ = f;
    }	
	public void setPauseOnLostFocus(boolean f) {
    	pauseOnLostFocus_ = f;
    }
	public void setTitlePostfix(String state) {
    	//String fname = new File(currentProgramPath_).getName();
    	if (currentProgramPath_.length()==0) {
    		setTitle(TITLE_PRE);
    	} else {
    		setTitle(TITLE_PRE +" - " + state + " - " + currentProgramPath_);
    	}
    }
	
	private void init() {
		/* 1. Initializes the CHIP and the graphics unit. */
		screen = new GraphicsUnit();
		chip = new Chip8(screen, this, this, this);
		
		/* 2. Loads default configurations. */
		ConfigAndPrefs cap = ConfigAndPrefs.getInstance();
		
		pauseOnLostFocus_ = cap.getPauseOnLostFocus();		
		chip.setCpuFrequency(cap.getSpeed());		
		keycodeMap_ = cap.getKeyMappings();
		
		/* For window's size and aspect */
		float ar = cap.getAspectRatio();
		int height = cap.getWindowSize();
		int width = (int)(ar * height);		
		screen.setPhysicalSize(new Dimension(width, height));
		screen.setPixelColor(cap.getForeColor());
		screen.setBackgroundColor(cap.getBackColor());
		
		setAlwaysOnTop(cap.getAlwaysOnTop());
		
		muted_ = cap.getMute();
		
		/* 3. Generates the audio clip */
		boolean aloaded = false;
		ToneGenerator tg = new ToneGenerator();
		Tone t;
		try {
			t = tg.generateTone(341.0, 0.062);
			audioClip = tg.generateAudioClipFromTones(Collections.singletonList(t));
			aloaded = true;
		} catch (Exception e2) {
			aloaded = false;
			
		}
		/* 3.1 Fallback for audio clip loading. */
		if (!aloaded || audioClip==null) {
			try {
				InputStream is = Resources.getInstance().open("340-s.wav");
				if (is==null) {
					throw new IOException("Cannot file audio file.");
				}
				audioStream = AudioSystem.getAudioInputStream(new BufferedInputStream(is));
				AudioFormat format = audioStream.getFormat();
				DataLine.Info info = new DataLine.Info(Clip.class, format);
				audioClip = (Clip) AudioSystem.getLine(info);
				audioClip.open(audioStream);
			} catch (UnsupportedAudioFileException | IOException e1) {
				audioClip = null;
			} catch (LineUnavailableException e1) {
				audioClip = null;
			}
		}
		
		/* 4. Load the menu! - From parent */
		reloadMenu();
	
		/* 5. More Layout and UI stuff, associate event listeners. */
        getContentPane().add((Component)screen, BorderLayout.NORTH);
        
        addKeyListener(this);
        addFocusListener(this);
        
		/* 6. Configure the window for some default behaviors. */
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				try {
					Point p = getLocation();
					ConfigAndPrefs.getInstance().setDefaultWindowPos(p);
					if (audioClip!=null)
						audioClip.close();
					if (audioStream!=null)
						audioStream.close();					
					chip.reset();

				} catch (IOException e1) {
					e1.printStackTrace();
				}		
				System.exit(0);
			}
		});
	}

	private void placeMe() {
    	ConfigAndPrefs cap = ConfigAndPrefs.getInstance();
    	Point p = cap.getWindowPos();
    	if (p.x==0 && p.y==0) {
    		Dimension size = getPreferredSize();
			GraphicsConfiguration gc = frame.getGraphicsConfiguration();
			Rectangle bounds = gc.getBounds();
			p = new Point((int) ((bounds.width / 2) - (size.getWidth() / 2)), 
					(int) ((bounds.height / 2) - (size.getHeight() / 2)));
    	}
    	setLocation(p.x, p.y);
    }
	public static void main(String[] args) {
		try {
			if (!Platforms.isJavaVersionOK()) {
				System.err.println("Cannot run on JRE version less than 1.8.");
			}
			if (args.length == 0) {
				// By default runs emulator
				String logfileName = "mochi8-log" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".log";
				System.setErr(new PrintStream(new File(logfileName)));
				start();
			} else {
				// If the first parameter is "asm" then start in (dis)assembler mode.
				if (args[0].equals("asm")) {
					AsmMain.run(Arrays.copyOfRange(args, 1, args.length));
				} else {
					System.err.println("Unrecognized sub-command, has to be 'asm' if any.");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}	
}
