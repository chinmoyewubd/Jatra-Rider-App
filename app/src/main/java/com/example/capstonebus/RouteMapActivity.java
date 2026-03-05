package com.example.capstonebus;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.res.ColorStateList;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RouteMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private TextView routeTitle, distanceText, etaText;

    // ✅ NEW (added only)
    private TextView speedText, lastUpdateText, activityStatusText;
    private View statusDot;

    private String routeName, busNumber;
    private LatLng userStartPoint;

    private DatabaseReference driverLocationRef;
    private DatabaseReference routesRef;
    private ValueEventListener busLocationListener;
    private Marker busMarker;
    private Marker passengerMarker;
    private Polyline busPathPolyline;
    private Polyline connectionPolyline;

    private List<LatLng> busRoutePoints = new ArrayList<>();
    private Handler updateHandler = new Handler();
    private boolean isApproaching = false;
    private LinearLayout loadingOverlay;

    private static final List<PatternItem> PATTERN_DASHED = Arrays.asList(
            new Dash(20), new Gap(10)
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_map);

        // Initialize UI components (same as before)
        routeTitle = findViewById(R.id.routeTitle);

        loadingOverlay = findViewById(R.id.loadingOverlay);

        // ✅ NEW (added only)
        speedText = findViewById(R.id.speedText);
        lastUpdateText = findViewById(R.id.lastUpdateText);
        activityStatusText = findViewById(R.id.activityStatusText);
        statusDot = findViewById(R.id.statusDot);

        // Get data from intent (same as before)
        routeName = getIntent().getStringExtra("routeName"); // means car to
        userStartPoint = getIntent().getParcelableExtra("userStartPoint");
        busNumber = getIntent().getStringExtra("busNumber");

        if (routeName != null) {
            routeTitle.setText("Bus " + routeName + " • Live Tracking");
        }

        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String trackingType = sharedPreferences.getString("tracking", "");

        if (trackingType.equals("publicbus")) {
            driverLocationRef = FirebaseDatabase.getInstance().getReference("driver_location/public_bus");
        } else {
            driverLocationRef = FirebaseDatabase.getInstance().getReference("driver_location/private_bus");
        }

        // Initialize Firebase (same as before)
        routesRef = FirebaseDatabase.getInstance().getReference("Routes");

        // Setup button listeners (same as before)
        setupButtonListeners();

        // Initialize map (same as before)
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void setupButtonListeners() {
        ImageButton backButton = findViewById(R.id.backButton);
        ImageButton refreshButton = findViewById(R.id.refreshButton);
        ImageButton navigationButton = findViewById(R.id.navigationButton);

        backButton.setOnClickListener(v -> finish());

        refreshButton.setOnClickListener(v -> refreshBusLocation());

        navigationButton.setOnClickListener(v -> {
            if (userStartPoint != null && mMap != null) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userStartPoint, 15f));
                Toast.makeText(this, "Centered on your location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void refreshBusLocation() {
        if (busLocationListener != null) {
            driverLocationRef.removeEventListener(busLocationListener);
        }

        if (busMarker != null) {
            busMarker.remove();
            busMarker = null;
        }
        if (busPathPolyline != null) {
            busPathPolyline.remove();
            busPathPolyline = null;
        }
        if (connectionPolyline != null) {
            connectionPolyline.remove();
            connectionPolyline = null;
        }

        startBusLocationTracking();
        Toast.makeText(this, "Location refreshed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        setupGoogleMapStyle();

        // SAME as before
        loadingOverlay.setVisibility(View.GONE);

        fetchBusRoutePoints();
    }

    private void setupGoogleMapStyle() {
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        mMap.setPadding(0, 200, 0, 400);
    }



    private void fetchBusRoutePoints() {
        if (routeName == null) return;

        routesRef.child(routeName).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                busRoutePoints.clear();
                for (DataSnapshot pointSnapshot : snapshot.getChildren()) {
                    BusRoute busRoute = pointSnapshot.getValue(BusRoute.class);
                    if (busRoute != null) {
                        try {
                            double lat = Double.parseDouble(busRoute.getLatitude());
                            double lng = Double.parseDouble(busRoute.getLongitude());
                            busRoutePoints.add(new LatLng(lat, lng));
                        } catch (Exception ignored) {}
                    }
                }

                if (!busRoutePoints.isEmpty()) {
                    addPassengerMarker();
                    startBusLocationTracking();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void addPassengerMarker() {
        if (userStartPoint != null) {
            passengerMarker = mMap.addMarker(new MarkerOptions()
                    .position(userStartPoint)
                    .title("Your Location")
                    .snippet("Boarding point")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                    .alpha(0.9f));

            if (passengerMarker != null) passengerMarker.showInfoWindow();
        }
    }

    private void startBusLocationTracking() {
        if (busNumber == null) return;

        Query busLocationQuery = driverLocationRef.child(busNumber).child("locations")
                .limitToLast(1);

        busLocationListener = busLocationQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot locationSnapshot : snapshot.getChildren()) {
                    Businfo busInfo = locationSnapshot.getValue(Businfo.class);
                    if (busInfo != null) {
                        LatLng busLocation = new LatLng(busInfo.getLat(), busInfo.getLon());

                        // ✅ NEW: update status/speed/lastUpdate (ONLY added)
                        long ts = busInfo.timestamp;
                        long now = System.currentTimeMillis();
                        long diffMinutes = (ts > 0) ? ((now - ts) / 60000L) : 9999L;

                        updateSpeedAndStatus(busInfo.getSpeed(), diffMinutes);
                        updateLastUpdateTime(ts);

                        // SAME as before
                        updateBusUI(busLocation);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void updateBusUI(LatLng busLocation) {
        updateBusMarker(busLocation);
        drawBusPathToPassenger(busLocation);
        checkIfBusIsApproaching(busLocation);
        zoomToOptimalView(busLocation);

        if (userStartPoint != null) {
            double distance = calculateRouteDistance(busLocation, userStartPoint);
            int etaMinutes = calculateETA(distance); // SAME as before (fixed speed)

        }
    }

    private void updateBusMarker(LatLng busLocation) {
        if (busMarker == null) {
            busMarker = mMap.addMarker(new MarkerOptions()
                    .position(busLocation)
                    .title("Car: " + busNumber)
                    .snippet("Live tracking")
                    .icon(getBitmapDescriptorFromVector(
                            isApproaching ? R.drawable.bus_marker_approaching : R.drawable.bus_marker
                    ))
                    .anchor(0.5f, 0.5f));
        } else {
            busMarker.setPosition(busLocation);
        }

        if (userStartPoint != null && busMarker != null) {
            double distance = calculateRouteDistance(busLocation, userStartPoint);
            String distanceText = formatDistance(distance);
            busMarker.setSnippet(distanceText + " from you");
        }

        if (busMarker != null) busMarker.showInfoWindow();
    }

    private String formatDistance(double distance) {
        if (distance < 1000) {
            return String.format("%d m", (int) distance);
        } else {
            return String.format("%.1f km", distance / 1000);
        }
    }

    private void drawBusPathToPassenger(LatLng busLocation) {
        if (busRoutePoints.isEmpty() || userStartPoint == null) return;

        if (busPathPolyline != null) busPathPolyline.remove();
        if (connectionPolyline != null) connectionPolyline.remove();

        LatLng closestRoutePointToBus = findClosestPointOnRoute(busLocation);
        if (closestRoutePointToBus == null) return;

        int startPointIndex = findPointIndexInRoute(userStartPoint);
        int busClosestIndex = findPointIndexInRoute(closestRoutePointToBus);
        if (startPointIndex == -1 || busClosestIndex == -1) return;

        connectionPolyline = mMap.addPolyline(new PolylineOptions()
                .add(busLocation, closestRoutePointToBus)
                .width(6f)
                .color(Color.parseColor("#9E9E9E"))
                .pattern(PATTERN_DASHED)
                .geodesic(true));

        List<LatLng> pathPoints = new ArrayList<>();

        if (busClosestIndex <= startPointIndex) {
            for (int i = busClosestIndex; i <= startPointIndex; i++) {
                pathPoints.add(busRoutePoints.get(i));
            }
        } else {
            for (int i = busClosestIndex; i >= startPointIndex; i--) {
                pathPoints.add(busRoutePoints.get(i));
            }
        }

        busPathPolyline = mMap.addPolyline(new PolylineOptions()
                .addAll(pathPoints)
                .width(12f)
                .color(Color.parseColor("#4285F4"))
                .startCap(new RoundCap())
                .endCap(new RoundCap())
                .geodesic(true));
    }

    private void checkIfBusIsApproaching(LatLng busLocation) {
        if (userStartPoint == null) return;

        Query prevLocationQuery = driverLocationRef.child(busNumber).child("locations")
                .limitToLast(2);

        prevLocationQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<LatLng> recentLocations = new ArrayList<>();
                for (DataSnapshot locSnapshot : snapshot.getChildren()) {
                    Businfo loc = locSnapshot.getValue(Businfo.class);
                    if (loc != null) {
                        recentLocations.add(new LatLng(loc.getLat(), loc.getLon()));
                    }
                }

                if (recentLocations.size() >= 2) {
                    boolean newApproachingStatus = isApproaching(recentLocations, userStartPoint);

                    if (newApproachingStatus != isApproaching) {
                        isApproaching = newApproachingStatus;

                        if (busMarker != null) {
                            busMarker.setIcon(getBitmapDescriptorFromVector(
                                    isApproaching ? R.drawable.bus_marker_approaching : R.drawable.bus_marker_away
                            ));
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private int calculateETA(double distance) {
        double speedKmh = 15;
        double timeHours = (distance / 1000) / speedKmh;
        return Math.max(1, (int) (timeHours * 60));
    }

    private boolean isApproaching(List<LatLng> locations, LatLng target) {
        if (locations.size() < 2) return false;
        double oldDistance = calculateRouteDistance(locations.get(0), target);
        double newDistance = calculateRouteDistance(locations.get(1), target);
        return newDistance < oldDistance;
    }

    private double calculateDistance(LatLng point1, LatLng point2) {
        float[] results = new float[1];
        android.location.Location.distanceBetween(
                point1.latitude, point1.longitude,
                point2.latitude, point2.longitude,
                results
        );
        return results[0];
    }

    private double calculateRouteDistance(LatLng busLocation, LatLng passengerLocation) {
        if (busRoutePoints.isEmpty()) return 0;

        LatLng closestBusPoint = findClosestPointOnRoute(busLocation);
        LatLng closestPassengerPoint = findClosestPointOnRoute(passengerLocation);

        if (closestBusPoint == null || closestPassengerPoint == null) {
            return calculateDistance(busLocation, passengerLocation);
        }

        int busIndex = findPointIndexInRoute(closestBusPoint);
        int passengerIndex = findPointIndexInRoute(closestPassengerPoint);

        if (busIndex == -1 || passengerIndex == -1) {
            return calculateDistance(busLocation, passengerLocation);
        }

        double busToRouteDistance = calculateDistance(busLocation, closestBusPoint);
        double passengerToRouteDistance = calculateDistance(passengerLocation, closestPassengerPoint);

        double routeDistance = 0;

        if (busIndex < passengerIndex) {
            for (int i = busIndex; i < passengerIndex; i++) {
                routeDistance += calculateDistance(busRoutePoints.get(i), busRoutePoints.get(i + 1));
            }
        } else {
            for (int i = busIndex; i > passengerIndex; i--) {
                routeDistance += calculateDistance(busRoutePoints.get(i), busRoutePoints.get(i - 1));
            }
        }

        return busToRouteDistance + routeDistance + passengerToRouteDistance;
    }

    private void zoomToOptimalView(LatLng busLocation) {
        if (userStartPoint == null) return;

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(busLocation);
        builder.include(userStartPoint);

        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 150));
    }

    private BitmapDescriptor getBitmapDescriptorFromVector(int vectorResId) {
        try {
            Drawable vectorDrawable = ContextCompat.getDrawable(this, vectorResId);
            if (vectorDrawable == null) return BitmapDescriptorFactory.defaultMarker();

            Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            vectorDrawable.setBounds(0, 0, 100, 100);
            vectorDrawable.draw(canvas);

            return BitmapDescriptorFactory.fromBitmap(bitmap);
        } catch (Exception e) {
            return BitmapDescriptorFactory.defaultMarker();
        }
    }

    private LatLng findClosestPointOnRoute(LatLng targetPoint) {
        if (busRoutePoints.isEmpty()) return null;
        LatLng closestPoint = busRoutePoints.get(0);
        double shortestDistance = calculateDistance(closestPoint, targetPoint);

        for (LatLng routePoint : busRoutePoints) {
            double distance = calculateDistance(routePoint, targetPoint);
            if (distance < shortestDistance) {
                shortestDistance = distance;
                closestPoint = routePoint;
            }
        }
        return closestPoint;
    }

    private int findPointIndexInRoute(LatLng point) {
        for (int i = 0; i < busRoutePoints.size(); i++) {
            if (calculateDistance(busRoutePoints.get(i), point) < 20.0) {
                return i;
            }
        }
        return -1;
    }

    // ✅ NEW helper (copied from Private logic)
    private void updateSpeedAndStatus(float speedMps, long minutesAgo) {
        int speedKmh = Math.round(speedMps * 3.6f);

        if (minutesAgo > 10) {
            speedText.setText("-- km/h");
            activityStatusText.setText("Inactive");
            activityStatusText.setTextColor(0xFFF44336);
            statusDot.setBackgroundTintList(ColorStateList.valueOf(0xFFF44336));
        } else if (speedKmh < 1) {
            speedText.setText(speedKmh + " km/h");
            activityStatusText.setText("Stopped");
            activityStatusText.setTextColor(0xFFFF9800);
            statusDot.setBackgroundTintList(ColorStateList.valueOf(0xFFFF9800));
        } else {
            speedText.setText(speedKmh + " km/h");
            activityStatusText.setText("Moving");
            activityStatusText.setTextColor(0xFF4CAF50);
            statusDot.setBackgroundTintList(ColorStateList.valueOf(0xFF4CAF50));
        }
    }

    // ✅ NEW helper (copied from Private logic)
    private void updateLastUpdateTime(long timestamp) {
        long currentTime = System.currentTimeMillis();
        long diffMillis = currentTime - timestamp;

        String timeAgo;

        if (timestamp <= 0) {
            timeAgo = "--";
        } else if (diffMillis < 1000) {
            timeAgo = "Just now";
        } else if (diffMillis < 60000) {
            long seconds = diffMillis / 1000;
            timeAgo = seconds + "s ago";
        } else if (diffMillis < 3600000) {
            long minutes = diffMillis / 60000;
            timeAgo = minutes + "m ago";
        } else if (diffMillis < 86400000) {
            long hours = diffMillis / 3600000;
            timeAgo = hours + "h ago";
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());
            timeAgo = sdf.format(new Date(timestamp));
        }

        lastUpdateText.setText("Updated: " + timeAgo);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (busLocationListener != null) {
            driverLocationRef.removeEventListener(busLocationListener);
        }
        updateHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (busLocationListener != null) {
            driverLocationRef.removeEventListener(busLocationListener);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMap != null && busNumber != null) {
            startBusLocationTracking();
        }
    }
}
