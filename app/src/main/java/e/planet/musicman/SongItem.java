package e.planet.musicman;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;

public class SongItem {

    //TODO: Load Album Cover via MediaStore

    //Outputs
    public File file;
    public String Title;
    public String Artist;
    public String Album;
    public Bitmap icon;

    //Privates
    private Bitmap defIcon;
    private Context c;
    private Cursor cursor;
    private String LOG_TAG = "SONGITEM";

    public SongItem(Context co, File f, Bitmap defaultIcon) {
        PerformanceTimer p = new PerformanceTimer();
        file = f;
        defIcon = defaultIcon;
        c = co;
        if (file != null) {
            ContentResolver cr = c.getContentResolver();

            Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            String selection = MediaStore.Audio.Media.DATA;
            String[] selectionArgs = {f.getAbsolutePath()};

            String[] projection = {MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM};//MediaStore.Audio.Albums.ALBUM_ART
            String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
            cursor = cr.query(uri, projection, selection + "=?", selectionArgs, sortOrder);

            String title = "";
            String artist = "";
            String album = "";

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    int idIndex = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
                    title = cursor.getString(idIndex);
                    int adIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
                    artist = cursor.getString(adIndex);
                    int AdIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
                    album = cursor.getString(AdIndex);
                }
            }
            p.printStep(LOG_TAG, "Building Cursor");
            if (title == null || title == "")
                title = file.getName();
            setTitle(title);
            p.printStep(LOG_TAG, "GetTitle");
            if (artist == null || artist == "")
                artist = "unknown";
            setArtist(artist);
            p.printStep(LOG_TAG, "GetArtist");
            setAlbum(album);
            p.printStep(LOG_TAG, "GetAlbum");
            setIcon(defIcon);
            p.printStep(LOG_TAG, "GetIcon");
        }
        p.printTotal(LOG_TAG, "SongItemBuilt");
    }

    public SongItem(Context co, File f) {
        PerformanceTimer p = new PerformanceTimer();
        p.start();
        file = f;
        defIcon = null;
        c = co;
        if (file != null) {
            ContentResolver cr = c.getContentResolver();

            Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            String selection = MediaStore.Audio.Media.DATA;
            String[] selectionArgs = {f.getAbsolutePath()};

            String[] projection = {MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM};//MediaStore.Audio.Albums.ALBUM_ART
            String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
            cursor = cr.query(uri, projection, selection + "=?", selectionArgs, sortOrder);

            String title = "";
            String artist = "";
            String album = "";

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    int idIndex = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
                    title = cursor.getString(idIndex);
                    int adIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
                    artist = cursor.getString(adIndex);
                    int AdIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
                    album = cursor.getString(AdIndex);
                }
            }
            p.printStep(LOG_TAG, "Building Cursor");
            if (title == null || title == "")
                title = file.getName();
            setTitle(title);
            p.printStep(LOG_TAG, "GetTitle");
            if (artist == null || artist == "")
                artist = "unknown";
            setArtist(artist);
            p.printStep(LOG_TAG, "GetArtist");
            setAlbum(album);
            p.printStep(LOG_TAG, "GetAlbum");
            setIcon(defIcon);
            p.printStep(LOG_TAG, "GetIcon");
        }
        p.printTotal(LOG_TAG, "SongItemBuilt");
    }

    private void setTitle(String s) {
        Title = s;
    }

    private void setArtist(String s) {
        Artist = s;
    }

    private void setAlbum(String s) {
        Album = s;
    }

    private void setIcon(Bitmap p) {
        icon = p;
    }
}
