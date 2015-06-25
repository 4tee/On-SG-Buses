package net.felixmyanmar.onsgbuses.geofencing;

import android.content.Context;
import android.location.Location;

import com.google.android.gms.location.Geofence;

import net.felixmyanmar.onsgbuses.container.BusStops;
import net.felixmyanmar.onsgbuses.container.Midpoint;
import net.felixmyanmar.onsgbuses.database.CoolDatabase;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;


public class GeofenceHelper {

    /**
     * Using latitude and longtiude, you can create Location object in Java.
     * Provider name is unnecessary though.
     *
     * @param lat latitude of point
     * @param lon longitude of point
     * @return Location object
     */
    private static Location createLocation(double lat, double lon) {
        Location aLocation = new Location(""); //provider name is unnecessary
        aLocation.setLatitude(lat);
        aLocation.setLongitude(lon);

        return aLocation;
    }

    /**
     * This uses the 'haversine' formula to calculate the half-way point along a great path between
     * two points.
     * http://www.movable-type.co.uk/scripts/latlong.html
     *
     * @param lat1 latitude of point 1 in double
     * @param lon1 longitude of point 1 in double
     * @param lat2 latitude of point 2 in double
     * @param lon2 longitude of point 2 in double
     * @return Location object of midpoint
     */
    private static Location getMidPoint(double lat1, double lon1, double lat2, double lon2) {
        double dLon = Math.toRadians(lon2 - lon1);

        //convert to radians
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);
        lon1 = Math.toRadians(lon1);

        double Bx = Math.cos(lat2) * Math.cos(dLon);
        double By = Math.cos(lat2) * Math.sin(dLon);
        double lat3 = Math.atan2(Math.sin(lat1) + Math.sin(lat2), Math.sqrt((Math.cos(lat1) + Bx) * (Math.cos(lat1) + Bx) + By * By));
        double lon3 = lon1 + Math.atan2(By, Math.cos(lat1) + Bx);

        return createLocation(Math.toDegrees(lat3), Math.toDegrees(lon3));
    }


    private static ArrayList<BusStops> getAllBusStops(Context context, String service_no, int direction) {
        CoolDatabase db = new CoolDatabase(context);
        ArrayList<BusStops> routeStartStops = db.getBusStops(service_no, direction, false);
        ArrayList<BusStops> routeEndStops = db.getBusStops(service_no, direction, true);

        // Combine and find all unique bus stops
        Set<BusStops> container = new LinkedHashSet<>(routeStartStops);
        container.addAll(routeEndStops);
        return new ArrayList<>(container);
    }


    public static ArrayList<Geofence> populateGeofenceList(Context context, String service_no, int direction) {

        int start = 0;

        ArrayList<Geofence> mGeofenceList = new ArrayList<>();
        ArrayList<BusStops> busStops = getAllBusStops(context, service_no, direction);

        /* Compute midpoints for all the bus stops */
        ArrayList<Midpoint> midpoints = new ArrayList<>();
        for (int index=start; index< busStops.size()-1; index++) {

            double Point1Lat = busStops.get(index).getLatitude();
            double Point1Lon = busStops.get(index).getLongitude();
            double Point2Lat = busStops.get(index+1).getLatitude();
            double Point2Lon = busStops.get(index+1).getLongitude();

            Location Point1 = createLocation(Point1Lat, Point1Lon);
            Location Point2 = createLocation(Point2Lat, Point2Lon);

            Location midLocation = getMidPoint(Point1Lat, Point1Lon, Point2Lat, Point2Lon);
            float geofenceRadius = Point1.distanceTo(Point2) / 2;
            Midpoint aMidPoint = new Midpoint();
            aMidPoint.setMidGeoPoint(midLocation);
            aMidPoint.setDistanceInMeter(geofenceRadius);
            aMidPoint.setBusStopNo(busStops.get(index+1).getBusStopNo());
            aMidPoint.setBusStopName(busStops.get(index+1).getBusStopName());
            midpoints.add(aMidPoint);
        }


        // set geofences into the intent service
        for (int index = 0; index < midpoints.size(); index++) {
            Midpoint aMidPoint = midpoints.get(index);

            mGeofenceList.add(new Geofence.Builder()
                    .setRequestId(index + ":" + aMidPoint.getBusStopNo() + ":" + aMidPoint.getBusStopName())
                    .setCircularRegion(
                            aMidPoint.getMidGeoPoint().getLatitude(),
                            aMidPoint.getMidGeoPoint().getLongitude(),
                            aMidPoint.getDistanceInMeter()
                    )
                    .setExpirationDuration(Constants.GEOFENCE_EXPIRATION_IN_MILLISECONDS)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                    .build());
        }

        return mGeofenceList;
    }
}
