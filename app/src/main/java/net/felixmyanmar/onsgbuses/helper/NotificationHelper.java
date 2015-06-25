package net.felixmyanmar.onsgbuses.helper;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import net.felixmyanmar.onsgbuses.R;
import net.felixmyanmar.onsgbuses.app.OnTheRoadActivity;


public class NotificationHelper {



    public static void showNotification(Context context, String notifDetails, boolean isAlarm) {

        // Get a PendingIntent containing the entire back stack.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        Intent notificationIntent = new Intent(context, OnTheRoadActivity.class);
        if (isAlarm) notificationIntent.setAction("exit");
        PendingIntent pIntent =
                PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setTicker(notifDetails)
                .setContentTitle(notifDetails)
                .setAutoCancel(true); // Dismiss notification once the user touches it.
                //.setOngoing(false); // No close or clear all button for it

        if (isAlarm) {
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Exit", pIntent);
            builder.setDefaults(Notification.DEFAULT_ALL);
            builder.setContentIntent(pIntent);
        } else {
            builder.setContentIntent(pIntent);
        }
        Notification myNotify = builder.build();
        if (isAlarm) myNotify.flags |= Notification.FLAG_INSISTENT;


        // Get an instance of the Notification manager
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Issue the notification
        mNotificationManager.notify(0, myNotify);
    }


//    public static void sendContNotification(Context context, String notifDetails)
//    {
//        Intent notificationIntent = new Intent(context, OnTheRoadActivity.class);
//        notificationIntent.setFlags(
//                Intent.FLAG_ACTIVITY_CLEAR_TOP |
//                        Intent.FLAG_ACTIVITY_SINGLE_TOP |
//                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
//
//        // Get a PendingIntent containing the entire back stack.
//        PendingIntent notificationPendingIntent =
//                PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
//        builder.setSmallIcon(R.drawable.ic_switch_on)
//                .setContentTitle(notifDetails)
//                .setContentIntent(notificationPendingIntent)
//                .setOngoing(true);
//
//        // Dismiss notification once the user touches it.
//        builder.setAutoCancel(true);
//
//        // Get an instance of the Notification manager
//        NotificationManager mNotificationManager =
//                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
//
//        // Issue the notification
//        mNotificationManager.notify(0, builder.build());
//    }
//
//
//    public static void sendNotification(Context context, String notificationDetails)
//    {
//        // Create an explicit content Intent that starts the main Activity.
//        Intent notificationIntent = new Intent(context, OnTheRoadActivity.class);
//
//        // Construct a task stack.
//        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
//
//        // Add the main Activity to the task stack as the parent.
//        stackBuilder.addParentStack(MainActivity.class);
//
//        // Push the content Intent onto the stack.
//        stackBuilder.addNextIntent(notificationIntent);
//
//        // Get a PendingIntent containing the entire back stack.
//        PendingIntent notificationPendingIntent =
//                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
//
//        // Get a notification builder that's compatible with platform versions >= 4
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
//        builder.setVibrate(new long[]{1000, 1000, 1000, 1000, 1000});
//        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//        builder.setSound(uri);
//
//        // Define the notification settings.
//        builder.setSmallIcon(R.drawable.ic_switch_on)
//                // In a real app, you may want to use a library like Volley
//                // to decode the Bitmap.
//                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
//                        R.drawable.ic_switch_on))
//                .setColor(Color.RED)
//                .setContentTitle(notificationDetails)
//                .setContentText(context.getString(R.string.geofence_transition_notification_text))
//                .setContentIntent(notificationPendingIntent);
//
//        // Dismiss notification once the user touches it.
//        builder.setAutoCancel(true);
//
//        // Get an instance of the Notification manager
//        NotificationManager mNotificationManager =
//                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
//
//        // Issue the notification
//        mNotificationManager.notify(0, builder.build());
//    }
}
