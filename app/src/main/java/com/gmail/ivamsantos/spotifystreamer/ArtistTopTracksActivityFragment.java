package com.gmail.ivamsantos.spotifystreamer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
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
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


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
        initTracksListView();
        loadTopTracks(artistId);

        return mRootView;
    }

    private void setupActionBar() {
        // http://www.slideshare.net/cbeyls/android-32084115 (slide 13)
        ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setSubtitle(artistName);
    }

    private void initTracksListView() {
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

    private void loadTopTracks(final String artistId) {
        setUiLoadingTracksState();

        Map<String, Object> options = new HashMap<>();
        options.put(SpotifyService.COUNTRY, getCountry());
        mSpotify.getArtistTopTrack(artistId, options, new Callback<Tracks>() {
            @Override
            public void success(final Tracks tracks, Response response) {
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        hideProgressBar();

                        boolean hasItems = (tracks.tracks.size() > 0);
                        if (hasItems) {
                            showTracksList();

                            mTracksAdapter.clear();
                            for (Track track : tracks.tracks) {
                                mTracksAdapter.add(track);
                            }
                        } else {
                            showNoTracksMessage();
                        }
                    }
                });
            }

            @Override
            public void failure(RetrofitError error) {
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        hideProgressBar();
                        Toast.makeText(getActivity(), R.string.top_tracks_loading_failure_message, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private String getCountry() {
        return getActivity().getApplicationContext().getResources().getConfiguration().locale.getCountry();
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
}
