package com.example.capstonebus;

import static android.os.SystemClock.sleep;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.maps.model.LatLng;

public class MainActivity extends AppCompatActivity {
    Button privatebus,publicbus,showallprivatebus;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        privatebus = findViewById(R.id.privatebus);

        showallprivatebus= findViewById(R.id.showallprivatebus);

        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);








        privatebus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                sharedPreferences.edit().putString("tracking", "privatebus").apply();
                Intent i  = new Intent(MainActivity.this,Search_activity. class);
                startActivity(i);
            }
        });

        showallprivatebus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i  = new Intent(MainActivity.this, ShowAllPrivateBus.class);
                startActivity(i);
            }
        });





    }


}