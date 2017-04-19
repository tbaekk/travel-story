/* Copyright 2017 Tim Baek
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tbaek.travelstory;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;
import com.tbaek.travelstory.model.Image;

import java.util.ArrayList;
import java.util.List;

public class ImageRenderer extends DefaultClusterRenderer<Image> implements
        ClusterManager.OnClusterClickListener<Image>,
        ClusterManager.OnClusterItemClickListener<Image>{
    private final IconGenerator mIconGenerator;
    private final IconGenerator mClusterIconGenerator;
    private final ImageView mImageView;
    private final ImageView mClusterImageView;
    private final int mDimension;
    private final Resources mResources;
    private final GoogleMap mMap;

    public ImageRenderer(Context appContext, Resources resources,
                         GoogleMap map, ClusterManager<Image> clusterManager) {
        super(appContext, map, clusterManager);

        // Initialize application level variables
        mMap = map;
        mResources = resources;
        mIconGenerator = new IconGenerator(appContext);
        mClusterIconGenerator = new IconGenerator(appContext);
        LayoutInflater inflater = LayoutInflater.from(appContext);

        // Set ClusterManager methods and listeners
        clusterManager.setOnClusterClickListener(this);
        clusterManager.setOnClusterItemClickListener(this);

        View multiImages = inflater.inflate(R.layout.multi_image, null);
        mClusterIconGenerator.setContentView(multiImages);
        mClusterImageView = (ImageView) multiImages.findViewById(R.id.image);

        // Format the image frame
        mDimension = (int) resources.getDimension(R.dimen.custom_profile_image);
        mImageView = new ImageView(appContext);
        mImageView.setLayoutParams(new ViewGroup.LayoutParams(mDimension, mDimension));

        int padding = (int) resources.getDimension(R.dimen.custom_profile_padding);
        mImageView.setPadding(padding, padding, padding, padding);
        mIconGenerator.setContentView(mImageView);
    }

    @Override
    public void onBeforeClusterItemRendered(Image image, MarkerOptions markerOptions) {
        // Draw a single image.
        // Set the info window to show their name.
        mImageView.setImageBitmap(image.mImage);
        Bitmap icon = mIconGenerator.makeIcon();
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon)).title(image.name);
    }

    @Override
    public void onBeforeClusterRendered(Cluster<Image> cluster, MarkerOptions markerOptions) {
        // Draw multiple images
        // Note: this method runs on the UI thread. Don't spend too much time in here (like in this example).
        List<Drawable> yourImages = new ArrayList<Drawable>(Math.min(4, cluster.getSize()));
        int width = mDimension;
        int height = mDimension;

        for (Image p : cluster.getItems()) {
            // Draw 4 at most.
            if (yourImages.size() == 4) break;
            Drawable drawable = new BitmapDrawable(mResources,p.mImage);
            drawable.setBounds(0, 0, width, height);
            yourImages.add(drawable);
        }
        MultiDrawable multiDrawable = new MultiDrawable(yourImages);
        multiDrawable.setBounds(0, 0, width, height);

        mClusterImageView.setImageDrawable(multiDrawable);
        Bitmap icon = mClusterIconGenerator.makeIcon(String.valueOf(cluster.getSize()));
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon));
    }

    @Override
    protected boolean shouldRenderAsCluster(Cluster cluster) {
        // Always render clusters.
        return cluster.getSize() > 1;
    }

    @Override
    public boolean onClusterClick(Cluster<Image> cluster) {
        // Zoom in the cluster. Need to create LatLngBounds and including all the cluster items
        // inside of bounds, then animate to center of the bounds.

        // Create the builder to collect all essential cluster items for the bounds.
        LatLngBounds.Builder builder = LatLngBounds.builder();
        for (ClusterItem item : cluster.getItems()) {
            builder.include(item.getPosition());
        }
        // Get the LatLngBounds
        final LatLngBounds bounds = builder.build();

        // Animate camera to the bounds
        try {
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public boolean onClusterItemClick(Image image) {
        Log.i("hit", "item clicked!");
        return false;
    }
}
