package ch.swissproductions.canora.tools;

import android.media.MediaPlayer;
import android.media.audiofx.Equalizer;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MediaPlayerEqualizer {
    public MediaPlayerEqualizer() {
        eq = new Equalizer(0, new MediaPlayer().getAudioSessionId());
        short n = eq.getNumberOfPresets();
        presets = new ArrayList<>();
        for (int i = 0; i < n - 1; i++) {
            preset p = new preset();
            p.name = eq.getPresetName((short) i);
            p.val = (short) i;
            presets.add(p);
        }
    }

    public void setPreset(int mediaplayerid, int preset) {
        try {
            eq = new Equalizer(0, mediaplayerid);
            eq.setEnabled(true);
            eq.usePreset((short) preset);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<String> getPresets() {
        List<String> ret = new ArrayList<>();
        for (int i = 0; i < presets.size(); i++) {
            Log.v(LOG_TAG, "ADDING: " + presets.get(i).name);
            ret.add(presets.get(i).name);
        }
        return ret;
    }

    private Equalizer eq;
    private List<preset> presets;
    private String selectedPreset;

    private String LOG_TAG = "MPE";

    private class preset {
        String name;
        short val;
    }
}
