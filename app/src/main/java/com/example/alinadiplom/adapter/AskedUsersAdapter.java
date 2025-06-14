package com.example.alinadiplom.adapter;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alinadiplom.ChatWithAdminActivity;
import com.example.alinadiplom.R;
import com.example.alinadiplom.security.CryptoHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class AskedUsersAdapter extends RecyclerView.Adapter<AskedUsersAdapter.ViewHolder> {

    private final List<String> userIds = new ArrayList<>();
    private final DatabaseReference usersRef;

    public AskedUsersAdapter() {
        usersRef = FirebaseDatabase.getInstance().getReference("Users");
    }

    public void setUserIds(List<String> ids) {
        userIds.clear();
        userIds.addAll(ids);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_asked_user, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String userUid = userIds.get(position);

        // Сразу помещаем заглушки, чтобы holder не был пуст
        holder.textUid.setText("Загрузка...");
        holder.textRoom.setText("");

        usersRef.child(userUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snap) {
                        String rawFio  = snap.child("fio").getValue(String.class);
                        String rawRoom = snap.child("room").getValue(String.class);

                        // Если нет ФИО или комнаты — убираем пользователя из списка
                        if (rawFio == null || rawFio.isEmpty() || rawRoom == null || rawRoom.isEmpty()) {
                            int pos = holder.getAdapterPosition();
                            if (pos != RecyclerView.NO_POSITION) {
                                userIds.remove(pos);
                                notifyItemRemoved(pos);
                            }
                            return;
                        }

                        // Иначе выводим расшифрованное ФИО и комнату
                        String fio;
                        try {
                            fio = CryptoHelper.decrypt(rawFio);
                        } catch (Exception e) {
                            fio = rawFio;
                        }

                        holder.textUid.setText("Житель: " + fio);
                        holder.textRoom.setText("Комната: " + rawRoom);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) { }
                });

        holder.itemView.setOnClickListener(v -> {
            String adminUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            Intent intent = new Intent(v.getContext(), ChatWithAdminActivity.class);
            intent.putExtra("currentUserId", adminUid);
            intent.putExtra("authorId", userUid);
            intent.putExtra("adminFio", "Вы");
            v.getContext().startActivity(intent);
        });
    }


    @Override
    public int getItemCount() {
        return userIds.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textUid, textRoom;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textUid  = itemView.findViewById(R.id.textUid);
            textRoom = itemView.findViewById(R.id.textRoom);
            if (textUid == null || textRoom == null) {
                throw new IllegalStateException("item_asked_user.xml must have TextViews with ids textUid and textRoom");
            }
        }
    }
}
