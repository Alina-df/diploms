package com.example.alinadiplom.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.alinadiplom.R;
import android.media.RingtoneManager;
import android.net.Uri;
public class NotificationHelper {

    private static final String CHANNEL_ID = "service_channel_id";
    private static final String CHANNEL_NAME = "Системные уведомления";

    public static void showLocalNotification(Context context, String title, String message) {
        if (!PrefsHelper.isPushEnabled(context)) {
            return;
        }

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager != null) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Уведомления о заявках");
            channel.setSound(soundUri, null); // Устанавливаем звук
            notificationManager.createNotificationChannel(channel);
        }


        Intent emptyIntent = new Intent();
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                emptyIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setSound(soundUri);


        if (notificationManager != null) {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }
    public static Notification buildForegroundServiceNotification(Context context) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Слушаем уведомления стиральной машины")
                .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        return builder.build();
    }


}
