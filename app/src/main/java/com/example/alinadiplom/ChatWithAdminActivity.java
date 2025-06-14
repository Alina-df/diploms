package com.example.alinadiplom;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alinadiplom.adapter.ChatAdapter;
import com.example.alinadiplom.model.ChatMessage;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class ChatWithAdminActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private RecyclerView recyclerView;
    private EditText messageInput;
    private ImageButton sendButton;
    private ChatAdapter adapter;

    private DatabaseReference chatRef;
    private ValueEventListener messagesListener;

    private String currentUserId;
    private String authorId;    // UID пользователя
    private String adminFio;    // отображаемое имя

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_admin);

        // Получение параметров
        currentUserId = getIntent().getStringExtra("currentUserId");
        authorId      = getIntent().getStringExtra("authorId");
        adminFio      = getIntent().getStringExtra("adminFio");

        if (currentUserId == null || authorId == null) {
            Toast.makeText(this, "Неверные параметры чата", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // UI
        toolbar      = findViewById(R.id.toolbar_admin);
        recyclerView = findViewById(R.id.recycler_admin_chat);
        messageInput = findViewById(R.id.edit_message_admin);
        sendButton   = findViewById(R.id.button_send_admin);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setTitle(adminFio != null ? adminFio : "Чат");
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // RecyclerView
        adapter = new ChatAdapter(currentUserId);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Инициализируем ветку чата
        String chatKey = makeChatKey(currentUserId, authorId);
        chatRef = FirebaseDatabase.getInstance()
                .getReference("adminMessages")
                .child(chatKey);

        // 1) Создаём слушатель
        messagesListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ChatMessage> messages = new ArrayList<>();
                for (DataSnapshot data : snapshot.getChildren()) {
                    ChatMessage msg = data.getValue(ChatMessage.class);
                    if (msg != null) messages.add(msg);
                }
                adapter.setMessages(messages);
                recyclerView.scrollToPosition(adapter.getItemCount() - 1);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatWithAdminActivity.this,
                        "Ошибка загрузки сообщений", Toast.LENGTH_SHORT).show();
            }
        };

        // 2) Подписываемся на эту ветку
        chatRef.orderByChild("timestamp")
                .addValueEventListener(messagesListener);

        // Отправка
        sendButton.setOnClickListener(v -> {
            String text = messageInput.getText().toString().trim();
            if (!text.isEmpty()) {
                sendMessage(text);
                messageInput.setText("");
            }
        });
    }

    private String makeChatKey(String a, String b) {
        if (a == null || b == null) throw new IllegalArgumentException("Null в makeChatKey");
        return (a.compareTo(b) < 0) ? a + "_" + b : b + "_" + a;
    }
    private void sendMessage(String text) {
        String id = FirebaseDatabase.getInstance().getReference().push().getKey();
        if (id == null) return;

        ChatMessage msg = new ChatMessage(text, currentUserId, authorId,
                System.currentTimeMillis(), false);

        DatabaseReference root = FirebaseDatabase.getInstance().getReference();

        // Определяем, является ли currentUserId админом
        root.child("Users").child(currentUserId).child("role")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snap) {
                        boolean iAmAdmin = "admin".equals(snap.getValue(String.class));

                        if (iAmAdmin) {
                            String chatKey = makeChatKey(currentUserId, authorId);
                            root.child("adminMessages").child(chatKey).child(id)
                                    .setValue(msg)
                                    .addOnSuccessListener(v -> removeForOtherAdmins());
                        } else {
                            root.child("Users").orderByChild("role").equalTo("admin")
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override public void onDataChange(@NonNull DataSnapshot admins) {
                                            for (DataSnapshot adm : admins.getChildren()) {
                                                String adminUid = adm.getKey();
                                                if (adminUid == null) continue;
                                                String key = makeChatKey(currentUserId, adminUid);
                                                root.child("adminMessages")
                                                        .child(key)
                                                        .child(id)
                                                        .setValue(msg);
                                            }
                                        }
                                        @Override public void onCancelled(@NonNull DatabaseError e) { }
                                    });
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) { }
                });
    }


    private void removeForOtherAdmins() {
        DatabaseReference usersRef = FirebaseDatabase.getInstance()
                .getReference("Users");

        usersRef.orderByChild("role").equalTo("admin")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snap) {
                        for (DataSnapshot adm : snap.getChildren()) {
                            String otherAdmin = adm.getKey();
                            if (otherAdmin == null || otherAdmin.equals(currentUserId)) continue;
                            String otherKey = makeChatKey(authorId, otherAdmin);
                            FirebaseDatabase.getInstance()
                                    .getReference("adminMessages")
                                    .child(otherKey)
                                    .removeValue();
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (chatRef != null && messagesListener != null) {
            chatRef.removeEventListener(messagesListener);
        }
    }
}
