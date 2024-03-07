package edu.nd.crepe.servicemanager;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import edu.nd.crepe.MainActivity;
import edu.nd.crepe.R;

public class CrepeNotificationManager {

    public static Notification showNotification(Context context, String notificationMessage) {
        String CHANNEL_ID = "CREPE_NOTIFICATION_CHANNEL";
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, MainActivity.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Crepe")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText(notificationMessage)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        // Issue the notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        Notification notification = builder.build();
        notificationManager.notify(1, notification);
        return notification;
    }
}
