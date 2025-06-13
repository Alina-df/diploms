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
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class AskedUsersAdapter extends RecyclerView.Adapter<AskedUsersAdapter.ViewHolder> {

    private List<String> userIds = new ArrayList<>();

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
        String uid = userIds.get(position);
        holder.textUid.setText("Пользователь: " + uid);
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ChatWithAdminActivity.class);
            intent.putExtra("currentUserId", uid);
            intent.putExtra("adminId", FirebaseAuth.getInstance().getCurrentUser().getUid());
            intent.putExtra("adminFio", "Вы");
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return userIds.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textUid;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textUid = itemView.findViewById(R.id.textUid);
        }
    }
}
