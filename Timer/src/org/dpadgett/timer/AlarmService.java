package org.dpadgett.timer;

import java.util.concurrent.Semaphore;

import android.app.Service;
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
	private Thread alarmSoundingThread;
	private Semaphore cancelSemaphore;
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
        cancelSemaphore = new Semaphore(0);
    }
    
	@Override
	public IBinder onBind(Intent intent) {
		return new LocalBinder();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (canonicalInstanceHandler != null) {
			canonicalInstanceHandler.post(new Runnable() {
				@Override
				public void run() {
					fragment.dismissAlarm();
				}
			});
		}
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
				alarmPlayer.setDataSource(fragment.getContext(), alarmUri);
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
		alarmSoundingThread = new Thread(new AlarmSoundingThread(endTimestamp));
		alarmSoundingThread.start();
	}

	public void resetCanonicalInstanceHandler() {
		canonicalInstanceHandler = null;
		fragment = null;
	}
	
	private class AlarmSoundingThread implements Runnable {
		private final long endTimestamp;
		
		private AlarmSoundingThread(long endTimestamp) {
			this.endTimestamp = endTimestamp;
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
				}
			}
		}
	}
}
