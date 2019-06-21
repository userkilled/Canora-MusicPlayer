package ch.swissproductions.canora.activities;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.*;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import ch.swissproductions.canora.*;
import ch.swissproductions.canora.data.Constants;
import ch.swissproductions.canora.data.data_playlist;
import ch.swissproductions.canora.data.data_song;
import ch.swissproductions.canora.managers.DataManager;
import ch.swissproductions.canora.managers.SettingsManager;
import ch.swissproductions.canora.managers.ThemeManager;
import ch.swissproductions.canora.managers.ViewPortManager;
import ch.swissproductions.canora.service.MusicPlayerService;
import ch.swissproductions.canora.tools.PerformanceTimer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    //Overrides
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //TODO: Reduce Startup Time
        Log.v(LOG_TAG, "ONCREATE CALLED");
        super.onCreate(savedInstanceState);
        globT.start();
        btnExecutor = Executors.newSingleThreadExecutor();
        pltemp = getIntent().getStringExtra(Constants.PARAMETER_PLAYLIST);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Log.v(LOG_TAG, "REQUESTING PERMISSION");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONID);
            } else {
                Log.v(LOG_TAG, "PERMISSION ALREADY GRANTED");
                sc = new SettingsManager(getApplicationContext());
                dm = new DataManager(getApplicationContext(), this, Integer.parseInt(sc.getSetting(Constants.SETTING_SORTBY)), pltemp);
                thm = new ThemeManager(sc);
                setTheme(thm.getThemeResourceID());
                setContentView(R.layout.layout_main);
                ListView lv = findViewById(R.id.mainViewport);
                registerForContextMenu(lv);
                findViewById(R.id.searchbox).setVisibility(View.GONE);
                findViewById(R.id.searchbybtn).setVisibility(View.GONE);
                findViewById(R.id.songDisplay).requestFocus();

                startplayer();
                registerReceiver();
                setListeners();
                colorControlWidgets();
            }
        } else {
            int readPerm = PermissionChecker.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            int writePerm = PermissionChecker.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (readPerm == PermissionChecker.PERMISSION_GRANTED && writePerm == PermissionChecker.PERMISSION_GRANTED) {
                Log.v(LOG_TAG, "PERMISSION ALREADY GRANTED");
                sc = new SettingsManager(getApplicationContext());
                dm = new DataManager(getApplicationContext(), this, Integer.parseInt(sc.getSetting(Constants.SETTING_SORTBY)), pltemp);
                thm = new ThemeManager(sc);
                setTheme(thm.getThemeResourceID());
                setContentView(R.layout.layout_main);
                ListView lv = findViewById(R.id.mainViewport);
                registerForContextMenu(lv);
                findViewById(R.id.searchbox).setVisibility(View.GONE);
                findViewById(R.id.searchbybtn).setVisibility(View.GONE);
                findViewById(R.id.songDisplay).requestFocus();

                startplayer();
                registerReceiver();
                setListeners();
                colorControlWidgets();
            } else {
                Log.v(LOG_TAG, "REQUESTING PERMISSION");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONID);
            }
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
        if (dm != null)
            dm.cancelTasks();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.v(LOG_TAG, "ONSTART CALLED");
        if (serv != null) {
            handleProgressAnimation(serv.getDuration(), serv.getCurrentPosition());
            updateSongDisplay();
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
        if (serv != null) {
            dm.loadContentFromMediaStore();
            dm.sortContent(sortBy);
            serv.setContent(dm.dataout);
            if (isSearching)
                vpm.showFiltered(searchTerm, searchBy);
            notifyAAandOM();
            dm.loadContentFromFiles();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Log.v(LOG_TAG, "ONCREATEOPTIONS");
        getMenuInflater().inflate(R.menu.main_menu, menu);
        menu.getItem(0).getIcon().mutate().setColorFilter(getColorFromAtt(R.attr.colorText), PorterDuff.Mode.MULTIPLY);
        return true;
    }

    @Override
    public void onBackPressed() {
        displayDialog(Constants.DIALOG_EXIT_CONFIRM);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        //Log.v(LOG_TAG, "ONPREPAREOPTIONS");
        if (vpm == null)
            return false;
        Menu m = menu;
        m.findItem(R.id.action_addTo).getSubMenu().clear();
        m.findItem(R.id.action_addTo).getSubMenu().add(0, R.id.action_playlist_create, 0, R.string.menu_options_newplaylist);
        Map<String, data_playlist> playlist = new TreeMap<>(dm.getPlayListsAsMap());
        int plc = 0;
        for (Map.Entry<String, data_playlist> entry : playlist.entrySet()) {
            //ADDTO
            SubMenu sub = m.findItem(R.id.action_addTo).getSubMenu();
            if (entry.getValue().Title.length() != 0) {
                sub.add(0, plc, 1, entry.getValue().Title);
                sub.findItem(plc).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        data_playlist t = new data_playlist(item.getTitle().toString(), getSelected());
                        multiSelect();
                        dm.updatePlayList(item.getTitle().toString(), t);
                        showToastMessage(t.audio.size() + " " + getString(R.string.misc_addedto) + " " + item.getTitle());
                        return false;
                    }
                });
            }
            plc++;
        }
        if (vpm.subMenu == Constants.DATA_SELECTOR_NONE) {
            if (dm.getSelector() == (Constants.DATA_SELECTOR_PLAYLISTS))
                m.findItem(R.id.action_playlist_edit).setVisible(true);
            else
                m.findItem(R.id.action_playlist_edit).setVisible(false);

            String tmp23 = dm.getIndex();
            if (tmp23.equals(""))
                tmp23 = getString(R.string.misc_tracks);
            getSupportActionBar().setTitle("   " + tmp23);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            Drawable ic;
            switch (dm.getSelector()) {
                case Constants.DATA_SELECTOR_PLAYLISTS:
                    ic = getDrawable(R.drawable.icon_playlists);
                    break;
                case Constants.DATA_SELECTOR_STATICPLAYLISTS_ALBUMS:
                    ic = getDrawable(R.drawable.icon_album);
                    break;
                case Constants.DATA_SELECTOR_STATICPLAYLISTS_ARTISTS:
                    ic = getDrawable(R.drawable.icon_interpret);
                    break;
                case Constants.DATA_SELECTOR_STATICPLAYLISTS_GENRES:
                    ic = getDrawable(R.drawable.icon_genre);
                    break;
                case Constants.DATA_SELECTOR_STATICPLAYLISTS_TRACKS:
                    ic = getDrawable(R.drawable.icon_tracks);
                    break;
                default:
                    ic = getDrawable(R.drawable.notification_smallicon);
            }
            ic.setColorFilter(getColorFromAtt(R.attr.colorText), PorterDuff.Mode.MULTIPLY);
            getSupportActionBar().setIcon(ic);
            Drawable d = getDrawable(R.drawable.icon_back);
            d.mutate().setColorFilter(getColorFromAtt(R.attr.colorText), PorterDuff.Mode.MULTIPLY);
            getSupportActionBar().setHomeAsUpIndicator(d);
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getColorFromAtt(R.attr.colorToolbar)));
        } else if (vpm.subMenu == (Constants.DATA_SELECTOR_STATICPLAYLISTS_ARTISTS)) {
            Log.v(LOG_TAG, "ARTISTS");
            m.findItem(R.id.action_playlist_edit).setVisible(false);
            getSupportActionBar().setDisplayShowHomeEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.misc_artists);
            Drawable d = getDrawable(R.drawable.icon_back);
            d.mutate().setColorFilter(getColorFromAtt(R.attr.colorText), PorterDuff.Mode.MULTIPLY);
            getSupportActionBar().setHomeAsUpIndicator(d);
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getColorFromAtt(R.attr.colorToolbar)));
        } else if (vpm.subMenu == (Constants.DATA_SELECTOR_STATICPLAYLISTS_ALBUMS)) {
            Log.v(LOG_TAG, "ALBUMS");
            m.findItem(R.id.action_playlist_edit).setVisible(false);
            getSupportActionBar().setDisplayShowHomeEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.misc_albums);
            Drawable d = getDrawable(R.drawable.icon_back);
            d.mutate().setColorFilter(getColorFromAtt(R.attr.colorText), PorterDuff.Mode.MULTIPLY);
            getSupportActionBar().setHomeAsUpIndicator(d);
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getColorFromAtt(R.attr.colorToolbar)));
        } else if (vpm.subMenu == (Constants.DATA_SELECTOR_STATICPLAYLISTS_GENRES)) {
            Log.v(LOG_TAG, "GENRES");
            m.findItem(R.id.action_playlist_edit).setVisible(false);
            getSupportActionBar().setDisplayShowHomeEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.misc_genres);
            Drawable d = getDrawable(R.drawable.icon_back);
            d.mutate().setColorFilter(getColorFromAtt(R.attr.colorText), PorterDuff.Mode.MULTIPLY);
            getSupportActionBar().setHomeAsUpIndicator(d);
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getColorFromAtt(R.attr.colorToolbar)));
        } else if (vpm.subMenu == (Constants.DATA_SELECTOR_PLAYLISTS)) {
            Log.v(LOG_TAG, "PLAYLISTS");
            m.findItem(R.id.action_playlist_edit).setVisible(false);
            getSupportActionBar().setDisplayShowHomeEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.misc_playlists);
            Drawable d = getDrawable(R.drawable.icon_back);
            d.mutate().setColorFilter(getColorFromAtt(R.attr.colorText), PorterDuff.Mode.MULTIPLY);
            getSupportActionBar().setHomeAsUpIndicator(d);
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getColorFromAtt(R.attr.colorToolbar)));
        }
        switch (vpm.state) {
            case Constants.ARRAYADAPT_STATE_DEFAULT:
                //Log.v(LOG_TAG, "OPTIONS NORMAL MODE");
                menu.findItem(R.id.action_addTo).setVisible(false);
                menu.findItem(R.id.action_cancel).setVisible(false);
                if (vpm.subMenu == Constants.DATA_SELECTOR_PLAYLISTS || vpm.subMenu == Constants.DATA_SELECTOR_NONE)
                    menu.findItem(R.id.action_select).setVisible(true);
                else
                    menu.findItem(R.id.action_select).setVisible(false);
                menu.findItem(R.id.action_delete).setVisible(false);
                break;
            case Constants.ARRAYADAPT_STATE_SELECT:
                //Log.v(LOG_TAG, "OPTIONS SELECT MODE");
                closeOptionsMenu();
                menu.findItem(R.id.action_cancel).setVisible(true);
                menu.findItem(R.id.action_select).setVisible(false);
                if (vpm.subMenu == Constants.DATA_SELECTOR_NONE) {
                    menu.findItem(R.id.action_addTo).setVisible(true);
                    if (dm.getSelector() == (Constants.DATA_SELECTOR_PLAYLISTS))
                        menu.findItem(R.id.action_delete).setVisible(true);
                    else
                        menu.findItem(R.id.action_delete).setVisible(false);
                } else {
                    menu.findItem(R.id.action_addTo).setVisible(false);
                    menu.findItem(R.id.action_delete).setVisible(true);
                }
                break;
        }
        if (vpm.subMenu == Constants.DATA_SELECTOR_NONE)
            menu.findItem(R.id.action_sortby).setVisible(true);
        else
            menu.findItem(R.id.action_sortby).setVisible(false);
        menu.findItem(R.id.action_addTo).getIcon().setColorFilter(getColorFromAtt(R.attr.colorText), PorterDuff.Mode.MULTIPLY);
        menu.findItem(R.id.action_search).getIcon().setColorFilter(getColorFromAtt(R.attr.colorText), PorterDuff.Mode.MULTIPLY);
        //Log.v(LOG_TAG, "EXIT OPTIONS");
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                displayDialog(Constants.DIALOG_SETTINGS);
                return true;
            case R.id.action_sortby:
                displayDialog(Constants.DIALOG_SORT);
                return true;
            case R.id.action_search:
                handleSearch();
                return true;
            case R.id.action_select:
                multiSelect();
                return true;
            case R.id.action_addTo:
                return true;
            case R.id.action_cancel:
                multiSelect();
                return true;
            case R.id.action_delete:
                if (vpm.subMenu == Constants.DATA_SELECTOR_NONE)
                    displayDialog(Constants.DIALOG_WARNING_FILE_DELETE_FROMPLAYLIST);
                else if (vpm.subMenu == Constants.DATA_SELECTOR_PLAYLISTS)
                    displayDialog(Constants.DIALOG_WARNING_PLAYLIST_DELETE_MULTIPLE);
                return true;
            case R.id.action_playlist_edit:
                displayDialog(Constants.DIALOG_PLAYLIST_EDIT);
                return true;
            case R.id.action_playlist_create:
                displayDialog(Constants.DIALOG_PLAYLIST_CREATE);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> l, View v, int position, long id) {
        if (!dm.filesfound)
            return;
        ImageButton btn = findViewById(R.id.buttonPlay);
        if (serv != null) {
            if (serv.play(dm.dataout.get(position).id) == 0)
                setPlayButton(btn, true);
            else
                setPlayButton(btn, false);
            serv.setEqualizerPreset(Integer.parseInt(sc.getSetting(Constants.SETTING_EQUALIZERPRESET)));
            updateSongDisplay();
            handleProgressAnimation(serv.getDuration(), serv.getCurrentPosition());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSIONID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                Log.v(LOG_TAG, "PERM GRANTED");
                sc = new SettingsManager(getApplicationContext());
                dm = new DataManager(getApplicationContext(), this, Integer.parseInt(sc.getSetting(Constants.SETTING_SORTBY)), pltemp);
                thm = new ThemeManager(sc);
                setTheme(thm.getThemeResourceID());
                setContentView(R.layout.layout_main);
                ListView lv = findViewById(R.id.mainViewport);
                registerForContextMenu(lv);
                findViewById(R.id.searchbox).setVisibility(View.GONE);
                findViewById(R.id.searchbybtn).setVisibility(View.GONE);
                findViewById(R.id.songDisplay).requestFocus();
                startplayer();
                registerReceiver();
                setListeners();
                colorControlWidgets();
            } else {
                Log.v(LOG_TAG, "PERM DENIED");
                System.exit(1);
            }
            return;
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        if (v.getId() == R.id.mainViewport && vpm.subMenu == Constants.DATA_SELECTOR_NONE) {
            if (!dm.filesfound)
                return;
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.list_menu, menu);
            vpm.setClicked(info.position);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.sel:
                multiSelect();
                dm.dataout.get(info.position).selected = true;
                notifyAAandOM();
                return true;
            case R.id.info:
                displayDialog(Constants.DIALOG_FILE_INFO);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        vpm.showData();
        notifyAAandOM();
        return true;
    }

    //Globals
    public MusicPlayerService serv;

    private BroadcastReceiver brcv;

    private NotificationManagerCompat notificationManager;

    /* Contains all the Song Data */
    public DataManager dm;

    /* Settings Manager */
    public SettingsManager sc;

    /* Theme Manager */
    private ThemeManager thm;

    public ViewPortManager vpm;

    private int sortBy;
    private int searchBy;

    private boolean switchUI = false;

    private PerformanceTimer globT = new PerformanceTimer();

    private String LOG_TAG = "main";

    private String searchTerm;
    private String pltemp = "";

    private View.OnClickListener playbutton_click;
    private View.OnClickListener prevbutton_click;
    private View.OnClickListener nexbutton_click;
    private View.OnClickListener shufbutton_click;
    private View.OnClickListener repbutton_click;
    private View.OnClickListener sortbybtn_click;

    private ValueAnimator animator;

    private int PERMISSIONID = 42;

    private ExecutorService btnExecutor;

    public void handleProgressAnimation(int dur, int pos) {
        /* Creates a new ValueAnimator for the Duration Bar and the Digits, And Calls Update Song Display*/
        Log.v(LOG_TAG, "UPDATE UI, DUR: " + dur + " POS: " + pos);
        final SeekBar pb = findViewById(R.id.songDurBar);
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
                if (serv == null)
                    return;
                double proc = 0;
                double dur = animation.getDuration();
                double pos = animation.getCurrentPlayTime();
                int minutesT = ((int) dur / 1000) / 60;
                int secondsT = ((int) dur / 1000) % 60;
                int minutesP = ((int) pos / 1000) / 60;
                int secondsP = ((int) pos / 1000) % 60;
                String dspt = leftpadZero(minutesP) + ":" + leftpadZero(secondsP) + " - " + leftpadZero(minutesT) + ":" + leftpadZero(secondsT);
                if (pos > 0 && dur > 0)
                    proc = (pos / dur) * 1000;
                pb.setProgress(safeDoubleToInt(proc));
                tv.setText(dspt);
            }
        });
        int minutesT = (dur / 1000) / 60;
        int secondsT = (dur / 1000) % 60;
        int minutesP = (pos / 1000) / 60;
        int secondsP = (pos / 1000) % 60;
        String dspt = leftpadZero(minutesP) + ":" + leftpadZero(secondsP) + " - " + leftpadZero(minutesT) + ":" + leftpadZero(secondsT);
        tv.setText(dspt);
        setPlayButton((ImageButton) findViewById(R.id.buttonPlay), serv.getPlaybackStatus());
        animator.start();
        if (!serv.getPlaybackStatus()) {
            animator.pause();
        }
    }

    public void updateSongDisplay() {
        /* Set the Song Title Text */
        String text = "";
        data_song s = serv.getCurrentSong();
        if (s != null) {
            text = s.Title + " " + getString(R.string.controls_by) + " " + s.Artist;
        }
        TextView txt = findViewById(R.id.songDisplay);
        txt.setText(text);
    }

    public void displayDialog(int m) {
        /*Various Dialogues*/
        switch (m) {
            case Constants.DIALOG_SORT:
                final Dialog dia = new Dialog(this);
                dia.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dia.setContentView(R.layout.dialog_sort);
                dia.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

                TextView stit = dia.findViewById(R.id.title);
                stit.setText(R.string.dialog_sortby_title);

                ListView slv = dia.findViewById(R.id.list);

                List<String> items = new ArrayList<>();
                items.add(getString(R.string.misc_title));
                items.add(getString(R.string.misc_artist));
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.list_item_singlechoice, items);
                slv.setAdapter(adapter);
                slv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        switch (position) {
                            case 0:
                                sortBy = Constants.SORT_BYTITLE;
                                sc.putSetting(Constants.SETTING_SORTBY, "" + sortBy);
                                break;
                            case 1:
                                sortBy = Constants.SORT_BYARTIST;
                                sc.putSetting(Constants.SETTING_SORTBY, "" + sortBy);
                                break;
                        }
                    }
                });
                slv.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
                switch (sortBy) {
                    case Constants.SORT_BYARTIST:
                        slv.setItemChecked(1, true);
                        break;
                    case Constants.SORT_BYTITLE:
                        slv.setItemChecked(0, true);
                        break;
                }
                Button okb = dia.findViewById(R.id.okbtn);
                okb.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dm.sortContent(sortBy);
                        vpm.reload();
                        if (isSearching)
                            vpm.showFiltered(searchTerm, searchBy);
                        notifyAAandOM();
                        dia.dismiss();
                    }
                });
                dia.show();
                break;
            case Constants.DIALOG_SEARCHBY:
                final Dialog sdia = new Dialog(this);
                sdia.requestWindowFeature(Window.FEATURE_NO_TITLE);
                sdia.setContentView(R.layout.dialog_searchby);
                sdia.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

                TextView setit = sdia.findViewById(R.id.title);
                setit.setText(R.string.dialog_searchby_title);

                ListView selv = sdia.findViewById(R.id.list);

                List<String> sitems = new ArrayList<>();
                sitems.add(getString(R.string.misc_title));
                sitems.add(getString(R.string.misc_artist));
                sitems.add(getString(R.string.misc_both));

                ArrayAdapter<String> sadapter = new ArrayAdapter<String>(this, R.layout.list_item_singlechoice, sitems);
                selv.setAdapter(sadapter);
                selv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        switch (position) {
                            case 0:
                                searchBy = Constants.SEARCH_BYTITLE;
                                sc.putSetting(Constants.SETTING_SEARCHBY, "" + searchBy);
                                break;
                            case 1:
                                searchBy = Constants.SEARCH_BYARTIST;
                                sc.putSetting(Constants.SETTING_SEARCHBY, "" + searchBy);
                                break;
                            case 2:
                                searchBy = Constants.SEARCH_BYBOTH;
                                sc.putSetting(Constants.SETTING_SEARCHBY, "" + searchBy);
                                break;
                        }
                    }
                });
                selv.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
                switch (searchBy) {
                    case Constants.SEARCH_BYTITLE:
                        selv.setItemChecked(0, true);
                        break;
                    case Constants.SEARCH_BYARTIST:
                        selv.setItemChecked(1, true);
                        break;
                    case Constants.SEARCH_BYBOTH:
                        selv.setItemChecked(2, true);
                        break;
                }
                Button sokb = sdia.findViewById(R.id.okbtn);
                sokb.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sdia.dismiss();
                    }
                });
                sdia.show();
                break;

            case Constants.DIALOG_PLAYLIST_EDIT:
                final Dialog pled = new Dialog(this);
                pled.requestWindowFeature(Window.FEATURE_NO_TITLE);
                pled.setContentView(R.layout.dialog_playlist_edit);
                pled.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                TextView plet = pled.findViewById(R.id.title);
                plet.setText(R.string.dialog_playlist_edit_title);
                final EditText et = pled.findViewById(R.id.plname);
                final String orig = dm.getIndex();
                et.setText(dm.getIndex());
                pled.findViewById(R.id.btnok).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String t = et.getText().toString();
                        if (t.equals(orig)) {
                        } else if (t.length() <= 0) {
                            showToastMessage(getString(R.string.error_emptyplname));
                        } else if (dm.checkPlayList(t)) {
                            showToastMessage(getString(R.string.misc_playlistexistsp1) + " " + t + " " + getString(R.string.misc_playlistexistsp2));
                        } else {
                            data_playlist r = dm.getPlayListsAsMap().get(orig);
                            r.Title = t;
                            dm.createPlayList(t, r);
                            dm.selectPlayList(t);
                            Log.v(LOG_TAG, "ORIG: " + orig);
                            dm.deletePlayList(orig);
                            dm.loadPlaylists(t);
                            if (isSearching)
                                vpm.showFiltered(searchTerm, searchBy);
                            notifyAAandOM();
                        }
                        pled.dismiss();
                    }
                });
                pled.findViewById(R.id.btnback).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        pled.dismiss();
                    }
                });
                pled.findViewById(R.id.btnDel).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        pled.dismiss();
                        displayDialog(Constants.DIALOG_WARNING_PLAYLIST_DELETE);
                    }
                });
                pled.show();
                et.requestFocus();
                break;

            case Constants.DIALOG_WARNING_FILE_DELETE_FROMPLAYLIST:
                final Dialog dd = new Dialog(this);
                dd.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dd.setContentView(R.layout.dialog_file_delete);
                dd.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                TextView dtit = dd.findViewById(R.id.title);
                dtit.setText(getString(R.string.dialog_file_deletefrom_title));
                dd.findViewById(R.id.btnPos).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (dm.getSelector() != (Constants.DATA_SELECTOR_PLAYLISTS)) {
                            showToastMessage(getString(R.string.error_delfromdef));
                        } else if (dm.dataout.size() <= getSelected().size()) {
                            showToastMessage(getString(R.string.error_delfromempty));
                        } else {
                            List<data_song> t = getSelected();
                            List<data_song> nw = new ArrayList<>();
                            for (int i = 0; i < dm.dataout.size(); i++) {
                                boolean ex = false;
                                for (int y = 0; y < t.size(); y++) {
                                    if (t.get(y).file.getAbsolutePath().equals(dm.dataout.get(i).file.getAbsolutePath())) {
                                        ex = true;
                                        break;
                                    }
                                }
                                if (!ex)
                                    nw.add(dm.dataout.get(i));
                            }
                            data_playlist tmp = new data_playlist(dm.getIndex(), nw);
                            dm.replacePlayList(dm.getIndex(), tmp);
                            dm.selectPlayList(dm.getIndex());
                            serv.setContent(dm.dataout);
                            vpm.reload();
                            if (isSearching)
                                vpm.showFiltered(searchTerm, searchBy);
                            multiSelect(false);
                            notifyAAandOM();
                            showToastMessage(t.size() + " " + getString(R.string.misc_removed));
                        }
                        dd.dismiss();
                    }
                });
                dd.findViewById(R.id.btnNeg).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dd.dismiss();
                    }
                });
                TextView tv = dd.findViewById(R.id.ays);
                tv.setText(getString(R.string.dialog_file_deletefrom_t1) + " " + getSelected().size() + " " + getString(R.string.dialog_file_deletefrom_t2));
                dd.show();
                break;
            case Constants.DIALOG_WARNING_PLAYLIST_DELETE:
                final Dialog pldd = new Dialog(this);
                pldd.requestWindowFeature(Window.FEATURE_NO_TITLE);
                LayoutInflater pldif = getLayoutInflater();
                pldd.setContentView(pldif.inflate(R.layout.dialog_playlist_delete, null));
                pldd.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                TextView pldtit = pldd.findViewById(R.id.title);
                pldtit.setText(R.string.dialog_playlist_delete_title);
                TextView fpf = pldd.findViewById(R.id.playlisttext);
                fpf.setText(dm.getIndex());
                pldd.findViewById(R.id.btnPos).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dm.deletePlayList(dm.getIndex());
                        dm.selectTracks();
                        serv.setContent(dm.dataout);
                        vpm.showSubmenu(Constants.DATA_SELECTOR_PLAYLISTS);

                        getSupportActionBar().setDisplayShowHomeEnabled(true);
                        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

                        String hexColor = "#" + Integer.toHexString(getColorFromAtt(R.attr.colorText) & 0x00ffffff); //Because ANDROID
                        String t = "<font color='" + hexColor + "'>" + getString(R.string.app_name) + "</font>";
                        getSupportActionBar().setTitle(Html.fromHtml(t));

                        if (isSearching)
                            vpm.showFiltered(searchTerm, searchBy);

                        notifyAAandOM();
                        pldd.dismiss();
                    }
                });
                pldd.findViewById(R.id.btnNeg).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        pldd.dismiss();
                    }
                });
                pldd.show();
                break;
            case Constants.DIALOG_PLAYLIST_CREATE:
                final Dialog eddia = new Dialog(this);
                eddia.requestWindowFeature(Window.FEATURE_NO_TITLE);
                LayoutInflater edif = getLayoutInflater();
                eddia.setContentView(edif.inflate(R.layout.dialog_playlist_create, null));
                eddia.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                TextView edtit = eddia.findViewById(R.id.title);
                edtit.setText(R.string.dialog_playlist_create_title);
                final EditText ip = eddia.findViewById(R.id.plname);
                eddia.findViewById(R.id.btnPos).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.v(LOG_TAG, "CREATING PLAYLIST");
                        List<data_song> t = getSelected();
                        multiSelect();
                        for (int i = 0; i < t.size(); i++) {
                            Log.v(LOG_TAG, "ITEM: " + t.get(i).file.getAbsolutePath());
                        }
                        if (ip.getText().toString().length() == 0) {
                            showToastMessage(getString(R.string.error_emptyplname));
                        } else if (dm.checkPlayList(ip.getText().toString())) {
                            dm.updatePlayList(ip.getText().toString(), getPlayList(ip.getText().toString(), t));
                            showToastMessage(t.size() + " " + getString(R.string.misc_addedto) + ip.getText().toString());
                        } else {
                            if (t.size() <= 0) {
                                showToastMessage(getString(R.string.error_createempty));
                            } else {
                                String in = ip.getText().toString();
                                dm.createPlayList(in, getPlayList(ip.getText().toString(), t));
                                showToastMessage(getString(R.string.misc_createpl) + ": " + in);
                            }
                        }
                        dm.sortContent(sortBy);
                        if (isSearching)
                            vpm.showFiltered(searchTerm, searchBy);
                        notifyAAandOM();
                        eddia.dismiss();
                    }
                });
                eddia.findViewById(R.id.btnNeg).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        eddia.dismiss();
                    }
                });
                eddia.show();
                ip.requestFocus();
                break;
            case Constants.DIALOG_EXIT_CONFIRM:
                final Dialog exdia = new Dialog(this);
                exdia.requestWindowFeature(Window.FEATURE_NO_TITLE);
                LayoutInflater exif = getLayoutInflater();
                exdia.setContentView(exif.inflate(R.layout.dialog_exit, null));
                exdia.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                exdia.findViewById(R.id.btnNeg).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        exdia.dismiss();
                    }
                });
                exdia.findViewById(R.id.btnPos).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish();
                    }
                });
                exdia.findViewById(R.id.btnNeut).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        exdia.dismiss();
                        moveTaskToBack(true);
                    }
                });
                TextView t = exdia.findViewById(R.id.title);
                t.setText(getString(R.string.misc_exit));
                exdia.show();
                break;
            case Constants.DIALOG_FILE_INFO:
                //TODO:POPULATE FILE INFO DIALOGE
                final Dialog fidia = new Dialog(this);
                fidia.requestWindowFeature(Window.FEATURE_NO_TITLE);
                LayoutInflater fiif = getLayoutInflater();
                fidia.setContentView(fiif.inflate(R.layout.dialog_file_info, null));
                fidia.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                TextView fitit = fidia.findViewById(R.id.title);
                fitit.setText(R.string.dialog_file_info_title);
                TextView fp = fidia.findViewById(R.id.filepathtitle);
                fp.setText(getString(R.string.dialog_file_info_t1) + ":");
                fp = fidia.findViewById(R.id.filepathtext);
                fp.setText(dm.dataout.get(vpm.getClicked()).file.getAbsolutePath());

                fidia.findViewById(R.id.okbtn).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        fidia.dismiss();
                    }
                });
                fidia.show();
                break;
            case Constants.DIALOG_SETTINGS:
                switchUI = true;
                Intent i = new Intent(this, SettingsActivity.class);
                i.putExtra(Constants.PARAMETER_PLAYLIST, dm.getIndex());
                startActivity(i);
                finish();
                break;
            case Constants.DIALOG_WARNING_PLAYLIST_DELETE_MULTIPLE:
                final Dialog deld = new Dialog(this);
                deld.requestWindowFeature(Window.FEATURE_NO_TITLE);
                LayoutInflater delif = getLayoutInflater();
                deld.setContentView(delif.inflate(R.layout.dialog_playlist_delete_multiple, null));
                deld.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                TextView delt = deld.findViewById(R.id.title);
                delt.setText(R.string.misc_delmultitle);
                TextView deltext = deld.findViewById(R.id.ayss);
                deltext.setText(getString(R.string.misc_delmul1) + " " + vpm.getSelected().size() + " " + getString(R.string.misc_delmul2));
                deld.findViewById(R.id.btnPos).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        vpm.deleteSelectedPlaylists();
                        notifyAAandOM();
                        deld.dismiss();
                        multiSelect(false);
                    }
                });
                deld.findViewById(R.id.btnNeg).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        deld.dismiss();
                    }
                });
                deld.show();
                break;
        }
    }

    public boolean isSearching = false;

    private void handleSearch() {
        EditText ed = findViewById(R.id.searchbox);
        ImageButton iv = findViewById(R.id.searchbybtn);
        if (ed.getVisibility() == View.GONE) {
            ed.setVisibility(View.VISIBLE);
            iv.setVisibility(View.VISIBLE);
            ed.setFocusableInTouchMode(true);
            ed.requestFocus();
            ed.addTextChangedListener(new TextChangedListener<EditText>(ed) {
                public void onTextChanged(EditText target, Editable s) {
                    searchTerm = target.getText().toString();
                    Log.v(LOG_TAG, "SEARCHTEXT: " + searchTerm);
                    if (searchTerm != "") {
                        isSearching = true;
                        vpm.showFiltered(searchTerm, searchBy);
                    } else
                        isSearching = false;
                }
            });
            toggleKeyboardView(this, this.getCurrentFocus(), true);
            if (searchTerm != null && searchTerm != "") {
                vpm.showFiltered(searchTerm, searchBy);
                isSearching = true;
            } else
                isSearching = false;
        } else {
            isSearching = false;
            toggleKeyboardView(this, this.getCurrentFocus(), false);
            ed.setVisibility(View.GONE);
            iv.setVisibility(View.GONE);
            vpm.showFiltered("", searchBy);
            findViewById(R.id.songDisplay).requestFocus();
        }
    }

    public void setListeners() {
        final ImageButton playbtn = findViewById(R.id.buttonPlay);
        ImageButton prevbtn = findViewById(R.id.buttonPrev);
        ImageButton nexbtn = findViewById(R.id.buttonNex);
        ImageButton shufbtn = findViewById(R.id.buttonShuff);
        ImageButton repbtn = findViewById(R.id.buttonRep);
        ImageButton sbbtn = findViewById(R.id.searchbybtn);
        playbutton_click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                class atask extends AsyncTask<String, String, String> {
                    @Override
                    protected String doInBackground(String... strings) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (serv != null) {
                                    boolean state = serv.pauseResume();
                                    if (state)
                                        setPlayButton(playbtn, true);
                                    else
                                        setPlayButton(playbtn, false);
                                    handleProgressAnimation(serv.getDuration(), serv.getCurrentPosition());
                                }
                            }
                        });
                        return "COMPLETE";
                    }
                }
                new atask().executeOnExecutor(btnExecutor);
            }
        };
        prevbutton_click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                class atask extends AsyncTask<String, String, String> {
                    @Override
                    protected String doInBackground(String... strings) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (serv != null && dm.filesfound) {
                                    serv.previous();
                                    updateSongDisplay();
                                    handleProgressAnimation(serv.getDuration(), serv.getCurrentPosition());
                                    setPlayButton(playbtn, true);
                                }
                            }
                        });
                        return "COMPLETE";
                    }
                }
                new atask().executeOnExecutor(btnExecutor);
            }
        };
        nexbutton_click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                class atask extends AsyncTask<String, String, String> {
                    @Override
                    protected String doInBackground(String... strings) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (serv != null && dm.filesfound) {
                                    serv.next();
                                    updateSongDisplay();
                                    handleProgressAnimation(serv.getDuration(), serv.getCurrentPosition());
                                    setPlayButton(playbtn, true);
                                }
                            }
                        });
                        return "COMPLETE";
                    }
                }
                new atask().executeOnExecutor(btnExecutor);
            }
        };
        shufbutton_click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImageButton btn = findViewById(R.id.buttonShuff);
                if (serv != null) {
                    if (serv.switchShuffle()) {
                        sc.putSetting(Constants.SETTING_SHUFFLE, "true");
                        colorControlWidgets();
                    } else {
                        sc.putSetting(Constants.SETTING_SHUFFLE, "false");
                        colorControlWidgets();
                    }
                }
            }
        };
        repbutton_click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImageButton btn = findViewById(R.id.buttonRep);
                if (serv != null) {
                    if (serv.switchRepeat()) {
                        sc.putSetting(Constants.SETTING_REPEAT, "true");
                        colorControlWidgets();
                    } else {
                        sc.putSetting(Constants.SETTING_REPEAT, "false");
                        colorControlWidgets();
                    }
                }
            }
        };
        sortbybtn_click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayDialog(Constants.DIALOG_SEARCHBY);
            }
        };
        playbtn.setOnClickListener(playbutton_click);
        prevbtn.setOnClickListener(prevbutton_click);
        nexbtn.setOnClickListener(nexbutton_click);
        shufbtn.setOnClickListener(shufbutton_click);
        repbtn.setOnClickListener(repbutton_click);
        sbbtn.setOnClickListener(sortbybtn_click);
        ListView listview = (ListView) findViewById(R.id.mainViewport);
        listview.setOnItemClickListener(this);

        SeekBar sb = findViewById(R.id.songDurBar);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int dur = 0;
            int pos = 0;
            boolean playing = false;

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (playing) {
                    serv.resume();
                    handleProgressAnimation(dur, pos);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (serv.getPlaybackStatus()) {
                    serv.pause();
                    playing = true;
                } else {
                    playing = false;
                }
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && serv.getCurrentSong() != null) {
                    float c = (float) progress / 1000;
                    int ms = safeLongToInt(Math.round(serv.getCurrentSong().length * c));
                    Log.v(LOG_TAG, "SEEKING TO " + ms + " ms");
                    serv.seek(ms);
                    pos = ms;
                    dur = safeLongToInt(serv.getCurrentSong().length);
                    handleProgressAnimation(dur, pos);
                }
            }
        });
        sb.getProgressDrawable().setColorFilter(getColorFromAtt(R.attr.colorHighlight), PorterDuff.Mode.SRC_ATOP);
        sb.getThumb().setColorFilter(getColorFromAtt(R.attr.colorHighlight), PorterDuff.Mode.SRC_ATOP);

        ImageButton imb = findViewById(R.id.btnswitchcontrolsvis);
        imb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchControlsVisibility();
            }
        });

        ImageButton trackBtn = findViewById(R.id.btnTracks);
        trackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                class trackAsync extends AsyncTask<String, String, String> {
                    @Override
                    protected String doInBackground(String... strings) {
                        Log.v(LOG_TAG, "TASK ENTRY");
                        dm.selectTracks();
                        serv.setContent(dm.dataout);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.v(LOG_TAG, "ISSEARCH: " + isSearching);
                                if (isSearching) {
                                    vpm.showFiltered(searchTerm, searchBy);
                                } else
                                    vpm.showData();
                                notifyAAandOM();
                            }
                        });
                        return "Complete";
                    }
                }
                vpm.subMenu = Constants.DATA_SELECTOR_NONE; //Needed so that the Actionbar is Correctly Setup after showEmpty
                multiSelect(false);
                runOnUiThread(new Runnable() {
                    //Clear the List Before heavy Loading Task for smoother Transition
                    @Override
                    public void run() {
                        vpm.showEmpty(Constants.DATA_SELECTOR_STATICPLAYLISTS_TRACKS);
                        notifyAAandOM();
                    }
                });
                new trackAsync().executeOnExecutor(btnExecutor);
            }
        });
        ImageButton playlistBtn = findViewById(R.id.btnPlaylists);
        playlistBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                class trackAsync extends AsyncTask<String, String, String> {
                    @Override
                    protected String doInBackground(String... strings) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                vpm.showSubmenu(Constants.DATA_SELECTOR_PLAYLISTS);
                                if (isSearching) {
                                    vpm.showFiltered(searchTerm, searchBy);
                                }
                                notifyAAandOM();
                            }
                        });
                        return "Complete";
                    }
                }
                vpm.subMenu = Constants.DATA_SELECTOR_PLAYLISTS;
                multiSelect(false);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        vpm.showEmpty(Constants.DATA_SELECTOR_PLAYLISTS);
                        notifyAAandOM();
                    }
                });
                new trackAsync().executeOnExecutor(btnExecutor);
            }
        });
        ImageButton artistBtn = findViewById(R.id.btnArtists);
        artistBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                class trackAsync extends AsyncTask<String, String, String> {
                    @Override
                    protected String doInBackground(String... strings) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                vpm.showSubmenu(Constants.DATA_SELECTOR_STATICPLAYLISTS_ARTISTS);
                                if (isSearching) {
                                    vpm.showFiltered(searchTerm, searchBy);
                                }
                                notifyAAandOM();
                            }
                        });
                        return "Complete";
                    }
                }
                vpm.subMenu = Constants.DATA_SELECTOR_STATICPLAYLISTS_ARTISTS;
                multiSelect(false);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        vpm.showEmpty(Constants.DATA_SELECTOR_STATICPLAYLISTS_ARTISTS);
                        notifyAAandOM();
                    }
                });
                new trackAsync().executeOnExecutor(btnExecutor);

            }
        });
        ImageButton albumBtn = findViewById(R.id.btnAlbums);
        albumBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                class trackAsync extends AsyncTask<String, String, String> {
                    @Override
                    protected String doInBackground(String... strings) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                vpm.showSubmenu(Constants.DATA_SELECTOR_STATICPLAYLISTS_ALBUMS);
                                if (isSearching) {
                                    vpm.showFiltered(searchTerm, searchBy);
                                }
                                notifyAAandOM();
                            }
                        });
                        return "Complete";
                    }
                }
                vpm.subMenu = Constants.DATA_SELECTOR_STATICPLAYLISTS_ALBUMS;
                multiSelect(false);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        vpm.showEmpty(Constants.DATA_SELECTOR_STATICPLAYLISTS_ALBUMS);
                        notifyAAandOM();
                    }
                });
                new trackAsync().executeOnExecutor(btnExecutor);
            }
        });
        ImageButton genreBtn = findViewById(R.id.btnGenres);
        genreBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                class trackAsync extends AsyncTask<String, String, String> {
                    @Override
                    protected String doInBackground(String... strings) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                vpm.showSubmenu(Constants.DATA_SELECTOR_STATICPLAYLISTS_GENRES);
                                if (isSearching) {
                                    vpm.showFiltered(searchTerm, searchBy);
                                }
                                notifyAAandOM();
                            }
                        });
                        return "Complete";
                    }
                }
                multiSelect(false);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        vpm.showEmpty(Constants.DATA_SELECTOR_STATICPLAYLISTS_GENRES);
                        notifyAAandOM();
                    }
                });
                new trackAsync().executeOnExecutor(btnExecutor);
            }
        });
    }

    private void colorControlWidgets() {
        ImageButton btn = findViewById(R.id.buttonPlay);
        btn.setColorFilter(getColorFromAtt(R.attr.colorText), PorterDuff.Mode.MULTIPLY);
        btn = findViewById(R.id.buttonPrev);
        btn.setColorFilter(getColorFromAtt(R.attr.colorText), PorterDuff.Mode.MULTIPLY);
        btn = findViewById(R.id.buttonNex);
        btn.setColorFilter(getColorFromAtt(R.attr.colorText), PorterDuff.Mode.MULTIPLY);
        btn = findViewById(R.id.searchbybtn);
        btn.setColorFilter(getColorFromAtt(R.attr.colorText), PorterDuff.Mode.MULTIPLY);
        if (serv != null && serv.getShuffle()) {
            btn = findViewById(R.id.buttonShuff);
            btn.setColorFilter(getColorFromAtt(R.attr.colorHighlight), PorterDuff.Mode.MULTIPLY);
        } else {
            btn = findViewById(R.id.buttonShuff);
            btn.setColorFilter(getColorFromAtt(R.attr.colorText), PorterDuff.Mode.MULTIPLY);
        }
        if (serv != null && serv.getRepeat()) {
            btn = findViewById(R.id.buttonRep);
            btn.setColorFilter(getColorFromAtt(R.attr.colorHighlight), PorterDuff.Mode.MULTIPLY);
        } else {
            btn = findViewById(R.id.buttonRep);
            btn.setColorFilter(getColorFromAtt(R.attr.colorText), PorterDuff.Mode.MULTIPLY);
        }
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
                        updateSongDisplay();
                        break;
                    case Constants.ACTION_STATUS_PLAYING:
                        ImageButton btn = findViewById(R.id.buttonPlay);
                        setPlayButton(btn, true);
                        handleProgressAnimation(serv.getDuration(), serv.getCurrentPosition());
                        break;
                    case Constants.ACTION_STATUS_PAUSED:
                        ImageButton btn1 = findViewById(R.id.buttonPlay);
                        setPlayButton(btn1, false);
                        handleProgressAnimation(serv.getDuration(), serv.getCurrentPosition());
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

    public void setPlayButton(ImageButton btnpar, boolean playpar) {
        final ImageButton btn = btnpar;
        final boolean play = playpar;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (play) {
                    btn.setImageResource(R.drawable.main_btnpause);
                } else {
                    btn.setImageResource(R.drawable.main_btnplay);
                }
            }
        });
    }

    public void multiSelect() {
        if (vpm.state == Constants.ARRAYADAPT_STATE_DEFAULT) {
            Log.v(LOG_TAG, "SWITCHING TO SELECT MODE");
            vpm.state = Constants.ARRAYADAPT_STATE_SELECT;
            for (int i = 0; i < dm.dataout.size(); i++) {
                dm.dataout.get(i).selected = false;
            }
        } else {
            Log.v(LOG_TAG, "SWITCHING TO NORMAL MODE");
            vpm.state = Constants.ARRAYADAPT_STATE_DEFAULT;
            for (int i = 0; i < dm.dataout.size(); i++) {
                dm.dataout.get(i).selected = false;
            }
        }
        notifyAAandOM();
    }

    public void multiSelect(boolean state) {
        if (state) {
            vpm.state = Constants.ARRAYADAPT_STATE_SELECT;
            invalidateOptionsMenu();
            for (int i = 0; i < dm.dataout.size(); i++) {
                dm.dataout.get(i).selected = false;
            }
        } else {
            vpm.state = Constants.ARRAYADAPT_STATE_DEFAULT;
            invalidateOptionsMenu();
            for (int i = 0; i < dm.dataout.size(); i++) {
                dm.dataout.get(i).selected = false;
            }
        }
    }

    private data_playlist getPlayList(String title, List<data_song> audio) {
        return new data_playlist(title, audio);
    }

    public List<data_song> getSelected() {
        List<data_song> ret = new ArrayList<>();
        for (int i = 0; i < dm.dataout.size(); i++) {
            if (dm.dataout.get(i).selected) {
                ret.add(dm.dataout.get(i));
            }
        }
        return ret;
    }

    public void notifyAAandOM() {
        if (vpm != null)
            vpm.notifyAA();
        invalidateOptionsMenu();
    }

    private void getSettings() {
        sortBy = Integer.parseInt(sc.getSetting(Constants.SETTING_SORTBY));
        searchBy = Integer.parseInt(sc.getSetting(Constants.SETTING_SEARCHBY));
        serv.setVolume(Float.parseFloat(sc.getSetting(Constants.SETTING_VOLUME)));
        serv.switchRepeat(Boolean.parseBoolean(sc.getSetting(Constants.SETTING_REPEAT)));
        serv.switchShuffle(Boolean.parseBoolean(sc.getSetting(Constants.SETTING_SHUFFLE)));
        colorControlWidgets();
    }

    public void showToastMessage(String msg) {
        Toast toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        toast.show();
    }

    public int getColorFromAtt(int v) {
        TypedValue tv = new TypedValue();
        Resources.Theme theme = getTheme();
        theme.resolveAttribute(v, tv, true);
        return tv.data;
    }

    public void switchControlsVisibility(boolean state) {
        View hideable = findViewById(R.id.part1);
        ImageButton imb = findViewById(R.id.btnswitchcontrolsvis);
        if (state) {
            hideable.setVisibility(View.VISIBLE);
            imb.setImageResource(R.drawable.main_btnclosecontrols);
            findViewById(R.id.songDisplay).requestFocus();
        } else {
            hideable.setVisibility(View.GONE);
            imb.setImageResource(R.drawable.main_btnopencontrols);
        }
    }

    public void switchControlsVisibility() {
        View hideable = findViewById(R.id.part1);
        ImageButton imb = findViewById(R.id.btnswitchcontrolsvis);
        if (hideable.getVisibility() == View.GONE) {
            hideable.setVisibility(View.VISIBLE);
            imb.setImageResource(R.drawable.main_btnclosecontrols);
            findViewById(R.id.songDisplay).requestFocus();
            //TODO: Fix unreliable Marquee in API 21
        } else {
            hideable.setVisibility(View.GONE);
            imb.setImageResource(R.drawable.main_btnopencontrols);
        }
    }

    //Tools
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

    public static void toggleKeyboardView(Context context, View view, boolean b) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (b)
            imm.showSoftInput(view, 0);
        else
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static void expandTouchArea(final View bigView, final View smallView, final int extraPadding) {
        bigView.post(new Runnable() {
            @Override
            public void run() {
                Rect rect = new Rect();
                smallView.getHitRect(rect);
                rect.top -= extraPadding;
                rect.left -= extraPadding;
                rect.right += extraPadding;
                rect.bottom += extraPadding;
                bigView.setTouchDelegate(new TouchDelegate(rect, smallView));
            }
        });
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
        if (!switchUI)
            stopService(intent);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            serv = ((MusicPlayerService.LocalBinder) service).getService();
            getSettings();
            dm.loadContentFromMediaStore();
            dm.sortContent(sortBy);

            dm.loadPlaylists(pltemp);
            vpm = new ViewPortManager(MainActivity.this, (ListView) findViewById(R.id.mainViewport), findViewById(R.id.SubMenuContainer), dm);
            dm.selectTracks();
            serv.setContent(dm.dataout);
            vpm.showData();
            dm.loadContentFromFiles();

            serv.setEqualizerPreset(Integer.parseInt(sc.getSetting(Constants.SETTING_EQUALIZERPRESET)));

            updateSongDisplay();
            handleProgressAnimation(serv.getDuration(), serv.getCurrentPosition());

            notifyAAandOM();

            globT.printStep(LOG_TAG, "Service Initialization");
        }

        public void onServiceDisconnected(ComponentName className) {
            serv = null;
        }
    };

    //Classes
    //TextChangedListener For Search Function
    public abstract class TextChangedListener<T> implements TextWatcher {
        private T target;

        public TextChangedListener(T target) {
            this.target = target;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            this.onTextChanged(target, s);
        }

        public abstract void onTextChanged(T target, Editable s);
    }
}
