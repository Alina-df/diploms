package com.example.alinadiplom;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

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
        holder.name.setText(notice.userName != null ? notice.userName : "Неизвестный пользователь");
        holder.room.setText(notice.room != null ? notice.room : "");
        holder.message.setText(notice.body);
        holder.tags.setText(notice.tags != null ? notice.tags : "");
        holder.avatar.setImageResource(notice.avatarResId != 0 ? notice.avatarResId : R.drawable.ic_avatar_placeholder);

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
            room = itemView.findViewById(R.id.personRoom);
            name = itemView.findViewById(R.id.personName);
            message = itemView.findViewById(R.id.personMessage);
            tags = itemView.findViewById(R.id.personTags);
            avatar = itemView.findViewById(R.id.personPhoto);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}