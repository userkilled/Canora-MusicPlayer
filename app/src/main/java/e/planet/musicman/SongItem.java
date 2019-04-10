package e.planet.musicman;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;

public class SongItem {
    //TODO: Load Album Cover via MediaStore
    public File file;

    public String Title;
    public String Artist;
    public String Album;

    public Bitmap icon;

    private Bitmap defIcon;

    private Context c;

    private Cursor cursor;

    private String LOG_TAG = "SONGITEM";

    public SongItem(Context co, File f, Bitmap defaultIcon) {
        file = f;
        defIcon = defaultIcon;
        c = co;
        if (file != null) {
            ContentResolver cr = c.getContentResolver();

            Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            String selection = MediaStore.Audio.Media.DATA;
            String[] selectionArgs = {f.getAbsolutePath()};

            String[] projection = {MediaStore.Audio.Media.TITLE};
            String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
            cursor = cr.query(uri, projection, selection + "=?", selectionArgs, sortOrder);
            Title = getTitle(f);

            projection = new String[]{MediaStore.Audio.Media.ARTIST};
            sortOrder = MediaStore.Audio.Media.ARTIST + " ASC";
            cursor = cr.query(uri, projection, selection + "=?", selectionArgs, sortOrder);
            Artist = getArtist(f);

            projection = new String[]{MediaStore.Audio.Media.ALBUM};
            sortOrder = MediaStore.Audio.Media.ALBUM + " ASC";
            cursor = cr.query(uri, projection, selection + "=?", selectionArgs, sortOrder);
            Album = getAlbum(f);

            /*projection = new String[]{MediaStore.Audio.Albums.ALBUM_ART};
            sortOrder = MediaStore.Audio.Albums.ALBUM_ART + " ASC";
            cursor = cr.query(uri, projection, selection + "=?", selectionArgs, sortOrder);*/
            icon = getIcon(f);
        }
    }

    public SongItem(Context co, File f) {
        file = f;
        defIcon = null;
        c = co;
        if (file != null) {
            ContentResolver cr = c.getContentResolver();

            Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            String selection = MediaStore.Audio.Media.DATA;
            String[] selectionArgs = {f.getAbsolutePath()};

            String[] projection = {MediaStore.Audio.Media.TITLE};
            String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
            cursor = cr.query(uri, projection, selection + "=?", selectionArgs, sortOrder);
            Title = getTitle(f);

            projection = new String[]{MediaStore.Audio.Media.ARTIST};
            sortOrder = MediaStore.Audio.Media.ARTIST + " ASC";
            cursor = cr.query(uri, projection, selection + "=?", selectionArgs, sortOrder);
            Artist = getArtist(f);

            projection = new String[]{MediaStore.Audio.Media.ALBUM};
            sortOrder = MediaStore.Audio.Media.ALBUM + " ASC";
            cursor = cr.query(uri, projection, selection + "=?", selectionArgs, sortOrder);
            Album = getAlbum(f);

            /*projection = new String[]{MediaStore.Audio.Albums.ALBUM_ART};
            sortOrder = MediaStore.Audio.Albums.ALBUM_ART + " ASC";
            cursor = cr.query(uri, projection, selection + "=?", selectionArgs, sortOrder);*/
            icon = getIcon(f);
        }
    }

    private String getTitle(File f) {
        String ret = "";

        if (cursor != null) {
            //Log.v(LOG_TAG, "CURSOR NOT NULL");
            while (cursor.moveToNext()) {
                //Log.v(LOG_TAG, "MOVENEXT");
                int idIndex = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
                String t = cursor.getString(idIndex);
                if (t == "")
                    t = f.getName();
                return t;
            }
        } else {
            Log.v(LOG_TAG, "CURSOR NULL");
        }
        return ret;
    }

    private String getArtist(File f) {
        String ret = "";
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int idIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
                String t = cursor.getString(idIndex);
                //Log.v(LOG_TAG,"ARTIST: " + t);
                if (t == "")
                    t = f.getName();
                return t;
            }
        } else {
            Log.v(LOG_TAG, "CURSOR NULL");
        }
        return ret;
    }

    private String getAlbum(File f) {
        String ret = "";
        if (cursor != null) {
            //Log.v(LOG_TAG, "CURSOR NOT NULL");
            while (cursor.moveToNext()) {
                //Log.v(LOG_TAG, "MOVENEXT");
                int idIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
                String t = cursor.getString(idIndex);
                if (t == "")
                    t = f.getName();
                return t;
            }
        } else {
            Log.v(LOG_TAG, "CURSOR NULL");
        }
        return ret;
    }

    private Bitmap getIcon(File f) {
        Bitmap ret = defIcon;
        return ret;
        /*
        if (cursor != null) {
            Log.v(LOG_TAG,"CURSOR NOT NULL");
            while (cursor.moveToNext()) {
            }
        }
        else
        {

        }
        return ret;*/
    }
}
