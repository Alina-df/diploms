package com.example.alinadiplom.ui.dashboard;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alinadiplom.R;
import com.example.alinadiplom.adapter.ServiceRequestAdapter;
import com.example.alinadiplom.model.ServiceRequest;
import com.example.alinadiplom.utils.NotificationHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class MyRequestsFragment extends Fragment {

    private RecyclerView recyclerView;
    private ServiceRequestAdapter adapter;
    private final List<ServiceRequest> requestList = new ArrayList<>();

    private DatabaseReference notifRef;
    private ValueEventListener notifListener;

    private String uid;
    private String userRoom = "";
    private boolean showOnlyMine = true;   // по умолчанию «Мои»

    /* ------------------- lifecycle ------------------- */

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    @Nullable
    @Override public View onCreateView(@NonNull LayoutInflater inflater,
                                       @Nullable ViewGroup container,
                                       @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_my_requests, container, false);

        recyclerView = v.findViewById(R.id.recyclerRequests);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ServiceRequestAdapter(requestList, this::onLongClickRequest);
        recyclerView.setAdapter(adapter);

        setupNotificationListener();
        fetchUserRoom();

        return v;
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        if (notifRef != null && notifListener != null) {
            notifRef.removeEventListener(notifListener);
        }
    }

    /* ------------------- меню ------------------- */

    @Override public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_my_requests, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_toggle_scope) {
            showOnlyMine = !showOnlyMine;
            item.setTitle(showOnlyMine ? "Показать комнату" : "Показать мои");
            loadRequests();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /* ------------------- загрузка комнаты и заявок ------------------- */

    private void fetchUserRoom() {
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("Users").child(uid);

        userRef.child("room").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                userRoom = snap.getValue(String.class);
                Log.d("MyRequestsFragment", "Комната пользователя: " + userRoom);
                loadRequests();
            }
            @Override public void onCancelled(@NonNull DatabaseError err) {}
        });
    }

    private ValueEventListener requestsListener;

    private void loadRequests() {
        if (userRoom == null || userRoom.isEmpty()) {
            Toast.makeText(getContext(), "Комната не указана в профиле", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("ServiceRequests");

        if (requestsListener != null) {
            ref.removeEventListener(requestsListener); // снять старый слушатель
        }

        requestsListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                requestList.clear();
                for (DataSnapshot ds : snap.getChildren()) {
                    ServiceRequest req = ds.getValue(ServiceRequest.class);
                    if (req == null) continue;
                    req.setRequestId(ds.getKey());

                    if (showOnlyMine && !uid.equals(req.getUserId())) continue;
                    requestList.add(req);
                }
                adapter.notifyDataSetChanged();
                Log.d("MyRequestsFragment", "Всего загружено: " + requestList.size());
            }
            @Override public void onCancelled(@NonNull DatabaseError err) {
                Log.e("MyRequestsFragment", "Ошибка: " + err.getMessage());
            }
        };

        ref.orderByChild("room").equalTo(userRoom)
                .addValueEventListener(requestsListener); // постоянный слушатель
    }

    /* ------------------- long‑tap удалить ------------------- */

    private void onLongClickRequest(ServiceRequest req) {
        if (!userRoom.equals(req.getRoom())) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Удалить заявку?")
                .setMessage("Удалить заявку \"" + req.getProblem() + "\"?")
                .setPositiveButton("Удалить", (d,i)->deleteRequest(req))
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void deleteRequest(ServiceRequest req) {
        FirebaseDatabase.getInstance()
                .getReference("ServiceRequests")
                .child(req.getRequestId())
                .removeValue()
                .addOnSuccessListener(a ->
                        Toast.makeText(getContext(), "Удалено", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /* ------------------- уведомления ------------------- */

    private void setupNotificationListener() {
        notifRef = FirebaseDatabase.getInstance()
                .getReference("UserNotifications")
                .child(uid);

        notifListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                for (DataSnapshot n : snap.getChildren()) {
                    String title = n.child("title").getValue(String.class);
                    String msg   = n.child("message").getValue(String.class);
                    if (title != null && msg != null) {
                        NotificationHelper.showLocalNotification(getContext(), title, msg);
                    }
                    n.getRef().removeValue();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError err) {}
        };
        notifRef.addValueEventListener(notifListener);
    }
}
