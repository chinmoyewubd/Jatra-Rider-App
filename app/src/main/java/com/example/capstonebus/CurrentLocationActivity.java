package com.example.capstonebus;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.capstonebus.databinding.ActivityCurrentLocationBinding;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import android.Manifest;
import android.graphics.Color;
import android.view.ViewGroup;
import android.widget.ImageView;

public class CurrentLocationActivity extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener, AdapterView.OnItemClickListener {

    private GoogleMap mMap;
    private final int FINE_PERMISSION_CODE = 1;
    private Button passengerLocation;
    private Location currentLocation;
    private LatLng newLocation;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private ActivityCurrentLocationBinding binding;
    private DatabaseReference databaseReference;

    // Search functionality variables
    private SearchView searchView;
    private ListView searchListView;
    private ArrayAdapter<String> searchAdapter;
    private List<String> locationNames = new ArrayList<>();
    private Map<String, LatLng> locationMap = new HashMap<>(); // Map to store placeName -> LatLng
    private DatabaseReference routesDatabaseRef;

    // Handler for UI updates
    private Handler uiHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCurrentLocationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        passengerLocation = findViewById(R.id.passengerLocation);
        databaseReference = FirebaseDatabase.getInstance().getReference("papassengerLocation");
        routesDatabaseRef = FirebaseDatabase.getInstance().getReference("Routes");

        // Initialize search components
        searchView = findViewById(R.id.searchView);
        searchListView = findViewById(R.id.searchListView);

        // Setup search functionality
        setupSearchView();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        getLastLocation();
        passengerLocation.setOnClickListener(this);

