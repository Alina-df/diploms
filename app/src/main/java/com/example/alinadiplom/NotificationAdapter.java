package com.example.alinadiplom;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private final List<NotificationItem> notifications;

    public NotificationAdapter(List<NotificationItem> notifications) {
        this.notifications = notifications;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView dateView, titleView, messageView;

        public ViewHolder(View itemView) {
            super(itemView);
            dateView = itemView.findViewById(R.id.dateView);
            titleView = itemView.findViewById(R.id.titleView);
            messageView = itemView.findViewById(R.id.messageView);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationItem item = notifications.get(position);
        holder.dateView.setText(item.date);
        holder.titleView.setText(item.title);
        holder.messageView.setText(item.message);

        // Раскрытие текста по нажатию
        holder.itemView.setOnClickListener(v -> {
            boolean expanded = holder.messageView.getMaxLines() > 2;
            holder.messageView.setMaxLines(expanded ? 2 : Integer.MAX_VALUE);
        });
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }
}

