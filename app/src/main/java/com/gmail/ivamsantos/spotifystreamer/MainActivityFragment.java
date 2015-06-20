package com.gmail.ivamsantos.spotifystreamer;

import android.content.Context;
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
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.gmail.ivamsantos.spotifystreamer.adapter.ArtistAdapter;

import java.util.ArrayList;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {
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

        return mRootView;
    }

    private void initSpotifyService() {
        SpotifyApi api = new SpotifyApi();
        mSpotify = api.getService();
    }

    private void initArtistsSearchBox() {
        EditText searchBox = (EditText) mRootView.findViewById(R.id.editTextSearchBox);
        searchBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
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

    private void hideSoftKeyboard(IBinder windowToken) {
        InputMethodManager imm =
                (InputMethodManager) getActivity().getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(windowToken, 0);
    }

    private void searchArtists(final String searchTerm) {
        mSpotify.searchArtists(searchTerm, new Callback<ArtistsPager>() {
            @Override
            public void success(final ArtistsPager artistsPager, Response response) {
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        mArtistsAdapter.clear();
                        for (Artist artist : artistsPager.artists.items) {
                            mArtistsAdapter.add(artist);
                        }
                    }
                });
            }

            @Override
            public void failure(RetrofitError error) {
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(getActivity(), "Failed to search for '" + searchTerm + "'", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void initArtistsListView() {
        ListView listView = (ListView) mRootView.findViewById(R.id.listViewArtists);
        listView.setAdapter(mArtistsAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Artist artist = mArtistsAdapter.getItem(position);
                Toast.makeText(getActivity(), artist.name, Toast.LENGTH_SHORT).show();
//                Intent showForecastIntent = new Intent(getActivity(), ForecastDetailActivity.class);
//                showForecastIntent.putExtra(getString(R.string.intent_forecast_detail_object_key), forecast);
//                startActivity(showForecastIntent);
            }
        });
    }
}
