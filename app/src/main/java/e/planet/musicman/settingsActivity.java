package e.planet.musicman;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.SeekBar;
import android.widget.Toast;

public class settingsActivity extends AppCompatActivity {
    //Callbacks
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(LOG_TAG, "ONCREATE CALLED");
        init();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.v(LOG_TAG, "ONSTOP CALLED");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.v(LOG_TAG, "ONDESTROY CALLED");
        if (player != null)
            doUnbindService();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //Globals
    String LOG_TAG = "SETTTINGS";
    playerService player = null;

    //Custom Functions
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.v(LOG_TAG, "Service Connected.");
            player = ((playerService.LocalBinder) service).getService();
            final SeekBar sk = (SeekBar) findViewById(R.id.seekBar1);
            sk.setProgress((int) (player.getVolume() * 100));
        }

        public void onServiceDisconnected(ComponentName className) {
            player = null;
        }
    };

    void doBindService() {
        Intent intent = new Intent(this, playerService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    void doUnbindService() {
        Intent intent = new Intent(this, playerService.class);
        unbindService(mConnection);
    }

    void init() {
        setContentView(R.layout.settingslayout);
        if (player == null)
            doBindService();
        final SeekBar sk = (SeekBar) findViewById(R.id.seekBar1);
        sk.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float c = (float) progress / 100;
                Log.v(LOG_TAG, "Setting Volume: " + c);
                if (player != null)
                    player.setVolume(c);
            }
        });
    }
}
