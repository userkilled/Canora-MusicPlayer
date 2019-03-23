package e.planet.musicman;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.util.*;

public class playerService extends Service {
    //Callbacks
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void onStartCommand() {

    }

    public void onCreate() {
        Arrays.fill(songHistory, -1);
    }

    public void onDestroy() {
        if (player != null) {
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
    float volume = 0.2f;

    String LOG_TAG = "SERV";

    Random rand = new Random();

    //Functions
    public void init(File[] files, int c) {
        Log.v(LOG_TAG, "Init called, " + c + " Files Found.");
        if (files != null) {
            songs = files;
            songCount = c;
        } else {
            Log.v(LOG_TAG, "Files are Null");
        }
        songPos = -1;
    }

    public void play(int id, Button btn) {
        boolean pisp = false;
        position = 0;
        if (player != null)
            pisp = player.isPlaying();
        if (songs[id] != null && songs[id].exists()) {
            Log.v(LOG_TAG, "Setting up Song: " + songs[id].getName());
            if (player != null) {
                player.stop();
                player.reset();
                player.release();
            }
            createPlayer(songs[id].getAbsolutePath());
            songPos = id;
            handleHistory(true);
            if (!pisp)
                btn.getBackground().setColorFilter(getResources().getColor(R.color.green), PorterDuff.Mode.MULTIPLY);
        }
    }

    public void pauseResume(Button btn) {
        if (player != null) {
            if (player.isPlaying()) {
                btn.getBackground().setColorFilter(getResources().getColor(R.color.colorBtns), PorterDuff.Mode.MULTIPLY);
                pause();
            } else {
                btn.getBackground().setColorFilter(getResources().getColor(R.color.green), PorterDuff.Mode.MULTIPLY);
                resume();
            }
        }
    }

    public void pause() {
        Log.v(LOG_TAG, "Pause");
        if (player != null) {
            player.pause();
            position = player.getCurrentPosition();
        }
    }

    public void resume() {
        Log.v(LOG_TAG, "Resume");
        if (player != null) {
            if (position > 0) {
                player.seekTo(position);
                player.start();
                setListener();
                setVolume(volume);
            } else {
                player.start();
                setListener();
                setVolume(volume);
            }
        }
    }

    public void next() {
        Log.v(LOG_TAG, "Next");
        if (player != null) {
            position = 0;
            if (shuffle) {
                player.stop();
                player.reset();
                player.release();
                List<Integer> tmp = new ArrayList<Integer>();
                for (int i : songHistory) {
                    tmp.add(i);
                }
                songPos = rand.nextInt(songCount - 1);
                while (tmp.contains(songPos) && songCount > 5)
                    songPos = rand.nextInt(songCount - 1);
                createPlayer(songs[songPos].getAbsolutePath());
                handleHistory(true);
                Log.v(LOG_TAG, "Playing: " + songs[songPos].getName());
            } else {
                player.stop();
                player.reset();
                player.release();
                if (songPos < songCount - 1) {
                    songPos++;
                } else {
                    songPos = 0;
                }
                createPlayer(songs[songPos].getAbsolutePath());
                handleHistory(true);
                Log.v(LOG_TAG, "Playing: " + songs[songPos].getName());
            }
        }
    }

    public void previous() {
        if (player != null) {
            position = 0;
            if (songHistory[1] != -1) {
                player.stop();
                player.reset();
                player.release();
                handleHistory(false);
                createPlayer(songs[songHistory[0]].getAbsolutePath());
                songPos = songHistory[0];
                Log.v(LOG_TAG, "Playing: " + songs[songPos].getName());
            } else  {
                player.stop();
                player.reset();
                player.release();
                if (songPos > 0) {
                    songPos--;
                } else {
                    songPos = songCount - 1;
                }
                createPlayer(songs[songPos].getAbsolutePath());
                Log.v(LOG_TAG, "Playing: " + songs[songPos].getName());
            }
        }
    }

    public void enableShuffle(Button btn) {
        if (shuffle) {
            btn.getBackground().setColorFilter(getResources().getColor(R.color.colorBtns), PorterDuff.Mode.MULTIPLY);
            shuffle = false;
        } else {
            btn.getBackground().setColorFilter(getResources().getColor(R.color.green), PorterDuff.Mode.MULTIPLY);
            shuffle = true;
        }
    }

    public void broadcastNewSong() {
        if (player != null) {
            Intent in = new Intent("com.musicman.NEWSONG");
            Bundle extras = new Bundle();
            extras.putInt("dur", player.getDuration());
            extras.putInt("pos", player.getCurrentPosition());
            in.putExtras(extras);
            sendBroadcast(in);
        }
    }

    public void setSongDisplay(TextView txt) {
        String txts = "";
        if (songPos != -1)
            txts = songs[songPos].getName();
        if (txts.length() > 39) {
            txts = txts.substring(0, 36);
            txts += "...";
        }
        txt.setText(txts);
    }

    public void handleHistory(boolean add) {
        Log.v(LOG_TAG, "Handle History Called. SongPos: " + songPos);
        if (add) {
            songHistory[5] = songHistory[4];
            songHistory[4] = songHistory[3];
            songHistory[3] = songHistory[2];
            songHistory[2] = songHistory[1];
            songHistory[1] = songHistory[0];
            songHistory[0] = songPos;
        } else {
            songHistory[0] = songHistory[1];
            songHistory[1] = songHistory[2];
            songHistory[2] = songHistory[3];
            songHistory[3] = songHistory[4];
            songHistory[4] = songHistory[5];
            songHistory[5] = -1;
        }
        Log.v(LOG_TAG, "Song History: " + songHistory[0] + " " + songHistory[1] + " " + songHistory[2] + " " + songHistory[3] + " " + songHistory[4] + " " + songHistory[5]);
    }

    public void createPlayer(String songP) {
        player = MediaPlayer.create(getApplicationContext(), Uri.parse(songP));
        player.start();
        setVolume(volume);
        setListener();
    }

    //CompletionListener
    public void setListener() {
        Log.v(LOG_TAG, "setListener Called.");
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Log.v(LOG_TAG, "Completion Listener Called.");
                if (repeatSong) {
                    player.stop();
                    player.seekTo(0);
                    player.start();
                    setListener();
                    broadcastNewSong();
                } else {
                    next();
                    broadcastNewSong();
                }
            }
        });
    }

    public void setVolume(float vol) {
        volume = vol;
        player.setVolume(vol, vol);
    }

    public String getSongName()
    {
        return songs[songPos].getName();
    }

    //Binder
    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        playerService getService() {
            return playerService.this;
        }
    }
}
