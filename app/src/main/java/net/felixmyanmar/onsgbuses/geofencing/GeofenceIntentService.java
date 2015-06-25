package net.felixmyanmar.onsgbuses.geofencing;

import android.app.IntentService;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import net.felixmyanmar.onsgbuses.helper.NotificationHelper;
import net.felixmyanmar.onsgbuses.app.OnTheRoadActivity;
import net.felixmyanmar.onsgbuses.R;
import net.felixmyanmar.onsgbuses.helper.SharedPreferenceHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Listener for geofence transition changes.
 * <p/>
 * Receives geofence transition events from Location Services in the form of an Intent containing
 * the transition type and geofence id(s) that triggered the transition. Creates a notification
 * as the output.
 */
public class GeofenceIntentService extends IntentService {

    protected static final String TAG = "on-geofence-intent";

    // last_found is to help determine the direction of the bus going
    private int last_found = -1;

    // isLockDir is the flag to guide you to find out the noise of gps and predict coming bus stop
    private boolean isLockedDir = false;

    // busStops holds all the bus stops along a direction
    private ArrayList<String> busStops;


    private boolean isActivelyRunning = false;


    public GeofenceIntentService() {
        // Use the TAG to name the worker thread.
        super(TAG);
    }

    /**
     * Handles incoming intents.
     *
     * @param intent sent by Location Services. This Intent is provided to Location
     *               Services (inside a PendingIntent) when addGeofences() is called.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        ArrayList<Integer> selectedIds;

        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    geofencingEvent.getErrorCode());
            Log.e(TAG, errorMessage);
            return;
        }

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {

            // Get the latest shared preference value
            last_found = SharedPreferenceHelper.getSharedIntPref(this, "last_found", -1);
            isLockedDir = SharedPreferenceHelper.getSharedBooleanPref(this, "isLockedDir");
            busStops = SharedPreferenceHelper.getStringArrayPref(this, "busStops");
            isActivelyRunning = SharedPreferenceHelper.getSharedBooleanPref(this, "isActivityForeground");

            // Get the geofences that were triggered. A single event can trigger multiple geofences.
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            // Get the transition details as a String.
            String geofenceTransitionDetails = getGeofenceTransitionDetails(triggeringGeofences);

            String[] args = findBusStopFromRecord(geofenceTransitionDetails);
            String currentStop = args[0];
            String currentBusName = args[1];

            // Store the busStop into SharedPreference so that when the route is shown,
            // it will automatically scroll to there.
            if (!currentStop.isEmpty()) SharedPreferenceHelper.setSharedStringPref(this, "busStop", currentStop);

            // Get the index from busStops array
            int found = busStops.indexOf(currentStop+":"+currentBusName);
            if (found > last_found) SharedPreferenceHelper.setSharedIntPref(this, "last_found", found);

            tryLockDirection(found);

            if (found > last_found && isLockedDir) {
                // send notification without the sound and vibration
                NotificationHelper.showNotification(this, "Next Stop: " + currentBusName, false);
            }

            // this gives you a list of all the bus stops that needs to trigger the notification
            selectedIds = SharedPreferenceHelper.getIntegerArrayPref(this, "selectedIds");

            for (int i = 0; i < selectedIds.size(); i++) {
                if (currentStop.equals(selectedIds.get(i) + "")) {
                    if (!isActivelyRunning)
                        NotificationHelper.showNotification(this, "Next Stop: " + currentBusName, true);
                    break;
                }
            }

            // send broadcast only when the activity is on foreground
            if (isActivelyRunning) {
                // Broadcast receiver to send back the info to Activity
                Intent broadcastIntent = new Intent();
                broadcastIntent.setAction(OnTheRoadActivity.MyBroadcastReceiver.RESPONSE);
                broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
                broadcastIntent.putExtra("Details", geofenceTransitionDetails);
                sendBroadcast(broadcastIntent);
            }

        } else {
            // Log the error.
            Log.e(TAG, getString(R.string.geofence_transition_invalid_type, geofenceTransition));
        }
    }


    private String[] findBusStopFromRecord(String aRecord) {

        // Sometimes, you can get two locations like
        // 18:42149:Aft King Albert Pk, 13:42071:Shell Kiosk
        String busStop = "", busName="";
        StringTokenizer all = new StringTokenizer(aRecord, ",");
        ArrayList<String> results = new ArrayList<>();
        while (all.hasMoreTokens())
            results.add(all.nextToken().trim());

        // sometimes, we have two or more geofences triggered at same time.
        // you need to sort based on the arraylist index for accurate result
        Collections.sort(results);

        for (int index = 0; index < results.size(); index++) {
            String busDetails = results.get(index);

            StringTokenizer stk = new StringTokenizer(busDetails, ":");
            ArrayList<String> tokens = new ArrayList<>();
            while (stk.hasMoreTokens())
                tokens.add(stk.nextToken().trim());

            busStop = tokens.get(1);
            busName = tokens.get(2);

            int found = busStops.indexOf(busStop+":"+busName);
            if (found > last_found) break;
        }

        return new String[]{ busStop, busName};
    }

    private void tryLockDirection(int found) {
        if (!isLockedDir) {
            // Lock the direction to true so that, it won't skip the stop a lot.
            isLockedDir = (found - last_found == 1);
            SharedPreferenceHelper.setSharedBooleanPref(this, "isLockedDir", isLockedDir);
        }
    }



    /**
     * Gets transition details and returns them as a formatted string.
     *
     * @param triggeringGeofences The geofence(s) triggered.
     * @return The transition details formatted as String.
     */
    private String getGeofenceTransitionDetails(List<Geofence> triggeringGeofences) {

        // Get the Ids of each geofence that was triggered.
        ArrayList triggeringGeofencesIdsList = new ArrayList();
        for (Geofence geofence : triggeringGeofences) {
            triggeringGeofencesIdsList.add(geofence.getRequestId());
        }
        String triggeringGeofencesIdsString = TextUtils.join(", ", triggeringGeofencesIdsList);

        return triggeringGeofencesIdsString;
    }

}
