package ch.swissproductions.canora.data;

import java.util.ArrayList;
import java.util.List;

public class data_playlist {
    public String Title;
    public List<data_song> audio = new ArrayList<>();
    public int resid; //RESOURCE ID OF ADD TO OPTIONS ITEM
    public int resid2; //RESOURCE ID OF SELECT OPTIONS ITEM

    public data_playlist(String title, List<data_song> in) {
        audio.clear();
        audio.addAll(in);
        Title = "" + title;
    }
}
