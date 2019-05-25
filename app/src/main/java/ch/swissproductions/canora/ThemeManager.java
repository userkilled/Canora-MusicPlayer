package ch.swissproductions.canora;

import android.app.Activity;
import android.util.Log;

import java.util.List;

public class ThemeManager {
    /*
    How to add a Theme:
    1. In the strings.xml add your Theme Title to the Themes String Array.
    2. In the Constants.java File add your Theme Title String and a Entry in the Themes Enum.
    3. Add Theme to styles.xml
     */
    public int getThemeResourceID() {
        for (int i = 0; i < themes.size(); i++) {
            if (themes.get(i).title.equals(currentTheme)) {
                return themes.get(i).resID;
            }
        }
        return -1;
    }

    public int getSpinnerPosition(int resid) {
        for (int i = 0; i < themes.size(); i++) {
            if (themes.get(i).resID == resid) {
                return i;
            }
        }
        return -1;
    }

    public boolean request(int spinnerpos) {
        Log.v(LOG_TAG, "REQUEST: " + spinnerpos + " SELECTED: " + currentTheme);
        if (!currentTheme.equals(themes.get(spinnerpos).title)) {
            selectTheme(themes.get(spinnerpos).title);
            return true;
        }
        return false;
    }

    public ThemeManager(SettingsManager s) {
        sc = s;
        themes = Constants.Themes.theThemes.get();
        currentTheme = sc.getSetting(Constants.SETTING_THEME);
        if (currentTheme.equals(""))
            currentTheme = Constants.THEME_DEFAULT;
        Log.v(LOG_TAG, "INIT SELECTED: " + currentTheme);
    }

    private String currentTheme;
    private SettingsManager sc;
    private String LOG_TAG = "THM";

    private List<data_theme> themes;

    private void selectTheme(String title) {
        currentTheme = title;
        sc.putSetting(Constants.SETTING_THEME, currentTheme);
    }
}
