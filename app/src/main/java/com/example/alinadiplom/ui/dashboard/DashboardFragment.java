package com.example.alinadiplom.ui.dashboard;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.alinadiplom.R;
import com.example.alinadiplom.databinding.FragmentDashboardBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private String currentDorm;
    private String currentRoom;
    private CountDownTimer timer;
    private ProgressBar progressBar;
    private boolean isNotificationEnabled;
    private String laundryId = "washer1";
    private long remainingTime = 0;
    private long totalTime = 0;
    private ValueEventListener roomDataListener;
    private ValueEventListener roommatesListener;
    private ValueEventListener laundryListener;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // Инициализация ViewModel
        DashboardViewModel dashboardViewModel =
                new ViewModelProvider(this).get(DashboardViewModel.class);

        // Инициализация View Binding
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Инициализация Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        progressBar = binding.progressBar;

        // Создание канала уведомлений
        createNotificationChannel();

        // Загружаем данные комнаты и соседей
        loadRoomData();

        // Проверяем статус стиралки при запуске
        checkLaundryStatus();

        // Обработка клика на "Занять"
        binding.buttonOccupy.setOnClickListener(v -> {
            if (binding != null) {
                binding.groupView.setVisibility(View.GONE);
                binding.laundryView.setVisibility(View.VISIBLE);
                updateLaundryStatus();
            }
        });

        // Обработка "Назад" из расширенного вида
        binding.buttonBackExpanded.setOnClickListener(v -> {
            if (binding != null) {
                stopTimer();
                binding.groupView.setVisibility(View.VISIBLE);
                binding.laundryView.setVisibility(View.GONE);
                progressBar.setProgress(0);
            }
        });

        // Обработка "Начать стирку"
        binding.buttonStartWash.setOnClickListener(v -> {
            if (binding != null) {
                String timerInput = binding.editTimer.getText().toString().trim();
                if (timerInput.isEmpty()) {
                    Toast.makeText(getContext(), "Введите время стирки", Toast.LENGTH_SHORT).show();
                    return;
                }
                int minutes = Integer.parseInt(timerInput);
                long duration = minutes * 60 * 1000;
                startWashing(duration);
                Toast.makeText(getContext(), "Стирка началась", Toast.LENGTH_SHORT).show();
            }
        });

        // Обработка переключателя уведомлений
        binding.checkNotify.setOnClickListener(v -> {
            if (binding != null) {
                isNotificationEnabled = binding.checkNotify.isChecked();
                Toast.makeText(getContext(), "Уведомления " + (isNotificationEnabled ? "включены" : "выключены"), Toast.LENGTH_SHORT).show();
            }
        });

        // Обработка бронирования
        binding.buttonReserve.setOnClickListener(v -> reserveLaundry());

        // Обработка добавления заметки
        binding.btnAddNote.setOnClickListener(v -> addNote());

        return root;
    }

    private void loadRoomData() {
        String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (uid == null) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Пользователь не авторизован", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        mDatabase.child("Users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (binding == null) return;
                currentDorm = snapshot.child("dorm").getValue(String.class);
                currentRoom = snapshot.child("room").getValue(String.class);
                if (currentDorm != null && currentRoom != null) {
                    binding.roomNumber.setText(currentDorm + " / " + currentRoom);
                    loadRoommates(currentDorm, currentRoom);
                } else {
                    binding.roomNumber.setText("Не указано");
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Общежитие или комната не указаны", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Ошибка загрузки данных комнаты", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateLaundryStatus() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId == null) return;
        Map<String, Object> data = new HashMap<>();
        data.put("status", "occupied");
        data.put("userId", userId);
        mDatabase.child("laundry").child(laundryId).updateChildren(data);
    }

    private void loadRoommates(String dorm, String room) {
        if (binding == null || dorm == null || room == null) return;
        binding.roommatesContainer.removeAllViews();
        String safeRoomKey = room.replace("/", "\\");
        roommatesListener = mDatabase.child("rooms").child(dorm).child(safeRoomKey).child("residents").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (binding == null) return;
                if (!snapshot.exists()) {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Соседи не найдены", Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
                for (DataSnapshot residentSnapshot : snapshot.getChildren()) {
                    String name = residentSnapshot.getValue(String.class);
                    if (name != null) {
                        addRoommate(name);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Ошибка загрузки соседей", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void addRoommate(String name) {
        if (binding == null) return;
        LinearLayout roommateLayout = new LinearLayout(getContext());
        roommateLayout.setOrientation(LinearLayout.HORIZONTAL);
        roommateLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        roommateLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        roommateLayout.setPadding(0, 0, 16, 0);

        ImageView avatar = new ImageView(getContext());
        avatar.setLayoutParams(new LinearLayout.LayoutParams(40, 40));
        avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);

        TextView nameView = new TextView(getContext());
        nameView.setText(name);
        nameView.setTextSize(14);
        nameView.setPadding(8, 0, 0, 0);

        roommateLayout.addView(avatar);
        roommateLayout.addView(nameView);
        binding.roommatesContainer.addView(roommateLayout);
    }

    private void checkLaundryStatus() {
        if (binding == null) return;
        laundryListener = mDatabase.child("laundry").child(laundryId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (binding == null) return;
                String status = snapshot.child("status").getValue(String.class);
                Long startTime = snapshot.child("startTime").getValue(Long.class);
                Long duration = snapshot.child("duration").getValue(Long.class);
                String userId = snapshot.child("userId").getValue(String.class);

                StringBuilder queueText = new StringBuilder();
                DataSnapshot reservations = snapshot.child("reservations");
                int queuePosition = 0;
                String currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
                for (DataSnapshot reservation : reservations.getChildren()) {
                    String queuedUserId = reservation.getValue(String.class);
                    queueText.append("Пользователь ").append(queuedUserId).append("\n");
                    if (queuedUserId != null && currentUserId != null && queuedUserId.equals(currentUserId)) {
                        queuePosition++;
                    }
                }
                binding.reservationQueue.setText(queueText.length() > 0 ? "Очередь:\n" + queueText : "");
                if (queuePosition > 0) {
                    binding.reservationQueue.append("\nВы находитесь в очереди: " + queuePosition);
                }

                if (status == null || status.equals("available")) {
                    mDatabase.child("laundry").child(laundryId).child("status").setValue("available");
                    binding.laundryTitle.setText("Стиралка № 1 ○");
                    binding.laundryExpandedTitle.setText("Стиралка №1 ○");
                    binding.buttonOccupy.setVisibility(View.VISIBLE);
                    binding.buttonReserve.setVisibility(View.GONE);
                    stopTimer();
                    progressBar.setProgress(0);
                    notifyNextInQueue();
                } else {
                    binding.buttonOccupy.setVisibility(View.GONE);
                    binding.buttonReserve.setVisibility(View.VISIBLE);
                    if (userId != null && currentUserId != null && userId.equals(currentUserId)) {
                        binding.buttonOccupy.setVisibility(View.GONE);
                        binding.buttonReserve.setVisibility(View.GONE);
                        if (startTime != null && duration != null) {
                            long currentTime = System.currentTimeMillis();
                            long elapsed = currentTime - startTime;
                            remainingTime = duration - elapsed;
                            totalTime = duration;
                            if (remainingTime > 0) {
                                binding.groupView.setVisibility(View.GONE);
                                binding.laundryView.setVisibility(View.VISIBLE);
                                startTimer(remainingTime);
                            } else {
                                resetLaundry();
                            }
                        }
                    } else {
                        binding.laundryTitle.setText("Стиралка № 1 ● (Занята)");
                        binding.laundryExpandedTitle.setText("Стиралка №1 ● (Занята)");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Ошибка загрузки статуса", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void startWashing(long duration) {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId == null) return;
        Map<String, Object> laundryData = new HashMap<>();
        laundryData.put("status", "occupied");
        laundryData.put("userId", userId);
        laundryData.put("startTime", System.currentTimeMillis());
        laundryData.put("duration", duration);

        mDatabase.child("laundry").child(laundryId).updateChildren(laundryData)
                .addOnSuccessListener(aVoid -> startTimer(duration))
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void startTimer(long millisInFuture) {
        if (binding == null) return;
        stopTimer();
        totalTime = millisInFuture;
        remainingTime = millisInFuture;
        progressBar.setMax(100);
        progressBar.setProgress(100);

        timer = new CountDownTimer(millisInFuture, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (binding == null) return;
                remainingTime = millisUntilFinished;
                int progress = (int) (millisUntilFinished * 100 / totalTime);
                progressBar.setProgress(progress);
                binding.laundryExpandedTitle.setText("Стиралка №1 ● (" + millisUntilFinished / 1000 + "s left)");
                binding.laundryTitle.setText("Стиралка № 1 ● (" + millisUntilFinished / 1000 + "s left)");
            }

            @Override
            public void onFinish() {
                if (binding == null) return;
                resetLaundry();
                if (isNotificationEnabled) {
                    sendNotification("Стирка завершена. Освободите машинку.");
                }
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Стирка завершена", Toast.LENGTH_SHORT).show();
                }
            }
        }.start();
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void resetLaundry() {
        Map<String, Object> resetData = new HashMap<>();
        resetData.put("status", "available");
        resetData.put("userId", null);
        resetData.put("startTime", null);
        resetData.put("duration", null);
        mDatabase.child("laundry").child(laundryId).updateChildren(resetData);
    }

    private void reserveLaundry() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId == null) return;
        DatabaseReference reservationRef = mDatabase.child("laundry").child(laundryId).child("reservations").push();
        reservationRef.setValue(userId)
                .addOnSuccessListener(aVoid -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Вы добавлены в очередь", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Ошибка бронирования: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void notifyNextInQueue() {
        DatabaseReference reservationsRef = mDatabase.child("laundry").child(laundryId).child("reservations");
        reservationsRef.limitToFirst(1).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot reservation : snapshot.getChildren()) {
                        String userId = reservation.getValue(String.class);
                        String currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
                        if (userId != null && currentUserId != null && userId.equals(currentUserId)) {
                            sendNotification("Ваша очередь использовать стиралку!");
                        }
                        reservation.getRef().removeValue();
                        break;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Ошибка обработки очереди", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            CharSequence name = "LaundryChannel";
            String description = "Channel for laundry notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("laundry_channel", name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = requireContext().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void sendNotification(String message) {
        Intent intent = new Intent(getContext(), DashboardFragment.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(getContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), "laundry_channel")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Стиральная машина")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager notificationManager = requireContext().getSystemService(NotificationManager.class);
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void addNote() {
        EditText input = new EditText(getContext());
        input.setHint("Введите заметку");

        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Новая заметка")
                .setView(input)
                .setPositiveButton("Добавить", (dialog, which) -> {
                    String noteText = input.getText().toString().trim();
                    if (!noteText.isEmpty()) {
                        addNoteToUI(noteText);
                        saveNoteToFirebase(noteText);
                    } else {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Заметка не может быть пустой", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void addNoteToUI(String noteText) {
        if (binding == null) return;
        TextView noteView = new TextView(getContext());
        noteView.setText(noteText);
        noteView.setTextSize(14);
        noteView.setPadding(8, 8, 8, 8);
        noteView.setBackgroundResource(android.R.drawable.list_selector_background);
        binding.tasksContainer.addView(noteView);
    }

    private void saveNoteToFirebase(String noteText) {
        String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (uid == null) return;
        Map<String, Object> noteData = new HashMap<>();
        noteData.put("text", noteText);
        noteData.put("timestamp", System.currentTimeMillis());
        mDatabase.child("users").child(uid).child("notes").push().setValue(noteData)
                .addOnSuccessListener(aVoid -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Заметка сохранена", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Ошибка сохранения: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (roomDataListener != null) {
            String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
            if (uid != null) {
                mDatabase.child("Users").child(uid).removeEventListener(roomDataListener);
            }
        }
        if (roommatesListener != null && currentDorm != null && currentRoom != null) {
            String safeRoomKey = currentRoom.replace("/", "\\");
            mDatabase.child("rooms").child(currentDorm).child(safeRoomKey).child("residents").removeEventListener(roommatesListener);
        }
        if (laundryListener != null) {
            mDatabase.child("laundry").child(laundryId).removeEventListener(laundryListener);
        }
        stopTimer();
        binding = null;
    }
}