package e.planet.musicman;

import java.util.ArrayList;
import java.util.List;

public class ItemPlayList {
    public String Title;
    public List<ItemSong> audio = new ArrayList<>();
    public ItemPlayList(String title,List<ItemSong> in)
    {
        audio.clear();
        audio.addAll(in);
        Title = "" + title;
    }
}
