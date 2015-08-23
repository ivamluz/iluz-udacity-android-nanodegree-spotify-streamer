package com.gmail.ivamsantos.spotifystreamer;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.gmail.ivamsantos.spotifystreamer.service.MediaPlayerService;
import com.gmail.ivamsantos.spotifystreamer.service.MediaPlayerService.MediaPlayerServiceBinder;
import com.gmail.ivamsantos.spotifystreamer.service.MediaPlayerService.ServiceState;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.Track;


/**
 * A placeholder fragment containing a simple view.
 */
public class TrackPlayerDialogFragment extends DialogFragment implements SeekBar.OnSeekBarChangeListener {
    public static final String LOG_TAG = TrackPlayerDialogFragment.class.getSimpleName();
    public static final String BUNDLE_KEY_ARTIST = "artist";
    public static final String BUNDLE_KEY_TRACKS = "tracks";
    public static final String BUNDLE_KEY_IS_MEDIA_PLAYER_SERVICE_BOUND = "is-media-player-service-bound";
    public static final String BUNDLE_KEY_CURRENT_TRACK_INDEX = "current-track-index";
    private MediaPlayerServiceBinder mMediaPlayerServiceBinder;
    private View mRootView;
    private ActionBar mActionBar;
    private Activity mParentActivity;
    private Artist mArtist;
    private Integer mCurrentTrackIndex;
    private ArrayList<Track> mTracks;
    private MediaPlayerService mMediaPlayerService;
    private boolean mIsMediaPlayerServiceBound = false;

    private ImageButton mPreviousButton;
    private ImageButton mPlayPauseButton;
    private ImageButton mFowardButton;
    private SeekBar mSeekBar;

    private ServiceConnection mMediaPlayerServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mMediaPlayerServiceBinder = (MediaPlayerServiceBinder) service;
            mMediaPlayerService = mMediaPlayerServiceBinder.getService();

            setPlayPauseButtonState(mMediaPlayerService.getState());

