package com.gmail.ivamsantos.spotifystreamer;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.gmail.ivamsantos.spotifystreamer.adapter.TrackAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Track;


/**
 * A placeholder fragment containing a simple view.
 */
public class ArtistTopTracksActivityFragment extends Fragment {
    public final static String LOG_TAG = ArtistTopTracksActivityFragment.class.getSimpleName();

    private ArrayAdapter<Track> mTracksAdapter;
    private View mRootView;
    private SpotifyService mSpotify;
    private String artistId;
    private String artistName;

    public ArtistTopTracksActivityFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);

        ArrayList<Track> tracks = new ArrayList<>();
        mTracksAdapter = new TrackAdapter(getActivity().getApplicationContext(), tracks);

        initSpotifyService();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_artist_top_tracks, container, false);

        Intent intent = getActivity().getIntent();
        artistId = intent.getStringExtra(getString(R.string.extra_artist_id));
        artistName = intent.getStringExtra(getString(R.string.extra_artist_name));

        setupActionBar();
        setupTracksListView();
        new LoadTopTracksTask().execute();

        return mRootView;
    }

    private void setupActionBar() {
        // http://www.slideshare.net/cbeyls/android-32084115 (slide 13)
        ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setSubtitle(artistName);
    }

    private void setupTracksListView() {
        ListView listView = (ListView) mRootView.findViewById(R.id.listViewTopTracks);
        listView.setAdapter(mTracksAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Track track = mTracksAdapter.getItem(position);
                String toastContent = track.name + " - " + track.album.name;
                Toast.makeText(getActivity().getApplicationContext(), toastContent, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initSpotifyService() {
        SpotifyApi api = new SpotifyApi();
        mSpotify = api.getService();
    }

    public void setUiLoadingTracksState() {
        hideTracksList();
        hideNoTracksMessage();
        showProgressBar();
    }

    private void showTracksList() {
        setTracksListVisibility(View.VISIBLE);
    }

    private void hideTracksList() {
        setTracksListVisibility(View.GONE);
    }

    private void setTracksListVisibility(int visibility) {
        mRootView.findViewById(R.id.listViewTopTracks).setVisibility(visibility);
    }

    private void showNoTracksMessage() {
        setNoTracksMessageVisibility(View.VISIBLE);
    }

    private void hideNoTracksMessage() {
        setNoTracksMessageVisibility(View.GONE);
    }

    private void setNoTracksMessageVisibility(int visibility) {
        mRootView.findViewById(R.id.noTracksMessage).setVisibility(visibility);
    }

    private void showProgressBar() {
        setProgressBarVisibility(View.VISIBLE);
    }

    private void hideProgressBar() {
        setProgressBarVisibility(View.GONE);
    }

    private void setProgressBarVisibility(int visibility) {
        mRootView.findViewById(R.id.loadTracksProgressBar).setVisibility(visibility);
    }

    private String getCountry() {
        return getActivity().getApplicationContext().getResources().getConfiguration().locale.getCountry();
    }


    private class LoadTopTracksTask extends AsyncTask<Void, Void, List<Track>> {
        private final String LOG_TAG = LoadTopTracksTask.class.getSimpleName();

        @Override
        protected void onPreExecute() {
            setUiLoadingTracksState();
            Log.d(LOG_TAG, "Entering onPreExecute().");
        }

        @Override
        protected List<Track> doInBackground(Void... params) {
            Map<String, Object> options = new HashMap<>();
            options.put(SpotifyService.COUNTRY, getCountry());

            List<Track> tracks = null;
            try {
                tracks = mSpotify.getArtistTopTrack(artistId, options).tracks;
                Log.d(LOG_TAG, "Found " + tracks.size() + " tracks.");
            } catch (Exception e) {
                Log.e(LOG_TAG, "Failed to load tracks with error: " + e.getMessage());
            }

            return tracks;
        }

        @Override
        protected void onPostExecute(List<Track> tracks) {
            Log.d(LOG_TAG, "Entering onPostExecute().");

            hideProgressBar();

            if (tracks == null) {
                Log.d(LOG_TAG, "tracks is null. An exception probably happened while contacting Spotify services.");
                Toast.makeText(getActivity(), getString(R.string.top_tracks_loading_failure_message), Toast.LENGTH_SHORT).show();
                return;
            }

            if (tracks.isEmpty()) {
                Log.d(LOG_TAG, "tracks is empty.");
                showNoTracksMessage();
                return;
            }

            Log.d(LOG_TAG, "Found " + tracks.size() + " artists. Updating mArtistsAdapter.");
            mTracksAdapter.clear();
            for (Track track : tracks) {
                mTracksAdapter.add(track);
            }
            showTracksList();
        }
    }
}
