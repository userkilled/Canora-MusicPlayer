package e.planet.musicman;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.util.*;

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
        Arrays.fill(songHistory,-1);
    }
    public void onDestroy()
    {
        if (player != null)
        {
            player.stop();
            player.release();
        }
    }
    //Custom Code

    //Globals
    MediaPlayer player;
    int position; //Position of Playing Song in Miliseconds

    File[] songs; //Song Files SortedBeforehand by a Map

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
        if (files != null) {
            songs = files;
            songCount = c;
        }
        else
        {
            Log.v(LOG_TAG,"Files are Null");
        }
    }
    public void play(int id, Button btn)
    {
        boolean pisp = false;
        position = 0;
        if (player != null)
            pisp = player.isPlaying();
        if (songs[id] != null && songs[id].exists()) {
            Log.v(LOG_TAG, "Setting up Song: " + songs[id].getName());
            if (player != null) {
                player.stop();
                player.release();
            }
            player = MediaPlayer.create(this, Uri.parse(songs[id].getAbsolutePath()));
            songPos = id;
            handleHistory(true);
            setListener();
            if (!pisp)
                btn.getBackground().setColorFilter(getResources().getColor(R.color.green), PorterDuff.Mode.MULTIPLY);
            player.start();
        }
    }
    public void pauseResume(Button btn)
    {
        if (player != null) {
            if (player.isPlaying()) {
                btn.getBackground().setColorFilter(getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
                pause();
            } else {
                btn.getBackground().setColorFilter(getResources().getColor(R.color.green), PorterDuff.Mode.MULTIPLY);
                resume();
            }
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
        position = 0;
        boolean pisp = false;
        if (player != null)
            pisp = player.isPlaying();
        if (shuffle)
        {
            player.stop();
            player.release();
            List<Integer> tmp = new ArrayList<Integer>();
            for (int i : songHistory)
            {
                tmp.add(i);
            }
            songPos = rand.nextInt(songCount);
            while(tmp.contains(songPos))
                songPos = rand.nextInt(songCount);
            player = MediaPlayer.create(getBaseContext(), Uri.parse(songs[songPos].getAbsolutePath()));
            handleHistory(true);
            Log.v(LOG_TAG,"Playing: " + songs[songPos].getName());
            if (pisp)
                player.start();
            setListener();
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
            if (pisp)
                player.start();
            setListener();
        }
    }
    public void previous()
    {
        boolean pisp = false;
        position = 0;
        if (player != null)
            pisp = player.isPlaying();
        if (songHistory[1] != -1)
        {
            player.stop();
            player.release();
            handleHistory(false);
            player = MediaPlayer.create(getBaseContext(),Uri.parse(songs[songHistory[0]].getAbsolutePath()));
            songPos = songHistory[0];
            Log.v(LOG_TAG,"Playing: " + songs[songHistory[0]].getName());
            if (pisp)
                player.start();
            setListener();
        }
    }
    public void enableShuffle(Button btn)
    {
        if (shuffle) {
            btn.getBackground().setColorFilter(getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
            shuffle = false;
        }
        else
        {
            btn.getBackground().setColorFilter(getResources().getColor(R.color.green), PorterDuff.Mode.MULTIPLY);
            shuffle = true;
        }
    }
    public void setSongDisplay(TextView txt)
    {
        String txts = songs[songPos].getName();
        if (txts.length() > 39)
        {
            txts = txts.substring(0,36);
            txts += "...";
        }
        txt.setText(txts);
    }
    public void handleHistory(boolean add)
    {
        Log.v(LOG_TAG,"SongPos: " + songPos);
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
            songHistory[2] = songHistory[3];
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
                setVolume(0.5f);
            }
        });
    }
    public void setVolume(float vol)
    {
        player.setVolume(vol,vol);
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
