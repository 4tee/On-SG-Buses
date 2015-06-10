package net.felixmyanmar.onsgbuses.geofencing;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import net.felixmyanmar.onsgbuses.MainActivity;
import net.felixmyanmar.onsgbuses.OnTheRoadActivity;
import net.felixmyanmar.onsgbuses.R;
import net.felixmyanmar.onsgbuses.SharedPreferenceHelper;

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

    /**
     * This constructor is required, and calls the super IntentService(String)
     * constructor with the name for a worker thread.
     */
    public GeofenceIntentService() {
        // Use the TAG to name the worker thread.
        super(TAG);
    }


    /**
     * Set current bus stop with the key and its value
     *
     * @param context sent by Location Service
     * @param key     key to hold sharedprefence
     * @param value   value to assign to key
     */
    private static void setSharedPref(Context context, String key, String value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, value);
        editor.apply();
    }


    /**
     * Find out if the activity is running at foreground
     *
     * @param context sent by Location Service
     * @return true if it is foreground
     */
    private boolean isActivityForeground(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("isActivityForeground", false);
    }


    int last_found = -1;
    boolean isLockedDir = false;
    ArrayList<String> busStops;

    private void getSharedPerference() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        last_found = prefs.getInt("last_found", -1);
        isLockedDir = prefs.getBoolean("isLockedDir", false);
        busStops = SharedPreferenceHelper.getStringArrayPref(this, "busStops");
    }

    private int findIndexOfBusStopsArray(String searchBusStop) {
        int foundIndex = -1;
        for (int i = 0; i < busStops.size(); i++) {
            if (busStops.get(i).contains(searchBusStop)) {
                foundIndex = i;
                break;
            }
        }
        return foundIndex;
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


            if (tokens.size() > 2) {
                busStop = tokens.get(1);
                busName = tokens.get(2);
            } else {
                busStop = tokens.get(0);
                busName = tokens.get(1);
            }
            int found = findIndexOfBusStopsArray(busStop);
            if (found > last_found) break;
        }

        return new String[]{ busStop, busName};
    }

    private void tryLockDirection(int found) {
        if (!isLockedDir) {
            // Lock the direction to true so that, it won't skip the stop a lot.
            isLockedDir = (found - last_found == 1);
            PreferenceManager.getDefaultSharedPreferences(this).edit().
                    putBoolean("isLockedDir", isLockedDir).apply();
        }
    }

    /**
     * Handles incoming intents.
     *
     * @param intent sent by Location Services. This Intent is provided to Location
     *               Services (inside a PendingIntent) when addGeofences() is called.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    geofencingEvent.getErrorCode());
            Log.e(TAG, errorMessage);
            return;
        }

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        ArrayList<Integer> selectedIds;

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {

            // Get the latest sharedperference
            getSharedPerference();

            // Get the geofences that were triggered. A single event can trigger multiple geofences.
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            // Get the transition details as a String.
            String geofenceTransitionDetails = getGeofenceTransitionDetails(
                    this,
                    geofenceTransition,
                    triggeringGeofences
            );

            String[] args = findBusStopFromRecord(geofenceTransitionDetails);
            String currentStop = args[0];
            String currentBusName = args[1];
            Log.d(TAG, "currentStop: " + currentStop);
            // Store the busStop into SharedPreference so that when the route is shown,
            // it will automatically scroll to there.
            if (!currentStop.isEmpty()) setSharedPref(this, "busStop", currentStop);
            int found = findIndexOfBusStopsArray(currentStop);

            tryLockDirection(found);

            if (found > last_found && isLockedDir) {
                sendContNotification("Next Stop: " + currentBusName);
            }

            // this gives you a list of all the bus stops that needs to trigger the notification
            selectedIds = SharedPreferenceHelper.getIntegerArrayPref(this, "selectedIds");

            for (int i = 0; i < selectedIds.size(); i++) {
                if (currentStop.equals(selectedIds.get(i) + "")) {
                    sendNotification("Next Stop: " + currentBusName);
                    break;
                }
            }

            // send broadcast only when the activity is on foreground
            if (isActivityForeground(this)) {
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

    /**
     * Gets transition details and returns them as a formatted string.
     *
     * @param context             The app context.
     * @param geofenceTransition  The ID of the geofence transition.
     * @param triggeringGeofences The geofence(s) triggered.
     * @return The transition details formatted as String.
     */
    private String getGeofenceTransitionDetails(
            Context context,
            int geofenceTransition,
            List<Geofence> triggeringGeofences) {

        String geofenceTransitionString = getTransitionString(geofenceTransition);

        // Get the Ids of each geofence that was triggered.
        ArrayList triggeringGeofencesIdsList = new ArrayList();
        for (Geofence geofence : triggeringGeofences) {
            triggeringGeofencesIdsList.add(geofence.getRequestId());
        }
        String triggeringGeofencesIdsString = TextUtils.join(", ", triggeringGeofencesIdsList);

        return triggeringGeofencesIdsString;
    }


    private void sendContNotification(String notifDetails) {

        Intent notificationIntent = new Intent(getApplicationContext(), OnTheRoadActivity.class);
        notificationIntent.setFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP |
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

        // Get a PendingIntent containing the entire back stack.
        PendingIntent notificationPendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.drawable.ic_switch_on)
                .setContentTitle(notifDetails)
                .setContentIntent(notificationPendingIntent)
                .setOngoing(true);

        // Dismiss notification once the user touches it.
        builder.setAutoCancel(true);

        // Get an instance of the Notification manager
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Issue the notification
        mNotificationManager.notify(0, builder.build());
    }


    /**
     * Posts a notification in the notification bar when a transition is detected.
     * If the user clicks the notification, control goes to the MainActivity.
     */
    private void sendNotification(String notificationDetails) {
        // Create an explicit content Intent that starts the main Activity.
        Intent notificationIntent = new Intent(getApplicationContext(), OnTheRoadActivity.class);

        // Construct a task stack.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

        // Add the main Activity to the task stack as the parent.
        stackBuilder.addParentStack(MainActivity.class);

        // Push the content Intent onto the stack.
        stackBuilder.addNextIntent(notificationIntent);

        // Get a PendingIntent containing the entire back stack.
        PendingIntent notificationPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        // Get a notification builder that's compatible with platform versions >= 4
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setVibrate(new long[]{1000, 1000, 1000, 1000, 1000});
        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        builder.setSound(uri);

        // Define the notification settings.
        builder.setSmallIcon(R.drawable.ic_switch_on)
                // In a real app, you may want to use a library like Volley
                // to decode the Bitmap.
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                        R.drawable.ic_switch_on))
                .setColor(Color.RED)
                .setContentTitle(notificationDetails)
                .setContentText(getString(R.string.geofence_transition_notification_text))
                .setContentIntent(notificationPendingIntent);

        // Dismiss notification once the user touches it.
        builder.setAutoCancel(true);

        // Get an instance of the Notification manager
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Issue the notification
        mNotificationManager.notify(0, builder.build());
    }

    /**
     * Maps geofence transition types to their human-readable equivalents.
     *
     * @param transitionType A transition type constant defined in Geofence
     * @return A String indicating the type of transition
     */
    private String getTransitionString(int transitionType) {
        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                return getString(R.string.geofence_transition_entered);
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                return getString(R.string.geofence_transition_exited);
            default:
                return getString(R.string.unknown_geofence_transition);
        }
    }
}
