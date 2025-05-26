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
import java.util.List;

public class FragmentAdminRequests extends Fragment {

    private RecyclerView recycler;
    private ProgressBar progress;
    private AdminRequestsAdapter adapter;
    private List<AdminRequest> list = new ArrayList<>();
    private DatabaseReference pendingRef;
    private DatabaseReference usersRef;
    private ValueEventListener requestsListener;
    private String uid;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_requests, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recycler = view.findViewById(R.id.requestsRecyclerView);
        progress = view.findViewById(R.id.progressBar);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));

        uid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) {
            Toast.makeText(getContext(), "Пользователь не авторизован", Toast.LENGTH_SHORT).show();
            NavHostFragment.findNavController(this).popBackStack();
            return;
        }

        pendingRef = FirebaseDatabase.getInstance().getReference("pendingAdmins");
        usersRef = FirebaseDatabase.getInstance().getReference("Users");

        // Проверка статуса администратора
        usersRef.child(uid).child("role").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String role = snapshot.getValue(String.class);
                if (!"admin".equals(role)) {
                    Toast.makeText(getContext(), "Доступ запрещён", Toast.LENGTH_SHORT).show();
                    NavHostFragment.findNavController(FragmentAdminRequests.this).popBackStack();
                } else {
                    setupAdapter();
                    loadRequests();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Ошибка проверки прав: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(FragmentAdminRequests.this).popBackStack();
            }
        });
    }

    private void setupAdapter() {
        adapter = new AdminRequestsAdapter(new AdminRequestsAdapter.OnActionListener() {
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
        requestsListener = pendingRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<AdminRequest> newList = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String reqUid = ds.getKey();
                    String fio = ds.child("fio").getValue(String.class);
                    String number = ds.child("number").getValue(String.class);
                    Long timestamp = ds.child("timestamp").getValue(Long.class);
                    if (reqUid != null && fio != null && number != null) {
                        newList.add(new AdminRequest(reqUid, fio, number, timestamp != null ? timestamp : 0));
                    }
                }
                adapter.updateRequests(newList);
                progress.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progress.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Ошибка загрузки заявок: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void approveRequest(AdminRequest req) {
        // Обновляем роль пользователя
        usersRef.child(req.uid).child("role").setValue("admin")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Удаляем заявку
                        pendingRef.child(req.uid).removeValue()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(getContext(), req.fio + " теперь администратор", Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Toast.makeText(getContext(), "Не удалось подтвердить: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void declineRequest(AdminRequest req) {
        pendingRef.child(req.uid).removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Заявка отклонена", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Не удалось отклонить: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (requestsListener != null) {
            pendingRef.removeEventListener(requestsListener);
        }
    }
}