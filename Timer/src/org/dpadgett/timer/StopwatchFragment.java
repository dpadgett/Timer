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

import org.dpadgett.compat.LinearLayout;
import org.dpadgett.widget.TimerTextView;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ScrollView;

/**
 * Fragment which handles the UI and logic for running a stopwatch.
 *
 * @author dpadgett
 */
public class StopwatchFragment extends Fragment {

	private long additionalElapsed = 0L;
	private long additionalLapTimeElapsed = 0L;
	private long timeStarted = 0L;
	private LapTimes lapTimes;
	private View rootView;
	private Context context;

	private TimerTextView timerText;
	private TimerTextView lapTimeText;

	private boolean isTimerRunning;
	
	private boolean autoStartStopwatch = false;

	public void start() {
		// We can start the timer via something other than the button
		// If that is the case, then make sure we are tracking the timer as started.
		if (isTimerRunning == false) {
			isTimerRunning = true;
		}
		Button startButton = (Button) rootView.findViewById(R.id.startButton);
		Button resetButton = (Button) rootView.findViewById(R.id.stopButton);
		timeStarted = System.currentTimeMillis();
		timerText.setStartingTime(timeStarted - additionalElapsed - additionalLapTimeElapsed);
		timerText.resume();
		lapTimeText.setStartingTime(timeStarted - additionalElapsed);
		lapTimeText.resume();
		timerText.forceUpdate(timeStarted);
		lapTimeText.forceUpdate(timeStarted);
		startButton.setText("Stop");
		resetButton.setText("Lap");
		saveState();
	}
	public void stop() {
		Button startButton = (Button) rootView.findViewById(R.id.startButton);
		Button resetButton = (Button) rootView.findViewById(R.id.stopButton);
		long timeStopped = System.currentTimeMillis();
		startButton.setText("Start");
		resetButton.setText("Reset");
		additionalElapsed += timeStopped - timeStarted;
		timerText.pause(timeStopped);
		lapTimeText.pause(timeStopped);
		saveState();
	}
	public void lap() {
		long origTimeStarted = timeStarted;
		timeStarted = System.currentTimeMillis();
		long lapTime = timeStarted - origTimeStarted + additionalElapsed; // this is the lap time
		additionalLapTimeElapsed += lapTime;
		additionalElapsed = 0L;
		lapTimeText.setStartingTime(timeStarted - additionalElapsed);

		// add it to the list of lap times
		lapTimes.add(lapTime);
		saveState();
	}
	public void reset() {
		timeStarted = 0L;
		additionalElapsed = 0L;
		additionalLapTimeElapsed = 0L;
		timerText.reset();
		lapTimeText.reset();
		lapTimes.clear();
		saveState();
	}
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        String startReason = TimerActivity.START_REASON_NONE;
        Bundle args = getArguments();
        if (args != null) {
               startReason = args.getString(TimerActivity.START_REASON);
        }
        if (startReason.equals(TimerActivity.START_REASON_AUTOSTART_STOPWATCH)) {
        	autoStartStopwatch = true;
        }
	}
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.stopwatch, container, false);
        context = rootView.getContext();
        
        ((LinearLayout) rootView).setDividerDrawable(
        		new ListView(rootView.getContext()).getDivider());
        
        Button startButton = (Button) rootView.findViewById(R.id.startButton);
        Button resetButton = (Button) rootView.findViewById(R.id.stopButton);
        
        timerText = (TimerTextView) rootView.findViewById(R.id.timerText);
        lapTimeText = (TimerTextView) rootView.findViewById(R.id.liveLapTime);
        lapTimeText.setTextPrefix("lap: ");
        
        lapTimes = new LapTimes((ScrollView) rootView.findViewById(R.id.scrollView1));
 
        startButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				isTimerRunning = !isTimerRunning;
				if (isTimerRunning) { // start
					start();
				} else { // stop
					stop();
				}

			}
        });
        resetButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				if (isTimerRunning) { // lap
					lap();
				} else { // reset
					reset();
				}

			}
        });

    	restoreState();


		// forcefully pre-render content so it is cached
		rootView.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
    	new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				rootView.layout(0, 0, rootView.getMeasuredWidth(), rootView.getMeasuredHeight());
				rootView.draw(new Canvas(Bitmap.createBitmap(rootView.getMeasuredWidth(), rootView.getMeasuredHeight(), Bitmap.Config.ARGB_8888)));
			}
		}, 1000);

        return rootView;
    }
    
    private void restoreState() {
        SharedPreferences prefs =
				context.getSharedPreferences("Stopwatch", Context.MODE_PRIVATE);

        if (prefs.contains("isTimerRunning")) {
        	isTimerRunning = prefs.getBoolean("isTimerRunning", false);
        	timeStarted = prefs.getLong("timeStarted", 0L);
        	additionalElapsed = prefs.getLong("additionalElapsed", 0L);
        	additionalLapTimeElapsed = prefs.getLong("additionalLapTimeElapsed", 0L);
        	lapTimes.restoreState(prefs);
        }
        
    	Button startButton = (Button) rootView.findViewById(R.id.startButton);
        Button resetButton = (Button) rootView.findViewById(R.id.stopButton);

		timerText.setStartingTime(timeStarted - additionalElapsed - additionalLapTimeElapsed);
		lapTimeText.setStartingTime(timeStarted - additionalElapsed);

    	if (autoStartStopwatch == true) {
    		if (isTimerRunning) {
    			lap();
    		} else {
    			// TODO: if current stopped timer == 0, then start
    			// if !=0, then lap and start, that way the time of a stopped timer is not lost.
    			reset();
    			start();
    		}
    		autoStartStopwatch = false;
    	}
		
		if (isTimerRunning) {
    		timerText.resume();
    		lapTimeText.resume();
			startButton.setText("Stop");
			resetButton.setText("Lap");
    	}
		timerText.forceUpdate(timeStarted);
		lapTimeText.forceUpdate(timeStarted);
    }

    @Override
	public void onSaveInstanceState(Bundle saveState) {
        super.onSaveInstanceState(saveState);
        if (lapTimes != null) {
	        lapTimes.saveState();
        }
    }

    /** Called whenever internal persisted state is changed */
    private void saveState() {
    	SharedPreferences.Editor prefs =
				context.getSharedPreferences("Stopwatch", Context.MODE_PRIVATE).edit();
        prefs.putBoolean("isTimerRunning", isTimerRunning);
        prefs.putLong("timeStarted", timeStarted);
        prefs.putLong("additionalElapsed", additionalElapsed);
        prefs.putLong("additionalLapTimeElapsed", additionalLapTimeElapsed);
        prefs.commit();
    }

    @Override
	public void onPause() {
		super.onPause();
        if (lapTimes != null) {
	        lapTimes.saveState();
        }
	}

	@Override
    public void onResume() {
    	super.onResume();
    	if (rootView != null) {
    		restoreState();
    	}
    }
}
