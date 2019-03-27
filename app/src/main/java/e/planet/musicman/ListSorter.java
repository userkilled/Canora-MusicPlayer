package e.planet.musicman;

import android.media.MediaMetadataRetriever;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;

public class ListSorter {

    //Globals

    //Constructor
    public void ListSorter()
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
        ArrayList<File> rtrn = new ArrayList(in.size());

        ArrayList<fileAndName> fn = new ArrayList<>(in.size());

        //Load Filehandles into ArrayList
        for (int i = 0; i < in.size(); i++)
        {
            fileAndName x = new fileAndName();
            x.file = in.get(i);
            x.title = getSongName(in.get(i).getAbsolutePath());
            fn.add(x);
        }
        //Sort ArrayList by Name
        fn.sort(new NameSorter());

        //Load Sorted Filehandles back into Return List
        for (int i = 0; i < fn.size(); i++)
        {
            rtrn.add(fn.get(i).file);
        }
        //Return List
        return rtrn;
    }

    private String getSongName(String path) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(path);
        if (mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) != null) {
            return mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        }
        else
        {
            return "";
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