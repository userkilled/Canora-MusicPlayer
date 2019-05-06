package e.planet.musicman;

import android.content.Context;

import java.util.Collections;
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
    public List<ItemSong> sort(Context c, List<ItemSong> in, int SortMode) {
        co = c;
        List<ItemSong> sorted = null;
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
    private List<ItemSong> sortSongsByTitle(List<ItemSong> in) {
        List<ItemSong> rtrn = in;
        Collections.sort(rtrn,new TitleSorter());
        return rtrn;
    }

    private List<ItemSong> sortSongsByArtist(List<ItemSong> in) {
        List<ItemSong> rt = in;
        Collections.sort(rt,new ArtistSorter());
        return rt;
    }

    //Comparators
    private class TitleSorter implements Comparator<ItemSong> {
        @Override
        public int compare(ItemSong f1, ItemSong f2) {
            return f1.Title.toLowerCase().compareTo(f2.Title.toLowerCase());
        }
    }

    private class ArtistSorter implements Comparator<ItemSong> {
        @Override
        public int compare(ItemSong o1, ItemSong o2) {
            return o1.Artist.toLowerCase().compareTo(o2.Artist.toLowerCase());
        }
    }
}