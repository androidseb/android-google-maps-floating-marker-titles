package com.exlyo.gmfmtexample;

import android.graphics.Color;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;

import com.exlyo.gmfmt.FloatingMarkerTitlesOverlay;
import com.exlyo.gmfmt.MarkerInfo;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        final FloatingMarkerTitlesOverlay floatingMarkersOverlay = findViewById(R.id.map_floating_markers_overlay);
        floatingMarkersOverlay.setSource(googleMap);

        final List<MarkerInfo> markerInfoList = SampleMarkerData.getSampleMarkersInfo();
        for (int i = 0; i < markerInfoList.size(); i++) {
            final MarkerInfo mi = markerInfoList.get(i);
            mMap.addMarker(new MarkerOptions().position(mi.getCoordinates()));
            floatingMarkersOverlay.addMarker(i, mi);
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLng(markerInfoList.get(0).getCoordinates()));
    }
}
