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
import com.example.alinadiplom.utils.NotificationHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;


public class RequestsManagementFragment extends Fragment {
    private RecyclerView recyclerView;
    private List<ServiceRequest> requestList = new ArrayList<>();
    private RequestsManagementAdapter adapter;

    private DatabaseReference requestsRef;          // <‑‑ ссылка на ветку
    private ValueEventListener requestsListener;    // <‑‑ живой слушатель
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_requests_management, container, false);


        recyclerView = view.findViewById(R.id.recyclerViewServiceRequests);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new RequestsManagementAdapter(
                requestList,
                new RequestsManagementAdapter.OnAcceptClickListener() {
                    @Override public void onAccept(ServiceRequest r) { onAcceptClick(r); }
                    @Override public void onReport(ServiceRequest r) { reportRequest(r); }
                });
        recyclerView.setAdapter(adapter);

        // Ссылка на базу
        requestsRef = FirebaseDatabase.getInstance().getReference("ServiceRequests");
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
    private void sendPushToUser(@NonNull String token, @NonNull String title,
                                @NonNull String body) {
        JSONObject payload = new JSONObject();
        JSONObject notification = new JSONObject();
        try {
            notification.put("title", title);
            notification.put("body",  body);
            payload.put("to", token);
            payload.put("notification", notification);
        } catch (Exception e) { e.printStackTrace(); return; }

        Request request = new Request.Builder()
                .url("https://fcm.googleapis.com/fcm/send")
                .addHeader("Authorization", "key=YOUR_SERVER_KEY")
                .post(RequestBody.create(payload.toString(),
                        MediaType.parse("application/json; charset=utf-8")))
                .build();

        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) { e.printStackTrace(); }
            @Override public void onResponse(Call call, @NonNull Response r) { r.close(); }
        });
    }
    private void onAcceptClick(ServiceRequest request) {
        DatabaseReference reqRef = FirebaseDatabase.getInstance()
                .getReference("ServiceRequests")
                .child(request.getRequestId());
        Map<String, Object> update = new HashMap<>();
        update.put("status", "Заявка принята. Ожидайте.");
        reqRef.updateChildren(update).addOnSuccessListener(unused -> {
            Toast.makeText(getContext(), "Заявка принята", Toast.LENGTH_SHORT).show();
            String userId = request.getUserId();
            if (userId == null || userId.isEmpty()) return;
            String notifId = FirebaseDatabase.getInstance()
                    .getReference("UserNotifications")
                    .child(userId)
                    .push()
                    .getKey();
            if (notifId == null) return;
            Map<String, Object> notif = new HashMap<>();
            notif.put("title", "Заявка принята");
            notif.put("message",
                    "Заявка по проблеме \"" + request.getProblem() + "\" принята. Ожидайте.");
            notif.put("timestamp", ServerValue.TIMESTAMP);
            FirebaseDatabase.getInstance()
                    .getReference("UserNotifications")
                    .child(userId)
                    .child(notifId)
                    .setValue(notif);
            FirebaseDatabase.getInstance().getReference("Users")
                    .child(userId)
                    .child("fcmToken")
                    .get()
                    .addOnSuccessListener(ds -> {
                        String token = ds.getValue(String.class);
                        if (token != null && !token.isEmpty()) {
                            sendPushToUser(token,
                                    "Заявка принята",
                                    "Мастер уже в пути!");
                        }
                    });
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(),
                    "Ошибка при обновлении заявки: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        });
    }
    private void reportRequest(ServiceRequest req) {
        String id = req.getRequestId();
        if (id == null) {
            Toast.makeText(getContext(),
                    "Нельзя создать отчёт: нет ID заявки",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        Map<String,Object> report = new HashMap<>();
        report.put("requestId",   id);
        report.put("processedAt", ServerValue.TIMESTAMP);
        report.put("processedBy",
                FirebaseAuth.getInstance().getCurrentUser().getUid());
        report.put("details", req);

        FirebaseDatabase.getInstance()
                .getReference("Reports")
                .child(id)
                .setValue(report)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(getContext(),
                                "Отчёт сохранён", Toast.LENGTH_SHORT).show()
                ).addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Ошибка отчёта: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }
    private void attachRealtimeListener() {
        requestsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                requestList.clear();
                for (DataSnapshot child : snap.getChildren()) {
                    ServiceRequest req = child.getValue(ServiceRequest.class);
                    if (req != null) {
                        req.setRequestId(child.getKey());
                        requestList.add(req);
                    }
                }
                // сортируем по времени (новые сверху)
                requestList.sort((a, b) ->
                        Long.compare(b.getTimestamp(), a.getTimestamp()));
                adapter.notifyDataSetChanged();      // обновляем RecyclerView
            }
            @Override public void onCancelled(@NonNull DatabaseError err) { }
        };
        // слушаем любые изменения
        requestsRef.addValueEventListener(requestsListener);
    }
    @Override
    public void onStart() {
        super.onStart();
        attachRealtimeListener();
    }
    @Override
    public void onStop() {
        super.onStop();
        if (requestsRef != null && requestsListener != null) {
            requestsRef.removeEventListener(requestsListener);   // отписываемся
        }
    }

}