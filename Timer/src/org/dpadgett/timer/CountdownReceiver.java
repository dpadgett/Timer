package org.dpadgett.timer;

import android.R.drawable;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;

/**
 * Service to track and fire alarms.
 *
 * @author dpadgett
 */
public class CountdownReceiver extends BroadcastReceiver {

	private MediaPlayer alarmPlayer;
	private Context context;

	private void initRingtone() {
		Uri alarmUri = getRingtoneUri();
		if (alarmUri != null) {
			alarmPlayer = new MediaPlayer();
			alarmPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			try {
				Context appContext = context;
				alarmPlayer.setDataSource(appContext, alarmUri);
				alarmPlayer.setLooping(true);
				alarmPlayer.prepare();
			} catch (Exception e) {
				throw new RuntimeException(e);
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
		Intent notificationIntent = new Intent(context, CountdownReceiver.class)
			.putExtra("startAlarm", false).putExtra("fromFragment", false);
		PendingIntent contentIntent =
				PendingIntent.getBroadcast(context, 0, notificationIntent, PendingIntent.FLAG_ONE_SHOT);

		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
		mNotificationManager.notify(R.id.countdownNotification, notification);
		
		System.out.println("Started ringtone");
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		this.context = context;
		if (intent.getBooleanExtra("startAlarm", false)) {
			initRingtone();
			countdownFinished();
			System.out.println("Starting alarm: " + intent + "; " + intent.getExtras());
		} else {
			dismissNotification();
			if (!intent.getBooleanExtra("fromFragment", true)) {
				Intent dismiss = new Intent(CountdownFragment.ACTION_DISMISS_DIALOG);
				context.sendBroadcast(dismiss);
				System.out.println("Sent request to dismiss dialog");
			}
		}
	}
}
