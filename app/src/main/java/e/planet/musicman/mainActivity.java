package e.planet.musicman;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class mainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    //Callbacks
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        globT.start();
        PerformanceTimer pt = new PerformanceTimer();
        Log.v(LOG_TAG, "onCreate Called");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainlayout);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.v(LOG_TAG, "REQUESTING PERMISSION");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_CONTACTS);
        } else {
            Log.v(LOG_TAG, "PERMISSION ALREADY GRANTED");
            pt.start();
            startplayer();
            pt.printStep(LOG_TAG,"STARTPLAYER");
            setExtensions();
            pt.printStep(LOG_TAG,"SETEXTENSIONS");
            loadFiles();
            pt.printStep(LOG_TAG,"LOADFILES");
            registerReceiver();
            pt.printStep(LOG_TAG,"REGISTERRECEIVER");
            setListeners();
            pt.printStep(LOG_TAG,"SETLISTENERS");
            pt.stop();
            getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_USE_LOGO);
            getSupportActionBar().setIcon(R.drawable.ic_launcher);
            pt.printStep(LOG_TAG,"onCreate");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.v(LOG_TAG, "ONDESTROY CALLED");
        stopplayer();
        if (brcv != null) {
            unregisterReceiver(brcv);
        }
        if (notificationManager != null)
            notificationManager.cancelAll();
    }

    @Override
    protected void onStart() {
        //NOTE: Gets called Multiple Times
        super.onStart();
        Log.v(LOG_TAG, "onStart Called.");
        if (player != null && player.player != null) {
            updateDigits(player.player.getDuration(), player.player.getCurrentPosition());
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.v(LOG_TAG, "ONSTOP CALLED");
        if (animator != null) {
            animator.cancel();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //TODO: Add New Files on Resume
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                // User chose the "Settings" item, show the app settings UI...
                Log.v(LOG_TAG, "Settings Pressed.");
                Intent myIntent = new Intent(mainActivity.this, settingsActivity.class);
                mainActivity.this.startActivity(myIntent);
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    public void onItemClick(AdapterView<?> l, View v, int position, long id) {
        Log.v(LOG_TAG, "You clicked Item: " + id + " at position:" + position);
        // Then you start a new Activity via Intent
        ImageButton btn = findViewById(R.id.buttonPlay);
        if (player != null) {
            if (player.play(safeLongToInt(id)))
                setPlayButton(btn,true);
            else
                setPlayButton(btn,false);
            updateSongDisplay();
            updateDigits(player.player.getDuration(), player.player.getCurrentPosition());
            createNotification();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.v(LOG_TAG, "PERM GRANTED");
                setExtensions();
                loadFiles();
                startplayer();
                registerReceiver();
                setListeners();
            } else {
                Log.v(LOG_TAG, "PERM DENIED");
                System.exit(1);
            }
            return;
        }
    }

    //My Code
    //Globals
    private playerService player;

    private ListView lv;
    private TextView songDisplay;

    private BroadcastReceiver brcv;

    NotificationManagerCompat notificationManager;

    PerformanceTimer globT = new PerformanceTimer();

    ArrayList<File> daSongs = new ArrayList<>();
    ArrayList<String> titles;

    String LOG_TAG = "main";

    View.OnClickListener playbutton_click;
    View.OnClickListener prevbutton_click;
    View.OnClickListener nexbutton_click;
    View.OnClickListener shufbutton_click;
    View.OnClickListener repbutton_click;

    ValueAnimator animator;
    int idur = 0;
    int ipos = 0;

    int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 42;

    List<String> validExtensions = new ArrayList<String>();
    List<String> searchPaths = new ArrayList<>();

    public void startplayer() {
        doBindService();
    }

    public void stopplayer() {
        if (player != null) doUnbindService();
    }

    //Callback when Player Service is Ready
    public void initPlayer() {
        PerformanceTimer p = new PerformanceTimer();
        p.start();
        int i = 0;
        while (i < 10) {
            i++;
            if (daSongs == null)
            {
                try{
                    wait(100);
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
            }
            else if (daSongs.size() > 0) {
                Log.v(LOG_TAG, "Player Init.");
                lv = (ListView) findViewById(R.id.listView1);
                ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.song_listitem, titles);
                lv.setAdapter(arrayAdapter);
                p.printStep(LOG_TAG,"Setting Listview adapter");
                if (daSongs != null) {
                    player.init(daSongs.toArray(new File[daSongs.size()]), daSongs.size());
                }
                p.printStep(LOG_TAG,"Player Initialization");
                updateSongDisplay();
                p.printStep(LOG_TAG,"updateSongDisplay");
                break;
            }
        }
        if (i == 10)
            Log.v(LOG_TAG,"initPlayer Failed to Load Files");
    }
    public void loadFiles()
    {
        PerformanceTimer pt = new PerformanceTimer();
        pt.startAverage();
        for (String str : searchPaths) {
            Log.v(LOG_TAG, "Searching in Directory: " + str);
            if (getPlayList(str) != null)
                daSongs.addAll(getPlayList(str));
            pt.stepAverage();
        }
        pt.printAverage(LOG_TAG,"File Loop to Fetch Files from Directory");
        Log.v(LOG_TAG, "Found " + daSongs.size() + "Songs.");
        Log.v(LOG_TAG,"Sorting Files.");
        pt.start();
        daSongs = sortFilesByName(daSongs);
        pt.printStep(LOG_TAG,"sortFilesByName");
        Log.v(LOG_TAG,"Files Sorted.");
    }

    public void updateDigits(int dur, int pos) {
        Log.v(LOG_TAG, "UPDATE UI, DUR: " + dur + " POS: " + pos + " ISPLAYING: " + player.player.isPlaying());
        final ProgressBar pb = findViewById(R.id.songDurBar);
        final TextView tv = findViewById(R.id.digitDisp);
        if (animator != null)
            animator.cancel();
        animator = ValueAnimator.ofInt(0, dur);
        animator.setDuration(dur);
        animator.setCurrentPlayTime(pos);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                //Log.v(LOG_TAG,"Animated Value: " + animation.getAnimatedValue());
                if (player == null || player.player == null)
                    return;
                double proc = 0;
                double dur = player.player.getDuration();
                double pos = player.player.getCurrentPosition();
                int minutesT = ((int) dur / 1000) / 60;
                int secondsT = ((int) dur / 1000) % 60;
                int minutesP = ((int) pos / 1000) / 60;
                int secondsP = ((int) pos / 1000) % 60;
                String dspt = leftpadZero(minutesP) + ":" + leftpadZero(secondsP) + "-" + leftpadZero(minutesT) + ":" + leftpadZero(secondsT);
                //Log.v(LOG_TAG,"CALCULATING: " + pos + " / " + dur + " * " + "100");
                if (pos > 0)
                    proc = (pos / dur) * 100;
                //Log.v(LOG_TAG,"Setting Value: " + proc + " Dur: " + dur + " Pos: " + pos);
                    if (player.player.isPlaying()) {
                        //Log.v(LOG_TAG,"Setting Progress: " + proc + " %");
                        pb.setProgress(safeDoubleToInt(proc));
                        tv.setText(dspt);
                    }
            }
        });
        if (player.getPlayerStatus())
            animator.start();
        updateSongDisplay();
    }

    public void updateSongDisplay() {
        TextView txt = findViewById(R.id.songDisplay);
        String txts = "";
        if (player != null)
            txts = player.getSongDisplay();
        if (txts.length() > 39) {
            txts = txts.substring(0, 36);
            txts += "...";
        }
        txt.setText(txts);
    }

    public void createNotification()
    {
        if (notificationManager == null)
            notificationManager = NotificationManagerCompat.from(this);
        else
            notificationManager.cancelAll();
        String txt = "";
        if (player != null)
            txt = player.getSongDisplay();
        Log.v(LOG_TAG,"Creating Notification");
        Intent intent = new Intent(this, mainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        Intent play = new Intent("com.musicman.PLAYPAUSE");
        PendingIntent pi = PendingIntent.getBroadcast(this,0,play,0);

        Intent nex = new Intent("com.musicman.NEXT");
        PendingIntent nexpi = PendingIntent.getBroadcast(this,0,nex,0);

        Intent prev = new Intent("com.musicman.PREV");
        PendingIntent prevpi = PendingIntent.getBroadcast(this,0,prev,0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "42")
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Currently Playing:")
                .setContentText(txt)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0,1,2)
                        .setMediaSession(null))
                .addAction(R.drawable.notification_btnprev,"Prev",prevpi);
        if (player != null && player.player != null && player.player.isPlaying())
            builder.addAction(R.drawable.notification_btnpause,"Pause",pi);
        else
                builder.addAction(R.drawable.notification_btnplay,"Play/Pause",pi);
        builder.addAction(R.drawable.notification_btnnext,"Next",nexpi);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.v(LOG_TAG,"CREATING MANAGER");
            CharSequence name = "MusicMan Control Channel";
            String description = "Description";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel("42", name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
        else {
            Log.v(LOG_TAG, "BELOW 25");
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            notificationManager.notify(42, builder.build());
        }
        notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(42, builder.build());
    }

    public void setListeners() {
        final ImageButton playbtn = findViewById(R.id.buttonPlay);
        ImageButton prevbtn = findViewById(R.id.buttonPrev);
        ImageButton nexbtn = findViewById(R.id.buttonNex);
        ImageButton shufbtn = findViewById(R.id.buttonShuff);
        ImageButton repbtn = findViewById(R.id.buttonRep);
        playbutton_click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (player != null && player.player != null) {
                    if (player.pauseResume())
                        setPlayButton(playbtn,true);
                    else
                        setPlayButton(playbtn,false);
                    updateDigits(player.player.getDuration(), player.player.getCurrentPosition());
                }
                createNotification();
            }
        };
        prevbutton_click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (player != null && player.player != null) {
                    player.previous();
                    updateSongDisplay();
                    updateDigits(player.player.getDuration(), player.player.getCurrentPosition());
                    createNotification();
                    setPlayButton(playbtn,true);
                }
            }
        };
        nexbutton_click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (player != null && player.player != null) {
                    player.next();
                    updateSongDisplay();
                    updateDigits(player.player.getDuration(), player.player.getCurrentPosition());
                    createNotification();
                    setPlayButton(playbtn,true);
                }
            }
        };
        shufbutton_click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImageButton btn = findViewById(R.id.buttonShuff);
                if (player != null && player.player != null) {
                    if (player.enableShuffle()) {

                        btn.setBackgroundColor(getResources().getColor(R.color.green));
                    } else {
                        btn.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                    }
                }
            }
        };
        repbutton_click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImageButton btn = findViewById(R.id.buttonRep);
                if (player != null && player.player != null) {
                    if (player.enableRepeat()) {

                        btn.setBackgroundColor(getResources().getColor(R.color.green));
                    } else {
                        btn.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                    }
                }
            }
        };
        playbtn.setOnClickListener(playbutton_click);
        prevbtn.setOnClickListener(prevbutton_click);
        nexbtn.setOnClickListener(nexbutton_click);
        shufbtn.setOnClickListener(shufbutton_click);
        repbtn.setOnClickListener(repbutton_click);

        ListView listview = (ListView) findViewById(R.id.listView1);
        listview.setOnItemClickListener(this);

        songDisplay = findViewById(R.id.songDisplay);
    }

    private void setExtensions() {
        validExtensions.add(".mp3");
        validExtensions.add(".mp4");
        validExtensions.add(".m4a");
        validExtensions.add(".avi");
        validExtensions.add(".aac");
        validExtensions.add(".mkv");
        validExtensions.add(".wav");
        searchPaths.add("/storage/emulated/0/Music");
        searchPaths.add("/storage/emulated/0/Download");
    }

    ArrayList<File> getPlayList(String rootPath) {
        //Log.v(LOG_TAG, "GET PLAYLIST DIRECTORY: " + rootPath);
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
                    //Log.v(LOG_TAG, "Found File: " + file.getAbsolutePath());
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

    private ArrayList<String> getListDisplay(ArrayList<File> files) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        ArrayList<String> rtrn = new ArrayList<String>(files.size());

        for (int i = 0; i < files.size(); i++) {
            mmr.setDataSource(files.get(i).getAbsolutePath());
            if (mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) != null && mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) != null)
            {
                rtrn.add(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) + " by " + mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));
            }
            else
            {
                rtrn.add(files.get(i).getName());
            }
        }

        return rtrn;
    }

    private ArrayList<File> sortFilesByName(ArrayList<File> in)
    {
        Log.v(LOG_TAG,"SORTING FILES");
        ListSorter ls = new ListSorter();
        ArrayList<File> f = ls.sort(in,Constants.SORTBYTITLE,getApplicationContext());
        titles = ls.titles;
        return f;
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            player = ((playerService.LocalBinder) service).getService();
            initPlayer();
            createNotification();
            globT.printStep(LOG_TAG,"Service Initialization");
            long l = globT.tdur;
            Snackbar.make(findViewById(android.R.id.content),"Initialization Time: " + l + " ms.\nFound Songs: " + daSongs.size(),Snackbar.LENGTH_LONG).show();
        }

        public void onServiceDisconnected(ComponentName className) {
            player = null;
        }
    };

    void doBindService() {
        Intent intent = new Intent(this, playerService.class);
        startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    void doUnbindService() {
        Intent intent = new Intent(this, playerService.class);
        unbindService(mConnection);
        stopService(intent);
    }

    private void registerReceiver() {
        brcv = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("com.musicman.NEWSONG")) {
                    int dur = intent.getIntExtra("dur", 0);
                    int pos = intent.getIntExtra("pos", 0);
                    Log.v(LOG_TAG, "com.musicman.NEWSONG Received: " + dur + " " + pos);
                    updateDigits(dur, pos);
                    createNotification();
                } else if (intent.getAction().equals(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
                    Log.v(LOG_TAG, "ACTION_AUDIO_BECOMING_NOISY Received.");
                    ImageButton btn = findViewById(R.id.buttonPlay);
                    if (player != null) {
                        if (player.pauseResume())
                            setPlayButton(btn,true);
                        else
                            setPlayButton(btn,false);
                        updateDigits(player.player.getDuration(), player.player.getCurrentPosition());
                        createNotification();
                    }
                }
                else if (intent.getAction().equals("com.musicman.PLAYING"))
                {
                    createNotification();
                    ImageButton btn = findViewById(R.id.buttonPlay);
                    setPlayButton(btn,true);
                    updateDigits(player.player.getDuration(),player.player.getCurrentPosition());
                }
                else if (intent.getAction().equals("com.musicman.PAUSED"))
                {
                    createNotification();
                    ImageButton btn = findViewById(R.id.buttonPlay);
                    setPlayButton(btn,false);
                    updateDigits(player.player.getDuration(),player.player.getCurrentPosition());
                }
            }
        };
        IntentFilter flt = new IntentFilter();
        flt.addAction("com.musicman.NEWSONG");
        flt.addAction("com.musicman.PLAYING");
        flt.addAction("com.musicman.PAUSED");
        flt.addAction(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(brcv, flt);
    }

    public void setPlayButton(ImageButton btn ,boolean play)
    {
        if (play)
        {
            btn.setImageResource(R.drawable.main_btnpause);
        }
        else
        {
            btn.setImageResource(R.drawable.main_btnplay);
        }
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

    public String leftpadZero(int val) {
        if ((val - 10) < 0) {
            String rt = "0" + val;
            return rt;
        } else {
            String rt = "" + val;
            return rt;
        }
    }
}
