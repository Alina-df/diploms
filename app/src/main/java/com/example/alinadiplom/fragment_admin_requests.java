package com.example.alinadiplom;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class fragment_admin_requests extends Fragment {

    private RecyclerView recycler;
    private ProgressBar progress;
    private AdminRequestsAdapter adapter;
    private List<AdminRequest> list = new ArrayList<>();

    private DatabaseReference pendingRef;
    private DatabaseReference adminsRef;
    private String uid;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_requests, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recycler = view.findViewById(R.id.requestsRecyclerView);
        progress = view.findViewById(R.id.progressBar);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        pendingRef = FirebaseDatabase.getInstance()
                .getReference("pendingAdmins");
        adminsRef  = FirebaseDatabase.getInstance()
                .getReference("admins");

        // Сначала проверим, что это админ
        adminsRef.child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snap) {
                        if (!snap.exists()) {
                            // не админ — выталкиваем назад
                            Toast.makeText(getContext(),
                                    "Доступ запрещён", Toast.LENGTH_SHORT).show();
                            NavHostFragment.findNavController(fragment_admin_requests.this)
                                    .popBackStack();
                        } else {
                            setupAdapter();
                            loadRequests();
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {
                        Toast.makeText(getContext(),
                                "Ошибка проверки прав", Toast.LENGTH_SHORT).show();
                        NavHostFragment.findNavController(fragment_admin_requests.this)
                                .popBackStack();
                    }
                });
    }

    private void setupAdapter() {
        adapter = new AdminRequestsAdapter(list,
                new AdminRequestsAdapter.OnActionListener() {
                    @Override
                    public void onApprove(AdminRequest req) {
                        approveRequest(req);
                    }
                    @Override
                    public void onDecline(AdminRequest req) {
                        declineRequest(req);
                    }
                });
        recycler.setAdapter(adapter);
    }

    private void loadRequests() {
        progress.setVisibility(View.VISIBLE);
        pendingRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                list.clear();
                for (DataSnapshot ds : snap.getChildren()) {
                    String reqUid    = ds.getKey();
                    String fio       = ds.child("fio").getValue(String.class);
                    String number    = ds.child("number").getValue(String.class);
                    long   ts        = ds.child("timestamp").getValue(Long.class);
                    list.add(new AdminRequest(reqUid, fio, number, ts));
                }
                adapter.notifyDataSetChanged();
                progress.setVisibility(View.GONE);
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {
                progress.setVisibility(View.GONE);
                Toast.makeText(getContext(),
                        "Ошибка загрузки заявок", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void approveRequest(AdminRequest req) {
        // Собираем данные админа
        Map<String,Object> adminData = new HashMap<>();
        adminData.put("fio",    req.fio);
        adminData.put("number", req.number);
        adminData.put("role",   "admin");

        adminsRef.child(req.uid).setValue(adminData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Удаляем заявку
                        pendingRef.child(req.uid).removeValue();
                        Toast.makeText(getContext(),
                                req.fio + " теперь админ",
                                Toast.LENGTH_SHORT).show();
                        loadRequests();
                    } else {
                        Toast.makeText(getContext(),
                                "Не удалось подтвердить",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void declineRequest(AdminRequest req) {
        pendingRef.child(req.uid).removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(),
                                "Заявка отклонена",
                                Toast.LENGTH_SHORT).show();
                        loadRequests();
                    } else {
                        Toast.makeText(getContext(),
                                "Не удалось отклонить",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
