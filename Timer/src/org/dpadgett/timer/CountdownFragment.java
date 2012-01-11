package org.dpadgett.timer;

import java.util.concurrent.Semaphore;

import android.R.attr;
import android.R.drawable;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.Ringtone;
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
import android.view.ViewGroup;
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
	private Ringtone alarmSound;
	
	public CountdownFragment() {
		this.inputMode = true;
		this.handler = new Handler();
		this.s = new Semaphore(0);
		this.timingThread = new Thread(new TimingThread());
	}
	
	private void initRingtone() {
		alarmSound = null;
		Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
		if (alert != null) {
			alarmSound = RingtoneManager.getRingtone(rootView.getContext(), alert);
		}
		if (alert == null || alarmSound == null) {
			// alert is null, using backup
			alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
			if (alert != null) {
				alarmSound = RingtoneManager.getRingtone(rootView.getContext(), alert);
			}
		}
		if (alert == null || alarmSound == null) {
			// alert backup is null, using 2nd backup
			alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
			if (alert != null) {
				alarmSound = RingtoneManager.getRingtone(rootView.getContext(), alert);
			}
		}
		if (alarmSound == null) {
			System.err.println("Could not find alert sound!");
		}
	}
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.countdown, container, false);
		danWidgets = DanWidgets.create(rootView);
		initRingtone();
        LinearLayout inputs = (LinearLayout) rootView.findViewById(R.id.inputsLayout);
        this.inputLayout = (LinearLayout) rootView.findViewById(R.id.inputsInnerLayout);
        countdownHours = (EditText) rootView.findViewById(R.id.countdownHours);
		countdownHours.addTextChangedListener(new IntLimiter(23));
		countdownMinutes = (EditText) rootView.findViewById(R.id.countdownMinutes);
		countdownMinutes.addTextChangedListener(new IntLimiter(59));
		countdownSeconds = (EditText) rootView.findViewById(R.id.countdownSeconds);
		countdownSeconds.addTextChangedListener(new IntLimiter(59));
        this.timerLayout = createTimerLayout(inputs);
        Button startButton = (Button) rootView.findViewById(R.id.startButton);
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
				alarmSound.stop();
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
    
    private static class IntLimiter implements TextWatcher {
    	private final int limit;
    	private String oldNumber;
    	
    	private IntLimiter(int limit) {
    		this.limit = limit;
    	}
    	
		@Override
		public void afterTextChanged(Editable arg0) {
			if (arg0.length() > 0) {
				int newNumber = Integer.parseInt(arg0.toString());
				if (newNumber > limit) {
					arg0.replace(0, arg0.length(), oldNumber);
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
    		alarmSound.play();
			startButton.setText("Dismiss");
			//TODO: hack
			timerText.setText(getTimerText(0));
			AlertDialog dialog = new AlertDialog.Builder(rootView.getContext())
				.setTitle("Countdown timer finished")
				.setPositiveButton("Dismiss", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						alarmSound.stop();
					}
				})
				.setCancelable(false)
				.create();
			dialog.show();
			NotificationManager mNotificationManager = 
					(NotificationManager) rootView.getContext().getSystemService(Context.NOTIFICATION_SERVICE);
			int icon = drawable.ic_dialog_info;
			CharSequence tickerText = "Countdown timer finished";
			long when = System.currentTimeMillis();

			Notification notification = new Notification(icon, tickerText, when);
			notification.flags |= Notification.FLAG_AUTO_CANCEL;
			
			Context context = rootView.getContext();
			CharSequence contentTitle = "Countdown timer finished";
			CharSequence contentText = "Tap here to dismiss";
			Intent notificationIntent = new Intent(context, TimerActivity.class);
			//notificationIntent.addFlags(Intent.)
			PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

			notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
			mNotificationManager.notify(R.id.countdownNotification, notification);
		}
    }
}
