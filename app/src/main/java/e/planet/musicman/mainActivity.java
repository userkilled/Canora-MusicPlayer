package e.planet.musicman;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.renderscript.ScriptGroup;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class mainActivity extends Activity {
    //Callbacks
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainlayout);
        startplayer();
    }
    protected void onStart()
    {
        super.onStart();
        Log.v(LOG_TAG,"onStart Called.");
        setExtensions();
        playbutton_click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                player.pauseResume();
            }
        };
        prevbutton_click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                player.previous();
            }
        };
        nexbutton_click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                player.next();
            }
        };
        Button playbtn = findViewById(R.id.buttonPlay);
        Button prevbtn = findViewById(R.id.buttonPrev);
        Button nexbtn = findViewById(R.id.buttonNex);
        playbtn.setOnClickListener(playbutton_click);
        prevbtn.setOnClickListener(prevbutton_click);
        nexbtn.setOnClickListener(nexbutton_click);
    }
    protected void onResume()
    {
        super.onResume();
    }
    protected void onPause()
    {
        super.onPause();
    }
    protected void onStop()
    {
        super.onStop();
        stopplayer();
    }
    protected void onRestart()
    {
        super.onRestart();
    }
    protected void onDestroy()
    {
        super.onDestroy();
    }
    //My Code
    //Globals
    private playerService player;

    File[] daSongs = new File[1000];

    String LOG_TAG = "main";

    View.OnClickListener playbutton_click;
    View.OnClickListener prevbutton_click;
    View.OnClickListener nexbutton_click;

    List<String> validExtensions = new ArrayList<String>();

    public void startplayer()
    {
        doBindService();
    }
    public void stopplayer()
    {
        doUnbindService();
    }

    public void initPlayer()
    {
        File folder = Environment.getExternalStorageDirectory();
        if (folder != null && player != null)
        {
            File[] songs = folder.listFiles();
            if (songs != null)
            {
                int ind = 0;
                for (File file : songs) {
                    String ext = getFileExtension(file);
                    if (validExtensions.contains(ext)) {
                        Log.v(LOG_TAG,"Adding " + file.getName() + " to the Songs.");
                        daSongs[ind] = file;
                        ind++;
                    }
                }
                player.init(daSongs,ind - 1);
                player.play(0);
            }
            else {
                Log.v(LOG_TAG,"SDCARD FILES ARE NULL, debug: " + folder.getAbsolutePath() + " " + folder.listFiles());
            }
        }
        else
        {
            Log.v(LOG_TAG,"SDCARD / PLAYER IS NULL");
        }
    }

    private void setExtensions()
    {
        validExtensions.add(".mp3");
        validExtensions.add(".avi");
    }

    private static String getFileExtension(File file) {
        String extension = "";

        try {
            if (file != null && file.exists()) {
                String name = file.getName();
                extension = name.substring(name.lastIndexOf("."));
            }
        } catch (Exception e) {
            extension = "";
        }

        return extension;

    }
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            player = ((playerService.LocalBinder)service).getService();
            initPlayer();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            player = null;
        }
    };

    void doBindService()
    {
        // Attempts to establish a connection with the service.  We use an
        // explicit class name because we want a specific service
        // implementation that we know will be running in our own process
        // (and thus won't be supporting component replacement by other
        // applications).
        Intent intent = new Intent(this,playerService.class);
        startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    void doUnbindService()
    {
        Intent intent = new Intent(this,playerService.class);
        unbindService(mConnection);
        stopService(intent);
    }
}
