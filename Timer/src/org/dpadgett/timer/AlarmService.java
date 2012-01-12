package org.dpadgett.timer;

import android.app.Service;
import android.content.Intent;
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

	public void resetCanonicalInstanceHandler() {
		canonicalInstanceHandler = null;
		fragment = null;
	}
}
