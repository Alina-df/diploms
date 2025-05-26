package com.example.alinadiplom;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
    private static final String TAG = "NotificationAdapter";
    private final List<Notice> notices;
    private final boolean isAdmin;

    public NotificationAdapter(List<Notice> notices, boolean isAdmin) {
        this.notices = notices;
        this.isAdmin = isAdmin;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView dateView, titleView, messageView;
        ImageButton deleteButton;

        public ViewHolder(View itemView) {
            super(itemView);
            dateView = itemView.findViewById(R.id.dateView);
            titleView = itemView.findViewById(R.id.titleView);
            messageView = itemView.findViewById(R.id.messageView);
            deleteButton = itemView.findViewById(R.id.deleteButton);
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
        Notice notice = notices.get(position);
        holder.dateView.setText(notice.date);
        holder.titleView.setText(notice.title);
        holder.messageView.setText(notice.body);

        // Show delete button for admins
        holder.deleteButton.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        holder.deleteButton.setOnClickListener(v -> {
            DatabaseReference noticeRef = FirebaseDatabase.getInstance().getReference("notices").child(notice.id);
            noticeRef.removeValue()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Deleted notice: " + notice.id);
                        // Removal will trigger Firebase listener in HomeFragment
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(holder.itemView.getContext(), "Ошибка удаления: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Delete error: " + e.getMessage());
                    });
        });

        // Expand/collapse text on click
        holder.itemView.setOnClickListener(v -> {
            boolean expanded = holder.messageView.getMaxLines() > 2;
            holder.messageView.setMaxLines(expanded ? 2 : Integer.MAX_VALUE);
        });
    }

    @Override
    public int getItemCount() {
        return notices.size();
    }
}