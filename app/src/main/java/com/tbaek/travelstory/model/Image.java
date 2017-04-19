package com.tbaek.travelstory.model;

import android.graphics.Bitmap;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;


public class Image implements ClusterItem {
    public final String place;
    public final Bitmap mImage;
    private final LatLng mPosition;

    public Image(String place, LatLng position, Bitmap pictureResource) {
        this.place = place;
        mPosition = position;
        mImage = pictureResource;
    }

    @Override
    public LatLng getPosition() {
        return mPosition;
    }

    @Override
    public String getTitle() {
        return null;
    }

    @Override
    public String getSnippet() {
        return null;
    }
}
