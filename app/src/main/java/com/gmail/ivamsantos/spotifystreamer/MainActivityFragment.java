package com.gmail.ivamsantos.spotifystreamer;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {
    private ArrayAdapter<String> mArtistsAdapter;
    private View mRootView;

    public MainActivityFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_main, container, false);

//        List<String> artists = Arrays.asList("Ramones", "Pearl Jam", "Dead Fish", "Deep Purple");
        List<String> artists = new ArrayList<>();
        mArtistsAdapter = new ArrayAdapter<String>(getActivity(), R.layout.list_item_artist, R.id.artistTextView, artists);

        initArtistsSearchBox();
        initArtistsListView();

        return mRootView;
    }

    private void initArtistsSearchBox() {
        EditText searchBox = (EditText) mRootView.findViewById(R.id.editTextSearchBox);
        searchBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView field, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    Toast.makeText(getActivity(), "Searching for '" + field.getText() + "'", Toast.LENGTH_SHORT).show();
                    handled = true;
                }
                return handled;
            }
        });
    }

    private void initArtistsListView() {
        ListView listView = (ListView) mRootView.findViewById(R.id.listViewArtists);
        listView.setAdapter(mArtistsAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String artist = mArtistsAdapter.getItem(position);
                Toast.makeText(getActivity(), artist, Toast.LENGTH_SHORT).show();
//                Intent showForecastIntent = new Intent(getActivity(), ForecastDetailActivity.class);
//                showForecastIntent.putExtra(getString(R.string.intent_forecast_detail_object_key), forecast);
//                startActivity(showForecastIntent);
            }
        });
    }

}
