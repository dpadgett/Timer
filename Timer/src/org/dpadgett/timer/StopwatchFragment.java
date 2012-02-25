package org.dpadgett.timer;

import org.dpadgett.widget.TimerTextView;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;

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
	private Bundle initialSavedState;

	public StopwatchFragment() {
		isTimerRunning = false;
	}

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialSavedState = savedInstanceState;
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
			Button startButton = (Button) rootView.findViewById(R.id.startButton);
			Button resetButton = (Button) rootView.findViewById(R.id.stopButton);

			@Override
			public void onClick(View arg0) {
				isTimerRunning = !isTimerRunning;
				if (isTimerRunning) { // start
					timeStarted = System.currentTimeMillis();
					timerText.setStartingTime(timeStarted - additionalElapsed - additionalLapTimeElapsed);
					timerText.resume();
					lapTimeText.setStartingTime(timeStarted - additionalElapsed);
					lapTimeText.resume();
					timerText.forceUpdate(timeStarted);
					lapTimeText.forceUpdate(timeStarted);
					startButton.setText("Stop");
					resetButton.setText("Lap");
				} else { // stop
					long timeStopped = System.currentTimeMillis();
					startButton.setText("Start");
					resetButton.setText("Reset");
					additionalElapsed += timeStopped - timeStarted;
					timerText.pause(timeStopped);
					lapTimeText.pause(timeStopped);
				}
			}
        });
        resetButton.setOnClickListener(new OnClickListener() {
        	
			@Override
			public void onClick(View arg0) {
				if (isTimerRunning) { // lap
					long origTimeStarted = timeStarted;
					timeStarted = System.currentTimeMillis();
					long lapTime = timeStarted - origTimeStarted + additionalElapsed; // this is the lap time
					additionalLapTimeElapsed += lapTime;
					additionalElapsed = 0L;
					lapTimeText.setStartingTime(timeStarted - additionalElapsed);

					// add it to the list of lap times
					lapTimes.add(lapTime);
				} else { // reset
					timeStarted = 0L;
					additionalElapsed = 0L;
					additionalLapTimeElapsed = 0L;
					timerText.reset();
					lapTimeText.reset();
					lapTimes.clear();
				}
			}
        });

        //if (savedInstanceState != null) {
        	restoreState(savedInstanceState);
        //}

        return rootView;
    }
    
    private void restoreState(Bundle savedInstanceState) {
        SharedPreferences prefs =
				context.getSharedPreferences("Stopwatch", Context.MODE_PRIVATE);

        if (prefs.contains("isTimerRunning")) {
        	isTimerRunning = prefs.getBoolean("isTimerRunning", false);
        	timeStarted = prefs.getLong("timeStarted", 0L);
        	additionalElapsed = prefs.getLong("additionalElapsed", 0L);
        	additionalLapTimeElapsed = prefs.getLong("additionalLapTimeElapsed", 0L);
        	lapTimes.restoreState(prefs);
        } else {
        	if (savedInstanceState == null) {
        		return;
        	}
	    	isTimerRunning = savedInstanceState.getBoolean("isTimerRunning", false);
	    	timeStarted = savedInstanceState.getLong("timeStarted", 0L);
	    	additionalElapsed = savedInstanceState.getLong("additionalElapsed", 0L);
	    	additionalLapTimeElapsed = savedInstanceState.getLong("additionalLapTimeElapsed", 0L);
	    	lapTimes.restoreState(savedInstanceState);
        }	    	
        
    	Button startButton = (Button) rootView.findViewById(R.id.startButton);
        Button resetButton = (Button) rootView.findViewById(R.id.stopButton);

		timerText.setStartingTime(timeStarted - additionalElapsed - additionalLapTimeElapsed);
		lapTimeText.setStartingTime(timeStarted - additionalElapsed);

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
        if (rootView != null) {
	        saveState.putBoolean("isTimerRunning", isTimerRunning);
	        saveState.putLong("timeStarted", timeStarted);
	        saveState.putLong("additionalElapsed", additionalElapsed);
	        saveState.putLong("additionalLapTimeElapsed", additionalLapTimeElapsed);
	        lapTimes.onSaveInstanceState(saveState);

	        SharedPreferences.Editor prefs =
					context.getSharedPreferences("Stopwatch", Context.MODE_PRIVATE).edit();
	        prefs.putBoolean("isTimerRunning", isTimerRunning);
	        prefs.putLong("timeStarted", timeStarted);
	        prefs.putLong("additionalElapsed", additionalElapsed);
	        prefs.putLong("additionalLapTimeElapsed", additionalLapTimeElapsed);
	        prefs.apply();
        } else {
        	saveState.putAll(initialSavedState);
        }
    }

	private Bundle savedState = null;
    @Override
    public void onPause() {
    	super.onPause();
    	savedState = new Bundle();
    	onSaveInstanceState(savedState);
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	if (savedState != null) {
    		restoreState(savedState);
    		System.out.println("resumed");
    	}
    }

	static String getTimerText(long elapsedTime) {
		long millis = elapsedTime % 1000;
		elapsedTime /= 1000;
		long secs = elapsedTime % 60;
		elapsedTime /= 60;
		long mins = elapsedTime % 60;
		elapsedTime /= 60;
		long hours = elapsedTime % 60;
		return String.format("%02d:%02d:%02d.%03d", hours, mins, secs, millis);
	}
}
