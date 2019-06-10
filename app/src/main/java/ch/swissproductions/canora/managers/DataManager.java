package ch.swissproductions.canora.managers;

import android.content.Context;
import android.database.Cursor;
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

public class DataManager {
    //Data Output For a Listview and Player Service, Manipulated by the various Control Functions
    public List<data_song> dataout = new ArrayList<>();

    public DataManager(Context c, MainActivity b, int SORTBY, String PlayListIndex) {
        gc = c;
        mainActivity = b;
        if (PlayListIndex != null)
            index = PlayListIndex;
        else
            index = "";
        String plPath = mainActivity.getFilesDir().getAbsolutePath() + "/PlayLists";
        fsa = new FileSystemAccessManager(plPath);
        sortBy = SORTBY;
        filesfound = true;
        setExtensionsAndSearchPaths();
    }

    //Public Callbacks
    public void loadPlaylists(String selected) {
        if (selected == null)
            index = "";
        else
            index = selected;
        if (!playlisttaskIsRunning) {
            plt = new PlayListTask();
            plt.execute();
        }
    }

    public void loadContentFromMediaStore() {
        List<data_song> ml = getSongsfromMediaStore();
        Log.v(LOG_TAG, "FOUND " + ml.size() + " SONGS IN MEDIASTORE");
        Tracks.audio.clear();
        Tracks.audio.addAll(ml);
        if (Tracks.audio.size() > 0)
            updateContent();
        Artists.clear();
        Albums.clear();
        Genres.clear();
        for (int i = 0; i < Tracks.audio.size(); i++) {
            data_song tr = Tracks.audio.get(i);
            if (Artists.containsKey(tr.Artist)) {
                Artists.get(tr.Artist).audio.add(tr);
            } else {
                List<data_song> t = new ArrayList<>();
                t.add(tr);
                data_playlist art = new data_playlist(tr.Artist, t);
                Artists.put(tr.Artist, art);
            }
            if (Albums.containsKey(tr.Album)) {
                Albums.get(tr.Album).audio.add(tr);
            } else {
                List<data_song> t = new ArrayList<>();
                t.add(tr);
                data_playlist art = new data_playlist(tr.Album, t);
                Albums.put(tr.Album, art);
            }
            if (Genres.containsKey(tr.Genre)) {
                Genres.get(tr.Genre).audio.add(tr);
            } else {
                List<data_song> t = new ArrayList<>();
                t.add(tr);
                data_playlist art = new data_playlist(tr.Genre, t);
                Genres.put(tr.Genre, art);
            }
        }
    }

    public void loadContentFromFiles() {
        if (!loadtaskIsRunning) {
            lft = new LoadFilesTask();
            lft.execute();
        }
    }

