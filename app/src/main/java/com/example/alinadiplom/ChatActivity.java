package com.example.alinadiplom;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
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
import com.example.alinadiplom.security.CryptoHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private MaterialToolbar materialToolbar;
    private EditText messageInput;
    private ImageButton sendButton;
    private ChatAdapter adapter;
    private ValueEventListener messagesListener;
    private DatabaseReference messagesRef;
    private String currentUserId;
    private String adId, authorId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Получение параметров
        adId = getIntent().getStringExtra("adId");
        authorId = getIntent().getStringExtra("authorId");
        currentUserId = getIntent().getStringExtra("currentUserId");
        materialToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(materialToolbar);

        // Устанавливаем заголовок по умолчанию (можете оставить пустым или задать другое значение)
        materialToolbar.setTitle("Загрузка...");

        // Включаем кнопку "Назад"
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Загрузка и установка имени пользователя
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(authorId);

        userRef.child("fullName").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String encryptedName = snapshot.getValue(String.class);
                if (encryptedName != null) {
                    try {
                        String decryptedName = CryptoHelper.decrypt(encryptedName);
                        // Устанавливаем реальное имя
                        materialToolbar.setTitle(decryptedName);
                    } catch (Exception e) {
                        e.printStackTrace();
                        materialToolbar.setTitle("Ошибка");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                materialToolbar.setTitle("Ошибка загрузки");
            }
        });


        // Инициализация UI
        recyclerView = findViewById(R.id.recycler_chat);
        messageInput = findViewById(R.id.edit_message);
        sendButton = findViewById(R.id.button_send);

        // Настройка RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatAdapter(currentUserId);
        recyclerView.setAdapter(adapter);

        // Ссылка на сообщения текущего пользователя
        messagesRef = FirebaseDatabase.getInstance().getReference()
                .child("messages")
                .child(adId)
                .child(currentUserId);

        // Загрузка сообщений
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
                Toast.makeText(ChatActivity.this, "Ошибка загрузки: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Отправка сообщения
        sendButton.setOnClickListener(v -> {
            String text = messageInput.getText().toString().trim();
            if (!text.isEmpty()) {
                sendMessage(text);
                messageInput.setText("");
            }
        });
        setupMessageListener();
    }
    private void setupMessageListener() {
        messagesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ChatMessage> messages = new ArrayList<>();
                for (DataSnapshot data : snapshot.getChildren()) {
                    ChatMessage message = data.getValue(ChatMessage.class);
                    if (message != null) {
                        messages.add(message);
                        if (!message.senderId.equals(currentUserId) && !message.read) {
                            markMessageAsRead(data.getKey());
                        }
                    }
                }
                adapter.setMessages(messages);
                recyclerView.scrollToPosition(adapter.getItemCount() - 1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatActivity.this, "Ошибка загрузки: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
        messagesRef.addValueEventListener(messagesListener);
    }

    private void markMessageAsRead(String messageId) {
        messagesRef.child(messageId).child("read").setValue(true);
    }

    private void sendMessage(String text) {
        String messageId = messagesRef.push().getKey();
        if (messageId != null) {
            ChatMessage message = new ChatMessage(
                    text,
                    currentUserId,
                    authorId,
                    System.currentTimeMillis(),
                    false
            );
            messagesRef.child(messageId).setValue(message);
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messagesRef != null && messagesListener != null) {
            messagesRef.removeEventListener(messagesListener);
        }
    }

}