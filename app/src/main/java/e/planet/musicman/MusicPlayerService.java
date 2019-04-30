package e.planet.musicman;

import android.app.*;
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

    public void onCreate() {
        Log.v(LOG_TAG, "ONCREATE");
        Arrays.fill(songIDHistory, -1);
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
    private List<SongItem> songs; //Reference to pl.viewList
    private PlayListContainer pl;

    private int songPos; //Index of currently Playing Song
    private SongItem currSong; //Currently Playing Song
    private int position; //Position of Playing Song in Miliseconds
    private int songCount; //Number of Songs Added

    private int[] songIDHistory = new int[6]; //Holds The IDS of The Songs

    private String LOG_TAG = "SERV";

    //Settings
    private boolean shuffle;
    private boolean repeatSong;
    private boolean playing;

    private float volume = 0.8f;

    public MusicPlayerService() {
        songPos = -1;
    }

    //Binder
    public class LocalBinder extends Binder {
        MusicPlayerService getService() {
            return MusicPlayerService.this;
        }
    }

    //Public Control Functions
    public int init(PlayListContainer plv) {
        Log.v(LOG_TAG, "Init called, ");
        if (plv != null) {
            pl = plv;
            songs = pl.playList;
            songCount = songs.size();
            return 0;
        } else {
            Log.v(LOG_TAG, "Files are Null");
            return 1;
        }
    }

    public int reload() {
        Log.v(LOG_TAG, "Reload Called");
        if (currSong != null) {
            for (int i = 0; i < songs.size(); i++) {
                if (songs.get(i).id == currSong.id) {
                    songPos = i;
                    break;
                } else
                    songPos = 0;
            }
        }
        songCount = songs.size();
        return 0;
    }

    public boolean play(int id) {
        position = 0;
        int index = getIndexOfSongID(id);
        if (songs.get(index) != null) {
            Log.v(LOG_TAG, "Setting up Song: " + songs.get(index).Title);
            songPos = index;
            if (player != null) {
                player.stop();
                player.reset();
                player.release();
            }
            currSong = new SongItem(songs.get(index));
            createPlayer(songs.get(index).file.getAbsolutePath());
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
                Log.v(LOG_TAG, "SHUFFLING");
                player.stop();
                player.reset();
                player.release();
                List<Integer> tmp = new ArrayList<Integer>();
                for (int i : songIDHistory) {
                    tmp.add(i);
                }
                songPos = rand.nextInt(songCount - 1);
                while (tmp.contains(songs.get(songPos).id) && songCount > 5)
                    songPos = rand.nextInt(songCount - 1);
                currSong = new SongItem(songs.get(songPos));
                createPlayer(songs.get(songPos).file.getAbsolutePath());
                showNotification();
                handleHistory(true);
                Log.v(LOG_TAG, "Playing: " + songs.get(songPos).Title);
            } else {
                Log.v(LOG_TAG, "NOT SHUFFLING");
                player.stop();
                player.reset();
                player.release();
                if (songPos < songCount - 1) {
                    songPos++;
                } else {
                    songPos = 0;
                }
                currSong = new SongItem(songs.get(songPos));
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
            if (songIDHistory[1] != -1) {
                player.stop();
                player.reset();
                player.release();
                handleHistory(false);
                currSong = new SongItem(songs.get(getIndexOfSongID(songIDHistory[0])));
                createPlayer(songs.get(getIndexOfSongID(songIDHistory[0])).file.getAbsolutePath());
                songPos = getIndexOfSongID(songIDHistory[0]);
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
                currSong = new SongItem(songs.get(songPos));
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

    public boolean getPlaybackStatus() {
        if (player != null) {
            if (player.isPlaying())
                return true;
        }
        return false;
    }

    public SongItem getCurrentSong() {
        return currSong;
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

        Intent oncloseIntent = new Intent(ACTION_QUIT);
        PendingIntent onclosepi = PendingIntent.getBroadcast(this.getApplicationContext(), 0, oncloseIntent, 0);
        nb.setDeleteIntent(onclosepi);

        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        String NOTIFICATION_CHANNEL_ID = "420";

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "NOTIFICATION_CHANNEL_NAME", importance);

            nb.setChannelId(NOTIFICATION_CHANNEL_ID);
            mNotifyMgr.createNotificationChannel(notificationChannel);
        }

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
            Intent in = new Intent(ACTION_STATUS_NEWSONG);
            Bundle extras = new Bundle();
            extras.putInt("dur", player.getDuration());
            extras.putInt("pos", player.getCurrentPosition());
            in.putExtras(extras);
            sendBroadcast(in);
            if (player.isPlaying())
                broadcast(ACTION_STATUS_PLAYING);
            else
                broadcast(ACTION_STATUS_PAUSED);
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
            songIDHistory[5] = songIDHistory[4];
            songIDHistory[4] = songIDHistory[3];
            songIDHistory[3] = songIDHistory[2];
            songIDHistory[2] = songIDHistory[1];
            songIDHistory[1] = songIDHistory[0];
            songIDHistory[0] = songs.get(songPos).id;
        } else {
            songIDHistory[0] = songIDHistory[1];
            songIDHistory[1] = songIDHistory[2];
            songIDHistory[2] = songIDHistory[3];
            songIDHistory[3] = songIDHistory[4];
            songIDHistory[4] = songIDHistory[5];
            songIDHistory[5] = -1;
        }
        Log.v(LOG_TAG, "Song History: " + songIDHistory[0] + " " + songIDHistory[1] + " " + songIDHistory[2] + " " + songIDHistory[3] + " " + songIDHistory[4] + " " + songIDHistory[5]);
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

    private int getIndexOfSongID(int id) {
        for (int i = 0; i < songs.size(); i++) {
            if (songs.get(i).id == id)
                return i;
        }
        return -1;
    }

    private void registerReceiver() {
        brcv = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case ACTION_QUIT:
                        stopSelf();
                    case android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY:
                        pause();
                        broadcast(ACTION_STATUS_PAUSED);
                        break;
                    case ACTION_TOGGLE_PLAYBACK:
                        pauseResume();
                        if (player != null && player.isPlaying())
                            broadcast(ACTION_STATUS_PLAYING);
                        else
                            broadcast(ACTION_STATUS_PAUSED);
                        break;
                    case ACTION_NEXT:
                        next();
                        broadcastNewSong();
                        break;
                    case ACTION_PREV:
                        previous();
                        broadcastNewSong();
                }
            }
        };
        IntentFilter flt = new IntentFilter();
        flt.addAction(ACTION_TOGGLE_PLAYBACK);
        flt.addAction(ACTION_NEXT);
        flt.addAction(ACTION_PREV);
        flt.addAction(ACTION_QUIT);
        flt.addAction(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(brcv, flt);
    }
}
