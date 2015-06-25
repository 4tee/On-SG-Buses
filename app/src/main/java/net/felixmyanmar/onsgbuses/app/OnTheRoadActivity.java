package net.felixmyanmar.onsgbuses.app;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import net.felixmyanmar.onsgbuses.R;
import net.felixmyanmar.onsgbuses.container.BusStops;
import net.felixmyanmar.onsgbuses.database.CoolDatabase;
import net.felixmyanmar.onsgbuses.geofencing.Constants;
import net.felixmyanmar.onsgbuses.geofencing.GeofenceErrorMessages;
import net.felixmyanmar.onsgbuses.geofencing.GeofenceIntentService;
import net.felixmyanmar.onsgbuses.container.Midpoint;
import net.felixmyanmar.onsgbuses.helper.SharedPreferenceHelper;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringTokenizer;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class OnTheRoadActivity extends AppCompatActivity implements
        SearchView.OnQueryTextListener,
        ConnectionCallbacks,
        OnConnectionFailedListener,
        ResultCallback<Status>,
        LocationListener {

    // UI items
    @InjectView(R.id.cool_recycler_view) RecyclerView recyclerView;
    @InjectView(R.id.add_geofences_button) Button mAddGeofencesButton;
    @InjectView(R.id.remove_geofences_button) Button mRemoveGeofencesButton;
    @InjectView(R.id.toolbar) Toolbar toolBar;
    @InjectView(R.id.lbl_title) TextView titleLabel;

    protected static final String TAG = "on-the-road";

    // The list of all bus stops in the direction
    private ArrayList<BusStops> busStops;

    // AdapterView for RecyclerView
    private BusRVAdapter mAdapter;

    // Listener for IntentService
    private MyBroadcastReceiver receiver;

    // Provides the entry point to Google Play services.
    protected GoogleApiClient mGoogleApiClient;

    // The list of geofences.
    protected ArrayList<Geofence> mGeofenceList;

    // Used to keep track of whether geofences were added.
    private boolean mGeofencesAdded;

    // Used when requesting to add or remove geofences.
    private PendingIntent mGeofencePendingIntent;

    // Used to persist application state about whether geofences were added.
    //private SharedPreferences mSharedPreferences;




    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (TextUtils.isEmpty(newText)) {
            mAdapter.getFilter().filter("");
        } else {
            mAdapter.getFilter().filter(newText);
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.control_menu, menu);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
            searchView.setQueryHint("Search Bus Stop No");
            searchView.setOnQueryTextListener(this);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.search:
                onSearchRequested();
                return true;
            case R.id.refresh:
                startUpdatesHandler();
                addGeofences();
                Toast.makeText(this,"Please wait. Searching for location",Toast.LENGTH_SHORT).show();
                return true;
            case R.id.stop:
                stopUpdatesHandler();
                removeGeofences();
                Toast.makeText(this,"System stopped.",Toast.LENGTH_SHORT).show();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ontheroad);
        ButterKnife.inject(this);

        setTitle("");
        setSupportActionBar(toolBar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        // get the data sent from previous activity using intent
        Intent touchIntent = getIntent();
        String service_no = touchIntent.getStringExtra("service_id");
        int direction = touchIntent.getIntExtra("direction", 1);



//        // Retrieve an instance of the SharedPreferences object.
//        mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME,
//                MODE_PRIVATE);

        // Get the value of mGeofencesAdded from SharedPreferences. Set to false as a default.
//        mGeofencesAdded = mSharedPreferences.getBoolean(Constants.GEOFENCES_ADDED_KEY, false);
//        setButtonsEnabledState();

        if (service_no != null) {
            SharedPreferenceHelper.setSharedStringPref(this, "service_id", service_no);
            SharedPreferenceHelper.setSharedIntPref(this, "direction", direction);
        } else {
            service_no = SharedPreferenceHelper.getSharedStringPref(this,"service_id","BPS1");
            direction = SharedPreferenceHelper.getSharedIntPref(this,"direction",1);
        }

        // List of bus stops along the direction
        CoolDatabase db = new CoolDatabase(this);
        ArrayList<BusStops> routeStartStops = db.getBusStops(service_no, direction, false);
        ArrayList<BusStops> routeEndStops = db.getBusStops(service_no, direction, true);

        // Combine and find all unique bus stops
        Set<BusStops> container = new LinkedHashSet<>(routeStartStops);
        container.addAll(routeEndStops);
        busStops = new ArrayList<>(container);

        // Display those bus stops in the recycler view
        mAdapter = new BusRVAdapter(this, busStops);
        recyclerView.setAdapter(mAdapter);

        // Empty list for storing geofences.
        mGeofenceList = new ArrayList<>();

        // Initially set the PendingIntent used in addGeofences() and removeGeofences() to null.
        mGeofencePendingIntent = null;

        // LocationUpdates
        mRequestingLocationUpdates = false;
        mLastUpdateTime = "";

        // Get the geofences used. Geofence data is hard coded in this sample.
        populateGeofenceList(0);

        // Update values using data stored in the Bundle.
        updateValuesFromBundle(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();

        String notifListen = getIntent().getAction();
        if (notifListen!=null && notifListen.equals("exit")) {
            Toast.makeText(this, "Stopping", Toast.LENGTH_SHORT).show();
        }


        // get sharedPreference value
        last_found = SharedPreferenceHelper.getSharedIntPref(this,"last_found",-1);
        isLockedDir = SharedPreferenceHelper.getSharedBooleanPref(this, "isLockedDir");
        busStop = SharedPreferenceHelper.getSharedStringPref(this, "busStop", "");
        SharedPreferenceHelper.setSharedBooleanPref(this,"isActivityForeground",true);

        // you can set the bus service number as window title
        //setTitle(SharedPreferenceHelper.getSharedStringPref(this, "service_id","BPS1"));
        titleLabel.setText(SharedPreferenceHelper.getSharedStringPref(this, "service_id","BPS1"));

        // kick off the request to build GoogleApiClient.
        buildGoogleApiClient();

        // connect the mGoogleApiClient
        mGoogleApiClient.connect();

        IntentFilter filter = new IntentFilter(MyBroadcastReceiver.RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        receiver = new MyBroadcastReceiver();
        registerReceiver(receiver, filter);

        if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
            startLocationUpdates();
        }

        //addGeofencesButtonHandler(this.getCurrentFocus());

//        // Reload sharepreference
//        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
//        last_found = prefs.getInt("last_found", -1);
//        isLockedDir = prefs.getBoolean("isLockedDir", false);
//        busStop = prefs.getString("busStop", "");

//        // update the perference that activity is now active.
//        prefs.edit().putBoolean("isActivityForeground", true).apply();

        // update the adapter to reflect based on geofence intent service
        int foundIndex = -1;
        if (!busStop.isEmpty()) foundIndex = findIndexOfBusStopsArray(busStop);
        if (foundIndex != -1) updateRecyclerView(foundIndex);

        mGeoListTextView.append("\nReload pref: last_found:" + last_found + " isLockedDir:" + isLockedDir);
        mGeoListTextView.setMovementMethod(new ScrollingMovementMethod());
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause invoked");
//        // Stop location updates to save battery, but don't disconnect the GoogleApiClient object.
//        if (mGoogleApiClient.isConnected()) {
//            stopLocationUpdates();
//        }

        ArrayList<String> arraySet = new ArrayList<>();
        for (int index=0; index<busStops.size(); index++) {
            arraySet.add(busStops.get(index).getBusStopNo()+":"+busStops.get(index).getBusStopName());
        }
        SharedPreferenceHelper.setStringArrayPref(this, "busStops", arraySet);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("last_found", last_found);
        editor.putBoolean("isLockedDir", isLockedDir);
        editor.putBoolean("isActivityForeground", false);
        editor.apply();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //if (mGoogleApiClient!=null) mGoogleApiClient.disconnect();
        try {
            this.unregisterReceiver(receiver);
        } catch (IllegalArgumentException iae) {
            //Nothing
        }
    }


    /**
     * Take care of popping the fragment back stack or finishing the activity
     * as appropriate.
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log.d(TAG, "onBackPressed");
        stopUpdatesHandler();
        removeGeofences();

        if (mGoogleApiClient!=null) mGoogleApiClient.disconnect();

    }

    @Override
    public void onConnected(Bundle bundle) {

        if (mCurrentLocation == null) {
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
            updateUI();

            addGeofences();
            startUpdatesHandler();
        }

        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(this, TAG + ": Connection suspended", Toast.LENGTH_SHORT).show();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(this, TAG + ": Connection error - " + connectionResult.getErrorCode(),
                Toast.LENGTH_SHORT).show();
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
    private Location getMidPoint(double lat1, double lon1, double lat2, double lon2) {

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

    /**
     * Using latitude and longtiude, you can create Location object in Java.
     * Provider name is unnecessary though.
     *
     * @param lat latitude of point
     * @param lon longitude of point
     * @return Location object
     */
    private Location createLocation(double lat, double lon) {
        Location aLocation = new Location(""); //provider name is unnecessary
        aLocation.setLatitude(lat);
        aLocation.setLongitude(lon);

        return aLocation;
    }

    /**
     * This sample hard codes geofence data. A real app might dynamically create geofences based on
     * the user's location.
     */
    public void populateGeofenceList(int start) {

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

    }

    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        // LocationUpdates
        createLocationRequest();
    }


    /**
     * Builds and returns a GeofencingRequest. Specifies the list of geofences to be monitored.
     * Also specifies how the geofence notifications are initially triggered.
     */
    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

        // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
        // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
        // is already inside that geofence.
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);

        // Add the geofences to be monitored by geofencing service.
        builder.addGeofences(mGeofenceList);

        // Return a GeofencingRequest.
        return builder.build();
    }


    public void addGeofencesButtonHandler(View view) {
        addGeofences();
    }

    /**
     * Adds geofences, which sets alerts to be notified when the device enters or exits one of the
     * specified geofences. Handles the success or failure results returned by addGeofences().
     */
    private void addGeofences() {
        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(this, "add "+getString(R.string.not_connected), Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            LocationServices.GeofencingApi.addGeofences(
                    mGoogleApiClient,
                    // The GeofenceRequest object.
                    getGeofencingRequest(),
                    // A pending intent that that is reused when calling removeGeofences(). This
                    // pending intent is used to generate an intent when a matched geofence
                    // transition is observed.
                    getGeofencePendingIntent()
            ).setResultCallback(this); // Result processed in onResult().


        } catch (SecurityException securityException) {
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
            logSecurityException(securityException);
        }
    }


    public void removeGeofencesButtonHandler(View view) {
        removeGeofences();
    }


    private void removeGeofences() {
        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(this, "remove " + getString(R.string.not_connected), Toast.LENGTH_SHORT).show();
            return;
        }
        try {

            // Remove geofences.
            LocationServices.GeofencingApi.removeGeofences(
                    mGoogleApiClient,
                    // This is the same pending intent that was used in addGeofences().
                    getGeofencePendingIntent()
            ).setResultCallback(this); // Result processed in onResult().
        } catch (SecurityException securityException) {
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
            logSecurityException(securityException);
        }
    }

    private void logSecurityException(SecurityException securityException) {
        Log.e(TAG, "You need to use GPS.", securityException);
    }

    /**
     * Runs when the result of calling addGeofences() and removeGeofences() becomes available.
     * Either method can complete successfully or with an error.
     *
     * Since this activity implements the {@link ResultCallback} interface, we are required to
     * define this method.
     *
     * @param status The Status returned through a PendingIntent when addGeofences() or
     *               removeGeofences() get called.
     */
    public void onResult(Status status) {
        if (status.isSuccess()) {
            // Update state and save in shared preferences.
            mGeofencesAdded = !mGeofencesAdded;
//            SharedPreferences.Editor editor = mSharedPreferences.edit();
//            editor.putBoolean(Constants.GEOFENCES_ADDED_KEY, mGeofencesAdded);
//            editor.apply();
            SharedPreferenceHelper.setSharedBooleanPref(this,Constants.GEOFENCES_ADDED_KEY, mGeofencesAdded);

            // Update the UI. Adding geofences enables the Remove Geofences button, and removing
            // geofences enables the Add Geofences button.
            setButtonsEnabledState();

            Toast.makeText(
                    this,
                    getString(mGeofencesAdded ? R.string.geofences_added :
                            R.string.geofences_removed),
                    Toast.LENGTH_SHORT
            ).show();
        } else {
            // Get the status code for the error and log it using a user-friendly message.
            String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    status.getStatusCode());
            Log.e(TAG, errorMessage);
        }
    }

    /**
     * Ensures that only one button is enabled at any time. The Add Geofences button is enabled
     * if the user hasn't yet added geofences. The Remove Geofences button is enabled if the
     * user has added geofences.
     */
    private void setButtonsEnabledState() {
        if (mGeofencesAdded) {
            mAddGeofencesButton.setEnabled(false);
            mRemoveGeofencesButton.setEnabled(true);
        } else {
            mAddGeofencesButton.setEnabled(true);
            mRemoveGeofencesButton.setEnabled(false);
        }
    }


    /**
     * Gets a PendingIntent to send with the request to add or remove Geofences. Location Services
     * issues the Intent inside this PendingIntent whenever a geofence transition occurs for the
     * current list of geofences.
     *
     * @return A PendingIntent for the IntentService that handles geofence transitions.
     */
    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceIntentService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }



    int last_found = -1;

    // isLockedDir will be true when diference between found and last_found is 1
    boolean isLockedDir = false;
    String busStop = "";


    /**
     * Update the recycler view based on found index.
     * @param found_index an integer to indicate the current stop
     */
    private void updateRecyclerView(int found_index) {
        // Move the listview to the center
        Display display = getWindowManager().getDefaultDisplay();
        int some_space;
        if (Build.VERSION.SDK_INT >= 13) {
            Point point = new Point();
            display.getSize(point);
            some_space = point.y / 3;
        } else {
            // deprecated, but it is for before API 13.
            some_space = display.getHeight() / 3;
        }

        // reset all the LEDs
        for (int i = 0; i < busStops.size(); i++) busStops.get(i).setLed_status(0);

        // set current bus stop
        busStops.get(found_index).setLed_status(1);

        // set pending bus stops
        if (found_index != busStops.size() - 1) {
            for (int i = found_index + 1; i < busStops.size(); i++)
                busStops.get(i).setLed_status(2);
        }

        mGeoListTextView.append("updateRecyclerView:" + found_index);
        // update the data adapter
        ((LinearLayoutManager) recyclerView.getLayoutManager()).scrollToPositionWithOffset(found_index, some_space);
        mAdapter.notifyDataSetChanged();
    }

    private int findIndexOfBusStopsArray(String searchBusStop) {
        int foundIndex = -1;
        for (int i = 0; i < busStops.size(); i++) {
            if (searchBusStop.equals(busStops.get(i).getBusStopNo() + "")) {
                mGeoListTextView.append("\nfound at " + i);
                foundIndex = i;
                break;
            }
        }
        return foundIndex;
    }

    /**
     * Broadcast receiver allows for the activity to listen for the broadcast from service
     * Since geofence is triggered by intentservice, you have to listen to this signal and
     * refresh your recyclerview with it.
     */
    public class MyBroadcastReceiver extends BroadcastReceiver {
        public static final String RESPONSE = "net.felixmyanmar.onsgbuses.intent.action.RESPONSE";

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive invoked");
            mGeoListTextView.append("\n"+intent.getStringExtra("Details") + " isLocked:" + isLockedDir);

            int found = -1;

            // Get all possible locations sent by the broadcaster
            StringTokenizer all = new StringTokenizer(intent.getStringExtra("Details"), ",");
            ArrayList<String> results = new ArrayList<>();
            while (all.hasMoreTokens()) {
                results.add(all.nextToken().trim());
            }

            Collections.sort(results);

            for (int index = 0; index < results.size(); index++) {
                String busDetails = results.get(index);

                StringTokenizer stk = new StringTokenizer(busDetails, ":");
                ArrayList<String> tokens = new ArrayList<>();
                while (stk.hasMoreTokens()) {
                    tokens.add(stk.nextToken().trim());
                }

                busStop = tokens.get(1);

                // Sometimes, you can get two locations like
                // 10:42149:Aft King Albert Pk, 18:42071:Shell Kiosk
//                if (tokens.size()>2) {
//                    busStop = tokens.get(1);
//                } else {
//                    busStop = tokens.get(0);
//                }

                // find the bus Stop based on bustStop from stringtokenizer
                mGeoListTextView.append("\n" + index + " look for busStop: " + busStop);
                found = findIndexOfBusStopsArray(busStop);
//                for (int i = 0; i < busStops.size(); i++) {
//                    if (busStop.equals(busStops.get(i).getBusStopNo() + "")) {
//                        mGeoListTextView.append("\nfound at " + i);
//                        found = i;
//                        break;
//                    }
//                }

                if (found > last_found) break;
                else mGeoListTextView.append("\nfound:" + found + " VS last_found:" + last_found);
            }


            // reset all the LEDs
            for (int i = 0; i < busStops.size(); i++) busStops.get(i).setLed_status(0);

            if (found != -1 && found > last_found ) {

                // Move the listview to the center
                Display display = getWindowManager().getDefaultDisplay();
                int some_space;
                if (Build.VERSION.SDK_INT >= 13) {
                    Point point = new Point();
                    display.getSize(point);
                    some_space = point.y / 3;
                } else {
                    // deprecated, but it is for before API 13.
                    some_space = display.getHeight() / 3;
                }

                if (!isLockedDir) {
                    mGeoListTextView.append("\nbefore Locking - found:" + found + " VS last_found:" + last_found);

                    // Do action
                    busStops.get(found).setLed_status(1);

                    if (found != busStops.size() - 1) {
                        for (int i = found + 1; i < busStops.size(); i++)
                            busStops.get(i).setLed_status(2);
                    }

                    // Lock the direction to true so that, it won't skip the stop a lot.
                    isLockedDir = (found - last_found == 1);

                    last_found = found;

                    mGeoListTextView.append("\ngo: " + last_found);
                    ((LinearLayoutManager) recyclerView.getLayoutManager()).scrollToPositionWithOffset(found, some_space);
                    mAdapter.notifyDataSetChanged();
                } else {

                    mGeoListTextView.append("\nafter Locking - found:" + found + " VS last_found:" + last_found);
                    // Once the direction is locked, the next bus stop index cannot exceed more than 1
                    if (found-last_found==1) {
                        mGeoListTextView.append("\nafter Locking - go");

                        busStops.get(found).setLed_status(1);
                        if (found != busStops.size() - 1) {
                            for (int i = found + 1; i < busStops.size(); i++)
                                busStops.get(i).setLed_status(2);
                        }
                        last_found = found;

                        mGeoListTextView.append("\ngo: " + last_found);
                        ((LinearLayoutManager) recyclerView.getLayoutManager()).scrollToPositionWithOffset(found, some_space);
                        mAdapter.notifyDataSetChanged();

                    } else {
                        mGeoListTextView.append("\nafter Locking - NO go");
                    /*invalid jump*/ }
                }
            }
            else {
                mGeoListTextView.append("\nSkip it because found:" + found + " < lastfound:" +last_found + " " + isLockedDir);
            }
        }
    }


    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    protected LocationRequest mLocationRequest;

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 5000;

    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    // Keys for storing activity state in the Bundle.
    protected final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    protected final static String LOCATION_KEY = "location-key";
    protected final static String LAST_UPDATED_TIME_STRING_KEY = "last-updated-time-string-key";

    /**
     * Represents a geographical location.
     */
    protected Location mCurrentLocation;

    /**
     * Tracks the status of the location updates request. Value changes when the user presses the
     * Start Updates and Stop Updates buttons.
     */
    protected Boolean mRequestingLocationUpdates;

    /**
     * Time when the location was updated represented as a String.
     */
    protected String mLastUpdateTime;

    // UI Widgets.
    @InjectView(R.id.toggleButton) ToggleButton mToggleButton;
    @InjectView(R.id.latitude_text) TextView mLatitudeTextView;
    @InjectView(R.id.longitude_text) TextView mLongitudeTextView;
    @InjectView(R.id.last_update_time_text) TextView mLastUpdateTimeTextView;
    @InjectView(R.id.geolist_text) TextView mGeoListTextView;
    @InjectView(R.id.start_updates_button) Button mStartUpdatesButton;
    @InjectView(R.id.stop_updates_button) Button mStopUpdatesButton;


