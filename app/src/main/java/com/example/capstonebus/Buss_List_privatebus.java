package com.example.capstonebus;

import android.app.ProgressDialog;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Buss_List_privatebus extends AppCompatActivity {
    private static final String TAG = "PrivateBusList";


    private final Map<String, List<BusRoute>> map2 = new HashMap<>();
    private final List<String> directRoutes = new ArrayList<>();

    // list shown in UI
    private final List<BusTrackingData> approachingBuses = new ArrayList<>();

    // pending updates (merged in batch UI tick)
    private final List<BusTrackingData> pendingBusUpdates = new ArrayList<>();

    private RecyclerView busList;
    private BusTrackingListAdapter trackingAdapter;

    private DatabaseReference driverLocationRef;
    private ValueEventListener busLocationListener;

    private ProgressDialog progressDialog;

    private TextView startPointTv, endPointTv;
    private LatLng routeStartPoint, routeEndPoint;
    private String destinationName;

    private Button alternateRouteButton;

    private final Handler updateHandler = new Handler(Looper.getMainLooper());
    private static final long UPDATE_INTERVAL = 1000;        // UI merge every 1s
    private static final double MAX_BUS_DISTANCE_KM = 15.0;  // 15km

    private final ExecutorService bg = Executors.newFixedThreadPool(3);

    private final Runnable batchUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            if (!pendingBusUpdates.isEmpty()) {
                synchronized (pendingBusUpdates) {
                    for (BusTrackingData updated : new ArrayList<>(pendingBusUpdates)) {
                        boolean found = false;
                        for (int i = 0; i < approachingBuses.size(); i++) {
                            if (approachingBuses.get(i).getBusNumber().equals(updated.getBusNumber())) {
                                approachingBuses.set(i, updated);
                                found = true;
                                break;
                            }
                        }
                        if (!found) approachingBuses.add(updated);
                    }
                    pendingBusUpdates.clear();
                }

                // DiffUtil updates only changed rows
                trackingAdapter.submitList(new ArrayList<>(approachingBuses));
            }
            updateHandler.postDelayed(this, UPDATE_INTERVAL);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buss_list);

        busList = findViewById(R.id.busList); // must be RecyclerView in XML
        startPointTv = findViewById(R.id.startPoint);
        endPointTv = findViewById(R.id.endPoint);
        alternateRouteButton = findViewById(R.id.alternateRouteButton);

        busList.setLayoutManager(new LinearLayoutManager(this));

        // ✅ CLICK IMPLEMENTED HERE
        trackingAdapter = new BusTrackingListAdapter(bus -> {
            Intent intent = new Intent(Buss_List_privatebus.this, RouteMapActivity.class);

            // IMPORTANT: routeName must be the route key
            intent.putExtra("routeName", bus.getRouteName());

            intent.putExtra("userStartPoint", routeStartPoint);
            intent.putExtra("userEndPoint", routeEndPoint);
            intent.putExtra("busNumber", bus.getBusNumber());

            startActivity(intent);
        });

        busList.setAdapter(trackingAdapter);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Finding available buses...");
        progressDialog.setCancelable(false);

        String startName = getFirstWord(getIntent().getStringExtra("nearestRoadPointName"));
        String destName = getFirstWord(getIntent().getStringExtra("destination"));

        startPointTv.setText(startName);
        endPointTv.setText(destName);

        routeStartPoint = getIntent().getParcelableExtra("nearestRoadPoint");
        destinationName = getIntent().getStringExtra("destination");

        driverLocationRef = FirebaseDatabase.getInstance().getReference("driver_location/private_bus");

        // Load route data and start tracking
        fetchRoutesData();
    }

    private String getFirstWord(String text) {
        if (text == null) return "";
        text = text.trim();
        String[] parts = text.split("\\s+");
        return parts.length > 0 ? parts[0] : text;
    }

    private void fetchRoutesData() {
        DatabaseReference routesRef = FirebaseDatabase.getInstance().getReference("Routes");
        progressDialog.show();

        routesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                map2.clear();
                for (DataSnapshot routeSnapshot : snapshot.getChildren()) {
                    String routeName = routeSnapshot.getKey();
                    List<BusRoute> routeInfo = new ArrayList<>();

                    for (DataSnapshot busRouteSnapshot : routeSnapshot.getChildren()) {
                        BusRoute busRoute = busRouteSnapshot.getValue(BusRoute.class);
                        if (busRoute != null) routeInfo.add(busRoute);
                    }

                    if (routeName != null) map2.put(routeName, routeInfo);
                }

                routeEndPoint = findLatLngForPlaceName(destinationName);

                if (routeEndPoint != null) {
                    findDirectRoutes(routeStartPoint, routeEndPoint);
                } else {
                    progressDialog.dismiss();
                    Toast.makeText(Buss_List_privatebus.this,
                            "Could not find destination location", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.dismiss();
                Toast.makeText(Buss_List_privatebus.this,
                        "Failed to load routes", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private LatLng findLatLngForPlaceName(String placeName) {
        if (placeName == null || placeName.isEmpty() || map2.isEmpty()) return null;

        for (Map.Entry<String, List<BusRoute>> entry : map2.entrySet()) {
            for (BusRoute route : entry.getValue()) {
                if (route.getPlaceName() != null &&
                        route.getPlaceName().trim().equalsIgnoreCase(placeName.trim())) {
                    try {
                        double lat = Double.parseDouble(route.getLatitude());
                        double lng = Double.parseDouble(route.getLongitude());
                        return new LatLng(lat, lng);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        return null;
    }

    private void findDirectRoutes(LatLng startPoint, LatLng endPoint) {
        if (startPoint == null || endPoint == null) {
            progressDialog.dismiss();
            Toast.makeText(this, "Invalid route points", Toast.LENGTH_SHORT).show();
            return;
        }

        directRoutes.clear();
        approachingBuses.clear();
        trackingAdapter.submitList(new ArrayList<>());

        for (Map.Entry<String, List<BusRoute>> entry : map2.entrySet()) {
            String routeName = entry.getKey();
            boolean hasStart = false, hasEnd = false;

            for (BusRoute route : entry.getValue()) {
                try {
                    double lat = Double.parseDouble(route.getLatitude());
                    double lng = Double.parseDouble(route.getLongitude());

                    if (lat == startPoint.latitude && lng == startPoint.longitude) hasStart = true;
                    if (lat == endPoint.latitude && lng == endPoint.longitude) hasEnd = true;
                } catch (Exception ignored) {
                }
            }

            if (hasStart && hasEnd) {
                directRoutes.add(routeName);
                findBusesForRoute(routeName, startPoint, endPoint);
            }
        }

        if (directRoutes.isEmpty()) {
            progressDialog.dismiss();

            Intent intent = new Intent(Buss_List_privatebus.this, ShowAllPrivateBus.class);
            intent.putExtra("userStartPoint", startPoint);
            intent.putExtra("userEndPoint", endPoint);
            startActivity(intent);
            finish();
        } else {
            alternateRouteButton.setVisibility(android.view.View.GONE);
        }
    }

    private void findBusesForRoute(String routeName, LatLng startPoint, LatLng endPoint) {
        Query query = driverLocationRef
                .orderByChild("Bus_Info/to")
                .equalTo(routeName);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                approachingBuses.clear();
                trackingAdapter.submitList(new ArrayList<>());

                for (DataSnapshot busSnapshot : snapshot.getChildren()) {
                    String busNumber = busSnapshot.getKey();

                    String status = busSnapshot.child("status").getValue(String.class);
                    if (status == null) status = "inactive";

                    BusDriverInfo busDriverInfo = busSnapshot.child("Bus_Info").getValue(BusDriverInfo.class);
                    if (busNumber == null || busDriverInfo == null) continue;

                    String driverName = busDriverInfo.getDriverName();
                    String mobile = busDriverInfo.getMobile();

                    checkBusMovement(busNumber, routeName, driverName, mobile, status, startPoint, endPoint);
                }

                progressDialog.dismiss();

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (approachingBuses.isEmpty()) {
                        Intent intent = new Intent(Buss_List_privatebus.this, ShowAllPrivateBus.class);
                        intent.putExtra("userStartPoint", startPoint);
                        intent.putExtra("userEndPoint", endPoint);
                        startActivity(intent);
                        finish();
                    }
                }, 1200);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.dismiss();
                Toast.makeText(Buss_List_privatebus.this,
                        "Error loading buses", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkBusMovement(String busNumber,
                                  String routeName,
                                  String driverName,
                                  String mobile,
                                  String status,
                                  LatLng startPoint,
                                  LatLng endPoint) {

        DatabaseReference locationsRef = driverLocationRef.child(busNumber).child("locations");

        locationsRef.limitToLast(3).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                bg.execute(() -> {
                    List<LatLng> locations = new ArrayList<>();
                    Businfo newestInfo = null;

                    float speedSum = 0f;
                    int speedCount = 0;

                    for (DataSnapshot locSnapshot : snapshot.getChildren()) {
                        Businfo info = locSnapshot.getValue(Businfo.class);
                        if (info != null) {
                            locations.add(new LatLng(info.getLat(), info.getLon()));
                            newestInfo = info;

                            speedSum += info.getSpeed();
                            speedCount++;
                        }
                    }

                    if (locations.size() < 2 || newestInfo == null) return;

                    LatLng newestLoc = locations.get(locations.size() - 1);

                    boolean approachingStart = isApproaching(locations, startPoint);
                    boolean approachingEnd = isApproaching(locations, endPoint);

                    float avgSpeedMps = (speedCount > 0) ? (speedSum / speedCount) : newestInfo.getSpeed();

                    if (approachingStart && approachingEnd) {
                        addOrUpdateBus(
                                busNumber, routeName, driverName, mobile, status,
                                newestLoc, newestInfo.timestamp, avgSpeedMps,
                                startPoint, endPoint
                        );
                    } else {
                        removeBusIfExists(busNumber);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private boolean isApproaching(List<LatLng> locations, LatLng target) {
        if (locations.size() == 2) {
            return distanceBetween(locations.get(1), target) < distanceBetween(locations.get(0), target);
        } else if (locations.size() >= 3) {
            double d1 = distanceBetween(locations.get(0), target);
            double d2 = distanceBetween(locations.get(1), target);
            double d3 = distanceBetween(locations.get(2), target);
            return (d3 < d2) && (d2 < d1);
        }
        return false;
    }

    private void addOrUpdateBus(String busNumber,
                                String routeName,
                                String driverName,
                                String mobile,
                                String status,
                                LatLng busLocation,
                                long timestamp,
                                float speedMps,
                                LatLng startPoint,
                                LatLng endPoint) {

        double distanceToStart = calculateRouteDistance(busLocation, startPoint, routeName);
        double distanceToEnd = calculateRouteDistance(busLocation, endPoint, routeName);

        if (distanceToStart >= distanceToEnd) {
            removeBusIfExists(busNumber);
            return;
        }

        if (distanceToStart > MAX_BUS_DISTANCE_KM * 1000) {
            removeBusIfExists(busNumber);
            return;
        }

        double routeLength = calculateRouteDistance(startPoint, endPoint, routeName);
        double progress = calculateApproachProgress(distanceToStart, routeLength);

        String nearestPlaceName = findNearestPlaceName(busLocation, routeName);
        Log.d(TAG,
                "addOrUpdateBus: bus=" + busNumber
                        + " driver=" + driverName
                        + " status=" + status
                        + " speed=" + speedMps + " m/s"
                        + " dStart=" + (int) distanceToStart + "m"
                        + " dEnd=" + (int) distanceToEnd + "m"
        );


        BusTrackingData updated = new BusTrackingData(
                busNumber,
                routeName,
                driverName,
                mobile,
                status,
                busLocation.latitude,
                busLocation.longitude,
                timestamp,
                speedMps,
                distanceToStart,
                distanceToEnd,
                progress,
                nearestPlaceName
        );

        synchronized (pendingBusUpdates) {
            for (int i = 0; i < pendingBusUpdates.size(); i++) {
                if (pendingBusUpdates.get(i).getBusNumber().equals(busNumber)) {
                    pendingBusUpdates.set(i, updated);
                    return;
                }
            }
            pendingBusUpdates.add(updated);
        }
    }

    private void removeBusIfExists(String busNumber) {
        synchronized (pendingBusUpdates) {
            pendingBusUpdates.removeIf(b -> busNumber.equals(b.getBusNumber()));
        }

        runOnUiThread(() -> {
            boolean removed = approachingBuses.removeIf(b -> busNumber.equals(b.getBusNumber()));
            if (removed) trackingAdapter.submitList(new ArrayList<>(approachingBuses));
        });
    }

    private double calculateRouteDistance(LatLng point1, LatLng point2, String routeName) {
        if (point1 == null || point2 == null || routeName == null) return 0;

        List<BusRoute> routePoints = map2.get(routeName);
        if (routePoints == null || routePoints.isEmpty()) {
            return distanceBetween(point1, point2);
        }

        List<LatLng> pathPoints = new ArrayList<>();
        for (BusRoute r : routePoints) {
            try {
                double lat = Double.parseDouble(r.getLatitude());
                double lng = Double.parseDouble(r.getLongitude());
                pathPoints.add(new LatLng(lat, lng));
            } catch (Exception ignored) {
            }
        }

        if (pathPoints.isEmpty()) return distanceBetween(point1, point2);

        int index1 = findClosestPointIndex(point1, pathPoints);
        int index2 = findClosestPointIndex(point2, pathPoints);

        if (index1 > index2) {
            int t = index1;
            index1 = index2;
            index2 = t;
        }

        double total = 0;
        for (int i = index1; i < index2; i++) {
            total += distanceBetween(pathPoints.get(i), pathPoints.get(i + 1));
        }
        return total;
    }

    private int findClosestPointIndex(LatLng point, List<LatLng> pathPoints) {
        int closestIndex = 0;
        double min = Double.MAX_VALUE;

        for (int i = 0; i < pathPoints.size(); i++) {
            double d = distanceBetween(point, pathPoints.get(i));
            if (d < min) {
                min = d;
                closestIndex = i;
            }
        }
        return closestIndex;
    }

    private double calculateApproachProgress(double currentDistance, double referenceDistance) {
        if (referenceDistance <= 0) return 0;
        double remainingRatio = currentDistance / referenceDistance;
        double progress = (1 - remainingRatio) * 100;
        return Math.max(0, Math.min(100, progress));
    }

    private double distanceBetween(LatLng p1, LatLng p2) {
        float[] results = new float[1];
        Location.distanceBetween(p1.latitude, p1.longitude, p2.latitude, p2.longitude, results);
        return results[0];
    }

    @Override
    protected void onStart() {
        super.onStart();
        startRealTimeTracking();
        updateHandler.postDelayed(batchUpdateRunnable, UPDATE_INTERVAL);
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopRealTimeTracking();
        updateHandler.removeCallbacks(batchUpdateRunnable);
    }

    private void startRealTimeTracking() {
        busLocationListener = driverLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (BusTrackingData bus : new ArrayList<>(approachingBuses)) {
                    checkBusMovement(
                            bus.getBusNumber(),
                            bus.getRouteName(),
                            bus.getDriverName(),
                            bus.getMobile(),
                            bus.getStatus(),
                            routeStartPoint,
                            routeEndPoint
                    );
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void stopRealTimeTracking() {
        if (busLocationListener != null) {
            driverLocationRef.removeEventListener(busLocationListener);
        }
    }

    private String findNearestPlaceName(LatLng busLocation, String busRouteName) {
        if (map2.isEmpty() || busLocation == null || busRouteName == null) return "Unknown Location";

        List<BusRoute> points = map2.get(busRouteName);
        if (points == null || points.isEmpty()) return "Unknown Location";

        String nearest = "Unknown Location";
        double shortest = Double.MAX_VALUE;

        for (BusRoute r : points) {
            try {
                double lat = Double.parseDouble(r.getLatitude());
                double lng = Double.parseDouble(r.getLongitude());
                LatLng routePoint = new LatLng(lat, lng);

                double d = distanceBetween(busLocation, routePoint);
                if (d < shortest) {
                    shortest = d;
                    nearest = r.getPlaceName();
                }
            } catch (Exception ignored) {
            }
        }

        return (shortest > 200.0) ? "Unknown Location" : nearest;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bg.shutdown();
    }
}
