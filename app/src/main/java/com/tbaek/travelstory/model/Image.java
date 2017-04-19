package com.tbaek.travelstory.model;

import android.graphics.Bitmap;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;


public class Image implements ClusterItem {
    public final String name;
    public final Bitmap mImage;
    private final LatLng mPosition;

    public Image(String name, LatLng position, Bitmap pictureResource) {
        this.name = name;
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
