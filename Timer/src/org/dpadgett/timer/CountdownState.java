/*  
 * Copyright 2012 Dan Padgett
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.dpadgett.timer;

import org.dpadgett.widget.CountdownTextView;

import android.content.SharedPreferences;

/**
 * Maintains the timing state, and keeps the textview state up-to-date.
 *
 * @author dpadgett
 */
public class CountdownState {
	public long endTime;
	private final CountdownTextView timerText;
	private boolean isRunning;
	
	public CountdownState(CountdownTextView timerText, SharedPreferences prefs) {
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
