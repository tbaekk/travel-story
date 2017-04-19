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
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterManager;
import com.tbaek.travelstory.database.DatabaseHelper;
import com.tbaek.travelstory.database.DatabaseUtil;
import com.tbaek.travelstory.model.Image;


public class MainActivity extends FragmentActivity implements OnMapReadyCallback, PlaceSelectionListener {

    private DatabaseHelper db = new DatabaseHelper(this);
    private ClusterManager<Image> mClusterManager;
    private GoogleMap mMap;
    private Place mSelectedPlace;
    private Context mContext;
    private Resources mResources;

    private static final int REQUEST_CODE_IMAGE_REQUEST = 1;
    private static final float zoomInLevel = 9.5f;
    private static final String ERROR_TAG = "error";
    private static final String CANCELED_TAG = "canceled";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        // Initialize Application level variables
        mContext = getApplicationContext();
        mResources = getResources();

        // Setup App variables (Google map & ClusterManager)
        setUpMap();

        // Retrieve the PlaceAutocompleteFragment.
        PlaceAutocompleteFragment searchFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.location_search);

        // Register a listener to receive callbacks when a place has been selected or an error has
        // occurred.
        searchFragment.setOnPlaceSelectedListener(this);
    }

    @Override
    public void onError(Status status) {
        Log.i(ERROR_TAG, status.getStatusMessage());
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
        mClusterManager.setRenderer(new ImageRenderer(mContext, mResources, mMap, mClusterManager));

        mMap.setOnCameraIdleListener(mClusterManager);
        mMap.setOnMarkerClickListener(mClusterManager);
        mMap.setOnInfoWindowClickListener(mClusterManager);
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
