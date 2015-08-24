package com.gmail.ivamsantos.spotifystreamer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.ShareActionProvider;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;

import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.Track;


public class TrackPlayerActivity extends ActionBarActivity {
    private ShareActionProvider mShareActionProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_player);

        if (savedInstanceState == null) {
            setupTrackPlayerFragment();
        }
    }

    private void setupTrackPlayerFragment() {
        Intent intent = getIntent();

        Artist artist = intent.getParcelableExtra(getString(R.string.extra_artist));
        int position = intent.getIntExtra(getString(R.string.extra_track_index), -1);
        ArrayList<Track> tracks = intent.getParcelableArrayListExtra(getString(R.string.extra_tracks));

        Bundle arguments = new Bundle();
        arguments.putParcelable(getString(R.string.extra_artist), artist);
        arguments.putInt(getString(R.string.extra_track_index), position);
        arguments.putParcelableArrayList(getString(R.string.extra_tracks), tracks);

        TrackPlayerDialogFragment fragment = new TrackPlayerDialogFragment();
        fragment.setArguments(arguments);

        getSupportFragmentManager().beginTransaction()
                .add(R.id.track_player_container, fragment)
                .commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_track_player, menu);
        setupShareMenu(menu);

        return true;
    }

    private void setupShareMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.menu_item_share);

        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
        if (mShareActionProvider == null) {
            return;
        }

        Intent shareIntent = new Intent();
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setAction(Intent.ACTION_SEND);

        Intent intent = getIntent();
        Artist artist = getIntent().getParcelableExtra(getString(R.string.extra_artist));
        int position = getIntent().getIntExtra(getString(R.string.extra_track_index), -1);
        ArrayList<Track> tracks = getIntent().getParcelableArrayListExtra(getString(R.string.extra_tracks));
        Track track = tracks.get(position);

        CharSequence textToShare = String.format("%s - %s: %s", artist.name, track.name, track.preview_url);
        shareIntent.putExtra(Intent.EXTRA_TEXT, textToShare);
        shareIntent.setType("text/plain");
        mShareActionProvider.setShareIntent(shareIntent);
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
