package e.planet.musicman;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayListManager {

    public List<SongItem> songRef = new ArrayList<>(); //Only Instantiated Once, Reference For ListAdapter

    public List<SongItem> songRefPlay = new ArrayList<>(); //Currently Selected PlayList, DeCoupled from the ListAdapter songRef

    //TODO Optimize High Memory Usage
    private Map<String,List<SongItem>> playLists = new HashMap<>();

    public final String DefDisp = "1251590609012";

    private String key = null; //Currently Selected PlayList ,Null Equals FullSongList Display

    private Context c;

    String LOG_TAG = "PLM";

    public PlayListManager(Context con)
    {
        playLists = getLocalCache();
        c = con;
    }

    public List<String> getPlayListNames()
    {
        List<String> ret = new ArrayList<>();
        for (Map.Entry<String, List<SongItem>> entry : playLists.entrySet()) {
            ret.add(entry.getKey());
        }
        return ret;
    }

    public int updateFullList(List<SongItem> s)
    {
        if (playLists.get(DefDisp) != null) {
            playLists.get(DefDisp).clear();
            playLists.get(DefDisp).addAll(s);
        }
        else
        {
            playLists.put(DefDisp,s);
            Log.v(LOG_TAG,"PUT: " + playLists.get(DefDisp).size());
        }
        return 0;
    }

    public int selectPlayList(String name)
    {
        if (playLists.get(name) != null) {
            Log.v(LOG_TAG,"SETTING: " + playLists.get(name).size());
            songRef = playLists.get(name);
            Log.v(LOG_TAG,"songRef Size:" + songRef.size());
        }
            else
            return 1;
        return 0;
    }

    public int sortPlayList(String name, int sortBy)
    {
        Log.v(LOG_TAG,"SORTING");
        switch (sortBy)
        {
            case Constants.SORT_BYTITLE:
                if (playLists.get(name) != null)
                {
                    List<SongItem> tmp = new ArrayList<>(playLists.get(name));
                    playLists.get(name).clear();
                    playLists.get(name).addAll(sortSongsByTitle(tmp));
                }
                else
                {
                    playLists.put(name,sortSongsByTitle(playLists.get(name)));
                }
                break;
            case Constants.SORT_BYARTIST:
                if (playLists.get(name) != null)
                {
                    List<SongItem> tmp = new ArrayList<>(playLists.get(name));
                    playLists.get(name).clear();
                    playLists.get(name).addAll(sortSongsByArtist(tmp));
                }
                else
                {
                    playLists.put(name,sortSongsByArtist(playLists.get(name)));
                }
                break;
        }
        return 0;
    }

    public int createPlayList(String name,List<SongItem> items)
    {
        //add to playLists
        //write playLists
        return 0;
    }

    public int removePlayList(String name)
    {
        //remove from playLists
        //write playLists
        return 0;
    }

    private Map<String,List<SongItem>> getLocalCache()
    {
        Map<String,List<SongItem>> ret = new HashMap<>();
        //REad File XML Map Key = Name List = PlayList
        return ret;
    }

    private int writeLocalCache(Map<String,List<SongItem>> c)
    {
        return 0;
    }

    private List<SongItem> sortSongsByTitle(List<SongItem> s) {
        List<SongItem> ret = new ArrayList<>();
        ListSorter ls = new ListSorter();
        ret = ls.sort(c, s, Constants.SORT_BYTITLE);
        return ret;
    }

    private List<SongItem> sortSongsByArtist(List<SongItem> s) {
        List<SongItem> ret = new ArrayList<>();
        ListSorter ls = new ListSorter();
        ret = ls.sort(c, s, Constants.SORT_BYARTIST);
        return ret;
    }
}
