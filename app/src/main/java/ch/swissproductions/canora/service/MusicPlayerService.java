package ch.swissproductions.canora.service;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.app.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.*;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import android.util.TypedValue;
import android.view.animation.LinearInterpolator;
import android.widget.RemoteViews;
import ch.swissproductions.canora.activities.MainActivity;
import ch.swissproductions.canora.application.MainApplication;
import ch.swissproductions.canora.tools.MediaPlayerEqualizer;
import ch.swissproductions.canora.R;
import ch.swissproductions.canora.data.data_song;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static ch.swissproductions.canora.data.Constants.*;

public class MusicPlayerService extends Service {
    //Overrides
    @Override
    public void onCreate() {
        Log.v(LOG_TAG, "ONCREATE");
        plm = new PlayBackManager();
        registerReceiver();
        exec = Executors.newSingleThreadExecutor();
        isReleased = true;
        nfm = NotificationManagerCompat.from(this);
        mpq = new MediaPlayerEqualizer();
    }

    @Override
    public void onDestroy() {
        Log.v(LOG_TAG, "ONDESTROY");
        if (player != null) {
            player.stop();
            player.release();
            isReleased = true;
        }
        if (brcv != null) {
            unregisterReceiver(brcv);
            brcv = null;
        }
        if (nfm != null) {
            nfm.cancelAll();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    //Public Control Functions
    public int setContent(List<data_song> pl) {
        Log.v(LOG_TAG, "SetContent Called Size: " + pl.size());
        List<data_song> t = new ArrayList<>(pl);
        plm.setContent(t);
        return 0;
    }

    public int play(int id) {
        data_song n = plm.setNext(id);
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
        data_song n = plm.getNext();
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
        data_song n = plm.getPrev();
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
                boolean ret = pause();
                return ret;
            } else {
                boolean ret = resume();
                return ret;
            }
        }
        return false;
    }

    public boolean pause() {
        Log.v(LOG_TAG, "Pause");
        if (player != null) {
            player.pause();
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
                setCompletionListener();
                setVolume(volume);
                showNotification();
            } else {
                player.start();
                setCompletionListener();
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
        plm.repeat = state;
        return state;
    }

    public boolean switchRepeat() {
        if (plm.repeat) {
            plm.repeat = false;
            return false;
        } else {
            plm.repeat = true;
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

    public int setEqualizerPreset(int preset) {
        Log.v(LOG_TAG, "SELECTING PRESET: " + preset);
        if (mpq != null && player != null) {
            mpq.setPreset(player.getAudioSessionId(), preset);
        }
        return 0;
    }

    public List<String> getEqualizerPresetNames() {
        if (mpq != null)
            return mpq.getPresets();
        else
            return new ArrayList<>();
    }

    //Various Public State Providers
    public boolean getPlaybackStatus() {
        if (player != null) {
            if (player.isPlaying())
                return true;
        }
        return false;
    }

    public data_song getCurrentSong() {
        return plm.currentSong;
    }

    public int getCurrentPosition() {
        if (player != null)
            return player.getCurrentPosition();
        return 0;
    }

    public int getDuration() {
        if (player != null)
            return player.getDuration();
        return 0;
    }

    public boolean getRepeat() {
        return plm.repeat;
    }

    public boolean getShuffle() {
        return plm.shuffle;
    }

    //Private Globals
    private MediaPlayer player;
    private boolean isReleased; //Indicates if MediaPlayer is Released to avoid Illegal State Exceptions
    private MediaPlayerEqualizer mpq;

    private PlayBackManager plm;

    private NotificationManagerCompat nfm;
    private int notificationID = 1;

    private String LOG_TAG = "SERV";

    private float volume = 0.8f;

    private ExecutorService exec;

    private Context T = this;

    private ValueAnimator notification_animator;
    private Long notification_animator_starttime;
    private Long notification_animator_delay = (long) 500; //The Notification gets refreshed every "notification_animator_delay" milliseconds by the value animator.

    private int position; //Position of Media Player in Miliseconds

    //Binder
    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public MusicPlayerService getService() {
            return MusicPlayerService.this;
        }
    }

    //Broadcast Receiver
    private BroadcastReceiver brcv;

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

    //Broadcast Sender
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
            isReleased = true;
        }
        songP = "file://" + songP;
        Log.v(LOG_TAG, "CREATING PLAYER: " + songP);
        player = MediaPlayer.create(getApplicationContext(), Uri.parse(songP));
        isReleased = false;
        if (player != null) {
            player.start();
            setVolume(volume);
            setCompletionListener();
        }
    }

