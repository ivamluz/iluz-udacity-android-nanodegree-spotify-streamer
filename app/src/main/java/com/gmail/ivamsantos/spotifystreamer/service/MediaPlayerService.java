package com.gmail.ivamsantos.spotifystreamer.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.gmail.ivamsantos.spotifystreamer.MainActivity;
import com.gmail.ivamsantos.spotifystreamer.helper.SpotifyImageHelper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.Track;

public class MediaPlayerService extends Service implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener {

    public static final String BROADCAST_CURRENT_TRACK_CHANGED = "current-track-changed";
    public static final String BROADCAST_PLAYER_STATE_CHANGED = "player-state-changed";
    public static final String BROADCAST_TRACK_PLAYBACK_CURRENT_PROGRESS = "track-playback-current-progress";
    public static final String BROADCAST_DATA_CURRENT_TRACK = "current-track";
    public static final String BROADCAST_DATA_CURRENT_STATE = "current-state";
    public static final String BROADCAST_DATA_CURRENT_PROGRESS = "current-progress";
    private static final String LOG_TAG = MediaPlayerService.class.getSimpleName();
    private static final float LOUD_VOLUME = 1.0f;
    private static final float DUCK_VOLUME = 0.1f;
    private static final int NOTIFICATION_ID = 1;
    private final IBinder selfBinder = new MediaPlayerServiceBinder();
    private MediaPlayer mMediaPlayer;
    private ArrayList<Track> mTracks;
    private int mCurrentTrackIndex = 0;
    private WifiManager.WifiLock mWifiLock;
    private AudioManager mAudioManager;
    private AudioFocus mAudioFocus = AudioFocus.NO_FOCUS_NO_DUCK;
    private ServiceState mState = ServiceState.STOPPED;
    private ScheduledExecutorService mScheduleTaskExecutor;
    private ScheduledFuture<?> mTrackProgressReporter;
    private NotificationManager mNotificationManager;
    private Artist mArtist;

    @Override
    public void onCreate() {
        super.onCreate();

        setupMediaPlayer();
        createWifiLock();

        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    private void setupMediaPlayer() {
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
    }

    @Override
    public void onDestroy() {
        releaseWifiLock();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return selfBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        deactivateProgressReporter();
        mMediaPlayer.stop();
        mMediaPlayer.release();

        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        foward();
    }

    public void foward() {
        int lastIndex = mTracks.size() - 1;
        if (mCurrentTrackIndex == lastIndex) {
            mCurrentTrackIndex = 0;
        } else {
            mCurrentTrackIndex++;
        }

        broadcastCurrentTrackIndex();
        startPlay();
    }

    public void previous() {
        int lastIndex = mTracks.size() - 1;
        if (mCurrentTrackIndex == 0) {
            mCurrentTrackIndex = lastIndex;
        } else {
            mCurrentTrackIndex--;
        }

        broadcastCurrentTrackIndex();
        startPlay();
    }

    /**
     * Seek to specific position of the track.
     * @param position milliseconds
     */
    public void seekTo(int position) {
        if (ServiceState.PLAYING.equals(mState) || ServiceState.PAUSED.equals(mState)) {
            mMediaPlayer.seekTo(position);
        }
    }

    public int getCurrentTrackIndex() {
        return mCurrentTrackIndex;
    }

    public ServiceState getState() {
        return mState;
    }

    private void broadcastCurrentTrackIndex() {
        Intent intent = new Intent(BROADCAST_CURRENT_TRACK_CHANGED);
        intent.putExtra(BROADCAST_DATA_CURRENT_TRACK, mCurrentTrackIndex);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void activateProgressReporter() {
        if (mScheduleTaskExecutor == null) {
            mScheduleTaskExecutor = new ScheduledThreadPoolExecutor(1);
        }

        mTrackProgressReporter = mScheduleTaskExecutor.scheduleAtFixedRate(new Runnable() {
            public void run() {
                Intent intent = new Intent(BROADCAST_TRACK_PLAYBACK_CURRENT_PROGRESS);
                intent.putExtra(BROADCAST_DATA_CURRENT_PROGRESS, mMediaPlayer.getCurrentPosition());
                LocalBroadcastManager.getInstance(MediaPlayerService.this).sendBroadcast(intent);
            }
        }, 500, 350, TimeUnit.MILLISECONDS);
    }

    private void deactivateProgressReporter() {
        if (mTrackProgressReporter != null) {
            mTrackProgressReporter.cancel(true);
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    // TODO i18n
    @Override
    public void onPrepared(MediaPlayer mp) {
        configAndStartMediaPlayer();
        updateNotification(getCurrentTrack().name + " (playing)");
    }

    private void configAndStartMediaPlayer() {
        switch (mAudioFocus) {
            case NO_FOCUS_NO_DUCK:
                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.pause();
                }
                return;
            case NO_FOCUS_CAN_DUCK:
                mMediaPlayer.setVolume(DUCK_VOLUME, DUCK_VOLUME);
                break;
            default:
                mMediaPlayer.setVolume(LOUD_VOLUME, LOUD_VOLUME);
        }

        if (!mMediaPlayer.isPlaying()) {
            activateProgressReporter();
            mMediaPlayer.start();
        }

        setServiceState(ServiceState.PLAYING);
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                this.onGainedAudioFocus();
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                this.onLostAudioFocus(false);
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                this.onLostAudioFocus(true);
                break;
            default:
        }
    }

    public void onGainedAudioFocus() {
        Log.d(LOG_TAG, "Gained audio focus.");
        mAudioFocus = AudioFocus.FOCUSED;

        if (ServiceState.PLAYING.equals(mState)) {
            configAndStartMediaPlayer();
        }
    }

    public void onLostAudioFocus(boolean canDuck) {
        Log.d(LOG_TAG, String.format("Lost audio focus. Can duck? %s", canDuck));
        mAudioFocus = canDuck ? AudioFocus.NO_FOCUS_CAN_DUCK : AudioFocus.NO_FOCUS_NO_DUCK;

        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            configAndStartMediaPlayer();
        }
    }

    public void setArtist(Artist artist) {
        mArtist = artist;
    }

    public ArrayList<Track> getTracksList() {
        return mTracks;
    }

    public void setTracksList(ArrayList<Track> tracks) {
        mTracks = tracks;
    }

    public void selectTrack(int trackIndex) {
        mCurrentTrackIndex = trackIndex;
    }

    public void startPlay() {
        mMediaPlayer.reset();

        Track currentTrack = getCurrentTrack();

        try {
            mMediaPlayer.setDataSource(currentTrack.preview_url);
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage());
            e.printStackTrace();
        }

        tryToGetAudioFocus();
        setServiceState(ServiceState.PREPARING);
        mMediaPlayer.prepareAsync();
        // TODO: i18n
        showNotification(currentTrack.name + " (loading)");
        mWifiLock.acquire();
    }

