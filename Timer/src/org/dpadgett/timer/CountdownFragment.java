package org.dpadgett.timer;

import org.dpadgett.timer.AlarmService.LocalBinder;
import org.dpadgett.timer.CountdownThread.OnFinishedListener;

import android.R.attr;
import android.R.drawable;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

public class CountdownFragment extends Fragment {

	private boolean inputMode;
	private LinearLayout inputLayout;
	private LinearLayout timerLayout;
	private View rootView;
	private final Handler handler;
	private EditText countdownHours;
	private EditText countdownMinutes;
	private EditText countdownSeconds;
	private CountdownThread timingThread;
	private MediaPlayer alarmPlayer;
	private AlertDialog alarmDialog;
	
	public CountdownFragment() {
		this.inputMode = true;
		this.handler = new Handler();
	}
	
	private Uri getRingtoneUri() {
		Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
		if (alarmUri == null) {
			// alert is null, using backup
			alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		}
		if (alarmUri == null) {
			// alert backup is null, using 2nd backup
			alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
		}
		if (alarmUri == null) {
			System.err.println("Could not find alert sound!");
		}
		return alarmUri;
	}
	
	private void initRingtone() {
		Uri alarmUri = getRingtoneUri();
		if (alarmUri != null) {
			alarmPlayer = new MediaPlayer();
			alarmPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			try {
				alarmPlayer.setDataSource(rootView.getContext(), alarmUri);
				alarmPlayer.setLooping(true);
				alarmPlayer.prepare();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.countdown, container, false);
        LinearLayout inputs = (LinearLayout) rootView.findViewById(R.id.inputsLayout);
        this.inputLayout = (LinearLayout) rootView.findViewById(R.id.inputsInnerLayout);
        Button startButton = (Button) rootView.findViewById(R.id.startButton);
        countdownHours = (EditText) rootView.findViewById(R.id.countdownHours);
		countdownMinutes = (EditText) rootView.findViewById(R.id.countdownMinutes);
		countdownSeconds = (EditText) rootView.findViewById(R.id.countdownSeconds);
		countdownHours.addTextChangedListener(new IntLimiter(23, countdownHours, null));
		countdownHours.setOnFocusChangeListener(new ClearOnFocusListener());
		countdownMinutes.addTextChangedListener(new IntLimiter(59, countdownMinutes, null));
		countdownMinutes.setOnFocusChangeListener(new ClearOnFocusListener());
		countdownSeconds.addTextChangedListener(new IntLimiter(59, countdownSeconds, null));
		countdownSeconds.setOnFocusChangeListener(new ClearOnFocusListener());
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
		rootView.getContext().bindService(
        		new Intent(rootView.getContext(), AlarmService.class),
        			new ServiceConnection() {

						@Override
						public void onServiceConnected(ComponentName arg0,
								IBinder binder) {
							LocalBinder localBinder = (LocalBinder) binder;
							localBinder.getService()
								.setCanonicalInstance(handler,
										CountdownFragment.this);
						}

						@Override
						public void onServiceDisconnected(ComponentName arg0) {
						}
        			
        		}, Context.BIND_AUTO_CREATE);
		if (savedInstanceState != null) {
			restoreState(savedInstanceState);
		}
        return rootView;
    }
    
    private void restoreState(Bundle savedInstanceState) {
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
    }

    @Override
    public void onDestroy() {
    	super.onDestroy();
    	timingThread.stopTimer();
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
			} else {
				inputs.removeAllViews();
				inputs.addView(timerLayout);
				startButton.setText("Cancel");
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
    	return 1000L * (Integer.parseInt(countdownHours.getText().toString()) * 60 * 60 +
    			Integer.parseInt(countdownMinutes.getText().toString()) * 60 +
    			Integer.parseInt(countdownSeconds.getText().toString()));
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
    
    public void dismissAlarm() {
    	alarmDialog.dismiss();
    	alarmPlayer.stop();
		alarmPlayer.release();
		alarmPlayer = null;
    }

    /** Plays the alarm and sets button text to 'dismiss' */
    private class PlayAlarm implements Runnable {
    	Button startButton = (Button) rootView.findViewById(R.id.startButton);

    	@Override
		public void run() {
			startButton.setText("Dismiss");
			final NotificationManager mNotificationManager = 
					(NotificationManager) rootView.getContext().getSystemService(Context.NOTIFICATION_SERVICE);
			int icon = drawable.ic_dialog_info;
			CharSequence tickerText = "Countdown timer finished";
			long when = System.currentTimeMillis();

			Notification notification = new Notification(icon, tickerText, when);
			notification.flags |= Notification.FLAG_AUTO_CANCEL | Notification.FLAG_ONGOING_EVENT;
			
			Context context = rootView.getContext();
			CharSequence contentTitle = "Countdown timer finished";
			CharSequence contentText = "Tap here to dismiss";
			Intent notificationIntent = new Intent(rootView.getContext(), AlarmService.class);
			PendingIntent contentIntent =
					PendingIntent.getService(context, 0, notificationIntent, 0);

			notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
			mNotificationManager.notify(R.id.countdownNotification, notification);
			
			alarmDialog = new AlertDialog.Builder(rootView.getContext())
					.setTitle("Countdown timer finished")
					.setPositiveButton("Dismiss",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.dismiss();
									mNotificationManager.cancel(R.id.countdownNotification);
									alarmPlayer.stop();
									alarmPlayer.release();
									alarmPlayer = null;
								}
							})
					.setCancelable(false)
					.create();
			alarmDialog.show();
			initRingtone();
    		alarmPlayer.start();
		}
    }
}
