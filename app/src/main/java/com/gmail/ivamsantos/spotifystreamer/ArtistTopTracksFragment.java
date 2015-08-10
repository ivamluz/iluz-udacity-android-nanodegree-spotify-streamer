package com.gmail.ivamsantos.spotifystreamer;

import android.app.Activity;
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
import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.Track;


/**
 * A placeholder fragment containing a simple view.
 */
public class ArtistTopTracksFragment extends Fragment {
    public static final String TAG = ArtistTopTracksFragment.class.getSimpleName();
    private static final String LOG_TAG = ArtistTopTracksFragment.class.getSimpleName();
    private ArrayAdapter<Track> mTracksAdapter;
    private View mRootView;
    private SpotifyService mSpotify;

    private Artist mArtist;
    private ArrayList<Track> mTracks;

    private OnTrackSelectedListener mOnTrackSelectedListener;

    public ArtistTopTracksFragment() {
    }

    public interface OnTrackSelectedListener {
        void onTrackSelectedListener(Artist artist, List<Track> tracks, int position);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mOnTrackSelectedListener = (OnTrackSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnTrackSelectedListener");
        }
    }

    public Artist getArtist() {
        return mArtist;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);

        mTracks = new ArrayList<>();
        mTracksAdapter = new TrackAdapter(getActivity().getApplicationContext(), mTracks);

        initSpotifyService();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_artist_top_tracks, container, false);

        Bundle arguments = getArguments();
        if (arguments != null) {
            mArtist = arguments.getParcelable(getString(R.string.extra_artist));

            setupActionBar();
            setupTracksListView();

            if (mArtist != null) {
                new LoadTopTracksTask().execute();
            } else {
                hideTracksList();
                showNoTracksMessage();
            }
        }

        return mRootView;
    }

    private void setupActionBar() {
        if (mArtist == null) {
            return;
        }

        // http://www.slideshare.net/cbeyls/android-32084115 (slide 13)
        ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setSubtitle(mArtist.name);
        }
    }

    private void setupTracksListView() {
        ListView listView = (ListView) mRootView.findViewById(R.id.listViewTopTracks);
        listView.setAdapter(mTracksAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mOnTrackSelectedListener.onTrackSelectedListener(mArtist, mTracks, position);
            }
        });
    }

    private void initSpotifyService() {
        SpotifyApi api = new SpotifyApi();
        mSpotify = api.getService();
    }

    private void setUiLoadingTracksState() {
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


    private class LoadTopTracksTask extends AsyncTask<Void, Void, ArrayList<Track>> {
        private final String LOG_TAG = LoadTopTracksTask.class.getSimpleName();

        @Override
        protected void onPreExecute() {
            setUiLoadingTracksState();
            Log.d(LOG_TAG, "Entering onPreExecute().");
        }

        @Override
        protected ArrayList<Track> doInBackground(Void... params) {
            try {
                mTracks = new ArrayList<>(mSpotify.getArtistTopTrack(mArtist.id, getCountry()).tracks);
                Log.d(LOG_TAG, "Found " + mTracks.size() + " mTracks.");
            } catch (Exception e) {
                Log.e(LOG_TAG, "Failed to load mTracks with error: " + e.getMessage());
            }

            return mTracks;
        }

        @Override
        protected void onPostExecute(ArrayList<Track> tracks) {
            Log.d(LOG_TAG, "Entering onPostExecute().");

            hideProgressBar();

            if (tracks == null) {
                Log.d(LOG_TAG, "mTracks is null. An exception probably happened while contacting Spotify services.");
                Toast.makeText(getActivity(), getString(R.string.top_tracks_loading_failure_message), Toast.LENGTH_SHORT).show();
                return;
            }

            if (tracks.isEmpty()) {
                Log.d(LOG_TAG, "mTracks is empty.");
                showNoTracksMessage();
                return;
            }

            Log.d(LOG_TAG, "Found " + tracks.size() + " artists. Updating mTracksAdapter.");
            mTracksAdapter.clear();
            for (Track track : tracks) {
                mTracksAdapter.add(track);
            }
            showTracksList();
        }
    }
}
