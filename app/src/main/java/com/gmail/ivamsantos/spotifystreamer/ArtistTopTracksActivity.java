package com.gmail.ivamsantos.spotifystreamer;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import kaaes.spotify.webapi.android.models.Artist;


public class ArtistTopTracksActivity extends ActionBarActivity {
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
        Artist artist = getIntent().getParcelableExtra(ArtistTopTracksFragment.ARGUMENT_KEY_ARTIST);
        ArtistTopTracksFragment fragment = ArtistTopTracksFragment.withArtist(artist);

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
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
