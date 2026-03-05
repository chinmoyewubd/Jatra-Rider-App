package com.example.capstonebus;

public class PrivateBus {
    private String busNumber;  // e.g., "BUS001" (from Firebase key)
    private String mobile;      // e.g., "ROUTE-123" (from Bus_Info)
    private String driverName;
    private String status;
    private String from;
    private String to;

    public PrivateBus() {
        // Default constructor required for Firebase
    }

    public PrivateBus(String busNumber, String mobile, String driverName, String status, String from, String to) {
        this.busNumber = busNumber;
        this.mobile = mobile;
        this.driverName = driverName;
        this.status = status;
        this.from = from;
        this.to = to;
    }

    // Getters
    public String getBusNumber() {
        return busNumber;
    }

    public String getMobile() {
        return mobile;
    }

    public String getDriverName() {
        return driverName;
    }

    public String getStatus() {
        return status;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    // Setters
    public void setBusNumber(String busNumber) {
        this.busNumber = busNumber;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public void setTo(String to) {
        this.to = to;
    }
}