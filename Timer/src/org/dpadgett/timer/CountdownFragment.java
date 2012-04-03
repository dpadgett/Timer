package org.dpadgett.timer;

import org.dpadgett.widget.CountdownTextView;
import org.dpadgett.widget.FasterNumberPicker;
import org.dpadgett.widget.FasterNumberPicker.OnValueChangeListener;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

/**
 * Fragment which handles the UI and logic for the countdown timer.
 *
 * @author dpadgett
 */
public class CountdownFragment extends Fragment {

	private boolean inputMode;
	private LinearLayout inputLayout;
	private LinearLayout timerLayout;
	private View rootView;
	private final Handler handler;
	private FasterNumberPicker countdownHours;
	private FasterNumberPicker countdownMinutes;
	private FasterNumberPicker countdownSeconds;
	private CountdownState timingState;

	public PendingIntent alarmPendingIntent;
	private AlarmSelector alarmSelector;

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

        FasterNumberPicker.Formatter twoDigitFormatter = new FasterNumberPicker.Formatter() {
			@Override
			public String format(int value) {
				return String.format("%02d", value);
			}
        };
        
        FasterNumberPicker.OnValueChangeListener saveTimestampListener = new OnValueChangeListener() {
			@Override
			public void onValueChange(FasterNumberPicker picker, int oldVal, int newVal) {
				SharedPreferences.Editor prefs = 
						getContext().getSharedPreferences("Countdown", Context.MODE_PRIVATE).edit();    		
				prefs.putLong("countdownInputs", getInputTimestamp());
				prefs.commit();
			}
		};

        countdownHours = (FasterNumberPicker) rootView.findViewById(R.id.countdownHours);
        countdownHours.setMinValue(0);
        countdownHours.setMaxValue(99);
		countdownHours.setFormatter(twoDigitFormatter);
		// I will burn in hell for this
		View view = countdownHours.findViewById(org.dpadgett.compat.R.id.numberpicker_input);
		if (view != null) {
			EditText inputText = (EditText) view;
			inputText.setFocusable(false);
		}
		countdownHours.setDisableInputText(true);
		countdownHours.setOnValueChangedListener(saveTimestampListener);

		countdownMinutes = (FasterNumberPicker) rootView.findViewById(R.id.countdownMinutes);
        countdownMinutes.setMinValue(0);
        countdownMinutes.setMaxValue(59);
		countdownMinutes.setFormatter(twoDigitFormatter);
		view = countdownMinutes.findViewById(org.dpadgett.compat.R.id.numberpicker_input);
		if (view != null) {
			EditText inputText = (EditText) view;
			inputText.setFocusable(false);
		}
		countdownMinutes.setDisableInputText(true);
		countdownMinutes.setOnValueChangedListener(saveTimestampListener);

		countdownSeconds = (FasterNumberPicker) rootView.findViewById(R.id.countdownSeconds);
        countdownSeconds.setMinValue(0);
        countdownSeconds.setMaxValue(59);
		countdownSeconds.setFormatter(twoDigitFormatter);
		view = countdownSeconds.findViewById(org.dpadgett.compat.R.id.numberpicker_input);
		if (view != null) {
			EditText inputText = (EditText) view;
			inputText.setFocusable(false);
		}
		countdownSeconds.setDisableInputText(true);
		countdownSeconds.setOnValueChangedListener(saveTimestampListener);

		this.alarmSelector =
				new AlarmSelector((Spinner) rootView.findViewById(R.id.alarm_tones));
		
        this.timerLayout =
        		(LinearLayout) inflater.inflate(R.layout.countdown_timer, container, false);

		startButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (inputMode) {
					inputModeOff();
				} else {
					inputModeOn();
				}
			}
        });

		restoreState();

		// forcefully pre-render content so it is cached
		rootView.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
		rootView.layout(0, 0, rootView.getMeasuredWidth(), rootView.getMeasuredHeight());
		rootView.draw(new Canvas(Bitmap.createBitmap(rootView.getMeasuredWidth(), rootView.getMeasuredHeight(), Bitmap.Config.ARGB_8888)));

		return rootView;
    }
    
	private void restoreState() {
        SharedPreferences prefs =
				getContext().getSharedPreferences("Countdown", Context.MODE_PRIVATE);

    	timingState = new CountdownState(
				(CountdownTextView) timerLayout.findViewById(R.id.countdownTimer), prefs);
		
        if (prefs.contains("countdownInputs")) {
	    	long countdownInputs = prefs.getLong("countdownInputs", 0L);
	    	countdownInputs /= 1000;
	    	countdownSeconds.setValue((int) (countdownInputs % 60));
	    	countdownInputs /= 60;
	    	countdownMinutes.setValue((int) (countdownInputs % 60));
	    	countdownInputs /= 60;
	    	countdownHours.setValue((int) (countdownInputs % 100));
	    	inputMode = !timingState.isRunning();
	    	if (!inputMode) {
	    		// countdown view
				LinearLayout inputs = (LinearLayout) rootView.findViewById(R.id.inputsLayout);
				Button startButton = (Button) rootView.findViewById(R.id.startButton);
				inputs.removeAllViews();
				inputs.addView(timerLayout);
				startButton.setText("Cancel");
				// timing thread will auto start itself
	    	}
        }
        
        alarmSelector.restoreState();
    }
    
	@Override
    public void onPause() {
    	super.onPause();
    	saveState();
    	handler.removeCallbacks(inputModeOff);
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	if (rootView != null) {
    		restoreState();
    	}
    }
    
    @Override
    public void onSaveInstanceState(Bundle saveState) {
    	super.onSaveInstanceState(saveState);
    	if (rootView != null) {
    		saveState();
    	}
    }

    private void saveState() {
		SharedPreferences.Editor prefs = 
			getContext().getSharedPreferences("Countdown", Context.MODE_PRIVATE).edit();    		
		
    	timingState.onSaveState(prefs);
		prefs.putLong("countdownInputs", getInputTimestamp());
    	prefs.commit();
    }

    @Override
    public void onDestroy() {
    	super.onDestroy();
    	timingState.stopTimer();
    	handler.removeCallbacks(inputModeOff);
    	alarmSelector.destroy();
    }

    public void inputModeOff() {
    	handler.post(inputModeOff);
    }

    public void inputModeOn() {
    	handler.post(inputModeOn);
    }

    private final Runnable inputModeOff = new Runnable() {

		@Override
		public void run() {
			if (rootView == null) {
				return;
			}
			inputMode = false;
			LinearLayout inputs = (LinearLayout) rootView.findViewById(R.id.inputsLayout);
			Button startButton = (Button) rootView.findViewById(R.id.startButton);

			timingState.startTimer(getInputTimestamp());
			
			inputs.removeAllViews();
			inputs.addView(timerLayout);
			startButton.setText("Cancel");
			
			AlarmManager alarmMgr = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
			// should be unique
			Intent intent = new Intent(getContext(), AlarmService.class)
				.putExtra("startAlarm", true)
				.setAction("startAlarmAt" + (timingState.endTime));
			alarmPendingIntent = PendingIntent.getService(getContext(), 0, intent,
					PendingIntent.FLAG_ONE_SHOT);
			alarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
					SystemClock.elapsedRealtime() + getInputTimestamp(), alarmPendingIntent);

			saveState();
		}
    	
    };
    
    private final Runnable inputModeOn = new Runnable() {

		@Override
		public void run() {
			if (rootView == null) {
				return;
			}
			inputMode = true;
			LinearLayout inputs = (LinearLayout) rootView.findViewById(R.id.inputsLayout);
			Button startButton = (Button) rootView.findViewById(R.id.startButton);

			//TODO: fixme
	    	handler.removeCallbacks(inputModeOff);
	    	handler.removeCallbacks(inputModeOn);
			inputs.removeAllViews();
			inputs.addView(inputLayout);
			startButton.setText("Start");
			timingState.stopTimer();
			if (alarmPendingIntent == null) {
				// should be unique
				Intent intent = new Intent(getContext(), AlarmService.class)
					.putExtra("startAlarm", true)
					.setAction("startAlarmAt" + (timingState.endTime));
				alarmPendingIntent = PendingIntent.getService(getContext(), 0, intent,
						PendingIntent.FLAG_ONE_SHOT);
			}
			AlarmManager alarmMgr = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
			alarmMgr.cancel(alarmPendingIntent);
			alarmPendingIntent = null;

			saveState();
		}
    	
    };
    
    private long getInputTimestamp() {
    	return 1000L * (countdownHours.getValue() * 60 * 60 +
    			countdownMinutes.getValue() * 60 +
    			countdownSeconds.getValue());
    }
}
