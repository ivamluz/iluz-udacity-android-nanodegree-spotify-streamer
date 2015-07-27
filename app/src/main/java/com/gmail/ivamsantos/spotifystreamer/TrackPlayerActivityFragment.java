package com.gmail.ivamsantos.spotifystreamer;

import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
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

    private String mArtistName;
    private Track mCurrentTrack;
    private ArrayList<Track> mTracks;


    View mRootView;
    boolean isPlaying = false;

    public TrackPlayerActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_track_player, container, false);

        loadValuesFromIntent();
        setupActionBar();
        bindLayoutComponentsValues();
        setupPlayerControlButtons();

        return mRootView;
    }

    private void loadValuesFromIntent() {
        Intent intent = getActivity().getIntent();
        mArtistName = intent.getStringExtra(getString(R.string.extra_artist_name));
        mTracks = intent.getParcelableArrayListExtra(getString(R.string.extra_tracks));

        // FIXME: Handle invalid index
        int trackIndex = intent.getIntExtra(getString(R.string.extra_track_index), TRACK_INDEX_UNAVAILABLE);
        mCurrentTrack = mTracks.get(trackIndex);
    }

    private void setupActionBar() {
        // http://www.slideshare.net/cbeyls/android-32084115 (slide 13)
        ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(mArtistName);
        actionBar.setSubtitle(mCurrentTrack.name);
    }

    private void bindLayoutComponentsValues() {
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
                Toast.makeText(getActivity(), "Moving to previous track.", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(getActivity(), "Playing track.", Toast.LENGTH_SHORT).show();
                } else {
                    playImageButton.setImageResource(android.R.drawable.ic_media_play);
                    Toast.makeText(getActivity(), "Paused track.", Toast.LENGTH_SHORT).show();

                    String url = "http://........"; // your URL here
                    MediaPlayer mediaPlayer = new MediaPlayer();
                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    try {
                        mediaPlayer.setDataSource(url);
                        mediaPlayer.prepare();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    mediaPlayer.start();
                }
            }
        });
    }

    private void setupNextTrackButton() {
        ImageButton nextImageButton = (ImageButton) mRootView.findViewById(R.id.trackPlayerNextButton);
        nextImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getActivity(), "Moving to next track.", Toast.LENGTH_SHORT).show();
                ;
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
