package com.example.alinadiplom.adapter;

import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alinadiplom.ChatActivity;
import com.example.alinadiplom.R;
import com.example.alinadiplom.model.ChatMessage;

import java.util.ArrayList;
import java.util.List;

public  class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {
    private final String currentUserId;
    private List<ChatMessage> messages = new ArrayList<>();

    public ChatAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        holder.messageText.setText(message.text);

        LinearLayout.LayoutParams params =
                (LinearLayout.LayoutParams) holder.messageText.getLayoutParams();

        if (message.senderId.equals(currentUserId)) {
            holder.container.setGravity(Gravity.END);
            holder.messageText.setBackgroundResource(R.drawable.bg_sent_message);
            holder.messageText.setTextColor(Color.WHITE);
        } else {
            holder.container.setGravity(Gravity.START);
            holder.messageText.setBackgroundResource(R.drawable.bg_received_message);
            holder.messageText.setTextColor(Color.BLACK);
        }

        holder.messageText.setLayoutParams(params);
    }



    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        LinearLayout container;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.text_message);
            container = itemView.findViewById(R.id.message_container);
        }
    }

}
