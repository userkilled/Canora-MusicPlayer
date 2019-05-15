package e.planet.musicman;

import android.util.Log;

public class ThemeManager {
    /*
    How to add a Theme:
    1. In the strings.xml add your Theme Title to the Themes String Array.
    2. In the Constants.java File add your Theme Title String and Spinnerpos Integer.
    3. Create a Copy of one of the Existing Theme Styles in the styles.xml, Here you can set the Colors of your Theme.
    4. Add Conditional Checks in Below Functions, "getThemeResourceID , getSpinnerPosition and request"
     */
    public int getThemeResourceID() {
        if (currentTheme.equals(Constants.THEME_BLUE)) {
            Log.v(LOG_TAG, "RETURNING BLUE THEME");
            return R.style.AppTheme_Blue;
        } else if (currentTheme.equals(Constants.THEME_MINT)) {
            Log.v(LOG_TAG, "RETURNING MINT THEME");
            return R.style.AppTheme_Mint;
        } else if (currentTheme.equals(Constants.THEME_DARK)) {
            return R.style.AppTheme_Dark;
        }
        return -1;
    }

    public int getSpinnerPosition(int resid) {
        if (resid == R.style.AppTheme_Blue) {
            return Constants.SPINNERPOS_THEME_BLUE;
        } else if (resid == R.style.AppTheme_Mint) {
            return Constants.SPINNERPOS_THEME_MINT;
        } else if (resid == R.style.AppTheme_Dark) {
            return Constants.SPINNERPOS_THEME_DARK;
        }
        return -1;
    }

    public boolean request(int spinnerpos) {
        Log.v(LOG_TAG, "REQUEST: " + spinnerpos + " SELECTED: " + currentTheme);
        if (spinnerpos == Constants.SPINNERPOS_THEME_BLUE && !currentTheme.equals(Constants.THEME_BLUE)) {
            selectTheme(Constants.THEME_BLUE);
            return true;
        } else if (spinnerpos == Constants.SPINNERPOS_THEME_MINT && !currentTheme.equals(Constants.THEME_MINT)) {
            selectTheme(Constants.THEME_MINT);
            return true;
        } else if (spinnerpos == Constants.SPINNERPOS_THEME_DARK && !currentTheme.equals(Constants.THEME_DARK)) {
            selectTheme(Constants.THEME_DARK);
            return true;
        }
        return false;
    }

    public ThemeManager(String sett, MainActivity ma) {
        mainActivity = ma;
        currentTheme = mainActivity.sc.getSetting(Constants.SETTING_THEME);
        if (currentTheme.equals(""))
            currentTheme = Constants.THEME_DEFAULT;
        Log.v(LOG_TAG, "INIT SELECTED: " + currentTheme);
    }

    private String currentTheme;
    private MainActivity mainActivity;
    private String LOG_TAG = "THM";

    private void selectTheme(String title) {
        currentTheme = title;
        mainActivity.sc.putSetting(Constants.SETTING_THEME, currentTheme);
    }
}
