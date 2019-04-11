package e.planet.musicman;

import android.content.Context;
import android.util.Log;

import java.util.Comparator;
import java.util.List;


public class ListSorter {
    //Globals
    String LOG_TAG = "LISTSRT";

    Context co;

    //Constructor
    public ListSorter() {

    }

    //Callbacks
    public List<SongItem> sort(Context c, List<SongItem> in, int SortMode) {
        co = c;
        List<SongItem> sorted = null;
        switch (SortMode) {
            case Constants.SORT_BYTITLE:
                sorted = sortSongsByTitle(in);
                break;
            case Constants.SORT_BYARTIST:
                sorted = sortSongsByArtist(in);
                break;
        }
        return sorted;
    }

    //Sort Functions
    private List<SongItem> sortSongsByTitle(List<SongItem> in) {
        List<SongItem> rtrn = in;
        rtrn.sort(new TitleSorter());
        return rtrn;
    }

    private List<SongItem> sortSongsByArtist(List<SongItem> in) {
        List<SongItem> rt = in;
        rt.sort(new ArtistSorter());
        return rt;
    }

    //Comparators
    private class TitleSorter implements Comparator<SongItem> {
        @Override
        public int compare(SongItem f1, SongItem f2) {
            return f1.Title.toLowerCase().compareTo(f2.Title.toLowerCase());
        }
    }

    private class ArtistSorter implements Comparator<SongItem> {
        @Override
        public int compare(SongItem o1, SongItem o2) {
            return o1.Artist.toLowerCase().compareTo(o2.Artist.toLowerCase());
        }
    }
}