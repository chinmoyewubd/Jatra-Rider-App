package com.example.capstonebus;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class BusTrackingAdapter extends ArrayAdapter<BusTrackingData> {

    private final Context context;
    private final List<BusTrackingData> buses;

    private static final float MIN_SPEED_MPS = 1.5f;     // fallback if speed is 0
    private static final long OFFLINE_MS = 60_000L;       // 1 min -> show inactive

    public BusTrackingAdapter(@NonNull Context context, @NonNull List<BusTrackingData> buses) {
        super(context, R.layout.buses_row, buses);
        this.context = context;
        this.buses = buses;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.buses_row, parent, false);

            holder = new ViewHolder();
            holder.busName = convertView.findViewById(R.id.busName);
            holder.distance = convertView.findViewById(R.id.distance);
            holder.placeName = convertView.findViewById(R.id.placeName);
            holder.phoneNumber = convertView.findViewById(R.id.phoneNumber);

            holder.money = convertView.findViewById(R.id.money);

            holder.statusText = convertView.findViewById(R.id.statusText);
            holder.statusDot = convertView.findViewById(R.id.statusDot);

            holder.eat = convertView.findViewById(R.id.eat);
            holder.ettTime = convertView.findViewById(R.id.ettTime);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        BusTrackingData bus = buses.get(position);

        // ✅ Driver
        holder.busName.setText("Driver: " + safe(bus.getDriverName()));

        // ✅ Phone
        holder.phoneNumber.setText("Phone: " + safe(bus.getMobile()));

        // ✅ BUS number on right (your "money" TextView is used as BUS001)
        holder.money.setText(safe(bus.getBusNumber()));

        // ✅ Distance (Away)
        double dStart = bus.getDistanceToStart(); // meters
        if (dStart < 1000) {
            holder.distance.setText((int) dStart + " m");
        } else {
            holder.distance.setText(String.format("%.1f km", dStart / 1000.0));
        }

        // ✅ Nearest place
        if (bus.getNearestPlaceName() != null && !bus.getNearestPlaceName().equalsIgnoreCase("Unknown Location")) {
            holder.placeName.setText(bus.getNearestPlaceName());
            holder.placeName.setVisibility(View.VISIBLE);
        } else {
            holder.placeName.setVisibility(View.GONE);
        }

        // ✅ Status (DB status + offline override)
        boolean offline = (bus.getTimestamp() <= 0) ||
                ((System.currentTimeMillis() - bus.getTimestamp()) > OFFLINE_MS);

        boolean active = !offline && "active".equalsIgnoreCase(safe(bus.getStatus()));
        setStatus(holder, active);

        // ✅ EAT + ETT using speed (m/s)
        float speed = bus.getSpeedMps();
        if (speed < MIN_SPEED_MPS) speed = MIN_SPEED_MPS;

        int eatMin = metersToMinutes(bus.getDistanceToStart(), speed);
        int ettMin = metersToMinutes(bus.getDistanceToEnd(), speed);

        holder.eat.setText(eatMin + " min");
        holder.ettTime.setText(ettMin + " min");

        return convertView;
    }

    private static void setStatus(ViewHolder holder, boolean active) {
        if (active) {
            holder.statusText.setText("Active");
            holder.statusText.setTextColor(0xFF4CAF50);
            holder.statusDot.setBackgroundTintList(ColorStateList.valueOf(0xFF4CAF50));
        } else {
            holder.statusText.setText("Inactive");
            holder.statusText.setTextColor(0xFFF44336);
            holder.statusDot.setBackgroundTintList(ColorStateList.valueOf(0xFFF44336));
        }
    }

    private static int metersToMinutes(double meters, float speedMps) {
        if (meters <= 0) return 0;
        double sec = meters / speedMps;
        int min = (int) Math.ceil(sec / 60.0);
        return Math.max(1, min);
    }

    private static String safe(String s) {
        return (s == null || s.trim().isEmpty()) ? "N/A" : s.trim();
    }

    private static class ViewHolder {
        TextView busName, distance, placeName, phoneNumber;
        TextView money;
        TextView statusText;
        View statusDot;
        TextView eat, ettTime;
    }
}
