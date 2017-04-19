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

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;
import com.tbaek.travelstory.database.DatabaseHelper;
import com.tbaek.travelstory.database.DatabaseUtil;
import com.tbaek.travelstory.model.Image;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends FragmentActivity implements OnMapReadyCallback, PlaceSelectionListener {

    private DatabaseHelper db = new DatabaseHelper(this);
    private ClusterManager<Image> mClusterManager;
    private GoogleMap mMap;
    private FloatingActionButton mFab;
    private Place mSelectedPlace;
    private Image mSelectedImage;

    private static final int REQUEST_CODE_IMAGE_REQUEST = 1;
    private static final float zoomInLevel = 9.5f;
    private static final String ERROR_TAG = "error";
    private static final String CANCELED_TAG = "canceled";

    public class ImageRenderer extends DefaultClusterRenderer<Image> implements
            ClusterManager.OnClusterClickListener<Image>,
            ClusterManager.OnClusterItemClickListener<Image>{
        private final IconGenerator mIconGenerator = new IconGenerator(getApplicationContext());
        private final IconGenerator mClusterIconGenerator = new IconGenerator(getApplicationContext());
        private final ImageView mImageView;
        private final ImageView mClusterImageView;
        private final int mDimension;

        public ImageRenderer() {
            super(getApplicationContext(), mMap, mClusterManager);

            // Set ClusterManager methods and listeners
            mClusterManager.setOnClusterClickListener(this);
            mClusterManager.setOnClusterItemClickListener(this);

            View multiImages = getLayoutInflater().inflate(R.layout.multi_image, null);
            mClusterIconGenerator.setContentView(multiImages);
            mClusterImageView = (ImageView) multiImages.findViewById(R.id.image);

            // Format the image frame
            mDimension = (int) getResources().getDimension(R.dimen.custom_profile_image);
            mImageView = new ImageView(getApplicationContext());
            mImageView.setLayoutParams(new ViewGroup.LayoutParams(mDimension, mDimension));

            int padding = (int) getResources().getDimension(R.dimen.custom_profile_padding);
            mImageView.setPadding(padding, padding, padding, padding);
            mIconGenerator.setContentView(mImageView);
        }

        @Override
        public void onBeforeClusterItemRendered(Image image, MarkerOptions markerOptions) {
            // Draw a single image. Set the info window to show their name.
            mImageView.setImageBitmap(image.mImage);
            Bitmap icon = mIconGenerator.makeIcon();
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon)).title(image.place);
        }

        @Override
        public void onBeforeClusterRendered(Cluster<Image> cluster, MarkerOptions markerOptions) {
            // Draw multiple images
            List<Drawable> yourImages = new ArrayList<Drawable>(Math.min(4, cluster.getSize()));
            int width = mDimension;
            int height = mDimension;

            for (Image p : cluster.getItems()) {
                // Draw 4 at most.
                if (yourImages.size() == 4) break;
                Drawable drawable = new BitmapDrawable(getResources(),p.mImage);
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
            // TODO: If Images are all in same location, open new activity to display ImageView.


            // Otherwise, zoom in the cluster. Need to create LatLngBounds and including all
            // the cluster items inside of bounds, then animate to center of the bounds.

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
            // Log.i("hit", "item clicked!");
            // TODO: Change to opening a display ImageView Activity
            mFab.setVisibility(View.VISIBLE);
            mSelectedImage = image;
            return false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        // Setup App variables (Google map & ClusterManager)
        setUpMap();
        // Register FloatingActionButton
        mFab = (FloatingActionButton) findViewById(R.id.floatingActionButton);
        mFab.setVisibility(View.GONE);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSelectedImage != null) {

                }
            }
        });
        // Retrieve the PlaceAutocompleteFragment.
        PlaceAutocompleteFragment searchFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.location_search);
        // Register a listener to receive callbacks when a place has been selected or an error has
        // occurred.
        searchFragment.setOnPlaceSelectedListener(this);
    }

    @Override
    public void onError(Status status) {
        Log.e(ERROR_TAG, status.getStatusMessage());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        db.close();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setMapToolbarEnabled(false);
        // Setup Cluster Manager
        setUpClusterManager();
        // Check if any data exists in the storage
        loadImageFromDatabase();
    }

    @Override
    public void onPlaceSelected(Place place) {
        mSelectedPlace = place;

        Intent intent = new Intent();
        // Show only images, no videos or anything else
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        // Always show the chooser (if there are multiple options available)
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_CODE_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_IMAGE_REQUEST) {
            if (resultCode == RESULT_OK) {
                LatLng position = mSelectedPlace.getLatLng();
                CharSequence place = mSelectedPlace.getName();

                try {
                    Uri selectedImageUri = data.getData();
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                    // Save Bitmap and LatLng
                    saveImageToDatabase(position, bitmap);
                    // Begin new cluster
                    addItems(position, bitmap);
                    mClusterManager.cluster();
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, zoomInLevel));
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(ERROR_TAG, e.getMessage());
                }
                startCluster();
            }
            else if (resultCode == RESULT_CANCELED) {
                Log.i(CANCELED_TAG, "Activity canceled, returned to previous activity");
            }
        }
    }

    private void setUpMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void setUpClusterManager() {
        mClusterManager = new ClusterManager<Image>(this, mMap);
        mClusterManager.setRenderer(new ImageRenderer());

        mMap.setOnCameraIdleListener(mClusterManager);
        mMap.setOnMarkerClickListener(mClusterManager);
        mMap.setOnInfoWindowClickListener(mClusterManager);
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if(mFab.isShown()) {
                    mFab.setVisibility(View.GONE);
                }
            }
        });
    }

    private void saveImageToDatabase(LatLng position, Bitmap selectedImage) {
        Double lat = position.latitude;
        Double lng = position.longitude;
        byte[] image = DatabaseUtil.getBytes(selectedImage);
        db.addEntry(lat, lng, image);
    }

    private void loadImageFromDatabase() {
        Cursor cursor = db.getAllImages();
        if (cursor == null) {
            return;
        }

        if (cursor.moveToFirst()) {
            do {
                LatLng position = new LatLng(cursor.getDouble(0), cursor.getDouble(1));
                Bitmap image = DatabaseUtil.getImage(cursor.getBlob(2));
                addItems(position, image);
            }while(cursor.moveToNext());
        }
        startCluster();
    }

    private void startCluster() {
        mClusterManager.cluster();
    }

    private void addItems(LatLng position, Bitmap image) {
        try {
            mClusterManager.addItem(new Image("Walter", position, image));
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(ERROR_TAG, e.getMessage());
        }
    }

}
