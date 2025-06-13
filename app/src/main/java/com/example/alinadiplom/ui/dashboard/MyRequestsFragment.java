package com.example.alinadiplom.ui.dashboard;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.alinadiplom.R;
import com.example.alinadiplom.adapter.ServiceRequestAdapter;
import com.example.alinadiplom.model.ServiceRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the  factory method to
 * create an instance of this fragment.
 */
public class MyRequestsFragment extends Fragment {
    private RecyclerView recyclerView;
    private ServiceRequestAdapter adapter;
    private List<ServiceRequest> requestList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_requests, container, false);
        recyclerView = view.findViewById(R.id.recyclerRequests);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ServiceRequestAdapter(requestList);
        recyclerView.setAdapter(adapter);

        loadRequests();
        return view;
    }

    private void loadRequests() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("ServiceRequests");
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String room = snapshot.child("room").getValue(String.class);
                    if (room == null || room.isEmpty()) return;

                    // Теперь загружаем заявки только для этой комнаты
                    ref.orderByChild("room").equalTo(room)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    requestList.clear();
                                    for (DataSnapshot snap : snapshot.getChildren()) {
                                        ServiceRequest req = snap.getValue(ServiceRequest.class);
                                        if (req != null) {
                                            requestList.add(req);
                                        }
                                    }
                                    adapter.notifyDataSetChanged();
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {}
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

    }
}
