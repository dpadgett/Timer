package org.dpadgett.timer;

import java.util.concurrent.Semaphore;

import android.R.attr;
import android.app.Fragment;
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
	
	public CountdownFragment() {
		this.inputMode = true;
		this.handler = new Handler();
		this.s = new Semaphore(0);
		this.timingThread = new Thread(new TimingThread());
	}
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.countdown, container, false);
		danWidgets = DanWidgets.create(rootView);
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
			Ringtone r = null;
			Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
			if (alert != null) {
				r = RingtoneManager.getRingtone(rootView.getContext(), alert);
			}
			if (alert == null || r == null) {
				// alert is null, using backup
				alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
				if (alert != null) {
					r = RingtoneManager.getRingtone(rootView.getContext(), alert);
				}
			}
			if (alert == null || r == null) {
				// alert backup is null, using 2nd backup
				alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
				if (alert != null) {
					r = RingtoneManager.getRingtone(rootView.getContext(), alert);
				}
			}
			if (r == null) {
				System.err.println("Could not find alert sound!");
			} else {
				r.play();
			}
		}
    }
}
