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
import com.example.alinadiplom.security.CryptoHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileFragment extends Fragment {

    private TextView tvFio, tvUniversity, tvFaculty, tvDorm, tvRoom;
    private Button btnSettings, btnDormInfo, btnAdminRequest;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private FragmentNotificationsBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        ProfileViewModel profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        tvFio = binding.tvFio;
        tvFaculty = binding.tvFaculty;
        tvDorm = binding.tvDormName;
        tvRoom = binding.tvRoom;
        btnSettings = binding.btnSettings;
        btnAdminRequest = binding.btnAdminRequest;

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        String uid = mAuth.getCurrentUser().getUid();

        // Изначально скрываем кнопку
        btnAdminRequest.setVisibility(View.GONE);

        // Загружаем данные пользователя и проверяем роль
        mDatabase.child("Users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String fio = null;
                    try {
                        fio = CryptoHelper.decrypt(snapshot.child("fio").getValue(String.class));
                    } catch (Exception e) {
                        fio = (snapshot.child("fio").getValue(String.class));
                    }
                    String faculty = snapshot.child("faculty").getValue(String.class);
                    String dorm = snapshot.child("dorm").getValue(String.class);
                    String room = snapshot.child("room").getValue(String.class);

                    tvFio.setText(fio != null ? fio : "Не указано");
                    tvFaculty.setText(faculty != null ? faculty : "Не указано");
                    tvDorm.setText(dorm != null ? dorm : "Не указано");
                    tvRoom.setText(room != null ? room : "Не указано");

                    // Проверяем роль и показываем кнопку если admin
                    String role = snapshot.child("role").getValue(String.class);
                    if ("admin".equals(role)) {
                        btnAdminRequest.setVisibility(View.VISIBLE);
                    } else {
                        btnAdminRequest.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
            }
        });

        btnSettings.setOnClickListener(v -> {
            NavHostFragment.findNavController(this).navigate(R.id.action_profile_to_settings);
        });
        btnAdminRequest.setOnClickListener(v -> {
            NavHostFragment.findNavController(this).navigate(R.id.action_navigation_profile_to_AdminFragment);
        });

        return root;
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}