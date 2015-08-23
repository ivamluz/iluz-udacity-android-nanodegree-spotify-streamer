package com.gmail.ivamsantos.spotifystreamer.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.gmail.ivamsantos.spotifystreamer.R;
import com.gmail.ivamsantos.spotifystreamer.helper.SpotifyImageHelper;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import kaaes.spotify.webapi.android.models.Track;

/**
 * Created by iluz on 19/06/15.
 * <p/>
 * https://github.com/codepath/android_guides/wiki/Using-an-ArrayAdapter-with-ListView
 */
public class TrackAdapter extends ArrayAdapter<Track> {
    public TrackAdapter(Context context, ArrayList<Track> tracks) {
        super(context, R.layout.list_item_artist, tracks);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Track track = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_track, parent, false);
        }

        ImageView albumImage = (ImageView) convertView.findViewById(R.id.album_image);
        TextView trackName = (TextView) convertView.findViewById(R.id.track_name);
        TextView albumName = (TextView) convertView.findViewById(R.id.album_name);

        String imageUrl = SpotifyImageHelper.getPreferredImageUrl(track.album.images);
        Picasso.with(getContext())
                .load(imageUrl)
                .placeholder(R.drawable.ic_artist_placeholder)
                .error(R.drawable.ic_error)
                .into(albumImage);

        trackName.setText(track.name);
        albumName.setText(track.album.name);

        return convertView;
    }
}
