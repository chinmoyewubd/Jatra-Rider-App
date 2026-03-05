package com.example.capstonebus;

public class BusRoute {

    private int SerialNo;

    private String Latitude;
    private String Longitude;
    private String Traffic8am;
    private String Traffic12pm;
    private String Traffic4pm;
    private String Traffic8pm;
    private String PlaceName;

    public BusRoute() {
        // Default constructor required for Firestore
    }

    public BusRoute(int SerialNo,String Latitude, String Longitude, String Traffic8am,String Traffic12pm, String Traffic4pm,String Traffic8pm, String PlaceName) {
        this.SerialNo = SerialNo;
        this.Latitude = Latitude;
        this.Longitude = Longitude;
        this.Traffic8am = Traffic8am;
        this.Traffic12pm = Traffic12pm;
        this.Traffic4pm  = Traffic4pm;
        this.Traffic8pm=Traffic8pm;
        this.PlaceName = PlaceName;
    }

    public int getSerialNo() {
        return SerialNo;
    }

    public void setSerialNo(int serialNo) {
        SerialNo = serialNo;
    }

    public String getLatitude() {
        return Latitude;
    }

    public void setLatitude(String latitude) {
        Latitude = latitude;
    }

    public String getLongitude() {
        return Longitude;
    }

    public void setLongitude(String longitude) {
        Longitude = longitude;
    }

    public String getTraffic8am() {
        return Traffic8am;
    }

    public void setTraffic8am(String traffic8am) {
        Traffic8am = traffic8am;
    }

    public String getTraffic12pm() {
        return Traffic12pm;
    }

    public void setTraffic12pm(String traffic12pm) {
        Traffic12pm = traffic12pm;
    }

    public String getTraffic4pm() {
        return Traffic4pm;
    }

    public void setTraffic4pm(String traffic4pm) {
        Traffic4pm = traffic4pm;
    }

    public String getTraffic8pm() {
        return Traffic8pm;
    }

    public void setTraffic8pm(String traffic8pm) {
        Traffic8pm = traffic8pm;
    }

    public String getPlaceName() {
        return PlaceName;
    }

    public void setPlaceName(String placeName) {
        PlaceName = placeName;
    }
}
