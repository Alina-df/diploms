package com.example.alinadiplom.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.alinadiplom.R;
import com.example.alinadiplom.databinding.FragmentNotificationsBinding; // Исправлено на правильный binding
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileFragment extends Fragment {

    private TextView tvFio, tvUniversity, tvFaculty, tvDorm, tvRoom;
    private Button btnSettings;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private FragmentNotificationsBinding binding; // Исправлено на правильный binding

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // Используем правильный ViewModel (предполагаем, что ProfilesViewModel не существует)
        ProfileViewModel profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        // Используем правильный binding для fragment_profile.xml
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Инициализация виджетов
        tvFio = binding.tvFio; // Используем binding для доступа к виджетам
        tvUniversity = binding.tvUniversity;
        tvFaculty = binding.tvFaculty;
        tvDorm = binding.tvDorm;
        tvRoom = binding.tvRoom;
        btnSettings = binding.btnSettings;

        // Инициализация Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Загрузка данных пользователя
        String uid = mAuth.getCurrentUser().getUid();
        mDatabase.child("Users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String fio = snapshot.child("fio").getValue(String.class);
                    String university = snapshot.child("university").getValue(String.class);
                    String faculty = snapshot.child("faculty").getValue(String.class);
                    String dorm = snapshot.child("dorm").getValue(String.class);
                    String room = snapshot.child("room").getValue(String.class);

                    tvFio.setText(fio != null ? fio : "Не указано");
                    tvUniversity.setText(university != null ? university : "Не указано");
                    tvFaculty.setText(faculty != null ? faculty : "Не указано");
                    tvDorm.setText(dorm != null ? dorm : "Не указано");
                    tvRoom.setText(room != null ? room : "Не указано");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
            }
        });

        // Переход на SettingsFragment
        btnSettings.setOnClickListener(v -> {
            NavHostFragment.findNavController(this).navigate(R.id.action_profile_to_settings);
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}