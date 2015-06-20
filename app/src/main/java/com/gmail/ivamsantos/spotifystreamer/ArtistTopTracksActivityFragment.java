package com.gmail.ivamsantos.spotifystreamer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


/**
 * A placeholder fragment containing a simple view.
 */
public class ArtistTopTracksActivityFragment extends Fragment {

    public ArtistTopTracksActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_artist_top_tracks, container, false);

        TextView tv = (TextView) rootView.findViewById(R.id.trackArtistName);

        Intent intent = getActivity().getIntent();
        String artistId = intent.getStringExtra(getString(R.string.extra_artist_id));
        String artistName = intent.getStringExtra(getString(R.string.extra_artist_name));

        tv.setText(artistId + " - " + artistName);

        return rootView;
    }
}