    public void sortContent(int SortBy) {
        this.sortBy = SortBy;
        if (dataout.size() == 0 || !filesfound)
            return;
        final List<data_song> srted;
        Log.v(LOG_TAG, "SORTING BY: " + sortBy);
        switch (sortBy) {
            case Constants.SORT_BYARTIST:
                Log.v(LOG_TAG, "SORTING BY ARTIST");
                srted = sortSongsByArtist(new ArrayList<>(dataout));
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dataout.clear();
                        dataout.addAll(srted);
                        mainActivity.notifyAAandOM();
                    }
                });
                break;
            case Constants.SORT_BYTITLE:
                Log.v(LOG_TAG, "SORTING BY TITLE");
                srted = sortSongsByTitle(new ArrayList<>(dataout));
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dataout.clear();
                        dataout.addAll(srted);
                        mainActivity.notifyAAandOM();
                    }
                });
                break;
        }
    }

    public int selectTracks() {
        selector = Constants.DATA_SELECTOR_STATICPLAYLISTS_TRACKS;
        index = "";
        Log.v(LOG_TAG, "SELECTING TRACKS");
        if (Tracks == null) {
            Log.e(LOG_TAG, "ERROR NO TRACKS FOUND");
            return 1;
        }
        updateContent();
        sortContent(sortBy);
        return 0;
    }

    public int selectArtist(String artist) {
        selector = Constants.DATA_SELECTOR_STATICPLAYLISTS_ARTISTS;
        index = artist;
        Log.v(LOG_TAG, "SELECTING TRACKS");
        if (Artists.get(index) == null) {
            Log.e(LOG_TAG, "ERROR NO TRACKS FOUND");
            return 1;
        }
        updateContent();
        sortContent(sortBy);
        return 0;
    }

    public int selectAlbum(String album) {
        selector = Constants.DATA_SELECTOR_STATICPLAYLISTS_ALBUMS;
        index = album;
        Log.v(LOG_TAG, "SELECTING TRACKS");
        if (Albums.get(album) == null) {
            Log.e(LOG_TAG, "ERROR NO TRACKS FOUND");
            return 1;
        }
        updateContent();
        sortContent(sortBy);
        return 0;
    }

    public int selectGenre(String genre) {
        selector = Constants.DATA_SELECTOR_STATICPLAYLISTS_GENRES;
        index = genre;
        Log.v(LOG_TAG, "SELECTING TRACKS");
        if (Genres.get(genre) == null) {
            Log.e(LOG_TAG, "ERROR NO TRACKS FOUND");
            return 1;
        }
        updateContent();
        sortContent(sortBy);
        return 0;
    }

    public int selectPlayList(String name) {
        selector = Constants.DATA_SELECTOR_PLAYLISTS;
        index = "" + name;
        Log.v(LOG_TAG, "SELECTING PLAYLIST: " + name);
        if (PlayLists.get(index) == null) {
            Log.e(LOG_TAG, "ERROR PLAYLIST NOT FOUND");
            return 1;
        }
        updateContent();
        sortContent(sortBy);
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
            Log.v(LOG_TAG, "PLAYLISTS NEW SIZE: " + PlayLists.size());
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
        return index;
    }

    public int getSelector() {
        return selector;
    }

    public Map<String, data_playlist> getPlayListsAsMap() {
        return PlayLists;
    }

    public List<String> getPlaylists() {
        List<String> ret = new ArrayList<>();
        for (Map.Entry<String, data_playlist> ent : PlayLists.entrySet()) {
            ret.add(ent.getKey());
        }
        return ret;
    }

    public List<String> getArtists() {
        List<String> ret = new ArrayList<>();
        for (Map.Entry<String, data_playlist> ent : Artists.entrySet()) {
            ret.add(ent.getKey());
        }
        return ret;
    }

    public List<String> getAlbums() {
        List<String> ret = new ArrayList<>();
        for (Map.Entry<String, data_playlist> ent : Albums.entrySet()) {
            ret.add(ent.getKey());
        }
        return ret;
    }

    public List<String> getGenres() {
        List<String> ret = new ArrayList<>();
        for (Map.Entry<String, data_playlist> ent : Genres.entrySet()) {
            ret.add(ent.getKey());
        }
        return ret;
    }

    public void cancelTasks() {
        if (plt != null)
            plt.cancel(true);
        if (lft != null)
            lft.cancel(true);
    }

    public boolean filesfound;

    //Private Globals
    private Context gc;
    private MainActivity mainActivity;
    private FileSystemAccessManager fsa;

    private int sortBy;

    private String LOG_TAG = "PLC";

    private int selector = Constants.DATA_SELECTOR_STATICPLAYLISTS_TRACKS; //Which Content is Selected
    private String index; //Index of the Selected Content

    private Map<String, data_playlist> PlayLists = new TreeMap<>();
    private Map<String, data_playlist> Artists = new TreeMap<>();
    private Map<String, data_playlist> Albums = new TreeMap<>();
    private Map<String, data_playlist> Genres = new TreeMap<>();

    private data_playlist Tracks = new data_playlist("Tracks", new ArrayList<data_song>());

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
        String writeStr = convertMAPtoXML(data);
        fsa.write(writeStr.getBytes());
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
            for (int i = 0; i < pln.getLength(); i++) {
                List<data_song> tmp = new ArrayList<>();
                NodeList sitm = pln.item(i).getChildNodes();
                for (int y = 0; y < sitm.getLength(); y++) {
                    data_song t = new data_song();
                    t.file = new File(sitm.item(y).getTextContent());
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
                MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media._ID
        }, null, null, null);
        boolean found = false;
        while (c != null && c.moveToNext()) {
            if (c.getString(c.getColumnIndex(MediaStore.Audio.Media.DATA)).equals(f.getAbsolutePath())) {
                t.Title = c.getString(c.getColumnIndex(MediaStore.Audio.Media.TITLE));
                t.Artist = c.getString(c.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                if (t.Artist == null)
                    t.Artist = mainActivity.getString(R.string.misc_unknown);
                t.Album = c.getString(c.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                if (t.Album == null)
                    t.Album = mainActivity.getString(R.string.misc_unknown);
                t.Genre = mainActivity.getString(R.string.misc_unknown);
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
            t.Album = m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
            if (t.Album == null)
                t.Album = mainActivity.getString(R.string.misc_unknown);
            t.Genre = m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE);
            if (t.Genre == null)
                t.Genre = mainActivity.getString(R.string.misc_unknown);
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
                MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media._ID
        }, null, null, null);
        while (c.moveToNext()) {
            data_song t = new data_song();
            t.Title = c.getString(c.getColumnIndex(MediaStore.Audio.Media.TITLE));
            t.Artist = c.getString(c.getColumnIndex(MediaStore.Audio.Media.ARTIST));
            if (t.Artist == null || t.Artist.length() == 0)
                t.Artist = mainActivity.getString(R.string.misc_unknown);
            t.Album = c.getString(c.getColumnIndex(MediaStore.Audio.Media.ALBUM));
            if (t.Album == null || t.Album.length() == 0)
                t.Album = mainActivity.getString(R.string.misc_unknown);
            t.Genre = mainActivity.getString(R.string.misc_unknown);
            t.file = new File(c.getString(c.getColumnIndex(MediaStore.Audio.Media.DATA)));
            t.length = Long.parseLong(c.getString(c.getColumnIndex(MediaStore.Audio.Media.DURATION)));
            t.id = GIDC++;
            ret.add(t);
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

    private void updateContent() {
        if (!filesfound) {
            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dataout.clear();
                    dataout.add(new data_song());
                }
            });
            return;
        } else {
            switch (selector) {
                case Constants.DATA_SELECTOR_PLAYLISTS:
                    if (PlayLists.get(index) == null || PlayLists.get(index).audio == null)
                        break;
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dataout.clear();
                            dataout.addAll(PlayLists.get(index).audio);
                        }
                    });
                    break;
                case Constants.DATA_SELECTOR_STATICPLAYLISTS_TRACKS:
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dataout.clear();
                            dataout.addAll(Tracks.audio);
                        }
                    });
                    break;
                case Constants.DATA_SELECTOR_STATICPLAYLISTS_ARTISTS:
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dataout.clear();
                            dataout.addAll(Artists.get(index).audio);
                        }
                    });
                    break;
                case Constants.DATA_SELECTOR_STATICPLAYLISTS_ALBUMS:
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dataout.clear();
                            dataout.addAll(Albums.get(index).audio);
                        }
                    });
                    break;
                case Constants.DATA_SELECTOR_STATICPLAYLISTS_GENRES:
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dataout.clear();
                            dataout.addAll(Genres.get(index).audio);
                        }
                    });
                    break;
                default:
                    Log.v(LOG_TAG, "ERROR: SELECTOR INVALID");
            }
        }
    }

    private boolean filtering = false;
    private boolean loadtaskIsRunning = false;
    private LoadFilesTask lft;

    protected class LoadFilesTask extends AsyncTask<String, Integer, String> {
        @Override
        protected String doInBackground(String... params) {
            sortContentLocal(sortBy);
            if (filtering) {
                mainActivity.vpm.showFiltered(mainActivity.vpm.searchTerm, mainActivity.vpm.searchBy);
            } else {
                mainActivity.notifyAAandOM();
            }
            List<data_song> nw = getSongsfromFiles();
            if (isCancelled())
                return "Cancelled";
            Log.v(LOG_TAG, "FOUND " + nw.size() + " AUDIO FILES ON DISK");
            List<data_song> nnw = mergeLists(Tracks.audio, nw);
            if (isCancelled())
                return "Cancelled";
            Tracks.audio.clear();
            Tracks.audio.addAll(nnw);
            if (Tracks.audio.size() == 0) {
                Log.e(LOG_TAG, "NO FILES FOUND");
                filesfound = false;
                updateContent();
                mainActivity.notifyAAandOM();
                return "ERROR:NOFILESFOUND";
            } else {
                filesfound = true;
            }
            updateContent();
            sortContentLocal(sortBy);
            try {
                while (mainActivity.serv == null) {
                    Thread.sleep(100);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            mainActivity.serv.setContent(dataout);
            if (filtering) {
                mainActivity.vpm.showFiltered(mainActivity.vpm.searchTerm, mainActivity.vpm.searchBy);
            } else {
                mainActivity.notifyAAandOM();
            }
            Artists.clear();
            Albums.clear();
            Genres.clear();
            for (int i = 0; i < Tracks.audio.size(); i++) {
                data_song tr = Tracks.audio.get(i);
                if (Artists.containsKey(tr.Artist)) {
                    Artists.get(tr.Artist).audio.add(tr);
                } else {
                    List<data_song> t = new ArrayList<>();
                    t.add(tr);
                    data_playlist art = new data_playlist(tr.Artist, t);
                    Artists.put(tr.Artist, art);
                }
                if (Albums.containsKey(tr.Album)) {
                    Albums.get(tr.Album).audio.add(tr);
                } else {
                    List<data_song> t = new ArrayList<>();
                    t.add(tr);
                    data_playlist art = new data_playlist(tr.Album, t);
                    Albums.put(tr.Album, art);
                }
                if (Genres.containsKey(tr.Genre)) {
                    Genres.get(tr.Genre).audio.add(tr);
                } else {
                    List<data_song> t = new ArrayList<>();
                    t.add(tr);
                    data_playlist art = new data_playlist(tr.Genre, t);
                    Genres.put(tr.Genre, art);
                }
            }
            mainActivity.vpm.reload();
            return "COMPLETE!";
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (loadtaskIsRunning) {
                Log.e(LOG_TAG, "LOAD ASYNC TASK ALREADY RUNNING, CANCELING");
                cancel(false);
            } else {
                loadtaskIsRunning = true;
            }
            Log.v(LOG_TAG, "LOAD ASYNC TASK ENTRY");
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            loadtaskIsRunning = false;
            Log.v(LOG_TAG, "LOAD ASYNC TASK EXIT: " + result);
        }

        private List<data_song> getSongsfromFiles() {
            List<data_song> ret = new ArrayList<>();
            for (String str : searchPaths) {
                if (isCancelled())
                    return null;
                Log.v(LOG_TAG, "Searching in Directory: " + str);
                List<data_song> te = getPlayListAsItems(str);
                if (isCancelled())
                    return null;
                if (te != null)
                    ret.addAll(te);
            }
            return ret;
        }

        private List<data_song> mergeLists(List<data_song> a, List<data_song> b) {
            List<data_song> ret = new ArrayList<>();
            if (a.size() > b.size()) {
                ret.addAll(a);
                for (int i = 0; i < b.size(); i++) {
                    if (isCancelled())
                        return null;
                    boolean ex = false;
                    for (int y = 0; y < a.size(); y++) {
                        if (isCancelled())
                            return null;
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
                    if (isCancelled())
                        return null;
                    boolean ex = false;
                    for (int y = 0; y < b.size(); y++) {
                        if (isCancelled())
                            return null;
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

        private List<data_song> getPlayListAsItems(String rootPath) {
            List<File> t = getPlayListFiles(rootPath);
            if (isCancelled())
                return null;
            List<data_song> ret = new ArrayList<>();
            for (int i = 0; i < t.size(); i++) {
                if (isCancelled())
                    return null;
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
                        if (isCancelled())
                            return null;
                        if (file.isDirectory()) {
                            if (getPlayListFiles(file.getAbsolutePath()) != null) {
                                fileList.addAll(getPlayListFiles(file.getAbsolutePath()));
                            } else {
                                if (isCancelled())
                                    return null;
                                break;
                            }
                        } else if (validExtensions.contains(getFileExtension(file))) {
                            if (isCancelled())
                                return null;
                            fileList.add(file);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return fileList;
        }

        private String getFileExtension(File file) {
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

        private void sortContentLocal(int SortBy) {
            sortBy = SortBy;
            List<data_song> srted;
            switch (sortBy) {
                case Constants.SORT_BYARTIST:
                    Log.v(LOG_TAG, "SORTING BY ARTIST");
                    srted = sortSongsByArtist(dataout);
                    final List<data_song> uit = srted;
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dataout.clear();
                            dataout.addAll(uit);
                        }
                    });
                    break;
                case Constants.SORT_BYTITLE:
                    Log.v(LOG_TAG, "SORTING BY TITLE");
                    srted = sortSongsByTitle(dataout);
                    final List<data_song> uit2 = srted;
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dataout.clear();
                            dataout.addAll(uit2);
                        }
                    });
                    break;
            }
        }
    }

    public boolean playlisttaskIsRunning = false;
    private PlayListTask plt;

    protected class PlayListTask extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... strings) {
            Map<String, data_playlist> tmp = getLocalPlayLists();
            if (selector == Constants.DATA_SELECTOR_PLAYLISTS && tmp.containsKey(index)) {
                //Prioritize the Selected Playlist
                for (int i = 0; i < tmp.get(index).audio.size(); i++) {
                    if (isCancelled())
                        return "CANCELLED";
                    tmp.get(index).audio.set(i, getMetadata(tmp.get(index).audio.get(i).file));
                }
                PlayLists.put(index, tmp.get(index));
                selectPlayList(index);
                mainActivity.notifyAAandOM();
            }
            for (Map.Entry<String, data_playlist> entry : tmp.entrySet()) {
                if (isCancelled())
                    return "CANCELLED";
                Log.v(LOG_TAG, "PROCESSING PLAYLIST: " + entry.getKey());
                if (entry.getKey() == "")
                    continue;
                for (int i = 0; i < entry.getValue().audio.size(); i++) {
                    if (isCancelled())
                        return "CANCELLED";
                    entry.getValue().audio.set(i, getMetadata(entry.getValue().audio.get(i).file));
                }
                PlayLists.put(entry.getKey(), entry.getValue());
                mainActivity.notifyAAandOM();
            }
            return "COMPLETE";
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (playlisttaskIsRunning)
                cancel(true);
            else
                playlisttaskIsRunning = true;
            Log.v(LOG_TAG, "PLAYLIST TASK ENTRY");
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            playlisttaskIsRunning = false;
            Log.v(LOG_TAG, "PLAYLIST TASK EXIT RESULT: " + result);
        }
    }
}
