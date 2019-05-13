package e.planet.musicman;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
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

import static java.security.AccessController.getContext;

public class PlayListManager {
    //Reference For ListView, Used to Manipulate the Shown Items, Always a Subset of contentList
    public List<ItemSong> viewList = new ArrayList<>();

    //Reference of the Currently Playing PlayList for the Service
    public List<ItemSong> contentList = new ArrayList<>();

    public PlayListManager(Context c, MainActivity b) {
        gc = c;
        mainActivity = b;
        plPath = mainActivity.getExternalFilesDir(null).getAbsolutePath() + "/PlayLists.xml";
        setExtensionsAndSearchPaths();
    }

    //Public Callbacks
    public void loadContent() {
        List<ItemSong> ml = getSongsfromMediaStore();
        Log.v(LOG_TAG, "FOUND " + ml.size() + " SONGS IN MEDIASTORE");
        ItemPlayList t = new ItemPlayList("", ml);
        Map<String, ItemPlayList> tmp = getLocalPlayLists();//TODO:Load Titles / Artist / Album in separate Playlists
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
        final List<ItemSong> srted;
        Log.v(LOG_TAG, "SORTING BY: " + sortBy);
        switch (sortBy) {
            case Constants.SORT_BYARTIST:
                Log.v(LOG_TAG, "SORTING BY ARTIST");
                srted = sortSongsByArtist(contentList);
                //Log.v(LOG_TAG, "First TITLE: " + srted.get(0).Title);
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
                //Log.v(LOG_TAG, "First TITLE: " + srted.get(0).Title);
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
        mainActivity.serv.reload();
        mainActivity.notifyArrayAdapter();
        return 0;
    }

    public int createPlayList(String name, ItemPlayList in) {
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

    public int updatePlayList(String name, ItemPlayList in) {
        Log.v(LOG_TAG, "UPDATE PLAYLIST: " + name);
        ItemPlayList n = mergePlayLists(PlayLists.get(name), in);
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
        m.findItem(R.id.action_addTo).getSubMenu().clear();
        m.findItem(R.id.action_addTo).getSubMenu().add(0, R.id.action_playlist_create, 0, "New Playlist").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                mainActivity.displayDialog(Constants.DIALOG_PLAYLIST_CREATE);
                return false;
            }
        });
        m.findItem(R.id.action_playlist_select).getSubMenu().clear();
        int plc = 0;
        for (Map.Entry<String, ItemPlayList> entry : PlayLists.entrySet()) {
            //ADDTO
            SubMenu sub = m.findItem(R.id.action_addTo).getSubMenu();
            if (entry.getValue().Title.length() != 0) {
                Log.v(LOG_TAG, "ADDING " + entry.getValue().Title + " to action addto");
                sub.add(0, plc, 1, entry.getValue().Title);
                sub.findItem(plc).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        Log.v(LOG_TAG, "CLICKED ITEM " + item.getTitle());
                        ItemPlayList t = new ItemPlayList(item.getTitle().toString(), mainActivity.getSelected());
                        mainActivity.multiSelect();
                        updatePlayList(item.getTitle().toString(), t);
                        mainActivity.showSnackMessage("Added " + t.audio.size() + " Items to " + t.Title);
                        return false;
                    }
                });
            }
            entry.getValue().resid = plc;
            //SELECT
            plc++;
            sub = m.findItem(R.id.action_playlist_select).getSubMenu();
            if (entry.getValue().Title.length() == 0) {
                Log.v(LOG_TAG, "ADDING DEFAULT to action select");
                sub.add(0, plc, 0, "DEFAULT");//DEFAULT PLAYLIST
            } else {
                Log.v(LOG_TAG, "ADDING " + entry.getValue().Title + " TO SELECT");
                sub.add(0, plc, 1, entry.getValue().Title);
            }
            entry.getValue().resid2 = plc;
            sub.findItem(plc).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    for (Map.Entry<String, ItemPlayList> entry : PlayLists.entrySet()) {
                        if (item.getItemId() == entry.getValue().resid2) {
                            if (selectPlayList(entry.getValue().Title) > 0) {
                                Log.v(LOG_TAG, "ERROR SELECTING PLAYLIST");
                            } else {
                                mainActivity.invalidateOptionsMenu();
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
                Log.v(LOG_TAG, "HIGHTLIGHTING ITEM: " + entry.getKey());
                sub.findItem(plc).setCheckable(true).setChecked(true);
            } else {
                Log.v(LOG_TAG, "HIDING ITEM: " + entry.getKey());
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
    private Map<String, ItemPlayList> PlayLists = new HashMap<>(); //Holds ALL Content, Empty Index = All Files, otherwise Index = Name of PlayList

    private int GIDC = 0; //ID Counter for Song Items

    private List<String> validExtensions = new ArrayList<String>();
    private List<String> searchPaths = new ArrayList<>();

    //Private Functions

    //START Local Playlists
    private Map<String, ItemPlayList> getLocalPlayLists() {
        Map<String, ItemPlayList> ret = getDataAsMap(plPath);
        if (ret.size() < 1) {
            Log.v(LOG_TAG, "NONE/CORRUPT PLAYLISTS FOUND");
        } else {
            for (Map.Entry<String, ItemPlayList> entry : ret.entrySet()) {
                Log.v(LOG_TAG, "FOUND PLAYLIST : " + entry.getKey() + " SIZE: " + entry.getValue().audio.size());
            }
        }
        return ret;
    }

    private void putLocalPlayLists(Map<String, ItemPlayList> in) {
        writeDataAsXML(plPath, in);
    }

    //XML Abstraction Layer
    private Map<String, ItemPlayList> getDataAsMap(String path) {
        Log.v(LOG_TAG, "GETDATA AS MAP");
        Map<String, ItemPlayList> ret = new HashMap<>();
        String XMLSTR = readFromDisk(path);
        if (XMLSTR.length() > 0) {
            ret = getMapFromXML(XMLSTR);
        }
        return ret;
    }

    private void writeDataAsXML(String path, Map<String, ItemPlayList> data) {
        if (data.size() > 0) {
            String writeStr = getXmlFromMap(data);
            writeToDisk(path, writeStr);
        }
    }

    //File System Abstraction Layer
    private String readFromDisk(String path) {
        Log.v(LOG_TAG, "READFROMDISK");
        File rf = new File(path);
        if (!rf.exists())
            return "";
        BufferedReader br;
        String ret = "";
        try {
            br = new BufferedReader(new FileReader(path));
            try {
                String tm;
                while ((tm = br.readLine()) != null) {
                    ret += tm;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.v(LOG_TAG, "XML RED FROM DISK: " + ret);
        return ret;
    }

    private void writeToDisk(String path, String data) {
        try {
            File tmp = new File(path);
            FileWriter fw = new FileWriter(tmp);
            Log.v(LOG_TAG, "WRITING: " + data);
            fw.write(data);
            fw.flush();
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Conversion
    private String getXmlFromMap(Map<String, ItemPlayList> in) {
        String ret = "<?xml version=\"1.0\"?><playlists>";
        for (Map.Entry<String, ItemPlayList> entry : in.entrySet()) {
            if (entry.getKey() == "")
                continue;
            ret += "<playlist title=\"" + entry.getKey() + "\">";
            for (int i = 0; i < entry.getValue().audio.size(); i++) {
                ret += "<song>" + entry.getValue().audio.get(i).file.getAbsolutePath() + "</song>";
            }
            ret += "</playlist>";
        }
        ret += "</playlists>";
        Log.v(LOG_TAG, "BUILT XML STRING: " + ret);
        return ret;
    }

    private Map<String, ItemPlayList> getMapFromXML(String xml) {
        Map<String, ItemPlayList> ret = new HashMap<>();
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
                List<ItemSong> tmp = new ArrayList<>();
                Log.v(LOG_TAG, "PLAYLIST NAME: " + pln.item(i).getAttributes().item(0).getTextContent());
                NodeList sitm = pln.item(i).getChildNodes();
                for (int y = 0; y < sitm.getLength(); y++) {
                    Log.v(LOG_TAG, "PATH: " + sitm.item(y).getTextContent());
                    ItemSong t = getMetadata(new File(sitm.item(y).getTextContent()));
                    tmp.add(t);
                }
                ItemPlayList p = new ItemPlayList(pln.item(i).getAttributes().item(0).getTextContent(), tmp);
                ret.put(p.Title, p);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    //END Local Playlists
    //Other
    private ItemSong getMetadata(File f) {
        //TODO:Get Metadata from mediastore
        ItemSong t = new ItemSong();
        Log.d(LOG_TAG, "Getting Metadata for File: " + f.getAbsolutePath());
        t.Title = f.getName();
        t.Artist = "unkown";
        t.file = new File(f.getAbsolutePath());
        t.id = GIDC++;
        /*
        Cursor c = gc.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{
                MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.DATA
        }, null, null, null);
        while (c.moveToNext()) {
            if (c.getString(c.getColumnIndex(MediaStore.Audio.Media.DATA)).equals(f.getAbsolutePath())) {
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
            }
        }
        c.close();
         */
        return t;
    }

    private boolean compareStrings(String haystack, String needle) {
        return Pattern.compile(Pattern.quote(needle), Pattern.CASE_INSENSITIVE).matcher(haystack).find();
    }

    private List<ItemSong> sortSongsByTitle(List<ItemSong> s) {
        List<ItemSong> ret = new ArrayList<>();
        ListSorter ls = new ListSorter();
        ret = ls.sort(gc, s, Constants.SORT_BYTITLE);
        return ret;
    }

    private List<ItemSong> sortSongsByArtist(List<ItemSong> s) {
        List<ItemSong> ret = new ArrayList<>();
        ListSorter ls = new ListSorter();
        ret = ls.sort(gc, s, Constants.SORT_BYARTIST);
        return ret;
    }

    private List<ItemSong> getSongsfromMediaStore() {
        List<ItemSong> ret = new ArrayList<>();
        Cursor c = gc.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{
                MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.DATA
        }, null, null, null);
        while (c.moveToNext()) {
            ItemSong t = new ItemSong();
            t.Title = c.getString(c.getColumnIndex(MediaStore.Audio.Media.TITLE));
            //Log.v(LOG_TAG, "TITLE: " + t.Title);
            t.Artist = c.getString(c.getColumnIndex(MediaStore.Audio.Media.ARTIST));
            //Log.v(LOG_TAG, "ARTIST: " + t.Artist);
            t.Album = c.getString(c.getColumnIndex(MediaStore.Audio.Media.ALBUM));
            //Log.v(LOG_TAG, "ALBUM: " + t.Album);
            t.file = new File(c.getString(c.getColumnIndex(MediaStore.Audio.Media.DATA)));
            //Log.v(LOG_TAG, "FILEPATH: " + t.file.getAbsolutePath());
            t.id = GIDC++;
            //Log.v(LOG_TAG, "ID: " + t.id);
            ret.add(t);
        }
        return ret;
    }

    private List<ItemSong> getSongsfromFiles() {
        List<ItemSong> ret = new ArrayList<>();
        for (String str : searchPaths) {
            Log.v(LOG_TAG, "Searching in Directory: " + str);
            List<ItemSong> te = getPlayListAsItems(str);
            if (te != null)
                ret.addAll(te);
        }
        return ret;
    }

    private ItemPlayList mergePlayLists(ItemPlayList orig, ItemPlayList adding) {
        Log.v(LOG_TAG, "MERGING PLAYLIST " + orig.Title + " WITH " + adding.Title);
        List<ItemSong> lst = new ArrayList<>();
        List<ItemSong> ax = orig.audio;
        List<ItemSong> bx = adding.audio;
        lst.addAll(ax);
        for (int i = 0; i < bx.size(); i++) {
            if (!lst.contains(bx.get(i))) {
                lst.add(bx.get(i));
            }
        }
        for (int i = 0; i < lst.size(); i++) {
            Log.v(LOG_TAG, "MERGED LIST ITEM: " + lst.get(i).file.getAbsolutePath());
        }
        ItemPlayList ret = new ItemPlayList(orig.Title, lst);
        ret.resid = orig.resid;
        ret.resid2 = orig.resid2;
        return ret;
    }

    private List<ItemSong> mergeLists(List<ItemSong> a, List<ItemSong> b) {
        List<ItemSong> ret = new ArrayList<>();
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

    private List<ItemSong> getPlayListAsItems(String rootPath) {
        Bitmap dicon = BitmapFactory.decodeResource(gc.getResources(), R.drawable.icon_unsetsong);
        List<File> t = getPlayListFiles(rootPath);
        List<ItemSong> ret = new ArrayList<>();
        for (int i = 0; i < t.size(); i++) {
            ItemSong s = new ItemSong(gc, t.get(i), GIDC++, dicon);
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
        }
    }

    public boolean taskIsRunning = false;

    protected class LoadFilesTask extends AsyncTask<Context, Integer, String> {
        @Override
        protected String doInBackground(Context... params) {
            List<ItemSong> nw = getSongsfromFiles();
            Log.v(LOG_TAG, "FOUND " + nw.size() + " AUDIO FILES ON DISK");
            List<ItemSong> nnw = mergeLists(PlayLists.get("").audio, nw);
            PlayLists.get("").audio.clear();
            PlayLists.get("").audio.addAll(nnw);
            updateContent();
            Log.v(LOG_TAG, "CONTENT SIZE AFTER MERGE: " + contentList.size());
            sortContent(mainActivity.sortBy);
            try {
                while (mainActivity.serv == null) {
                    Thread.sleep(100);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            mainActivity.serv.reload();
            if (filtering) {
                //Log.v(LOG_TAG,"FILTERING");
                showFiltered(searchTerm, searchBy);
            } else {
                //Log.v(LOG_TAG, "NOT FILTERING, DONE");
                mainActivity.notifyArrayAdapter();
            }
            return "COMPLETE!";
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.v(LOG_TAG, "LOAD ASYNC START");
            if (taskIsRunning) {
                Log.v(LOG_TAG, "LOAD ASYNC TASK ALREADY RUNNING, CANCELING");
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
                return "ERROR: CONTENT LIST SIZE EQUALS 0";
            List<ItemSong> flt = new ArrayList<>();
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
            final List<ItemSong> inp = flt;
            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    viewList.clear();
                    viewList.addAll(inp);
                }
            });
            mainActivity.serv.reload();
            mainActivity.notifyArrayAdapter();
            return "COMPLETE!";
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.v(LOG_TAG, "SEARCH ASYNC START");
            if (searchtaskIsRunning) {
                //TODO#POLISHING: Possible Race Condition
                Log.v(LOG_TAG, "SEARCH ASYNC TASK ALREADY RUNNING, CANCELING");
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
