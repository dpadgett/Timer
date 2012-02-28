package org.dpadgett.timer;

import org.dpadgett.widget.CountdownTextView;

import android.app.AlarmManager;
import android.app.Fragment;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.NumberPicker.Formatter;

public class CountdownFragment extends Fragment {

	private boolean inputMode;
	private LinearLayout inputLayout;
	private LinearLayout timerLayout;
	private View rootView;
	private final Handler handler;
	private NumberPicker countdownHours;
	private NumberPicker countdownMinutes;
	private NumberPicker countdownSeconds;
	private CountdownThread timingThread;

	public PendingIntent alarmPendingIntent;
	
	public CountdownFragment() {
		this.inputMode = true;
		this.handler = new Handler();
	}

	public Context getContext() {
		return rootView.getContext();
	}
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.countdown_simplified, container, false);
        this.inputLayout = (LinearLayout) rootView.findViewById(R.id.inputsInnerLayout);
        Button startButton = (Button) rootView.findViewById(R.id.startButton);

        Formatter twoDigitFormatter = new NumberPicker.Formatter() {
			@Override
			public String format(int value) {
				return String.format("%02d", value);
			}
        };

        countdownHours = (NumberPicker) rootView.findViewById(R.id.countdownHours);
        countdownHours.setMinValue(0);
        countdownHours.setMaxValue(99);
		countdownHours.setFormatter(twoDigitFormatter);
		// I will burn in hell for this
		View view = countdownHours.findViewById(Resources.getSystem().getIdentifier("numberpicker_input", "id", "android"));
		if (view != null) {
			EditText inputText = (EditText) view;
			inputText.setFocusable(false);
		}
		countdownMinutes = (NumberPicker) rootView.findViewById(R.id.countdownMinutes);
        countdownMinutes.setMinValue(0);
        countdownMinutes.setMaxValue(59);
		countdownMinutes.setFormatter(twoDigitFormatter);
		view = countdownMinutes.findViewById(Resources.getSystem().getIdentifier("numberpicker_input", "id", "android"));
		if (view != null) {
			EditText inputText = (EditText) view;
			inputText.setFocusable(false);
		}
		countdownSeconds = (NumberPicker) rootView.findViewById(R.id.countdownSeconds);
        countdownSeconds.setMinValue(0);
        countdownSeconds.setMaxValue(59);
		countdownSeconds.setFormatter(twoDigitFormatter);
		view = countdownSeconds.findViewById(Resources.getSystem().getIdentifier("numberpicker_input", "id", "android"));
		if (view != null) {
			EditText inputText = (EditText) view;
			inputText.setFocusable(false);
		}
        this.timerLayout =
        		(LinearLayout) inflater.inflate(R.layout.countdown_timer, container, false);
		startButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				handler.post(toggleInputMode);
			}
        });
		restoreState(savedInstanceState);
        return rootView;
    }
    
    private void restoreState(Bundle savedInstanceState) {
        SharedPreferences prefs =
				getContext().getSharedPreferences("Countdown", Context.MODE_PRIVATE);

    	timingThread = new CountdownThread(
				(CountdownTextView) timerLayout.findViewById(R.id.countdownTimer),
				savedInstanceState, prefs);
		
    	Log.i(getClass().getName(), "Prefs: " + prefs.getAll());
        if (prefs.contains("countdownInputs")) {
	    	long countdownInputs = prefs.getLong("countdownInputs", 0L);
	    	countdownInputs /= 1000;
	    	countdownSeconds.setValue((int) (countdownInputs % 60));
	    	countdownInputs /= 60;
	    	countdownMinutes.setValue((int) (countdownInputs % 60));
	    	countdownInputs /= 60;
	    	countdownHours.setValue((int) (countdownInputs % 100));
	    	inputMode = prefs.getBoolean("inputMode", inputMode);
	    	if (!inputMode && timingThread.isRunning()) {
	    		// countdown view
	    		inputMode = false;
				LinearLayout inputs = (LinearLayout) rootView.findViewById(R.id.inputsLayout);
				Button startButton = (Button) rootView.findViewById(R.id.startButton);
				inputs.removeAllViews();
				inputs.addView(timerLayout);
				startButton.setText("Cancel");
				// timing thread will auto start itself
	    	}
        } else if (savedInstanceState != null) {
	    	long countdownInputs = savedInstanceState.getLong("countdownInputs", 0L);
	    	countdownInputs /= 1000;
	    	countdownSeconds.setValue((int) (countdownInputs % 60));
	    	countdownInputs /= 60;
	    	countdownMinutes.setValue((int) (countdownInputs % 60));
	    	countdownInputs /= 60;
	    	countdownHours.setValue((int) (countdownInputs % 100));
	    	inputMode = savedInstanceState.getBoolean("inputMode", inputMode);
	    	if (!inputMode && timingThread.isRunning()) {
	    		// countdown view
	    		inputMode = false;
				LinearLayout inputs = (LinearLayout) rootView.findViewById(R.id.inputsLayout);
				Button startButton = (Button) rootView.findViewById(R.id.startButton);
				inputs.removeAllViews();
				inputs.addView(timerLayout);
				startButton.setText("Cancel");
				// timing thread will auto start itself
	    	}
		}
    }
    
    private Bundle savedInstance = null;
    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		savedInstance = savedInstanceState;
	}
    
    @Override
    public void onPause() {
    	super.onPause();
    	savedInstance = new Bundle();
    	onSaveInstanceState(savedInstance);
    	handler.removeCallbacks(toggleInputMode);
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	if (savedInstance != null) {
    		restoreState(savedInstance);
    		System.out.println("resumed");
    	}
    }
    
    @Override
    public void onSaveInstanceState(Bundle saveState) {
    	super.onSaveInstanceState(saveState);
    	if (rootView != null) {
    		Log.i(getClass().getName(), "Saving instance state to shared prefs");
    		SharedPreferences.Editor prefs = 
    			getContext().getSharedPreferences("Countdown", Context.MODE_PRIVATE).edit();    		
    		
    		prefs.putBoolean("inputMode", inputMode);
	    	saveState.putBoolean("inputMode", inputMode);
	    	timingThread.onSaveState(saveState);
	    	timingThread.onSaveState(prefs);
    		prefs.putLong("countdownInputs", getInputTimestamp());
	    	saveState.putLong("countdownInputs", getInputTimestamp());
	    	prefs.apply();
    	} else {
    		if (savedInstance != null) {
    			saveState.putAll(savedInstance);
    		}
    	}
    }

    @Override
    public void onDestroy() {
    	super.onDestroy();
    	timingThread.stopTimer();
    	handler.removeCallbacks(toggleInputMode);
    }
    
    public void toggleInputMode() {
    	handler.post(toggleInputMode);
    }
    
    private final Runnable toggleInputMode = new Runnable() {

		@Override
		public void run() {
			if (rootView == null) {
				return;
			}
			inputMode = !inputMode;
			LinearLayout inputs = (LinearLayout) rootView.findViewById(R.id.inputsLayout);
			Button startButton = (Button) rootView.findViewById(R.id.startButton);
			if (inputMode) {
				//TODO: fixme
		    	handler.removeCallbacks(toggleInputMode);
				inputs.removeAllViews();
				inputs.addView(inputLayout);
				startButton.setText("Start");
				timingThread.stopTimer();
				if (alarmPendingIntent == null) {
					// should be unique
					Intent intent = new Intent(getContext(), AlarmService.class)
						.putExtra("startAlarm", true)
						.setAction("startAlarmAt" + (timingThread.endTime));
					alarmPendingIntent = PendingIntent.getService(getContext(), 0, intent,
							PendingIntent.FLAG_ONE_SHOT);
				}
				AlarmManager alarmMgr = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
				alarmMgr.cancel(alarmPendingIntent);
				alarmPendingIntent = null;
			} else {
				timingThread.startTimer(getInputTimestamp());
				
				inputs.removeAllViews();
				inputs.addView(timerLayout);
				startButton.setText("Cancel");
				
				AlarmManager alarmMgr = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
				// should be unique
				Intent intent = new Intent(getContext(), AlarmService.class)
					.putExtra("startAlarm", true)
					.setAction("startAlarmAt" + (timingThread.endTime));
				alarmPendingIntent = PendingIntent.getService(getContext(), 0, intent,
						PendingIntent.FLAG_ONE_SHOT);
				alarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
						SystemClock.elapsedRealtime() + getInputTimestamp(), alarmPendingIntent);

				//handler.postAtTime(this, 
				//		SystemClock.uptimeMillis() + (timingThread.endTime - System.currentTimeMillis()));
			}
		}
    	
    };
    
    private long getInputTimestamp() {
    	return 1000L * (countdownHours.getValue() * 60 * 60 +
    			countdownMinutes.getValue() * 60 +
    			countdownSeconds.getValue());
    }
}
