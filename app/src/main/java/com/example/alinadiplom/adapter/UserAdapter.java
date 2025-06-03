package com.example.alinadiplom.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alinadiplom.R;
import com.example.alinadiplom.model.UserItem;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * Адаптер для списка пользователей. Показывает ФИО, роль и кнопку "Сделать админом",
 * если роль != "admin". При клике меняет роль в БД.
 */
public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    // Полный список пользователей (без фильтрации)
    private final List<UserItem> fullList;

    // Список, который отображается (после фильтра Search)
    private final List<UserItem> filteredList;

    public UserAdapter() {
        fullList = new ArrayList<>();
        filteredList = new ArrayList<>();
    }

    // Добавить всех пользователей и обновить отображаемый список
    public void setUsers(List<UserItem> users) {
        fullList.clear();
        fullList.addAll(users);

        // Изначально показываем весь список
        filteredList.clear();
        filteredList.addAll(users);

        notifyDataSetChanged();
    }

    // Фильтрация по ФИО (входит ли substring в полный ФИО, регистронезависимо)
    public void filterByFio(String query) {
        filteredList.clear();
        if (query == null || query.trim().isEmpty()) {
            // Если строка поиска пустая — возвращаем весь список
            filteredList.addAll(fullList);
        } else {
            String lower = query.trim().toLowerCase();
            for (UserItem u : fullList) {
                if (u.fio != null && u.fio.toLowerCase().contains(lower)) {
                    filteredList.add(u);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserItem user = filteredList.get(position);
        holder.tvFio.setText(user.fio != null ? user.fio : "Без имени");
        holder.tvRole.setText("Роль: " + (user.role != null ? user.role : "неизвестно"));

        // Кнопка "Сделать админом" видна, если роль != "admin"
        if ("admin".equals(user.role)) {
            holder.btnMakeAdmin.setVisibility(View.GONE);
        } else {
            holder.btnMakeAdmin.setVisibility(View.VISIBLE);
            holder.btnMakeAdmin.setOnClickListener(v -> {
                // Сразу меняем роль в БД на "admin"
                DatabaseReference userRef = FirebaseDatabase.getInstance()
                        .getReference("Users")
                        .child(user.uid)
                        .child("role");

                userRef.setValue("admin")
                        .addOnSuccessListener(aVoid -> {
                            // Обновляем локально — чтобы кнопка спряталась сразу
                            user.role = "admin";
                            notifyItemChanged(position);
                            Toast.makeText(v.getContext(),
                                    user.fio + " теперь администратор",
                                    Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(v.getContext(),
                                    "Ошибка при изменении роли: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        });
            });
        }
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvFio, tvRole;
        Button btnMakeAdmin;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFio = itemView.findViewById(R.id.text_user_fio);
            tvRole = itemView.findViewById(R.id.text_user_role);
            btnMakeAdmin = itemView.findViewById(R.id.button_make_admin);
        }
    }
}
