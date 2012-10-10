package org.dpadgett.timer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;

// This is an activity that is "exported" and can be started
// by other applications. One use case is an NFC tag that 
// automatically starts a new timer.
// TODO: NFC is a specific use case. Rename from 
// Stopwatch_NFC to something else (StopwatchAutoStart?)
public class Stopwatch_NFC extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stopwatch__nfc);
        
        Intent intent = new Intent(this, TimerActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra(TimerActivity.START_REASON, TimerActivity.START_REASON_NFC);
        startActivity(intent);
    }

    //@Override
    //public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.activity_stopwatch__nfc, menu);
        //return true;
    //}
}
