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
 * - status: available / occupied / reserved
 * - userId: UID текущего пользователя или null
 * - startTime: время начала стирки (ms) или null
 * - duration: длительность стирки в ms или null
 * - reservedTime: когда текущий зарезервированный получил право (ms) или null
 * - /reservations: очередь { pushId: { userId, timestamp } }
 */
public class LaundryManager {

    private final Context context;
    private final DatabaseReference laundryRef;
    private CountDownTimer washTimer;
    private CountDownTimer reservedTimer;

    public LaundryManager(Context context) {
        this.context = context.getApplicationContext();
        this.laundryRef = FirebaseDatabase.getInstance()
                .getReference("laundry");
        initIfNeeded();
    }

    /** Если узел /laundry пустой, задаём статус=available */
    private void initIfNeeded() {
        laundryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("status", "available");
                    data.put("userId", null);
                    data.put("startTime", null);
                    data.put("duration", null);
                    data.put("reservedTime", null);
                    laundryRef.updateChildren(data);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    /** Запускает стирку: очищает очередь, сбрасывает reservedTimer */
    public void startWashing(String userId, int minutes) {
        cancelWashTimer();
        cancelReservedTimer();

        long startTime = System.currentTimeMillis();
        long durationMs = minutes * 60L * 1000L;
        Map<String, Object> data = new HashMap<>();
        data.put("status", "occupied");
        data.put("userId", userId);
        data.put("startTime", startTime);
        data.put("duration", durationMs);
        data.put("reservedTime", null);
        // Очистить очередь
        laundryRef.child("reservations").removeValue();
        laundryRef.updateChildren(data)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context,
                            "Стирка запущена на " + minutes + " мин", Toast.LENGTH_SHORT).show();
                    startWashTimer(durationMs);
                    startForegroundService(startTime, durationMs);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(context,
                                "Ошибка запуска стирки: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    /** Добавляет пользователя в очередь, если его там ещё нет */
    public void reserve(final String userId) {
        DatabaseReference resRef = laundryRef.child("reservations");
        resRef.orderByChild("userId").equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snap) {
                        if (snap.exists()) {
                            Toast.makeText(context,
                                    "Вы уже в очереди", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        long now = System.currentTimeMillis();
                        Map<String, Object> val = new HashMap<>();
                        val.put("userId", userId);
                        val.put("timestamp", now);
                        laundryRef.child("reservations").push().setValue(val)
                                .addOnSuccessListener(aVoid ->
                                        Toast.makeText(context,
                                                "Вы добавлены в очередь", Toast.LENGTH_SHORT).show()
                                )
                                .addOnFailureListener(e ->
                                        Toast.makeText(context,
                                                "Ошибка бронирования: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                );
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    /** Отменяет бронь и, если это текущий reserved, переводит очередь дальше */
    public void cancelReservation(final String userId) {
        cancelReservedTimer();
        DatabaseReference resRef = laundryRef.child("reservations");
        resRef.orderByChild("userId").equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snap) {
                        for (DataSnapshot c : snap.getChildren()) {
                            c.getRef().removeValue();
                        }
                        // Проверяем, был ли это текущий reserved
                        laundryRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override public void onDataChange(@NonNull DataSnapshot s) {
                                String status = s.child("status").getValue(String.class);
                                String uid    = s.child("userId").getValue(String.class);
                                if ("reserved".equals(status) && userId.equals(uid)) {
                                    // Этот пользователь сбросил бронь — ищем следующего
                                    processNextReservation();
                                }
                            }
                            @Override public void onCancelled(@NonNull DatabaseError e) {}
                        });
                        Toast.makeText(context,
                                "Бронь отменена", Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    /** Обрабатывает очередь — проводит первого к reserved и даёт ему 5 минут */
    private void processNextReservation() {
        cancelReservedTimer();

        DatabaseReference resRef = laundryRef.child("reservations");
        resRef.orderByChild("timestamp")
                .limitToFirst(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snap) {
                        if (!snap.exists()) {
                            release();
                            return;
                        }
                        DataSnapshot first = snap.getChildren().iterator().next();
                        String nextUser = first.child("userId").getValue(String.class);
                        first.getRef().removeValue();

                        long now = System.currentTimeMillis();
                        Map<String,Object> data = new HashMap<>();
                        data.put("status", "reserved");
                        data.put("userId", nextUser);
                        data.put("reservedTime", now);
                        data.put("startTime", null);
                        data.put("duration", null);
                        laundryRef.updateChildren(data)
                                .addOnSuccessListener(aVoid -> startReservedTimer());
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    /** Таймер 5 минут для зарезервированного */
    private void startReservedTimer() {
        cancelReservedTimer();
        reservedTimer = new CountDownTimer(5 * 60_000L, 5 * 60_000L) {
            @Override public void onTick(long millisUntilFinished) {}
            @Override public void onFinish() {
                processNextReservation();
            }
        }.start();
    }

    private void cancelReservedTimer() {
        if (reservedTimer != null) {
            reservedTimer.cancel();
            reservedTimer = null;
        }
    }

    /** Таймер локальный для стирки; по окончании — processNextReservation() */
    private void startWashTimer(long durationMs) {
        cancelWashTimer();
        washTimer = new CountDownTimer(durationMs, 1000) {
            @Override public void onTick(long millisUntilFinished) {}
            @Override public void onFinish() {
                Toast.makeText(context,
                        "Стирка завершена", Toast.LENGTH_SHORT).show();
                processNextReservation();
                stopForegroundService();
            }
        }.start();
    }

    private void cancelWashTimer() {
        if (washTimer != null) {
            washTimer.cancel();
            washTimer = null;
        }
    }

    /** Сбрасывает всё в available и запускает очередь */
    public void release() {
        cancelWashTimer();
        cancelReservedTimer();
        stopForegroundService();

        Map<String,Object> data = new HashMap<>();
        data.put("status", "available");
        data.put("userId", null);
        data.put("startTime", null);
        data.put("duration", null);
        data.put("reservedTime", null);
        laundryRef.updateChildren(data)
                .addOnSuccessListener(aVoid -> processNextReservation());
    }

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

    public void addStatusListener(ValueEventListener listener) {
        laundryRef.addValueEventListener(listener);
    }
    public void removeStatusListener(ValueEventListener listener) {
        laundryRef.removeEventListener(listener);
    }

    public void cleanup() {
        cancelWashTimer();
        cancelReservedTimer();
    }
}
