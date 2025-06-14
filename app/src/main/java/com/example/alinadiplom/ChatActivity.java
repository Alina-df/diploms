package com.example.alinadiplom;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
    private DatabaseReference usersRef;

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

        materialToolbar.setTitle("Загрузка...");

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Чат"); // Можно позже менять динамически
        }

        // Ссылка на пользователей
        usersRef = FirebaseDatabase.getInstance().getReference("Users");

        // Изначально показываем имя автора (собеседника)
        loadUserNameIntoToolbar(authorId);

        // UI
        recyclerView = findViewById(R.id.recycler_chat);
        messageInput = findViewById(R.id.edit_message);
        sendButton = findViewById(R.id.button_send);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatAdapter(currentUserId);
        recyclerView.setAdapter(adapter);

        // Ссылка на сообщения (по объявлению и текущему пользователю)
        messagesRef = FirebaseDatabase.getInstance().getReference()
                .child("messages")
                .child(adId)
                .child(currentUserId);

        // Загрузка сообщений + слушатель
        setupMessageListener();

        // Отправка сообщений
        sendButton.setOnClickListener(v -> {
            String text = messageInput.getText().toString().trim();
            if (!text.isEmpty()) {
                sendMessage(text);
                messageInput.setText("");
            }
        });
    }

    private void loadUserNameIntoToolbar(String userId) {
        usersRef.child(userId).child("fio").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String encryptedName = snapshot.getValue(String.class);
                if (encryptedName != null) {
                    try {
                        String decryptedName = CryptoHelper.decrypt(encryptedName);
                        materialToolbar.setTitle(decryptedName);
                        Log.d("ToolbarDebug", "Заголовок установлен: " + decryptedName);
                    } catch (Exception e) {
                        e.printStackTrace();
                        materialToolbar.setTitle(encryptedName);
                    }
                } else {
                    materialToolbar.setTitle("Пользователь");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                materialToolbar.setTitle("Ошибка загрузки");
            }
        });
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

                // Меняем заголовок в тулбаре на имя последнего отправителя (если не текущий пользователь)
                if (!messages.isEmpty()) {
                    ChatMessage lastMessage = messages.get(messages.size() - 1);
                    String lastSenderId = lastMessage.senderId;

                    if (!lastSenderId.equals(currentUserId)) {
                        loadUserNameIntoToolbar(lastSenderId);
                    } else {
                        // Если последний отправитель — это текущий пользователь,
                        // показываем имя собеседника (authorId)
                        loadUserNameIntoToolbar(authorId);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatActivity.this, "Ошибка загрузки: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
        messagesRef.orderByChild("timestamp").addValueEventListener(messagesListener);
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
