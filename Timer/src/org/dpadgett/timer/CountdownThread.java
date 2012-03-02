package org.dpadgett.timer;

import org.dpadgett.widget.CountdownTextView;

import android.content.SharedPreferences;

public class CountdownThread {
	public long endTime;
	private final CountdownTextView timerText;
	private boolean isRunning;
	
	public CountdownThread(CountdownTextView timerText, SharedPreferences prefs) {
		this.timerText = timerText;
		isRunning = false;
		endTime = 0L;
		if (prefs.contains("endTime")) {
			restoreState(prefs);
		}
	}
	
	private void restoreState(SharedPreferences prefs) {
		endTime = prefs.getLong("endTime", endTime);
		timerText.setEndingTime(endTime);
		isRunning = prefs.getBoolean("isRunning", isRunning)
				&& endTime > System.currentTimeMillis();
		if (isRunning) {
			timerText.forceUpdate(System.currentTimeMillis());
		}
	}
	
	public void stopTimer() {
		if (isRunning) {
			isRunning = false;
		}
	}
	
	public void startTimer(long duration) {
		if (!isRunning) {
			long currentTime = System.currentTimeMillis();
			endTime = currentTime + duration;
			timerText.setEndingTime(endTime);
			timerText.forceUpdate(currentTime);
			isRunning = true;
		}
	}
	
	public boolean isRunning() {
		return isRunning;
	}
	
	public void onSaveState(SharedPreferences.Editor prefs) {
		prefs.putBoolean("isRunning", isRunning);
		prefs.putLong("endTime", endTime);
	}
}
