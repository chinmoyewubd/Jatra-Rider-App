package com.example.capstonebus;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RouteMapActivityPrivate extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Marker busMarker;
    private LatLng lastLocation;

    private DatabaseReference locationRef;

    private ImageButton backBtn, refreshBtn, navBtn;
    private LinearLayout loadingOverlay;
    private TextView routeTitle, speedText, lastUpdateText, activityStatusText;

    private View statusDot;

    private String busNumber;
    private String busId;
    private String status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.route_map_private);

        // Get data from intent
        busNumber = getIntent().getStringExtra("busNumber");
        busId = getIntent().getStringExtra("busId");
        status = getIntent().getStringExtra("status");

        // Default values if not provided
        if (busNumber == null) busNumber = "Unknown";
        if (busId == null) busId = "N/A";
        if (status == null) status = "inactive";

        // UI elements
        backBtn = findViewById(R.id.backButton);
        refreshBtn = findViewById(R.id.refreshButton);
        navBtn = findViewById(R.id.navigationButton);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        routeTitle = findViewById(R.id.routeTitle);
        speedText = findViewById(R.id.speedText);
        lastUpdateText = findViewById(R.id.lastUpdateText);
        activityStatusText = findViewById(R.id.activityStatusText);
        statusDot = findViewById(R.id.statusDot);

        // Set title with bus number
        routeTitle.setText(busNumber + " • Live Tracking");

        // Back button
        backBtn.setOnClickListener(v -> onBackPressed());

        // Refresh camera
        refreshBtn.setOnClickListener(v -> {
            if (lastLocation != null)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(lastLocation, 16f));
        });

        // Navigation button
        navBtn.setOnClickListener(v -> {
            if (lastLocation != null) {
                Uri gmmIntentUri = Uri.parse("google.navigation:q=" +
                        lastLocation.latitude + "," + lastLocation.longitude);
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                startActivity(mapIntent);
            }
        });

        // Load Map
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapPrivate);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        locationRef = FirebaseDatabase.getInstance()
                .getReference("driver_location/private_bus/" + busNumber + "/locations");

        startRealtimeUpdates();
    }

    private void startRealtimeUpdates() {
        locationRef.limitToLast(1).addValueEventListener(
                new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        if (!snapshot.hasChildren()) return;

                        for (DataSnapshot snap : snapshot.getChildren()) {
                            // Use Businfo model to get all data
                            Businfo businfo = snap.getValue(Businfo.class);

                            if (businfo == null) continue;

                            Double lat = businfo.getLat();
                            Double lon = businfo.getLon();
                            float speed = businfo.getSpeed();
                            long timestamp = businfo.timestamp;

                            if (lat == null || lon == null) continue;

                            LatLng newPos = new LatLng(lat, lon);
                            updateBusMarker(newPos);

                            // Calculate time difference
                            long currentTime = System.currentTimeMillis();
                            long diffMillis = currentTime - timestamp;
                            long diffMinutes = diffMillis / 60000;

                            // Update speed and activity status based on time difference
                            updateSpeedAndStatus(speed, diffMinutes);

                            // Update last update time
                            updateLastUpdateTime(timestamp);

                            loadingOverlay.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                }
        );
    }

    private void updateSpeedAndStatus(float speed, long minutesAgo) {
        // Convert m/s to km/h
        int speedKmh = Math.round(speed * 3.6f);

        // Determine activity status based on time and speed
        if (minutesAgo > 10) {
            // No update for more than 10 minutes - Bus is inactive
            speedText.setText("-- km/h");
            activityStatusText.setText("Inactive");
            activityStatusText.setTextColor(0xFFF44336); // Red
            statusDot.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFFF44336)
            );
        } else if (speedKmh < 1) {
            // Speed is very low - Bus is stopped/parked
            speedText.setText(speedKmh + " km/h");
            activityStatusText.setText("Stopped");
            activityStatusText.setTextColor(0xFFFF9800); // Orange
            statusDot.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFFFF9800)
            );
        } else {
            // Bus is moving
            speedText.setText(speedKmh + " km/h");
            activityStatusText.setText("Moving");
            activityStatusText.setTextColor(0xFF4CAF50); // Green
            statusDot.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFF4CAF50)
            );
        }
    }

    private void updateLastUpdateTime(long timestamp) {
        long currentTime = System.currentTimeMillis();
        long diffMillis = currentTime - timestamp;

        String timeAgo;

        if (diffMillis < 1000) {
            timeAgo = "Just now";
        } else if (diffMillis < 60000) {
            // Less than 1 minute
            long seconds = diffMillis / 1000;
            timeAgo = seconds + "s ago";
        } else if (diffMillis < 3600000) {
            // Less than 1 hour
            long minutes = diffMillis / 60000;
            timeAgo = minutes + "m ago";
        } else if (diffMillis < 86400000) {
            // Less than 24 hours
            long hours = diffMillis / 3600000;
            timeAgo = hours + "h ago";
        } else {
            // Show actual time
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());
            timeAgo = sdf.format(new Date(timestamp));
        }

        lastUpdateText.setText("Updated: " + timeAgo);
    }

    private void updateBusMarker(LatLng newPos) {

        if (busMarker == null) {
            // Initial marker
            busMarker = mMap.addMarker(new MarkerOptions()
                    .position(newPos)
                    .title("Bus " + busNumber)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            );

            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(newPos)
                    .zoom(16f)
                    .build();

            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        } else {
            // Smooth move
            animateMarker(busMarker, newPos);
            mMap.animateCamera(CameraUpdateFactory.newLatLng(newPos));
        }

        lastLocation = newPos;
    }

    private void animateMarker(Marker marker, LatLng newPos) {

        LatLng startPos = marker.getPosition();
        long duration = 600;
        long startTime = System.currentTimeMillis();

        android.os.Handler handler = new android.os.Handler();

        handler.post(new Runnable() {
            @Override
            public void run() {

                float elapsed = (float) (System.currentTimeMillis() - startTime) / duration;
                if (elapsed > 1) elapsed = 1;

                double lat = startPos.latitude + ((newPos.latitude - startPos.latitude) * elapsed);
                double lon = startPos.longitude + ((newPos.longitude - startPos.longitude) * elapsed);

                marker.setPosition(new LatLng(lat, lon));

                if (elapsed < 1) {
                    handler.postDelayed(this, 16);
                }
            }
        });
    }
}