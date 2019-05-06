package e.planet.musicman;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class PlayListManager {
    //Main Reference For ListView
    public List<SongItem> viewList = new ArrayList<>();

    //Reference of the Currently Playing PlayList for the Service
    public List<SongItem> playList = new ArrayList<>();

    public PlayListManager(Context c, MainActivity b) {
        gc = c;
        mainActivity = b;
        setExtensionsAndSearchPaths();
    }

    //Public Callbacks
    public void loadContent() {
        List<SongItem> ml = getSongsfromMediaStore();
        setContent(ml);
        if (!taskIsRunning)
            new LoadFilesTask().execute(gc);
    }

    public void loadContent(List<SongItem> i) {
        setContent(i);
    }

    public void showFiltered(String term, int srb) {
        if (contentList.size() == 0)
            return;
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
            case Constants.SEARCH_BYBOTH:
                Log.v(LOG_TAG, "SEARCH BY BOTH");
                for (int i = 0; i < contentList.size(); i++) {
                    if (compareStrings(contentList.get(i).Title, term) || compareStrings(contentList.get(i).Artist, term)) {
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
        if (contentList.size() == 0)
            return;
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
    private MainActivity mainActivity;

    private String LOG_TAG = "PLC";

    private List<SongItem> contentList = new ArrayList<>(); //Holds ALL the Content

    private int GIDC = 0; //ID Counter for Song Items

    private List<String> validExtensions = new ArrayList<String>();
    private List<String> searchPaths = new ArrayList<>();

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

    private List<SongItem> getSongsfromMediaStore() {
        List<SongItem> ret = new ArrayList<>();
        Cursor c = gc.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{
                MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.DATA
        }, null, null, null);
        while (c.moveToNext()) {
            SongItem t = new SongItem();
            t.Title = c.getString(c.getColumnIndex(MediaStore.Audio.Media.TITLE));
            Log.v(LOG_TAG, "TITLE: " + t.Title);
            t.Artist = c.getString(c.getColumnIndex(MediaStore.Audio.Media.ARTIST));
            Log.v(LOG_TAG, "ARTIST: " + t.Artist);
            t.Album = c.getString(c.getColumnIndex(MediaStore.Audio.Media.ALBUM));
            Log.v(LOG_TAG, "ALBUM: " + t.Album);
            t.file = new File(c.getString(c.getColumnIndex(MediaStore.Audio.Media.DATA)));
            Log.v(LOG_TAG, "FILEPATH: " + t.file.getAbsolutePath());
            t.id = GIDC++;
            Log.v(LOG_TAG, "ID: " + t.id);
            ret.add(t);
        }
        return ret;
    }

    private List<SongItem> getSongsfromFiles() {
        List<SongItem> ret = new ArrayList<>();
        for (String str : searchPaths) {
            Log.v(LOG_TAG, "Searching in Directory: " + str);
            List<SongItem> te = getPlayListAsItems(str);
            if (te != null)
                ret.addAll(te);
        }
        return ret;
    }

    private List<SongItem> mergeLists(List<SongItem> a, List<SongItem> b) {
        List<SongItem> ret = new ArrayList<>();
        if (a.size() > b.size()) {
            ret.addAll(a);
            for (int i = 0; i < b.size(); i++) {
                boolean ex = false;
                for (int y = 0; y < a.size(); y++) {
                    if (b.get(i).file.getAbsolutePath().equals(a.get(y).file.getAbsolutePath())) {
                        ex = true;
                    }
                }
                if (!ex) {
                    ret.add(b.get(i));
                }
            }
        } else {
            ret.addAll(b);
            for (int i = 0; i < a.size(); i++) {
                boolean ex = false;
                for (int y = 0; y < b.size(); y++) {
                    if (a.get(i).file.getAbsolutePath().equals(b.get(y).file.getAbsolutePath())) {
                        ex = true;
                    }
                }
                if (!ex) {
                    ret.add(a.get(i));
                }
            }
        }
        return ret;
    }

    private void setContent(List<SongItem> in) {
        contentList.clear();
        contentList.addAll(in);

        viewList.clear();
        viewList.addAll(in);

        playList.clear();
        playList.addAll(in);
    }

    private void setExtensionsAndSearchPaths() {
        validExtensions.add(".mp3");
        validExtensions.add(".mp4");
        validExtensions.add(".m4a");
        validExtensions.add(".avi");
        validExtensions.add(".aac");
        validExtensions.add(".mkv");
        validExtensions.add(".wav");
        searchPaths.add("/storage/emulated/0/Music");
        searchPaths.add("/storage/emulated/0/Download");
    }

    private List<SongItem> getPlayListAsItems(String rootPath) {
        Bitmap dicon = BitmapFactory.decodeResource(gc.getResources(), R.drawable.icon_unsetsong);
        List<File> t = getPlayListFiles(rootPath);
        List<SongItem> ret = new ArrayList<>();
        for (int i = 0; i < t.size(); i++) {
            SongItem s = new SongItem(gc, t.get(i), GIDC++, dicon);
            ret.add(s);
        }
        return ret;
    }

    private ArrayList<File> getPlayListFiles(String rootPath) {
        ArrayList<File> fileList = new ArrayList<>();
        try {
            File rootFolder = new File(rootPath);
            File[] files = rootFolder.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    if (getPlayListFiles(file.getAbsolutePath()) != null) {
                        fileList.addAll(getPlayListFiles(file.getAbsolutePath()));
                    } else {
                        break;
                    }
                } else if (validExtensions.contains(getFileExtension(file))) {
                    fileList.add(file);
                }
            }
            return fileList;
        } catch (Exception e) {
            return null;
        }
    }

    private static String getFileExtension(File file) {
        String extension = "";

        try {
            if (file != null && file.exists()) {
                String name = file.getName();
                extension = name.substring(name.lastIndexOf("."));
            }
        } catch (Exception e) {
            extension = "";
        }

        return extension;

    }

    public boolean taskIsRunning = false;

    protected class LoadFilesTask extends AsyncTask<Context, Integer, String> {
        @Override
        protected String doInBackground(Context... params) {
            // Do the time comsuming task here
            taskIsRunning = true;
            Log.v(LOG_TAG, "ASYNC START");
            List<SongItem> nw = getSongsfromFiles();
            Log.v(LOG_TAG, "FOUND FILES: " + nw.size());
            List<SongItem> nnw = mergeLists(contentList, nw);
            setContent(nnw);
            Log.v(LOG_TAG, "NEW SIZE: " + contentList.size());
            sortPlayList(mainActivity.sortBy);
            mainActivity.serv.reload();
            mainActivity.updateArrayAdapter();
            Log.v(LOG_TAG, "ASYNC END");
            taskIsRunning = false;
            return "COMPLETE!";
        }

        // -- gets called just before thread begins
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        // -- called from the publish progress
        // -- notice that the datatype of the second param gets passed to this
        // method
        @Override
        protected void onProgressUpdate(Integer... values) {

        }

        // -- called if the cancel button is pressed
        @Override
        protected void onCancelled() {
            super.onCancelled();
        }

        // -- called as soon as doInBackground method completes
        // -- notice that the third param gets passed to this method
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            // Show the toast message here
        }
    }

}
