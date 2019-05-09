package e.planet.musicman;

import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

public class ItemPlayList {
    public String Title;
    public List<ItemSong> audio = new ArrayList<>();
    public int resid; //RESOURCE ID OF ADD TO ITEM
    public int resid2; //RESOURCE ID OF SELECT ITEM

    public ItemPlayList(String title, List<ItemSong> in) {
        audio.clear();
        audio.addAll(in);
        Title = "" + title;
    }
}
