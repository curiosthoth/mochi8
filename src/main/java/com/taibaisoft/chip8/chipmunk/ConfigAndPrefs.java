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

import java.awt.Color;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.util.prefs.Preferences;

public class ConfigAndPrefs {
	public static final String MP_LAST_PATH = "mp_last_path";
	public static final String MP_AUTO_RUN = "mp_auto_run";
	public static final String MP_PAUSE_ON_LOST_FOCUS = "mp_pause_on_lost_focus";
	public static final String MP_MUTE = "mp_mute";
	public static final String MP_SPEED = "mp_speed";
	public static final String MP_SPEED_SELECTION = "mp_speed_sel";
	
	public static final String MP_FORECOLOR = "mp_forecolor";
	public static final String MP_FORECOLOR_SELECTION = "mp_forecolor_sel";
	public static final String MP_BACKCOLOR = "mp_backcolor";
	public static final String MP_BACKCOLOR_SELECTION = "mp_backcolor_sel";
	public static final String MP_ASPECT_RATIO = "mp_aspect_ratio";
	public static final String MP_ASPECT_RATIO_SELECTION = "mp_aspect_ratio_sel";
	public static final String MP_WINDOW_SIZE = "mp_window_size_ratio";
	public static final String MP_WINDOW_SIZE_SELECTION = "mp_window_size_sel";
	public static final String MP_ALWAYS_ON_TOP = "mp_always_on_top";
	
	
	public static final String MP_DEFAULT_DEVELOPER_MODE = "mp_default_developer_mode";
	public static final String MP_RECENT_FILES = "mp_recent_files";
	
	public static final String MP_WINDOW_POS_X = "mp_wx";
	public static final String MP_WINDOW_POS_Y = "mp_wy";
	
	public static final String MP_KEY_MAPPINGS = "mp_keymappings";
	
	public static final String MP_FIRST_TIME = "mp_first_time";
	
	private static final byte[] DEFAULT_KEY_MAPPINGS = new byte[] {
		(byte)KeyEvent.VK_1, (byte)KeyEvent.VK_2, (byte)KeyEvent.VK_3, (byte)KeyEvent.VK_4,
		(byte)KeyEvent.VK_Q, (byte)KeyEvent.VK_W, (byte)KeyEvent.VK_E, (byte)KeyEvent.VK_R,
		(byte)KeyEvent.VK_A, (byte)KeyEvent.VK_S, (byte)KeyEvent.VK_D, (byte)KeyEvent.VK_F,	
		(byte)KeyEvent.VK_Z, (byte)KeyEvent.VK_X, (byte)KeyEvent.VK_C, (byte)KeyEvent.VK_V,	
};

	private Preferences prefs = null;
	
	private static ConfigAndPrefs me = null;
	
	public static ConfigAndPrefs getInstance() {
		if (me==null) {
			me = new ConfigAndPrefs();
		}
		return me;
	}
	private ConfigAndPrefs() {
		prefs = Preferences.userNodeForPackage(this.getClass());
	}
	
