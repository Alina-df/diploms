// Файл: app/src/main/java/com/example/alinadiplom/ui/dashboard/NotificationHelper.java

package com.example.alinadiplom.ui.dashboard;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.example.alinadiplom.R;

/**
 * Хелпер для отправки локальных уведомлений.
 * Пример: уведомление, когда стирка завершилась, и уведомление следующему человеку в очереди.
 */
public class NotificationHelper {

    private static final String CHANNEL_ID = "laundry_channel";
    private final Context context;

    public NotificationHelper(Context context) {
        this.context = context.getApplicationContext();
        createChannel();
    }

    // Создаём канал (если еще не создан)
    private void createChannel() {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Laundry Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Уведомления о статусе стиралки");
            manager.createNotificationChannel(channel);
        }
    }

    /**
     * Отправить локальное уведомление тому же пользователю о завершении стирки.
     */
    public void sendFinishNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Стиральная машина")
                .setContentText("Ваша стирка завершена. Пожалуйста, заберите вещи.")
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }
    public void sendReadyNotification(String nextUserId) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Стиральная машина")
                .setContentText("Теперь ваша очередь использовать стиралку!")
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }
}
