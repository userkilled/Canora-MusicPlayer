package ch.swissproductions.canora.tools;

import android.content.Context;
import ch.swissproductions.canora.data.Constants;
import ch.swissproductions.canora.data.data_song;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class ListSorter {
    //Callbacks
    public List<data_song> sort(Context c, List<data_song> in, int SortMode) {
        List<data_song> sorted = null;
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
    private List<data_song> sortSongsByTitle(List<data_song> in) {
        List<data_song> rtrn = in;
        Collections.sort(rtrn, new TitleSorter());
        return rtrn;
    }

    private List<data_song> sortSongsByArtist(List<data_song> in) {
        List<data_song> rt = in;
        Collections.sort(rt, new ArtistSorter());
        return rt;
    }

    //Comparators
    private class TitleSorter implements Comparator<data_song> {
        @Override
        public int compare(data_song f1, data_song f2) {
            return f1.Title.toLowerCase().compareTo(f2.Title.toLowerCase());
        }
    }

    private class ArtistSorter implements Comparator<data_song> {
        @Override
        public int compare(data_song o1, data_song o2) {
            return o1.Artist.toLowerCase().compareTo(o2.Artist.toLowerCase());
        }
    }
}