	public void setLastVisitedPath(String path) {
		prefs.put(MP_LAST_PATH, path);
	}
	public String getLastVisitedPath() {
		return prefs.get(MP_LAST_PATH, "");
	}
	public void setAutoRunOnLoad(boolean a) {
		prefs.putBoolean(MP_AUTO_RUN, a);
	}
	public boolean getAutoRunOnLoad() {
		return prefs.getBoolean(MP_AUTO_RUN, true);
	}
	public void setDefaultDeveloperMode(boolean dev) {
		prefs.putBoolean(MP_DEFAULT_DEVELOPER_MODE, dev);
	}
	public boolean getDeveloperMode() {
		return prefs.getBoolean(MP_DEFAULT_DEVELOPER_MODE, false);
	}
	public void setDefaultPauseOnLostFocus(boolean f) {
		prefs.putBoolean(MP_PAUSE_ON_LOST_FOCUS, f);
	}
	public boolean getPauseOnLostFocus() {
		return prefs.getBoolean(MP_PAUSE_ON_LOST_FOCUS, false);
	}
	public void setDefaultMute(boolean f) {
		prefs.putBoolean(MP_MUTE, f);
	}
	public boolean getMute() {
		return prefs.getBoolean(MP_MUTE, false);
	}
	public void setDefaultSpeed(int speed) {
		prefs.putInt(MP_SPEED, speed);
	}
	public int getSpeed() {
		return prefs.getInt(MP_SPEED, 600);
	}
	public void setDefaultSpeedSelection(int speedSel) {
		prefs.putInt(MP_SPEED_SELECTION, speedSel);
	}
	public int getSpeedSelection() {
		return prefs.getInt(MP_SPEED_SELECTION, 5);
	}
	public void setDefaultForeColor(Color color) {
		prefs.putInt(MP_FORECOLOR, color.getRGB());
	}
	public Color getForeColor() {
		return new Color(prefs.getInt(MP_FORECOLOR, Color.LIGHT_GRAY.getRGB()));
	}
	public void setDefaultForeColorSelection(int index) {
		prefs.putInt(MP_FORECOLOR_SELECTION, index);
	}
	public int getForeColorSelection() {
		return prefs.getInt(MP_FORECOLOR_SELECTION, 0);
	}
	public void setDefaultBackColor(Color color) {
		prefs.putInt(MP_BACKCOLOR, color.getRGB());
	}
	public Color getBackColor() {
		return new Color(prefs.getInt(MP_BACKCOLOR, Color.BLACK.getRGB()));
	}
	public void setDefaultBackColorSelection(int index) {
		prefs.putInt(MP_BACKCOLOR_SELECTION, index);
	}
	public int getBackColorSelection() {
		return prefs.getInt(MP_BACKCOLOR_SELECTION, 0);
	}	
	
	public void setDefaultAspectRatio(float r) {
		prefs.putFloat(MP_ASPECT_RATIO, r);
	}
	public float getAspectRatio() {
		return prefs.getFloat(MP_ASPECT_RATIO, 1.0f);
	}
	
	public void setDefaultAspectRatioSelection(int index) {
		prefs.putInt(MP_ASPECT_RATIO_SELECTION, index);
	}
	public int getAspectRatioSelection() {
		return prefs.getInt(MP_ASPECT_RATIO_SELECTION, 0);
	}
	public void setDefaultWindowSize(int size) {
		// Height!
		prefs.putInt(MP_WINDOW_SIZE, size);
	}
	public int getWindowSize() {
		// Height!
		return prefs.getInt(MP_WINDOW_SIZE, 360);
	}
	public void setDefaultWindowSizeSelection(int index) {
		prefs.putInt(MP_WINDOW_SIZE_SELECTION, index);
	}
	public int getWindowSizeSelection() {
		return prefs.getInt(MP_WINDOW_SIZE_SELECTION, 0);
	}	
	public void setDefaultAlwaysOnTop(boolean f) {
		prefs.putBoolean(MP_ALWAYS_ON_TOP, f);
	}
	public boolean getAlwaysOnTop() {
		return prefs.getBoolean(MP_ALWAYS_ON_TOP, false);
	}
	public void setDefaultWindowPos(Point p) {
		prefs.putInt(MP_WINDOW_POS_X, p.x);
		prefs.putInt(MP_WINDOW_POS_Y, p.y);
	}
	public Point getWindowPos() {
		int x = prefs.getInt(MP_WINDOW_POS_X, 0);
		int y = prefs.getInt(MP_WINDOW_POS_Y, 0);
		return new Point(x,y);
	}
	
	public void setKeyMappings(int[] keyMappings) {
		// To byte array
		int len = keyMappings.length;
		byte[] a = new byte[len];
		int i = 0;
		for (;i<len; ++i) {
			a[i] = (byte)keyMappings[i];
		}
		prefs.putByteArray(MP_KEY_MAPPINGS, a);
	}
	public byte[] getDefaultKeyMappings() {
		return DEFAULT_KEY_MAPPINGS;
	}
	public int[] getKeyMappings() {
		byte[] a =  prefs.getByteArray(MP_KEY_MAPPINGS, DEFAULT_KEY_MAPPINGS);
		int len = a.length;
		int[] b = new int[len];
		int i = 0;
		for (;i<len; ++i) {
			b[i] = (int)a[i];
		}	
		return b;
	}
	
	/**
	 * Detects if first time running mochi. Anyway set first time flag to false.
	 * @return
	 */
	public boolean isFirstTime() {
		boolean ret = prefs.getBoolean(MP_FIRST_TIME, true);
		prefs.putBoolean(MP_FIRST_TIME, false);
		return ret;
	}
	/*
	public void pushRecentFile(String fileName) {
		recentFiles_.add(fileName);
	}
	*/
}
