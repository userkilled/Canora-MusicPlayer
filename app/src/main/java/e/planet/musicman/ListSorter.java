package e.planet.musicman;

import android.media.MediaMetadataRetriever;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;

public class ListSorter {

    //Globals
    String LOG_TAG = "LISTSRT";

    //Constructor
    public ListSorter()
    {

    }

    //Callbacks
    public ArrayList<File> sort(ArrayList<File> in, int opt)
    {
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

        PerformanceTimer at = new PerformanceTimer(); //Average Performance Timer
        at.startAverage();
        //Load Filehandles into ArrayList
        for (int i = 0; i < in.size(); i++)
        {
            fileAndName x = new fileAndName();
            x.file = in.get(i);
            x.title = getSongName(in.get(i).getAbsolutePath());
            fn.add(x);
            at.stepAverage();
        }
        at.printAverage(LOG_TAG,"Loop to Read Files into Custom Object");
        //Sort ArrayList by Name
        fn.sort(new NameSorter());

        //Load Sorted Filehandles back into Return List
        at.startAverage();
        for (int i = 0; i < fn.size(); i++)
        {
            rtrn.add(fn.get(i).file);
            at.stepAverage();
        }
        at.printAverage(LOG_TAG,"Loop to get the Files back from the Custom Object");
        pt.printStep("LISTSORTER","SORTFILESBYNAME");
        pt.stop();
        //Return List
        return rtrn;
    }

    private String getSongName(String path) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(path);
        if (mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) != null) {
            return mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE).substring(0,1).toLowerCase();
        }
        else
        {
            File rt = new File(path);
            return rt.getName().substring(0,1).toLowerCase();
        }
    }

    //Objects
    private class NameSorter implements Comparator<fileAndName>
    {
        @Override
        public int compare(fileAndName f1, fileAndName f2)
        {
            return f1.title.compareTo(f2.title);
        }
    }

    private static class fileAndName
    {
        File file;
        String title;
    }
}