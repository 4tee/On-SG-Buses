package net.felixmyanmar.onsgbuses.container;

import android.location.Location;

public class Midpoint {

    /* This will contain geo location with longitude and latitude */
    Location midGeoPoint;

    /* Radius to geofence the midGeoPoint */
    float distanceInMeter;

    /* Upcoming Bus Stop Name */
    String busStopName;

    /* Upcoming Bus Stop Number */

    public int getBusStopNo() {
        return busStopNo;
    }

    public void setBusStopNo(int busStopNo) {
        this.busStopNo = busStopNo;
    }

    public String getBusStopName() {
        return busStopName;
    }

    public void setBusStopName(String busStopName) {
        this.busStopName = busStopName;
    }

    int busStopNo;

    public float getDistanceInMeter() {
        return distanceInMeter;
    }

    public void setDistanceInMeter(float distanceInMeter) {
        this.distanceInMeter = distanceInMeter;
    }

    public Location getMidGeoPoint() {
        return midGeoPoint;
    }

    public void setMidGeoPoint(Location midGeoPoint) {
        this.midGeoPoint = midGeoPoint;
    }

    public Midpoint() {
    }
}
