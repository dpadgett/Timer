package org.dpadgett.timer;

import android.R.drawable;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;

public class AlarmService extends Service {

	private MediaPlayer alarmPlayer;
	private Context context;

	private void initRingtone() {
		Uri alarmUri = getRingtoneUri();
		if (alarmUri != null) {
			alarmPlayer = new MediaPlayer();
			alarmPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
			try {
				alarmPlayer.setDataSource(context, alarmUri);
				alarmPlayer.setLooping(true);
				alarmPlayer.prepare();
			} catch (Exception e) {
				throw new RuntimeException("Couldn't init ringtone " + alarmUri.toString(), e);
			}
		}
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
	
	public void dismissNotification() {
		if (alarmPlayer != null) {
			alarmPlayer.stop();
			alarmPlayer.release();
			alarmPlayer = null;
			System.out.println("Stopped ringtone");
		} else {
			System.out.println("Alarm not ringing!");
		}
		NotificationManager manager = 
				(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		manager.cancel(R.id.countdownNotification);
	}

	private void countdownFinished() {
		// creates the notification, notification dialog, and starts the ringtone
		alarmPlayer.start();
		
		NotificationManager mNotificationManager = 
				(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		int icon = drawable.ic_dialog_info;
		CharSequence tickerText = "Countdown timer finished";
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, tickerText, when);
		notification.flags |= Notification.FLAG_AUTO_CANCEL | Notification.FLAG_ONGOING_EVENT;
		
		CharSequence contentTitle = "Countdown timer finished";
		CharSequence contentText = "Tap here to dismiss";
		Intent notificationIntent = new Intent(context, AlarmService.class)
			.putExtra("startAlarm", false).putExtra("fromFragment", false)
			.setAction("internalStopAlarm");
		PendingIntent contentIntent =
				PendingIntent.getService(context, 0, notificationIntent, PendingIntent.FLAG_ONE_SHOT);

		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
		mNotificationManager.notify(R.id.countdownNotification, notification);
		
		System.out.println("Started ringtone");

		Intent showDialog = new Intent(TimerActivity.ACTION_SHOW_DIALOG);
		context.sendBroadcast(showDialog);
		System.out.println("Sent request to show dialog");
	}
	
	// This is the old onStart method that will be called on the pre-2.0
	// platform.  On 2.0 or later we override onStartCommand() so this
	// method will not be called.
	@Override
	public void onStart(Intent intent, int startId) {
		context = getApplicationContext();
	    handleCommand(intent);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		context = getApplicationContext();
	    handleCommand(intent);
	    // We want this service to continue running until it is explicitly
	    // stopped, so return sticky.
	    return START_STICKY;
	}
	
	private void handleCommand(Intent intent) {
		SharedPreferences.Editor prefs =
				getSharedPreferences("TimerActivity", Context.MODE_PRIVATE).edit();
		if (intent.getBooleanExtra("startAlarm", false)) {
			initRingtone();
			countdownFinished();
			prefs.putBoolean("countdownDialogShowing", true);
			prefs.commit();
			System.out.println("Starting alarm: " + intent + "; " + intent.getExtras());
		} else {
			dismissNotification();
			if (!intent.getBooleanExtra("fromFragment", true)) {
				Intent dismiss = new Intent(TimerActivity.ACTION_DISMISS_DIALOG);
				context.sendBroadcast(dismiss);
				System.out.println("Sent request to dismiss dialog");
			}
			prefs.putBoolean("countdownDialogShowing", false);
			prefs.commit();
			context.stopService(new Intent(context, getClass()));
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return new Binder();
	}

}
