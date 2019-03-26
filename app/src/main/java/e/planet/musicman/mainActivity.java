package e.planet.musicman;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
        Log.v(LOG_TAG, "onCreate Called");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainlayout);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.v(LOG_TAG, "REQUESTING PERMISSION");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_CONTACTS);
        } else {
            Log.v(LOG_TAG, "PERMISSION ALREADY GRANTED");
            startplayer();
            setExtensions();
            registerReceiver();
            setListeners();
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
        Button btn = findViewById(R.id.buttonPlay);
        if (player != null) {
            if (player.play(safeLongToInt(id)))
                btn.getBackground().setColorFilter(getResources().getColor(R.color.green), PorterDuff.Mode.MULTIPLY);
            else
                btn.getBackground().setColorFilter(getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
            updateSongDisplay();
            updateDigits(player.player.getDuration(), player.player.getCurrentPosition());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.v(LOG_TAG, "PERM GRANTED");
                startplayer();
                setExtensions();
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

    ArrayList<File> daSongs = new ArrayList<>();

    String LOG_TAG = "main";

    View.OnClickListener playbutton_click;
    View.OnClickListener prevbutton_click;
    View.OnClickListener nexbutton_click;
    View.OnClickListener shufbutton_click;

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

    public void initPlayer() {
        for (String str : searchPaths) {
            Log.v(LOG_TAG, "Searching in Directory: " + str);
            if (getPlayList(str) != null)
                daSongs.addAll(getPlayList(str));
        }
        Log.v(LOG_TAG, "Found " + daSongs.size() + "Songs.");
        if (daSongs != null && daSongs.size() > 0) {
            Log.v(LOG_TAG, "Player Init.");
            lv = (ListView) findViewById(R.id.listView1);
            ArrayList<String> sngNames = getNames(daSongs);
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.song_listitem, sngNames);
            lv.setAdapter(arrayAdapter);
            player.init(daSongs.toArray(new File[daSongs.size()]), daSongs.size());
            updateSongDisplay();

        } else {
            Log.v(LOG_TAG, "No Songs Found.");
            //System.exit(1);
        }
    }

    public void updateDigits(int dur, int pos) {
        Log.v(LOG_TAG, "UPDATE UI, DUR: " + dur + " POS: " + pos + " ISPLAYING: " + player.player.isPlaying());
        final ProgressBar pb = findViewById(R.id.songDurBar);
        final TextView tv = findViewById(R.id.digitDisp);
        if (animator != null)
            animator.cancel();
        animator = ValueAnimator.ofInt(0, dur);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                //Log.v(LOG_TAG,"Animated Value: " + animation.getAnimatedValue());
                double proc = 0;
                double dur = animation.getDuration();
                double pos = (Integer) animation.getAnimatedValue();
                int minutesT = ((int) dur / 1000) / 60;
                int secondsT = ((int) dur / 1000) % 60;
                int minutesP = ((int) pos / 1000) / 60;
                int secondsP = ((int) pos / 1000) % 60;
                String dspt = leftpadZero(minutesP) + ":" + leftpadZero(secondsP) + " - " + leftpadZero(minutesT) + ":" + leftpadZero(secondsT);
                if (pos > 0)
                    proc = (pos / dur) * 100;
                //Log.v(LOG_TAG,"Setting Value: " + proc + " Dur: " + dur + " Pos: " + pos);
                if (player != null && player.player != null) {
                    if (player.player.isPlaying()) {
                        pb.setProgress(safeDoubleToInt(proc));
                        tv.setText(dspt);
                    }
                }
            }
        });

        animator.setDuration(dur);
        animator.setCurrentPlayTime(pos);
        animator.start();
        animator.setInterpolator(new LinearInterpolator());
        updateSongDisplay();
    }

    public void updateSongDisplay() {
        TextView txt = findViewById(R.id.songDisplay);
        String txts = "";
        if (player != null)
            txts = player.getSongName();
        if (txts.length() > 39) {
            txts = txts.substring(0, 36);
            txts += "...";
        }
        txt.setText(txts);
    }

    public void setListeners() {
        playbutton_click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button btn = findViewById(R.id.buttonPlay);
                if (player != null && player.player != null) {
                    if (player.pauseResume())
                        btn.getBackground().setColorFilter(getResources().getColor(R.color.green), PorterDuff.Mode.MULTIPLY);
                    else
                        btn.getBackground().setColorFilter(getResources().getColor(R.color.colorBtns), PorterDuff.Mode.MULTIPLY);
                    updateDigits(player.player.getDuration(), player.player.getCurrentPosition());
                    if (!player.player.isPlaying())
                        animator.cancel();
                    Log.v(LOG_TAG, "AnimVal: " + animator.getAnimatedValue());
                }
            }
        };
        prevbutton_click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (player != null && player.player != null) {
                    player.previous();
                    updateSongDisplay();
                    updateDigits(player.player.getDuration(), player.player.getCurrentPosition());
                    Button btn = findViewById(R.id.buttonPlay);
                    btn.getBackground().setColorFilter(getResources().getColor(R.color.green), PorterDuff.Mode.MULTIPLY);
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
                    Button btn = findViewById(R.id.buttonPlay);
                    btn.getBackground().setColorFilter(getResources().getColor(R.color.green), PorterDuff.Mode.MULTIPLY);
                }
            }
        };
        shufbutton_click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button btn = findViewById(R.id.buttonShuff);
                if (player != null && player.player != null) {
                    if (player.enableShuffle()) {
                        btn.getBackground().setColorFilter(getResources().getColor(R.color.green), PorterDuff.Mode.MULTIPLY);
                    } else {
                        btn.getBackground().setColorFilter(getResources().getColor(R.color.colorBtns), PorterDuff.Mode.MULTIPLY);
                    }
                }
            }
        };
        Button playbtn = findViewById(R.id.buttonPlay);
        Button prevbtn = findViewById(R.id.buttonPrev);
        Button nexbtn = findViewById(R.id.buttonNex);
        Button shufbtn = findViewById(R.id.buttonShuff);
        playbtn.setOnClickListener(playbutton_click);
        playbtn.getBackground().setColorFilter(getResources().getColor(R.color.colorBtns), PorterDuff.Mode.MULTIPLY);
        prevbtn.setOnClickListener(prevbutton_click);
        nexbtn.setOnClickListener(nexbutton_click);
        shufbtn.setOnClickListener(shufbutton_click);
        shufbtn.getBackground().setColorFilter(getResources().getColor(R.color.colorBtns), PorterDuff.Mode.MULTIPLY);

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
        Log.v(LOG_TAG, "GET PLAYLIST DIRECTORY: " + rootPath);
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
                    Log.v(LOG_TAG, "Found File: " + file.getAbsolutePath());
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

    private ArrayList<String> getNames(ArrayList<File> files) {
        ArrayList<String> rtrn = new ArrayList<String>(files.size());
        for (int i = 0; i < files.size(); i++) {
            rtrn.add(files.get(i).getName());
        }
        return rtrn;
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            player = ((playerService.LocalBinder) service).getService();
            initPlayer();
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
                } else if (intent.getAction().equals(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
                    Log.v(LOG_TAG, "ACTION_AUDIO_BECOMING_NOISY Received.");
                    Button btn = findViewById(R.id.buttonPlay);
                    if (player != null) {
                        if (player.pauseResume())
                            btn.getBackground().setColorFilter(getResources().getColor(R.color.green), PorterDuff.Mode.MULTIPLY);
                        else
                            btn.getBackground().setColorFilter(getResources().getColor(R.color.colorBtns), PorterDuff.Mode.MULTIPLY);
                        updateDigits(player.player.getDuration(), player.player.getCurrentPosition());
                    }
                }
            }
        };
        IntentFilter flt = new IntentFilter();
        flt.addAction("com.musicman.NEWSONG");
        flt.addAction(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(brcv, flt);
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
