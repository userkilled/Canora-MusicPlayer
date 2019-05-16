package e.planet.musicman;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class PlayListManager {
    //Reference For ListView, Used to Manipulate the Shown Items, Always a Subset of contentList
    public List<data_song> viewList = new ArrayList<>();

    //Reference of the Currently Playing PlayList for the Service
    public List<data_song> contentList = new ArrayList<>();

    public PlayListManager(Context c, MainActivity b) {
        gc = c;
        mainActivity = b;
        plPath = mainActivity.getExternalFilesDir(null).getAbsolutePath() + "/PlayLists";
        Log.v(LOG_TAG, "PLAYLISTS FILE: " + plPath);
        setExtensionsAndSearchPaths();
    }

    //Public Callbacks
    public void loadContent() {
        List<data_song> ml = getSongsfromMediaStore();
        Log.v(LOG_TAG, "FOUND " + ml.size() + " SONGS IN MEDIASTORE");
        data_playlist t = new data_playlist("", ml);
        Map<String, data_playlist> tmp = getLocalPlayLists();//TODO:Load Titles / Artist / Album in separate Playlists
        tmp.put("", t);
        PlayLists.clear();
        PlayLists.putAll(tmp);
        updateContent();
        if (!taskIsRunning)
            new LoadFilesTask().execute(gc);
    }

    public boolean filtering = false;

    public void showFiltered(String term, int srb) {
        searchTerm = term;
        searchBy = srb;
        new SearchFilesTask().execute(gc);
    }

    public void sortContent(int SortBy) {
        this.sortBy = SortBy;
        if (contentList.size() == 0)
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
        Log.v(LOG_TAG, "UPDATE CONTENT PLI: " + pli);
        updateContent();
        sortContent(sortBy);
        if (contentList.size() > 0)
            mainActivity.serv.setContent(contentList);
        mainActivity.notifyArrayAdapter();
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

    public boolean checkPlayList(String name) {
        if (PlayLists.get(name) != null) {
            return true;
        } else
            return false;
    }

    public String getIndex() {
        return pli;
    }

    public void updateOptionsMenu(Menu m) {
        //TODO#REWRITE: Bad Practice to Modify the View from PLC
        m.findItem(R.id.action_addTo).getSubMenu().clear();
        m.findItem(R.id.action_addTo).getSubMenu().add(0, R.id.action_playlist_create, 0, R.string.menu_options_newplaylist).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                mainActivity.displayDialog(Constants.DIALOG_PLAYLIST_CREATE);
                return false;
            }
        });
        m.findItem(R.id.action_playlist_select).getSubMenu().clear();
        int plc = 0;
        for (Map.Entry<String, data_playlist> entry : PlayLists.entrySet()) {
            //ADDTO
            SubMenu sub = m.findItem(R.id.action_addTo).getSubMenu();
            if (entry.getValue().Title.length() != 0) {
                sub.add(0, plc, 1, entry.getValue().Title);
                sub.findItem(plc).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        data_playlist t = new data_playlist(item.getTitle().toString(), mainActivity.getSelected());
                        mainActivity.multiSelect();
                        updatePlayList(item.getTitle().toString(), t);
                        mainActivity.showSnackMessage(t.audio.size() + " " + mainActivity.getString(R.string.misc_added));
                        return false;
                    }
                });
            }
            entry.getValue().resid = plc;
            //SELECT
            plc++;
            sub = m.findItem(R.id.action_playlist_select).getSubMenu();
            if (entry.getValue().Title.length() == 0) {
                sub.add(0, plc, 0, R.string.misc_allfiles);//DEFAULT PLAYLIST
            } else {
                sub.add(0, plc, 1, entry.getValue().Title);
            }
            entry.getValue().resid2 = plc;
            sub.findItem(plc).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    for (Map.Entry<String, data_playlist> entry : PlayLists.entrySet()) {
                        if (item.getItemId() == entry.getValue().resid2) {
                            if (selectPlayList(entry.getValue().Title) > 0) {
                                Log.e(LOG_TAG, "ERROR SELECTING PLAYLIST");
                            } else {
                                mainActivity.invalidateOptionsMenu();
                            }
                            if (entry.getValue().Title.equals("")) {
                                mainActivity.getSupportActionBar().setDisplayShowHomeEnabled(true);
                                mainActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                                String hexColor = "#" + Integer.toHexString(mainActivity.getColorFromAtt(R.attr.colorText) & 0x00ffffff); //Because ANDROID
                                String t = "<font color='" + hexColor + "'>" + mainActivity.getString(R.string.app_name) + "</font>";
                                mainActivity.getSupportActionBar().setTitle(Html.fromHtml(t));
                            } else {
                                mainActivity.getSupportActionBar().setDisplayShowHomeEnabled(false);
                                mainActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                                String hexColor = "#" + Integer.toHexString(mainActivity.getColorFromAtt(R.attr.colorText) & 0x00ffffff); //Because ANDROID
                                String t = "<font color='" + hexColor + "'>" + entry.getValue().Title + "</font>";
                                mainActivity.getSupportActionBar().setTitle(Html.fromHtml(t));
                            }
                            sortContent(sortBy);
                            return true;
                        }
                    }
                    return false;
                }
            });
            //HIGHLIGHT
            if (entry.getKey().equals(pli)) {
                sub.findItem(plc).setCheckable(true).setChecked(true);
            } else {
                sub.findItem(plc).setCheckable(false).setChecked(false);
            }
            plc++;
            if (!pli.equals("")) {
                m.findItem(R.id.action_playlist_del).setVisible(true);
                m.findItem(R.id.action_playlist_del).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        mainActivity.displayDialog(Constants.DIALOG_PLAYLIST_DELETE);
                        return false;
                    }
                });
            } else {
                m.findItem(R.id.action_playlist_del).setVisible(false);
            }
        }
    }

    //Private Globals
    private Context gc;
    private MainActivity mainActivity;

    private int searchBy;
    private String searchTerm;

    private int sortBy;

    private String LOG_TAG = "PLC";

    private String plPath;

    private String pli = ""; //Current Index of PlayLists Indicating Currently Selected PlayList
    private Map<String, data_playlist> PlayLists = new HashMap<>(); //Holds ALL Content, Empty Index = All Files, otherwise Index = Name of PlayList

    private int GIDC = 0; //ID Counter for Song Items

    private List<String> validExtensions = new ArrayList<String>();
    private List<String> searchPaths = new ArrayList<>();

    //Private Functions

    //START Local Playlists
    private Map<String, data_playlist> getLocalPlayLists() {
        Map<String, data_playlist> ret = getDataAsMap(plPath);
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
        writeDataAsXML(plPath, in);
    }

    //XML Abstraction Layer
    private Map<String, data_playlist> getDataAsMap(String path) {
        Map<String, data_playlist> ret = new HashMap<>();
        String XMLSTR = readFromDisk(path);
        if (XMLSTR.length() > 0) {
            ret = getMapFromXML(XMLSTR);
        }
        return ret;
    }

    //TODO#REWRITE: Single Data Modification Factory
    private void writeDataAsXML(String path, Map<String, data_playlist> data) {
        if (data.size() > 0) {
            String writeStr = getXmlFromMap(data);
            writeToDisk(path, writeStr);
        }
    }

    //File System Abstraction Layer
    private String readFromDisk(String path) {
        File rf = new File(path);
        if (!rf.exists())
            return "";
        byte[] bFile = new byte[(int) rf.length()];
        String ret = "";
        try {
            //convert file into array of bytes
            FileInputStream fileInputStream = new FileInputStream(rf);
            fileInputStream.read(bFile);
            fileInputStream.close();
            ret = decompress(bFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    private void writeToDisk(String path, String data) {
        try {
            FileOutputStream fos = new FileOutputStream(new File(path));
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            byte[] tdat = compress(data);
            bos.write(tdat);
            bos.flush();
            bos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Compression
    public byte[] compress(String string) throws IOException {
        Log.v(LOG_TAG, "PLAYLISTS DECOMPRESSED SIZE: " + string.getBytes().length + " BYTES");
        ByteArrayOutputStream os = new ByteArrayOutputStream(string.length());
        GZIPOutputStream gos = new GZIPOutputStream(os);
        gos.write(string.getBytes());
        gos.close();
        byte[] compressed = os.toByteArray();
        os.close();
        Log.v(LOG_TAG, "PLAYLISTS COMPRESSED SIZE: " + compressed.length + " BYTES");
        return compressed;
    }

    public String decompress(byte[] compressed) throws IOException {
        Log.v(LOG_TAG, "PLAYLISTS COMPRESSED SIZE: " + compressed.length + " BYTES");
        final int BUFFER_SIZE = 32;
        ByteArrayInputStream is = new ByteArrayInputStream(compressed);
        GZIPInputStream gis = new GZIPInputStream(is, BUFFER_SIZE);
        StringBuilder string = new StringBuilder();
        byte[] data = new byte[BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = gis.read(data)) != -1) {
            string.append(new String(data, 0, bytesRead));
        }
        gis.close();
        is.close();
        Log.v(LOG_TAG, "PLAYLISTS DECOMPRESSED SIZE: " + string.toString().getBytes().length + " BYTES");
        return string.toString();
    }

    //Conversion
    private String getXmlFromMap(Map<String, data_playlist> in) {
        String ret = "<?xml version=\"1.0\"?><playlists>";
        for (Map.Entry<String, data_playlist> entry : in.entrySet()) {
            if (entry.getKey() == "")
                continue;
            ret += "<playlist title=\"" + entry.getKey() + "\">";
            for (int i = 0; i < entry.getValue().audio.size(); i++) {
                File f = entry.getValue().audio.get(i).file;
                if (f != null && f.exists())
                    ret += "<song>" + f.getAbsolutePath() + "</song>";
            }
            ret += "</playlist>";
        }
        ret += "</playlists>";
        return ret;
    }

    private Map<String, data_playlist> getMapFromXML(String xml) {
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

    //END Local Playlists
    //Other
    private data_song getMetadata(File f) {
        data_song t = new data_song();
        //Log.v(LOG_TAG, "Getting Metadata for File: " + f.getAbsolutePath());
        Cursor c = gc.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{
                MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.DURATION
        }, null, null, null);
        while (c.moveToNext()) {
            if (c.getString(c.getColumnIndex(MediaStore.Audio.Media.DATA)).equals(f.getAbsolutePath())) {
                t.Title = c.getString(c.getColumnIndex(MediaStore.Audio.Media.TITLE));
                t.Artist = c.getString(c.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                t.Album = c.getString(c.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                t.file = new File(c.getString(c.getColumnIndex(MediaStore.Audio.Media.DATA)));
                t.length = Long.parseLong(c.getString(c.getColumnIndex(MediaStore.Audio.Media.DURATION)));
                t.id = GIDC++;
            }
        }
        c.close();
        if (t.file == null) {
            Log.e(LOG_TAG, "FILE " + f.getAbsolutePath() + " NOT FOUND IN MEDIASTORE, FALLING BACK TO MEDIA-METADATA-RETRIEVER (SLOW PERFORMANCE)");
            t.file = f;
            MediaMetadataRetriever m = new MediaMetadataRetriever();
            m.setDataSource(f.getAbsolutePath());
            t.Title = m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            t.Artist = m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            t.length = Long.parseLong(m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        }
        return t;
    }

    private boolean compareStrings(String haystack, String needle) {
        return Pattern.compile(Pattern.quote(needle), Pattern.CASE_INSENSITIVE).matcher(haystack).find();
    }

    private List<data_song> sortSongsByTitle(List<data_song> s) {
        List<data_song> ret = new ArrayList<>();
        ListSorter ls = new ListSorter();
        ret = ls.sort(gc, s, Constants.SORT_BYTITLE);
        return ret;
    }

    private List<data_song> sortSongsByArtist(List<data_song> s) {
        List<data_song> ret = new ArrayList<>();
        ListSorter ls = new ListSorter();
        ret = ls.sort(gc, s, Constants.SORT_BYARTIST);
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
            if (!lst.contains(bx.get(i))) {
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

    private void updateContent() {
        if (PlayLists.get(pli).audio != null) {
            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    viewList.clear();
                    viewList.addAll(PlayLists.get(pli).audio);
                }
            });
            contentList.clear();
            contentList.addAll(PlayLists.get(pli).audio);
            Log.v(LOG_TAG,"CONTENT LIST SET SIZE: " + contentList.size());
        } else {
            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    viewList.clear();
                    viewList.addAll(PlayLists.get("").audio);
                }
            });
            contentList.clear();
            contentList.addAll(PlayLists.get("").audio);
            Log.v(LOG_TAG,"CONTENT LIST SET SIZE: " + contentList.size());
        }
    }

    public boolean taskIsRunning = false;

    protected class LoadFilesTask extends AsyncTask<Context, Integer, String> {
        @Override
        protected String doInBackground(Context... params) {
            List<data_song> nw = getSongsfromFiles();
            Log.v(LOG_TAG, "FOUND " + nw.size() + " AUDIO FILES ON DISK");
            List<data_song> nnw = mergeLists(PlayLists.get("").audio, nw);
            PlayLists.get("").audio.clear();
            PlayLists.get("").audio.addAll(nnw);
            updateContent();
            if (contentList.size() == 0) {
                Log.e(LOG_TAG, "NO FILES FOUND");
            }
            sortContent(mainActivity.sortBy);
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
                mainActivity.notifyArrayAdapter();
            }
            return "COMPLETE!";
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.v(LOG_TAG, "LOAD ASYNC START");
            if (taskIsRunning) {
                Log.e(LOG_TAG, "LOAD ASYNC TASK ALREADY RUNNING, CANCELING");
                cancel(false);
            } else
                taskIsRunning = true;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Log.v(LOG_TAG, "LOAD ASYNC END: " + result);
            taskIsRunning = false;
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
            List<data_song> flt = new ArrayList<>();
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
            final List<data_song> inp = flt;
            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    viewList.clear();
                    viewList.addAll(inp);
                }
            });
            mainActivity.notifyArrayAdapter();
            return "COMPLETE!";
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.v(LOG_TAG, "SEARCH ASYNC START");
            if (searchtaskIsRunning) {
                //TODO#POLISHING: Possible Race Condition
                Log.e(LOG_TAG, "SEARCH ASYNC TASK ALREADY RUNNING, CANCELING");
                cancel(false);
            } else
                searchtaskIsRunning = true;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Log.v(LOG_TAG, "SEARCH ASYNC END: " + result);
            searchtaskIsRunning = false;
        }
    }
}
