package org.dpadgett.timer;

import java.util.concurrent.Semaphore;

import android.R.drawable;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

/**
 * Service to track and fire alarms.
 *
 * @author dpadgett
 */
public class AlarmService extends Service {

	private Handler canonicalInstanceHandler = null;
	private CountdownFragment fragment = null;
	private AlarmSoundingThread alarmSoundingThread;
	private MediaPlayer alarmPlayer;

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        AlarmService getService() {
            // Return this instance of LocalService so clients can call public methods
            return AlarmService.this;
        }
    }

    @Override
    public void onCreate() {
    }
    
	@Override
	public IBinder onBind(Intent intent) {
		return new LocalBinder();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		dismissNotification();
		dismissAlarmDialog();
		return 0;
	}
	
	public void setCanonicalInstance(Handler handler, CountdownFragment fragment) {
		if (canonicalInstanceHandler == null) {
			canonicalInstanceHandler = handler;
			this.fragment = fragment;
		}
	}
	
	private void initRingtone() {
		Uri alarmUri = getRingtoneUri();
		if (alarmUri != null) {
			alarmPlayer = new MediaPlayer();
			alarmPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			try {
				Context appContext = getApplicationContext();
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
	
	public void countdownStarted(long endTimestamp) {
		alarmSoundingThread = new AlarmSoundingThread(endTimestamp);
		new Thread(alarmSoundingThread).start();
	}
	
	public void cancelCountdown() {
		if (alarmSoundingThread != null) {
			alarmSoundingThread.cancel();
			alarmSoundingThread = null;
		}
	}
	
	public void dismissNotification() {
		if (alarmPlayer != null) {
			alarmPlayer.stop();
			alarmPlayer = null;
			System.out.println("Stopped ringtone");
		}
		NotificationManager manager = 
				(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		manager.cancel(R.id.countdownNotification);
		if (alarmSoundingThread != null) {
			alarmSoundingThread.cancel(); // just in case
			alarmSoundingThread = null;
		}
	}

	private void dismissAlarmDialog() {
		if (canonicalInstanceHandler != null) {
			canonicalInstanceHandler.post(new Runnable() {
				@Override
				public void run() {
					fragment.dismissAlarmDialog();
				}
			});
		} else {
			System.out.println("Warning: no canonical instance handler...");
		}
	}

	public void resetCanonicalInstanceHandler() {
		canonicalInstanceHandler = null;
		fragment = null;
	}
	
	private void countdownFinished() {
		// creates the notification, notification dialog, and starts the ringtone
		alarmPlayer.start();
		
		NotificationManager mNotificationManager = 
				(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		int icon = drawable.ic_dialog_info;
		CharSequence tickerText = "Countdown timer finished";
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, tickerText, when);
		notification.flags |= Notification.FLAG_AUTO_CANCEL | Notification.FLAG_ONGOING_EVENT;
		
		CharSequence contentTitle = "Countdown timer finished";
		CharSequence contentText = "Tap here to dismiss";
		Intent notificationIntent = new Intent(getApplicationContext(), AlarmService.class);
		PendingIntent contentIntent =
				PendingIntent.getService(getApplicationContext(), 0, notificationIntent, 0);

		notification.setLatestEventInfo(getApplicationContext(), contentTitle, contentText, contentIntent);
		mNotificationManager.notify(R.id.countdownNotification, notification);
		
		System.out.println("Started ringtone");
	}
	
	private class AlarmSoundingThread implements Runnable {
		private final long endTimestamp;
		private final Semaphore cancelSemaphore;
		
		private AlarmSoundingThread(long endTimestamp) {
			this.endTimestamp = endTimestamp;
			this.cancelSemaphore = new Semaphore(0);
		}
		
		@Override
		public void run() {
			while (!cancelSemaphore.tryAcquire()) {
				long currentTime = System.currentTimeMillis();
				long sleepTime = (endTimestamp - currentTime) / 2;
				if (sleepTime < 10) {
					sleepTime = 10;
				}
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
				}
				if (System.currentTimeMillis() >= endTimestamp) {
					// ring notification
					initRingtone();
					countdownFinished();
					break;
				}
			}
		}
		
		public void cancel() {
			cancelSemaphore.release();
		}
	}
}
