package net.felixmyanmar.onsgbuses;

public class BusStops {

    private int sequence;
    private int busStopNo;
    private String busStopName;
    private float latitude;
    private float longitude;

    public String getBusStopName() {
        return busStopName;
    }

    public void setBusStopName(String busStopName) {
        this.busStopName = busStopName;
    }

    public int getBusStopNo() {
        return busStopNo;
    }

    public void setBusStopNo(int busStopNo) {
        this.busStopNo = busStopNo;
    }

    public float getLatitude() {
        return latitude;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }

    public float getLongitude() {
        return longitude;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    @Override
    public boolean equals(Object o) {
        if (o==null) return false;
        if (!(o instanceof BusStops)) return false;

        BusStops other = (BusStops) o;
        return other.busStopNo == this.busStopNo;
    }

    @Override
    public int hashCode() {
        return busStopNo;
    }
}
