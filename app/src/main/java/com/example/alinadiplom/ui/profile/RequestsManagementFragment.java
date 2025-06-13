package com.example.alinadiplom.ui.profile;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.alinadiplom.R;
import com.example.alinadiplom.adapter.RequestsManagementAdapter;
import com.example.alinadiplom.model.ServiceRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class RequestsManagementFragment extends Fragment {
    private RecyclerView recyclerView;
    private List<ServiceRequest> requestList = new ArrayList<>();
    private RequestsManagementAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_requests_management, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewServiceRequests);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new RequestsManagementAdapter(requestList, this::onAcceptClick);
        recyclerView.setAdapter(adapter);

        loadRequests();
        return view;
    }

    private void loadRequests() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("ServiceRequests");

        ref.orderByChild("timestamp").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                requestList.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    ServiceRequest req = snap.getValue(ServiceRequest.class);
                    if (req != null) {
                        req.setRequestId(snap.getKey()); // Чтобы потом обновить по ID
                        requestList.add(req);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void onAcceptClick(ServiceRequest request) {
        // Ссылка на конкретную заявку
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("ServiceRequests")
                .child(request.getRequestId());

        // Обновляем статус заявки
        Map<String, Object> update = new HashMap<>();
        update.put("status", "Заявка принята. Ожидайте.");

        ref.updateChildren(update).addOnSuccessListener(unused -> {
            Toast.makeText(getContext(), "Заявка принята", Toast.LENGTH_SHORT).show();

            // Отправка уведомления пользователю
            String userId = request.getUserId(); // важно: должно быть заполнено при создании заявки
            if (userId != null && !userId.isEmpty()) {
                // Генерация уникального ID уведомления
                String notificationId = FirebaseDatabase.getInstance()
                        .getReference("UserNotifications")
                        .child(userId)
                        .push()
                        .getKey();

                if (notificationId == null) return;

                // Содержимое уведомления
                Map<String, Object> notifMap = new HashMap<>();
                notifMap.put("title", "Заявка принята");
                notifMap.put("message", "Заявка по проблеме \"" + request.getProblem() + "\" принята. Ожидайте.");
                notifMap.put("timestamp", ServerValue.TIMESTAMP);

                // Сохраняем уведомление в Firebase
                FirebaseDatabase.getInstance()
                        .getReference("UserNotifications")
                        .child(userId)
                        .child(notificationId)
                        .setValue(notifMap);
            }

        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Ошибка при обновлении заявки: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }


}