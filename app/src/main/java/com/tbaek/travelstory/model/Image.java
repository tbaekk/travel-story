package com.tbaek.travelstory.model;

import android.graphics.Bitmap;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;


public class Image implements ClusterItem {
    public final String place;
    public final Bitmap mImage;
    private final LatLng mPosition;
    private final String mImageId;

    public Image(String id, String place, LatLng position, Bitmap pictureResource) {
        this.place = place;
        mPosition = position;
        mImage = pictureResource;
        mImageId = id;
    }

    @Override
    public String getTitle() {
        return null;
    }

    @Override
    public String getSnippet() {
        return null;
    }

    @Override
    public LatLng getPosition() {
        return mPosition;
    }

    public String getId() { return mImageId; }
}
