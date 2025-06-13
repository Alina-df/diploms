package com.example.alinadiplom.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import androidx.appcompat.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alinadiplom.AddEventActivity;
import com.example.alinadiplom.AddNoticeActivity;
import com.example.alinadiplom.SearchFragment;
import com.example.alinadiplom.model.Event;
import com.example.alinadiplom.adapter.EventAdapter;
import com.example.alinadiplom.model.Notice;
import com.example.alinadiplom.adapter.NotificationAdapter;
import com.example.alinadiplom.adapter.PeopleAdapter;
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
    private static final String TAG = "HomeFragment";

    private FragmentHomeBinding binding;
    private NotificationAdapter notificationAdapter;
    private PeopleAdapter peopleAdapter;
    private EventAdapter eventAdapter;
    private RecyclerView recyclerView;
    private SearchView searchView;

    private List<Notice> adminNotices = new ArrayList<>();
    private List<Notice> peopleNotices = new ArrayList<>();
    private ValueEventListener noticesListener;
    private boolean isAdmin = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Настраиваем RecyclerView для объявлений/услуг
        recyclerView = root.findViewById(R.id.notificationRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Сначала инициализируем адаптеры (будут обновлены после получения роли)
        notificationAdapter = new NotificationAdapter(adminNotices, isAdmin);
        peopleAdapter = new PeopleAdapter(peopleNotices, isAdmin);
        recyclerView.setAdapter(notificationAdapter);
        searchView = root.findViewById(R.id.searchView);

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (uid != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("Users")
                    .child(uid)
                    .child("role");

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    // Сразу же проверяем binding
                    if (binding == null) return;

                    String role = snapshot.getValue(String.class);
                    isAdmin = "admin".equals(role);
                    Log.d(TAG, "User is admin: " + isAdmin);
                    // Пересоздаем адаптеры с учётом isAdmin
                    notificationAdapter = new NotificationAdapter(adminNotices, isAdmin);
                    peopleAdapter = new PeopleAdapter(peopleNotices, isAdmin);

                    // Показываем правильный адаптер в соответствии с текущим выделенным табом
                    int colorNotices = getResources().getColor(R.color.blue);
                    if (binding.tabNotices.getCurrentTextColor() == colorNotices) {
                        recyclerView.setAdapter(notificationAdapter);
                    } else {
                        recyclerView.setAdapter(peopleAdapter);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    if (getContext() != null) {
                        Toast.makeText(getContext(),
                                "Ошибка проверки прав: " + error.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                    Log.e(TAG, "Admin check error: " + error.getMessage());
                }
            });
        }

        // Инициализируем табы
        binding.tabNotices.setOnClickListener(v -> {
            binding.tabNotices.setTextColor(getResources().getColor(R.color.blue));
            binding.tabPeople.setTextColor(getResources().getColor(R.color.gray));
            recyclerView.setAdapter(notificationAdapter);
            Log.d(TAG, "Switched to Объявления tab, admin notices: " + adminNotices.size());
        });

        binding.tabPeople.setOnClickListener(v -> {
            binding.tabNotices.setTextColor(getResources().getColor(R.color.gray));
            binding.tabPeople.setTextColor(getResources().getColor(R.color.blue));
            recyclerView.setAdapter(peopleAdapter);
            Log.d(TAG, "Switched to Люди tab, people notices: " + peopleNotices.size());
        });

        // RecyclerView для мероприятий (горизонтальный)
        RecyclerView eventsRecyclerView = root.findViewById(R.id.eventsRecyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(
                getContext(), LinearLayoutManager.HORIZONTAL, false);
        eventsRecyclerView.setLayoutManager(layoutManager);

        List<Event> eventList = new ArrayList<>();
        eventAdapter = new EventAdapter(getContext(), eventList);
        eventsRecyclerView.setAdapter(eventAdapter);

        // Загрузка мероприятий из Firebase
        loadEventsFromFirebase(eventAdapter, eventList);

        // Кнопки “Добавить объявление” и “Добавить мероприятие”
        ImageButton addNoticeBtn = root.findViewById(R.id.addNoticeButton);
        addNoticeBtn.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), AddNoticeActivity.class));
        });

        ImageButton addEventBtn = root.findViewById(R.id.addEventButton);
        if (isAdmin){
            addEventBtn.setVisibility(View.VISIBLE);
        }
        addEventBtn.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), AddEventActivity.class));
        });
        searchView.setOnClickListener(v -> {
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, new SearchFragment());
            transaction.addToBackStack(null);
            transaction.commit();
        });

        // Наконец, запускаем загрузку объявлений/услуг
        loadNoticesFromFirebase();

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        // При возвращении во фрагмент обновляем списки
        if (noticeListenerExists()) {
            loadNoticesFromFirebase();
        }
        loadEventsFromFirebase(eventAdapter, new ArrayList<>());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Перед обнулением binding обязательно отписаться от слушателей!
        if (noticesListener != null) {
            FirebaseDatabase.getInstance()
                    .getReference("notices")
                    .removeEventListener(noticesListener);
            noticesListener = null;
        }
        binding = null;
    }

    private void loadEventsFromFirebase(EventAdapter adapter, List<Event> eventList) {
        DatabaseReference eventsRef =
                FirebaseDatabase.getInstance().getReference("events");

        eventsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Проверяем binding
                if (binding == null) return;

                eventList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Event event = ds.getValue(Event.class);
                    if (event != null) {
                        eventList.add(event);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Ошибка загрузки мероприятий: " + error.getMessage());
            }
        });
    }

    private void loadNoticesFromFirebase() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("notices");

        // Если предыдущий слушатель существует, удаляем его
        if (noticesListener != null) {
            ref.removeEventListener(noticesListener);
        }

        noticesListener = ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (binding == null) return;

                adminNotices.clear();
                peopleNotices.clear();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    try {
                        Notice notice = ds.getValue(Notice.class);
                        if (notice == null) continue;

                        // Ключ узла — это adId!
                        notice.id = ds.getKey();

                        // Остальные поля заполняются автоматически через ds.getValue(Notice.class)
                        Boolean isAdminNode = ds.child("isAdmin").getValue(Boolean.class);
                        notice.isAdmin = isAdminNode != null && isAdminNode;

                        if (notice.isAdmin) {
                            adminNotices.add(notice);
                        } else {
                            if (notice.userName != null && notice.body != null) {
                                peopleNotices.add(notice);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing notice: " + ds.getKey() + " | " + e.getMessage());
                    }
                }

                notificationAdapter.notifyDataSetChanged();
                peopleAdapter.notifyDataSetChanged();

                int colorNotices = getResources().getColor(R.color.blue);
                if (binding.tabNotices.getCurrentTextColor() == colorNotices) {
                    recyclerView.setAdapter(notificationAdapter);
                } else {
                    recyclerView.setAdapter(peopleAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (binding != null && getContext() != null) {
                    Toast.makeText(getContext(),
                            "Ошибка загрузки объявлений: " + error.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
                Log.e(TAG, "Firebase error: " + error.getMessage());
            }
        });
    }

    private boolean noticeListenerExists() {
        return noticesListener != null;
    }
}
