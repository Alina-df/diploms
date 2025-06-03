package com.example.alinadiplom.ui.dashboard;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.alinadiplom.R;

public class LaundryTimerService extends Service {

    public static final String CHANNEL_ID = "LaundryTimerChannel";
    public static final int NOTIF_ID = 1;

    public static final String EXTRA_START_TIME = "startTime";
    public static final String EXTRA_DURATION = "duration";

    private CountDownTimer timer;
    private long endTime;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        long startTime = intent.getLongExtra(EXTRA_START_TIME, 0);
        long duration = intent.getLongExtra(EXTRA_DURATION, 0);
        endTime = startTime + duration;

        startForeground(NOTIF_ID, buildNotification(duration));

        startTimer(duration);

        return START_STICKY;
    }

    private void startTimer(long durationMs) {
        timer = new CountDownTimer(durationMs, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                Notification notification = buildNotification(millisUntilFinished);
                NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                nm.notify(NOTIF_ID, notification);
            }

            @Override
            public void onFinish() {
                stopSelf();
                // Можно тут отправить broadcast или обновить Firebase
            }
        };
        timer.start();
    }

    private Notification buildNotification(long millisRemaining) {
        long minutes = millisRemaining / 60000;
        long seconds = (millisRemaining / 1000) % 60;
        String timeStr = String.format("%02d:%02d", minutes, seconds);

        // Intent для открытия приложения при клике на уведомление
        Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Стирка в процессе")
                .setContentText("Осталось: " + timeStr)
                .setSmallIcon(R.drawable.ic_washing_machine) // Иконка должна быть в drawable
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Стирка",
                    NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
