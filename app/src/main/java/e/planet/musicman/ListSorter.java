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

public class ListSorter {

    //Globals
    String LOG_TAG = "LISTSRT";

    Context co;

    ArrayList<String> titles = new ArrayList<>();

    //Constructor
    public ListSorter()
    {

    }

    //Callbacks
    public ArrayList<File> sort(ArrayList<File> in, int opt, Context cont)
    {
        co = cont;
        ArrayList<File> sorted = null;
        if (opt == Constants.SORTBYTITLE)
        {
            sorted = sortFilesByName(in);
        }
        else if (opt == Constants.SORTBYARTIST)
        {

        }
        else
        {

        }
        return sorted;
    }

    //Functions
    private ArrayList<File> sortFilesByName(ArrayList<File> in)
    {
        PerformanceTimer pt = new PerformanceTimer(); //Function Performance Timer
        pt.start();
        //TODO:This Function Eats up Startup Time, Optimize

        ArrayList<File> rtrn = new ArrayList(in.size());

        ArrayList<fileAndName> fn = new ArrayList<>(in.size());

        PerformanceTimer st = new PerformanceTimer();
        PerformanceTimer at = new PerformanceTimer(); //Average Performance Timer
        at.startAverage();
        st.startAverage();
        //Load Filehandles into ArrayList
        for (int i = 0; i < in.size(); i++)
        {
            fileAndName x = new fileAndName();
            x.file = in.get(i);
            st.start();
            //x.title = getSongName(in.get(i).getAbsolutePath());
            x.title = getSongNameFromMediaStore(in.get(i).getAbsolutePath(),co);
            st.stepAverage();
            fn.add(x);
            at.stepAverage();
        }
        at.printAverage(LOG_TAG,"Loop to Read Files into Custom Object");
        st.printAverage(LOG_TAG,"GetSongName");
        //Sort ArrayList by Name
        fn.sort(new NameSorter());

        //Load Sorted Filehandles back into Return List
        at.startAverage();
        for (int i = 0; i < fn.size(); i++)
        {
            rtrn.add(fn.get(i).file);
            titles.add(fn.get(i).title);
            at.stepAverage();
        }
        at.printAverage(LOG_TAG,"Loop to get the Files back from the Custom Object");
        pt.printStep(LOG_TAG,"sortFilesByName");
        pt.stop();
        //Return List
        return rtrn;
    }

    private String getSongName(String path) {
        PerformanceTimer m = new PerformanceTimer();
        m.start();
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        m.printStep(LOG_TAG,"Object Instantiation");
        mmr.setDataSource(path);
        m.printStep(LOG_TAG,"Setting Datasource");
        String title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        m.printStep(LOG_TAG,"MMR Retrieve Metadata");
        if (title != null) {
            m.printStep(LOG_TAG,"getSongName");
            return title.substring(0,1).toLowerCase();
        }
        else
        {
            File rt = new File(path);
            m.printStep(LOG_TAG,"getSongName");
            return rt.getName().substring(0,1).toLowerCase();
        }
        //ReWrite
    }
    private String getSongNameFromMediaStore(String songPath, Context context) {
        //PerformanceTimer p = new PerformanceTimer();
        //p.start();
        String id = "";
        ContentResolver cr = context.getContentResolver();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.DATA;
        String[] selectionArgs = {songPath};
        String[] projection = {MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST};
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";

        Cursor cursor = cr.query(uri, projection, selection + "=?", selectionArgs, sortOrder);

        //Log.v(LOG_TAG, songPath);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int idIndex = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
                int arIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
                id = cursor.getString(idIndex);
                String ar = cursor.getString(arIndex);
                id = id + " by " + ar;
            }
        }
        else
        {
            File rt = new File(songPath);
            id = rt.getName();
        }
        //p.printStep(LOG_TAG,"getSongNameFromMediaStore");
        return id;
    }

    //Objects
    private class NameSorter implements Comparator<fileAndName>
    {
        @Override
        public int compare(fileAndName f1, fileAndName f2)
        {
            return f1.title.substring(0,1).toLowerCase().compareTo(f2.title.substring(0,1).toLowerCase());
        }
    }

    private static class fileAndName
    {
        File file;
        String title;
        String firstChar;
    }
}