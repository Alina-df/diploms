package com.example.alinadiplom.ui.dashboard;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Отвечает за загрузку информации о комнате и соседях:
 * - loadUserRoom: из узла Users/{uid} достаёт dorm и room,
 *   после чего вызывает onRoomLoaded или onError.
 * - loadRoommates: из узла rooms/{dorm}/{room}/residents перечисляет всех соседей.
 */
public class RoomDataManager {

    public interface RoomCallback {
        void onRoomLoaded(String dorm, String room);
        void onError(String errorMessage);
    }

    public interface RoommatesCallback {
        void onRoommatesLoaded(Map<String, String> roommates); // UID -> Name
        void onError(String errorMessage);
    }

    private final DatabaseReference usersRef;
    private final DatabaseReference roomsRef;
    private ValueEventListener roomListener;
    private ValueEventListener roommatesListener;

    public RoomDataManager(DatabaseReference usersRef, DatabaseReference roomsRef) {
        this.usersRef = usersRef;
        this.roomsRef = roomsRef;
    }

    /**
     * Загружает dorm+room из узла Users/{uid}
     */
    public void loadUserRoom(String uid, RoomCallback callback) {
        usersRef.child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            callback.onError("Пользователь не найден");
                            return;
                        }

                        String dorm = snapshot.child("dorm").getValue(String.class);
                        String room = snapshot.child("room").getValue(String.class);

                        if (dorm != null && room != null) {
                            callback.onRoomLoaded(dorm, room);
                        } else {
                            callback.onError("Общежитие или комната не указаны");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError("Ошибка загрузки данных комнаты: " + error.getMessage());
                    }
                });
    }

    /**
     * Загружает всех соседей из узла rooms/{dorm}/{room}/residents
     * с обработкой специальных символов в ключах комнат
     */
    public void loadRoommates(String dorm, String room, RoommatesCallback callback) {
        // Нормализация ключа комнаты
        String normalizedRoom = normalizeRoomKey(room);

        roomsRef.child(dorm)
                .child(normalizedRoom)
                .child("residents")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            callback.onError("Комната не найдена");
                            return;
                        }

                        Map<String, String> roommates = new HashMap<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            String name = child.getValue(String.class);
                            if (name != null) {
                                roommates.put(child.getKey(), name);
                            }
                        }

                        if (roommates.isEmpty()) {
                            callback.onError("Соседи не найдены");
                        } else {
                            callback.onRoommatesLoaded(roommates);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError("Ошибка загрузки соседей: " + error.getMessage());
                    }
                });
    }

    /**
     * Нормализация ключа комнаты для Firebase
     * - Заменяет прямые слеши на обратные
     * - Экранирует специальные символы
     */
    private String normalizeRoomKey(String roomKey) {
        // Заменяем прямые слеши на обратные
        String normalized = roomKey.replace("/", "\\");

        // Дополнительная обработка специальных символов при необходимости
        // Например: normalized = normalized.replace(".", "%2E");

        return normalized;
    }

    /**
     * Убираем слушателей, чтобы не было утечек.
     */
    public void cleanup() {
        if (roomListener != null) {
            usersRef.removeEventListener(roomListener);
            roomListener = null;
        }
        if (roommatesListener != null) {
            roomsRef.removeEventListener(roommatesListener);
            roommatesListener = null;
        }
    }
}