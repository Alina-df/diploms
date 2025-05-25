package com.example.alinadiplom.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alinadiplom.AddNoticeActivity;
import com.example.alinadiplom.Event;
import com.example.alinadiplom.EventAdapter;
import com.example.alinadiplom.Load_activity;
import com.example.alinadiplom.NotificationAdapter;
import com.example.alinadiplom.NotificationItem;
import com.example.alinadiplom.PeopleAdapter;
import com.example.alinadiplom.PersonItem;
import com.example.alinadiplom.R;
import com.example.alinadiplom.databinding.FragmentHomeBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private NotificationAdapter notificationAdapter;
    private PeopleAdapter peopleAdapter;
    private RecyclerView recyclerView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        recyclerView = root.findViewById(R.id.notificationRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Данные для вкладки "Объявления"
        loadNotificationsFromFirebase();


        // Данные для вкладки "Люди"
        List<PersonItem> peopleList = new ArrayList<>();
        peopleList.add(new PersonItem("1502 комната", "Кузнецова Екатерина", "Привет! Я делаю расклады...", "#таро #нумерология", R.drawable.ic_avatar_placeholder));
        peopleList.add(new PersonItem("1603 комната", "Петров Семён", "Помогаю с мат. анализом...", "#программирование #матан", R.drawable.ic_avatar_placeholder));
        peopleAdapter = new PeopleAdapter(peopleList);

        recyclerView.setAdapter(notificationAdapter); // По умолчанию

        // Переключатели вкладок
        binding.tabNotices.setOnClickListener(v -> {
            binding.tabNotices.setTextColor(getResources().getColor(R.color.blue));
            binding.tabPeople.setTextColor(getResources().getColor(R.color.gray));
            recyclerView.setAdapter(notificationAdapter);
        });

        binding.tabPeople.setOnClickListener(v -> {
            binding.tabNotices.setTextColor(getResources().getColor(R.color.gray));
            binding.tabPeople.setTextColor(getResources().getColor(R.color.blue));
            recyclerView.setAdapter(peopleAdapter);
        });



        RecyclerView eventsRecyclerView = root.findViewById(R.id.eventsRecyclerView);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        eventsRecyclerView.setLayoutManager(layoutManager);

        List<Event> eventList = new ArrayList<>();
        eventList.add(new Event("Киновечер", R.drawable.movie_night));
        eventList.add(new Event("Песни под гитару", R.drawable.guitar_night));
        eventList.add(new Event("Мастер-класс", R.drawable.master_class));
        eventList.add(new Event("Брейншторминг", R.drawable.brainstorming));

        EventAdapter adapter = new EventAdapter(getContext(), eventList);
        eventsRecyclerView.setAdapter(adapter);
        ImageButton addNoticeBtn = root.findViewById(R.id.addNoticeButton);
        addNoticeBtn.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddNoticeActivity.class);
            startActivity(intent);
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
    @Override
    public void onResume() {
        super.onResume();
        loadNotificationsFromFirebase();
    }

    private void loadNotificationsFromFirebase() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("notices");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<NotificationItem> list = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String date = ds.child("date").getValue(String.class);
                    String title = ds.child("title").getValue(String.class);
                    String body = ds.child("body").getValue(String.class); // 'message' → 'body'

                    list.add(new NotificationItem(date, title, body));
                }
                notificationAdapter = new NotificationAdapter(list);

                // Если активна вкладка "Объявления" — отобразить новые данные
                if (binding.tabNotices.getCurrentTextColor() == getResources().getColor(R.color.blue)) {
                    recyclerView.setAdapter(notificationAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Можно показать Toast
            }
        });
    }
}
