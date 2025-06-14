package com.example.alinadiplom.ui.dashboard;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.alinadiplom.R;
import com.example.alinadiplom.adapter.QueueAdapter;
import com.example.alinadiplom.security.CryptoHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class LaundryQueueFragment extends Fragment {

    private RecyclerView recyclerView;
    private List<String> userQueue = new ArrayList<>();
    private String currentUserId;
    private QueueAdapter adapter;

    private TextView positionText;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_laundry_queue, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewQueue);
        positionText = view.findViewById(R.id.textViewPosition);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new QueueAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        loadQueue();

        return view;
    }

    private void loadQueue() {
        DatabaseReference laundryRef = FirebaseDatabase.getInstance().getReference("laundry");

        laundryRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                String currentLaundryUserId = snapshot.child("userId").getValue(String.class);

                List<Map.Entry<String, Long>> reservationsList = new ArrayList<>();
                DataSnapshot reservationsSnap = snapshot.child("reservations");
                for (DataSnapshot child : reservationsSnap.getChildren()) {
                    String userId = child.child("userId").getValue(String.class);
                    Long timestamp = child.child("timestamp").getValue(Long.class);

                    if (userId != null && timestamp != null) {
                        reservationsList.add(new AbstractMap.SimpleEntry<>(userId, timestamp));
                    }
                }

                Collections.sort(reservationsList, Comparator.comparingLong(Map.Entry::getValue));

                List<String> queueUserIds = new ArrayList<>();
                for (Map.Entry<String, Long> entry : reservationsList) {
                    queueUserIds.add(entry.getKey());
                }

                adapter.updateList(queueUserIds);

                int position = -1;
                for (int i = 0; i < queueUserIds.size(); i++) {
                    if (queueUserIds.get(i).equals(currentUserId)) {
                        position = i + 1;
                        break;
                    }
                }

                // Теперь загрузим данные текущего пользователя, который стирает
                if (currentLaundryUserId != null) {
                    DatabaseReference userRef = FirebaseDatabase.getInstance()
                            .getReference("Users")
                            .child(currentLaundryUserId);

                    int finalPosition = position;
                    userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                            String displayName = "никто";
                            String encryptedFio = userSnapshot.child("fio").getValue(String.class);
                            String room = userSnapshot.child("room").getValue(String.class);
                            if (userSnapshot.exists()) {
                                try {

                                    String fioDecrypted = CryptoHelper.decrypt(encryptedFio);
                                    displayName = fioDecrypted + " " + room;
                                } catch (Exception e) {
                                    displayName = encryptedFio+ " " + room;
                                }
                            }

                            StringBuilder statusText = new StringBuilder();
                            statusText.append("Сейчас стирает: ").append(displayName);
                            if (finalPosition != -1) {
                                statusText.append("\nВаша позиция в очереди: ").append(finalPosition);
                            } else {
                                statusText.append("\nВы не в очереди");
                            }
                            positionText.setText(statusText.toString());
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            // В случае ошибки показа имени подставим только uid
                            StringBuilder statusText = new StringBuilder();
                            statusText.append("Сейчас стирает: ").append(currentLaundryUserId);
                            if (finalPosition != -1) {
                                statusText.append("\nВаша позиция в очереди: ").append(finalPosition);
                            } else {
                                statusText.append("\nВы не в очереди");
                            }
                            positionText.setText(statusText.toString());
                        }
                    });
                } else {
                    // Никто не стирает
                    StringBuilder statusText = new StringBuilder();
                    statusText.append("Сейчас стирает: никто");
                    if (position != -1) {
                        statusText.append("\nВаша позиция в очереди: ").append(position);
                    } else {
                        statusText.append("\nВы не в очереди");
                    }
                    positionText.setText(statusText.toString());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }


}
