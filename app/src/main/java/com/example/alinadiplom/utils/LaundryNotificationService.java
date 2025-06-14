package com.example.alinadiplom.utils;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LaundryNotificationService extends Service {

    private DatabaseReference machineRef;
    private ValueEventListener notificationListener;

    @Override
    public void onCreate() {
        super.onCreate();

        // Заменить на реальный ID машины или слушать все сразу
        String machineId = "machine_1"; 
        machineRef = FirebaseDatabase.getInstance().getReference("LaundryMachine").child(machineId);

        notificationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String title = snapshot.child("title").getValue(String.class);
                    String message = snapshot.child("message").getValue(String.class);

                    if (title != null && message != null) {
                        NotificationHelper.showLocalNotification(getApplicationContext(), title, message);
                        machineRef.child("notification").removeValue();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        machineRef.child("notification").addValueEventListener(notificationListener);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Запускаем foreground с обязательным уведомлением (чтобы сервис не убивали)
        startForeground(1, NotificationHelper.buildForegroundServiceNotification(this));

        return START_STICKY;  // Сервис будет перезапущен, если убит
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (machineRef != null && notificationListener != null) {
            machineRef.child("notification").removeEventListener(notificationListener);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
