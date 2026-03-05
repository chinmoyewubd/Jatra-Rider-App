package com.example.capstonebus;

// BusTrackingData contains everything your list UI needs.
public class BusTrackingData {

    private String busNumber;        // Firebase key (BUS001)
    private String routeName;        // direct routeName (route key)
    private String driverName;
    private String mobile;           // phone number
    private String status;           // active / inactive

    private double latitude;
    private double longitude;

    private long timestamp;          // latest location timestamp (ms)
    private float speedMps;          // latest speed in m/s

    private double distanceToStart;  // meters
    private double distanceToEnd;    // meters
    private double routeProgress;    // 0-100 (optional)
    private String nearestPlaceName;

    public BusTrackingData() {}

    public BusTrackingData(String busNumber,
                           String routeName,
                           String driverName,
                           String mobile,
                           String status,
                           double latitude,
                           double longitude,
                           long timestamp,
                           float speedMps,
                           double distanceToStart,
                           double distanceToEnd,
                           double routeProgress,
                           String nearestPlaceName) {

        this.busNumber = busNumber;
        this.routeName = routeName;
        this.driverName = driverName;
        this.mobile = mobile;
        this.status = status;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.speedMps = speedMps;
        this.distanceToStart = distanceToStart;
        this.distanceToEnd = distanceToEnd;
        this.routeProgress = routeProgress;
        this.nearestPlaceName = nearestPlaceName;
    }

    public String getBusNumber() { return busNumber; }
    public void setBusNumber(String busNumber) { this.busNumber = busNumber; }

    public String getRouteName() { return routeName; }
    public void setRouteName(String routeName) { this.routeName = routeName; }

    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public float getSpeedMps() { return speedMps; }
    public void setSpeedMps(float speedMps) { this.speedMps = speedMps; }

    public double getDistanceToStart() { return distanceToStart; }
    public void setDistanceToStart(double distanceToStart) { this.distanceToStart = distanceToStart; }

    public double getDistanceToEnd() { return distanceToEnd; }
    public void setDistanceToEnd(double distanceToEnd) { this.distanceToEnd = distanceToEnd; }

    public double getRouteProgress() { return routeProgress; }
    public void setRouteProgress(double routeProgress) { this.routeProgress = routeProgress; }

    public String getNearestPlaceName() { return nearestPlaceName; }
    public void setNearestPlaceName(String nearestPlaceName) { this.nearestPlaceName = nearestPlaceName; }
}
