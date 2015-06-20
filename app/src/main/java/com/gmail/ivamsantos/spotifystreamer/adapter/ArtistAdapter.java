package com.gmail.ivamsantos.spotifystreamer.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.gmail.ivamsantos.spotifystreamer.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import kaaes.spotify.webapi.android.models.Artist;

/**
 * Created by iluz on 19/06/15.
 *
 * https://github.com/codepath/android_guides/wiki/Using-an-ArrayAdapter-with-ListView
 */
public class ArtistAdapter extends ArrayAdapter<Artist> {
    public ArtistAdapter(Context context, ArrayList<Artist> artists) {
        super(context, R.layout.list_item_artist, artists);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Artist artist = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_artist, parent, false);
        }

        ImageView artistImage = (ImageView) convertView.findViewById(R.id.artistImage);
        TextView artistName = (TextView) convertView.findViewById(R.id.artistName);

        if (artist.images.size() > 0) {
            // http://javatechig.com/android/how-to-use-picasso-library-in-android
            Picasso.with(getContext())
                    .load(artist.images.get(0).url)
                    .placeholder(R.drawable.ic_artist_placeholder)
                    .error(R.drawable.ic_error)
                    .into(artistImage);
        }

        artistName.setText(artist.name);

        return convertView;
    }
}
