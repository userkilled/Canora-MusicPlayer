package e.planet.musicman;

import android.content.res.Resources;
import android.util.Log;

public class ThemeManager {
    public int getThemeResourceID()
    {
        if (selectedTheme.equals(Constants.THEME_BLUE))
        {
            Log.v(LOG_TAG,"RETURNING BLUE THEME");
            return R.style.AppTheme_Blue;
        }
        else if (selectedTheme.equals(Constants.THEME_MINT))
        {
            Log.v(LOG_TAG,"RETURNING MINT THEME");
            return R.style.AppTheme_Mint;
        }
        return -1;
    }
    public boolean request(String title)
    {
        Log.v(LOG_TAG,"REQUEST: " + title + " SELECTED: " + selectedTheme);
        if (!selectedTheme.equals(title))
        {
            selectedTheme = title;
            mainActivity.sc.putSetting(Constants.SETTING_THEME,selectedTheme);
            return true;
        }
        return false;
    }
    public ThemeManager(String sett, MainActivity ma)
    {
        mainActivity = ma;
        selectedTheme = mainActivity.sc.getSetting(Constants.SETTING_THEME);
        Log.v(LOG_TAG,"INIT SELECTED: " + selectedTheme);
    }
    private String selectedTheme;
    private MainActivity mainActivity;
    private String LOG_TAG = "THM";
}
