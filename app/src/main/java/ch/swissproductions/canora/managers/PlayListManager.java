package ch.swissproductions.canora.managers;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;
import ch.swissproductions.canora.tools.ListSorter;
import ch.swissproductions.canora.R;
import ch.swissproductions.canora.activities.MainActivity;
import ch.swissproductions.canora.data.Constants;
import ch.swissproductions.canora.data.data_playlist;
import ch.swissproductions.canora.data.data_song;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

public class PlayListManager {
    //Reference For ListView, Used to Manipulate the Shown Items, Always a Subset of contentList
    public List<data_song> viewList = new ArrayList<>();

    //Reference of the Currently Playing PlayList for the Service
    public List<data_song> contentList = new ArrayList<>();

    public PlayListManager(Context c, MainActivity b, int SORTBY, String PlayListIndex) {
        gc = c;
        mainActivity = b;
        if (PlayListIndex != null)
            pli = PlayListIndex;
        else
            pli = "";
        String plPath = mainActivity.getFilesDir().getAbsolutePath() + "/PlayLists";
        fsa = new FileSystemAccessManager(plPath);
        sortBy = SORTBY;
        filesfound = true;
        setExtensionsAndSearchPaths();
    }

    //Public Callbacks
    public void loadPlaylists(String selected) {
        //TODO: Optimize Playlists Loading / Storing
        if (selected == null)
            pli = "";
        else
            pli = selected;
        Map<String, data_playlist> tmp = getLocalPlayLists();//TODO:Load Titles / Artist / Album in separate Playlists
        tmp.put("", new data_playlist("", PlayLists.get("").audio));
        PlayLists.clear();
        PlayLists.putAll(tmp);
        sortContent(sortBy);
        selectPlayList(pli);
        if (searchTerm != null && !searchTerm.equals(""))
            showFiltered(searchTerm, searchBy);
        mainActivity.notifyAAandOM();
    }

    public void loadContentFromMediaStore() {
        List<data_song> ml = getSongsfromMediaStore();
        Log.v(LOG_TAG, "FOUND " + ml.size() + " SONGS IN MEDIASTORE");
        data_playlist t = new data_playlist("", ml);
        PlayLists.put("", t);
        if (t.audio.size() > 0)
            updateContent();
    }

    public void loadContentFromFiles() {
        new LoadFilesTask().execute();
    }

