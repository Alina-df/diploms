// Файл: app/src/main/java/com/example/alinadiplom/ui/dashboard/LaundryManager.java

package com.example.alinadiplom.ui.dashboard;

import android.content.Context;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import android.os.CountDownTimer;

/**
 * Класс, инкапсулирующий логику работы стиральной машины:
 * - смена статуса (available/occupied) в Firebase,
 * - запуск таймера CountDownTimer,
 * - управление очередью и уведомления.
 */
public class LaundryManager {

    private final Context context;
    private final DatabaseReference laundryRef;
    private final String laundryId;
    private final ProgressBar progressBar;
    private final NotificationHelper notificationHelper;

    private CountDownTimer timer;
    private boolean isNotificationEnabled = false;

    public LaundryManager(Context context,
                          String laundryId,
                          ProgressBar progressBar,
                          NotificationHelper notificationHelper) {
        this.context = context.getApplicationContext();
        this.laundryId = laundryId;
        this.progressBar = progressBar;
        this.notificationHelper = notificationHelper;
        this.laundryRef = FirebaseDatabase.getInstance()
                .getReference("laundry")
                .child(laundryId);
    }

    /**
     * Сразу “занять” машинку (обновляет только статус и userId).
     */
    public void updateLaundryStatus(String userId) {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "occupied");
        data.put("userId", userId);
        laundryRef.updateChildren(data)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(context,
                                "Стиралка занята",
                                Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(context,
                                "Не удалось занять стиралку: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Запуск стирки с таймером.
     * После успеха сохраняем startTime и duration → запускаем CountDownTimer.
     *
     * @param userId     UID текущего пользователя.
     * @param durationMs Длительность стирки в миллисекундах.
     * @param onStarted  Runnable, который будет вызван сразу после успешного обновления статуса.
     */
    public void startWashing(String userId, long durationMs, Runnable onStarted) {
        long startTime = System.currentTimeMillis();
        Map<String, Object> data = new HashMap<>();
        data.put("status", "occupied");
        data.put("userId", userId);
        data.put("startTime", startTime);
        data.put("duration", durationMs);

        laundryRef.updateChildren(data)
                .addOnSuccessListener(aVoid -> {
                    if (onStarted != null) onStarted.run();
                    startTimer(durationMs);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(context,
                                "Ошибка запуска стирки: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Сбрасывает статус машинки в “available”.
     */
    void resetLaundry() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "available");
        data.put("userId", null);
        data.put("startTime", null);
        data.put("duration", null);
        laundryRef.updateChildren(data);
    }

    /**
     * Добавляет текущего пользователя в очередь.
     */
    public void reserveLaundry(String userId) {
        laundryRef.child("reservations").push().setValue(userId)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(context,
                                "Вы добавлены в очередь", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(context,
                                "Ошибка бронирования: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Проверяет очередь и посылает уведомление следующему.
     * Запускается после того, как текущая стирка полностью завершилась.
     */
    void notifyNextInQueue() {
        laundryRef.child("reservations")
                .limitToFirst(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) return;

                        // Берём первый элемент очереди
                        DataSnapshot first = snapshot.getChildren().iterator().next();
                        String nextUserId = first.getValue(String.class);
                        if (nextUserId != null) {
                            // Отправляем уведомление “Ваша очередь”
                            notificationHelper.sendReadyNotification(nextUserId);
                        }
                        // Удаляем этот элемент из очереди
                        first.getRef().removeValue();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {/**/}
                });
    }

    /**
     * Запускает CountDownTimer (progressBar показывает прогресс).
     */
    private void startTimer(long durationMs) {
        stopTimer();
        progressBar.setMax((int) durationMs);

        // Используем AtomicLong, чтобы знать исходное время
        AtomicLong total = new AtomicLong(durationMs);

        timer = new CountDownTimer(durationMs, 1000) {
            @Override public void onTick(long millisUntilFinished) {
                int progress = (int) (total.get() - millisUntilFinished);
                progressBar.setProgress(progress);
            }

            @Override public void onFinish() {
                // Устанавливаем прогресс в полный максимум
                progressBar.setProgress((int) total.get());

                // Сбрасываем статус машинки в “available”
                resetLaundry();

                // Уведомляем текущего пользователя локально
                if (isNotificationEnabled) {
                    notificationHelper.sendFinishNotification();
                }

                // Выводим Toast
                Toast.makeText(context,
                        "Стирка завершена", Toast.LENGTH_SHORT).show();

                // Уведомляем следующего в очереди
                notifyNextInQueue();
            }
        }.start();
    }

    /**
     * Останавливает таймер, если он запущен.
     */
    public void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    /**
     * Включить/выключить локальные уведомления (по окончании стирки).
     */
    public void setNotificationEnabled(boolean enabled) {
        this.isNotificationEnabled = enabled;
    }

    /**
     * Если понадобится удалить слушатели внутри LaundryManager, можно расширить этот метод.
     */
    public void cleanup() {
        stopTimer();
        // Слушателей (ValueEventListener) в данном классе нет,
        // поэтому дополнительных действий не требуется.
    }
}
