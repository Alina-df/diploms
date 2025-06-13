package com.example.alinadiplom.adapter;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alinadiplom.R;
import com.example.alinadiplom.model.Notice;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

/**
 * PeopleAdapter: показывает список "людей" (Notice),
 * а по клику открывает PersonProfileFragment. Кнопка удаления —
 * только для админа.
 */
public class PeopleAdapter extends RecyclerView.Adapter<PeopleAdapter.PersonViewHolder> {
    private static final String TAG = "PeopleAdapter";

    private final List<Notice> notices;
    private final boolean isAdmin;

    public PeopleAdapter(List<Notice> notices, boolean isAdmin) {
        this.notices = notices;
        this.isAdmin = isAdmin;
    }

    @NonNull
    @Override
    public PersonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_person, parent, false);
        return new PersonViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PersonViewHolder holder, int position) {
        Notice notice = notices.get(position);

        // Изначально показываем текущие значения
        holder.name.setText(notice.userName != null ? notice.userName : "Неизвестный пользователь");
        holder.room.setText(notice.room != null ? notice.room : "");
        holder.message.setText(notice.body != null ? notice.body : "");
        holder.tags.setText(notice.tags != null ? notice.tags : "");
        holder.avatar.setImageResource(R.drawable.circle_progress);
        holder.deleteButton.setVisibility(isAdmin ? View.VISIBLE : View.GONE);

        // Проверка и обновление имени пользователя из базы
        if (notice.userId != null && !notice.userId.isEmpty()) {
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("Users")
                    .child(notice.userId)
                    .child("fio");

            userRef.get().addOnSuccessListener(dataSnapshot -> {
                if (dataSnapshot.exists()) {
                    String updatedName = dataSnapshot.getValue(String.class);
                    if (updatedName != null && !updatedName.equals(notice.userName)) {
                        // Обновляем имя в notice и UI
                        notice.userName = updatedName;
                        holder.name.setText(updatedName);
                    }
                }
            }).addOnFailureListener(e -> Log.e(TAG, "Ошибка при получении имени: " + e.getMessage()));
        }

        holder.deleteButton.setOnClickListener(v -> {
            new AlertDialog.Builder(holder.itemView.getContext())
                    .setTitle("Удалить объявление")
                    .setMessage("Вы уверены, что хотите удалить это объявление?")
                    .setPositiveButton("Удалить", (dialog, which) -> {
                        DatabaseReference noticeRef = FirebaseDatabase
                                .getInstance()
                                .getReference("notices")
                                .child(notice.id);
                        noticeRef.removeValue()
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "Deleted notice: " + notice.id))
                                .addOnFailureListener(e -> {
                                    Toast.makeText(holder.itemView.getContext(),
                                            "Ошибка удаления: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                    Log.e(TAG, "Delete error: " + e.getMessage());
                                });
                    })
                    .setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        holder.itemView.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("name", notice.userName != null ? notice.userName : "");
            bundle.putString("room", notice.room != null ? notice.room : "");
            bundle.putString("about", notice.body != null ? notice.body : "");
            bundle.putString("tags", notice.tags != null ? notice.tags : "");
            bundle.putString("telegramLink", notice.telegramLink != null ? notice.telegramLink : "");
            bundle.putString("personId", notice.userId);
            bundle.putString("adId", notice.id);

            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.action_home_to_person, bundle);
        });
    }


    @Override
    public int getItemCount() {
        return notices.size();
    }

    static class PersonViewHolder extends RecyclerView.ViewHolder {
        TextView name, room, message, tags;
        ImageView avatar;
        ImageButton deleteButton;

        public PersonViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.personName);
            room = itemView.findViewById(R.id.personRoom);
            message = itemView.findViewById(R.id.personMessage);
            tags = itemView.findViewById(R.id.personTags);
            avatar = itemView.findViewById(R.id.personPhoto);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}
