package com.example.alinadiplom.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alinadiplom.AddNoticeActivity;
import com.example.alinadiplom.Event;
import com.example.alinadiplom.EventAdapter;
import com.example.alinadiplom.Notice;
import com.example.alinadiplom.NotificationAdapter;
import com.example.alinadiplom.PeopleAdapter;
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
    private RecyclerView recyclerView;
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

        recyclerView = root.findViewById(R.id.notificationRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Check admin status
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("role");
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String role = snapshot.getValue(String.class);
                    isAdmin = "admin".equals(role);
                    Log.d(TAG, "User is admin: " + isAdmin);
                    // Reinitialize adapters with admin status
                    notificationAdapter = new NotificationAdapter(adminNotices, isAdmin);
                    peopleAdapter = new PeopleAdapter(peopleNotices, isAdmin);
                    // Update RecyclerView based on active tab
                    if (binding.tabNotices.getCurrentTextColor() == getResources().getColor(R.color.blue)) {
                        recyclerView.setAdapter(notificationAdapter);
                    } else {
                        recyclerView.setAdapter(peopleAdapter);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(getContext(), "Ошибка проверки прав: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Admin check error: " + error.getMessage());
                }
            });
        }

        // Initialize adapters (default, will be updated after admin check)
        notificationAdapter = new NotificationAdapter(adminNotices, isAdmin);
        peopleAdapter = new PeopleAdapter(peopleNotices, isAdmin);
        recyclerView.setAdapter(notificationAdapter);

        // Tab switchers
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

        // Events RecyclerView
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

        // Add notice button
        ImageButton addNoticeBtn = root.findViewById(R.id.addNoticeButton);
        addNoticeBtn.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddNoticeActivity.class);
            startActivity(intent);
        });

        // Load notices
        loadNoticesFromFirebase();

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadNoticesFromFirebase();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (noticesListener != null) {
            FirebaseDatabase.getInstance().getReference("notices").removeEventListener(noticesListener);
        }
        binding = null;
    }

    private void loadNoticesFromFirebase() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("notices");
        if (noticesListener != null) {
            ref.removeEventListener(noticesListener);
        }
        noticesListener = ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                adminNotices.clear();
                peopleNotices.clear();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    try {
                        Notice notice = new Notice();
                        notice.id = ds.child("id").getValue(String.class);
                        notice.title = ds.child("title").getValue(String.class);
                        notice.body = ds.child("body").getValue(String.class);
                        notice.date = ds.child("date").getValue(String.class);
                        Boolean isAdmin = ds.child("isAdmin").getValue(Boolean.class);
                        notice.isAdmin = isAdmin != null && isAdmin;
                        notice.userId = ds.child("userId").getValue(String.class);
                        notice.userName = ds.child("userName").getValue(String.class);
                        notice.room = ds.child("room").getValue(String.class);
                        notice.tags = ds.child("tags").getValue(String.class);
                        notice.avatarResId = R.drawable.ic_avatar_placeholder;

                        Log.d(TAG, "Loaded notice: id=" + notice.id + ", isAdmin=" + notice.isAdmin +
                                ", userName=" + notice.userName + ", room=" + notice.room);

                        if (notice.isAdmin) {
                            adminNotices.add(notice);
                        } else {
                            if (notice.userName != null && notice.body != null) {
                                peopleNotices.add(notice);
                            } else {
                                Log.w(TAG, "Skipping invalid individual notice: id=" + notice.id);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing notice: " + ds.getKey() + ", error: " + e.getMessage());
                    }
                }

                Log.d(TAG, "Admin notices: " + adminNotices.size() + ", People notices: " + peopleNotices.size());
                notificationAdapter.notifyDataSetChanged();
                peopleAdapter.notifyDataSetChanged();

                if (binding != null) {
                    if (binding.tabNotices.getCurrentTextColor() == getResources().getColor(R.color.blue)) {
                        recyclerView.setAdapter(notificationAdapter);
                    } else {
                        recyclerView.setAdapter(peopleAdapter);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Ошибка загрузки объявлений: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
                Log.e(TAG, "Firebase error: " + error.getMessage());
            }
        });
    }
}