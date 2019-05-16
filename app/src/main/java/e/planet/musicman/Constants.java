package e.planet.musicman;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Constants {
    public static final String ACTION_TOGGLE_PLAYBACK = "com.musicman.PLAYPAUSE";
    public static final String ACTION_NEXT = "com.musicman.NEXT";
    public static final String ACTION_PREV = "com.musicman.PREV";

    public static final String ACTION_STATUS_NEWSONG = "com.musicman.NEWSONG";
    public static final String ACTION_STATUS_PLAYING = "com.musicman.PLAYING";
    public static final String ACTION_STATUS_PAUSED = "com.musicman.PAUSED";

    public static final String ACTION_QUIT = "com.musicman.QUIT";

    public enum Themes {
        theThemes();
        private List<data_theme> themes;
        Themes() {
            themes = new ArrayList<>();
            themes.add(new data_theme(Constants.THEME_BLUE,R.style.AppTheme_Blue));
            themes.add(new data_theme(Constants.THEME_MINT,R.style.AppTheme_Mint));
            themes.add(new data_theme(Constants.THEME_DARK,R.style.AppTheme_Dark));
        }
        public List<data_theme> get()
        {
            return themes;
        }
    }

    public static final String THEME_BLUE = "Blue";
    public static final String THEME_MINT = "Mint";
    public static final String THEME_DARK = "Dark";

    public static final String THEME_DEFAULT = THEME_BLUE;

    public enum Settings {
        theSettings;
        private Map<String, String> settings;
        private List<String> settList;

        Settings() {
            settings = new HashMap<>();
            settList = new ArrayList<>();
            settings.put("SETTING_SEARCHBY", SETTING_SEARCHBY);
            settList.add(SETTING_SEARCHBY);
            settings.put("SETTING_SORTBY", SETTING_SORTBY);
            settList.add(SETTING_SORTBY);
            settings.put("SETTING_VOLUME", SETTING_VOLUME);
            settList.add(SETTING_VOLUME);
            settings.put("SETTING_REPEAT", SETTING_REPEAT);
            settList.add(SETTING_REPEAT);
            settings.put("SETTING_SHUFFLE", SETTING_SHUFFLE);
            settList.add(SETTING_SHUFFLE);
            settings.put("SETTING_THEME", SETTING_THEME);
            settList.add(SETTING_THEME);
        }

        public Map<String, String> getSettings() {
            return settings;
        }

        public List<String> getSettingsList() {
            return settList;
        }
    }

    public static final String SETTING_SEARCHBY = "SEARCHBY";
    public static final String SETTING_SORTBY = "SORTBY";
    public static final String SETTING_VOLUME = "VOLUME";
    public static final String SETTING_SHUFFLE = "SHUFFLE";
    public static final String SETTING_REPEAT = "REPEAT";
    public static final String SETTING_THEME = "THEME";

    public static final int SORT_BYTITLE = 0;
    public static final int SORT_BYARTIST = 1;

    public static final int SEARCH_BYTITLE = 0;
    public static final int SEARCH_BYARTIST = 1;
    public static final int SEARCH_BYBOTH = 2;

    public static final int DIALOG_SORT = 1;
    public static final int DIALOG_SETTINGS = 2;
    public static final int DIALOG_SEARCHBY = 3;
    public static final int DIALOG_FILE_DELETE_FROMPLAYLIST = 4;
    public static final int DIALOG_FILE_INFO = 5;
    public static final int DIALOG_PLAYLIST_CREATE = 6;
    public static final int DIALOG_PLAYLIST_DELETE = 7;
    public static final int DIALOG_PLAYLIST_EDIT = 8;

    public static final int ARRAYADAPT_STATE_DEFAULT = 0;
    public static final int ARRAYADAPT_STATE_SELECT = 1;
}
