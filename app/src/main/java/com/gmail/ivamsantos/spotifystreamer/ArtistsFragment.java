package com.gmail.ivamsantos.spotifystreamer;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.gmail.ivamsantos.spotifystreamer.adapter.ArtistAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;


/**
 * Main fragment for searching artists.
 */
public class ArtistsFragment extends Fragment {
    public static final String LOG_TAG = ArtistsFragment.class.getSimpleName();
    private static final int SEARCH_LIMIT = 50;
    private static final String INITIAL_SEARCH_TERM = "a";
    public static final String BUNDLE_KEY_ARTISTS = "artists";
    public static final String BUNDLE_KEY_SEARCH_TERM = "search-term";

    private SharedPreferences mPreferences;

    private ArrayList<Artist> mArtists;
    private ArrayAdapter<Artist> mArtistsAdapter;
    private View mRootView;
    private SpotifyService mSpotify;

    private OnArtistSelectedListener mOnArtistSelectedListener;
    private OnArtistsLoadedListener mOnArtistsLoadedListener;

    public ArtistsFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        initSpotifyService();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_artists, container, false);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        String searchTerm = "";
        mArtists = new ArrayList<>();
        if (savedInstanceState != null) {
            searchTerm = savedInstanceState.getString(BUNDLE_KEY_SEARCH_TERM);
            mArtists = savedInstanceState.getParcelableArrayList(BUNDLE_KEY_ARTISTS);
        }
        mArtistsAdapter = new ArtistAdapter(getActivity().getApplicationContext(), mArtists);

        setupArtistsSearchBox(searchTerm);
        setupArtistsListView();

        if (mArtists.isEmpty()) {
            loadInitialArtists();
        }

        return mRootView;
    }

    private void loadInitialArtists() {
        new SearchArtistsTask().execute(INITIAL_SEARCH_TERM);
    }

    private void initSpotifyService() {
        SpotifyApi api = new SpotifyApi();
        mSpotify = api.getService();
    }

    private void setupArtistsSearchBox(String searchTerm) {
        TextView searchBox = searchBox();
        searchBox.setText(searchTerm);
        searchBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView field, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String searchTerms = field.getText().toString();
                    new SearchArtistsTask().execute(searchTerms);

                    hideSoftKeyboard(field.getWindowToken());

                    handled = true;
                }
                return handled;
            }
        });
    }

    private TextView searchBox() {
        return (TextView) mRootView.findViewById(R.id.search_box);
    }

    private void hideSoftKeyboard(IBinder windowToken) {
        InputMethodManager imm =
                (InputMethodManager) getActivity().getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(windowToken, 0);
    }

    private void setUiSearchingState() {
        showProgressBar();
        hideResultsList();
        hideNoResultsMessage();
    }

    private void setupArtistsListView() {
        ListView listView = artistsList();
        listView.setAdapter(mArtistsAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Artist artist = mArtistsAdapter.getItem(position);
                mOnArtistSelectedListener.onArtistSelectedListener(artist);
            }
        });
    }

    private void showResultsList() {
        setResultsListVisibility(View.VISIBLE);
    }

    private void hideResultsList() {
        setResultsListVisibility(View.GONE);
    }

    private void setResultsListVisibility(int visibility) {
        artistsList().setVisibility(visibility);
    }

    private ListView artistsList() {
        return (ListView) mRootView.findViewById(R.id.list_artists);
    }

    private void showNoResultsMessage() {
        setNoResultsMessageVisibility(View.VISIBLE);
    }

    private void hideNoResultsMessage() {
        setNoResultsMessageVisibility(View.GONE);
    }

    private void setNoResultsMessageVisibility(int visibility) {
        mRootView.findViewById(R.id.no_artists_message).setVisibility(visibility);
    }

    private void showProgressBar() {
        setProgressBarVisibility(View.VISIBLE);
    }

    private void hideProgressBar() {
        setProgressBarVisibility(View.GONE);
    }

    private void setProgressBarVisibility(int visibility) {
        mRootView.findViewById(R.id.search_progress_bar).setVisibility(visibility);
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putParcelableArrayList(BUNDLE_KEY_ARTISTS, mArtists);
        bundle.putString(BUNDLE_KEY_SEARCH_TERM, searchBox().getText().toString());
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mOnArtistSelectedListener = (OnArtistSelectedListener) activity;
            mOnArtistsLoadedListener = (OnArtistsLoadedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement both OnArtistSelectedListener and OnArtistsLoadedListener interfaces.");
        }
    }

    public interface OnArtistSelectedListener {
        void onArtistSelectedListener(Artist artist);
    }

    public interface OnArtistsLoadedListener {
        void onArtistsLoadedListener(List<Artist> artists);
    }

    private class SearchArtistsTask extends AsyncTask<String, Integer, List<Artist>> {
        private final String LOG_TAG = SearchArtistsTask.class.getSimpleName();

        @Override
        protected void onPreExecute() {
            setUiSearchingState();
            Log.d(LOG_TAG, "Entering onPreExecute().");
        }

        @Override
        protected List<Artist> doInBackground(String... params) {
            boolean isSearchTermsEmpty = (params == null) || (params.length == 0);
            if (isSearchTermsEmpty) {
                Log.d(LOG_TAG, "Search term is empty. Skipping doInBackground().");
                return null;
            }

            String searchTerms = params[0];
            Map<String, Object> options = new HashMap<>();
            options.put(SpotifyService.LIMIT, SEARCH_LIMIT);
            String countryCode = mPreferences.getString(getString(R.string.pref_key_country_code), getString(R.string.pref_country_code_default));
            options.put(SpotifyService.COUNTRY, countryCode);

            List<Artist> artists = null;
            try {
                Log.d(LOG_TAG, String.format("Querying spotify. Params: [searchTerms: %s, country: %s]", searchTerms, countryCode));
                artists = mSpotify.searchArtists(searchTerms, options).artists.items;
                Log.d(LOG_TAG, "Found " + artists.size() + " artists.");
            } catch (Exception e) {
                Log.e(LOG_TAG, "Failed to search artists with error: " + e.getMessage());
            }

            return artists;
        }

        @Override
        protected void onPostExecute(List<Artist> artists) {
            Log.d(LOG_TAG, "Entering onPostExecute().");

            mOnArtistsLoadedListener.onArtistsLoadedListener(artists);

            hideProgressBar();

            if (artists == null) {
                Log.d(LOG_TAG, "artists is null. An exception probably happened while contacting Spotify services.");
                Toast.makeText(getActivity(), getString(R.string.search_failure_message), Toast.LENGTH_SHORT).show();

                return;
            }

            if (artists.isEmpty()) {
                Log.d(LOG_TAG, "artists is empty.");
                showNoResultsMessage();

                return;
            }

            Log.d(LOG_TAG, "Found " + artists.size() + " artists. Updating mArtistsAdapter.");
            mArtistsAdapter.clear();
            for (Artist artist : artists) {
                mArtistsAdapter.add(artist);
            }

            ListView artistsListView = artistsList();
            if (!mArtistsAdapter.isEmpty()) {
                artistsListView.smoothScrollToPosition(0);
            }

            showResultsList();
        }
    }
}
