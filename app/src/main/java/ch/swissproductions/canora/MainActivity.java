package ch.swissproductions.canora;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
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
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    //Callbacks
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(LOG_TAG, "ONCREATE CALLED");
        super.onCreate(savedInstanceState);
        globT.start();
        sc = new SettingsManager(getApplicationContext());
        pl = new PlayListManager(getApplicationContext(), this);
        thm = new ThemeManager(sc);
        setTheme(thm.getThemeResourceID());
        setContentView(R.layout.layout_main);
        ListView lv = findViewById(R.id.mainViewport);
        registerForContextMenu(lv);
        setListAdapter();
        setupActionBar();
        findViewById(R.id.searchbox).setVisibility(View.GONE);
        findViewById(R.id.searchbybtn).setVisibility(View.GONE);

        pltemp = getIntent().getStringExtra(Constants.PARAMETER_PLAYLIST);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.v(LOG_TAG, "REQUESTING PERMISSION");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_CONTACTS);
        } else {
            Log.v(LOG_TAG, "PERMISSION ALREADY GRANTED");
            startplayer();
            registerReceiver();
            setListeners();
            colorControlWidgets();
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
            loadFiles();
        }
        invalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.v(LOG_TAG, "ONCREATEOPTIONS");
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
        //TODO#POLISHING: Options Menu Changes are still Visible
        Menu m = menu;
        final MainActivity mainActivity = this;
        m.findItem(R.id.action_addTo).getSubMenu().clear();
        m.findItem(R.id.action_addTo).getSubMenu().add(0, R.id.action_playlist_create, 0, R.string.menu_options_newplaylist);
        m.findItem(R.id.action_playlist_select).getSubMenu().clear();
        int plc = 0;
        for (Map.Entry<String, data_playlist> entry : pl.getPlayLists().entrySet()) {
            //ADDTO
            SubMenu sub = m.findItem(R.id.action_addTo).getSubMenu();
            if (entry.getValue().Title.length() != 0) {
                sub.add(0, plc, 1, entry.getValue().Title);
                sub.findItem(plc).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        data_playlist t = new data_playlist(item.getTitle().toString(), mainActivity.getSelected());
                        mainActivity.multiSelect();
                        pl.updatePlayList(item.getTitle().toString(), t);
                        mainActivity.showToastMessage(t.audio.size() + " " + mainActivity.getString(R.string.misc_addedto) + " " + item.getTitle());
                        return false;
                    }
                });
            }
            entry.getValue().resid = plc;
            //SELECT
            plc++;
            sub = m.findItem(R.id.action_playlist_select).getSubMenu();
            if (entry.getValue().Title.length() == 0) {
                sub.add(0, plc, 0, R.string.misc_allfiles);//DEFAULT PLAYLIST
            } else {
                sub.add(0, plc, 1, entry.getValue().Title);
            }
            entry.getValue().resid2 = plc;
            sub.findItem(plc).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    for (Map.Entry<String, data_playlist> entry : pl.getPlayLists().entrySet()) {
                        if (item.getItemId() == entry.getValue().resid2) {
                            if (pl.selectPlayList(entry.getValue().Title) > 0) {
                                Log.e(LOG_TAG, "ERROR SELECTING PLAYLIST");
                            } else {
                                mainActivity.invalidateOptionsMenu();
                            }
                            if (entry.getValue().Title.equals("")) {
                                String hexColor = "#" + Integer.toHexString(mainActivity.getColorFromAtt(R.attr.colorText) & 0x00ffffff); //Because ANDROID
                                String t = "<font color='" + hexColor + "'>" + mainActivity.getString(R.string.app_name) + "</font>";
                                mainActivity.getSupportActionBar().setTitle(Html.fromHtml(t));
                            } else {
                                String hexColor = "#" + Integer.toHexString(mainActivity.getColorFromAtt(R.attr.colorText) & 0x00ffffff); //Because ANDROID
                                String t = "<font color='" + hexColor + "'>" + entry.getValue().Title + "</font>";
                                mainActivity.getSupportActionBar().setTitle(Html.fromHtml(t));
                            }
                            pl.sortContent(sortBy);
                            return true;
                        }
                    }
                    return false;
                }
            });
            //HIGHLIGHT
            if (entry.getKey().equals(pl.getIndex())) {
                sub.findItem(plc).setCheckable(true).setChecked(true);
            } else {
                sub.findItem(plc).setCheckable(false).setChecked(false);
            }
            plc++;
            if (!pl.getIndex().equals("")) {
                m.findItem(R.id.action_playlist_edit).setVisible(true);
                getSupportActionBar().setDisplayShowHomeEnabled(false);
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                String hexColor = "#" + Integer.toHexString(mainActivity.getColorFromAtt(R.attr.colorText) & 0x00ffffff); //Because ANDROID
                String t = "<font color='" + hexColor + "'>" + entry.getValue().Title + "</font>";
                mainActivity.getSupportActionBar().setTitle(Html.fromHtml(t));
            } else {
                m.findItem(R.id.action_playlist_edit).setVisible(false);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                String hexColor = "#" + Integer.toHexString(mainActivity.getColorFromAtt(R.attr.colorText) & 0x00ffffff); //Because ANDROID
                String t = "<font color='" + hexColor + "'>" + mainActivity.getString(R.string.app_name) + "</font>";
                mainActivity.getSupportActionBar().setTitle(Html.fromHtml(t));
            }
        }

        switch (arrayAdapter.state) {
            case Constants.ARRAYADAPT_STATE_DEFAULT:
                Log.v(LOG_TAG, "OPTIONS NORMAL MODE");
                menu.findItem(R.id.action_addTo).setVisible(false);
                menu.findItem(R.id.action_cancel).setVisible(false);
                menu.findItem(R.id.action_select).setVisible(true);
                menu.findItem(R.id.action_playlist_select).setVisible(true);
                menu.findItem(R.id.action_deleteFromPlaylist).setVisible(false);
                break;
            case Constants.ARRAYADAPT_STATE_SELECT:
                Log.v(LOG_TAG, "OPTIONS SELECT MODE");
                closeOptionsMenu();
                menu.findItem(R.id.action_addTo).setVisible(true);
                menu.findItem(R.id.action_cancel).setVisible(true);
                menu.findItem(R.id.action_select).setVisible(false);
                menu.findItem(R.id.action_playlist_select).setVisible(false);
                if (!pl.getIndex().equals(""))
                    menu.findItem(R.id.action_deleteFromPlaylist).setVisible(true);
                break;
        }

        menu.findItem(R.id.action_playlist_select).getIcon().setColorFilter(getColorFromAtt(R.attr.colorText), PorterDuff.Mode.MULTIPLY);
        menu.findItem(R.id.action_addTo).getIcon().setColorFilter(getColorFromAtt(R.attr.colorText), PorterDuff.Mode.MULTIPLY);
        menu.findItem(R.id.action_search).getIcon().setColorFilter(getColorFromAtt(R.attr.colorText), PorterDuff.Mode.MULTIPLY);
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
            case R.id.action_deleteFromPlaylist:
                displayDialog(Constants.DIALOG_WARNING_FILE_DELETE_FROMPLAYLIST);
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
        ImageButton btn = findViewById(R.id.buttonPlay);
        if (serv != null) {
            if (serv.play(pl.viewList.get(position).id) == 0)
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
        if (requestCode == MY_PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.v(LOG_TAG, "PERM GRANTED");
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
        if (v.getId() == R.id.mainViewport) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.list_menu, menu);
            arrayAdapter.setClicked(info.position);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.sel:
                multiSelect();
                pl.viewList.get(info.position).selected = true;
                arrayAdapter.notifyDataSetChanged();
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
        if (!pl.getIndex().equals(""))
            pl.selectPlayList("");
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        String hexColor = "#" + Integer.toHexString(getColorFromAtt(R.attr.colorText) & 0x00ffffff); //Because ANDROID
        String t = "<font color='" + hexColor + "'>" + getString(R.string.app_name) + "</font>";
        getSupportActionBar().setTitle(Html.fromHtml(t));

        invalidateOptionsMenu();
        return true;
    }

    //Globals
    public MusicPlayerService serv;

    private BroadcastReceiver brcv;

    private SongAdapter arrayAdapter;

    NotificationManagerCompat notificationManager;

    /* Contains all the Song Data */
    PlayListManager pl;

    /* Settings Manager */
    SettingsManager sc;

    /* Theme Manager */
    ThemeManager thm;

    int sortBy;
    int searchBy;

    private boolean switchUI = false;

    PerformanceTimer globT = new PerformanceTimer();

    String LOG_TAG = "main";

    private String searchTerm;
    private String pltemp = "";

    View.OnClickListener playbutton_click;
    View.OnClickListener prevbutton_click;
    View.OnClickListener nexbutton_click;
    View.OnClickListener shufbutton_click;
    View.OnClickListener repbutton_click;
    View.OnClickListener sortbybtn_click;

    ValueAnimator animator;

    int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 42;

    public void loadFiles() {
        Log.v(LOG_TAG, "LOADING FILES");
        pl.loadContent();
        pl.sortContent(sortBy);
        serv.setContent(pl.contentList);
        if (pltemp != null) {
            pl.selectPlayList(pltemp);
            pltemp = null;
            invalidateOptionsMenu();
        }
        notifyArrayAdapter();
    }

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
        /*Toolbar Menu Item Dialoges*/
        switch (m) {
            case Constants.DIALOG_SORT:
                AlertDialog.Builder b = new AlertDialog.Builder(this, R.style.DialogStyle);
                b.setTitle(R.string.dialog_sortby_title);
                b.setPositiveButton(R.string.misc_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        pl.sortContent(sortBy);
                        arrayAdapter.notifyDataSetChanged();
                    }
                });
                b.setNegativeButton(R.string.misc_back, null);
                CharSequence[] arr = {getString(R.string.misc_title), getString(R.string.misc_artist)};
                b.setSingleChoiceItems(arr, sortBy, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
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
                final AlertDialog sortdia = b.create();
                LayoutInflater l = LayoutInflater.from(this);
                View e = l.inflate(R.layout.dialog_sort, null);
                sortdia.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        sortdia.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColorFromAtt(R.attr.colorDialogText));
                        sortdia.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getColorFromAtt(R.attr.colorDialogText));
                    }
                });

                sortdia.setView(e);
                sortdia.getWindow().setBackgroundDrawable(new ColorDrawable(getColorFromAtt(R.attr.colorDialogBackground)));
                sortdia.show();
                break;
            case Constants.DIALOG_SETTINGS:
                switchUI = true;
                Intent i = new Intent(this, SettingsActivity.class);
                i.putExtra(Constants.PARAMETER_PLAYLIST, pl.getIndex());
                startActivity(i);
                finish();
                break;
            case Constants.DIALOG_SEARCHBY:
                AlertDialog.Builder b1 = new AlertDialog.Builder(this, R.style.DialogStyle);
                b1.setTitle(R.string.dialog_searchby_title);
                b1.setPositiveButton(R.string.misc_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                b1.setNegativeButton(R.string.misc_back, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Do Nothing
                    }
                });
                CharSequence[] arr1 = {getString(R.string.misc_title), getString(R.string.misc_artist), getString(R.string.misc_both)};
                b1.setSingleChoiceItems(arr1, searchBy, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
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
                final AlertDialog serdia = b1.create();
                LayoutInflater l1 = LayoutInflater.from(this);
                View e1 = l1.inflate(R.layout.dialog_sort, null);
                serdia.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        serdia.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColorFromAtt(R.attr.colorDialogText));
                        serdia.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getColorFromAtt(R.attr.colorDialogText));
                    }
                });
                serdia.setView(e1);
                serdia.getWindow().setBackgroundDrawable(new ColorDrawable(getColorFromAtt(R.attr.colorDialogBackground)));
                serdia.show();
                break;
            case Constants.DIALOG_PLAYLIST_CREATE:
                LayoutInflater lif = LayoutInflater.from(this);
                View vi = lif.inflate(R.layout.dialog_playlist_create, null);
                AlertDialog.Builder buil = new AlertDialog.Builder(this, R.style.DialogStyle);
                buil.setTitle(R.string.dialog_playlist_create_title);
                final EditText ip = vi.findViewById(R.id.plname);
                buil.setPositiveButton(R.string.misc_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.v(LOG_TAG, "CREATING PLAYLIST");
                        List<data_song> t = getSelected();
                        multiSelect();
                        for (int i = 0; i < t.size(); i++) {
                            Log.v(LOG_TAG, "ITEM: " + t.get(i).file.getAbsolutePath());
                        }
                        if (ip.getText().toString().length() == 0) {
                            showToastMessage(getString(R.string.error_emptyplname));
                        } else if (pl.checkPlayList(ip.getText().toString())) {
                            pl.updatePlayList(ip.getText().toString(), getPlayList(ip.getText().toString(), t));
                            showToastMessage(t.size() + " " + getString(R.string.misc_addedto) + ip.getText().toString());
                        } else {
                            if (t.size() <= 0) {
                                showToastMessage(getString(R.string.error_createempty));
                            } else {
                                String in = ip.getText().toString();
                                pl.createPlayList(in, getPlayList(ip.getText().toString(), t));
                                showToastMessage(getString(R.string.misc_createpl) + ": " + in);
                            }
                        }
                        pl.sortContent(sortBy);
                        invalidateOptionsMenu();
                    }
                });
                buil.setNegativeButton(R.string.misc_back, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Do Nothing
                    }
                });
                final AlertDialog plcdia = buil.create();
                plcdia.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        plcdia.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColorFromAtt(R.attr.colorDialogText));
                        plcdia.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getColorFromAtt(R.attr.colorDialogText));
                    }
                });
                plcdia.setView(vi);
                plcdia.getWindow().setBackgroundDrawable(new ColorDrawable(getColorFromAtt(R.attr.colorDialogBackground)));
                plcdia.show();
                break;
            case Constants.DIALOG_WARNING_FILE_DELETE_FROMPLAYLIST:
                LayoutInflater liff = LayoutInflater.from(this);
                View viv = liff.inflate(R.layout.dialog_file_delete, null);
                AlertDialog.Builder build = new AlertDialog.Builder(this, R.style.DialogStyle);
                TextView tv = viv.findViewById(R.id.ays);
                tv.setText(getString(R.string.dialog_file_deletefrom_t1) + " " + getSelected().size() + " " + getString(R.string.dialog_file_deletefrom_t2));
                build.setPositiveButton(R.string.misc_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (pl.getIndex().equals("")) {
                            showToastMessage(getString(R.string.error_delfromdef));
                        } else if (pl.contentList.size() <= getSelected().size()) {
                            showToastMessage(getString(R.string.error_delfromempty));
                        } else {
                            List<data_song> t = getSelected();
                            List<data_song> nw = new ArrayList<>();
                            for (int i = 0; i < pl.contentList.size(); i++) {
                                boolean ex = false;
                                for (int y = 0; y < t.size(); y++) {
                                    if (t.get(y).file.getAbsolutePath().equals(pl.contentList.get(i).file.getAbsolutePath())) {
                                        ex = true;
                                        break;
                                    }
                                }
                                if (!ex)
                                    nw.add(pl.contentList.get(i));
                            }
                            data_playlist tmp = new data_playlist(pl.getIndex(), nw);
                            pl.replacePlayList(pl.getIndex(), tmp);
                            pl.selectPlayList(pl.getIndex());
                            multiSelect(false);
                            notifyArrayAdapter();
                            showToastMessage(t.size() + " " + getString(R.string.misc_removed));
                        }
                    }
                });
                build.setNegativeButton(R.string.misc_no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Do Nothing
                    }
                });
                final AlertDialog eddia = build.create();
                eddia.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        eddia.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColorFromAtt(R.attr.colorDialogText));
                        eddia.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getColorFromAtt(R.attr.colorDialogText));
                    }
                });
                eddia.setView(viv);
                eddia.getWindow().setBackgroundDrawable(new ColorDrawable(getColorFromAtt(R.attr.colorDialogBackground)));
                eddia.show();
                break;
            case Constants.DIALOG_FILE_INFO:
                //TODO:POPULATE FILE INFO DIALOGE
                LayoutInflater lifff = LayoutInflater.from(this);
                View vive = lifff.inflate(R.layout.dialog_file_info, null);
                TextView fp = vive.findViewById(R.id.filepathtitle);
                fp.setText(getString(R.string.dialog_file_info_t1) + ":");

                fp = vive.findViewById(R.id.filepathtext);
                fp.setText(pl.viewList.get(arrayAdapter.clicked).file.getAbsolutePath());

                AlertDialog.Builder builde = new AlertDialog.Builder(this, R.style.DialogStyle);
                builde.setTitle(R.string.dialog_file_info_title);
                builde.setPositiveButton(R.string.misc_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                final AlertDialog eddiae = builde.create();
                eddiae.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        eddiae.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColorFromAtt(R.attr.colorDialogText));
                        eddiae.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getColorFromAtt(R.attr.colorDialogText));
                    }
                });
                eddiae.setView(vive);
                eddiae.getWindow().setBackgroundDrawable(new ColorDrawable(getColorFromAtt(R.attr.colorDialogBackground)));
                eddiae.show();
                break;
            case Constants.DIALOG_WARNING_PLAYLIST_DELETE:
                LayoutInflater laf = LayoutInflater.from(this);
                View vivef = laf.inflate(R.layout.dialog_playlist_delete, null);
                TextView fpf = vivef.findViewById(R.id.playlisttext);
                fpf.setText(pl.getIndex());
                AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DialogStyle);
                builder.setPositiveButton(R.string.misc_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        pl.deletePlayList(pl.getIndex());
                        pl.selectPlayList("");

                        getSupportActionBar().setDisplayShowHomeEnabled(true);
                        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

                        String hexColor = "#" + Integer.toHexString(getColorFromAtt(R.attr.colorText) & 0x00ffffff); //Because ANDROID
                        String t = "<font color='" + hexColor + "'>" + getString(R.string.app_name) + "</font>";
                        getSupportActionBar().setTitle(Html.fromHtml(t));

                        invalidateOptionsMenu();
                    }
                });
                builder.setNegativeButton(R.string.misc_no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                final AlertDialog eddiaet = builder.create();
                eddiaet.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        eddiaet.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColorFromAtt(R.attr.colorDialogText));
                        eddiaet.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getColorFromAtt(R.attr.colorDialogText));
                    }
                });
                eddiaet.setView(vivef);
                eddiaet.getWindow().setBackgroundDrawable(new ColorDrawable(getColorFromAtt(R.attr.colorDialogBackground)));
                eddiaet.show();
                break;
            case Constants.DIALOG_EXIT_CONFIRM:
                LayoutInflater exli = LayoutInflater.from(this);
                View exv = exli.inflate(R.layout.dialog_exit, null);
                AlertDialog.Builder exbuild = new AlertDialog.Builder(this, R.style.DialogStyle);
                exbuild.setTitle(R.string.misc_exit);
                exbuild.setPositiveButton(R.string.misc_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
                exbuild.setNegativeButton(R.string.misc_no, null);
                exbuild.setNeutralButton(R.string.misc_minimize,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                moveTaskToBack(true);
                            }
                        });
                final AlertDialog exad = exbuild.create();
                exad.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        exad.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColorFromAtt(R.attr.colorDialogText));
                        exad.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getColorFromAtt(R.attr.colorDialogText));
                    }
                });
                exad.setView(exv);
                exad.getWindow().setBackgroundDrawable(new ColorDrawable(getColorFromAtt(R.attr.colorDialogBackground)));
                exad.show();
                break;
            case Constants.DIALOG_PLAYLIST_EDIT:
                LayoutInflater plli = LayoutInflater.from(this);
                View plv = plli.inflate(R.layout.dialog_playlist_edit, null);
                AlertDialog.Builder plbuild = new AlertDialog.Builder(this, R.style.DialogStyle);
                final EditText et = plv.findViewById(R.id.plname);
                final String orig = pl.getIndex();
                et.setText(pl.getIndex());
                plbuild.setTitle(R.string.menu_options_editpl);
                plbuild.setPositiveButton(R.string.misc_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String t = et.getText().toString();
                        if (t.length() > 0 && !t.equals(orig)) {
                            pl.createPlayList(t, pl.getPlayLists().get(orig));
                            pl.selectPlayList(t);
                            pl.deletePlayList(orig);
                            loadFiles();
                            invalidateOptionsMenu();
                        }
                    }
                });
                plbuild.setNegativeButton(R.string.misc_back, null);
                plbuild.setNeutralButton(R.string.menu_options_delpl,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                displayDialog(Constants.DIALOG_WARNING_PLAYLIST_DELETE);
                            }
                        });
                final AlertDialog plad = plbuild.create();
                plad.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        plad.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColorFromAtt(R.attr.colorDialogText));
                        plad.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getColorFromAtt(R.attr.colorDialogText));
                    }
                });
                plad.setView(plv);
                plad.getWindow().setBackgroundDrawable(new ColorDrawable(getColorFromAtt(R.attr.colorDialogBackground)));
                plad.show();
                break;
        }
    }

    private void setupActionBar() {
        ActionBar actionbar = getSupportActionBar();
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_USE_LOGO);
        Drawable mc = getDrawable(R.drawable.mainicon40x40);
        mc.mutate().setColorFilter(getColorFromAtt(R.attr.colorText), PorterDuff.Mode.MULTIPLY);
        getSupportActionBar().setIcon(mc);
        getSupportActionBar().setLogo(mc);
        actionbar.setBackgroundDrawable(new ColorDrawable(getColorFromAtt(R.attr.colorToolbar)));
        String hexColor = "#" + Integer.toHexString(getColorFromAtt(R.attr.colorText) & 0x00ffffff); //Because ANDROID
        String t = "<font color='" + hexColor + "'>" + getString(R.string.app_name) + "</font>";
        actionbar.setTitle(Html.fromHtml(t));
        Drawable d = getDrawable(R.drawable.icon_back);
        d.mutate().setColorFilter(getColorFromAtt(R.attr.colorText), PorterDuff.Mode.MULTIPLY);
        actionbar.setHomeAsUpIndicator(d);
        getSupportActionBar().setElevation(0);
    }

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
                        pl.showFiltered(searchTerm, searchBy);
                    }
                }
            });
            toggleKeyboardView(this, this.getCurrentFocus(), true);
            pl.filtering = true;
            if (searchTerm != null)
                pl.showFiltered(searchTerm, searchBy);
        } else {
            toggleKeyboardView(this, this.getCurrentFocus(), false);
            ed.setVisibility(View.GONE);
            iv.setVisibility(View.GONE);
            pl.filtering = false;
            pl.showFiltered("", searchBy);
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
                if (serv != null) {
                    if (serv.pauseResume())
                        setPlayButton(playbtn, true);
                    else
                        setPlayButton(playbtn, false);
                    handleProgressAnimation(serv.getDuration(), serv.getCurrentPosition());
                }
            }
        };
        prevbutton_click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (serv != null && serv != null) {
                    serv.previous();
                    updateSongDisplay();
                    handleProgressAnimation(serv.getDuration(), serv.getCurrentPosition());
                    setPlayButton(playbtn, true);
                }
            }
        };
        nexbutton_click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (serv != null && serv != null) {
                    serv.next();
                    updateSongDisplay();
                    handleProgressAnimation(serv.getDuration(), serv.getCurrentPosition());
                    setPlayButton(playbtn, true);
                }
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

    private void setListAdapter() {
        ListView lv = (ListView) findViewById(R.id.mainViewport);
        arrayAdapter = new SongAdapter(this, pl.viewList);
        lv.setAdapter(arrayAdapter);
        lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
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

    public void setPlayButton(ImageButton btn, boolean play) {
        if (play) {
            btn.setImageResource(R.drawable.main_btnpause);
        } else {
            btn.setImageResource(R.drawable.main_btnplay);
        }
    }

    public void multiSelect() {
        if (arrayAdapter.state == Constants.ARRAYADAPT_STATE_DEFAULT) {
            Log.v(LOG_TAG, "SWITCHING TO SELECT MODE");
            arrayAdapter.state = Constants.ARRAYADAPT_STATE_SELECT;
            invalidateOptionsMenu();
            for (int i = 0; i < pl.contentList.size(); i++) {
                pl.contentList.get(i).selected = false;
            }
        } else {
            Log.v(LOG_TAG, "SWITCHING TO NORMAL MODE");
            arrayAdapter.state = Constants.ARRAYADAPT_STATE_DEFAULT;
            invalidateOptionsMenu();
            for (int i = 0; i < pl.contentList.size(); i++) {
                pl.contentList.get(i).selected = false;
            }
        }
        arrayAdapter.notifyDataSetChanged();
    }

    public void multiSelect(boolean state) {
        if (state) {
            arrayAdapter.state = Constants.ARRAYADAPT_STATE_SELECT;
            invalidateOptionsMenu();
            for (int i = 0; i < pl.contentList.size(); i++) {
                pl.contentList.get(i).selected = false;
            }
        } else {
            arrayAdapter.state = Constants.ARRAYADAPT_STATE_DEFAULT;
            invalidateOptionsMenu();
            for (int i = 0; i < pl.contentList.size(); i++) {
                pl.contentList.get(i).selected = false;
            }
        }
    }

    private data_playlist getPlayList(String title, List<data_song> audio) {
        return new data_playlist(title, audio);
    }

    public List<data_song> getSelected() {
        List<data_song> ret = new ArrayList<>();
        for (int i = 0; i < pl.contentList.size(); i++) {
            if (pl.contentList.get(i).selected) {
                ret.add(pl.contentList.get(i));
            }
        }
        return ret;
    }

    public void notifyArrayAdapter() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                arrayAdapter.notifyDataSetChanged();
            }
        });
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
            loadFiles();
            serv.init();
            updateSongDisplay();
            invalidateOptionsMenu();
            serv.setEqualizerPreset(Integer.parseInt(sc.getSetting(Constants.SETTING_EQUALIZERPRESET)));
            handleProgressAnimation(serv.getDuration(), serv.getCurrentPosition());
            globT.printStep(LOG_TAG, "Service Initialization");
            //long l = globT.tdur;
            //showToastMessage(getString(R.string.misc_init) + ": " + l + " ms.");
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

    //ArrayAdapter of Song List Display
    public class SongAdapter extends ArrayAdapter<data_song> {

        public int state = Constants.ARRAYADAPT_STATE_DEFAULT;

        private Context mContext;
        private List<data_song> viewList;

        public SongAdapter(@NonNull Context context, List<data_song> list) {
            super(context, 0, list);
            mContext = context;
            viewList = list;
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
        }

        @NonNull
        @Override
        public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View listItem = convertView;
            if (listItem == null)
                listItem = LayoutInflater.from(mContext).inflate(R.layout.list_item_song, parent, false);
            if (viewList.get(position) == null)
                return listItem;
            TextView sn = listItem.findViewById(R.id.listsongname);
            TextView in = listItem.findViewById(R.id.listinterpret);
            TextView ln = listItem.findViewById(R.id.songlength);
            sn.setText(viewList.get(position).Title);
            in.setText(viewList.get(position).Artist);
            String lstr = "" + leftpadZero(safeLongToInt(TimeUnit.MILLISECONDS.toMinutes(viewList.get(position).length))) + ":" + leftpadZero(safeLongToInt(TimeUnit.MILLISECONDS.toSeconds(viewList.get(position).length - TimeUnit.MILLISECONDS.toMinutes(viewList.get(position).length) * 60000)));
            ln.setText(lstr);
            final CheckBox mcb = listItem.findViewById(R.id.checkbox);
            final View exp = listItem.findViewById(R.id.hitbox);
            switch (state) {
                case Constants.ARRAYADAPT_STATE_SELECT:
                    mcb.setVisibility(View.VISIBLE);
                    final ListView lv = findViewById(R.id.mainViewport);
                    mcb.setChecked(viewList.get(position).selected);
                    mcb.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            if (((CheckBox) v).isChecked()) {
                                viewList.get(position).selected = true;

                            } else {
                                viewList.get(position).selected = false;
                            }
                        }
                    });
                    expandTouchArea(exp, (View) mcb, 1000);
                    break;
                default:
                    mcb.setVisibility(View.GONE);
                    expandTouchArea(exp, (View) mcb, 0);
                    break;
            }
            return listItem;
        }

        public int clicked;

        public void setClicked(int pos) {
            clicked = pos;
        }
    }
}
