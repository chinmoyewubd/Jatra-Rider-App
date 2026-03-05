package com.example.capstonebus;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class PrivateBusAdapter extends RecyclerView.Adapter<PrivateBusAdapter.BusViewHolder> {

    private Context context;
    private List<PrivateBus> busList;

    public PrivateBusAdapter(Context context, List<PrivateBus> busList) {
        this.context = context;
        this.busList = busList;
    }

    @NonNull
    @Override
    public BusViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_private_bus, parent, false);
        return new BusViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BusViewHolder holder, int position) {
        PrivateBus bus = busList.get(position);

        // Set Bus Number (e.g., BUS001) - from Firebase key
        holder.busNumberText.setText(bus.getDriverName());

        // Set Driver Name
        holder.driverNameText.setText(bus.getMobile());

        // Set Bus ID/Route ID (e.g., ROUTE-123) - from Bus_Info
        holder.busIdText.setText(bus.getBusNumber());

        // Set Route From and To
        holder.routeFromText.setText(bus.getFrom());
        holder.routeToText.setText(bus.getTo());

        // Set Status Indicator with Color
        String status = bus.getStatus();
        if (status != null && status.equalsIgnoreCase("active")) {
            // Active Bus - Green
            holder.statusText.setText("Active");
            holder.statusText.setTextColor(0xFF4CAF50); // Green
            holder.statusDot.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFF4CAF50)
            );
        } else {
            // Inactive Bus - Red
            holder.statusText.setText("Inactive");
            holder.statusText.setTextColor(0xFFF44336); // Red
            holder.statusDot.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFFF44336)
            );
        }

        // Click Listener - Open Map Activity
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, RouteMapActivityPrivate.class);
            intent.putExtra("busNumber", bus.getBusNumber());
            intent.putExtra("mobile", bus.getMobile());
            intent.putExtra("driverName", bus.getDriverName());
            intent.putExtra("from", bus.getFrom());
            intent.putExtra("to", bus.getTo());
            intent.putExtra("status", bus.getStatus());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return busList.size();
    }

    // ViewHolder Class
    public static class BusViewHolder extends RecyclerView.ViewHolder {
        TextView busNumberText;   // BUS001 (Firebase key)
        TextView driverNameText;  // Driver: Rahim
        TextView busIdText;       // ID: ROUTE-123 (from Bus_Info)
        TextView routeFromText;   // Dhaka
        TextView routeToText;     // Chittagong
        TextView statusText;      // Active/Inactive
        View statusDot;           // Green/Red status dot

        public BusViewHolder(@NonNull View itemView) {
            super(itemView);
            busNumberText = itemView.findViewById(R.id.busNumberText);
            driverNameText = itemView.findViewById(R.id.driverNameText);
            busIdText = itemView.findViewById(R.id.busIdText);
            routeFromText = itemView.findViewById(R.id.routeFromText);
            routeToText = itemView.findViewById(R.id.routeToText);
            statusText = itemView.findViewById(R.id.statusText);
            statusDot = itemView.findViewById(R.id.statusDot);
        }
    }
}