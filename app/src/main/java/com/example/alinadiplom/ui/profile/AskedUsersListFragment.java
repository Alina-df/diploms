package com.example.alinadiplom.ui.profile;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.alinadiplom.R;
import com.example.alinadiplom.adapter.AskedUsersAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AskedUsersListFragment extends Fragment {

    private RecyclerView recyclerView;
    private AskedUsersAdapter adapter;
    private DatabaseReference chatRef;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_asked_users_list, container, false);
        recyclerView = view.findViewById(R.id.recyclerAskedUsers);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AskedUsersAdapter();
        recyclerView.setAdapter(adapter);

        chatRef = FirebaseDatabase.getInstance().getReference("adminMessages");

        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Set<String> userIds = new HashSet<>();
                for (DataSnapshot chat : snapshot.getChildren()) {
                    String[] ids = chat.getKey().split("_");
                    if (ids.length == 2) {
                        userIds.add(ids[0]);  // ID пользователя, задавшего вопрос
                    }
                }

                List<String> uidList = new ArrayList<>(userIds);
                adapter.setUserIds(uidList); // Передаём список в адаптер
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        return view;
    }
}
