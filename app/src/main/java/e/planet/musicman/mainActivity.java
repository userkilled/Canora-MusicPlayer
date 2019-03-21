package e.planet.musicman;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.renderscript.Sampler;
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

    private BroadcastReceiver brcv;

    private void registerReceiver() {
        brcv = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int dur = intent.getIntExtra("dur",0);
                int pos = intent.getIntExtra("pos",0);
                Log.v(LOG_TAG,"CUSTOM BROADCAST RECEIVER CALLED Received: " + dur + " " + pos);
                updateUi(dur,pos);
            }
        };
        registerReceiver(brcv, new IntentFilter("com.musicman.NEWSONG"));
    }

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
                if (player != null) {
                    player.pauseResume(btn);
                    updateUi(player.player.getDuration(), player.player.getCurrentPosition());
                }
            }
        };
        prevbutton_click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (player != null) {
                    player.previous();
                    player.setSongDisplay(songDisplay);
                    updateUi(player.player.getDuration(), player.player.getCurrentPosition());
                }
            }
        };
        nexbutton_click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (player != null) {
                    player.next();
                    player.setSongDisplay(songDisplay);
                    updateUi(player.player.getDuration(), player.player.getCurrentPosition());
                }
            }
        };
        shufbutton_click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button btn = findViewById(R.id.buttonShuff);
                if (player != null)
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

        registerReceiver();
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
        Log.v(LOG_TAG,"ONSTOP CALLED");
    }
    protected void onRestart()
    {
        super.onRestart();
    }
    protected void onDestroy()
    {
        super.onDestroy();
        Log.v(LOG_TAG,"ONDESTROY CALLED");
        animator.cancel();
        stopplayer();
        if(brcv != null) {
            unregisterReceiver(brcv);
        }
    }
    public void onItemClick(AdapterView<?> l, View v, int position, long id) {
        Log.v(LOG_TAG, "You clicked Item: " + id + " at position:" + position);
        // Then you start a new Activity via Intent
        Button btn = findViewById(R.id.buttonPlay);
        if (player != null) {
            player.play(safeLongToInt(id), btn);
            player.setSongDisplay(songDisplay);
            updateUi(player.player.getDuration(), player.player.getCurrentPosition());
        }
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

    ValueAnimator animator;
    long timepassed;

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
            player.init(daSongs.toArray(new File[daSongs.size()]),daSongs.size());
            player.setSongDisplay(songDisplay);

        }
        else
        {
            Log.v(LOG_TAG,"No Songs Found.");
            //System.exit(1);
        }
    }
    public void updateUi(int dur, int pos)
    {
        Log.v(LOG_TAG,"UPDATE UI, DUR: " + dur + " POS: " + pos + " ISPLAYING: " + player.player.isPlaying());
        final ProgressBar pb = findViewById(R.id.songDurBar);
        animator = ValueAnimator.ofInt(0,dur);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                    Log.v(LOG_TAG,"Animated Value: " + animation.getAnimatedValue());
                    double proc = 0;
                    double dur = animation.getDuration();
                    double pos = (Integer)animation.getAnimatedValue();
                    if (pos > 0)
                        proc = (pos / dur) * 100;
                    Log.v(LOG_TAG,"Setting Value: " + proc + " Dur: " + dur + " Pos: " + pos);
                    if (player != null && player.player != null) {
                        if (player.player.isPlaying())
                            pb.setProgress(safeDoubleToInt(proc));
                    }
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
            }
        });

        animator.setDuration(dur);
        animator.setCurrentPlayTime(pos);
        animator.start();
        player.setSongDisplay(songDisplay);
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
                    Log.v(LOG_TAG,"Found File: " + file.getAbsolutePath());
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
    public static int safeDoubleToInt(double l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException
                    (l + " cannot be cast to int without changing its value.");
        }
        return (int) l;
    }
}
