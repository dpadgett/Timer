/*  
 * Copyright 2012 Dan Padgett
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

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

/**
 * Service which controls playback of a single alarm, initiated by an
 * {@link Intent}.
 * 
 * @author dpadgett
 */
public class AlarmService extends Service {

    private MediaPlayer alarmPlayer;
    private Context context;

    private void initRingtone() {
        final Uri alarmUri = getRingtoneUri(context.getSharedPreferences("Countdown", MODE_PRIVATE));
        if (alarmUri != null) {
            alarmPlayer = new MediaPlayer();
            alarmPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            try {
                alarmPlayer.setDataSource(context, alarmUri);
                alarmPlayer.setLooping(true);
                alarmPlayer.prepare();
            } catch (final Exception e) {
                // Log.e(getClass().getName(), "Couldn't init ringtone " + alarmUri.toString(), e);
                alarmPlayer.release();
                alarmPlayer = null;
            }
        }
    }

    static Uri getRingtoneUri(final SharedPreferences prefs) {
        final String alarmUriString = prefs.getString("alarmUri", null);
        Uri alarmUri = null;
        if (alarmUriString != null) {
            alarmUri = Uri.parse(alarmUriString);
        }
        if (alarmUri == null) {
            alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        }
        if (alarmUri == null) {
            // alert is null, using backup
            alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        if (alarmUri == null) {
            // alert backup is null, using 2nd backup
            alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        }
        if (alarmUri == null) {
            // Log.w(AlarmService.class.getName(), "Could not find alert sound!");
        }
        return alarmUri;
    }

    public void dismissNotification() {
        if (alarmPlayer != null) {
            alarmPlayer.stop();
            alarmPlayer.release();
            alarmPlayer = null;
            // Log.i(getClass().getName(), "Stopped ringtone");
        } else {
            // Log.i(getClass().getName(), "Alarm not ringing!");
        }
        final NotificationManager manager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(R.id.countdownNotification);
    }

    private void countdownFinished() {
        // creates the notification, notification dialog, and starts the ringtone
        if (alarmPlayer != null) {
            alarmPlayer.start();
        }

        final NotificationManager mNotificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        final int icon = drawable.ic_dialog_info;
        final String tickerText = context.getString(R.string.countdown_timer_finished);
        final long when = System.currentTimeMillis();

        final Notification notification = new Notification(icon, tickerText, when);
        notification.flags |= Notification.FLAG_AUTO_CANCEL | Notification.FLAG_ONGOING_EVENT;

        final String contentTitle = context.getString(R.string.countdown_timer_finished);
        final String contentText = context.getString(R.string.tap_to_dismiss);
        final Intent notificationIntent = new Intent(context, AlarmService.class).putExtra("startAlarm", false)
                .putExtra("fromFragment", false).setAction("internalStopAlarm");
        final PendingIntent contentIntent = PendingIntent.getService(context, 0, notificationIntent,
                PendingIntent.FLAG_ONE_SHOT);

        notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
        mNotificationManager.notify(R.id.countdownNotification, notification);

        // Log.i(getClass().getName(), "Started ringtone");

        final Intent showDialog = new Intent(TimerActivity.ACTION_SHOW_DIALOG);
        context.sendBroadcast(showDialog);
        // Log.i(getClass().getName(), "Sent request to show dialog");
    }

    // This is the old onStart method that will be called on the pre-2.0
    // platform.  On 2.0 or later we override onStartCommand() so this
    // method will not be called.
    @Override
    public void onStart(final Intent intent, final int startId) {
        context = getApplicationContext();
        handleCommand(intent);
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        context = getApplicationContext();
        handleCommand(intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    private void handleCommand(final Intent intent) {
        final SharedPreferences prefs = getSharedPreferences("TimerActivity", Context.MODE_PRIVATE);
        final SharedPreferences.Editor prefsEditor = prefs.edit();
        if (intent.getBooleanExtra("startAlarm", false)) {
            if (!prefs.getBoolean("countdownDialogShowing", false)) {
                initRingtone();
                countdownFinished();
                prefsEditor.putBoolean("countdownDialogShowing", true);
                prefsEditor.commit();
                // Log.i(getClass().getName(), "Starting alarm: " + intent + "; " + intent.getExtras());
            } else {
                // Log.i(getClass().getName(), "Ignoring start alarm intent: " + intent + "; dialog already shown: " + intent.getExtras());
            }
        } else {
            dismissNotification();
            if (!intent.getBooleanExtra("fromFragment", true)) {
                final Intent dismiss = new Intent(TimerActivity.ACTION_DISMISS_DIALOG);
                context.sendBroadcast(dismiss);
                // Log.i(getClass().getName(), "Sent request to dismiss dialog");
            }
            prefsEditor.putBoolean("countdownDialogShowing", false);
            prefsEditor.commit();
            context.stopService(new Intent(context, getClass()));
        }
    }

    @Override
    public IBinder onBind(final Intent intent) {
        return new Binder();
    }

}
