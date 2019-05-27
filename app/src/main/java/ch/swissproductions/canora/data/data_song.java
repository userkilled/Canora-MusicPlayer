package ch.swissproductions.canora.data;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;

public class data_song {

    //TODO: Load Album Cover via MediaStore

    //Outputs
    public int id;

    public File file;
    public String Title;
    public String Artist;
    public String Album;
    public Bitmap icon;
    public long length;

    public boolean selected;//Item Selection State (Add to Playlist etc.)

    //Privates
    private Bitmap defIcon;
    private Context c;
    private Cursor cursor;
    private String LOG_TAG = "SONGITEM";

    public data_song() {
        selected = false;
    }

    public data_song(data_song in) {
        id = in.id;
        file = in.file;
        Title = in.Title;
        Artist = in.Artist;
        Album = in.Album;
        icon = in.icon;
        selected = false;
    }

    public data_song(Context co, File f, int i, Bitmap defaultIcon) {
        file = f;
        defIcon = defaultIcon;
        c = co;
        id = i;
        if (file != null) {
            ContentResolver cr = c.getContentResolver();

            Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            String selection = MediaStore.Audio.Media.DATA;
            String[] selectionArgs = {f.getAbsolutePath()};

            String[] projection = {MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.DURATION};//MediaStore.Audio.Albums.ALBUM_ART
            String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
            cursor = cr.query(uri, projection, selection + "=?", selectionArgs, sortOrder);

            String title = "";
            String artist = "";
            String album = "";
            long ltemp = 0;

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    int idIndex = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
                    title = cursor.getString(idIndex);
                    int adIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
                    artist = cursor.getString(adIndex);
                    int AdIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
                    album = cursor.getString(AdIndex);
                    int ldIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
                    ltemp = cursor.getLong(ldIndex);
                }
            }
            if (title == null || title == "")
                title = file.getName();
            setTitle(title);
            if (artist == null || artist == "")
                artist = "unknown";
            setArtist(artist);
            setAlbum(album);
            setIcon(defIcon);
            setLength(ltemp);
        }
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

    private void setLength(long l) {
        length = l;
    }
}
