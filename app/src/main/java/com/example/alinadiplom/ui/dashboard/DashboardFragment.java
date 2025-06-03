// Файл: app/src/main/java/com/example/alinadiplom/ui/dashboard/DashboardFragment.java

package com.example.alinadiplom.ui.dashboard;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.alinadiplom.R;
import com.example.alinadiplom.databinding.FragmentDashboardBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.HashMap;
import java.util.Map;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private LaundryManager laundryManager;
    private RoomDataManager roomDataManager;
    private FirebaseAuth mAuth;
    private String myUid;
    private ValueEventListener laundryListener;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // 1. Авторизация
        mAuth = FirebaseAuth.getInstance();
        myUid = mAuth.getCurrentUser() != null
                ? mAuth.getCurrentUser().getUid()
                : null;
        if (myUid == null) {
            Toast.makeText(getContext(), "Пользователь не авторизован", Toast.LENGTH_SHORT).show();
            return root;
        }

        // 2. RoomDataManager: загрузка комнаты и соседей
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");
        DatabaseReference roomsRef = FirebaseDatabase.getInstance().getReference("rooms");
        roomDataManager = new RoomDataManager(usersRef, roomsRef);
        loadRoomData();

        // 3. LaundryManager: управление стиральной машиной
        laundryManager = new LaundryManager(requireContext());
        attachLaundryListener();

        // Обработчики кнопок прачечной
        binding.buttonOccupy.setOnClickListener(v -> {
            binding.editMinutes.setVisibility(View.VISIBLE);
            binding.buttonStartWash.setVisibility(View.VISIBLE);
            binding.buttonOccupy.setVisibility(View.GONE);
            binding.buttonReserve.setVisibility(View.GONE);
            binding.buttonCancelReserve.setVisibility(View.GONE);
            binding.laundryQueueInfo.setText("");
        });

        binding.buttonStartWash.setOnClickListener(v -> {
            String minutesStr = binding.editMinutes.getText().toString().trim();
            if (TextUtils.isEmpty(minutesStr)) {
                Toast.makeText(getContext(), "Введите количество минут", Toast.LENGTH_SHORT).show();
                return;
            }
            int minutes = Integer.parseInt(minutesStr);
            if (minutes <= 0) {
                Toast.makeText(getContext(), "Минуты должны быть > 0", Toast.LENGTH_SHORT).show();
                return;
            }
            laundryManager.startWashing(myUid, minutes);
            binding.editMinutes.setText("");
            binding.editMinutes.setVisibility(View.GONE);
            binding.buttonStartWash.setVisibility(View.GONE);
        });

        binding.buttonReserve.setOnClickListener(v ->
                laundryManager.reserve(myUid)
        );
        binding.buttonCancelReserve.setOnClickListener(v ->
                laundryManager.cancelReservation(myUid)
        );

        // 4. Обработка отправки запроса на услугу
        binding.buttonSendService.setOnClickListener(v -> sendServiceRequest());

        return root;
    }

    /** Загрузка номера комнаты и соседей из RoomDataManager */
    private Map<String, String> cachedRoommates = null;
    private String cachedRoom = null;

    private void loadRoomData() {
        if (cachedRoom != null && cachedRoommates != null) {
            // Используем кэшированные данные
            binding.roomNumber.setText(cachedRoom);
            binding.roommatesContainer.removeAllViews();
            for (String name : cachedRoommates.values()) {
                TextView tv = new TextView(getContext());
                tv.setText(name);
                tv.setTextSize(14);
                tv.setPadding(8, 4, 8, 4);
                binding.roommatesContainer.addView(tv);
            }
            return; // не делаем запрос к Firebase
        }

        // Если кэша нет — загружаем из Firebase
        roomDataManager.loadUserRoom(myUid, new RoomDataManager.RoomCallback() {
            @Override
            public void onRoomLoaded(String dorm, String room) {
                cachedRoom = room;
                binding.roomNumber.setText(room);
                roomDataManager.loadRoommates(dorm, room, new RoomDataManager.RoommatesCallback() {
                    @Override
                    public void onRoommatesLoaded(Map<String, String> roommates) {
                        cachedRoommates = roommates;
                        binding.roommatesContainer.removeAllViews();
                        for (String name : roommates.values()) {
                            TextView tv = new TextView(getContext());
                            tv.setText(name);
                            tv.setTextSize(14);
                            tv.setPadding(8, 4, 8, 4);
                            binding.roommatesContainer.addView(tv);
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        // ...
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                // ...
            }
        });
    }


    /** Подписка на изменения в /laundry для обновления UI */
    private void attachLaundryListener() {
        laundryListener = new ValueEventListener() {
            CountDownTimer uiTimer;

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String status = snapshot.child("status").getValue(String.class);
                String occupiedBy = snapshot.child("userId").getValue(String.class);

                Long startTime = snapshot.child("startTime").getValue(Long.class);
                Long duration = snapshot.child("duration").getValue(Long.class);

                // Отменяем предыдущий UI таймер
                if (uiTimer != null) {
                    uiTimer.cancel();
                    uiTimer = null;
                }

                if (status == null || status.equals("available")) {
                    binding.laundryStatus.setText("○ Свободна");
                    binding.laundryTimer.setText("");
                    // ... остальной UI

                } else {
                    if (occupiedBy != null && occupiedBy.equals(myUid)) {
                        binding.laundryStatus.setText("● Занята вами");
                    } else {
                        binding.laundryStatus.setText("● Занята");
                    }

                    // Запускаем локальный CountDownTimer на UI для отображения оставшегося времени
                    if (startTime != null && duration != null) {
                        long now = System.currentTimeMillis();
                        long finishTime = startTime + duration;
                        long millisLeft = finishTime - now;

                        if (millisLeft > 0) {
                            uiTimer = new CountDownTimer(millisLeft, 1000) {
                                @Override
                                public void onTick(long millisUntilFinished) {
                                    long minutes = millisUntilFinished / 60000;
                                    long seconds = (millisUntilFinished / 1000) % 60;
                                    binding.laundryTimer.setText(String.format("Осталось: %02d:%02d", minutes, seconds));
                                }

                                @Override
                                public void onFinish() {
                                    binding.laundryTimer.setText("");
                                }
                            }.start();
                        } else {
                            binding.laundryTimer.setText("");
                        }
                    } else {
                        binding.laundryTimer.setText("");
                    }

                    // Остальной UI
                    // ...
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        };
        laundryManager.addStatusListener(laundryListener);
    }


    /** Отправка запроса на услугу в /serviceRequests */
    private void sendServiceRequest() {
        String room = binding.editRoom.getText().toString().trim();
        String problem = binding.editProblem.getText().toString().trim();

        if (TextUtils.isEmpty(room)) {
            Toast.makeText(getContext(), "Укажите номер комнаты", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(problem)) {
            Toast.makeText(getContext(), "Опишите проблему", Toast.LENGTH_SHORT).show();
            return;
        }

        long timestamp = System.currentTimeMillis();
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("serviceRequests")
                .push();

        Map<String, Object> data = new HashMap<>();
        data.put("userId", myUid);
        data.put("room", room);
        data.put("problem", problem);
        data.put("timestamp", timestamp);

        ref.setValue(data)
                .addOnSuccessListener(aVoid -> {
                    binding.serviceInfo.setText("Запрос отправлен");
                    binding.editRoom.setText("");
                    binding.editProblem.setText("");
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Ошибка отправки: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (laundryListener != null) {
            laundryManager.removeStatusListener(laundryListener);
        }
        laundryManager.cleanup();
        roomDataManager.cleanup();
        binding = null;
    }
}
