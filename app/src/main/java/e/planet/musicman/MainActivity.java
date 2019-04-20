package e.planet.musicman;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
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
        Log.v(LOG_TAG, "ONCREATE CALLED");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_main);

        globT.start();

        sortBy = Constants.SORT_BYTITLE;//TODO SETTING

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.v(LOG_TAG, "REQUESTING PERMISSION");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_CONTACTS);
        } else {
            Log.v(LOG_TAG, "PERMISSION ALREADY GRANTED");
            startplayer();
            setExtensionsAndSearchPaths();
            new LoadFilesTask().execute(this);
            registerReceiver();
            setListeners();
            getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_USE_LOGO);
            getSupportActionBar().setIcon(R.drawable.mainicon);
            getSupportActionBar().setLogo(R.drawable.mainicon);
            ActionBar actionbar = getSupportActionBar();
            String hexColor = "#" + Integer.toHexString(ContextCompat.getColor(this, R.color.colorAccent) & 0x00ffffff); //Because ANDROID
            String t = "<font color='" + hexColor + "'>MusicMan </font>";
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
        Log.v(LOG_TAG, "ONSTART CALLED");
        if (serv != null && serv.player != null) {
            handleProgressAnimation(serv.player.getDuration(), serv.player.getCurrentPosition());
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
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.v(LOG_TAG, "ONRESTART CALLED");
        //TODO: Reload Song Cache on Resume
        new LoadFilesTask().execute(this);
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
                Log.v(LOG_TAG, "Settings Pressed");
                displayDialog(Constants.DIALOG_SETTINGS);
                return true;

            case R.id.action_sortby:
                Log.v(LOG_TAG, "SortBy Pressed");
                displayDialog(Constants.DIALOG_SORT);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> l, View v, int position, long id) {
        Log.v(LOG_TAG, "You clicked Item: " + id + " at position:" + position);
        ImageButton btn = findViewById(R.id.buttonPlay);
        if (serv != null) {
            if (serv.play(position))
                setPlayButton(btn, true);
            else
                setPlayButton(btn, false);
            updateSongDisplay();
            handleProgressAnimation(serv.player.getDuration(), serv.player.getCurrentPosition());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.v(LOG_TAG, "PERM GRANTED");
                startplayer();
                setExtensionsAndSearchPaths();
                loadFiles();
                registerReceiver();
                setListeners();
                getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_USE_LOGO);
                getSupportActionBar().setIcon(R.drawable.mainicon);
                getSupportActionBar().setLogo(R.drawable.mainicon);
                ActionBar actionbar = getSupportActionBar();
                String hexColor = "#" + Integer.toHexString(ContextCompat.getColor(this, R.color.colorAccent) & 0x00ffffff); //Because ANDROID
                String t = "<font color='" + hexColor + "'>MusicMan </font>";
                actionbar.setTitle(Html.fromHtml(t));
            } else {
                Log.v(LOG_TAG, "PERM DENIED");
                System.exit(1);
            }
            return;
        }
    }

    //Globals
    private MusicPlayerService serv;

    private BroadcastReceiver brcv;

    private SongAdapter arrayAdapter;

    NotificationManagerCompat notificationManager;

    /*The Only Reference to The SongFiles along with a Copy in The MusicPlayerService*/
    List<SongItem> songItemList = new ArrayList<>();

    int sortBy = Constants.SORT_BYTITLE; //Global Sorter Variable

    int idH = 0; //Stores Maximum ID Given out, Used for getting a new ID each Song

    PerformanceTimer globT = new PerformanceTimer();

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

    public void initPlayer() {
        /* Callback when Player Service is Ready */
        Log.v(LOG_TAG, "INIT PLAYER");
        setListAdapter();
        serv.init(songItemList);
        updateSongDisplay();
    }

    public void loadFiles() {
        /* Main Entry Point after Permission is Granted, This Function Makes the Initial Search for Music Files, No Further Search is Performed after this Function */
        Log.v(LOG_TAG, "LOADING FILES");
        List<SongItem> nl = new ArrayList<>();
        for (String str : searchPaths) {
            Log.v(LOG_TAG, "Searching in Directory: " + str);
            if (getPlayListFiles(str) != null)
                nl.addAll(getPlayListAsItems(str));
        }
        songItemList.clear();
        songItemList.addAll(nl);
        songItemList = sortSongsWrapper(songItemList);
    }

    public void handleProgressAnimation(int dur, int pos) {
        /* Creates a new ValueAnimator for the Duration Bar and the Digits, And Calls Update Song Display*/
        Log.v(LOG_TAG, "UPDATE UI, DUR: " + dur + " POS: " + pos + " ISPLAYING: " + serv.player.isPlaying());
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
                if (serv == null || serv.player == null)
                    return;
                double proc = 0;
                double dur = animation.getDuration();
                double pos = animation.getCurrentPlayTime();
                int minutesT = ((int) dur / 1000) / 60;
                int secondsT = ((int) dur / 1000) % 60;
                int minutesP = ((int) pos / 1000) / 60;
                int secondsP = ((int) pos / 1000) % 60;
                String dspt = leftpadZero(minutesP) + ":" + leftpadZero(secondsP) + " - " + leftpadZero(minutesT) + ":" + leftpadZero(secondsT);
                if (pos > 0)
                    proc = (pos / dur) * 100;
                if (serv.player.isPlaying()) {
                    pb.setProgress(safeDoubleToInt(proc));
                    tv.setText(dspt);
                }
            }
        });
        if (serv.getPlaybackStatus())
            animator.start();
        updateSongDisplay();
    }

    public void updateSongDisplay() {
        /* Set the Song Title Text */
        String text = "";
        SongItem s = serv.getCurrentSong();
        if (s != null) {
            text = s.Title + " by " + s.Artist;
        }
        TextView txt = findViewById(R.id.songDisplay);
        /* No Longer Needed 1 Line Max Set
        if (text.length() > 39) {
            text = text.substring(0, 36);
            text += "...";
        }*/
        txt.setText(text);
    }

    public void displayDialog(int m) {
        /*Toolbar Menu Item Dialoges*/
        switch (m) {
            case Constants.DIALOG_SORT:
                AlertDialog.Builder b = new AlertDialog.Builder(this);
                b.setTitle("Sort By");
                b.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Sort By Selection
                        songItemList = sortSongsWrapper(songItemList);
                        serv.reload(songItemList);
                        arrayAdapter.notifyDataSetChanged();
                    }
                });
                b.setNegativeButton("Back", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Do Nothing
                    }
                });
                CharSequence[] arr = {"Title", "Artist"};
                b.setSingleChoiceItems(arr, sortBy, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                sortBy = Constants.SORT_BYTITLE;
                                break;
                            case 1:
                                sortBy = Constants.SORT_BYARTIST;
                                break;
                        }
                    }
                });
                final AlertDialog sortdia = b.create();

                LayoutInflater l = LayoutInflater.from(this);
                View e = l.inflate(R.layout.dialog_sort, null);

                sortdia.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        sortdia.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.colorDialogText, null));
                        sortdia.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.colorDialogText, null));
                    }
                });

                sortdia.setView(e);
                sortdia.show();
                break;

            case Constants.DIALOG_SETTINGS:
                AlertDialog.Builder d = new AlertDialog.Builder(this);
                d.setTitle("Settings");
                d.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                final AlertDialog setdia = d.create();
                LayoutInflater f = LayoutInflater.from(this);
                View v = f.inflate(R.layout.dialog_settings, null);
                SeekBar pb = v.findViewById(R.id.seekBar1);
                pb.setProgress((int) (serv.getVolume() * 100));
                pb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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
                        if (serv != null)
                            serv.setVolume(c);
                    }
                });

                setdia.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        setdia.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.colorDialogText, null));
                    }
                });

                setdia.setView(v);
                setdia.show();
                break;
        }
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
                if (serv != null && serv.player != null) {
                    if (serv.pauseResume())
                        setPlayButton(playbtn, true);
                    else
                        setPlayButton(playbtn, false);
                    handleProgressAnimation(serv.player.getDuration(), serv.player.getCurrentPosition());
                }
            }
        };
        prevbutton_click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (serv != null && serv.player != null) {
                    serv.previous();
                    updateSongDisplay();
                    handleProgressAnimation(serv.player.getDuration(), serv.player.getCurrentPosition());
                    setPlayButton(playbtn, true);
                }
            }
        };
        nexbutton_click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (serv != null && serv.player != null) {
                    serv.next();
                    updateSongDisplay();
                    handleProgressAnimation(serv.player.getDuration(), serv.player.getCurrentPosition());
                    setPlayButton(playbtn, true);
                }
            }
        };
        shufbutton_click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImageButton btn = findViewById(R.id.buttonShuff);
                if (serv != null) {
                    if (serv.enableShuffle()) {

                        btn.setBackgroundColor(getResources().getColor(R.color.colorhighlight, null));
                    } else {
                        btn.setBackgroundColor(Color.TRANSPARENT);
                    }
                }
            }
        };
        repbutton_click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImageButton btn = findViewById(R.id.buttonRep);
                if (serv != null) {
                    if (serv.enableRepeat()) {

                        btn.setBackgroundColor(getResources().getColor(R.color.colorhighlight, null));
                    } else {
                        btn.setBackgroundColor(Color.TRANSPARENT);
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
        arrayAdapter = new SongAdapter(this, songItemList);
        lv.setAdapter(arrayAdapter);
    }

    private void setExtensionsAndSearchPaths() {
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
        List<File> t = getPlayListFiles(rootPath);
        List<SongItem> ret = new ArrayList<>();
        for (int i = 0; i < t.size(); i++) {
            SongItem s = new SongItem(this, t.get(i), requestSongID(), dicon);
            ret.add(s);
        }
        return ret;
    }

    ArrayList<File> getPlayListFiles(String rootPath) {
        ArrayList<File> fileList = new ArrayList<>();
        try {
            File rootFolder = new File(rootPath);
            File[] files = rootFolder.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    if (getPlayListFiles(file.getAbsolutePath()) != null) {
                        fileList.addAll(getPlayListFiles(file.getAbsolutePath()));
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

    private int requestSongID() {
        return idH++;
    }

    private List<SongItem> sortSongsWrapper(List<SongItem> s) {
        switch (sortBy) {
            case Constants.SORT_BYARTIST:
                return sortSongsByArtist(s);
            case Constants.SORT_BYTITLE:
                return sortSongsByTitle(s);
            default:
                return null;
        }
    }

    private List<SongItem> sortSongsByTitle(List<SongItem> s) {
        List<SongItem> ret = new ArrayList<>();
        ListSorter ls = new ListSorter();
        ret = ls.sort(this, s, Constants.SORT_BYTITLE);
        return ret;
    }

    private List<SongItem> sortSongsByArtist(List<SongItem> s) {
        List<SongItem> ret = new ArrayList<>();
        ListSorter ls = new ListSorter();
        ret = ls.sort(this, s, Constants.SORT_BYARTIST);
        return ret;
    }

    private void registerReceiver() {
        brcv = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case Constants.ACTION_QUIT:
                        finish();
                        break;
                    case Constants.ACTION_STATUS_NEWSONG:
                        int dur = intent.getIntExtra("dur", 0);
                        int pos = intent.getIntExtra("pos", 0);
                        handleProgressAnimation(dur, pos);
                        break;
                    case Constants.ACTION_STATUS_PLAYING:
                        ImageButton btn = findViewById(R.id.buttonPlay);
                        setPlayButton(btn, true);
                        handleProgressAnimation(serv.player.getDuration(), serv.player.getCurrentPosition());
                        break;
                    case Constants.ACTION_STATUS_PAUSED:
                        ImageButton btn1 = findViewById(R.id.buttonPlay);
                        setPlayButton(btn1, false);
                        handleProgressAnimation(serv.player.getDuration(), serv.player.getCurrentPosition());
                        break;
                }
            }
        };
        IntentFilter flt = new IntentFilter();
        flt.addAction(Constants.ACTION_STATUS_NEWSONG);
        flt.addAction(Constants.ACTION_STATUS_PLAYING);
        flt.addAction(Constants.ACTION_STATUS_PAUSED);
        flt.addAction(Constants.ACTION_QUIT);
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
    public void startplayer() {
        doBindService();
    }

    public void stopplayer() {
        if (serv != null) doUnbindService();
    }

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
            serv = ((MusicPlayerService.LocalBinder) service).getService();
            initPlayer();
            globT.printStep(LOG_TAG, "Service Initialization");
            long l = globT.tdur;
            Snackbar.make(findViewById(android.R.id.content), "Initialization Time: " + l + " ms.\nFound Songs: " + songItemList.size(), Snackbar.LENGTH_LONG).show();
        }

        public void onServiceDisconnected(ComponentName className) {
            serv = null;
        }
    };

    //ArrayAdapter of Song List Display
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
                listItem = LayoutInflater.from(mContext).inflate(R.layout.list_item_song, parent, false);
            TextView sn = listItem.findViewById(R.id.listsongname);
            TextView in = listItem.findViewById(R.id.listinterpret);
            ImageView iv = listItem.findViewById(R.id.imageview);
            sn.setText(songList.get(position).Title);
            in.setText(songList.get(position).Artist);
            iv.setImageBitmap(songList.get(position).icon);
            return listItem;
        }
    }

    //AsyncTask

    protected class LoadFilesTask extends AsyncTask<Context, Integer, String> {
        @Override
        protected String doInBackground(Context... params) {
            // Do the time comsuming task here
            loadFiles();
            Log.v(LOG_TAG,"NEW SIZE: " + songItemList.size());
            serv.reload(songItemList);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    arrayAdapter.notifyDataSetChanged();
                }
            });
            return "COMPLETE!";
        }

        // -- gets called just before thread begins
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        // -- called from the publish progress
        // -- notice that the datatype of the second param gets passed to this
        // method
        @Override
        protected void onProgressUpdate(Integer... values) {

        }

        // -- called if the cancel button is pressed
        @Override
        protected void onCancelled() {
            super.onCancelled();
        }

        // -- called as soon as doInBackground method completes
        // -- notice that the third param gets passed to this method
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            // Show the toast message here
        }
    }
}
