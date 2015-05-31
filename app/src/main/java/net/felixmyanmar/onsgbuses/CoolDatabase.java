package net.felixmyanmar.onsgbuses;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

import java.util.ArrayList;

/**
 * Using SQLiteAssetHelper, the database is going to copy the database from the asset folder.
 * The rest of the stuffs are the same as normal SQLiteOpenHelper class in Android.
 *
 * SQLiteAssetHelper is intended as a drop in alternative for the framework's SQLiteOpenHelper.
 * Please familiarize yourself with the behaviour and lifecycle of that class.
 *
 * https://github.com/jgilfelt/android-sqlite-asset-helper
 *
 *  dependencies {
 *      compile 'com.readystatesoftware.sqliteasset:sqliteassethelper:+'
 *  }
 *
 *
 * Minimum Requirement:
 * - A databases folder inside assets
 * - A SQLite database inside the databases folder whose file name matches the database name
 */
public class CoolDatabase extends SQLiteAssetHelper {

    private static final String DATABASE_NAME = "travel.sqlite";
    private static final int DATABASE_VERSION = 1;

    public CoolDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }



    public ArrayList<String> getAllBusServices() {

        ArrayList<String> bus_services = new ArrayList<>();
        SQLiteDatabase coolDb = getReadableDatabase();

        String sqlStr = "SELECT DISTINCT service_id FROM bus_route ORDER BY service_id*1, service_id";
        Cursor cursor = coolDb.rawQuery(sqlStr, null);

        if (cursor.moveToFirst()) {
            do {
                bus_services.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }

        cursor.close();
        coolDb.close();

        return bus_services;
    }


    public ArrayList<BusRoute> getBusRoute(String service_no) {
        ArrayList<BusRoute> bus_route = new ArrayList<>();
        SQLiteDatabase coolDb = getReadableDatabase();

        String sqlStr = "SELECT * FROM bus_route WHERE service_id='"+ service_no+ "'";
        Cursor cursor = coolDb.rawQuery(sqlStr, null);

        if (cursor.moveToFirst()) {
            do {
                BusRoute aRoute = new BusRoute();
                aRoute.setService_id(cursor.getString(1));
                aRoute.setDirection(cursor.getInt(2));
                aRoute.setSequence(cursor.getInt(3));
                aRoute.setRoute_segment_id(cursor.getInt(4));

                bus_route.add(aRoute);
            } while (cursor.moveToNext());
        }

        cursor.close();
        coolDb.close();

        return bus_route;
    }


    /**
     * Get bus stops along the route; One annoying problem is the route_segment.
     * Each route segments contains point A and point B. Problem came when we need to
     * retireve two columns into one. SQL become so complicated, so I decided to break
     * it down by getEndStop boolean value.
     *
     * getEndStop = false; take only start_stop from route_segment
     * true: take only end_stop from route_segment
     *
     * @param service_no  - all bus service no.. can be string (eg. NR1)
     * @param direction - either 1 or 2. Loop bus has only 1.
     * @param getEndStop - to simply the SQL statement.
     * @return list of bus stops containing the sequence, bus stop name & no, geo coordinates
     */
    public ArrayList<BusStops> getBusStops(String service_no, int direction, boolean getEndStop) {

        ArrayList<BusStops> bus_stops = new ArrayList<>();
        SQLiteDatabase coolDb = getReadableDatabase();

        String stopToUse = "start_stop";
        if (getEndStop) stopToUse = "end_stop";

        String sqlStr = "SELECT sequence, " + stopToUse + ", name, latitude, longitude FROM bus_route br " +
                "JOIN route_segments rs ON br.route_segment_id = rs._id " +
                "JOIN bus_stops bs ON bs._id = rs."+stopToUse+ " " +
                "WHERE service_id='"+ service_no+ "' " +
                "AND direction='" + direction +"'";
        Cursor cursor = coolDb.rawQuery(sqlStr, null);

        if (cursor.moveToFirst()) {
            do {
                BusStops aBusStop = new BusStops();
                aBusStop.setSequence(cursor.getInt(0));
                aBusStop.setBusStopNo(cursor.getInt(1));
                aBusStop.setBusStopName(cursor.getString(2));
                aBusStop.setLatitude(cursor.getFloat(3));
                aBusStop.setLongitude(cursor.getFloat(4));
                aBusStop.setLed_status(0);

                bus_stops.add(aBusStop);
            } while (cursor.moveToNext());
        }

        cursor.close();
        coolDb.close();

        return bus_stops;
    }
}
