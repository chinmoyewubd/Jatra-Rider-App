package com.example.capstonebus;

public class passengerLocation {

    private Double lat;
    private Double lon;
    public long timestamp;

    public passengerLocation() {
        // Default constructor required for calls to DataSnapshot.getValue(passengerLocation.class)
    }

    public passengerLocation(Double lat, Double lon) {
        this.lat = lat;
        this.lon = lon;
        this.timestamp = System.currentTimeMillis();
    }

    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }

    public Double getLon() { return lon; }
    public void setLon(Double lon) { this.lon = lon; }
}

