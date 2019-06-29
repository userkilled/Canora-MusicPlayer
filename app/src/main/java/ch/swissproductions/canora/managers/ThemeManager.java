package ch.swissproductions.canora.managers;

import android.util.Log;
import ch.swissproductions.canora.data.Constants;
import ch.swissproductions.canora.data.data_theme;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ThemeManager {
    /*
    How to add a Theme:
    1. In the Constants.java File add your Theme Title String and a Entry in the Themes Enum.
    2. Add Theme to styles.xml
    3. Done. Your Theme should now Show up under the Settings->Themes Spinner
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

    public List<CharSequence> getThemeNames()
    {
        List<CharSequence> ret = new ArrayList<>();
        for (int i = 0; i < themes.size(); i++)
        {
            ret.add(themes.get(i).title);
        }
        return ret;
    }

    public ThemeManager(SettingsManager s) {
        sc = s;
        themes = Constants.Themes.theThemes.get();
        Collections.sort(themes, new Comparator() {
            public int compare(Object o1, Object o2) {
                data_theme p1 = (data_theme) o1;
                data_theme p2 = (data_theme) o2;
                return p1.title.compareToIgnoreCase(p2.title);
            }
        });
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
