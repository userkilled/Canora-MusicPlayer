package ch.swissproductions.canora.activities;

import android.content.*;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Spinner;
import ch.swissproductions.canora.service.MusicPlayerService;
import ch.swissproductions.canora.R;
import ch.swissproductions.canora.managers.SettingsManager;
import ch.swissproductions.canora.managers.ThemeManager;
import ch.swissproductions.canora.data.Constants;

public class SettingsActivity extends AppCompatActivity {

    private MusicPlayerService serv;
    private SettingsManager sc;
    private ThemeManager thm;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pltemp = getIntent().getStringExtra(Constants.PARAMETER_PLAYLIST);
        doBindService();
        sc = new SettingsManager(this);
        thm = new ThemeManager(sc);
        setTheme(thm.getThemeResourceID());
        setContentView(R.layout.layout_settings);
        setupActionBar();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent t = new Intent(this, MainActivity.class);
                t.putExtra(Constants.PARAMETER_PLAYLIST, pltemp);
                startActivity(t);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        Intent t = new Intent(this, MainActivity.class);
        t.putExtra(Constants.PARAMETER_PLAYLIST, pltemp);
        startActivity(t);
        finish();
    }

    private String LOG_TAG = "SETTINGS";

    private String pltemp = "";

    private void doBindService() {
        Intent intent = new Intent(this, MusicPlayerService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    private void doUnbindService() {
        unbindService(mConnection);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            serv = ((MusicPlayerService.LocalBinder) service).getService();
            Log.v(LOG_TAG, "SERVICE CONNECTED");
            init();
        }

        public void onServiceDisconnected(ComponentName className) {
            serv = null;
        }
    };

    private void setupActionBar() {
        ActionBar actionbar = getSupportActionBar();
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_USE_LOGO);
        Drawable mc = getDrawable(R.drawable.notification_smallicon);
        mc.mutate().setColorFilter(getColorFromAtt(R.attr.colorText), PorterDuff.Mode.MULTIPLY);
        actionbar.setBackgroundDrawable(new ColorDrawable(getColorFromAtt(R.attr.colorToolbar)));
        String hexColor = "#" + Integer.toHexString(getColorFromAtt(R.attr.colorText) & 0x00ffffff); //Because ANDROID
        String t = "<font color='" + hexColor + "'>" + getString(R.string.dialog_settings_title) + "</font>";
        actionbar.setTitle(Html.fromHtml(t));
        Drawable d = getDrawable(R.drawable.icon_back);
        d.mutate().setColorFilter(getColorFromAtt(R.attr.colorText), PorterDuff.Mode.MULTIPLY);
        actionbar.setHomeAsUpIndicator(d);
        getSupportActionBar().setElevation(0);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void init() {
        SeekBar pb = findViewById(R.id.seekBar1);
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
                sc.putSetting(Constants.SETTING_VOLUME, "" + c);
            }
        });
        pb.getProgressDrawable().setColorFilter(getColorFromAtt(R.attr.colorHighlight), PorterDuff.Mode.SRC_ATOP);
        pb.getThumb().setColorFilter(getColorFromAtt(R.attr.colorHighlight), PorterDuff.Mode.SRC_ATOP);

        Spinner spinner = findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.Themes, R.layout.spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(thm.getSpinnerPosition(thm.getThemeResourceID()));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                if (thm.request(arg2)) {
                    recreate();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {

            }

        });
        ArrayAdapter<String> ada = new ArrayAdapter<>(this, R.layout.spinner_item, serv.getEqualizerPresetNames());
        ada.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner = findViewById(R.id.spinnerEqualizer);
        spinner.setAdapter(ada);
        spinner.setSelection(Integer.parseInt(sc.getSetting(Constants.SETTING_EQUALIZERPRESET)));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                serv.setEqualizerPreset(arg2);
                sc.putSetting(Constants.SETTING_EQUALIZERPRESET, "" + arg2);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {

            }

        });
    }

    public int getColorFromAtt(int v) {
        TypedValue tv = new TypedValue();
        Resources.Theme theme = getTheme();
        theme.resolveAttribute(v, tv, true);
        return tv.data;
    }
}
