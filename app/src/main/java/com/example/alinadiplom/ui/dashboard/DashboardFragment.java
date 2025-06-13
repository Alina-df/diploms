// Файл: app/src/main/java/com/example/alinadiplom/ui/dashboard/DashboardFragment.java

package com.example.alinadiplom.ui.dashboard;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.alinadiplom.R;
import com.example.alinadiplom.databinding.FragmentDashboardBinding;
import com.example.alinadiplom.utils.NotificationHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.HashMap;
import java.util.Map;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private LaundryManager laundryManager;
    private RoomDataManager roomDataManager;
    private FirebaseAuth mAuth;

    private ChildEventListener notificationListener;

    private String myUid;
    private Spinner spinnerServiceType;
    private EditText editRoom, editProblem;
    Button buttonSendService, buttonMyRequests;
    private TextView serviceInfo;
    private ValueEventListener laundryListener;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        spinnerServiceType = root.findViewById(R.id.spinnerServiceType);
        editRoom = root.findViewById(R.id.editRoom);
        editProblem = root.findViewById(R.id.editProblem);
        buttonSendService = root.findViewById(R.id.buttonSendService);
        serviceInfo = root.findViewById(R.id.serviceInfo);
        mAuth = FirebaseAuth.getInstance();
        myUid = mAuth.getCurrentUser() != null
                ? mAuth.getCurrentUser().getUid()
                : null;
        if (myUid == null) {
            Toast.makeText(getContext(), "Пользователь не авторизован", Toast.LENGTH_SHORT).show();
            return root;
        }
        buttonMyRequests = root.findViewById(R.id.buttonMyRequests);
        buttonMyRequests.setOnClickListener(v -> {
            NavHostFragment.findNavController(DashboardFragment.this)
                    .navigate(R.id.action_dashboard_to_myRequests);
        });

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getContext(), android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Сантехник", "Столяр", "Электрик"}
        );
        spinnerServiceType.setAdapter(adapter);

        buttonSendService.setOnClickListener(v -> sendServiceRequest());
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
        DatabaseReference notiRef = FirebaseDatabase.getInstance()
                .getReference("UserNotifications")
                .child(myUid);

        notiRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if (!isAdded()) return;

                String title = snapshot.child("title").getValue(String.class);
                String message = snapshot.child("message").getValue(String.class);

                NotificationHelper.showLocalNotification(requireContext(), title, message);

                snapshot.getRef().removeValue(); // удаляем уведомление из Firebase
            }

            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
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
        String room = editRoom.getText().toString().trim();
        String problem = editProblem.getText().toString().trim();
        String type = spinnerServiceType.getSelectedItem().toString();

        if (room.isEmpty() || problem.isEmpty()) {
            serviceInfo.setText("Пожалуйста, заполните все поля.");
            return;
        }

        String requestId = FirebaseDatabase.getInstance().getReference()
                .child("ServiceRequests")
                .push()
                .getKey();

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("room", room);
        requestMap.put("problem", problem);
        requestMap.put("type", type);
        requestMap.put("status", "Ожидает обработки");
        requestMap.put("timestamp", ServerValue.TIMESTAMP);
        requestMap.put("userId", uid);  // Добавляем пользователя заявки

        FirebaseDatabase.getInstance()
                .getReference("ServiceRequests")
                .child(requestId)
                .setValue(requestMap)
                .addOnSuccessListener(aVoid -> {
                    serviceInfo.setText("Заявка принята, ожидайте.");
                    editRoom.setText("");
                    editProblem.setText("");
                })
                .addOnFailureListener(e -> {
                    serviceInfo.setText("Ошибка отправки: " + e.getMessage());
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (laundryListener != null) {
            laundryManager.removeStatusListener(laundryListener);
        }

        // Удаление уведомлений
        DatabaseReference notiRef = FirebaseDatabase.getInstance()
                .getReference("UserNotifications")
                .child(myUid);
        if (notificationListener != null) {
            notiRef.removeEventListener(notificationListener);
        }

        laundryManager.cleanup();
        roomDataManager.cleanup();
        binding = null;
    }

}
