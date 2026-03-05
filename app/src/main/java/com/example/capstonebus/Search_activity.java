package com.example.capstonebus;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Search_activity extends AppCompatActivity {

    private Map<String, List<BusRoute>> map2 = new HashMap<>();
    private Button btnSearch;
    private TextView etFrom, etTo;

    private RelativeLayout loadingLayout;
    private ProgressBar progressBar;

    private LatLng nearestRoadPoint;
    private String destination;
    private String nearestRoadPointName;
    private LatLng currentlocation;

    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // Bind views
        etFrom = findViewById(R.id.etFrom);
        etTo = findViewById(R.id.etTo);
        btnSearch = findViewById(R.id.btnSearch);
        loadingLayout = findViewById(R.id.loadingLayout);
        progressBar = findViewById(R.id.progressBar);

        // Hide loading initially
        loadingLayout.setVisibility(View.GONE);

        // Firebase reference
        databaseReference = FirebaseDatabase.getInstance().getReference("Routes");

        // Get current location from intent
        Intent intent = getIntent();
        currentlocation = intent.getParcelableExtra("currentLocation");

        // Fetch routes from Firebase
        fetchRoutesData();

        // Click "From" field
        etFrom.setOnClickListener(v -> {
            Intent i = new Intent(Search_activity.this, CurrentLocationActivity.class);
            i.putExtra("etTo", "etFrom");
            startActivityForResult(i, 1);
        });

        // Click "To" field
        etTo.setOnClickListener(v -> {
            Intent i = new Intent(Search_activity.this, Search_bar_activity.class);
            i.putExtra("etTo", "etTo");
            startActivityForResult(i, 1);
        });

        // Load initial values
        updateUIFromPreferences();

        // Search button click
        btnSearch.setOnClickListener(v -> {
            String destination = etTo.getText().toString().trim();
            if (destination.isEmpty() || destination.equals("Destination")) {
                Toast.makeText(Search_activity.this, "Please select a destination", Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String trackingType = sharedPreferences.getString("tracking", "");

            Intent i;
            if ("publicbus".equals(trackingType)) {
                i = new Intent(Search_activity.this, Buss_List_privatebus.class);
            } else {
                i = new Intent(Search_activity.this, Buss_List_privatebus.class);
            }

            if (nearestRoadPoint != null) {
                i.putExtra("nearestRoadPoint", nearestRoadPoint);
            }

            i.putExtra("nearestRoadPointName", nearestRoadPointName);
            i.putExtra("destination", destination);

            startActivity(i);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Update UI every time the activity resumes
        updateUIFromPreferences();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            // Handle "From" selection
            if (data.hasExtra("selected")) {
                String selectedFrom = data.getStringExtra("selected");
                etFrom.setText(selectedFrom);
                nearestRoadPointName = selectedFrom;
                editor.putString("nearestRoadPointName", selectedFrom);
            }

            // Handle "To" selection
            if (data.hasExtra("selected2")) {
                String selectedTo = data.getStringExtra("selected2");
                etTo.setText(selectedTo);
                destination = selectedTo;
                editor.putString("destination", selectedTo);
            }

            editor.apply();
        }
    }

    /**
     * Updates the UI (etFrom and etTo) from SharedPreferences
     */
    private void updateUIFromPreferences() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        destination = prefs.getString("destination", "");
        nearestRoadPointName = prefs.getString("nearestRoadPointName", "");

        String latStr = prefs.getString("nearestRoadPointLat", null);
        String lngStr = prefs.getString("nearestRoadPointLng", null);

        nearestRoadPoint = null;
        if (latStr != null && lngStr != null) {
            try {
                nearestRoadPoint = new LatLng(Double.parseDouble(latStr), Double.parseDouble(lngStr));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        // Update "From" field
        if (nearestRoadPointName != null && !nearestRoadPointName.trim().isEmpty()) {
            etFrom.setText(nearestRoadPointName);
        } else {
            etFrom.setText("Boarding Point");
        }

        // Update "To" field
        if (destination != null && !destination.trim().isEmpty()) {
            etTo.setText(destination);
        } else {
            etTo.setText("Destination");
        }
    }

    private void fetchRoutesData() {
        // Show loading
        runOnUiThread(() -> loadingLayout.setVisibility(View.VISIBLE));

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                map2.clear();

                for (DataSnapshot routeSnapshot : snapshot.getChildren()) {
                    String routeName = routeSnapshot.getKey();
                    List<BusRoute> routeInfo = new ArrayList<>();

                    for (DataSnapshot busRouteSnapshot : routeSnapshot.getChildren()) {
                        BusRoute busRoute = busRouteSnapshot.getValue(BusRoute.class);
                        if (busRoute != null) {
                            routeInfo.add(busRoute);
                        }
                    }

                    if (routeName != null) {
                        map2.put(routeName, routeInfo);
                    }
                }

                if (currentlocation != null) {
                    findClosestBusRoute(currentlocation);
                } else {
                    runOnUiThread(() -> loadingLayout.setVisibility(View.GONE));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                runOnUiThread(() -> {
                    loadingLayout.setVisibility(View.GONE);
                    Toast.makeText(Search_activity.this, "Failed to load data", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void findClosestBusRoute(LatLng currentLocation) {
        runOnUiThread(() -> loadingLayout.setVisibility(View.VISIBLE));

        double closestDistance = Double.MAX_VALUE;
        BusRoute closestRoute = null;

        for (Map.Entry<String, List<BusRoute>> entry : map2.entrySet()) {
            List<BusRoute> busRoutes = entry.getValue();

            for (BusRoute route : busRoutes) {
                final String routeLatStr = route.getLatitude();
                final String routeLngStr = route.getLongitude();

                try {
                    double routeLat = Double.parseDouble(routeLatStr);
                    double routeLng = Double.parseDouble(routeLngStr);
                    LatLng routeLatLng = new LatLng(routeLat, routeLng);
                    double distance = manhattanDistance(currentLocation, routeLatLng);

                    if (distance < closestDistance) {
                        closestDistance = distance;
                        closestRoute = route;
                    }
                } catch (Exception e) {
                    System.out.println("Error parsing lat/lng: " + e.getMessage());
                }
            }
        }

        final BusRoute finalClosestRoute = closestRoute;
        final double finalDistance = closestDistance;

        runOnUiThread(() -> {
            loadingLayout.setVisibility(View.GONE);

            if (finalClosestRoute != null) {
                SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();

                nearestRoadPointName = finalClosestRoute.getPlaceName();
                nearestRoadPoint = new LatLng(
                        Double.parseDouble(finalClosestRoute.getLatitude()),
                        Double.parseDouble(finalClosestRoute.getLongitude())
                );

                // Save to SharedPreferences
                editor.putString("nearestRoadPointName", nearestRoadPointName);
                editor.putString("nearestRoadPointLat", String.valueOf(nearestRoadPoint.latitude));
                editor.putString("nearestRoadPointLng", String.valueOf(nearestRoadPoint.longitude));
                editor.apply();

                // Update UI
                etFrom.setText(nearestRoadPointName);

                System.out.println("Nearest Route: " + finalClosestRoute.getPlaceName() +
                        " Dist: " + finalDistance);
            } else {
                Toast.makeText(Search_activity.this, "No nearest bus route found", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private double manhattanDistance(LatLng point1, LatLng point2) {
        return Math.abs(point1.latitude - point2.latitude) + Math.abs(point1.longitude - point2.longitude);
    }
}