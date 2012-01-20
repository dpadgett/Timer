package org.dpadgett.timer;

import org.dpadgett.timer.CountdownThread.OnFinishedListener;

import android.R.attr;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.NumberPicker;
import android.widget.NumberPicker.Formatter;
import android.widget.TextView;

public class CountdownFragment extends Fragment {

	static final String ACTION_DISMISS_DIALOG = "org.dpadgett.timer.CountdownFragment.DISMISS_DIALOG";
	
	private boolean inputMode;
	private LinearLayout inputLayout;
	private LinearLayout timerLayout;
	private View rootView;
	private final Handler handler;
	private NumberPicker countdownHours;
	private NumberPicker countdownMinutes;
	private NumberPicker countdownSeconds;
	private CountdownThread timingThread;
	private AlertDialog alarmDialog;

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
        rootView = inflater.inflate(R.layout.countdown, container, false);
		getContext().getApplicationContext().registerReceiver(dismissDialogReceiver, new IntentFilter(ACTION_DISMISS_DIALOG));
        LinearLayout inputs = (LinearLayout) rootView.findViewById(R.id.inputsLayout);
        this.inputLayout = (LinearLayout) rootView.findViewById(R.id.inputsInnerLayout);
        Button startButton = (Button) rootView.findViewById(R.id.startButton);
        countdownHours = (NumberPicker) rootView.findViewById(R.id.countdownHours);
        countdownHours.setMinValue(0);
        countdownHours.setMaxValue(99);
        Formatter twoDigitFormatter = new NumberPicker.Formatter() {
			@Override
			public String format(int value) {
				return String.format("%02d", value);
			}
        };
		countdownHours.setFormatter(twoDigitFormatter);
		countdownMinutes = (NumberPicker) rootView.findViewById(R.id.countdownMinutes);
        countdownMinutes.setMinValue(0);
        countdownMinutes.setMaxValue(59);
		countdownMinutes.setFormatter(twoDigitFormatter);
		countdownSeconds = (NumberPicker) rootView.findViewById(R.id.countdownSeconds);
        countdownSeconds.setMinValue(0);
        countdownSeconds.setMaxValue(59);
		countdownSeconds.setFormatter(twoDigitFormatter);
		//countdownHours.addTextChangedListener(new IntLimiter(99, countdownHours, null));
		//countdownHours.setOnFocusChangeListener(new ClearOnFocusListener());
		//countdownMinutes.addTextChangedListener(new IntLimiter(59, countdownMinutes, null));
		//countdownMinutes.setOnFocusChangeListener(new ClearOnFocusListener());
		//countdownSeconds.addTextChangedListener(new IntLimiter(59, countdownSeconds, null));
		//countdownSeconds.setOnFocusChangeListener(new ClearOnFocusListener());
        this.timerLayout = createTimerLayout(inputs);
		timingThread = new CountdownThread(
				DanWidgets.create(timerLayout).getTextView(R.id.countdownTimer),
				savedInstanceState);
		timingThread.addOnFinishedListener(new OnFinishedListener() {
			@Override
			public void onFinished() {
				handler.post(new PlayAlarm());
				handler.post(new ToggleInputMode());
			}
		});
		startButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				handler.post(new ToggleInputMode());
			}
        });
		if (savedInstanceState != null) {
			restoreState(savedInstanceState);
		}
        return rootView;
    }
    
    private void restoreState(Bundle savedInstanceState) {
    	long countdownInputs = savedInstanceState.getLong("countdownInputs", 0L);
    	countdownInputs /= 1000;
    	countdownSeconds.setValue((int) (countdownInputs % 60));
    	countdownInputs /= 60;
    	countdownMinutes.setValue((int) (countdownInputs % 60));
    	countdownInputs /= 60;
    	countdownHours.setValue((int) (countdownInputs % 100));
    	inputMode = savedInstanceState.getBoolean("inputMode", inputMode);
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
    
    @Override
    public void onSaveInstanceState(Bundle saveState) {
    	super.onSaveInstanceState(saveState);
    	saveState.putBoolean("inputMode", inputMode);
    	timingThread.onSaveState(saveState);
    	saveState.putLong("countdownInputs", getInputTimestamp());
    }

    @Override
    public void onDestroy() {
    	super.onDestroy();
    	timingThread.stopTimer();
		getContext().unregisterReceiver(dismissDialogReceiver);
    }
    
    private class ToggleInputMode implements Runnable {

		@Override
		public void run() {
			inputMode = !inputMode;
			LinearLayout inputs = (LinearLayout) rootView.findViewById(R.id.inputsLayout);
			Button startButton = (Button) rootView.findViewById(R.id.startButton);
			if (inputMode) {
				inputs.removeAllViews();
				inputs.addView(inputLayout);
				startButton.setText("Start");
				timingThread.stopTimer();
				if (alarmPendingIntent != null) {
					AlarmManager alarmMgr = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
					alarmMgr.cancel(alarmPendingIntent);
					alarmPendingIntent = null;
				}
			} else {
				inputs.removeAllViews();
				inputs.addView(timerLayout);
				startButton.setText("Cancel");
				
				AlarmManager alarmMgr = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
				// should be unique
				Intent intent = new Intent(getContext(), AlarmService.class)
					.putExtra("startAlarm", true)
					.setAction("startAlarmAt" + (getInputTimestamp() + System.currentTimeMillis()));
				alarmPendingIntent = PendingIntent.getService(getContext(), 0, intent,
						PendingIntent.FLAG_ONE_SHOT);
				alarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
						SystemClock.elapsedRealtime() + getInputTimestamp(), alarmPendingIntent);

				timingThread.startTimer(getInputTimestamp());
			}
		}
    	
    }
    
    private class ClearOnFocusListener implements OnFocusChangeListener {
		@Override
		public void onFocusChange(View v, boolean hasFocus) {
			/*final View view = v;
			if (hasFocus) {
				handler.post(new Runnable() {
					@Override
					public void run() {
						((TextView)view).setText("");
					}
				});
			} else {
				handler.post(new Runnable() {
					@Override
					public void run() {
						String curText = ((TextView)view).getText().toString();
						int curValue = 0;
						if (curText.length() > 0) {
							curValue = Integer.parseInt(curText);
						}
						((TextView)view).setText(String.format("%02d", curValue));
					}
				});
			}*/
		}
    }
    
    private static class IntLimiter implements TextWatcher {
    	private final int limit;
    	private String oldNumber;
    	private final View nextFocus;
		private final View thisView;
    	
    	private IntLimiter(int limit, View thisView, View nextFocus) {
    		this.limit = limit;
    		this.thisView = thisView;
    		this.nextFocus = nextFocus;
    	}
    	
		@Override
		public void afterTextChanged(Editable arg0) {
			if (arg0.length() > 0) {
				int newNumber = Integer.parseInt(arg0.toString());
				if (newNumber > limit) {
					arg0.replace(0, arg0.length(), oldNumber);
				}
			}
			
			/*int finalValue = 0;
			if (arg0.length() > 0) {
				finalValue = Integer.parseInt(arg0.toString());
			}*/
			if (/*finalValue * 10 > limit || */arg0.length() + 1 > new String("" + limit).length()) {
				if (nextFocus == null) {
					InputMethodManager imm =
							(InputMethodManager) thisView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(thisView.getWindowToken(), 0);
				} else {
					nextFocus.requestFocus();
				}
			}
		}

		@Override
		public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
				int arg3) {
			oldNumber = arg0.toString();
		}

		@Override
		public void onTextChanged(CharSequence arg0, int arg1, int arg2,
				int arg3) {
		}
    	
    }
    
    private long getInputTimestamp() {
    	return 1000L * (countdownHours.getValue() * 60 * 60 +
    			countdownMinutes.getValue() * 60 +
    			countdownSeconds.getValue());
    }
    
    private static LinearLayout createTimerLayout(LinearLayout inputs) {
		LinearLayout runningLayout = new LinearLayout(inputs.getContext());
		runningLayout.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 0f));
		runningLayout.setGravity(Gravity.CENTER_HORIZONTAL);
		runningLayout.setId(R.id.inputsInnerLayout);
		
		TextView timerText = new TextView(inputs.getContext());
		timerText.setText("00:00:00");
		timerText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 50f);
		timerText.setTextAppearance(inputs.getContext(), attr.textAppearanceLarge);
		timerText.setId(R.id.countdownTimer);
		
		runningLayout.addView(timerText);
		return runningLayout;
    }
    
    public void dismissAlarmDialog() {
    	alarmDialog.dismiss();
    }

    /** Plays the alarm and sets button text to 'dismiss' */
    private class PlayAlarm implements Runnable {
    	@Override
		public void run() {
			alarmDialog = new AlertDialog.Builder(rootView.getContext())
					.setTitle("Countdown timer finished")
					.setPositiveButton("Dismiss",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.dismiss();
									Intent intent = new Intent(getContext(), AlarmService.class)
										.putExtra("startAlarm", false).putExtra("fromFragment", true)
										.setAction("stopAlarm");
									getContext().startService(intent);
								}
							})
					.setCancelable(false)
					.create();
			alarmDialog.show();
		}
    }
    
    private BroadcastReceiver dismissDialogReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			alarmDialog.dismiss();
		}
    };
}
