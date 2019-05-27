package ch.swissproductions.canora.data;

import java.util.ArrayList;
import java.util.List;

public class data_playlist {
    public String Title;
    public List<data_song> audio = new ArrayList<>();
    public int resid; //RESOURCE ID OF ADD TO OPTIONS ITEM IN THE ACTIONBAR, Needed Because the Option Menu Items Get Generated Dynamically at Runtime
    public int resid2; //RESOURCE ID OF SELECT OPTIONS ITEM IN THE ACTIONBAR

    public data_playlist(String title, List<data_song> in) {
        audio.clear();
        audio.addAll(in);
        Title = "" + title;
    }
}