            if (mIsMediaPlayerServiceBound) {
                if (mCurrentTrackIndex != mMediaPlayerService.getCurrentTrackIndex()) {
                    mCurrentTrackIndex = mMediaPlayerService.getCurrentTrackIndex();
                    bindValues();
                }
            } else {
                mMediaPlayerService.setArtist(mArtist);
                mMediaPlayerService.setTracksList(mTracks);
                mMediaPlayerService.selectTrack(mCurrentTrackIndex);
                mMediaPlayerService.startPlay();
                mIsMediaPlayerServiceBound = true;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mIsMediaPlayerServiceBound = false;
        }
    };

    private BroadcastReceiver mOnCurrentTrackChangedReceiver;
    private BroadcastReceiver mOnCurrentStateChangedReceiver;
    private BroadcastReceiver mOnTrackProgressUpdatedReceiver;

    private void setPlayPauseButtonState(ServiceState state) {
        if (ServiceState.PLAYING.equals(state)) {
            playPauseButton().setImageResource(android.R.drawable.ic_media_pause);
        } else if (ServiceState.PAUSED.equals(state) || ServiceState.STOPPED.equals(state)) {
            playPauseButton().setImageResource(android.R.drawable.ic_media_play);
        }
    }

    private ImageButton playPauseButton() {
        return (ImageButton) mRootView.findViewById(R.id.player_play_pause_button);
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        setRetainInstance(true);
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_track_player, container, false);
        mParentActivity = getActivity();

        if (savedInstanceState == null) {
            loadArguments();
        } else {
            loadSavedInstanceState(savedInstanceState);
//            mCurrentTrackIndex = mMediaPlayerService.getCurrentTrackIndex();
        }

        setupMediaPlayerService();

        setupActionBar();
        bindValues();

        setupControlButtons();
        setupSeekBar();

        setupBroadcastReceivers();

        return mRootView;
    }

    private void setupControlButtons() {
        mPreviousButton = previousButton();
        mPreviousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMediaPlayerService.previous();
            }
        });

        mPlayPauseButton = playPauseButton();
        mPlayPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMediaPlayerService.togglePlayback();
            }
        });

        mFowardButton = nextButton();
        mFowardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMediaPlayerService.foward();
            }
        });
    }

    private void setupSeekBar() {
        mSeekBar = seekBar();
        mSeekBar.setProgress(0);
        mSeekBar.setMax(30);
        mSeekBar.setOnSeekBarChangeListener(this);
    }

    private ImageButton nextButton() {
        return (ImageButton) mRootView.findViewById(R.id.player_next_button);
    }

    private ImageButton previousButton() {
        return (ImageButton) mRootView.findViewById(R.id.player_previous_button);
    }

    private SeekBar seekBar() {
        return (SeekBar) mRootView.findViewById(R.id.track_player_seek_bar);
    }

    private void enablePlayerControls() {
        playPauseButton().setEnabled(true);
        nextButton().setEnabled(true);
        previousButton().setEnabled(true);
        seekBar().setEnabled(true);
    }

    private void disablePlayerControls() {
        playPauseButton().setEnabled(false);
        nextButton().setEnabled(false);
        previousButton().setEnabled(false);
        seekBar().setEnabled(false);
    }

    private void setupBroadcastReceivers() {
        LocalBroadcastManager.getInstance(mParentActivity).registerReceiver(getOnCurrentTrackChangedReceiver(),
                new IntentFilter(MediaPlayerService.BROADCAST_CURRENT_TRACK_CHANGED));

        LocalBroadcastManager.getInstance(mParentActivity).registerReceiver(getOnCurrentStateChangedReceiver(),
                new IntentFilter(MediaPlayerService.BROADCAST_PLAYER_STATE_CHANGED));

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(getOnTrackProgressUpdatedReceiver(),
                new IntentFilter(MediaPlayerService.BROADCAST_TRACK_PLAYBACK_CURRENT_PROGRESS));
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putParcelable(BUNDLE_KEY_ARTIST, mArtist);
        bundle.putParcelableArrayList(BUNDLE_KEY_TRACKS, mTracks);
        bundle.putInt(BUNDLE_KEY_CURRENT_TRACK_INDEX, mCurrentTrackIndex);
        bundle.putBoolean(BUNDLE_KEY_IS_MEDIA_PLAYER_SERVICE_BOUND, mIsMediaPlayerServiceBound);
    }

    private void loadSavedInstanceState(Bundle savedInstanceState) {
        mArtist = savedInstanceState.getParcelable(BUNDLE_KEY_ARTIST);
        mTracks = savedInstanceState.getParcelableArrayList(BUNDLE_KEY_TRACKS);
        mCurrentTrackIndex = savedInstanceState.getInt(BUNDLE_KEY_CURRENT_TRACK_INDEX);
        mIsMediaPlayerServiceBound = savedInstanceState.getBoolean(BUNDLE_KEY_IS_MEDIA_PLAYER_SERVICE_BOUND);
    }

    private void setupMediaPlayerService() {
        Context context = mParentActivity.getApplicationContext();
        Intent intent = new Intent(context, MediaPlayerService.class);
        context.startService(intent);
        context.bindService(intent, mMediaPlayerServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void loadArguments() {
        Bundle arguments = getArguments();
        mArtist = arguments.getParcelable(getString(R.string.extra_artist));
        mTracks = arguments.getParcelableArrayList(getString(R.string.extra_tracks));
        mCurrentTrackIndex = arguments.getInt(getString(R.string.extra_track_index));
    }

    private void setupActionBar() {
        mActionBar = actionBar();
        mActionBar.setDisplayHomeAsUpEnabled(true);
    }

    private ActionBar actionBar() {
        // http://www.slideshare.net/cbeyls/android-32084115 (slide 13)
        return ((ActionBarActivity) mParentActivity).getSupportActionBar();
    }

    private void bindValues() {
        TextView artistNameTextView = (TextView) mRootView.findViewById(R.id.track_player_artist_name);
        TextView albumNameTextView = (TextView) mRootView.findViewById(R.id.track_player_album_name);
        TextView trackNameTextView = (TextView) mRootView.findViewById(R.id.track_player_track_name);
        ImageView albumImageImageView = (ImageView) mRootView.findViewById(R.id.track_player_album_image);

        Track currentTrack = currentTrack();
        artistNameTextView.setText(mArtist.name);
        albumNameTextView.setText(currentTrack.album.name);
        trackNameTextView.setText(currentTrack.name);

        Picasso.with(mParentActivity)
                .load(getAlbumImage(currentTrack))
                .placeholder(R.drawable.art_cover_placeholder)
                .error(R.drawable.art_cover_placeholder)
                .into(albumImageImageView);

        mActionBar = actionBar();
        mActionBar.setTitle(mArtist.name);
        mActionBar.setSubtitle(currentTrack.name);
    }

    private Track currentTrack() {
        return mTracks.get(mCurrentTrackIndex);
    }

    private String getAlbumImage(Track track) {

        Log.i(LOG_TAG, "ORIENTATION: " + getResources().getConfiguration().orientation);

        String imageUrl = null;
        if (!track.album.images.isEmpty()) {
            imageUrl = track.album.images.get(0).url;
        }

        return imageUrl;
    }


    private BroadcastReceiver getOnCurrentTrackChangedReceiver() {
        if (mOnCurrentStateChangedReceiver != null) {
            return mOnCurrentTrackChangedReceiver;
        }

        mOnCurrentTrackChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                try {
                    Log.d(LOG_TAG, "CurrentTrackChanged broadcast received.");
                    mCurrentTrackIndex = intent.getIntExtra(MediaPlayerService.BROADCAST_DATA_CURRENT_TRACK, 0);
                    Log.d(LOG_TAG, String.format("Current track index: %s", mCurrentTrackIndex));
                    bindValues();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        return mOnCurrentTrackChangedReceiver;
    }

    private BroadcastReceiver getOnCurrentStateChangedReceiver() {
        if (mOnCurrentStateChangedReceiver != null) {
            return mOnCurrentStateChangedReceiver;
        }

        mOnCurrentStateChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                try {
                    Log.d(LOG_TAG, "CurrentStateChanged broadcast received.");
                    ServiceState state = (ServiceState) intent.getSerializableExtra(MediaPlayerService.BROADCAST_DATA_CURRENT_STATE);
                    Log.d(LOG_TAG, String.format("Current state: %s", state));

                    setPlayPauseButtonState(state);

                    if (ServiceState.PREPARING.equals(state)) {
                        disablePlayerControls();
                    } else {
                        enablePlayerControls();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        return mOnCurrentStateChangedReceiver;
    }

    private BroadcastReceiver getOnTrackProgressUpdatedReceiver() {
        if (mOnTrackProgressUpdatedReceiver != null) {
            return mOnTrackProgressUpdatedReceiver;
        }

        mOnTrackProgressUpdatedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                try {
                    int progress = intent.getIntExtra(MediaPlayerService.BROADCAST_DATA_CURRENT_PROGRESS, 0);
                    progress = (int) Math.ceil((double) progress / 1000);

                    Log.v(LOG_TAG, String.format("Current progress: %s", progress));
                    seekBar().setProgress(progress);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        return mOnTrackProgressUpdatedReceiver;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            // should be in milliseconds.
            int position = progress * 1000;
            mMediaPlayerService.seekTo(position);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // not interested in this event.
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // not interested in this event.
    }

    // http://stackoverflow.com/a/12434038
    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance()) {
            getDialog().setDismissMessage(null);
        }
        super.onDestroyView();
    }
}
