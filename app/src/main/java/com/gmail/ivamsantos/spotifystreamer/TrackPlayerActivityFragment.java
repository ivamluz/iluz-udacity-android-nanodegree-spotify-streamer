package com.gmail.ivamsantos.spotifystreamer;

import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import kaaes.spotify.webapi.android.models.Track;


/**
 * A placeholder fragment containing a simple view.
 */
public class TrackPlayerActivityFragment extends Fragment {
    public static final String LOG_TAG = TrackPlayerActivityFragment.class.getSimpleName();
    public static final int TRACK_INDEX_UNAVAILABLE = -1;

    boolean isPlaying = false;
    private View mRootView;
    private String mArtistName;
    private Track mCurrentTrack;
    private Integer mCurrentTrackIndex;
    private ArrayList<Track> mTracks;

    private ActionBar mActionBar;

    private MediaPlayer mMediaPlayer;

    public TrackPlayerActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_track_player, container, false);

        loadValuesFromIntent();
        setupActionBar();
        setLayoutComponentsValues();
        setupPlayerControlButtons();

        return mRootView;
    }

    private void loadValuesFromIntent() {
        Intent intent = getActivity().getIntent();
        mArtistName = intent.getStringExtra(getString(R.string.extra_artist_name));
        mTracks = intent.getParcelableArrayListExtra(getString(R.string.extra_tracks));

        // FIXME: Handle invalid index
        mCurrentTrackIndex = intent.getIntExtra(getString(R.string.extra_track_index), TRACK_INDEX_UNAVAILABLE);
        mCurrentTrack = mTracks.get(mCurrentTrackIndex);
    }

    private void setupActionBar() {
        // http://www.slideshare.net/cbeyls/android-32084115 (slide 13)
        mActionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setTitle(mArtistName);
        mActionBar.setSubtitle(mCurrentTrack.name);
    }

    private void setLayoutComponentsValues() {
        TextView artistNameTextView = (TextView) mRootView.findViewById(R.id.trackPlayerArtistName);
        TextView albumNameTextView = (TextView) mRootView.findViewById(R.id.trackPlayerAlbumName);
        TextView trackNameTextView = (TextView) mRootView.findViewById(R.id.trackPlayerTrackName);
        ImageView albumImageImageView = (ImageView) mRootView.findViewById(R.id.trackPlayerAlbumImage);
        TextView durationTextView = (TextView) mRootView.findViewById(R.id.trackPlayerDuration);

        artistNameTextView.setText(mArtistName);
        albumNameTextView.setText(mCurrentTrack.album.name);
        trackNameTextView.setText(mCurrentTrack.name);
        durationTextView.setText(getFormattedTrackDuration());

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
        final ImageButton playImageButton = (ImageButton) mRootView.findViewById(R.id.trackPlayerPlayButton);

        playImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isPlaying = !isPlaying;
                if (isPlaying) {
                    // http://stackoverflow.com/a/8354606
                    playImageButton.setImageResource(android.R.drawable.ic_media_pause);
                    play();
                } else {
                    playImageButton.setImageResource(android.R.drawable.ic_media_play);
                    pause();
                }
            }
        });
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
                    // ... react appropriately ...
                    // The MediaPlayer has moved to the Error state, must be reset!
                    return false;
                }
            });

            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mMediaPlayer.start();
                }
            });

            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    playNext();
                }
            });
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
        Toast.makeText(getActivity(), "Paused track.", Toast.LENGTH_SHORT).show();
    }

    /**
     * Move the player to the previous track. If currently on the last track, moves to the
     * last one.
     */
    private void previous() {
        boolean isOnFirstTrack = (mCurrentTrackIndex == 0);
        if (isOnFirstTrack) {
            int lastIndex = (mTracks.size() - 1);
            mCurrentTrackIndex = lastIndex;
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

    private String getFormattedTrackDuration() {
        // http://stackoverflow.com/a/625624
        return String.format("%d:%d",
                TimeUnit.MILLISECONDS.toMinutes(mCurrentTrack.duration_ms),
                TimeUnit.MILLISECONDS.toSeconds(mCurrentTrack.duration_ms) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(mCurrentTrack.duration_ms))
        );
    }

}
