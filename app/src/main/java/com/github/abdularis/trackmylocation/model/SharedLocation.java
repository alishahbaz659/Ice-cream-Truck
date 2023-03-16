package com.github.abdularis.trackmylocation.model;

public class SharedLocation {

    private LatLong mLocation;
    private String useruid;
    private String usertype;


    public String getUseruid() {
        return useruid;
    }

    public void setUseruid(String useruid) {
        this.useruid = useruid;
    }

    public String getUsertype() {
        return usertype;
    }

    public void setUsertype(String usertype) {
        this.usertype = usertype;
    }

    public SharedLocation() {
        mLocation = new LatLong();
        useruid = "";
        usertype = "";
    }


    public LatLong getLocation() {
        return mLocation;
    }

    public void setLocation(LatLong location) {
        mLocation = location;
    }

    public static class LatLong {
        public double latitude;
        public double longitude;

        public LatLong() {
            this(0.0, 0.0);
        }

        public LatLong(double lat, double lng) {
            latitude = lat;
            longitude = lng;
        }
    }

}