    //Notification Widget
    @TargetApi(24)
    public void showNotification() {
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            //Fallback to old Notification because Custom Layout gets Squashed below API 24 and setCustomContentView is >24 only
            Map<String, String> md = new HashMap<>();
            if (plm.currentSong != null) {
                md.put("TITLE", plm.currentSong.Title);
                md.put("ARTIST", plm.currentSong.Artist);
            } else {
                md.put("TITLE", "");
                md.put("ARTIST", "");
            }
            Notification.Builder nb = new Notification.Builder(T)
                    .setShowWhen(false)
                    .setStyle(new Notification.MediaStyle().setShowActionsInCompactView(0, 1, 2));
            nb.setSmallIcon(R.drawable.notification_smallicon)
                    .setContentTitle(md.get("TITLE"))
                    .setContentText(md.get("ARTIST"))
                    .addAction(R.drawable.main_btnprev, "prev", retrievePlaybackAction(3));
            if (!isReleased && player.isPlaying())
                nb.addAction(R.drawable.main_btnpause, "pause", retrievePlaybackAction(1));
            else
                nb.addAction(R.drawable.main_btnplay, "play", retrievePlaybackAction(1));
            nb.addAction(R.drawable.main_btnnext, "next", retrievePlaybackAction(2));
            Intent resultIntent = new Intent(T, MainActivity.class);
            resultIntent.setAction("android.intent.action.MAIN");
            resultIntent.addCategory("android.intent.category.LAUNCHER");
            PendingIntent resultPendingIntent = PendingIntent.getActivity(T, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            nb.setContentIntent(resultPendingIntent);
            Intent oncloseIntent = new Intent(ACTION_QUIT);
            PendingIntent onclosepi = PendingIntent.getBroadcast(getApplicationContext(), 0, oncloseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            nb.setDeleteIntent(onclosepi);

            Notification noti = nb.build();
            nfm.notify(notificationID, noti);
        } else {
            //Fancy Custom Notification
            MainApplication mp = (MainApplication) getApplicationContext();
            setTheme(mp.selectedThemeID);

            final RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.custom_notification_musicplayer);

            contentView.setTextViewText(R.id.title, getString(R.string.app_name));
            contentView.setTextColor(R.id.title, getColorFromAtt(R.attr.colorText));

            Map<String, String> md = new HashMap<>();
            if (plm.currentSong != null) {
                md.put("TITLE", plm.currentSong.Title);
                md.put("ARTIST", plm.currentSong.Artist);
            } else {
                md.put("TITLE", "");
                md.put("ARTIST", "");
            }

            contentView.setTextViewText(R.id.song_title, md.get("TITLE"));
            contentView.setTextColor(R.id.song_title, getColorFromAtt(R.attr.colorText));

            contentView.setTextViewText(R.id.song_interpret, md.get("ARTIST"));
            contentView.setTextColor(R.id.song_interpret, getColorFromAtt(R.attr.colorText));

            contentView.setInt(R.id.background, "setBackgroundColor", getColorFromAtt(R.attr.colorFrame));

            contentView.setInt(R.id.icon, "setColorFilter", getColorFromAtt(R.attr.colorText));

            if (!isReleased && player.isPlaying()) {
                contentView.setImageViewResource(R.id.btnPlayimg, R.drawable.main_btnpause);
            } else {
                contentView.setImageViewResource(R.id.btnPlayimg, R.drawable.main_btnplay);
            }

            contentView.setInt(R.id.btnPlayimg, "setColorFilter", getColorFromAtt(R.attr.colorText));
            contentView.setInt(R.id.btnPrevimg, "setColorFilter", getColorFromAtt(R.attr.colorText));
            contentView.setInt(R.id.btnNextimg, "setColorFilter", getColorFromAtt(R.attr.colorText));

            contentView.setOnClickPendingIntent(R.id.btnPlay, retrievePlaybackAction(1));
            contentView.setOnClickPendingIntent(R.id.btnPrev, retrievePlaybackAction(3));
            contentView.setOnClickPendingIntent(R.id.btnNext, retrievePlaybackAction(2));

            Intent resultIntent = new Intent(T, MainActivity.class);
            resultIntent.setAction("android.intent.action.MAIN");
            resultIntent.addCategory("android.intent.category.LAUNCHER");
            PendingIntent resultPendingIntent = PendingIntent.getActivity(T, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            contentView.setOnClickPendingIntent(R.id.btnOpen, resultPendingIntent);

            if (plm.currentSong != null && !isReleased && player.isPlaying()) {
                if (notification_animator != null)
                    notification_animator.cancel();
                notification_animator = ValueAnimator.ofInt(0, player.getDuration());
                notification_animator.setDuration(player.getDuration());
                notification_animator.setCurrentPlayTime(player.getCurrentPosition());
                notification_animator.setInterpolator(new LinearInterpolator());
                notification_animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        if (notification_animator_starttime == null || System.currentTimeMillis() - notification_animator_starttime > notification_animator_delay) {
                            notification_animator_starttime = System.currentTimeMillis();
                        } else {
                            return;
                        }
                        MainApplication mp = (MainApplication) getApplicationContext();
                        setTheme(mp.selectedThemeID);

                        final RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.custom_notification_musicplayer);

                        contentView.setTextViewText(R.id.title, getString(R.string.app_name));
                        contentView.setTextColor(R.id.title, getColorFromAtt(R.attr.colorText));

                        Map<String, String> md = new HashMap<>();
                        if (plm.currentSong != null) {
                            md.put("TITLE", plm.currentSong.Title);
                            md.put("ARTIST", plm.currentSong.Artist);
                        } else {
                            md.put("TITLE", "");
                            md.put("ARTIST", "");
                        }

                        contentView.setTextViewText(R.id.song_title, md.get("TITLE"));
                        contentView.setTextColor(R.id.song_title, getColorFromAtt(R.attr.colorText));

                        contentView.setTextViewText(R.id.song_interpret, md.get("ARTIST"));
                        contentView.setTextColor(R.id.song_interpret, getColorFromAtt(R.attr.colorText));

                        contentView.setInt(R.id.background, "setBackgroundColor", getColorFromAtt(R.attr.colorFrame));

                        contentView.setInt(R.id.icon, "setColorFilter", getColorFromAtt(R.attr.colorText));

                        if (!isReleased && player.isPlaying()) {
                            contentView.setImageViewResource(R.id.btnPlayimg, R.drawable.main_btnpause);
                        } else {
                            contentView.setImageViewResource(R.id.btnPlayimg, R.drawable.main_btnplay);
                        }

                        contentView.setInt(R.id.btnPlayimg, "setColorFilter", getColorFromAtt(R.attr.colorText));
                        contentView.setInt(R.id.btnPrevimg, "setColorFilter", getColorFromAtt(R.attr.colorText));
                        contentView.setInt(R.id.btnNextimg, "setColorFilter", getColorFromAtt(R.attr.colorText));

                        contentView.setOnClickPendingIntent(R.id.btnPlay, retrievePlaybackAction(1));
                        contentView.setOnClickPendingIntent(R.id.btnPrev, retrievePlaybackAction(3));
                        contentView.setOnClickPendingIntent(R.id.btnNext, retrievePlaybackAction(2));

                        Intent resultIntent = new Intent(T, MainActivity.class);
                        resultIntent.setAction("android.intent.action.MAIN");
                        resultIntent.addCategory("android.intent.category.LAUNCHER");
                        PendingIntent resultPendingIntent = PendingIntent.getActivity(T, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                        contentView.setOnClickPendingIntent(R.id.btnOpen, resultPendingIntent);

                        contentView.setProgressBar(R.id.progbar, (int) animation.getDuration(), (int) animation.getCurrentPlayTime(), false);

                        //Progressbar Color Start
                        Method setTintMethod = null;
                        try {
                            setTintMethod = RemoteViews.class.getMethod("setProgressTintList", int.class, ColorStateList.class);
                        } catch (SecurityException e) {
                            e.printStackTrace();
                        } catch (NoSuchMethodException e) {
                            e.printStackTrace();
                        }
                        if (setTintMethod != null) {
                            try {
                                setTintMethod.invoke(contentView, R.id.progbar, ColorStateList.valueOf(getColorFromAtt(R.attr.colorHighlight)));
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            } catch (InvocationTargetException e) {
                                e.printStackTrace();
                            }
                        }
                        //Progressbar Color End

                        Notification.Builder mBuilder = new Notification.Builder(T);//TODO: DEPRECATED NOTIFICATION BUILDER
                        mBuilder.setSmallIcon(R.drawable.notification_smallicon);

                        mBuilder.setCustomContentView(contentView);

                        mBuilder.setVisibility(Notification.VISIBILITY_PUBLIC);

                        Intent oncloseIntent = new Intent(ACTION_QUIT);
                        PendingIntent onclosepi = PendingIntent.getBroadcast(getApplicationContext(), 0, oncloseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                        mBuilder.setDeleteIntent(onclosepi);

                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                            String NOTIFICATION_CHANNEL_ID = "8191023875";
                            int importance = NotificationManager.IMPORTANCE_DEFAULT;
                            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "CANORA_NOTIFICATION_CHANNEL", importance);
                            mBuilder.setChannelId(NOTIFICATION_CHANNEL_ID);
                            mNotifyMgr.createNotificationChannel(notificationChannel);
                        }

                        Notification notification = mBuilder.build();
                        nfm.notify(1, notification);
                    }
                });
                notification_animator.start();
            } else if (plm.currentSong != null && !isReleased && !player.isPlaying()) {
                if (notification_animator != null)
                    notification_animator.cancel();
                contentView.setProgressBar(R.id.progbar, player.getDuration(), player.getCurrentPosition(), false);

                //Progressbar Color Start
                Method setTintMethod = null;
                try {
                    setTintMethod = RemoteViews.class.getMethod("setProgressTintList", int.class, ColorStateList.class);
                } catch (SecurityException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
                if (setTintMethod != null) {
                    try {
                        setTintMethod.invoke(contentView, R.id.progbar, ColorStateList.valueOf(getColorFromAtt(R.attr.colorHighlight)));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
                //Progressbar Color End

            } else {
                if (notification_animator != null)
                    notification_animator.cancel();
                contentView.setProgressBar(R.id.progbar, 1, 1, false);

                //Progressbar Color Start
                Method setTintMethod = null;
                try {
                    setTintMethod = RemoteViews.class.getMethod("setProgressTintList", int.class, ColorStateList.class);
                } catch (SecurityException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
                if (setTintMethod != null) {
                    try {
                        setTintMethod.invoke(contentView, R.id.progbar, ColorStateList.valueOf(getColorFromAtt(R.attr.colorHighlight)));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
                //Progressbar Color End
            }

            Notification.Builder mBuilder = new Notification.Builder(T);
            mBuilder.setSmallIcon(R.drawable.notification_smallicon);

            mBuilder.setCustomContentView(contentView);

            mBuilder.setVisibility(Notification.VISIBILITY_PUBLIC);

            Intent oncloseIntent = new Intent(ACTION_QUIT);
            PendingIntent onclosepi = PendingIntent.getBroadcast(getApplicationContext(), 0, oncloseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setDeleteIntent(onclosepi);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                String NOTIFICATION_CHANNEL_ID = "8191023875";
                int importance = NotificationManager.IMPORTANCE_DEFAULT;
                NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "CANORA_NOTIFICATION_CHANNEL", importance);
                mBuilder.setChannelId(NOTIFICATION_CHANNEL_ID);
                mNotifyMgr.createNotificationChannel(notificationChannel);
            }

            Notification notification = mBuilder.build();
            nfm.notify(1, notification);
        }
    }

    public int getColorFromAtt(int v) {
        TypedValue tv = new TypedValue();
        Resources.Theme theme = getTheme();
        theme.resolveAttribute(v, tv, true);
        return tv.data;
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

    private void setCompletionListener() {
        Log.v(LOG_TAG, "setCompletionListener Called.");
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Log.v(LOG_TAG, "Completion Listener Called.");
                if (plm.repeat) {
                    mp.seekTo(0);
                    mp.start();
                    setCompletionListener();
                    broadcastNewSong();
                } else {
                    next();
                    broadcastNewSong();
                }
            }
        });
    }

    private class PlayBackManager {
        /*
        Provides data_song
         */
        public data_song currentSong;

        public boolean shuffle;

        public boolean repeat;

        public PlayBackManager() {
            shuffle = false;
            repeat = false;
            history = new Stack<>();
            forwardHistory = new Stack<>();
            currentIndex = 0;
        }

        public data_song setNext(int id) {
            if (content.size() == 0) {
                Log.v(LOG_TAG, "CONTENT EMPTY");
                return null;
            }
            if (currentSong != null)
                history.push(currentSong);
            forwardHistory.empty();
            currentIndex = getIndexOfID(id);
            currentSong = content.get(currentIndex);
            return currentSong;
        }

        public data_song getNext() {
            if (content.size() == 0) {
                Log.v(LOG_TAG, "CONTENT EMPTY");
                return null;
            }
            if (currentSong != null)
                history.push(currentSong);
            if (forwardHistory.size() > 0) {
                currentSong = forwardHistory.pop();
                currentIndex = getIndexOfID(currentSong.id);
                return currentSong;
            }
            if (shuffle) {
                if (shufflememavailable.size() <= 0) {
                    shufflememavailable = new ArrayList<>(content);
                }
                Random rand = new Random();
                int t = rand.nextInt(shufflememavailable.size());
                currentIndex = t;
                currentSong = shufflememavailable.get(currentIndex);
                shufflememavailable.remove(t);
                return currentSong;
            } else {
                shufflememavailable = new ArrayList<>(content);
                if (content.size() > 1 && currentIndex < content.size() - 1) {
                    currentIndex = currentIndex + 1;
                } else {
                    currentIndex = 0;
                }
                currentSong = content.get(currentIndex);
                return currentSong;
            }
        }

        public data_song getPrev() {
            if (content.size() == 0)
                return null;
            if (currentSong != null)
                forwardHistory.push(new data_song(currentSong));
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

        public void setContent(List<data_song> c) {
            content = new ArrayList<>(c);
            shufflememavailable = new ArrayList<>(content);
        }

        private Stack<data_song> history;
        private Stack<data_song> forwardHistory;

        private List<data_song> shufflememavailable;

        private List<data_song> content;
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
