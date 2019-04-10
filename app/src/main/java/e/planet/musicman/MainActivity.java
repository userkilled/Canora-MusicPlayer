package e.planet.musicman;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.*;
import android.view.animation.LinearInterpolator;
import android.widget.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    //Callbacks
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        globT.start();
        PerformanceTimer pt = new PerformanceTimer();
        Log.v(LOG_TAG, "onCreate Called");
        setContentView(R.layout.mainlayout);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.v(LOG_TAG, "REQUESTING PERMISSION");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_CONTACTS);
        } else {
            Log.v(LOG_TAG, "PERMISSION ALREADY GRANTED");
            pt.start();
            startplayer();
            pt.printStep(LOG_TAG, "STARTPLAYER");
            setExtensions();
            pt.printStep(LOG_TAG, "SETEXTENSIONS");
            loadFiles();
            pt.printStep(LOG_TAG, "LOADFILES");
            registerReceiver();
            pt.printStep(LOG_TAG, "REGISTERRECEIVER");
            setListeners();
            pt.printStep(LOG_TAG, "SETLISTENERS");
            pt.stop();
            getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_USE_LOGO);
            getSupportActionBar().setIcon(R.drawable.mainicon);
            getSupportActionBar().setLogo(R.drawable.mainicon);
            pt.printStep(LOG_TAG, "onCreate");
            ActionBar actionbar = getSupportActionBar();
            //String t = "<font color='#c800ff'>ActionBarTitle </font>";
            String hexColor = "#" + Integer.toHexString(ContextCompat.getColor(this, R.color.colorAccent) & 0x00ffffff); //Because ANDROID
            String t = "<font color='" + hexColor + "'>MusicMan </font>";
            //Log.v(LOG_TAG,"COLOR:" + t + ":");
            actionbar.setTitle(Html.fromHtml(t));
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
        super.onStart();
        Log.v(LOG_TAG, "onStart Called.");
        if (player != null && player.player != null) {
            handleProgressAnimation(player.player.getDuration(), player.player.getCurrentPosition());
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
        //TODO: Reload Song Cache on Resume
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
                Intent myIntent = new Intent(MainActivity.this, SettingsActivity.class);
                MainActivity.this.startActivity(myIntent);
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
                setPlayButton(btn, true);
            else
                setPlayButton(btn, false);
            updateSongDisplay();
            handleProgressAnimation(player.player.getDuration(), player.player.getCurrentPosition());
            //createNotification();
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
    private MusicPlayerService player;

    private BroadcastReceiver brcv;

    NotificationManagerCompat notificationManager;

    PerformanceTimer globT = new PerformanceTimer();

    //The Only Reference to The SongFiles along with another one in the Player Service
    List<SongItem> songItemList = new ArrayList<>();

    String LOG_TAG = "main";

    View.OnClickListener playbutton_click;
    View.OnClickListener prevbutton_click;
    View.OnClickListener nexbutton_click;
    View.OnClickListener shufbutton_click;
    View.OnClickListener repbutton_click;

    ValueAnimator animator;

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
            if (songItemList == null) {
                try {
                    wait(100);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (songItemList.size() > 0) {
                Log.v(LOG_TAG, "Player Init.");
                setListAdapter();
                if (songItemList != null) {
                    player.init(songItemList, songItemList.size());
                }
                p.printStep(LOG_TAG, "Player Initialization");
                updateSongDisplay();
                p.printStep(LOG_TAG, "updateSongDisplay");
                break;
            }
        }
        if (i == 10)
            Log.v(LOG_TAG, "initPlayer Failed to Load Files");
    }

    public void loadFiles() {
        //Main Entry Point after Permission is Granted
        /* This Function Makes the Initial Search for Music Files, No Further Search is Performed after this Function */

        PerformanceTimer pt = new PerformanceTimer();
        pt.startAverage();
        for (String str : searchPaths) {
            Log.v(LOG_TAG, "Searching in Directory: " + str);
            if (getPlayList(str) != null)
                songItemList.addAll(getPlayListAsItems(str));
            pt.stepAverage();
        }
        pt.printAverage(LOG_TAG, "File Loop to Fetch Files from Directory");
        Log.v(LOG_TAG, "Found " + songItemList.size() + "Songs.");
        Log.v(LOG_TAG, "Sorting Files.");
        pt.start();
        songItemList = sortSongsByTitle(songItemList);
        pt.printStep(LOG_TAG, "sortFilesByName");
        Log.v(LOG_TAG, "Files Sorted.");
    }

    public void handleProgressAnimation(int dur, int pos) {
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
                double dur = animation.getDuration();
                double pos = animation.getCurrentPlayTime();
                //Log.v(LOG_TAG,"ANIMUPDATE, DUR: " + dur + " POS: " + pos);
                int minutesT = ((int) dur / 1000) / 60;
                int secondsT = ((int) dur / 1000) % 60;
                int minutesP = ((int) pos / 1000) / 60;
                int secondsP = ((int) pos / 1000) % 60;
                String dspt = leftpadZero(minutesP) + ":" + leftpadZero(secondsP) + " - " + leftpadZero(minutesT) + ":" + leftpadZero(secondsT);
                //Log.v(LOG_TAG,"CALCULATING: " + pos + " / " + dur + " * " + "100");
                if (pos > 0)
                    proc = (pos / dur) * 100;
                //Log.v(LOG_TAG,"Setting Value: " + proc + " Dur: " + dur + " Pos: " + pos);
                if (player.player.isPlaying()) {
                    //Log.v(LOG_TAG,"Setting Progress: " + proc + " %");
                    //Log.v(LOG_TAG,"Percentage: " + safeDoubleToInt(proc));
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
        String text = "";
        SongItem s = player.getCurrentSong();
        if (s != null) {
            text = s.Title + " by " + s.Artist;
        }
        TextView txt = findViewById(R.id.songDisplay);
        if (text.length() > 39) {
            text = text.substring(0, 36);
            text += "...";
        }
        txt.setText(text);
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
                        setPlayButton(playbtn, true);
                    else
                        setPlayButton(playbtn, false);
                    handleProgressAnimation(player.player.getDuration(), player.player.getCurrentPosition());
                    // createNotification();
                }
            }
        };
        prevbutton_click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (player != null && player.player != null) {
                    player.previous();
                    updateSongDisplay();
                    handleProgressAnimation(player.player.getDuration(), player.player.getCurrentPosition());
                    //createNotification();
                    setPlayButton(playbtn, true);
                }
            }
        };
        nexbutton_click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (player != null && player.player != null) {
                    player.next();
                    updateSongDisplay();
                    handleProgressAnimation(player.player.getDuration(), player.player.getCurrentPosition());
                    //createNotification();
                    setPlayButton(playbtn, true);
                }
            }
        };
        shufbutton_click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImageButton btn = findViewById(R.id.buttonShuff);
                if (player != null) {
                    if (player.enableShuffle()) {

                        btn.setBackgroundColor(getResources().getColor(R.color.colorhighlight));
                    } else {
                        btn.setBackgroundColor(Color.TRANSPARENT);
                        //btn.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                    }
                }
            }
        };
        repbutton_click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImageButton btn = findViewById(R.id.buttonRep);
                if (player != null) {
                    if (player.enableRepeat()) {

                        btn.setBackgroundColor(getResources().getColor(R.color.colorhighlight));
                    } else {
                        btn.setBackgroundColor(Color.TRANSPARENT);
                        //btn.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
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
    }

    private void setListAdapter() {
        ListView lv = (ListView) findViewById(R.id.listView1);
        SongAdapter arrayAdapter = new SongAdapter(this, songItemList);
        lv.setAdapter(arrayAdapter);
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

    List<SongItem> getPlayListAsItems(String rootPath) {
        Bitmap dicon = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.icon_unsetsong);
        List<File> t = getPlayList(rootPath);
        List<SongItem> ret = new ArrayList<>();
        for (int i = 0; i < t.size(); i++) {
            SongItem n = new SongItem(this, t.get(i), dicon);
            ret.add(n);
        }
        return ret;
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
                }
            }
            return fileList;
        } catch (Exception e) {
            return null;
        }
    }

    private List<SongItem> sortSongsByTitle(List<SongItem> s) {
        List<SongItem> ret = new ArrayList<>();
        ListSorter ls = new ListSorter();
        ret = ls.sort(s, this);
        return ret;
    }

    private void registerReceiver() {
        brcv = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("com.musicman.NEWSONG")) {
                    int dur = intent.getIntExtra("dur", 0);
                    int pos = intent.getIntExtra("pos", 0);
                    Log.v(LOG_TAG, "com.musicman.NEWSONG Received: " + dur + " " + pos);
                    handleProgressAnimation(dur, pos);
                    //createNotification();
                } else if (intent.getAction().equals(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
                    Log.v(LOG_TAG, "ACTION_AUDIO_BECOMING_NOISY Received.");
                    ImageButton btn = findViewById(R.id.buttonPlay);
                    if (player != null) {
                        if (player.pauseResume())
                            setPlayButton(btn, true);
                        else
                            setPlayButton(btn, false);
                        handleProgressAnimation(player.player.getDuration(), player.player.getCurrentPosition());
                        //createNotification();
                    }
                } else if (intent.getAction().equals("com.musicman.PLAYING")) {
                    //createNotification();
                    ImageButton btn = findViewById(R.id.buttonPlay);
                    setPlayButton(btn, true);
                    handleProgressAnimation(player.player.getDuration(), player.player.getCurrentPosition());
                } else if (intent.getAction().equals("com.musicman.PAUSED")) {
                    //createNotification();
                    ImageButton btn = findViewById(R.id.buttonPlay);
                    setPlayButton(btn, false);
                    handleProgressAnimation(player.player.getDuration(), player.player.getCurrentPosition());
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

    public void setPlayButton(ImageButton btn, boolean play) {
        if (play) {
            btn.setImageResource(R.drawable.main_btnpause);
        } else {
            btn.setImageResource(R.drawable.main_btnplay);
        }
    }

    //Tools
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

    //Service Binding
    void doBindService() {
        Intent intent = new Intent(this, MusicPlayerService.class);
        startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    void doUnbindService() {
        Intent intent = new Intent(this, MusicPlayerService.class);
        unbindService(mConnection);
        stopService(intent);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            player = ((MusicPlayerService.LocalBinder) service).getService();
            initPlayer();
            //createNotification();
            globT.printStep(LOG_TAG, "Service Initialization");
            long l = globT.tdur;
            Snackbar.make(findViewById(android.R.id.content), "Initialization Time: " + l + " ms.\nFound Songs: " + songItemList.size(), Snackbar.LENGTH_LONG).show();
        }

        public void onServiceDisconnected(ComponentName className) {
            player = null;
        }
    };

    //Classes
    public class SongAdapter extends ArrayAdapter<SongItem> {

        private Context mContext;
        private List<SongItem> songList = new ArrayList<>();

        public SongAdapter(@NonNull Context context, List<SongItem> list) {
            super(context, 0, list);
            mContext = context;
            songList = list;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View listItem = convertView;
            if (listItem == null)
                listItem = LayoutInflater.from(mContext).inflate(R.layout.song_listitem, parent, false);
            TextView sn = listItem.findViewById(R.id.listsongname);
            TextView in = listItem.findViewById(R.id.listinterpret);
            ImageView iv = listItem.findViewById(R.id.imageview);
            sn.setText(songList.get(position).Title);
            in.setText(songList.get(position).Artist);
            iv.setImageBitmap(songList.get(position).icon);
            return listItem;
        }
    }
}
