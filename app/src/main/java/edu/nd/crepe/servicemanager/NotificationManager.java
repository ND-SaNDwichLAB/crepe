package edu.nd.crepe.servicemanager;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import edu.nd.crepe.R;

public class NotificationManager {
    private Context context;
    private Activity activity;
    private String notificationMessage;

    final private String CHANNEL_ID = "CREPE_NOTIFICATION_CHANNEL";
    public NotificationManager(Context context, Activity activity) {
        this.context = context;
        this.activity = activity;
    }

    public void showNotification(String notificationMessage) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("CREPE")
                .setContentText(notificationMessage)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        // Issue the notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        } else {
            notificationManager.notify(getUniqueNotificationId(), builder.build());
        }
    }

    private int getUniqueNotificationId() {
        return (int) System.currentTimeMillis();
    }
}
