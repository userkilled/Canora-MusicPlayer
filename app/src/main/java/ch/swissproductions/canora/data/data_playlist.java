package ch.swissproductions.canora.data;

import java.util.ArrayList;
import java.util.List;

public class data_playlist {
    public String Title;
    public List<data_song> audio = new ArrayList<>();

    public data_playlist(String title, List<data_song> in) {
        audio.clear();
        audio.addAll(in);
        Title = "" + title;
    }
}
