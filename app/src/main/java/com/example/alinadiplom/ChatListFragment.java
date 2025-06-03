package com.example.alinadiplom;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.alinadiplom.model.ChatMessage;
import com.example.alinadiplom.model.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 * create an instance of this fragment.
 */
public class ChatListFragment extends Fragment {
    private RecyclerView recyclerView;
    private ChatListAdapter adapter;
    private DatabaseReference chatsRef;
    private ValueEventListener chatsListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_chat_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ChatListAdapter();
        recyclerView.setAdapter(adapter);

        String adId = getArguments().getString("adId");
        if (adId != null) {
            setupChatsListener(adId);
        }
    }

    private void setupChatsListener(String adId) {
        chatsRef = FirebaseDatabase.getInstance().getReference()
                .child("messages")
                .child(adId);

        chatsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, ChatPreview> chatPreviews = new HashMap<>();

                // Собираем данные о чатах
                for (DataSnapshot child : snapshot.getChildren()) {
                    if (!"creatorId".equals(child.getKey())) {
                        String senderId = child.getKey();
                        ChatPreview preview = chatPreviews.getOrDefault(senderId, new ChatPreview());

                        for (DataSnapshot msg : child.getChildren()) {
                            ChatMessage message = msg.getValue(ChatMessage.class);
                            if (message != null && message.timestamp > preview.lastTimestamp) {
                                preview.lastMessage = message.text;
                                preview.lastTimestamp = message.timestamp;
                                preview.unreadCount += message.senderId.equals(senderId) && !message.read ? 1 : 0;
                            }
                        }
                        chatPreviews.put(senderId, preview);
                    }
                }
                adapter.setChatPreviews(chatPreviews);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Ошибка загрузки чатов: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
        chatsRef.addValueEventListener(chatsListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (chatsRef != null && chatsListener != null) {
            chatsRef.removeEventListener(chatsListener);
        }
    }

    // Класс для хранения превью чата
    static class ChatPreview {
        String lastMessage = "";
        long lastTimestamp = 0;
        int unreadCount = 0;
    }

    // Адаптер списка чатов
    private class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatViewHolder> {
        private Map<String, ChatPreview> chatPreviews = new HashMap<>();
        private List<String> senderIds = new ArrayList<>();

        public void setChatPreviews(Map<String, ChatPreview> previews) {
            this.chatPreviews = previews;
            this.senderIds = new ArrayList<>(previews.keySet());
            // Сортируем по времени последнего сообщения
            Collections.sort(senderIds, (id1, id2) ->
                    Long.compare(previews.get(id2).lastTimestamp, previews.get(id1).lastTimestamp));
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_preview, parent, false);
            return new ChatViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
            String senderId = senderIds.get(position);
            ChatPreview preview = chatPreviews.get(senderId);

            holder.lastMessageView.setText(preview.lastMessage);
            holder.timeView.setText(formatTime(preview.lastTimestamp));

            if (preview.unreadCount > 0) {
                holder.unreadBadge.setVisibility(View.VISIBLE);
                holder.unreadBadge.setText(String.valueOf(preview.unreadCount));
            } else {
                holder.unreadBadge.setVisibility(View.GONE);
            }

            loadUserInfo(senderId, holder.nameView, holder.avatarView);

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(requireActivity(), AuthorChatActivity.class);
                intent.putExtra("adId", getArguments().getString("adId"));
                intent.putExtra("senderId", senderId);
                startActivity(intent);
            });
        }

        private String formatTime(long timestamp) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }

        private void loadUserInfo(String userId, TextView nameView, ImageView avatarView) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference()
                    .child("users")
                    .child(userId);

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        nameView.setText(user.name);
                        if (user.avatarUrl != null && !user.avatarUrl.isEmpty()) {
                            Glide.with(requireContext())
                                    .load(user.avatarUrl)
                                    .circleCrop()
                                    .into(avatarView);
                        } else {
                            avatarView.setImageResource(R.drawable.ic_profile);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("ChatList", "Ошибка загрузки пользователя", error.toException());
                }
            });
        }

        @Override
        public int getItemCount() {
            return senderIds.size();
        }

        class ChatViewHolder extends RecyclerView.ViewHolder {
            ImageView avatarView;
            TextView nameView, lastMessageView, timeView, unreadBadge;

            public ChatViewHolder(@NonNull View itemView) {
                super(itemView);
                avatarView = itemView.findViewById(R.id.chat_avatar);
                nameView = itemView.findViewById(R.id.chat_name);
                lastMessageView = itemView.findViewById(R.id.chat_last_message);
                timeView = itemView.findViewById(R.id.chat_time);
                unreadBadge = itemView.findViewById(R.id.chat_unread_badge);
            }
        }
    }
}