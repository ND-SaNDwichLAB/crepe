package edu.nd.crepe.servicemanager;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import edu.nd.crepe.R;

public class CrepeNotificationManager {
    private static final String TAG = "CrepeNotificationMgr";

    private Context context;
    private Activity activity;
    private String notificationMessage;

    final private String CHANNEL_ID = "CREPE_NOTIFICATION_CHANNEL";
    public CrepeNotificationManager(Context context, Activity activity) {
        this.context = context;
        this.activity = activity;
    }

    public void showNotification(String notificationMessage) {
        if (context == null) {
            Log.w(TAG, "No context to show the notification with, dropping it: " + notificationMessage);
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("CREPE")
                .setContentText(notificationMessage)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        // The notification permission only exists (and only has to be requested) from Android 13 on.
        // Asking for it on older versions can never succeed and would silently swallow every notification.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            if (activity != null && !activity.isFinishing()) {
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            } else {
                Log.w(TAG, "Notification permission is missing and there is no activity to ask for it");
            }
            return;
        }

        // Issue the notification
        try {
            NotificationManagerCompat.from(context).notify(getUniqueNotificationId(), builder.build());
        } catch (SecurityException e) {
            // the permission can be revoked between the check above and the call
            Log.w(TAG, "Not allowed to post notifications", e);
        }
    }

    private int getUniqueNotificationId() {
        return (int) System.currentTimeMillis();
    }
}
