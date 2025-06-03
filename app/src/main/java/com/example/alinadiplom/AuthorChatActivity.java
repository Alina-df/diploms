package com.example.alinadiplom;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alinadiplom.adapter.ChatAdapter;
import com.example.alinadiplom.model.ChatMessage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AuthorChatActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private EditText messageInput;
    private ImageButton sendButton;
    private ChatAdapter adapter;
    private DatabaseReference messagesRef;
    private String adId, senderId, currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        adId = getIntent().getStringExtra("adId");
        senderId = getIntent().getStringExtra("senderId");
        currentUserId = FirebaseAuth.getInstance().getUid();

        recyclerView = findViewById(R.id.recycler_chat);
        messageInput = findViewById(R.id.edit_message);
        sendButton = findViewById(R.id.button_send);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatAdapter(currentUserId);
        recyclerView.setAdapter(adapter);

        messagesRef = FirebaseDatabase.getInstance().getReference()
                .child("messages")
                .child(adId)
                .child(senderId);

        messagesRef.orderByChild("timestamp").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ChatMessage> messages = new ArrayList<>();
                for (DataSnapshot data : snapshot.getChildren()) {
                    ChatMessage message = data.getValue(ChatMessage.class);
                    if (message != null) {
                        messages.add(message);
                    }
                }
                adapter.setMessages(messages);
                recyclerView.scrollToPosition(adapter.getItemCount() - 1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AuthorChatActivity.this, "Ошибка загрузки: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        sendButton.setOnClickListener(v -> {
            String text = messageInput.getText().toString().trim();
            if (!text.isEmpty()) {
                sendMessage(text);
                messageInput.setText("");
            }
        });
    }
    private void sendMessage(String text) {
        String messageId = messagesRef.push().getKey();
        if (messageId != null) {
            ChatMessage message = new ChatMessage(
                    text,
                    currentUserId,
                    senderId,
                    System.currentTimeMillis(),
                    false
            );
            messagesRef.child(messageId).setValue(message);
        }
    }

}