package com.example.capstonebus;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

public class BusTrackingListAdapter extends ListAdapter<BusTrackingData, BusTrackingListAdapter.VH> {

    public interface OnBusClickListener {
        void onBusClick(BusTrackingData bus);
    }

    private final OnBusClickListener clickListener;

    private static final float MIN_SPEED_MPS = 1.5f;     // fallback when speed is 0
    private static final long OFFLINE_MS = 60_000L;       // 1 minute -> inactive

    public BusTrackingListAdapter(@NonNull OnBusClickListener listener) {
        super(DIFF_CALLBACK);
        this.clickListener = listener;
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        String key = getItem(position).getBusNumber();
        return key == null ? RecyclerView.NO_ID : key.hashCode();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.buses_row, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        BusTrackingData bus = getItem(position);

        // ✅ click (use getAdapterPosition for older RecyclerView versions)
        h.itemView.setOnClickListener(v -> {
            int p = h.getAdapterPosition();
            if (p != RecyclerView.NO_POSITION) {
                BusTrackingData clicked = getItem(p);
                android.util.Log.d("BusTrackingListAdapter",
                        "CLICK: car=" + clicked.getBusNumber() + " driver=" + clicked.getDriverName());
                clickListener.onBusClick(clicked);
            }
        });


        // Driver name
        h.busName.setText(safe(bus.getDriverName()));

        // Phone number
        h.phoneNumber.setText("Phone: " + safe(bus.getMobile()));

        // Right side (BUS001)
        h.money.setText(safe(bus.getBusNumber()));

        // Distance to start
        double dStart = bus.getDistanceToStart();
        if (dStart < 1000) {
            h.distance.setText((int) dStart + " m");
        } else {
            h.distance.setText(String.format("%.1f km", dStart / 1000.0));
        }

        // Nearest place
        String place = bus.getNearestPlaceName();
        if (place != null && !place.equalsIgnoreCase("Unknown Location") && !place.trim().isEmpty()) {
            h.placeName.setText(place);
            h.placeName.setVisibility(View.VISIBLE);
        } else {
            h.placeName.setVisibility(View.GONE);
        }

        // Status
        boolean offline = bus.getTimestamp() > 0 && (System.currentTimeMillis() - bus.getTimestamp()) > OFFLINE_MS;
        boolean active = !offline && "active".equalsIgnoreCase(safe(bus.getStatus()));
        setStatus(h, active);

        // EAT + ETT using speed (m/s)
        float speed = bus.getSpeedMps();
        if (speed < MIN_SPEED_MPS) speed = MIN_SPEED_MPS;

        int eatMin = metersToMinutes(bus.getDistanceToStart(), speed);
        int ettMin = metersToMinutes(bus.getDistanceToEnd(), speed);

        h.eat.setText(eatMin + " min");
        h.ettTime.setText(ettMin + " min");
    }

    private static void setStatus(VH h, boolean active) {
        if (active) {
            h.statusText.setText("Active");
            h.statusText.setTextColor(0xFF4CAF50);
            h.statusDot.setBackgroundTintList(ColorStateList.valueOf(0xFF4CAF50));
        } else {
            h.statusText.setText("Inactive");
            h.statusText.setTextColor(0xFFF44336);
            h.statusDot.setBackgroundTintList(ColorStateList.valueOf(0xFFF44336));
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

    static class VH extends RecyclerView.ViewHolder {
        TextView busName, distance, placeName, phoneNumber, money;
        TextView statusText, eat, ettTime;
        View statusDot;

        VH(@NonNull View itemView) {
            super(itemView);
            busName = itemView.findViewById(R.id.busName);
            distance = itemView.findViewById(R.id.distance);
            placeName = itemView.findViewById(R.id.placeName);
            phoneNumber = itemView.findViewById(R.id.phoneNumber);
            money = itemView.findViewById(R.id.money);

            statusText = itemView.findViewById(R.id.statusText);
            statusDot = itemView.findViewById(R.id.statusDot);

            eat = itemView.findViewById(R.id.eat);
            ettTime = itemView.findViewById(R.id.ettTime);
        }
    }

    private static final DiffUtil.ItemCallback<BusTrackingData> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<BusTrackingData>() {
                @Override
                public boolean areItemsTheSame(@NonNull BusTrackingData oldItem, @NonNull BusTrackingData newItem) {
                    if (oldItem.getBusNumber() == null) return false;
                    return oldItem.getBusNumber().equals(newItem.getBusNumber());
                }

                @Override
                public boolean areContentsTheSame(@NonNull BusTrackingData o, @NonNull BusTrackingData n) {
                    return eq(o.getDriverName(), n.getDriverName())
                            && eq(o.getMobile(), n.getMobile())
                            && eq(o.getStatus(), n.getStatus())
                            && eq(o.getNearestPlaceName(), n.getNearestPlaceName())
                            && o.getDistanceToStart() == n.getDistanceToStart()
                            && o.getDistanceToEnd() == n.getDistanceToEnd()
                            && o.getSpeedMps() == n.getSpeedMps()
                            && o.getTimestamp() == n.getTimestamp();
                }

                private boolean eq(String a, String b) {
                    if (a == null && b == null) return true;
                    if (a == null) return false;
                    return a.equals(b);
                }
            };
}
