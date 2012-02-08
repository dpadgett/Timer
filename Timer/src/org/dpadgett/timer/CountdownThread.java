package org.dpadgett.timer;

import java.util.concurrent.Semaphore;

import org.dpadgett.widget.CountdownTextView;

import android.os.Bundle;

public class CountdownThread {
	public long endTime;
	private final CountdownTextView timerText;
	private final Semaphore cancelSemaphore;
	private boolean isRunning;
	private Thread timerThread;
	
	public CountdownThread(CountdownTextView timerText, Bundle savedInstanceState) {
		this.timerText = timerText;
		this.cancelSemaphore = new Semaphore(0);
		isRunning = false;
		endTime = 0L;
		if (savedInstanceState != null) {
			restoreState(savedInstanceState);
		}
	}
	
	private void restoreState(Bundle savedInstanceState) {
		endTime = savedInstanceState.getLong("endTime", endTime);
		timerText.setEndingTime(endTime);
		isRunning = savedInstanceState.getBoolean("isRunning", isRunning)
				&& endTime > System.currentTimeMillis();
		if (isRunning) {
			//timerThread = new Thread(new TimingThread());
			//timerThread.start();
			timerText.forceUpdate(System.currentTimeMillis());
		}
	}
	
	public void stopTimer() {
		if (isRunning) {
			/*cancelSemaphore.release();
			try {
				timerThread.join();
			} catch (InterruptedException e) {
			}*/
			isRunning = false;
			timerThread = null;
		}
	}
	
	public void startTimer(long duration) {
		if (!isRunning) {
			endTime = System.currentTimeMillis() + duration;
			//timerThread = new Thread(new TimingThread());
			//timerThread.start();
			timerText.setEndingTime(endTime);
			isRunning = true;
		}
	}
	
	public boolean isRunning() {
		return isRunning;
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
					//for (OnFinishedListener l : onFinishedListeners) {
					//	l.onFinished();
					//}
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
    
    static String getTimerText(long elapsedTime) {
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