//    @OnClick(R.id.toggleButton) void onClick() {
//        if (!mToggleButton.isChecked()) {
//            mGeoListTextView.setVisibility(View.VISIBLE);
//        }
//        else {
//            mGeoListTextView.setVisibility(View.GONE);
//        }
//    }


    /**
     * Sets up the location request. Android has two location request settings:
     * {@code ACCESS_COARSE_LOCATION} and {@code ACCESS_FINE_LOCATION}. These settings control
     * the accuracy of the current location. This sample uses ACCESS_FINE_LOCATION, as defined in
     * the AndroidManifest.xml.
     * <p/>
     * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
     * interval (5 seconds), the Fused Location Provider API returns location updates that are
     * accurate to within a few feet.
     * <p/>
     * These settings are appropriate for mapping applications that show real-time location
     * updates.
     */
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Handles the Start Updates button and requests start of location updates. Does nothing if
     * updates have already been requested.
     */
    private void startUpdatesHandler() {
        if (!mRequestingLocationUpdates) {
            mRequestingLocationUpdates = true;
            //setLocationButtonsEnabledState();
            startLocationUpdates();
        }
    }

    /**
     * Handles the Stop Updates button, and requests removal of location updates. Does nothing if
     * updates were not previously requested.
     */
    private void stopUpdatesHandler() {
        if (mRequestingLocationUpdates) {
            mRequestingLocationUpdates = false;
            //setLocationButtonsEnabledState();
            stopLocationUpdates();
        }
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    protected void stopLocationUpdates() {
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.

        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    /**
     * Requests location updates from the FusedLocationApi.
     */
    protected void startLocationUpdates() {
        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    /**
     * Callback that fires when the location changes.
     */
    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged invoked");
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        updateUI();
    }






    /**
     * Ensures that only one button is enabled at any time. The Start Updates button is enabled
     * if the user is not requesting location updates. The Stop Updates button is enabled if the
     * user is requesting location updates.
     */
    private void setLocationButtonsEnabledState() {
        if (mRequestingLocationUpdates) {
            mStartUpdatesButton.setEnabled(false);
            mStopUpdatesButton.setEnabled(true);
        } else {
            mStartUpdatesButton.setEnabled(true);
            mStopUpdatesButton.setEnabled(false);
        }
    }

    /**
     * Updates the latitude, the longitude, and the last location time in the UI.
     */
    private void updateUI() {
        if (mCurrentLocation != null) {
            mLatitudeTextView.setText(String.valueOf(mCurrentLocation.getLatitude()));
            mLongitudeTextView.setText(String.valueOf(mCurrentLocation.getLongitude()));
            mLastUpdateTimeTextView.setText(mLastUpdateTime);
        }
    }

    /**
     * Updates fields based on data stored in the bundle.
     *
     * @param savedInstanceState The activity state saved in the Bundle.
     */
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Update the value of mRequestingLocationUpdates from the Bundle, and make sure that
            // the Start Updates and Stop Updates buttons are correctly enabled or disabled.
            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        REQUESTING_LOCATION_UPDATES_KEY);
                //setLocationButtonsEnabledState();
            }

            // Update the value of mCurrentLocation from the Bundle and update the UI to show the
            // correct latitude and longitude.
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                // Since LOCATION_KEY was found in the Bundle, we can be sure that mCurrentLocation
                // is not null.
                mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }

            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                mLastUpdateTime = savedInstanceState.getString(LAST_UPDATED_TIME_STRING_KEY);
            }
            updateUI();
        }
    }

    /**
     * Stores activity data in the Bundle.
     */
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, mRequestingLocationUpdates);
        savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);
        savedInstanceState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        super.onSaveInstanceState(savedInstanceState);
    }
}
