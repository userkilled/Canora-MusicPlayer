package e.planet.musicman;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;

import java.io.File;

public class SongItem {
    public File file;

    public String Title;
    public String Artist;
    public String Album;

    public Bitmap icon;

    private Bitmap defIcon;

    private MediaMetadataRetriever mmr = new MediaMetadataRetriever();

    public SongItem(File f, Bitmap defaultIcon)
    {
        file = f;
        defIcon = defaultIcon;
        if (file != null)
        {
            Title = getTitle(f);
            Artist = getArtist(f);
            Album = getAlbum(f);
            icon = getIcon(f);
        }
    }

    public SongItem(File f)
    {
        file = f;
        defIcon = null;
        if (file != null) {
            Title = getTitle(f);
            Artist = getArtist(f);
            Album = getAlbum(f);
            icon = getIcon(f);
        }
    }

    private String getTitle(File f)
    {
        String ret = "";
        mmr.setDataSource(f.getAbsolutePath());
        if (mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) != null)
        {
            ret = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        }
        else
        {
            ret = f.getName();
        }
        return ret;
    }
    private String getArtist(File f)
    {
        String ret = "";
        if (mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) != null)
        {
            ret = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
        }
        else
        {
            ret = "unknown";
        }
        return ret;
    }
    private String getAlbum(File f)
    {
        String ret = "";
        if (mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) != null)
        {
            ret = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
        }
        else
        {
            ret = "unknown";
        }
        return ret;
    }
    private Bitmap getIcon(File f)
    {
        Bitmap ret = null;
        if (mmr.getEmbeddedPicture() != null)
        {
            byte[] art = mmr.getEmbeddedPicture();
            ret = BitmapFactory.decodeByteArray(art,0,art.length);
        }
        else
        {
            ret = defIcon;
        }
        return ret;
    }
}
