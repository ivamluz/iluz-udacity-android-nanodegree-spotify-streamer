package com.gmail.ivamsantos.spotifystreamer;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
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
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


/**
 * Main fragment for searching artists.
 */
public class MainActivityFragment extends Fragment {
    public static final String LOG_TAG = MainActivityFragment.class.getSimpleName();
    public static final int SEARCH_LIMIT = 50;
    public static final String INITIAL_SEARCH_TERM = "a";

    private ArrayAdapter<Artist> mArtistsAdapter;
    private View mRootView;
    private SpotifyService mSpotify;

    public MainActivityFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);

        // http://stackoverflow.com/a/28667895
        ArrayList<Artist> artists = new ArrayList<>();
        mArtistsAdapter = new ArtistAdapter(getActivity().getApplicationContext(), artists);

        initSpotifyService();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_main, container, false);

        initArtistsSearchBox();
        initArtistsListView();

        loadInitialArtists();

        return mRootView;
    }

    private void loadInitialArtists() {
        searchArtists(INITIAL_SEARCH_TERM);
    }

    private void initSpotifyService() {
        SpotifyApi api = new SpotifyApi();
        mSpotify = api.getService();
    }

    private void initArtistsSearchBox() {
        searchBox().setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView field, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String searchTerm = field.getText().toString();
                    searchArtists(searchTerm);

                    hideSoftKeyboard(field.getWindowToken());

                    handled = true;
                }
                return handled;
            }
        });
    }

    private TextView searchBox() {
        return (TextView) mRootView.findViewById(R.id.searchBox);
    }

    private void hideSoftKeyboard(IBinder windowToken) {
        InputMethodManager imm =
                (InputMethodManager) getActivity().getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(windowToken, 0);
    }

    private void searchArtists(final String searchTerm) {
        setUiSearchingState();

        Map<String, Object> options = new HashMap<>();
        options.put(SpotifyService.LIMIT, SEARCH_LIMIT);
        mSpotify.searchArtists(searchTerm, options, new Callback<ArtistsPager>() {
            @Override
            public void success(final ArtistsPager artistsPager, Response response) {
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        hideProgressBar();

                        boolean hasItems = (artistsPager.artists.items.size() > 0);
                        if (hasItems) {
                            showResultsList();

                            mArtistsAdapter.clear();
                            for (Artist artist : artistsPager.artists.items) {
                                mArtistsAdapter.add(artist);
                            }
                        } else {
                            showNoResultsMessage();
                        }
                    }
                });
            }

            @Override
            public void failure(RetrofitError error) {
                hideProgressBar();
                // showErrorMessage();

                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(getActivity(), "Failed to search for '" + searchTerm + "'", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void setUiSearchingState() {
        showProgressBar();
        hideResultsList();
        hideNoResultsMessage();
    }

    private void initArtistsListView() {
        ListView listView = (ListView) mRootView.findViewById(R.id.listViewArtists);
        listView.setAdapter(mArtistsAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Artist artist = mArtistsAdapter.getItem(position);

                Intent showTopTracksIntent = new Intent(getActivity(), ArtistTopTracksActivity.class);
                showTopTracksIntent.putExtra(getString(R.string.extra_artist_id), artist.id);
                showTopTracksIntent.putExtra(getString(R.string.extra_artist_name), artist.name);

                startActivity(showTopTracksIntent);
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
        mRootView.findViewById(R.id.listViewArtists).setVisibility(visibility);
    }

    private void showNoResultsMessage() {
        setNoResultsMessageVisibility(View.VISIBLE);
    }

    private void hideNoResultsMessage() {
        setNoResultsMessageVisibility(View.GONE);
    }

    private void setNoResultsMessageVisibility(int visibility) {
        mRootView.findViewById(R.id.noResultsMessage).setVisibility(visibility);
    }

    private void showProgressBar() {
        setProgressBarVisibility(View.VISIBLE);
    }

    private void hideProgressBar() {
        setProgressBarVisibility(View.GONE);
    }

    private void setProgressBarVisibility(int visibility) {
        mRootView.findViewById(R.id.searchProgressBar).setVisibility(visibility);
    }
}
