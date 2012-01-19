package org.dpadgett.timer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import android.os.Bundle;

public class CountdownThread {
	private final List<OnFinishedListener> onFinishedListeners;
	public long endTime;
	private final DanTextView timerText;
	private final Semaphore cancelSemaphore;
	private boolean isRunning;
	private Thread timerThread;
	
	public CountdownThread(DanTextView timerText, Bundle savedInstanceState) {
		onFinishedListeners = new ArrayList<OnFinishedListener>();
		this.timerText = timerText;
		this.cancelSemaphore = new Semaphore(0);
		isRunning = false;
		endTime = 0L;
		if (savedInstanceState != null) {
			restoreState(savedInstanceState);
		}
	}
	
	private void restoreState(Bundle savedInstanceState) {
		isRunning = savedInstanceState.getBoolean("isRunning", isRunning);
		endTime = savedInstanceState.getLong("endTime", endTime);
		if (isRunning) {
			timerThread = new Thread(new TimingThread());
			timerThread.start();
		}
	}
	
	public void addOnFinishedListener(OnFinishedListener l) {
		onFinishedListeners.add(l);
	}
	
	public boolean removeOnFinishedListener(OnFinishedListener l) {
		return onFinishedListeners.remove(l);
	}
	
	public interface OnFinishedListener {
		void onFinished();
	}
	
	public void stopTimer() {
		if (isRunning) {
			cancelSemaphore.release();
			try {
				timerThread.join();
			} catch (InterruptedException e) {
			}
			isRunning = false;
			timerThread = null;
		}
	}
	
	public void startTimer(long duration) {
		if (!isRunning) {
			endTime = System.currentTimeMillis() + duration;
			timerThread = new Thread(new TimingThread());
			timerThread.start();
			isRunning = true;
		}
	}
	
	public void onSaveState(Bundle saveState) {
		saveState.putBoolean("isRunning", isRunning);
		saveState.putLong("endTime", endTime);
	}
	
	private class TimingThread implements Runnable {
		@Override
		public void run() {
			while (!cancelSemaphore.tryAcquire()) {
				long timeUntilEnd = endTime - System.currentTimeMillis();
				if (timeUntilEnd <= 0) {
					timeUntilEnd = 0; // so we never go negative
					timerText.setText(getTimerText(0));
					// we hit the end of the timer, so run the callbacks
					for (OnFinishedListener l : onFinishedListeners) {
						l.onFinished();
					}
					cancelSemaphore.acquireUninterruptibly();
					break;
				}
				String timeText = getTimerText(timeUntilEnd);
				timerText.setText(timeText);
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
				}
			}
		}
    }
    
    private static String getTimerText(long elapsedTime) {
		if (elapsedTime % 1000 > 0) {
			elapsedTime += 1000;
		}
    	elapsedTime /= 1000;
		long secs = elapsedTime % 60;
		elapsedTime /= 60;
		long mins = elapsedTime % 60;
		elapsedTime /= 60;
		long hours = elapsedTime % 100;
		return String.format("%02d:%02d:%02d", hours, mins, secs);
	}
}
