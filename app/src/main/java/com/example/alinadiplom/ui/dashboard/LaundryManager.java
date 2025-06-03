package com.example.alinadiplom.ui.dashboard;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.CountDownTimer;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.database.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Логика для одной стиралки:
 * - status: available / occupied
 * - userId: UID текущего пользователя или null
 * - startTime: время начала стирки (ms) или null
 * - duration: длительность стирки в ms или null
 * - /reservations: очередь { pushId: { userId, timestamp } }
 */
public class LaundryManager {

    private final Context context;
    private final DatabaseReference laundryRef;
    private CountDownTimer washTimer;

    public LaundryManager(Context context) {
        this.context = context.getApplicationContext();
        this.laundryRef = FirebaseDatabase.getInstance()
                .getReference("laundry");
        initIfNeeded();
    }

    /** Если узел /laundry пустой, задаём статус=available, userId=null */
    private void initIfNeeded() {
        laundryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("status", "available");
                    data.put("userId", null);
                    data.put("startTime", null);
                    data.put("duration", null);
                    laundryRef.updateChildren(data);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    /** Занять стиралку: status=occupied, userId=currentUserId, без таймера */
    public void occupy(String userId) {
        cancelWashTimer();
        Map<String, Object> data = new HashMap<>();
        data.put("status", "occupied");
        data.put("userId", userId);
        data.put("startTime", null);
        data.put("duration", null);
        laundryRef.child("reservations").removeValue(); // сбросить очередь
        laundryRef.updateChildren(data)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(context,
                                "Стиралка занята", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(context,
                                "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Запустить стирку с таймером.
     * Устанавливает status, userId, startTime и duration в Firebase,
     * и локальный CountDownTimer, по окончании которого сбрасывает статус.
     */
    // Метод в LaundryManager
    private void startForegroundService(long startTime, long durationMs) {
        Intent intent = new Intent(context, LaundryTimerService.class);
        intent.putExtra(LaundryTimerService.EXTRA_START_TIME, startTime);
        intent.putExtra(LaundryTimerService.EXTRA_DURATION, durationMs);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    private void stopForegroundService() {
        Intent intent = new Intent(context, LaundryTimerService.class);
        context.stopService(intent);
    }

    // В startWashing вызови после обновления Firebase:
    public void startWashing(String userId, int minutes) {
        cancelWashTimer();
        long startTime = System.currentTimeMillis();
        long durationMs = minutes * 60L * 1000L;

        Map<String, Object> data = new HashMap<>();
        data.put("status", "occupied");
        data.put("userId", userId);
        data.put("startTime", startTime);
        data.put("duration", durationMs);
        laundryRef.child("reservations").removeValue(); // сброс очереди
        laundryRef.updateChildren(data)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context,
                            "Стирка запущена на " + minutes + " мин", Toast.LENGTH_SHORT).show();
                    startForegroundService(startTime, durationMs);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(context,
                                "Ошибка запуска стирки: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    // В release нужно остановить сервис:



    /** Локальный таймер: по окончании сбрасывает статус и уведомляет */
    private void startWashTimer(long durationMs) {
        washTimer = new CountDownTimer(durationMs, 1000) {
            @Override public void onTick(long millisUntilFinished) {
                // Здесь при желании можно обновлять прогресс в UI
            }
            @Override public void onFinish() {
                release();
                Toast.makeText(context,
                        "Стирка завершена", Toast.LENGTH_SHORT).show();
            }
        }.start();
    }

    private void cancelWashTimer() {
        if (washTimer != null) {
            washTimer.cancel();
            washTimer = null;
        }
    }

    /** Сброс статуса: status → available, userId=null, startTime=null, duration=null */
    public void release() {
        cancelWashTimer();
        stopForegroundService();

        Map<String, Object> data = new HashMap<>();
        data.put("status", "available");
        data.put("userId", null);
        data.put("startTime", null);
        data.put("duration", null);
        laundryRef.updateChildren(data);
    }

    /** Добавить в очередь: /reservations/{pushId} = { userId, timestamp } */
    public void reserve(String userId) {
        long now = System.currentTimeMillis();
        DatabaseReference pushRef = laundryRef.child("reservations").push();
        Map<String, Object> value = new HashMap<>();
        value.put("userId", userId);
        value.put("timestamp", now);
        pushRef.setValue(value)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(context,
                                "Вы добавлены в очередь", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(context,
                                "Ошибка бронирования: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    /** Отменить бронь: ищем записи, где userId == userId, и удаляем */
    public void cancelReservation(String userId) {
        laundryRef.child("reservations")
                .orderByChild("userId")
                .equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean found = false;
                        for (DataSnapshot child : snapshot.getChildren()) {
                            child.getRef().removeValue();
                            found = true;
                        }
                        if (found) {
                            Toast.makeText(context,
                                    "Бронь отменена", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context,
                                    "Вы не в очереди", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(context,
                                "Ошибка отмены брони", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /** Слушатель статуса и очереди: передаётся во фрагмент, чтобы обновлять UI */
    public void addStatusListener(ValueEventListener listener) {
        laundryRef.addValueEventListener(listener);
    }

    public void removeStatusListener(ValueEventListener listener) {
        laundryRef.removeEventListener(listener);
    }

    /** Необходимо вызывать в onDestroyView(), чтобы отменить таймер */
    public void cleanup() {
        cancelWashTimer();
    }
}