        // Fetch locations for search in background
        fetchLocationsFromFirebase();
    }

    private void setupSearchView() {
        searchAdapter = new ArrayAdapter<String>(this, R.layout.custom_list_item, R.id.list_item_text, locationNames) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(R.id.list_item_text);
                textView.setTextColor(Color.BLACK);
                textView.setTextSize(16);
                return view;
            }
        };

        searchListView.setAdapter(searchAdapter);
        searchListView.setOnItemClickListener(this);
        searchListView.setBackgroundColor(Color.WHITE);

        searchView.setIconifiedByDefault(false);
        searchView.setBackgroundColor(Color.TRANSPARENT);

        int searchPlateId = searchView.getContext().getResources().getIdentifier("android:id/search_plate", null, null);
        View searchPlate = searchView.findViewById(searchPlateId);
        if (searchPlate != null) {
            searchPlate.setBackgroundColor(Color.TRANSPARENT);
        }

        searchView.setQueryHint("Type location name...");

        // Revert to the original more accurate search implementation
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Use the original filtering approach which was more accurate
                searchAdapter.getFilter().filter(newText);
                if (newText.isEmpty()) {
                    searchListView.setVisibility(View.GONE);
                } else {
                    searchListView.setVisibility(View.VISIBLE);
                }
                return false;
            }
        });

        searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus && !locationNames.isEmpty() && !searchView.getQuery().toString().isEmpty()) {
                    searchListView.setVisibility(View.VISIBLE);
                } else {
                    searchListView.setVisibility(View.GONE);
                }
            }
        });

        searchView.postDelayed(new Runnable() {
            @Override
            public void run() {
                applySearchViewTextColor();
            }
        }, 100);
    }

    private void applySearchViewTextColor() {
        try {
            int searchTextId = getResources().getIdentifier("android:id/search_src_text", null, null);
            TextView searchText = searchView.findViewById(searchTextId);
            if (searchText != null) {
                searchText.setTextColor(Color.BLACK);
                searchText.setHintTextColor(Color.GRAY);
                searchText.setTextSize(16);
            }

            // Style the search icon
            int searchIconId = getResources().getIdentifier("android:id/search_mag_icon", null, null);
            ImageView searchIcon = searchView.findViewById(searchIconId);
            if (searchIcon != null) {
                searchIcon.setColorFilter(Color.GRAY);
            }

        } catch (Exception e) {
            Log.e("SearchView", "Could not set text color", e);
        }
    }

    private void fetchLocationsFromFirebase() {
        // Show loading indicator if needed
        routesDatabaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Process data in background thread
                new Thread(() -> {
                    List<String> tempLocationNames = new ArrayList<>();
                    Map<String, LatLng> tempLocationMap = new HashMap<>();
                    Set<String> uniquePlacesSet = new HashSet<>();

                    for (DataSnapshot routeSnapshot : snapshot.getChildren()) {
                        for (DataSnapshot busStopSnapshot : routeSnapshot.getChildren()) {
                            // Get the placeName directly from the snapshot
                            String placeName = busStopSnapshot.child("placeName").getValue(String.class);

                            if (placeName != null && !placeName.isEmpty() && !uniquePlacesSet.contains(placeName)) {
                                uniquePlacesSet.add(placeName);
                                tempLocationNames.add(placeName);

                                // Get latitude and longitude
                                String latStr = busStopSnapshot.child("latitude").getValue(String.class);
                                String lngStr = busStopSnapshot.child("longitude").getValue(String.class);

                                if (latStr != null && lngStr != null) {
                                    try {
                                        double lat = Double.parseDouble(latStr);
                                        double lng = Double.parseDouble(lngStr);
                                        LatLng location = new LatLng(lat, lng);
                                        tempLocationMap.put(placeName, location);
                                    } catch (NumberFormatException e) {
                                        Log.e("CurrentLocationActivity", "Error parsing coordinates for: " + placeName, e);
                                    }
                                }
                            }
                        }
                    }

                    // Update UI on main thread
                    uiHandler.post(() -> {
                        locationNames.clear();
                        locationNames.addAll(tempLocationNames);
                        locationMap.clear();
                        locationMap.putAll(tempLocationMap);
                        searchAdapter.notifyDataSetChanged();
                        Log.d("CurrentLocationActivity", "Loaded " + locationNames.size() + " locations");
                    });
                }).start();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                uiHandler.post(() -> {
                    Toast.makeText(CurrentLocationActivity.this, "Failed to load locations", Toast.LENGTH_SHORT).show();
                    Log.e("CurrentLocationActivity", "Database error: " + error.getMessage());
                });
            }
        });
    }

    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_PERMISSION_CODE);
            return;
        }

        Task<Location> task = fusedLocationProviderClient.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    currentLocation = location;

                    // Only set up the map once location is fetched
                    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                            .findFragmentById(R.id.map);
                    if (mapFragment != null) {
                        mapFragment.getMapAsync(CurrentLocationActivity.this);
                    }
                } else {
                    Toast.makeText(CurrentLocationActivity.this, "Location not available", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (currentLocation != null) {
            // After map is ready, move the camera to the current location
            newLocation = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newLocation, 15)); // Zoom to a reasonable level

            MarkerOptions options = new MarkerOptions()
                    .position(newLocation)
                    .title("I am here")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN));

            // Add marker to the map
            mMap.addMarker(options);

            mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker clickedMarker) {
                    // Show a Toast with the latitude and longitude of the marker
                    LatLng position = clickedMarker.getPosition();
                    String message = "Latitude: " + position.latitude + ", Longitude: " + position.longitude;
                    System.out.println(message);
                    Toast.makeText(CurrentLocationActivity.this, message, Toast.LENGTH_SHORT).show();
                    return false; // Return false to allow the default behavior (camera move)
                }
            });
        } else {
            Toast.makeText(this, "Location is null, unable to update camera.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == FINE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            } else {
                Toast.makeText(this, "Location permission is denied. Please allow permission to access location.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (currentLocation != null) {
            double lat = currentLocation.getLatitude();
            double lon = currentLocation.getLongitude();

            passengerLocation p = new passengerLocation(lat, lon);
            String key = databaseReference.push().getKey();

            databaseReference.child(key).setValue(p).
                    addOnSuccessListener(aVoid -> {
                        Toast.makeText(CurrentLocationActivity.this, "Location saved successfully", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(CurrentLocationActivity.this, "Failed to save location: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });

            LatLng newLocation = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            Intent i = new Intent(CurrentLocationActivity.this, Search_activity.class);
            i.putExtra("currentLocation", newLocation);

            // Also send the selected location name if available from search
            String currentQuery = searchView.getQuery().toString();
            if (!currentQuery.isEmpty() && locationMap.containsKey(currentQuery)) {
                i.putExtra("nearestRoadPointName", currentQuery);
            }

            startActivity(i);
        } else {
            Toast.makeText(this, "something went wrong", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String selectedPlaceName = searchAdapter.getItem(position);
        LatLng selectedLocation = locationMap.get(selectedPlaceName);

        if (selectedLocation != null) {
            // Update the search view text
            searchView.setQuery(selectedPlaceName, false);
            searchListView.setVisibility(View.GONE);

            // Hide keyboard after selection
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);

            // Update the map to show the selected location
            if (mMap != null) {
                mMap.clear();
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(selectedLocation, 16));
                mMap.addMarker(new MarkerOptions()
                        .position(selectedLocation)
                        .title(selectedPlaceName)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

                // Add a circle to highlight the selected area
                mMap.addCircle(new CircleOptions()
                        .center(selectedLocation)
                        .radius(100) // 100 meters radius
                        .strokeColor(Color.BLUE)
                        .fillColor(Color.argb(30, 0, 0, 255))
                        .strokeWidth(2));
            }

            // Update current location to the selected location
            currentLocation = new Location("selected");
            currentLocation.setLatitude(selectedLocation.latitude);
            currentLocation.setLongitude(selectedLocation.longitude);

            // Show a nice toast message
            Toast.makeText(this, "📍 " + selectedPlaceName + " selected", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Location coordinates not found for: " + selectedPlaceName, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up handler to prevent memory leaks
        uiHandler.removeCallbacksAndMessages(null);
    }
}