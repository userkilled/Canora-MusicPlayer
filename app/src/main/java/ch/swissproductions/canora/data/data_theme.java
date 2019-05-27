package ch.swissproductions.canora.data;

public class data_theme {
    public String title;
    public Integer resID; //ID OF SPINNER ITEM, Needed because Spinner Items Get Dynamically Generated at Runtime

    public data_theme(String t, int r) {
        title = t;
        resID = r;
    }
}