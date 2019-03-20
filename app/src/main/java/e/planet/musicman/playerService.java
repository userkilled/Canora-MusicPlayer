package e.planet.musicman;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;

public class playerService extends Service {
    //Callbacks
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    public void onStartCommand()
    {

    }
    public void onCreate()
    {
        player = MediaPlayer.create(this,R.raw.music);
        Arrays.fill(songHistory,-1);
    }
    public void onDestroy()
    {
        player.stop();
        player.release();
    }
    //Custom Code

    //Globals
    MediaPlayer player;
    int position; //Position of Playing Song in Miliseconds

    File[] songs = new File[1000]; //Song Files SortedBeforehand by a Map

    int songPos; //Index of currently Playing Song
    int songCount; //Number of Songs Added

    int[] songHistory = new int[6];

    //Settings
    boolean shuffle;
    boolean repeatSong;
    boolean repeatAll;

    String LOG_TAG = "SERV";

    Random rand = new Random();

    //Functions
    public void init(File[] files, int c)
    {
        Log.v(LOG_TAG,"Init called");
        songs = files;
        songCount = c;
    }
    public void play(int id)
    {
        Log.v(LOG_TAG,"Setting up Song: " + songs[id].getName());
        if (player != null)
        {
            player.stop();
            player.release();
        }
        player = MediaPlayer.create(this, Uri.parse(songs[id].getAbsolutePath()));
        handleHistory(true);
        setListener();
        songPos = id;
    }
    public void pauseResume()
    {
        if (player.isPlaying())
        {
            pause();
        }
        else {
            resume();
        }
    }
    public void pause()
    {
        Log.v(LOG_TAG,"Pause");
        player.pause();
        position = player.getCurrentPosition();
    }
    public void resume()
    {
        Log.v(LOG_TAG,"Resume");
        if (position > 0)
        {
            player.seekTo(position);
            player.start();
        }
        else
        {
            player.start();
        }
    }
    public void next()
    {
        Log.v(LOG_TAG,"Next");
        if (shuffle)
        {
            player.stop();
            player.release();
            songPos = rand.nextInt(songCount);


            player = MediaPlayer.create(getBaseContext(), Uri.parse(songs[songPos].getAbsolutePath()));
            handleHistory(true);
            Log.v(LOG_TAG,"Playing: " + songs[songPos].getName());
            player.start();
        }
        else
        {
            if (songPos < songCount) {
                songPos++;
            }
            else
            {
                songPos = 0;
            }
            player.stop();
            player.release();
            player = MediaPlayer.create(getBaseContext(), Uri.parse(songs[songPos].getAbsolutePath()));
            handleHistory(true);
            Log.v(LOG_TAG,"Playing: " + songs[songPos].getName());
            player.start();
        }
    }
    public void previous()
    {
        if (songHistory[0] != -1)
        {
            player.stop();
            player.release();
            handleHistory(false);
            player = MediaPlayer.create(getBaseContext(),Uri.parse(songs[songHistory[0]].getAbsolutePath()));
            Log.v(LOG_TAG,"Playing: " + songs[songHistory[0]].getName());
            player.start();
            songPos = songHistory[0];
        }
    }
    public void handleHistory(boolean add)
    {
        if (add) {
            songHistory[5] = songHistory[4];
            songHistory[4] = songHistory[3];
            songHistory[3] = songHistory[2];
            songHistory[2] = songHistory[1];
            songHistory[1] = songHistory[0];
            songHistory[0] = songPos;
        }
        else
        {
            songHistory[0] = songHistory[1];
            songHistory[1] = songHistory[2];
            songHistory[3] = songHistory[4];
            songHistory[4] = songHistory[5];
            songHistory[5] = -1;
        }
        Log.v(LOG_TAG,"Song History: " + songHistory[0] + " " + songHistory[1] + " " + songHistory[2] + " " + songHistory[3] + " " + songHistory[4] + " " + songHistory[5]);
    }

    //CompletionListener
    public void setListener()
    {
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (repeatSong)
                {
                    player.stop();
                    player.seekTo(0);
                    player.start();
                }
                else if (shuffle)
                {
                    player.stop();
                    player.release();
                    songPos = rand.nextInt(songCount);
                    player = MediaPlayer.create(getBaseContext(), Uri.parse(songs[songPos].getAbsolutePath()));
                    player.start();
                }
                else
                {
                    player.stop();
                    if (songPos < songCount) {
                        songPos++;
                    }
                    else{
                        songPos = 0;
                    }
                    player.release();
                    player = MediaPlayer.create(getBaseContext(), Uri.parse(songs[songPos].getAbsolutePath()));
                    player.start();
                }
            }
        });
    }

    //Binder
    private final IBinder mBinder = new LocalBinder();
    public class LocalBinder extends Binder
    {
        playerService getService()
        {
            return playerService.this;
        }
    }
}
