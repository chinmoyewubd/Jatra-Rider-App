package com.example.capstonebus;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;  // Corrected import

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Search_bar_activity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    ListView fromList;
    private SearchView whereFromGo;  // Using the correct SearchView class
    ArrayList<BusRoute> busRouteList = new ArrayList<>();
    ArrayAdapter<String> arrayAdapter;
    Set<String> uniquePlacesSet = new HashSet<>();
    ArrayList<String> locationList;
    DatabaseReference databaseRef;

    //where to go
    String touch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_bar);
        System.out.println("searchbaractivity");


        // Initialize UI elements
        fromList = findViewById(R.id.fromList);
        whereFromGo = findViewById(R.id.whereFromGo);
        //where to go have to change the name because the name is weired
        touch = getIntent().getStringExtra("etTo");
        databaseRef = FirebaseDatabase.getInstance().getReference("Routes");

   // Sample Data
        String[] locations = {
                 "test"
        };
        locationList = new ArrayList<>(Arrays.asList(locations));

        // Setup Adapter
        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, locationList);
        fromList.setAdapter(arrayAdapter);
        fromList.setOnItemClickListener(this);

        // Fix SearchView (Expand by default)
        whereFromGo.setIconifiedByDefault(false);

        // Implement Search Functionality
        whereFromGo.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                arrayAdapter.getFilter().filter(newText);
                return false;
            }
        });
    }

    @Override
    protected void onStart() {
        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot route : snapshot.getChildren()) {
                    for (DataSnapshot point : route.getChildren()) {
                        String placeName = point.child("placeName").getValue(String.class);
                        if (placeName != null) {
                            uniquePlacesSet.add(placeName);
                        }
                    }
                }

                locationList.addAll(uniquePlacesSet);
                Log.d("UniquePlaces", locationList.toString());

                // Use uniquePlaceNamesList as needed (e.g., show in RecyclerView)
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Database error: " + error.getMessage());
            }
        });
        super.onStart();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String value = arrayAdapter.getItem(position);
        Toast.makeText(this, "Selected: " + value, Toast.LENGTH_SHORT).show();

        Intent resultIntent = new Intent();
        if ("etTo".equalsIgnoreCase(touch)) {
            resultIntent.putExtra("selected2", value);
        } else {
            resultIntent.putExtra("selected", value);
        }

        setResult(RESULT_OK, resultIntent);
        finish(); // Close this activity and return to Search_activity
    }

}

