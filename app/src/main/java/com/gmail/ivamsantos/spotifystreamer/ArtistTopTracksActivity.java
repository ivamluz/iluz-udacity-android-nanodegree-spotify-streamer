package com.gmail.ivamsantos.spotifystreamer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.Track;


public class ArtistTopTracksActivity extends ActionBarActivity implements ArtistTopTracksFragment.OnTrackSelectedListener {
    public final static String LOG_TAG = ArtistTopTracksActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_top_tracks);

        if (savedInstanceState == null) {
            setupTopTracksFragment();
        }
    }

    private void setupTopTracksFragment() {
        Artist artist = getIntent().getParcelableExtra(getString(R.string.extra_artist));
        Bundle arguments = new Bundle();
        arguments.putParcelable(getString(R.string.extra_artist), artist);

        ArtistTopTracksFragment fragment = new ArtistTopTracksFragment();
        fragment.setArguments(arguments);

        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.artist_top_tracks_container, fragment)
                .commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_artist_top_tracks, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);

                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onTrackSelectedListener(Artist artist, List<Track> tracks, int position) {
        Intent showTopTracksIntent = new Intent(this, TrackPlayerActivity.class);
        showTopTracksIntent.putExtra(getString(R.string.extra_artist), artist);
        showTopTracksIntent.putExtra(getString(R.string.extra_track_index), position);
        showTopTracksIntent.putExtra(getString(R.string.extra_tracks), (ArrayList) tracks);

        startActivity(showTopTracksIntent);
    }
}
