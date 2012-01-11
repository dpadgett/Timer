package org.dpadgett.timer;

import java.util.concurrent.Semaphore;

import android.R.attr;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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
	private final Semaphore s;
	private DanWidgets danWidgets;
	private Thread timingThread;
	private MediaPlayer alarmPlayer;
	
	public CountdownFragment() {
		this.inputMode = true;
		this.handler = new Handler();
		this.s = new Semaphore(0);
		this.timingThread = new Thread(new TimingThread());
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
		danWidgets = DanWidgets.create(rootView);
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
        startButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				handler.post(new ToggleInputMode());
			}
        });
        return rootView;
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
				s.release();
				try {
					timingThread.join();
				} catch (InterruptedException e) {
				}
				timingThread = new Thread(new TimingThread());
			} else {
				inputs.removeAllViews();
				inputs.addView(timerLayout);
				TextView timerText = (TextView) timerLayout.findViewById(R.id.countdownTimer);
				timerText.setText(getInputTime());
				startButton.setText("Cancel");
				timingThread.start();
			}
		}
    	
    }
    
    private class ClearOnFocusListener implements OnFocusChangeListener {
		@Override
		public void onFocusChange(View v, boolean hasFocus) {
			final View view = v;
			if (hasFocus) {
				handler.post(new Runnable() {
					@Override
					public void run() {
						((TextView)view).setText("");
					}
				});
			}/* else {
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
    
    private String getInputTime() {
    	return getTimerText(getInputTimestamp());
    }
    
    private static String getTimerText(long elapsedTime) {
		if (elapsedTime % 1000 > 0) {
			elapsedTime += 1000;
		}
    	elapsedTime /= 1000;
		long secs = elapsedTime % 60;
		elapsedTime /= 60;
		long mins = elapsedTime % 60;
		elapsedTime /= 60;
		long hours = elapsedTime % 60;
		return String.format("%02d:%02d:%02d", hours, mins, secs);
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
    
    private class TimingThread implements Runnable {
		@Override
		public void run() {
			long endTime = System.currentTimeMillis() + getInputTimestamp();
			DanTextView timerText = danWidgets.getTextView(R.id.countdownTimer);
			while (!s.tryAcquire()) {
				long timeUntilEnd = endTime - System.currentTimeMillis();
				if (timeUntilEnd <= 0) {
					// we hit the end of the timer, so run an alert!
					timeUntilEnd = 0;
					playAlarm();
					handler.post(new ToggleInputMode());
					s.acquireUninterruptibly();
					break;
				}
				String timeText = getTimerText(timeUntilEnd);
				timerText.setText(timeText);
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
				}
			}
		}

		private void playAlarm() {
			handler.post(new PlayAlarm());
		}
    }

    /** Plays the alarm and sets button text to 'dismiss' */
    private class PlayAlarm implements Runnable {
    	Button startButton = (Button) rootView.findViewById(R.id.startButton);
    	TextView timerText = (TextView) rootView.findViewById(R.id.countdownTimer);

    	@Override
		public void run() {
			startButton.setText("Dismiss");
			//TODO: hack
			timerText.setText(getTimerText(0));
			/*final NotificationManager mNotificationManager = 
					(NotificationManager) rootView.getContext().getSystemService(Context.NOTIFICATION_SERVICE);
			int icon = drawable.ic_dialog_info;
			CharSequence tickerText = "Countdown timer finished";
			long when = System.currentTimeMillis();

			Notification notification = new Notification(icon, tickerText, when);
			notification.flags |= Notification.FLAG_AUTO_CANCEL | Notification.FLAG_ONGOING_EVENT;
			
			Context context = rootView.getContext();
			CharSequence contentTitle = "Countdown timer finished";
			CharSequence contentText = "Tap here to dismiss";
			Intent notificationIntent = new Intent("org.dpadgett.timer.COUNTDOWN_DISMISS")
				.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			PendingIntent contentIntent =
					PendingIntent.getActivity(context, 0, notificationIntent, 0);

			notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
			mNotificationManager.notify(R.id.countdownNotification, notification);*/
			
			AlertDialog dialog = new AlertDialog.Builder(rootView.getContext())
					.setTitle("Countdown timer finished")
					.setPositiveButton("Dismiss",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.dismiss();
									//mNotificationManager.cancel(R.id.countdownNotification);
									alarmPlayer.stop();
									alarmPlayer.release();
									alarmPlayer = null;
								}
							})
					.setCancelable(false)
					.create();
			dialog.show();
			initRingtone();
    		alarmPlayer.start();
		}
    }
}
