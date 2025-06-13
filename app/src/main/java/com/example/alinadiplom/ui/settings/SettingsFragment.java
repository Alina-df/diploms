package com.example.alinadiplom.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.alinadiplom.R;

public class SettingsFragment extends Fragment {

    private Button btnNotifications, btnAccount, btnDormitory, btnHelp;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        btnNotifications = view.findViewById(R.id.btnSettingsNotifications);
        btnAccount = view.findViewById(R.id.btnSettingsAccount);
        btnDormitory = view.findViewById(R.id.btnSettingsDormitory);
        btnHelp = view.findViewById(R.id.btnSettingsHelp);

        // Переход к уведомлениям
        btnNotifications.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_settingsFragment_to_NotificationsSettingsFragment));

        // Переход к аккаунту
        btnAccount.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_settingsFragment_to_AccountSettingsFragment));

        // Переход к общежитию (у тебя уже есть)
        btnDormitory.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_settingsFragment_to_DormInfoFragment));

        // Переход к помощи
        btnHelp.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_settingsFragment_to_HelpFragment));

        return view;
    }
}
