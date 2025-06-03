package com.example.alinadiplom.ui.dashboard;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

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
        void onRoommateFound(String name);
        void onNoRoommates();
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
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String dorm = snapshot.child("dorm").getValue(String.class);
                        String room = snapshot.child("room").getValue(String.class);
                        if (dorm != null && room != null) {
                            callback.onRoomLoaded(dorm, room);
                        } else {
                            callback.onError("Общежитие или комната не указаны");
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError("Ошибка загрузки данных комнаты");
                    }
                });
    }

    /**
     * Загружает всех соседей из узла rooms/{dorm}/{room}/residents
     */
    public void loadRoommates(String dorm, String room, RoommatesCallback callback) {
        String safeRoomKey = room.replace("/", "\\");
        roomsRef.child(dorm)
                .child(safeRoomKey)
                .child("residents")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            callback.onNoRoommates();
                            return;
                        }
                        for (DataSnapshot child : snapshot.getChildren()) {
                            String name = child.getValue(String.class);
                            if (name != null) {
                                callback.onRoommateFound(name);
                            }
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        callback.onNoRoommates();
                    }
                });
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
