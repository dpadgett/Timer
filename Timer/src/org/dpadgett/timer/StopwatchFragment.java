package org.dpadgett.timer;

import java.util.concurrent.Semaphore;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

public class StopwatchFragment extends Fragment {

	private long additionalElapsed = 0L;
	private long additionalLapTimeElapsed = 0L;
	private long timeStarted = 0L;
	private Thread updateTimerThread = null;
	private DanWidgets danWidgets;
	private LapTimes lapTimes;
	private View rootView;
	
	private Semaphore s;

	private boolean isTimerRunning;
	private Bundle initialSavedState;

	public StopwatchFragment() {
		isTimerRunning = false;
		s = new Semaphore(0);
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
        danWidgets = DanWidgets.create(rootView);
        
        ((LinearLayout) rootView).setDividerDrawable(
        		new ListView(rootView.getContext()).getDivider());
        
        Button startButton = (Button) rootView.findViewById(R.id.button1);
        Button resetButton = (Button) rootView.findViewById(R.id.button2);
        
        lapTimes = new LapTimes((ScrollView) rootView.findViewById(R.id.scrollView1));
        
        startButton.setOnClickListener(new OnClickListener() {
			DanButton startButton = danWidgets.getButton(R.id.button1);
			DanButton resetButton = danWidgets.getButton(R.id.button2);
			DanTextView timerText = danWidgets.getTextView(R.id.textView1);
			DanTextView lapTimerText = danWidgets.getTextView(R.id.liveLapTime);

			@Override
			public void onClick(View arg0) {
				isTimerRunning = !isTimerRunning;
				if (isTimerRunning) { // start
					timeStarted = System.currentTimeMillis();
					updateTimerThread = new Thread(new UpdateTimerThread());
					updateTimerThread.start();
					startButton.setText("Stop");
					resetButton.setText("Lap");
				} else { // stop
					s.release();
					try {
						updateTimerThread.join();
					} catch (InterruptedException e) {
					}
					updateTimerThread = null;
					startButton.setText("Start");
					resetButton.setText("Reset");
					additionalElapsed += System.currentTimeMillis() - timeStarted;
					String updateText = getTimerText(additionalElapsed + additionalLapTimeElapsed);
					timerText.setText(updateText);
					updateText = getTimerText(additionalElapsed);
					lapTimerText.setText("lap: " + updateText);
				}
			}
        });
        resetButton.setOnClickListener(new OnClickListener() {
        	
        	DanTextView timerText = danWidgets.getTextView(R.id.textView1);
        	DanTextView lapTimerText = danWidgets.getTextView(R.id.liveLapTime);
        	
			@Override
			public void onClick(View arg0) {
				if (isTimerRunning) { // lap
					long origTimeStarted = timeStarted;
					timeStarted = System.currentTimeMillis();
					long lapTime = timeStarted - origTimeStarted + additionalElapsed; // this is the lap time
					additionalLapTimeElapsed += lapTime;
					additionalElapsed = 0L;

					// add it to the list of lap times
					lapTimes.add(lapTime);
				} else { // reset
					timeStarted = 0L;
					additionalElapsed = 0L;
					additionalLapTimeElapsed = 0L;
					timerText.setText("00:00:00.000");
					lapTimerText.setText("lap: 00:00:00.000");
					lapTimes.clear();
				}
			}
        });

        if (savedInstanceState != null) {
        	restoreState(savedInstanceState);
        }

        return rootView;
    }
    
    private void restoreState(Bundle savedInstanceState) {
    	isTimerRunning = savedInstanceState.getBoolean("isTimerRunning", false);
    	timeStarted = savedInstanceState.getLong("timeStarted", 0L);
    	additionalElapsed = savedInstanceState.getLong("additionalElapsed", 0L);
    	additionalLapTimeElapsed = savedInstanceState.getLong("additionalLapTimeElapsed", 0L);
    	lapTimes.restoreState(savedInstanceState);
    	
    	Button startButton = (Button) rootView.findViewById(R.id.button1);
        Button resetButton = (Button) rootView.findViewById(R.id.button2);
    	TextView timerText = (TextView) rootView.findViewById(R.id.textView1);
    	TextView lapTimerText = (TextView) rootView.findViewById(R.id.liveLapTime);
    	long elapsedTime = additionalElapsed + additionalLapTimeElapsed;

    	if (isTimerRunning) {
    		updateTimerThread = new Thread(new UpdateTimerThread());
    		updateTimerThread.start();
			startButton.setText("Stop");
			resetButton.setText("Lap");
			elapsedTime = System.currentTimeMillis() - timeStarted + additionalElapsed + additionalLapTimeElapsed;
    	}

    	String updateText = getTimerText(elapsedTime);
		timerText.setText(updateText);
		updateText = getTimerText(elapsedTime - additionalLapTimeElapsed);
		lapTimerText.setText("lap: " + updateText);
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
        } else {
        	saveState.putAll(initialSavedState);
        }
    }

	private Bundle savedState = null;
    @Override
    public void onPause() {
    	super.onPause();
    	if (isTimerRunning) {
    		s.release();
			try {
				updateTimerThread.join();
			} catch (InterruptedException e) {
			}
			updateTimerThread = null;
    	}
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

    private class UpdateTimerThread implements Runnable {
		@Override
		public void run() {
			DanTextView timerText = danWidgets.getTextView(R.id.textView1);
			DanTextView lapTimerText = danWidgets.getTextView(R.id.liveLapTime);
			while (!s.tryAcquire()) {
				long elapsedTime = System.currentTimeMillis() - timeStarted + additionalElapsed + additionalLapTimeElapsed;
				String updateText = getTimerText(elapsedTime);
				timerText.setText(updateText);
				
				long elapsedLapTime = System.currentTimeMillis() - timeStarted + additionalElapsed;
				updateText = getTimerText(elapsedLapTime);
				lapTimerText.setText("lap: " + updateText);
				//Thread.yield();
				try {
					Thread.sleep(20);
				} catch (InterruptedException e) {
				}
			}
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
