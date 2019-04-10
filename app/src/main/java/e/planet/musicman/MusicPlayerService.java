package e.planet.musicman;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.*;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.util.*;

import static e.planet.musicman.Constants.*;

public class MusicPlayerService extends Service {
    //Callbacks
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void onStartCommand() {
    }

    public void onCreate() {
        Log.v(LOG_TAG, "ONCREATE");
        Arrays.fill(songHistory, -1);
        registerReceiver();
        handleMediaController();
    }

    public void onDestroy() {
        Log.v(LOG_TAG, "ONDESTROY");
        if (player != null) {
            player.stop();
            player.release();
        }
        if (brcv != null) {
            unregisterReceiver(brcv);
            brcv = null;
        }
        if (nfm != null) {
            nfm.cancelAll();
        }
    }

    //Globals
    MediaPlayer player;

    private Random rand = new Random();

    private final IBinder mBinder = new LocalBinder();
    private BroadcastReceiver brcv;

    private NotificationManagerCompat nfm;
    private int notificationID = 1;

    /*The Player iterates over this Array of File handles depending on the Settings(Shuffle ,repeat)*/
    private List<SongItem> songs; //Song Files in Sorted Form

    private int position; //Position of Playing Song in Miliseconds
    private int songPos; //Index of currently Playing Song
    private int songCount; //Number of Songs Added

    private int[] songHistory = new int[6];

    private String LOG_TAG = "SERV";

    //Settings
    private boolean shuffle;
    private boolean repeatSong;
    private boolean playing;

    private float volume = 0.8f;

    //Binder

    public class LocalBinder extends Binder {
        MusicPlayerService getService() {
            return MusicPlayerService.this;
        }
    }

    //Public Control Functions

    public int init(List<SongItem> files, int c) {
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
        if (songs.get(id) != null) {
            Log.v(LOG_TAG, "Setting up Song: " + songs.get(id).Title);
            songPos = id;
            if (player != null) {
                player.stop();
                player.reset();
                player.release();
            }
            createPlayer(songs.get(id).file.getAbsolutePath());
            showNotification();
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
            playing = false;
            position = player.getCurrentPosition();
            showNotification();
        }
        return false;
    }

    public boolean resume() {
        Log.v(LOG_TAG, "Resume");
        if (player != null) {
            if (position > 0) {
                player.seekTo(position);
                player.start();
                playing = true;
                setListener();
                setVolume(volume);
                showNotification();
            } else {
                player.start();
                playing = true;
                setListener();
                setVolume(volume);
                showNotification();
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
                createPlayer(songs.get(songPos).file.getAbsolutePath());
                showNotification();
                handleHistory(true);
                Log.v(LOG_TAG, "Playing: " + songs.get(songPos).Title);
            } else {
                player.stop();
                player.reset();
                player.release();
                if (songPos < songCount - 1) {
                    songPos++;
                } else {
                    songPos = 0;
                }
                createPlayer(songs.get(songPos).file.getAbsolutePath());
                showNotification();
                handleHistory(true);
                Log.v(LOG_TAG, "Playing: " + songs.get(songPos).Title);
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
                createPlayer(songs.get(songHistory[0]).file.getAbsolutePath());
                songPos = songHistory[0];
                showNotification();
                Log.v(LOG_TAG, "Playing: " + songs.get(songPos).Title);
            } else {
                player.stop();
                player.reset();
                player.release();
                if (songPos > 0) {
                    songPos--;
                } else {
                    songPos = songCount - 1;
                }
                createPlayer(songs.get(songPos).file.getAbsolutePath());
                showNotification();
                Log.v(LOG_TAG, "Playing: " + songs.get(songPos).Title);
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

    public boolean enableRepeat() {
        if (repeatSong) {
            repeatSong = false;
            return false;
        } else {
            repeatSong = true;
            return true;
        }
    }

    public void setVolume(float vol) {
        volume = vol;
        if (player != null)
            player.setVolume(vol, vol);
    }

    public float getVolume() {
        return volume;
    }

    public boolean getPlayerStatus() {
        if (player != null) {
            if (player.isPlaying())
                return true;
        }
        return false;
    }

    public SongItem getCurrentSong() {
        if (songPos > -1 && songs != null) {
            return songs.get(songPos);
        } else {
            return null;
        }
    }

    //Private Functions

    private void handleMediaController() {
        nfm = NotificationManagerCompat.from(this);
        showNotification();
    }

    private void showNotification() {
        Map<String, String> md = new HashMap<>();
        if (songs != null) {
            md.put("TITLE", songs.get(songPos).Title);
            md.put("ARTIST", songs.get(songPos).Artist);
        } else {
            md.put("TITLE", "");
            md.put("ARTIST", "");
        }
        Notification.Builder nb = new Notification.Builder(this)
                .setShowWhen(false)
                .setStyle(new Notification.MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2))
                .setColor(0x020202)
                .setSmallIcon(R.drawable.notification_mainicon)
                .setContentTitle(md.get("TITLE"))
                .setContentText(md.get("ARTIST"))
                .addAction(R.drawable.main_btnprev, "prev", retreivePlaybackAction(3));

        if (player != null && player.isPlaying())
            nb.addAction(R.drawable.main_btnpause, "pause", retreivePlaybackAction(1));
        else
            nb.addAction(R.drawable.main_btnplay, "play", retreivePlaybackAction(1));
        nb.addAction(R.drawable.main_btnnext, "next", retreivePlaybackAction(2));
        Intent resultIntent = new Intent(this, MainActivity.class);
        resultIntent.setAction("android.intent.action.MAIN");
        resultIntent.addCategory("android.intent.category.LAUNCHER");
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        nb.setContentIntent(resultPendingIntent);
        Notification noti = nb.build();
        nfm.notify(notificationID, noti);
    }

    private PendingIntent retreivePlaybackAction(int which) {
        Intent action;
        PendingIntent pendingIntent;
        switch (which) {
            case 1:
                action = new Intent(ACTION_TOGGLE_PLAYBACK);
                PendingIntent playpi = PendingIntent.getBroadcast(this, 0, action, 0);
                pendingIntent = playpi;
                return pendingIntent;
            case 2:
                action = new Intent(ACTION_NEXT);
                PendingIntent nex = PendingIntent.getBroadcast(this, 0, action, 0);
                pendingIntent = nex;
                return pendingIntent;
            case 3:
                action = new Intent(ACTION_PREV);
                PendingIntent prevpi = PendingIntent.getBroadcast(this, 0, action, 0);
                pendingIntent = prevpi;
                return pendingIntent;
            default:
                break;
        }
        return null;
    }

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
        playing = true;
        setVolume(volume);
        setListener();
    }

    private void setListener() {
        Log.v(LOG_TAG, "setListener Called.");
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Log.v(LOG_TAG, "Completion Listener Called.");
                if (!playing)
                    return;
                if (repeatSong) {
                    mp.seekTo(0);
                    mp.start();
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
