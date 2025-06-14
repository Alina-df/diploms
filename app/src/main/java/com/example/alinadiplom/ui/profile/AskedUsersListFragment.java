package com.example.alinadiplom.ui.profile;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.example.alinadiplom.R;
import com.example.alinadiplom.adapter.AskedUsersAdapter;
import com.example.alinadiplom.model.ChatMessage;
import com.google.firebase.auth.FirebaseAuth;
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
    private String thisAdminUid;
    Spinner spinner;
    private ValueEventListener waitingListener;
    private boolean waitingMode = true;

    ArrayAdapter<String> spinnerAdapter;
    final String[] filterOptions = {"Только с вопросами", "Все пользователи"};
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_asked_users_list, container, false);

        recyclerView = view.findViewById(R.id.recyclerAskedUsers);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AskedUsersAdapter();
        recyclerView.setAdapter(adapter);
        spinner = view.findViewById(R.id.spinner_filter);
        spinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, filterOptions);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);
        thisAdminUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        chatRef = FirebaseDatabase.getInstance().getReference("adminMessages");
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                if (pos == 0) {           // Только с вопросами
                    if (!waitingMode) {                 // если раньше были «Все»
                        chatRef.addValueEventListener(waitingListener);
                        waitingMode = true;
                    }
                    loadOnlyAskedUsers();
                } else {                  // Все пользователи
                    if (waitingMode) {
                        chatRef.removeEventListener(waitingListener);
                        waitingMode = false;
                    }
                    loadAllUsers();
                }
            }


            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        waitingListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                Set<String> waitingUsers = new HashSet<>();

                for (DataSnapshot chatSnap : snapshot.getChildren()) {
                    String key = chatSnap.getKey();           //  user_admin  (лексикографически)
                    if (key == null) continue;

                    String[] parts = key.split("_");
                    if (parts.length != 2) continue;

                    String uidA = parts[0];
                    String uidB = parts[1];

                    if (!uidA.equals(thisAdminUid) && !uidB.equals(thisAdminUid)) continue;

                    String userUid  = uidA.equals(thisAdminUid) ? uidB : uidA;

                    boolean answered = false;
                    for (DataSnapshot msgSnap : chatSnap.getChildren()) {
                        ChatMessage msg = msgSnap.getValue(ChatMessage.class);
                        if (msg == null) continue;
                        if (!userUid.equals(msg.senderId)) {
                            answered = true;
                            break;
                        }
                    }
                    if (!answered) {
                        waitingUsers.add(userUid);
                    }
                }
                adapter.setUserIds(new ArrayList<>(waitingUsers));
            }

            @Override public void onCancelled(@NonNull DatabaseError error) { }
        };
        chatRef.addValueEventListener(waitingListener);


        return view;
    }
    private void loadOnlyAskedUsers() {
        chatRef = FirebaseDatabase.getInstance().getReference("adminMessages");

        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                Set<String> waitingUserIds = new HashSet<>();

                for (DataSnapshot chatSnap : snapshot.getChildren()) {
                    String key = chatSnap.getKey();
                    if (key == null) continue;

                    String[] parts = key.split("_");
                    if (parts.length != 2) continue;
                    String userId = parts[0];
                    String adminId = parts[1];

                    boolean answered = false;
                    for (DataSnapshot msgSnap : chatSnap.getChildren()) {
                        ChatMessage msg = msgSnap.getValue(ChatMessage.class);
                        if (msg != null && adminId.equals(msg.senderId)) {
                            answered = true;
                            break;
                        }
                    }
                    if (!answered) {
                        waitingUserIds.add(userId);
                    }
                }

                adapter.setUserIds(new ArrayList<>(waitingUserIds));
            }

            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void loadAllUsers() {
        FirebaseDatabase.getInstance()
                .getReference("Users")       // без orderByChild/ equalTo
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<String> all = new ArrayList<>();
                        for (DataSnapshot user : snapshot.getChildren()) {
                            all.add(user.getKey());
                        }
                        adapter.setUserIds(all);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) { }
                });
    }


}
