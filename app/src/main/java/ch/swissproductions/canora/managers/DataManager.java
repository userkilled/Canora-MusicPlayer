package ch.swissproductions.canora.managers;

import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import ch.swissproductions.canora.tools.ListSorter;
import ch.swissproductions.canora.R;
import ch.swissproductions.canora.activities.MainActivity;
import ch.swissproductions.canora.data.Constants;
import ch.swissproductions.canora.data.data_playlist;
import ch.swissproductions.canora.data.data_song;
import ch.swissproductions.canora.tools.PerformanceTimer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class DataManager {
    /* Manages the Loading,Selection and Sorting of the Data Sets */

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
        PerformanceTimer lp = new PerformanceTimer();
        lp.start();
        Thread sn = new Thread() {
            @Override
            public void run() {
                List<data_song> ml = getSongsfromMediaStore();
                Log.v(LOG_TAG, "FOUND " + ml.size() + " SONGS IN MEDIASTORE");
                Tracks.audio.clear();
                Tracks.audio.addAll(ml);
            }
        };
        Thread an = new Thread() {
            @Override
            public void run() {
                Albums = getAlbumsfromMS();
            }
        };
        Thread gn = new Thread() {
            @Override
            public void run() {
                Genres = getGenresfromMS();
            }
        };
        ExecutorService exc = Executors.newCachedThreadPool();
        exc.execute(sn);
        exc.execute(an);
        exc.execute(gn);
        exc.shutdown();
        try {
            exc.awaitTermination(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        updateContent();
        lp.printStep(LOG_TAG, "LOADCONTENTFROMMS");
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
                dataout.clear();
                dataout.addAll(srted);
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mainActivity.notifyAAandOM();
                    }
                });
                break;
            case Constants.SORT_BYTITLE:
                Log.v(LOG_TAG, "SORTING BY TITLE");
                srted = sortSongsByTitle(new ArrayList<>(dataout));
                dataout.clear();
                dataout.addAll(srted);
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
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

    private String LOG_TAG = "DM";

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
                    if (!new File(sitm.item(y).getTextContent()).exists())
                        break;
                    data_song t = new data_song();
                    t.file = new File(sitm.item(y).getTextContent());
                    tmp.add(t);
                }
                data_playlist p = new data_playlist(pln.item(i).getAttributes().item(0).getTextContent(), tmp);
                if (p != null)
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
        if (!f.exists())
            return null;
        Cursor mediaCursor;
        String[] mediaProjection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION
        };
        mediaCursor = gc.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaProjection, null, null, null);
        boolean found = false;
        data_song t = new data_song();
        while (mediaCursor != null && mediaCursor.moveToNext()) {
            if (mediaCursor.getString(mediaCursor.getColumnIndex(MediaStore.Audio.Media.DATA)).equals(f.getAbsolutePath())) {
                t.Title = mediaCursor.getString(mediaCursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                t.Artist = mediaCursor.getString(mediaCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                if (t.Artist == null)
                    t.Artist = mainActivity.getString(R.string.misc_unknown);
                t.Album = mediaCursor.getString(mediaCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                if (t.Album == null)
                    t.Album = mainActivity.getString(R.string.misc_unknown);
                t.Genre = mainActivity.getString(R.string.misc_unknown);
                t.file = new File(mediaCursor.getString(mediaCursor.getColumnIndex(MediaStore.Audio.Media.DATA)));
                t.length = Long.parseLong(mediaCursor.getString(mediaCursor.getColumnIndex(MediaStore.Audio.Media.DURATION)));
                t.id = GIDC++;
                found = true;
            }
        }
        mediaCursor.close();
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
            t.Genre = (m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE));
            t.length = Long.parseLong(m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
            byte[] b = m.getEmbeddedPicture();
            if (b != null)
                t.icon = BitmapFactory.decodeByteArray(b, 0, m.getEmbeddedPicture().length, null);
            else
                t.icon = BitmapFactory.decodeResource(mainActivity.getResources(), R.drawable.notification_unsetsongicon);
            t.id = GIDC++;
        }
        return t;
    }

    private Map<String, data_playlist> getAlbumsfromMS() {
        Map<String, data_playlist> ret = new HashMap<>();
        Uri uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
        String[] columns = {android.provider.MediaStore.Audio.Albums._ID, android.provider.MediaStore.Audio.Albums.ALBUM};
        Cursor cursor = mainActivity.getApplicationContext().getContentResolver().query(uri, columns, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            final datainterface di = new datainterface(ret);
            class mythread extends Thread {
                private String title;
                private data_playlist out;
                private List<data_song> cache;

                public mythread(String tit, List<data_song> cacheP) {
                    title = tit;
                    cache = new ArrayList<>(cacheP);
                }

                @Override
                public void run() {
                    List<data_song> found = new ArrayList<>();
                    for (int i = 0; i < cache.size(); i++) {
                        if (cache.get(i).Album.equals(title)) {
                            found.add(cache.get(i));
                        }
                    }
                    out = new data_playlist(title, found);
                    di.putData(out.Title, out);
                }
            }
            List<String> names = new ArrayList<>();
            do {
                names.add(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM)));
            }
            while (cursor.moveToNext());
            cursor.close();
            String[] mediaProjection = {
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.ALBUM
            };
            Cursor mc = mainActivity.getApplicationContext().getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaProjection, null, null, null);
            List<data_song> cache = new ArrayList<>();
            if (mc != null) {
                while (mc.moveToNext()) {
                    data_song ds = new data_song();
                    ds.file = new File(mc.getString(mc.getColumnIndex(MediaStore.Audio.Media.DATA)));
                    ds.length = Long.parseLong(mc.getString(mc.getColumnIndex(MediaStore.Audio.Media.DURATION)));
                    ds.Title = mc.getString(mc.getColumnIndex(MediaStore.Audio.Media.TITLE));
                    ds.Artist = mc.getString(mc.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                    ds.Album = mc.getString(mc.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                    ds.id = GIDC++;
                    cache.add(ds);
                }
            }
            mc.close();
            try {
                ExecutorService exec = Executors.newCachedThreadPool();
                for (int i = 0; i < names.size(); i++) {
                    exec.execute(new mythread(names.get(i), cache));
                }
                exec.shutdown();
                while (!exec.awaitTermination(5, TimeUnit.MILLISECONDS)) {
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new TreeMap<>(ret);
    }

    private Map<String, data_playlist> getArtistsfromMS() {
        Map<String, data_playlist> ret = new HashMap<>();
        Uri uri = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;
        String[] columns = {MediaStore.Audio.Artists._ID, MediaStore.Audio.Artists.ARTIST};
        Cursor cursor = mainActivity.getApplicationContext().getContentResolver().query(uri, columns, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            final datainterface di = new datainterface(ret);
            class mythread extends Thread {
                private String title;
                private data_playlist out;
                private List<data_song> cache;

                public mythread(String tit, List<data_song> cacheP) {
                    title = tit;
                    cache = new ArrayList<>(cacheP);
                }

                @Override
                public void run() {
                    List<data_song> found = new ArrayList<>();
                    for (int i = 0; i < cache.size(); i++) {
                        if (cache.get(i).Artist.equals(title)) {
                            found.add(cache.get(i));
                        }
                    }
                    out = new data_playlist(title, found);
                    di.putData(out.Title, out);
                }
            }
            List<String> names = new ArrayList<>();
            do {
                names.add(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST)));
            }
            while (cursor.moveToNext());
            cursor.close();
            String[] mediaProjection = {
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.ALBUM
            };
            Cursor mc = mainActivity.getApplicationContext().getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaProjection, null, null, null);
            List<data_song> cache = new ArrayList<>();
            if (mc != null) {
                while (mc.moveToNext()) {
                    data_song ds = new data_song();
                    ds.file = new File(mc.getString(mc.getColumnIndex(MediaStore.Audio.Media.DATA)));
                    ds.length = Long.parseLong(mc.getString(mc.getColumnIndex(MediaStore.Audio.Media.DURATION)));
                    ds.Title = mc.getString(mc.getColumnIndex(MediaStore.Audio.Media.TITLE));
                    ds.Artist = mc.getString(mc.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                    ds.Album = mc.getString(mc.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                    ds.id = GIDC++;
                    cache.add(ds);
                }
            }
            mc.close();
            try {
                ExecutorService exec = Executors.newCachedThreadPool();
                for (int i = 0; i < names.size(); i++) {
                    exec.execute(new mythread(names.get(i), cache));
                }
                exec.shutdown();
                exec.awaitTermination(1, TimeUnit.MINUTES);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new TreeMap<>(ret);
    }

    private Map<String, data_playlist> getGenresfromMS() {
        Map<String, data_playlist> ret = new HashMap<>();
        Uri uri = MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI;
        String[] columns = {MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME};
        Cursor cursor = mainActivity.getApplicationContext().getContentResolver().query(uri, columns, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            final datainterface di = new datainterface(ret);
            class mythread extends Thread {
                private String title;
                private int genreID;
                private data_playlist out;

                public mythread(String tit, int genID) {
                    title = tit;
                    genreID = genID;
                }

                @Override
                public void run() {
                    String[] mediaProjection = {
                            MediaStore.Audio.Media._ID,
                            MediaStore.Audio.Media.ARTIST,
                            MediaStore.Audio.Media.ALBUM,
                            MediaStore.Audio.Media.TITLE,
                            MediaStore.Audio.Media.DATA,
                            MediaStore.Audio.Media.DURATION,
                            MediaStore.Audio.Media.ALBUM
                    };
                    Uri genu = MediaStore.Audio.Genres.Members.getContentUri("external", genreID);
                    String orderBy = android.provider.MediaStore.Audio.Media.TITLE;
                    Cursor mc = mainActivity.getApplicationContext().getContentResolver().query(genu, mediaProjection, null, null, orderBy);
                    List<data_song> titles = new ArrayList<>();
                    if (mc != null) {
                        while (mc.moveToNext()) {
                            data_song ds = new data_song();
                            ds.file = new File(mc.getString(mc.getColumnIndex(MediaStore.Audio.Media.DATA)));
                            ds.length = Long.parseLong(mc.getString(mc.getColumnIndex(MediaStore.Audio.Media.DURATION)));
                            ds.Title = mc.getString(mc.getColumnIndex(MediaStore.Audio.Media.TITLE));
                            ds.Artist = mc.getString(mc.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                            ds.Album = mc.getString(mc.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                            titles.add(ds);
                        }
                    }
                    out = new data_playlist(title, titles);
                    mc.close();
                    di.putData(out.Title, out);
                }
            }
            List<String> names = new ArrayList<>();
            List<Integer> ids = new ArrayList<>();
            do {
                names.add(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Genres.NAME)));
                ids.add(Integer.parseInt(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Genres._ID))));
            }
            while (cursor.moveToNext());
            cursor.close();
            try {
                ExecutorService exec = Executors.newCachedThreadPool();
                for (int i = 0; i < names.size(); i++) {
                    exec.execute(new mythread(names.get(i), ids.get(i)));
                }
                exec.shutdown();
                while (!exec.awaitTermination(5, TimeUnit.MILLISECONDS)) {
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new TreeMap<>(ret);
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
        Cursor mediaCursor;
        String[] mediaProjection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION
        };
        List<data_song> ret = new ArrayList<>();

        mediaCursor = gc.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaProjection, null, null, null);

        while (mediaCursor.moveToNext()) {
            data_song t = new data_song();
            t.Title = mediaCursor.getString(mediaCursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
            t.Artist = mediaCursor.getString(mediaCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
            if (t.Artist == null || t.Artist.length() == 0)
                t.Artist = mainActivity.getString(R.string.misc_unknown);
            t.Album = mediaCursor.getString(mediaCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
            if (t.Album == null || t.Album.length() == 0)
                t.Album = mainActivity.getString(R.string.misc_unknown);

            t.Genre = mainActivity.getString(R.string.misc_unknown);

            t.file = new File(mediaCursor.getString(mediaCursor.getColumnIndex(MediaStore.Audio.Media.DATA)));
            t.length = Long.parseLong(mediaCursor.getString(mediaCursor.getColumnIndex(MediaStore.Audio.Media.DURATION)));
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
        searchPaths.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath());
        searchPaths.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath());
    }

    private void updateContent() {
        if (!filesfound) {
            dataout.clear();
            dataout.add(new data_song());
            return;
        } else {
            switch (selector) {
                case Constants.DATA_SELECTOR_PLAYLISTS:
                    if (PlayLists.get(index) == null || PlayLists.get(index).audio == null)
                        break;
                    dataout.clear();
                    dataout.addAll(PlayLists.get(index).audio);
                    break;
                case Constants.DATA_SELECTOR_STATICPLAYLISTS_TRACKS:
                    dataout.clear();
                    dataout.addAll(Tracks.audio);
                    break;
                case Constants.DATA_SELECTOR_STATICPLAYLISTS_ARTISTS:
                    dataout.clear();
                    dataout.addAll(Artists.get(index).audio);
                    break;
                case Constants.DATA_SELECTOR_STATICPLAYLISTS_ALBUMS:
                    dataout.clear();
                    dataout.addAll(Albums.get(index).audio);
                    break;
                case Constants.DATA_SELECTOR_STATICPLAYLISTS_GENRES:
                    dataout.clear();
                    dataout.addAll(Genres.get(index).audio);
                    break;
                default:
                    Log.v(LOG_TAG, "ERROR: SELECTOR INVALID");
            }
        }
    }

    private class datainterface {
        private Map<String, data_playlist> ret;

        public datainterface(Map<String, data_playlist> datRef) {
            ret = datRef;
        }

        synchronized public void putData(String key, data_playlist d) {
            ret.put(key, d);
        }
    }

    private boolean loadtaskIsRunning = false;
    private LoadFilesTask lft;

    protected class LoadFilesTask extends AsyncTask<String, Integer, String> {
        //Refresh Files that are missing in the Mediastore, such as recently Downloaded etc.
        @Override
        protected String doInBackground(String... params) {
            List<File> nw = getSongsfromFiles();
            if (isCancelled())
                return "Cancelled";
            Log.v(LOG_TAG, "FOUND " + nw.size() + " AUDIO FILES ON DISK");
            class mediaLoader {
                public mediaLoader() {
                    queue = 0;
                }

                public void addQueue() {
                    queue++;
                }

                public void remQueue() {
                    queue--;
                }

                public boolean getStat() {
                    if (queue > 0)
                        return false;
                    else
                        return true;
                }

                private int queue;
            }
            final mediaLoader ml = new mediaLoader();
            boolean reload = false;
            for (int i = 0; i < nw.size(); i++) {
                boolean instore = false;
                for (int y = 0; y < Tracks.audio.size(); y++) {
                    if (Tracks.audio.get(y).file.getAbsolutePath().equals(nw.get(i).getAbsolutePath())) {
                        instore = true;
                        break;
                    }
                }
                if (!instore) {
                    Log.v(LOG_TAG, "RESCANNING: " + nw.get(i).getAbsolutePath());
                    reload = true;
                    ml.addQueue();
                    MediaScannerConnection.scanFile(mainActivity, new String[]{nw.get(i).getAbsolutePath()}, null, new MediaScannerConnection.OnScanCompletedListener() {
                        @Override
                        public void onScanCompleted(String path, Uri uri) {
                            ml.remQueue();
                        }
                    });
                }
            }
            if (reload) {
                Log.v(LOG_TAG, "REFRESHING MEDIASTORE");
                try {
                    //Wait for Mediastore Update to finish
                    while (!ml.getStat()) {
                        TimeUnit.MILLISECONDS.sleep(500);
                    }
                    loadContentFromMediaStore();
                } catch (Exception e) {
                    e.printStackTrace();
                    return "ERROR";
                }
                mainActivity.serv.setContent(dataout);
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mainActivity.vpm.reload();
                        if (mainActivity.isSearching) {
                            mainActivity.vpm.showFiltered(mainActivity.vpm.searchTerm, mainActivity.vpm.searchBy);
                        }
                    }
                });
            } else {
                Log.v(LOG_TAG, "NO MEDIASTORE REFRESH NEEDED");
            }
            return "COMPLETE";
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

        private List<File> getSongsfromFiles() {
            List<File> ret = new ArrayList<>();
            for (String str : searchPaths) {
                if (isCancelled())
                    return null;
                Log.v(LOG_TAG, "Searching in Directory: " + str);
                List<File> te = getPlayListFiles(str);
                if (isCancelled())
                    return null;
                if (te != null)
                    ret.addAll(te);
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
                    if (entry.getValue().audio.get(i).file.exists())
                        entry.getValue().audio.set(i, getMetadata(entry.getValue().audio.get(i).file));
                    else {
                        Log.e(LOG_TAG, "ERROR: PLAYLIST FILENOTFOUND " + tmp.get(index).audio.get(i).file.getAbsolutePath());
                        entry.getValue().audio.remove(i);
                        i--;
                    }
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
