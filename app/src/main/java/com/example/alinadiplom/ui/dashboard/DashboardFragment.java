// Файл: app/src/main/java/com/example/alinadiplom/ui/dashboard/DashboardFragment.java

package com.example.alinadiplom.ui.dashboard;

import android.app.AlertDialog;
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
import androidx.fragment.app.FragmentTransaction;
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
    private String currentStatus = "available";
    private boolean isReservedByMe = false;
    private ChildEventListener notificationListener;
    private String myUid;
    private Spinner spinnerServiceType;
    private EditText editRoom, editProblem;
    Button buttonSendService, buttonMyRequests;
    private CountDownTimer uiTimer;
    private ValueEventListener laundryListener;
    private TextView serviceInfo;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        spinnerServiceType = root.findViewById(R.id.spinnerServiceType);
        editRoom = root.findViewById(R.id.editRoom);
        editRoom.setVisibility(View.GONE);
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
        Button buttonLaundryList = root.findViewById(R.id.buttonLaundrylist);
        buttonLaundryList.setOnClickListener(v -> {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_navigation_dashboard_to_laundryQueueFragment);
        });
        spinnerServiceType.setAdapter(adapter);
        buttonSendService.setOnClickListener(v -> sendServiceRequest());
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");
        DatabaseReference roomsRef = FirebaseDatabase.getInstance().getReference("rooms");
        roomDataManager = new RoomDataManager(usersRef, roomsRef);
        loadRoomData();
        laundryManager = new LaundryManager(requireContext());
        attachLaundryListener();
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

        binding.buttonReserve.setOnClickListener(v -> {
            if (isReservedByMe) {
                laundryManager.reserve(myUid);
            } else {
                String status = currentStatus;
                if ("available".equals(status)) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Сколько минут стирки?")
                            .setItems(new CharSequence[]{"2","30","45","60","90"}, (d,i) -> {
                                int minutes = Integer.parseInt(new String[]{"2","30","45","60","90"}[i]);
                                laundryManager.startWashing(myUid, minutes);
                            })
                            .show();
                } else {
                    // очередь
                    laundryManager.reserve(myUid);
                }
            }
        });
        binding.buttonCancelReserve.setOnClickListener(v ->
                laundryManager.cancelReservation(myUid)
        );
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
                snapshot.getRef().removeValue();
            }

            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
        return root;
    }
    private Map<String, String> cachedRoommates = null;
    private String cachedRoom = null;
    private void loadRoomData() {
        if (cachedRoom != null && cachedRoommates != null) {
            binding.roomNumber.setText(cachedRoom);
            binding.roommatesContainer.removeAllViews();
            for (String name : cachedRoommates.values()) {
                TextView tv = new TextView(getContext());
                tv.setText(name);
                tv.setTextSize(14);
                tv.setPadding(8, 4, 8, 4);
                binding.roommatesContainer.addView(tv);
            }
            return;
        }
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
            }
        });
    }
    private void attachLaundryListener() {
        laundryListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // 1. Сброс UI-та
                if (uiTimer != null) { uiTimer.cancel(); uiTimer = null; }
                binding.editMinutes.setVisibility(View.GONE);
                binding.buttonStartWash.setVisibility(View.GONE);
                binding.buttonReserve.setVisibility(View.GONE);
                binding.buttonCancelReserve.setVisibility(View.GONE);
                binding.laundryTimer.setText("");

                // 2. Читаем поля
                String status     = snapshot.child("status").getValue(String.class);
                String occupiedBy = snapshot.child("userId").getValue(String.class);
                Long   reservedAt = snapshot.child("reservedAt").getValue(Long.class);
                currentStatus     = status != null ? status : "available";
                boolean isMine    = myUid.equals(occupiedBy);

                // 3. UI‑логика
                switch (currentStatus) {

                    case "occupied":
                        if (isMine) {
                            binding.laundryStatus.setText("● Занята вами");
                        } else {
                            binding.laundryStatus.setText("● Занята");
                            binding.buttonReserve.setText("Записаться в очередь");
                            binding.buttonReserve.setVisibility(View.VISIBLE);
                        }
                        break;

                    case "reserved":
                        if (isMine) {
                            binding.laundryStatus.setText("● Ваша очередь активна");

                            // поле минут + кнопки «Начать» / «Отменить»
                            binding.editMinutes.setVisibility(View.VISIBLE);
                            binding.buttonStartWash.setVisibility(View.VISIBLE);
                            binding.buttonCancelReserve.setVisibility(View.VISIBLE);

                            // обратный отсчёт 5 минут
                            if (reservedAt != null) {
                                long left = reservedAt + 5*60_000 - System.currentTimeMillis();
                                startUiTimer(left);
                            }
                        } else {
                            binding.laundryStatus.setText("● Зарезервирована");
                            binding.buttonReserve.setText("Записаться в очередь");
                            binding.buttonReserve.setVisibility(View.VISIBLE);
                        }
                        break;

                    case "available":
                    default:
                        binding.laundryStatus.setText("○ Свободна");
                        binding.buttonReserve.setText("Занять");
                        binding.buttonReserve.setVisibility(View.VISIBLE);
                        break;
                }

                // 4. Таймер стирки, если уже «occupied»
                Long startTime = snapshot.child("startTime").getValue(Long.class);
                Long duration  = snapshot.child("duration").getValue(Long.class);
                if ("occupied".equals(currentStatus) && startTime != null && duration != null) {
                    long left = startTime + duration - System.currentTimeMillis();
                    startUiTimer(left);
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) { }
        };

        // Слушатель статуса
        laundryManager.addStatusListener(laundryListener);

        /* ---------- КНОПКИ ---------- */

        // Занять / Записаться
        binding.buttonReserve.setOnClickListener(v -> {
            if ("available".equals(currentStatus)) {
                // сразу предлагаем время и стартуем
                showMinutesDialogAndStart();
            } else {
                laundryManager.reserve(myUid);          // записываемся в очередь
            }
        });

        // Начать стирку по брони
        binding.buttonStartWash.setOnClickListener(v -> {
            int mins = parseMinutes(binding.editMinutes.getText().toString());
            if (mins <= 0) mins = 30;
            laundryManager.startWashing(myUid, mins);
        });

        // Отменить бронь
        binding.buttonCancelReserve.setOnClickListener(v ->
                new AlertDialog.Builder(requireContext())
                        .setMessage("Отменить бронь?")
                        .setPositiveButton("Да", (d, w) -> laundryManager.cancelReservation(myUid))
                        .setNegativeButton("Нет", null)
                        .show());
    }

    /* ---------- ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ---------- */

    private void startUiTimer(long millis) {
        if (millis <= 0) return;
        uiTimer = new CountDownTimer(millis, 1000) {
            @Override public void onTick(long ms) {
                long m = ms / 60000, s = (ms / 1000) % 60;
                binding.laundryTimer.setText(String.format("Осталось: %02d:%02d", m, s));
            }
            @Override public void onFinish() { binding.laundryTimer.setText(""); }
        }.start();
    }

    private void showMinutesDialogAndStart() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Сколько минут стирки?")
                .setItems(new String[]{"30","45","60","90"}, (d, w) -> {
                    int mins = Integer.parseInt(new String[]{"30","45","60","90"}[w]);
                    laundryManager.startWashing(myUid, mins);
                })
                .show();
    }

    private int parseMinutes(String s) {
        try { return Integer.parseInt(s.trim()); }
        catch (Exception e) { return 0; }
    }



    private void sendServiceRequest() {
        String problem = editProblem.getText().toString().trim();
        String type = spinnerServiceType.getSelectedItem().toString();

        if (problem.isEmpty()) {
            serviceInfo.setText("Пожалуйста, опишите проблему.");
            return;
        }
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);
        userRef.child("room").get().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || !task.getResult().exists()) {
                serviceInfo.setText("Не удалось получить номер комнаты.");
                return;
            }
            String room = task.getResult().getValue(String.class);
            if (room == null || room.isEmpty()) {
                serviceInfo.setText("Комната не указана в профиле.");
                return;
            }
            String requestId = FirebaseDatabase.getInstance().getReference()
                    .child("ServiceRequests")
                    .push()
                    .getKey();

            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("room", room);
            requestMap.put("problem", problem);
            requestMap.put("type", type);
            requestMap.put("status", "Ожидает обработки");
            requestMap.put("timestamp", ServerValue.TIMESTAMP);
            requestMap.put("userId", uid); // Важно!
            FirebaseDatabase.getInstance()
                    .getReference("ServiceRequests")
                    .child(requestId)
                    .setValue(requestMap)
                    .addOnSuccessListener(aVoid -> {
                        serviceInfo.setText("Заявка принята, ожидайте.");
                        editProblem.setText("");
                    })
                    .addOnFailureListener(e -> {
                        serviceInfo.setText("Ошибка отправки: " + e.getMessage());
                    });

        });
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (laundryListener != null) {
            laundryManager.removeStatusListener(laundryListener);
        }
        DatabaseReference notiRef = FirebaseDatabase.getInstance()
                .getReference("UserNotifications")
                .child(myUid);
        if (notificationListener != null) {
            notiRef.removeEventListener(notificationListener);
        }
        laundryManager.cleanup();
        roomDataManager.cleanup();
        if (uiTimer != null) {
            uiTimer.cancel();
            uiTimer = null;
        }
        binding = null;
    }

}
