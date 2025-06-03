// Файл: NotificationHelper.java
package com.example.alinadiplom.ui.dashboard;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;

import androidx.core.app.NotificationCompat;

import com.example.alinadiplom.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

/**
 * Хелпер для отправки локальных уведомлений о:
 * - завершении стирки текущим пользователем,
 * - нотификации «ваша очередь» следующему в очереди (через запись в Realtime DB).
 */
public class NotificationHelper {

    private static final String CHANNEL_ID = "laundry_channel";
    private final Context context;

    public NotificationHelper(Context context) {
        this.context = context.getApplicationContext();
        createChannel();
    }

    private void createChannel() {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Laundry Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Уведомления о статусе стиралок");
            manager.createNotificationChannel(channel);
        }
    }

    /** Локальная нотификация о завершении стирки (на текущее устройство). */
    public void sendFinishNotification(String laundryId) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Стиралка " + laundryId)
                .setContentText("Ваша стирка завершена. Пожалуйста, заберите вещи.")
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            int notificationId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
            manager.notify(notificationId, builder.build());
        }
    }

    /**
     * “Ваша очередь” → пишем в /users/{nextUserId}/laundryNotifications
     * Чтобы следующий пользователь получил Toast (через слушатель в DashboardFragment).
     */
    public void sendQueueNotification(String nextUserId, String laundryId) {
        DatabaseReference notifRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(nextUserId)
                .child("laundryNotifications")
                .push();

        Map<String, Object> payload = new HashMap<>();
        payload.put("message", "Теперь ваша очередь использовать " + laundryId);
        payload.put("timestamp", System.currentTimeMillis());
        notifRef.setValue(payload);
    }
}
