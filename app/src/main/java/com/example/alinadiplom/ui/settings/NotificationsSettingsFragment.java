package com.example.alinadiplom.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.example.alinadiplom.R;

public class NotificationsSettingsFragment extends Fragment {

    private SwitchCompat switchAnnouncements, switchEvents, switchPush;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications_settings, container, false);

        switchAnnouncements = view.findViewById(R.id.switch_announcements);
        switchEvents = view.findViewById(R.id.switch_events);
        switchPush = view.findViewById(R.id.switch_push);

        // Здесь можно добавить сохранение состояния в SharedPreferences или Firebase

        return view;
    }
}
