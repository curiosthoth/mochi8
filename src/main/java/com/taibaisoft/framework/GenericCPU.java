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


/**
 * A generic abstract CPU support up to 32-bit addressing.
 * @author jeffreybian
 *
 */
public abstract class GenericCPU {
	
	final static int MAX_CALL_STACK_DEPTH = 32;
	final static int MAX_TIMERS = 8;
	
	/**
	 * Every CPU has at least a Program Counter
	 */
	protected int pc = 0x0;		
	
	/**
	 * Every computer should have a stack for pushing on/popping contexts
	 */
	protected int[] callStack = new int[MAX_CALL_STACK_DEPTH];
	
	/**
	 * The Stack Pointer.
	 */
	protected int sp = 0x0;
	
	/**
	 * For state control. Running is either true or false.
	 */
	protected volatile boolean running = false;
	
	/**
	 * Paused setting to true will cause the CPU to enter empty loops. 
	 */
	protected volatile boolean paused = false;
	
	/**
	 * For handling the exceptions thrown at oneCycle() method.
	 */
	protected IExceptionHandler exceptHandler = null;
	
	/**
	 * For timers
	 */
	protected double[] timerIntervals = new double[MAX_TIMERS];
	protected double[] prevTimers = new double[MAX_TIMERS];
	
	protected boolean dbgStepMode = false;
	protected boolean dbgPaused = false;
	protected boolean debug = false;
	
	public abstract void oneCycle() throws Exception;
	public abstract void timerCallback(int timerId);
	public abstract void blitGraphics(double delta);
	
	/**
	 * Auxilary class for generating clock.
	 */
	protected InternalClockSource clockSource = new InternalClockSource();
	
	protected Thread loopThread = null;
	
	public GenericCPU() {
		for (int i = 0;i<MAX_TIMERS; ++i) {
			timerIntervals[i] = -1;
		}
	}
	
	/**
	 * Not accurate.
	 * @param timerId
	 * @param interval double In Nano (10^-12) seconds. 
	 */
	public void registerTimer(int timerId, double interval) {
		if (timerId>=0 && timerId<MAX_TIMERS) {
			timerIntervals[timerId] = interval;
		}
	}
	public void deregisterTimer(int timerId) {
		if (timerId>0 && timerId<MAX_TIMERS) {
			timerIntervals[timerId] = -1;
		}
	}
	public void registerExceptionHandler(IExceptionHandler handler) {
		exceptHandler = handler;
	}

	/**
	 * This method is idempotent.
	 */
	public void run() {
		if (!running) {
			running = true;
			loopThread = new Thread(clockSource);
			loopThread.start();
		}
	}
	/**
	 * This method is idempotent.
	 * PC, STACK are not resetted after this call.
	 */
	public void stop() {
		running = false;
		paused = false;
		// Wait for termination
		if ( loopThread!=null ) {
			while (loopThread.isAlive()) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}	
	}
	public boolean hasStartedRunning() {
		return running;
	} 
	public void pause() {
		paused = true;
	}
	public void resume() {
		paused = false;
	}
	/**
	 * Note this method test if the CPU is actually running, while hasStartedRunning() only
	 * tests if running flag is on.
	 * @return
	 */
	public boolean isRunning() {
		return running && (loopThread!=null && loopThread.isAlive());
	}
	
	/**
	 * Note only returns true when running and is paused.
	 * @return
	 */
	public boolean isPaused() {
		return paused && running;
	}
	public void setCpuFrequency(int freq) {
		try {
			clockSource.setCPUFrequency(freq);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void setRenderFrequency(int freq) {
		try {
			clockSource.setRenderFrequency(freq);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	class InternalClockSource implements Runnable {
		final int NS_PER_S = 1000000000;
		final int MAX_UPDATES_BEFORE_RENDER = 1;
		private double renderInterval = NS_PER_S / (double)NS_PER_S;   	
		private double updateInterval = NS_PER_S / (double)NS_PER_S;
		
		public void setCPUFrequency(int freq) {
			updateInterval = NS_PER_S / (double)freq;
		}
		public void setRenderFrequency(int freq) {
			renderInterval = NS_PER_S / (double)freq;
		}
		
		@Override
		public void run() {			
			double lastUpdateTime = System.nanoTime();
			double lastRenderTime = System.nanoTime();

			while (running) {
				double now = System.nanoTime();
				int updateCount = 0;

				if (!paused && !dbgPaused) {
					// Do as many game updates as we need to, potentially playing
					// catchup.
					try {
						if (!debug) {
							while ( now - lastUpdateTime > updateInterval && 
								   updateCount < MAX_UPDATES_BEFORE_RENDER) {
								oneCycle();
								lastUpdateTime += updateInterval;
								updateCount++;
							}
						} else {
							oneCycle();
						}
					} catch (Exception e) {
						if (exceptHandler!=null) {
							running = exceptHandler.onException(e);
						}
					}
					// Update timers
					for (int i = 0; i<MAX_TIMERS; ++i) {
						if (timerIntervals[i]>0 && now - prevTimers[i] >= timerIntervals[i]) {
							timerCallback(i);
							prevTimers[i]= now;
							// Warn timing lagging due to debugging.
						}
					}
					// If for some reason an update takes forever, we don't want to
					// do an insane number of catchups.
					// If you were doing some sort of game that needed to keep EXACT
					// time, you would get rid of this.
					if (now - lastUpdateTime > updateInterval) {
						lastUpdateTime = now - updateInterval;
					}				
					// The delta parameter is for Lerp though
					// currently we don't use it.
					blitGraphics((now - lastRenderTime)/NS_PER_S);

					lastRenderTime = now;
					// -----------------------------------------------
					while (now - lastRenderTime < renderInterval
							&& now - lastUpdateTime < updateInterval) {
						Thread.yield();
						try {
							Thread.sleep(1);
						} catch (Exception e) {
						}
						now = System.nanoTime();
					}
				} else {
					try {
						Thread.sleep(10);
					} catch (Exception e) {
					}
				}
			}			
		}
	
	}
}
