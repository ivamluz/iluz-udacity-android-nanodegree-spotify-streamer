package com.gmail.ivamsantos.spotifystreamer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.util.List;

import kaaes.spotify.webapi.android.models.Artist;


public class MainActivity extends ActionBarActivity implements ArtistsFragment.OnArtistSelectedListener, ArtistsFragment.OnArtistsLoadedListener {
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
            Log.i(LOG_TAG, "On single panel layout. Launching " + ArtistTopTracksActivity.class.getSimpleName() + ".");
            launchTopTracksActivity(artist);
        }
    }

    @Override
    public void onArtistsLoadedListener(List<Artist> artists) {
        if (artists != null && !artists.isEmpty()) {
            setupTopTracksFragment(artists.get(0));
        }
    }

    private void launchTopTracksActivity(Artist artist) {
        Intent showTopTracksIntent = new Intent(this, ArtistTopTracksActivity.class);
        showTopTracksIntent.putExtra(ArtistTopTracksFragment.ARGUMENT_KEY_ARTIST, artist);
        startActivity(showTopTracksIntent);
    }

    private void setupTopTracksFragment(Artist artist) {
        ArtistTopTracksFragment topTracksFragment =
                (ArtistTopTracksFragment) getSupportFragmentManager().findFragmentById(R.id.artist_top_tracks_container);

        boolean topTracksFragmentDemandsInitialization = topTracksFragment == null
                || topTracksFragment.getArtist() == null
                || topTracksFragment.getArtist().id != artist.id;

        if (topTracksFragmentDemandsInitialization) {
            topTracksFragment = ArtistTopTracksFragment.withArtist(artist);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.artist_top_tracks_container, topTracksFragment, ArtistTopTracksFragment.TAG)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();
        }
    }
}
