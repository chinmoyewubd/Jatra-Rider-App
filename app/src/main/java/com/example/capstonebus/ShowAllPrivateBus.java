package com.example.capstonebus;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ShowAllPrivateBus extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PrivateBusAdapter adapter;
    private List<PrivateBus> busList;
    private DatabaseReference databaseReference;

    // UI Components
    private ProgressBar progressBar;
    private TextView emptyView;

    // For background processing
    private ExecutorService executorService;
    private Handler mainHandler;

    // ✅ ADDED: keep listener reference so we can remove it safely
    private ValueEventListener busesListener; // ✅ ADDED

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_all_private_bus);

        // Initialize UI components
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true); // Performance optimization

        busList = new ArrayList<>();
        adapter = new PrivateBusAdapter(this, busList);
        recyclerView.setAdapter(adapter);

        // Initialize background executor and main thread handler
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        databaseReference = FirebaseDatabase.getInstance()
                .getReference("driver_location/private_bus");

        // Show loading (listener will attach in onStart)
        showLoading(); // ✅ CHANGED (kept but now we attach listener later)
    }

    // ✅ ADDED: attach listener when screen is visible
    @Override
    protected void onStart() { // ✅ ADDED
        super.onStart();       // ✅ ADDED
        attachListener();      // ✅ ADDED
    }

    // ✅ ADDED: detach listener when leaving screen
    @Override
    protected void onStop() { // ✅ ADDED
        super.onStop();       // ✅ ADDED
        detachListener();     // ✅ ADDED
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
    }

    private void showContent() {
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
    }

    private void showEmpty() {
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
    }

    // ✅ ADDED: replaces loadPrivateBuses() (same logic but safe lifecycle)
    private void attachListener() { // ✅ ADDED
        if (busesListener != null) return; // ✅ ADDED (avoid double attach)

        busesListener = new ValueEventListener() { // ✅ ADDED
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                // ✅ ADDED: guard so we don't execute after executor is shut down
                if (isFinishing() || isDestroyed()
                        || executorService == null
                        || executorService.isShutdown()) {
                    return;
                }

                // Process data in background thread to avoid UI blocking
                executorService.execute(() -> {
                    List<PrivateBus> tempList = new ArrayList<>();

                    for (DataSnapshot busSnapshot : snapshot.getChildren()) {
                        try {
                            // Get Bus Number from Firebase key (e.g., BUS001)
                            String busNumber = busSnapshot.getKey();

                            // Get status
                            String status = busSnapshot.child("status").getValue(String.class);
                            if (status == null) {
                                status = "inactive";
                            }

                            // Get Bus_Info data using BusDriverInfo model
                            BusDriverInfo busInfo = busSnapshot.child("Bus_Info")
                                    .getValue(BusDriverInfo.class);

                            // Extract data from BusDriverInfo or use defaults
                            String driverName = "Unknown Driver";
                            String mobile = "N/A";
                            String from = "Unknown";
                            String to = "Unknown";

                            if (busInfo != null) {
                                if (busInfo.getDriverName() != null) driverName = busInfo.getDriverName();
                                if (busInfo.getMobile() != null) mobile = busInfo.getMobile();
                                if (busInfo.getFrom() != null) from = busInfo.getFrom();
                                if (busInfo.getTo() != null) to = busInfo.getTo();
                            }

                            // Create PrivateBus object
                            PrivateBus bus = new PrivateBus(busNumber, mobile, driverName, status, from, to);
                            tempList.add(bus);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    // Update UI on main thread
                    mainHandler.post(() -> {
                        // ✅ ADDED: guard before touching UI
                        if (isFinishing() || isDestroyed()) return;

                        busList.clear();
                        busList.addAll(tempList);
                        adapter.notifyDataSetChanged();

                        if (busList.isEmpty()) {
                            showEmpty();
                        } else {
                            showContent();
                        }
                    });
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                mainHandler.post(() -> {
                    if (isFinishing() || isDestroyed()) return; // ✅ ADDED
                    progressBar.setVisibility(View.GONE);
                    emptyView.setText("Error loading buses: " + error.getMessage());
                    showEmpty();
                });
            }
        };

        databaseReference.addValueEventListener(busesListener); // ✅ ADDED
    }

    // ✅ ADDED: stop firebase callbacks to prevent onDataChange() after destroy
    private void detachListener() { // ✅ ADDED
        if (busesListener != null) {
            databaseReference.removeEventListener(busesListener);
            busesListener = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // ✅ ADDED: remove listener first
        detachListener(); // ✅ ADDED

        // Cleanup executor service safely
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow(); // ✅ CHANGED (shutdownNow safer here)
        }
        executorService = null; // ✅ ADDED
    }
}
