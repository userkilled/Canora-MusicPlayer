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
        plm = new PlayBackManager();
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
    private MediaPlayer player;
    private int position; //Position of Media Player in Miliseconds

    private final IBinder mBinder = new LocalBinder();
    private BroadcastReceiver brcv;

    private NotificationManagerCompat nfm;
    private int notificationID = 1;

    private PlayBackManager plm;

    private String LOG_TAG = "SERV";

    //Settings
    private boolean repeatSong;
    private boolean playing;

    private float volume = 0.8f;

    public MusicPlayerService() {
    }

    //Binder
    public class LocalBinder extends Binder {
        MusicPlayerService getService() {
            return MusicPlayerService.this;
        }
    }

    //Public Control Functions
    public int init(List<ItemSong> in) {
        Log.v(LOG_TAG, "Init called, ");
        if (in != null) {
            plm.setContent(in);
            return 0;
        } else {
            Log.v(LOG_TAG, "Files are Null");
            return 1;
        }
    }

    public int reload(List<ItemSong> pl) {
        Log.v(LOG_TAG, "Reload Called");
        //Called when Content Changes
        plm.setContent(pl);
        return 0;
    }

    public int play(int id) {
        ItemSong n = plm.setNext(id);
        if (n != null) {
            Log.v(LOG_TAG, "Setting up Song: " + n.Title);
            createPlayer(n.file.getAbsolutePath());
            showNotification();
            return 0;
        }
        Log.v(LOG_TAG, "ERROR");
        return 1;
    }

    public int next() {
        Log.v(LOG_TAG, "Next");
        ItemSong n = plm.getNext();
        if (n != null) {
            Log.v(LOG_TAG, "Setting up Song: " + n.Title);
            createPlayer(n.file.getAbsolutePath());
            showNotification();
            return 0;
        }
        Log.v(LOG_TAG, "ERROR");
        return 1;
    }

    public int previous() {
        Log.v(LOG_TAG, "Previous");
        ItemSong n = plm.getPrev();
        if (n != null) {
            Log.v(LOG_TAG, "Setting up Song: " + n.Title);
            createPlayer(n.file.getAbsolutePath());
            showNotification();
            return 0;
        }
        Log.v(LOG_TAG, "ERROR");
        return 1;
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

    public int seek(int ms) {
        if (player != null) {
            position = ms;
            player.seekTo(ms);
        }
        return 0;
    }

    public void stop() {
        Log.v(LOG_TAG, "Stop");
        if (player != null) {
            player.stop();
        }
        plm.currentSong = null;
    }

    public boolean switchShuffle(boolean state) {
        plm.shuffle = state;
        return state;
    }

    public boolean switchShuffle() {
        if (plm.shuffle) {
            plm.shuffle = false;
            return false;
        } else {
            plm.shuffle = true;
            return true;
        }
    }

    public boolean switchRepeat(boolean state) {
        repeatSong = state;
        return state;
    }

    public boolean switchRepeat() {
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

    public ItemSong getCurrentSong() {
        return plm.currentSong;
    }

    public int getCurrentPosition()
    {
        if (player != null)
            return player.getCurrentPosition();
        return 0;
    }

    public int getDuration()
    {
        if (player != null)
            return player.getDuration();
        return 0;
    }

    //Private Functions
    private void handleMediaController() {
        nfm = NotificationManagerCompat.from(this);
        showNotification();
    }

    private void showNotification() {
        Map<String, String> md = new HashMap<>();
        if (plm.currentSong != null) {
            md.put("TITLE", plm.currentSong.Title);
            md.put("ARTIST", plm.currentSong.Artist);
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
                .addAction(R.drawable.main_btnprev, "prev", retrievePlaybackAction(3));

        if (player != null && player.isPlaying())
            nb.addAction(R.drawable.main_btnpause, "pause", retrievePlaybackAction(1));
        else
            nb.addAction(R.drawable.main_btnplay, "play", retrievePlaybackAction(1));
        nb.addAction(R.drawable.main_btnnext, "next", retrievePlaybackAction(2));

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

    private PendingIntent retrievePlaybackAction(int which) {
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

    private void createPlayer(String songP) {
        if (player != null) {
            Log.v(LOG_TAG, "Resetting Player");

            if (player.isPlaying())
                player.stop();

            player.reset();
            player.release();
        }
        songP = "file://" + songP;
        Log.v(LOG_TAG, "CREATING PLAYER: " + songP);
        player = MediaPlayer.create(getApplicationContext(), Uri.parse(songP));
        if (player != null) {
            player.start();
            playing = true;
            setVolume(volume);
            setListener();
        }
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

    private class PlayBackManager {
        /*
        Provides ItemSong
         */
        public ItemSong currentSong;

        public boolean shuffle;

        public PlayBackManager() {
            shuffle = false;
            history = new Stack<>();
            currentIndex = 0;
        }

        public ItemSong setNext(int id) {
            if (content.size() == 0) {
                Log.v(LOG_TAG,"CONTENT EMPTY");
                return null;
            }
            if (currentSong != null)
                history.push(currentSong);
            currentIndex = getIndexOfID(id);
            currentSong = content.get(currentIndex);
            return currentSong;
        }

        public ItemSong getNext() {
            if (content.size() == 0) {
                Log.v(LOG_TAG,"CONTENT EMPTY");
                return null;
            }
            if (currentSong != null)
                history.push(currentSong);
            if (shuffle) {
                Random rand = new Random();
                int t = rand.nextInt(content.size());
                //TODO: Shuffle Memory
                currentIndex = t;
                currentSong = content.get(currentIndex);
                return currentSong;
            } else {
                if (content.size() > 1 && currentIndex < content.size() - 1) {
                    currentIndex = currentIndex + 1;
                } else {
                    currentIndex = 0;
                }
                currentSong = content.get(currentIndex);
                return currentSong;
            }
        }

        public ItemSong getPrev() {
            if (content.size() == 0)
                return null;
            if (history.size() > 0) {
                String p = history.pop().file.getAbsolutePath();
                for (int i = 0; i < content.size(); i++) {
                    if (content.get(i).file.getAbsolutePath().equals(p)) {
                        currentIndex = i;
                        currentSong = content.get(i);
                        return content.get(i);
                    }
                }
                currentIndex = 0;
                currentSong = content.get(currentIndex);
                return content.get(currentIndex);
            } else {
                if (content.size() > 1 && currentIndex > 0)
                    currentIndex--;
                else
                    currentIndex = content.size() - 1;
                currentSong = content.get(currentIndex);
                return content.get(currentIndex);
            }
        }

        public void setContent(List<ItemSong> c) {
            content = c;
        }

        private Stack<ItemSong> history;

        private List<ItemSong> content;
        private int currentIndex;

        private int getIndexOfID(int id) {
            for (int i = 0; i < content.size(); i++) {
                if (content.get(i).id == id)
                    return i;
            }
            return 0;
        }

    }
}
