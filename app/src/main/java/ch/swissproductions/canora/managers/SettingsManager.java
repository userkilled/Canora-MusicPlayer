package ch.swissproductions.canora.managers;

import android.content.Context;
import android.util.Log;
import ch.swissproductions.canora.data.Constants;
import ch.swissproductions.canora.data.SettingsProtoBuff;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SettingsManager {
    public SettingsManager(Context mA) {
        String sp = mA.getCacheDir().getAbsolutePath() + "/settings";
        fsa = new FileSystemAccessManager(sp);
        data = readData();
        if (!verifyData(data)) {
            Log.e(LOG_TAG, "NONE/CORRUPT LOCAL DATA FOUND");
            data.clear();
            data.put(Constants.SETTING_SEARCHBY, "" + Constants.SEARCH_BYTITLE);
            data.put(Constants.SETTING_SORTBY, "" + Constants.SORT_BYTITLE);
            data.put(Constants.SETTING_VOLUME, "0.7");
            data.put(Constants.SETTING_REPEAT, "false");
            data.put(Constants.SETTING_SHUFFLE, "false");
            data.put(Constants.SETTING_THEME, Constants.THEME_DEFAULT);
            data.put(Constants.SETTING_EQUALIZERPRESET, "0");
            writeData(data);
        }
    }

    public String getSetting(String index) {
        if (data.get(index) != null)
            return data.get(index);
        else
            return "";
    }

    public void putSetting(String index, String dataS) {
        data.put(index, dataS);
        writeData(data);
    }

    private String LOG_TAG = "SETC";

    private Map<String, String> data; //Key = Constants.SETTING_...

    private FileSystemAccessManager fsa;

    //XML Abstraction Layer
    private Map<String, String> readData() {
        Map<String, String> ret = new HashMap<>();
        byte[] rdata = fsa.read();
        try {
            SettingsProtoBuff.Settings set = SettingsProtoBuff.Settings.parseFrom(rdata);
            for (SettingsProtoBuff.Setting it : set.getSettingsList())
            {
                if (it.hasName() && it.hasValue())
                    ret.put(it.getName(),it.getValue());
            }
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            ret.clear();
        }
        return ret;
    }

    private void writeData(Map<String, String> data) {
        if (data.size() > 0) {
            SettingsProtoBuff.Settings.Builder set = SettingsProtoBuff.Settings.newBuilder();
            for (Map.Entry<String,String> it : data.entrySet())
            {
                SettingsProtoBuff.Setting s = SettingsProtoBuff.Setting.newBuilder().setName(it.getKey()).setValue(it.getValue()).build();
                set.addSettings(s);
            }
            byte[] wdata = set.build().toByteArray();
            fsa.write(wdata);
        }
    }

    //Misc
    private boolean verifyData(Map<String, String> dat) {
        if (dat.size() < 1) {
            return false;
        }
        List<String> sett = Constants.Settings.theSettings.getSettingsList();
        for (int i = 0; i < sett.size(); i++) {
            boolean etemp = false;
            for (Map.Entry<String, String> entry : dat.entrySet()) {
                if (entry.getKey().equals(sett.get(i)) && entry.getValue().length() > 0) {
                    etemp = true;
                    break;
                }
            }
            if (!etemp) {
                return false;
            }
        }
        return true;
    }
}