    public void continuePlaying() {
        if (!ServiceState.PAUSED.equals(mState)) {
            return;
        }

        tryToGetAudioFocus();
        mWifiLock.acquire();
        mMediaPlayer.start();
        activateProgressReporter();
        setServiceState(ServiceState.PLAYING);
    }

    private void pause() {
        if (ServiceState.PLAYING.equals(mState)) {
            setServiceState(ServiceState.PAUSED);
            deactivateProgressReporter();
            // TODO: i18n
            updateNotification(getCurrentTrack().name + " (paused)");
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
            }
            releaseResources(false);
        }
    }

    private void releaseResources(boolean releaseMediaPlayer) {
        stopForeground(true);
        if (releaseMediaPlayer) {
            releaseMediaPlayer();
        }
        releaseWifiLock();
    }

    private void releaseMediaPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    public void togglePlayback() {
        switch (mState) {
            case STOPPED:
                startPlay();
                break;
            case PAUSED:
                continuePlaying();
                break;
            case PLAYING:
                pause();
                break;
            default:
                Log.e(LOG_TAG, String.format("Invalid state %s while toggling playback", mState));
        }
    }

    private void setServiceState(ServiceState state) {
        if (mState == state) {
            return;
        }

        if (ServiceState.PLAYING.equals(state)) {
            activateProgressReporter();
        } else {
            deactivateProgressReporter();
        }

        mState = state;

        Intent intent = new Intent(BROADCAST_PLAYER_STATE_CHANGED);
        intent.putExtra(BROADCAST_DATA_CURRENT_STATE, mState);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private Track getCurrentTrack() {
        return mTracks.get(mCurrentTrackIndex);
    }

    private void createWifiLock() {
        String lockId = String.format("%sLock", this.getClass().getSimpleName());
        mWifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, lockId);
    }

    private void releaseWifiLock() {
        if (mWifiLock != null && mWifiLock.isHeld()) {
            mWifiLock.release();
        }
    }

    private void tryToGetAudioFocus() {
        if (mAudioFocus != AudioFocus.FOCUSED && this.requestAudioFocus()) {
            mAudioFocus = AudioFocus.FOCUSED;
        }
    }

    private void giveUpAudioFocus() {
        if (AudioFocus.FOCUSED.equals(mAudioFocus) && this.giveupAudioFocus()) {
            mAudioFocus = AudioFocus.NO_FOCUS_NO_DUCK;
        }
    }

    public boolean requestAudioFocus() {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
                mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
    }

    public boolean giveupAudioFocus() {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == mAudioManager.abandonAudioFocus(this);
    }

    private void updateNotification(String text) {
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(getApplicationContext(), MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = buildNotification(text, pi);
        mNotificationManager.notify(NOTIFICATION_ID, notification);
    }

    private void showNotification(String text) {
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(getApplicationContext(), MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = buildNotification(text, pi);
        startForeground(NOTIFICATION_ID, notification);
    }

    private Notification buildNotification(String text, PendingIntent pi) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder
                .setContentIntent(pi)
                .setTicker(text)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(true)
                .setContentTitle(mArtist.name);

        return builder.build();
    }

    private enum AudioFocus {
        NO_FOCUS_NO_DUCK,
        NO_FOCUS_CAN_DUCK,
        FOCUSED
    }

    public enum ServiceState {
        STOPPED,
        PREPARING,
        PLAYING,
        PAUSED
    }

    public class MediaPlayerServiceBinder extends Binder {
        public MediaPlayerService getService() {
            return MediaPlayerService.this;
        }
    }
}
