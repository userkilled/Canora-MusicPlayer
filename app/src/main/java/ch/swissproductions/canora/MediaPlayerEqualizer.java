package ch.swissproductions.canora;

import android.media.MediaPlayer;
import android.media.audiofx.Equalizer;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MediaPlayerEqualizer {
    public MediaPlayerEqualizer(MediaPlayer m) {
        mp = m;
        eq = new Equalizer(0,mp.getAudioSessionId());
        short n = eq.getNumberOfPresets();
        presets = new ArrayList<>();
        for (int i = 0; i < n - 1;i++)
        {
            preset p = new preset();
            p.name = eq.getPresetName((short)i);
            p.val = (short)i;
            presets.add(p);
        }
    }
    public void setPreset(String name)
    {
        for (int i = 0; i < presets.size(); i++)
        {
            if (presets.get(i).name.equals(name))
            {
                eq.usePreset(presets.get(i).val);
            }
        }
        selectedPreset = name;
    }
    public List<String> getPresets()
    {
        List<String>ret = new ArrayList<>();
        for (int i = 0; i < presets.size(); i++)
        {
            Log.v(LOG_TAG,"ADDING: " + presets.get(i).name);
            ret.add(presets.get(i).name);
        }
        return ret;
    }
    public void updateMP(MediaPlayer m)
    {
        mp = m;
        eq = new Equalizer(0,mp.getAudioSessionId());
        eq.setEnabled(true);
        for (int i = 0; i < presets.size(); i++)
        {
            if (presets.get(i).name.equals(selectedPreset))
            {
                eq.usePreset(presets.get(i).val);
            }
        }
    }
    private Equalizer eq;
    private MediaPlayer mp;
    private List<preset> presets;
    private String selectedPreset;

    private String LOG_TAG = "MPE";

    private class preset {
        String name;
        short val;
    }
}