    public void showFiltered(String term, int srb) {
        try {
            searchTerm = term;
            searchBy = srb;
            if (contentList.size() == 0 || !filesfound)
                return;
            if (!taskIsRunning) {
                new SearchFilesTask().execute(gc);
            } else {
                List<data_song> cl = new ArrayList<>(contentList);
                Log.v(LOG_TAG, "CONTENT SIZE:" + cl.size());
                List<data_song> flt = new ArrayList<>();
                switch (srb) {
                    case Constants.SEARCH_BYTITLE:
                        Log.v(LOG_TAG, "SEARCH BY TITLE");
                        for (int i = 0; i < cl.size(); i++) {
                            if (compareStrings(cl.get(i).Title, term)) {
                                flt.add(cl.get(i));
                            }
                        }
                        break;
                    case Constants.SEARCH_BYARTIST:
                        Log.v(LOG_TAG, "SEARCH BY ARTIST");
                        for (int i = 0; i < cl.size(); i++) {
                            if (compareStrings(cl.get(i).Artist, term)) {

                                flt.add(cl.get(i));
                            }
                        }
                        break;
                    case Constants.SEARCH_BYBOTH:
                        Log.v(LOG_TAG, "SEARCH BY BOTH");
                        for (int i = 0; i < cl.size(); i++) {
                            if (compareStrings(cl.get(i).Title, term) || compareStrings(cl.get(i).Artist, term)) {
                                flt.add(cl.get(i));
                            }
                        }
                        break;
                }
                final List<data_song> inp = flt;
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        viewList.clear();
                        viewList.addAll(inp);
                    }
                });
                mainActivity.notifyAAandOM();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sortContent(int SortBy) {
        this.sortBy = SortBy;
        if (contentList.size() == 0 || !filesfound)
            return;
        final List<data_song> srted;
        Log.v(LOG_TAG, "SORTING BY: " + sortBy);
        switch (sortBy) {
            case Constants.SORT_BYARTIST:
                Log.v(LOG_TAG, "SORTING BY ARTIST");
                srted = sortSongsByArtist(contentList);
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        viewList.clear();
                        viewList.addAll(srted);
                    }
                });
                break;
            case Constants.SORT_BYTITLE:
                Log.v(LOG_TAG, "SORTING BY TITLE");
                srted = sortSongsByTitle(contentList);
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        viewList.clear();
                        viewList.addAll(srted);
                    }
                });
                break;
        }
    }

    public int selectPlayList(String name) {
        pli = "" + name;
        if (pli.equals(""))
            Log.v(LOG_TAG, "SELECTING DEFAULT PLAYLIST");
        else
            Log.v(LOG_TAG, "SELECTING PLAYLIST: " + name);
        if (PlayLists.get(pli) == null) {
            Log.e(LOG_TAG, "ERROR PLAYLIST NOT FOUND");
            return 1;
        }
        updateContent();
        sortContent(sortBy);
        if (contentList.size() > 0)
            mainActivity.serv.setContent(contentList);
        mainActivity.notifyAAandOM();
        return 0;
    }

    public int createPlayList(String name, data_playlist in) {
        Log.v(LOG_TAG, "CREATE PLAYLIST: " + name);
        PlayLists.put(name, in);
        putLocalPlayLists(PlayLists);
        return 0;
    }

    public int deletePlayList(String name) {
        Log.v(LOG_TAG, "DELETING PLAYLIST: " + name);
        if (name.length() > 0) {
            PlayLists.remove(name);
            putLocalPlayLists(PlayLists);
        }
        return 0;
    }

    public int updatePlayList(String name, data_playlist in) {
        Log.v(LOG_TAG, "UPDATE PLAYLIST: " + name);
        data_playlist n = mergePlayLists(PlayLists.get(name), in);
        PlayLists.put(name, n);
        putLocalPlayLists(PlayLists);
        return 0;
    }

    public int replacePlayList(String name, data_playlist in) {
        if (!name.equals("")) {
            PlayLists.get(name).audio.clear();
            PlayLists.get(name).audio.addAll(in.audio);
            putLocalPlayLists(PlayLists);
            return 0;
        }
        return 1;
    }

    public boolean checkPlayList(String name) {
        if (PlayLists.get(name) != null) {
            return true;
        } else
            return false;
    }

    public String getIndex() {
        return pli;
    }

    public Map<String, data_playlist> getPlayLists() {
        return PlayLists;
    }

    //Notifies the PlayListManager of the Search Box State in the MainActivity, needed for the Async Tasks to correctly Filter after Processing
    public void setFilter(boolean IsFiltering) {
        filtering = IsFiltering;
    }

    public boolean filesfound;

    //Private Globals
    private Context gc;
    private MainActivity mainActivity;
    private FileSystemAccessManager fsa;

    private int searchBy;
    private String searchTerm;

    private int sortBy;

    private String LOG_TAG = "PLC";

    private String pli; //Current Index of PlayLists Indicating Currently Selected PlayList
    private Map<String, data_playlist> PlayLists = new TreeMap<>(); //Holds ALL Content, Empty Index = All Files, otherwise Index = Name of PlayList

    private int GIDC = 0; //ID Counter for Song Items

    private List<String> validExtensions = new ArrayList<String>();
    private List<String> searchPaths = new ArrayList<>();

    //Private Functions

    //START Local Playlists
    private Map<String, data_playlist> getLocalPlayLists() {
        Map<String, data_playlist> ret = getDataAsMap();
        if (ret.size() < 1) {
            Log.e(LOG_TAG, "NONE/CORRUPT PLAYLISTS FOUND");
        } else {
            for (Map.Entry<String, data_playlist> entry : ret.entrySet()) {
                Log.v(LOG_TAG, "FOUND PLAYLIST : " + entry.getKey() + " SIZE: " + entry.getValue().audio.size());
            }
        }
        return ret;
    }

    private void putLocalPlayLists(Map<String, data_playlist> in) {
        writeDataAsXML(in);
    }

    //XML Abstraction Layer
    private Map<String, data_playlist> getDataAsMap() {
        Map<String, data_playlist> ret = new HashMap<>();
        String XMLSTR = new String(fsa.read());
        if (XMLSTR.length() > 0) {
            ret = convertXMLtoMAP(XMLSTR);
        }
        return ret;
    }

    private void writeDataAsXML(Map<String, data_playlist> data) {
        if (data.size() > 0) {
            String writeStr = convertMAPtoXML(data);
            fsa.write(writeStr.getBytes());
        }
    }

    //Conversion
    private String convertMAPtoXML(Map<String, data_playlist> in) {
        String ret = "<?xml version=\"1.0\"?><playlists>";
        for (Map.Entry<String, data_playlist> entry : in.entrySet()) {
            if (entry.getKey() == "")
                continue;
            ret += "<playlist title=\"" + encodeXML(entry.getKey()) + "\">";
            for (int i = 0; i < entry.getValue().audio.size(); i++) {
                File f = entry.getValue().audio.get(i).file;
                if (f != null && f.exists())
                    ret += "<song>" + encodeXML(f.getAbsolutePath()) + "</song>";
            }
            ret += "</playlist>";
        }
        ret += "</playlists>";
        return ret;
    }

    private Map<String, data_playlist> convertXMLtoMAP(String xml) {
        Map<String, data_playlist> ret = new HashMap<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            StringBuilder xmlStringBuilder = new StringBuilder();
            xmlStringBuilder.append(xml);
            ByteArrayInputStream input = new ByteArrayInputStream(xmlStringBuilder.toString().getBytes("UTF-8"));
            Document doc = builder.parse(input);
            Element root = doc.getDocumentElement();
            NodeList pln = root.getChildNodes();
            Log.v(LOG_TAG, "FOUND " + pln.getLength() + " PLAYLISTS");
            for (int i = 0; i < pln.getLength(); i++) {
                List<data_song> tmp = new ArrayList<>();
                NodeList sitm = pln.item(i).getChildNodes();
                for (int y = 0; y < sitm.getLength(); y++) {
                    Log.v(LOG_TAG, "PATH: " + sitm.item(y).getTextContent());
                    data_song t = getMetadata(new File(sitm.item(y).getTextContent()));
                    tmp.add(t);
                }
                data_playlist p = new data_playlist(pln.item(i).getAttributes().item(0).getTextContent(), tmp);
                ret.put(p.Title, p);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static String encodeXML(CharSequence s) {
        StringBuilder sb = new StringBuilder();
        int len = s.length();
        for (int i = 0; i < len; i++) {
            int c = s.charAt(i);
            if (c >= 0xd800 && c <= 0xdbff && i + 1 < len) {
                c = ((c - 0xd7c0) << 10) | (s.charAt(++i) & 0x3ff);    // UTF16 decode
            }
            if (c < 0x80) {      // ASCII range: test most common case first
                if (c < 0x20 && (c != '\t' && c != '\r' && c != '\n')) {
                    // Illegal XML character, even encoded. Skip or substitute
                    sb.append("&#xfffd;");   // Unicode replacement character
                } else {
                    switch (c) {
                        case '&':
                            sb.append("&amp;");
                            break;
                        case '>':
                            sb.append("&gt;");
                            break;
                        case '<':
                            sb.append("&lt;");
                            break;
                        case '\'':
                            sb.append("&apos;");
                            break;
                        case '\"':
                            sb.append("&quot;");
                            break;
                        case '\n':
                            sb.append("&#10;");
                            break;
                        case '\r':
                            sb.append("&#13;");
                            break;
                        case '\t':
                            sb.append("&#9;");
                            break;

                        default:
                            sb.append((char) c);
                    }
                }
            } else if ((c >= 0xd800 && c <= 0xdfff) || c == 0xfffe || c == 0xffff) {
                // Illegal XML character, even encoded. Skip or substitute
                sb.append("&#xfffd;");   // Unicode replacement character
            } else {
                sb.append("&#x");
                sb.append(Integer.toHexString(c));
                sb.append(';');
            }
        }
        return sb.toString();
    }

    //END Local Playlists
    //Other
    private data_song getMetadata(File f) {
        data_song t = new data_song();
        //Log.v(LOG_TAG, "Getting Metadata for File: " + f.getAbsolutePath());
        Cursor c = gc.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{
                MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.DURATION
        }, null, null, null);
        boolean found = false;
        while (c != null && c.moveToNext()) {
            if (c.getString(c.getColumnIndex(MediaStore.Audio.Media.DATA)).equals(f.getAbsolutePath())) {
                t.Title = c.getString(c.getColumnIndex(MediaStore.Audio.Media.TITLE));
                t.Artist = c.getString(c.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                t.Album = c.getString(c.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                t.file = new File(c.getString(c.getColumnIndex(MediaStore.Audio.Media.DATA)));
                t.length = Long.parseLong(c.getString(c.getColumnIndex(MediaStore.Audio.Media.DURATION)));
                t.id = GIDC++;
                found = true;
            }
        }
        c.close();
        if (!found) {
            MediaMetadataRetriever m = new MediaMetadataRetriever();
            m.setDataSource(f.getAbsolutePath());
            t.file = f;
            t.Title = m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            if (t.Title == null)
                t.Title = f.getName();
            t.Artist = m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            if (t.Artist == null)
                t.Artist = mainActivity.getString(R.string.misc_unknown);
            t.length = Long.parseLong(m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
            byte[] b = m.getEmbeddedPicture();
            if (b != null)
                t.icon = BitmapFactory.decodeByteArray(b, 0, m.getEmbeddedPicture().length, null);
            else
                t.icon = BitmapFactory.decodeResource(mainActivity.getResources(), R.drawable.mainicon);
            t.id = GIDC++;
        }
        return t;
    }

    private boolean compareStrings(String haystack, String needle) {
        return Pattern.compile(Pattern.quote(needle), Pattern.CASE_INSENSITIVE).matcher(haystack).find();
    }

    private List<data_song> sortSongsByTitle(List<data_song> s) {
        List<data_song> ret = new ArrayList<>();
        ListSorter ls = new ListSorter();
        try {
            ret = ls.sort(gc, s, Constants.SORT_BYTITLE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    private List<data_song> sortSongsByArtist(List<data_song> s) {
        List<data_song> ret = new ArrayList<>();
        ListSorter ls = new ListSorter();
        try {
            ret = ls.sort(gc, s, Constants.SORT_BYARTIST);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    private List<data_song> getSongsfromMediaStore() {
        List<data_song> ret = new ArrayList<>();
        Cursor c = gc.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{
                MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.DURATION
        }, null, null, null);
        while (c.moveToNext()) {
            data_song t = new data_song();
            t.Title = c.getString(c.getColumnIndex(MediaStore.Audio.Media.TITLE));
            t.Artist = c.getString(c.getColumnIndex(MediaStore.Audio.Media.ARTIST));
            t.Album = c.getString(c.getColumnIndex(MediaStore.Audio.Media.ALBUM));
            t.file = new File(c.getString(c.getColumnIndex(MediaStore.Audio.Media.DATA)));
            t.length = Long.parseLong(c.getString(c.getColumnIndex(MediaStore.Audio.Media.DURATION)));
            t.id = GIDC++;
            ret.add(t);
        }
        return ret;
    }

    private List<data_song> getSongsfromFiles() {
        List<data_song> ret = new ArrayList<>();
        for (String str : searchPaths) {
            Log.v(LOG_TAG, "Searching in Directory: " + str);
            List<data_song> te = getPlayListAsItems(str);
            if (te != null)
                ret.addAll(te);
        }
        return ret;
    }

    private data_playlist mergePlayLists(data_playlist orig, data_playlist adding) {
        Log.v(LOG_TAG, "MERGING PLAYLIST " + orig.Title + " WITH " + adding.Title);
        List<data_song> lst = new ArrayList<>();
        List<data_song> ax = orig.audio;
        List<data_song> bx = adding.audio;
        lst.addAll(ax);
        for (int i = 0; i < bx.size(); i++) {
            boolean exists = false;
            for (int y = 0; y < ax.size(); y++) {
                if (ax.get(y).file.getAbsolutePath().equals(bx.get(i).file.getAbsolutePath())) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                lst.add(bx.get(i));
            }
        }
        data_playlist ret = new data_playlist(orig.Title, lst);
        ret.resid = orig.resid;
        ret.resid2 = orig.resid2;
        return ret;
    }

    private List<data_song> mergeLists(List<data_song> a, List<data_song> b) {
        List<data_song> ret = new ArrayList<>();
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

    private List<data_song> getPlayListAsItems(String rootPath) {
        Bitmap dicon = BitmapFactory.decodeResource(gc.getResources(), R.drawable.icon_unsetsong);
        List<File> t = getPlayListFiles(rootPath);
        List<data_song> ret = new ArrayList<>();
        for (int i = 0; i < t.size(); i++) {
            data_song s = getMetadata(t.get(i));
            ret.add(s);
        }
        return ret;
    }

    private ArrayList<File> getPlayListFiles(String rootPath) {
        ArrayList<File> fileList = new ArrayList<>();
        try {
            File rootFolder = new File(rootPath);
            File[] files = rootFolder.listFiles();
            if (files != null) {
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
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fileList;
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

    private void updateContent(data_playlist in) {
        if (in != null) {
            viewList.clear();
            viewList.addAll(in.audio);
            contentList.clear();
            contentList.addAll(in.audio);
            Log.v(LOG_TAG, "CONTENT LIST SET SIZE: " + contentList.size());
        }
    }

    private void updateContent() {
        if (!filesfound) {
            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    viewList.clear();
                    viewList.add(new data_song());
                }
            });
            return;
        } else if (PlayLists.get(pli) != null && PlayLists.get(pli).audio != null) {
            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    viewList.clear();
                    viewList.addAll(PlayLists.get(pli).audio);
                }
            });
            contentList.clear();
            contentList.addAll(PlayLists.get(pli).audio);
            Log.v(LOG_TAG, "CONTENT LIST SET SIZE: " + contentList.size());
        } else {
            Log.v(LOG_TAG, "ERROR: COULD NOT SELECT PLAYLIST / NULL");
        }
    }

    private boolean taskIsRunning = false;

    private boolean filtering = false;

    protected class LoadFilesTask extends AsyncTask<String, Integer, String> {
        @Override
        protected String doInBackground(String... params) {
            List<data_song> nw = getSongsfromFiles();
            Log.v(LOG_TAG, "FOUND " + nw.size() + " AUDIO FILES ON DISK");
            List<data_song> nnw = mergeLists(PlayLists.get("").audio, nw);
            PlayLists.get("").audio.clear();
            PlayLists.get("").audio.addAll(nnw);
            if (PlayLists.get("").audio.size() == 0) {
                Log.e(LOG_TAG, "NO FILES FOUND");
                filesfound = false;
                updateContent();
                mainActivity.notifyAAandOM();
                return "ERROR:NOFILESFOUND";
            } else {
                filesfound = true;
            }
            updateContent();
            sortContent(sortBy);
            try {
                while (mainActivity.serv == null) {
                    Thread.sleep(100);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            mainActivity.serv.setContent(contentList);
            if (filtering) {
                showFiltered(searchTerm, searchBy);
            } else {
                mainActivity.notifyAAandOM();
            }
            return "COMPLETE!";
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (taskIsRunning) {
                Log.e(LOG_TAG, "LOAD ASYNC TASK ALREADY RUNNING, CANCELING");
                cancel(false);
            } else {
                taskIsRunning = true;
            }
            Log.v(LOG_TAG, "LOAD ASYNC TASK ENTRY");
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            taskIsRunning = false;
            Log.v(LOG_TAG, "LOAD ASYNC TASK EXIT: " + result);
        }
    }

    public boolean searchtaskIsRunning = false;

    protected class SearchFilesTask extends AsyncTask<Context, Integer, String> {
        @Override
        protected String doInBackground(Context... params) {
            int srb = 0 + searchBy;
            String term = "" + searchTerm;
            if (contentList.size() == 0)
                return "ERROR: EMPTY CONTENT LIST";
            List<data_song> cl = new ArrayList<>(contentList);
            Log.v(LOG_TAG, "CONTENT SIZE:" + cl.size());
            List<data_song> flt = new ArrayList<>();
            switch (srb) {
                case Constants.SEARCH_BYTITLE:
                    Log.v(LOG_TAG, "SEARCH BY TITLE");
                    for (int i = 0; i < cl.size(); i++) {
                        if (compareStrings(cl.get(i).Title, term)) {
                            flt.add(cl.get(i));
                        }
                    }
                    break;
                case Constants.SEARCH_BYARTIST:
                    Log.v(LOG_TAG, "SEARCH BY ARTIST");
                    for (int i = 0; i < cl.size(); i++) {
                        if (compareStrings(cl.get(i).Artist, term)) {

                            flt.add(cl.get(i));
                        }
                    }
                    break;
                case Constants.SEARCH_BYBOTH:
                    Log.v(LOG_TAG, "SEARCH BY BOTH");
                    for (int i = 0; i < cl.size(); i++) {
                        if (compareStrings(cl.get(i).Title, term) || compareStrings(cl.get(i).Artist, term)) {
                            flt.add(cl.get(i));
                        }
                    }
                    break;
            }
            final List<data_song> inp = flt;
            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    viewList.clear();
                    viewList.addAll(inp);
                }
            });
            mainActivity.notifyAAandOM();
            return "COMPLETE!";
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (searchtaskIsRunning) {
                //TODO#POLISHING: Possible Race Condition
                Log.e(LOG_TAG, "SEARCH ASYNC TASK ALREADY RUNNING, CANCELING");
                cancel(false);
            } else {
                searchtaskIsRunning = true;
                Log.v(LOG_TAG, "SEARCH ASYNC TASK ENTRY");
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            searchtaskIsRunning = false;
            Log.v(LOG_TAG, "SEARCH ASYNC TASK EXIT: " + result);
        }
    }
}
