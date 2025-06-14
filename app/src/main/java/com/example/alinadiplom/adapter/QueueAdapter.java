package com.example.alinadiplom.adapter;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alinadiplom.security.CryptoHelper;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.ViewHolder> {
    private List<String> userIds;
    private Map<String, String> userCache = new HashMap<>(); // userId -> fio + " " + room

    public QueueAdapter(List<String> userIds) {
        this.userIds = userIds;
    }

    public void updateList(List<String> newUserIds) {
        this.userIds = newUserIds;
        userCache.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public QueueAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull QueueAdapter.ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        String userId = userIds.get(position);

        // Сначала ставим заглушку, чтобы не было "пусто"
        holder.textView.setText((position + 1) + ". Загрузка...");

        // Проверяем кэш
        if (userCache.containsKey(userId)) {
            holder.textView.setText((position + 1) + ". " + userCache.get(userId));
            return;
        }

        // Запрашиваем данные пользователя
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(userId);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    holder.textView.setText((position + 1) + ". Пользователь не найден");
                    return;
                }

                try {
                    String encryptedFio = snapshot.child("fio").getValue(String.class);
                    String room = snapshot.child("room").getValue(String.class);

                    String fioDecrypted = CryptoHelper.decrypt(encryptedFio);
                    String displayText = fioDecrypted + " " + room;

                    userCache.put(userId, displayText);

                    // Проверяем, что этот holder всё ещё относится к нужной позиции
                    if (holder.getAdapterPosition() == position) {
                        holder.textView.setText((position + 1) + ". " + displayText);
                    }

                } catch (Exception e) {
                    Log.e("QueueAdapter", "Ошибка дешифровки или данных: ", e);
                    String encryptedFio = snapshot.child("fio").getValue(String.class);
                    String room = snapshot.child("room").getValue(String.class);

                    String fioDecrypted = encryptedFio;
                    String displayText = fioDecrypted + " " + room;

                    userCache.put(userId, displayText);

                    // Проверяем, что этот holder всё ещё относится к нужной позиции
                    if (holder.getAdapterPosition() == position) {
                        holder.textView.setText((position + 1) + ". " + displayText);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                holder.textView.setText((position + 1) + ". Ошибка загрузки");
            }
        });
    }

    @Override
    public int getItemCount() {
        return userIds.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }
    }
}
