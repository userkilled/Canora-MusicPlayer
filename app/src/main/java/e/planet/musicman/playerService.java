package e.planet.musicman;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.ActivityCompat;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
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

    public void onStartCommand() {
    }

    public void onCreate() {
        Arrays.fill(songHistory, -1);
        registerReceiver();
    }

    public void onDestroy() {
        if (player != null) {
            player.stop();
            player.release();
        }
    }

    //Globals
    MediaPlayer player;
    Random rand = new Random();
    private final IBinder mBinder = new LocalBinder();
    private BroadcastReceiver brcv;

    /*The Player iterates over this Array of File handles depending on the Settings(Shuffle ,repeat)*/
    File[] songs; //Song Files in Sorted Form

    int position; //Position of Playing Song in Miliseconds
    int songPos; //Index of currently Playing Song
    int songCount; //Number of Songs Added
    int[] songHistory = new int[6];

    String LOG_TAG = "SERV";

    //Settings
    boolean shuffle;
    boolean repeatSong;
    float volume = 0.2f;

    //Binder
    public class LocalBinder extends Binder {
        playerService getService() {
            return playerService.this;
        }
    }

    //Public Control Functions
    public int init(File[] files, int c) {
        Log.v(LOG_TAG, "Init called, " + c + " Files Found.");
        songPos = -1;
        if (files != null) {
            songs = files;
            songCount = c;
            return 0;
        } else {
            Log.v(LOG_TAG, "Files are Null");
            return 1;
        }
    }

    public boolean play(int id) {
        position = 0;
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
            return true;
        } else {
            if (!player.isPlaying())
                return false;
            else
                return true;
        }
    }

    public boolean pauseResume() {
        if (player != null) {
            if (player.isPlaying()) {
                return pause();
            } else {
                return resume();
            }
        }
        return false;
    }

    public boolean pause() {
        Log.v(LOG_TAG, "Pause");
        if (player != null) {
            player.pause();
            position = player.getCurrentPosition();
        }
        return false;
    }

    public boolean resume() {
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
            return true;
        }
        return false;
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
            } else {
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

    public boolean enableShuffle() {
        if (shuffle) {
            shuffle = false;
            return false;
        } else {
            shuffle = true;
            return true;
        }
    }

    public void setVolume(float vol) {
        volume = vol;
        if (player != null)
            player.setVolume(vol, vol);
    }

    public String getSongName() {
        if (songPos != -1)
            return songs[songPos].getName();
        else
            return "";
    }

    //Private Functions
    private void broadcastNewSong() {
        if (player != null) {
            Intent in = new Intent("com.musicman.NEWSONG");
            Bundle extras = new Bundle();
            extras.putInt("dur", player.getDuration());
            extras.putInt("pos", player.getCurrentPosition());
            in.putExtras(extras);
            sendBroadcast(in);
            if (player.isPlaying())
                broadcast("com.musicman.PLAYING");
            else
                broadcast("com.musicman.PAUSED");
        }
    }

    private void broadcast(String msg) {
        if (player != null) {
            Intent in = new Intent(msg);
            sendBroadcast(in);
        }
    }

    private void handleHistory(boolean add) {
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

    private void createPlayer(String songP) {
        player = MediaPlayer.create(getApplicationContext(), Uri.parse(songP));
        player.start();
        setVolume(volume);
        setListener();
    }

    private void setListener() {
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

    private void registerReceiver() {
        brcv = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("com.musicman.PLAYPAUSE")) {
                    pauseResume();
                    if (player != null && player.isPlaying())
                        broadcast("com.musicman.PLAYING");
                    else
                        broadcast("com.musicman.PAUSED");
                } else if (intent.getAction().equals(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
                    pause();
                    broadcast("com.musicman.PAUSED");
                } else if (intent.getAction().equals("com.musicman.NEXT")) {
                    next();
                    broadcastNewSong();
                } else if (intent.getAction().equals("com.musicman.PREV")) {
                    previous();
                    broadcastNewSong();
                }
            }
        };
        IntentFilter flt = new IntentFilter();
        flt.addAction("com.musicman.PLAYPAUSE");
        flt.addAction("com.musicman.NEXT");
        flt.addAction("com.musicman.PREV");
        flt.addAction(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(brcv, flt);
    }
}
