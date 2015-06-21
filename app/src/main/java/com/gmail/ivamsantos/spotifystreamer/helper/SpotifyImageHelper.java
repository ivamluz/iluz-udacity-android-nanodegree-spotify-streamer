package com.gmail.ivamsantos.spotifystreamer.helper;

import java.util.List;

import kaaes.spotify.webapi.android.models.Image;

/**
 * Helper class for dealing with Spotify image objects.
 */
public class SpotifyImageHelper {
    public static final int MINIMUM_PREFERRED_IMAGE_WIDTH = 200;
    public static String LOG_TAG = SpotifyImageHelper.class.getSimpleName();

    /**
     * Given a list of images, return the most appropriate one for rendering on the app.
     */
    public static Image getPreferredImage(List<Image> images) {
        boolean imagesAvailable = (images != null) && (images.size() > 0);
        if (!imagesAvailable) {
            return null;
        }

        Image image = null;
        for (Image currentImage : images) {
            if (currentImage.width >= MINIMUM_PREFERRED_IMAGE_WIDTH) {
                image = currentImage;
            }
        }

        if (image == null) {
            image = images.get(0);
        }

        return image;
    }

    /**
     * Given a list of images, return the URL of the most appropriate one for rendering on the app.
     */
    public static String getPreferredImageUrl(List<Image> images) {
        Image image = getPreferredImage(images);
        if (image == null) {
            return null;
        } else {
            return image.url;
        }
    }
}