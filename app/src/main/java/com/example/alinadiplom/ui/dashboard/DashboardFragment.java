// Файл: app/src/main/java/com/example/alinadiplom/ui/dashboard/DashboardFragment.java

package com.example.alinadiplom.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
    private FirebaseAuth mAuth;
    private DatabaseReference userNotifRef;
    private ValueEventListener notifListener;
    private NotificationHelper notificationHelper;
    private LaundryManager laundryManager;
    private String myUid;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        DashboardViewModel dashboardViewModel =
                new ViewModelProvider(this).get(DashboardViewModel.class);
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        mAuth = FirebaseAuth.getInstance();
        myUid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (myUid == null) {
            Toast.makeText(getContext(), "Пользователь не авторизован", Toast.LENGTH_SHORT).show();
            return root;
        }
        notificationHelper = new NotificationHelper(requireContext());
        laundryManager = new LaundryManager(
                requireContext(),
                "washer1",
                binding.progressBar,
                notificationHelper
        );
        subscribeToIncomingNotifications();
        loadRoomData();
        checkLaundryStatus();
        binding.buttonOccupy.setOnClickListener(v -> {
            laundryManager.updateLaundryStatus(myUid);
            binding.groupView.setVisibility(View.GONE);
            binding.laundryView.setVisibility(View.VISIBLE);
        });
        binding.buttonBackExpanded.setOnClickListener(v -> {
            laundryManager.stopTimer();
            binding.groupView.setVisibility(View.VISIBLE);
            binding.laundryView.setVisibility(View.GONE);
            binding.progressBar.setProgress(0);
        });
        binding.buttonStartWash.setOnClickListener(v -> {
            String timerInput = binding.editTimer.getText().toString().trim();
            if (timerInput.isEmpty()) {
                Toast.makeText(getContext(), "Введите время стирки (минут)", Toast.LENGTH_SHORT).show();
                return;
            }
            int minutes = Integer.parseInt(timerInput);
            long durationMs = minutes * 60L * 1000L;
            laundryManager.setNotificationEnabled(binding.checkNotify.isChecked());
            laundryManager.startWashing(myUid, durationMs, () -> {
                Toast.makeText(getContext(), "Стирка запущена", Toast.LENGTH_SHORT).show();
            });
        });
        binding.buttonReserve.setOnClickListener(v -> {
            laundryManager.reserveLaundry(myUid);
        });
        binding.btnAddNote.setOnClickListener(v -> addNote());

        return root;
    }
    private void loadRoomData() {
        // TODO: Здесь вы можете вызвать RoomDataManager, если он у вас есть
        // В этом примере оставим как есть – код из вашего старого метода loadRoomData().
    }
    private void subscribeToIncomingNotifications() {
        DatabaseReference root = FirebaseDatabase.getInstance().getReference();
        userNotifRef = root.child("users").child(myUid).child("laundryNotifications");

        notifListener = userNotifRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String message = ds.child("message").getValue(String.class);
                    if (message != null) {
                        Toast.makeText(getContext(),
                                "Прачечная: " + message,
                                Toast.LENGTH_LONG).show();
                    }
                    ds.getRef().removeValue();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { /**/ }
        });
    }
    private void checkLaundryStatus() {
        DatabaseReference laundryRef = FirebaseDatabase.getInstance()
                .getReference("laundry")
                .child("washer1");
        ValueEventListener laundryListener = laundryRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (binding == null) return;
                String status = snapshot.child("status").getValue(String.class);
                String occupiedBy = snapshot.child("userId").getValue(String.class);
                Long startTime = snapshot.child("startTime").getValue(Long.class);
                Long duration = snapshot.child("duration").getValue(Long.class);
                if (status == null || status.equals("available")) {
                    // Если свободна:
                    binding.laundryTitle.setText("Стиралка №1 ○");
                    binding.laundryExpandedTitle.setText("Стиралка №1 ○");
                    binding.buttonOccupy.setVisibility(View.VISIBLE);
                    binding.buttonReserve.setVisibility(View.GONE);
                    laundryManager.stopTimer();
                    binding.progressBar.setProgress(0);
                    laundryManager.notifyNextInQueue();
                } else {
                    binding.buttonOccupy.setVisibility(View.GONE);
                    binding.buttonReserve.setVisibility(View.VISIBLE);
                    if (occupiedBy != null && occupiedBy.equals(myUid)) {
                        if (startTime != null && duration != null) {
                            long elapsed = System.currentTimeMillis() - startTime;
                            long remaining = duration - elapsed;
                            if (remaining > 0) {
                                binding.groupView.setVisibility(View.GONE);
                                binding.laundryView.setVisibility(View.VISIBLE);
                                laundryManager.setNotificationEnabled(binding.checkNotify.isChecked());
                                laundryManager.startWashing(myUid, remaining, null);
                            } else {
                                // Уже просрочено – сбрасываем
                                laundryManager.resetLaundry();
                            }
                        }
                    } else {
                        // Если занята другим – просто показываем “занята” без таймера
                        binding.laundryTitle.setText("Стиралка №1 ● (Занята)");
                        binding.laundryExpandedTitle.setText("Стиралка №1 ● (Занята)");
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { /**/ }
        });
        binding.getRoot().setTag(R.id.laundry_listener_tag, laundryListener);
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
                        Toast.makeText(getContext(),
                                "Заметка не может быть пустой", Toast.LENGTH_SHORT).show();
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
        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("notes")
                .push()
                .setValue(noteData)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(getContext(),
                                "Заметка сохранена", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Ошибка сохранения заметки: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (notifListener != null && userNotifRef != null) {
            userNotifRef.removeEventListener(notifListener);
            notifListener = null;
        }
        ValueEventListener laundryListener = (ValueEventListener) binding.getRoot()
                .getTag(R.id.laundry_listener_tag);
        if (laundryListener != null) {
            FirebaseDatabase.getInstance()
                    .getReference("laundry")
                    .child("washer1")
                    .removeEventListener(laundryListener);
        }
        laundryManager.cleanup();
        binding = null;
    }
}
