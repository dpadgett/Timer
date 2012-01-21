package org.dpadgett.timer;

import org.dpadgett.timer.CountdownThread.OnFinishedListener;

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
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
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
        this.timerLayout =
        		(LinearLayout) inflater.inflate(R.layout.countdown_timer, container, false);
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
				// done to prevent momentary flicker
				TextView timerText = (TextView) timerLayout.findViewById(R.id.countdownTimer);
				timerText.setText(CountdownThread.getTimerText(getInputTimestamp()));
				
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
    
    private long getInputTimestamp() {
    	return 1000L * (countdownHours.getValue() * 60 * 60 +
    			countdownMinutes.getValue() * 60 +
    			countdownSeconds.getValue());
    }
    
    /** Pops up the alarm dialog */
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
