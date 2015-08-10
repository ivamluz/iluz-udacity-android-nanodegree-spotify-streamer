package com.gmail.ivamsantos.spotifystreamer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.Track;


public class MainActivity extends ActionBarActivity implements ArtistsFragment.OnArtistSelectedListener,
        ArtistsFragment.OnArtistsLoadedListener, ArtistTopTracksFragment.OnTrackSelectedListener {
    public static final String LOG_TAG = MainActivity.class.getSimpleName();

    boolean mIsDualPaneLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mIsDualPaneLayout = (findViewById(R.id.artist_top_tracks_container) != null);
        boolean hasSavedInstanceState = (savedInstanceState != null);
        if (mIsDualPaneLayout && !hasSavedInstanceState) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.artist_top_tracks_container, new ArtistTopTracksFragment())
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onArtistSelectedListener(Artist artist) {
        Log.i(LOG_TAG, "onArtistSelectedListener triggered.");

        if (mIsDualPaneLayout) {
            Log.i(LOG_TAG, "On dual panel layout. Setting " + ArtistTopTracksFragment.class.getSimpleName() + " fragment.");
            setupTopTracksFragment(artist);
        } else {
            Log.i(LOG_TAG, "On single panel layout. Launching " + ArtistTopTracksActivity.class.getSimpleName() + " activity.");
            launchTopTracksActivity(artist);
        }
    }

    @Override
    public void onArtistsLoadedListener(List<Artist> artists) {
        if (!mIsDualPaneLayout) {
            return;
        }

        if (artists != null && !artists.isEmpty()) {
            setupTopTracksFragment(artists.get(0));
        } else {
            setupTopTracksFragment(null);
        }
    }

    @Override
    public void onTrackSelectedListener(Artist artist, List<Track> tracks, int position) {
        Bundle arguments = new Bundle();
        arguments.putParcelable(getString(R.string.extra_artist), artist);
        arguments.putInt(getString(R.string.extra_track_index), position);
        arguments.putParcelableArrayList(getString(R.string.extra_tracks), (ArrayList) tracks);

        DialogFragment dialog = new TrackPlayerDialogFragment();
        dialog.setArguments(arguments);
        dialog.show(getSupportFragmentManager(), TrackPlayerDialogFragment.class.getSimpleName());
    }

    private void launchTopTracksActivity(Artist artist) {
        Intent showTopTracksIntent = new Intent(this, ArtistTopTracksActivity.class);
        showTopTracksIntent.putExtra(getString(R.string.extra_artist), artist);
        startActivity(showTopTracksIntent);
    }

    private void setupTopTracksFragment(Artist artist) {
        ArtistTopTracksFragment topTracksFragment =
                (ArtistTopTracksFragment) getSupportFragmentManager().findFragmentById(R.id.artist_top_tracks_container);

        boolean topTracksFragmentDemandsSetup = topTracksFragment == null
                || topTracksFragment.getArtist() == null
                || artist == null
                || topTracksFragment.getArtist().id != artist.id;

        if (topTracksFragmentDemandsSetup) {
            Bundle arguments = new Bundle();
            arguments.putParcelable(getString(R.string.extra_artist), artist);

            topTracksFragment = new ArtistTopTracksFragment();
            topTracksFragment.setArguments(arguments);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.artist_top_tracks_container, topTracksFragment, ArtistTopTracksFragment.TAG)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();
        }
    }
}
