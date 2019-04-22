package e.planet.musicman;

import android.content.Context;
import android.util.Log;
import e.planet.musicman.SongItem;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class PlayListContainer {
    //Main Reference For ListView
    public List<SongItem> viewList = new ArrayList<>();

    //Reference of the Currently Playing PlayList for the Service
    public List<SongItem> playList = new ArrayList<>();

    public PlayListContainer(Context c) {
        gc = c;
    }

    //Public Callbacks
    public void setContent(List<SongItem> i) {
        contentList.clear();
        contentList.addAll(i);

        viewList.clear();
        viewList.addAll(i);

        playList.clear();
        playList.addAll(i);
    }

    public void showFiltered(String term, int srb) {
        List<SongItem> flt = new ArrayList<>();
        switch (srb) {
            case Constants.SEARCH_BYTITLE:
                Log.v(LOG_TAG, "SEARCH BY TITLE");
                for (int i = 0; i < contentList.size(); i++) {
                    if (compareStrings(contentList.get(i).Title, term)) {
                        flt.add(contentList.get(i));
                    }
                }
                break;
            case Constants.SEARCH_BYARTIST:
                Log.v(LOG_TAG, "SEARCH BY ARTIST");
                for (int i = 0; i < contentList.size(); i++) {
                    if (compareStrings(contentList.get(i).Artist, term)) {
                        flt.add(contentList.get(i));
                    }
                }
                break;
        }
        if (flt.size() > 0) {
            viewList.clear();
            viewList.addAll(flt);
        }
    }

    public void sortPlayList(int sortBy) {
        List<SongItem> srted;
        Log.v(LOG_TAG, "SORTING BY: " + sortBy);
        switch (sortBy) {
            case Constants.SORT_BYARTIST:
                Log.v(LOG_TAG, "SORTING BY ARTIST");
                srted = sortSongsByArtist(contentList);
                Log.v(LOG_TAG, "First TITLE: " + srted.get(0).Title);
                viewList.clear();
                viewList.addAll(srted);
                playList.clear();
                playList.addAll(srted);
                break;
            case Constants.SORT_BYTITLE:
                Log.v(LOG_TAG, "SORTING BY TITLE");
                srted = sortSongsByTitle(contentList);
                Log.v(LOG_TAG, "First TITLE: " + srted.get(0).Title);
                viewList.clear();
                viewList.addAll(srted);
                playList.clear();
                playList.addAll(srted);
                break;
        }
    }

    //Private Globals
    private Context gc;

    private String LOG_TAG = "PLC";

    private List<SongItem> contentList = new ArrayList<>(); //Holds ALL the Content

    //Private Functions

    private boolean compareStrings(String haystack, String needle) {
        return Pattern.compile(Pattern.quote(needle), Pattern.CASE_INSENSITIVE).matcher(haystack).find();
    }

    private List<SongItem> sortSongsByTitle(List<SongItem> s) {
        List<SongItem> ret = new ArrayList<>();
        ListSorter ls = new ListSorter();
        ret = ls.sort(gc, s, Constants.SORT_BYTITLE);
        return ret;
    }

    private List<SongItem> sortSongsByArtist(List<SongItem> s) {
        List<SongItem> ret = new ArrayList<>();
        ListSorter ls = new ListSorter();
        ret = ls.sort(gc, s, Constants.SORT_BYARTIST);
        return ret;
    }
}
