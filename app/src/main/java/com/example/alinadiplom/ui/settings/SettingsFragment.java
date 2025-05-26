package com.example.alinadiplom.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.example.alinadiplom.R;
import com.google.firebase.auth.FirebaseAuth;

public class SettingsFragment extends Fragment {

    private EditText etNewPassword;
    private Button btnSavePassword, logoutButton;
    private SwitchCompat switchAnnouncements, switchEvents, switchPush;
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Инициализация виджетов
        etNewPassword = view.findViewById(R.id.etNewPassword);
        btnSavePassword = view.findViewById(R.id.btnSavePassword);
        switchAnnouncements = view.findViewById(R.id.switch_announcements);
        switchEvents = view.findViewById(R.id.switch_events);
        switchPush = view.findViewById(R.id.switch_push);
        logoutButton = view.findViewById(R.id.logoutButton);

        // Инициализация Firebase
        mAuth = FirebaseAuth.getInstance();

        // Обработка сохранения нового пароля
        btnSavePassword.setOnClickListener(v -> {
            String newPassword = etNewPassword.getText().toString().trim();
            if (newPassword.length() < 6) {
                Toast.makeText(getContext(), "Пароль должен быть не менее 6 символов", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.getCurrentUser().updatePassword(newPassword)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(), "Пароль изменен", Toast.LENGTH_SHORT).show();
                            etNewPassword.setText("");
                        } else {
                            Toast.makeText(getContext(), "Ошибка: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        // Обработка выхода (как в вашем NotificationsFragment)
        logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            getActivity().finish();
        });

        // Переключатели можно дополнить логикой позже
        return view;
    }
}