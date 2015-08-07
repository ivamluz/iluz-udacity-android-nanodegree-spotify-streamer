package com.gmail.ivamsantos.spotifystreamer;

import android.app.Dialog;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;

import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.Track;


/**
 * A placeholder fragment containing a simple view.
 */
public class TrackPlayerDialogFragment extends DialogFragment {
    public static final String LOG_TAG = TrackPlayerDialogFragment.class.getSimpleName();
    public static final int TRACK_INDEX_UNAVAILABLE = -1;

    boolean isPlaying = false;
    private View mRootView;
    private Artist mArtist;
    private Track mCurrentTrack;
    private Integer mCurrentTrackIndex;
    private ArrayList<Track> mTracks;

    private boolean mIsPaused;
    private int mCurrentPosition;

    private ActionBar mActionBar;

    private MediaPlayer mMediaPlayer;

    public TrackPlayerDialogFragment() {
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
//        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//        // Get the layout inflater
//        LayoutInflater inflater = getActivity().getLayoutInflater();
//
//        // Inflate and set the layout for the dialog
//        // Pass null as the parent view because its going in the dialog layout
//        builder.setView(inflater.inflate(R.layout.fragment_track_player, null));
//        return builder.create();

        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_track_player, container, false);

        mIsPaused = false;
        mCurrentPosition = 0;

        loadArguments();
        setupActionBar();
        setLayoutComponentsValues();
        setupPlayerControlButtons();

        return mRootView;
    }

    private void loadArguments() {
        Bundle arguments = getArguments();
        mArtist = arguments.getParcelable(getString(R.string.extra_artist));
        mTracks = arguments.getParcelableArrayList(getString(R.string.extra_tracks));

        // FIXME: Handle invalid index
        mCurrentTrackIndex = arguments.getInt(getString(R.string.extra_track_index));
        mCurrentTrack = mTracks.get(mCurrentTrackIndex);
    }

    private void setupActionBar() {
        // http://www.slideshare.net/cbeyls/android-32084115 (slide 13)
        mActionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setTitle(mArtist.name);
        mActionBar.setSubtitle(mCurrentTrack.name);
    }

    private void setLayoutComponentsValues() {
        TextView artistNameTextView = (TextView) mRootView.findViewById(R.id.trackPlayerArtistName);
        TextView albumNameTextView = (TextView) mRootView.findViewById(R.id.trackPlayerAlbumName);
        TextView trackNameTextView = (TextView) mRootView.findViewById(R.id.trackPlayerTrackName);
        ImageView albumImageImageView = (ImageView) mRootView.findViewById(R.id.trackPlayerAlbumImage);

        artistNameTextView.setText(mArtist.name);
        albumNameTextView.setText(mCurrentTrack.album.name);
        trackNameTextView.setText(mCurrentTrack.name);

        Picasso.with(getActivity())
                .load(getAlbumImage())
                .placeholder(R.drawable.art_cover_placeholder)
                .error(R.drawable.art_cover_placeholder)
                .into(albumImageImageView);
    }

    private String getAlbumImage() {
        String imageUrl = null;
        if (!mCurrentTrack.album.images.isEmpty()) {
            imageUrl = mCurrentTrack.album.images.get(0).url;
        }

        return imageUrl;
    }

    private void setupPlayerControlButtons() {
        setupPreviousTrackButton();
        setupPlayTrackButton();
        setupNextTrackButton();
    }

    private void setupPreviousTrackButton() {
        ImageButton previousImageButton = (ImageButton) mRootView.findViewById(R.id.trackPlayerPreviousButton);

        previousImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                previous();
            }
        });
    }

    private void setupPlayTrackButton() {
        final ImageButton playImageButton = playImageButton();

        playImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isPlaying = !isPlaying;
                if (isPlaying) {
                    play();
                } else {
                    pause();
                }
            }
        });
    }

    private ImageButton playImageButton() {
        return (ImageButton) mRootView.findViewById(R.id.trackPlayerPlayButton);
    }

    private void play() {
        String url = mCurrentTrack.preview_url;

        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();

            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    Toast.makeText(getActivity(), "An error occurred while playing the track.", Toast.LENGTH_SHORT).show();
                    mMediaPlayer.reset();
                    mIsPaused = false;
                    // ... react appropriately ...
                    // The MediaPlayer has moved to the Error state, must be reset!
                    return false;
                }
            });

            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mMediaPlayer.start();
                    mIsPaused = false;
                    playImageButton().setImageResource(android.R.drawable.ic_media_pause);

                }
            });

            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    playNext();
                }
            });
        }

        if (mIsPaused) {
            mIsPaused = false;
            mMediaPlayer.start();
            mMediaPlayer.seekTo(mCurrentPosition);
            playImageButton().setImageResource(android.R.drawable.ic_media_pause);


            return;
        }

        mMediaPlayer.reset();
        try {
            mMediaPlayer.setDataSource(url);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mMediaPlayer.prepareAsync();
    }

    private void playNext() {
        foward();
        play();
    }

    private void pause() {
        if (mIsPaused) {
            return;
        }

        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mIsPaused = true;
            mCurrentPosition = mMediaPlayer.getCurrentPosition();
            mMediaPlayer.pause();
            playImageButton().setImageResource(android.R.drawable.ic_media_play);
        }
    }

    /**
     * Move the player to the previous track. If currently on the last track, moves to the
     * last one.
     */
    private void previous() {
        boolean isOnFirstTrack = (mCurrentTrackIndex == 0);
        if (isOnFirstTrack) {
            mCurrentTrackIndex = (mTracks.size() - 1);
        } else {
            mCurrentTrackIndex--;
        }

        updateCurrentTrack();
    }

    /**
     * Move the player to the next track. If currently on the last track, moves to the first one.
     */
    private void foward() {
        boolean isOnLastTrack = mCurrentTrackIndex == (mTracks.size() - 1);
        if (isOnLastTrack) {
            mCurrentTrackIndex = 0;
        } else {
            mCurrentTrackIndex++;
        }

        updateCurrentTrack();
    }

    /**
     * Update screen with the new current track info. If is currently playing, start playing the
     * new current track.
     */
    private void updateCurrentTrack() {
        mCurrentPosition = 0;
        mIsPaused = false;
        mCurrentTrack = mTracks.get(mCurrentTrackIndex);
        setLayoutComponentsValues();
        mActionBar.setSubtitle(mCurrentTrack.name);

        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            play();
        }
    }

    private void setupNextTrackButton() {
        ImageButton nextImageButton = (ImageButton) mRootView.findViewById(R.id.trackPlayerNextButton);
        nextImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                foward();
            }
        });
    }
}
