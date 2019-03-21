package e.planet.musicman;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.renderscript.ScriptGroup;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static java.lang.Math.toIntExact;

public class mainActivity extends Activity implements AdapterView.OnItemClickListener {
    //Callbacks
    int MY_PERMISSIONS_REQUEST_READ_CONTACTS;
    private ListView lv;
    private TextView songDisplay;
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.v(LOG_TAG,"onCreate Called");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainlayout);
        if (ContextCompat.checkSelfPermission(mainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(mainActivity.this, new String[]{Manifest.permission.READ_CONTACTS}, MY_PERMISSIONS_REQUEST_READ_CONTACTS);
        }
        startplayer();
        setExtensions();
        playbutton_click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button btn = findViewById(R.id.buttonPlay);
                player.pauseResume(btn);
            }
        };
        prevbutton_click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                player.previous();
                player.setSongDisplay(songDisplay);
            }
        };
        nexbutton_click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                player.next();
                player.setSongDisplay(songDisplay);
            }
        };
        shufbutton_click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button btn = findViewById(R.id.buttonShuff);
                player.enableShuffle(btn);
            }
        };
        Button playbtn = findViewById(R.id.buttonPlay);
        Button prevbtn = findViewById(R.id.buttonPrev);
        Button nexbtn = findViewById(R.id.buttonNex);
        Button shufbtn = findViewById(R.id.buttonShuff);
        playbtn.setOnClickListener(playbutton_click);
        playbtn.getBackground().setColorFilter(getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
        prevbtn.setOnClickListener(prevbutton_click);
        nexbtn.setOnClickListener(nexbutton_click);
        shufbtn.setOnClickListener(shufbutton_click);
        shufbtn.getBackground().setColorFilter(getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);

        ListView listview = (ListView) findViewById(R.id.listView1);
        listview.setOnItemClickListener(this);
        songDisplay = findViewById(R.id.songDisplay);
    }
    protected void onStart()
    {
        //NOTE: Gets called Multiple Times
        super.onStart();
        Log.v(LOG_TAG,"onStart Called.");
    }
    protected void onResume()
    {
        super.onResume();
        //TODO: Add New Files on Resume
    }
    protected void onPause()
    {
        super.onPause();
    }
    protected void onStop()
    {
        super.onStop();
    }
    protected void onRestart()
    {
        super.onRestart();
    }
    protected void onDestroy()
    {
        super.onDestroy();
        stopplayer();
    }
    public void onItemClick(AdapterView<?> l, View v, int position, long id) {
        Log.v(LOG_TAG, "You clicked Item: " + id + " at position:" + position);
        // Then you start a new Activity via Intent
        Button btn = findViewById(R.id.buttonPlay);
        player.play(safeLongToInt(id),btn);
        player.setSongDisplay(songDisplay);
    }
    //My Code
    //Globals
    private playerService player;

    ArrayList<File> daSongs;

    String LOG_TAG = "main";

    View.OnClickListener playbutton_click;
    View.OnClickListener prevbutton_click;
    View.OnClickListener nexbutton_click;
    View.OnClickListener shufbutton_click;

    List<String> validExtensions = new ArrayList<String>();

    public void startplayer()
    {
        doBindService();
    }
    public void stopplayer()
    {
        if (player != null) doUnbindService();
    }

    public void initPlayer()
    {
        File folder = new File("/storage/emulated/0/");
        Log.v(LOG_TAG,"Searching in Directory: " + folder.getAbsolutePath());
        daSongs = getPlayList(folder.getAbsolutePath());
        if (daSongs != null && daSongs.size() > 0)
        {
            Log.v(LOG_TAG,"Player Init.");
            lv = (ListView) findViewById(R.id.listView1);
            ArrayList<String> sngNames = getNames(daSongs);
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, sngNames );
            lv.setAdapter(arrayAdapter);
            player.init(daSongs.toArray(new File[daSongs.size()]),daSongs.size() - 1);
            player.setSongDisplay(songDisplay);
        }
        else
        {
            Log.v(LOG_TAG,"No Songs Found.");
            //System.exit(1);
        }
    }

    private void setExtensions()
    {
        validExtensions.add(".mp3");
        validExtensions.add(".avi");
    }

    ArrayList<File> getPlayList(String rootPath) {
        ArrayList<File> fileList = new ArrayList<>();

        try {
            File rootFolder = new File(rootPath);
            File[] files = rootFolder.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    if (getPlayList(file.getAbsolutePath()) != null) {
                        fileList.addAll(getPlayList(file.getAbsolutePath()));
                    } else {
                        break;
                    }
                } else if (validExtensions.contains(getFileExtension(file))) {
                    fileList.add(file);
                    Log.v(LOG_TAG,"Found File: " + file.getName());
                }
            }
            return fileList;
        } catch (Exception e) {
            return null;
        }
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

    private ArrayList<String> getNames(ArrayList<File> files)
    {
        ArrayList<String> rtrn = new ArrayList<String>(files.size());
        for (int i = 0; i < files.size();i++)
        {
            rtrn.add(files.get(i).getName());
        }
        return rtrn;
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            player = ((playerService.LocalBinder)service).getService();
            initPlayer();
        }

        public void onServiceDisconnected(ComponentName className) {
            player = null;
        }
    };

    void doBindService()
    {
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
    public static int safeLongToInt(long l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException
                    (l + " cannot be cast to int without changing its value.");
        }
        return (int) l;
    }
}
