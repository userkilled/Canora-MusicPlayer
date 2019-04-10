package e.planet.musicman;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ListSorter {
    //TODO: Try to Optimize sortSongsByName

    //Globals
    String LOG_TAG = "LISTSRT";

    Context co;

    //Constructor
    public ListSorter()
    {

    }

    //Callbacks
    public List<SongItem> sort(List<SongItem> in, Context cont)
    {
        co = cont;
        List<SongItem> sorted = null;
        sorted = sortSongsByName(in);
        return sorted;
    }

    //Private Functions
    private List<SongItem> sortSongsByName(List<SongItem> in)
    {
        PerformanceTimer pt = new PerformanceTimer(); //Function Performance Timer
        pt.start();
        List<SongItem> rtrn = in;
        Log.v(LOG_TAG,"INPUT LIST SIZE: " + in.size());
        rtrn.sort(new NameSorter());
        Log.v(LOG_TAG,"SORTED LIST SIZE: " + rtrn.size());
        return rtrn;
    }

    //Objects
    private class NameSorter implements Comparator<SongItem>
    {
        @Override
        public int compare(SongItem f1, SongItem f2)
        {
           // Log.v(LOG_TAG,"Comparing: " + f1.Title + " to " + f2.Title);
            return f1.Title.substring(0,1).toLowerCase().compareTo(f2.Title.substring(0,1).toLowerCase());
        }
    }
}