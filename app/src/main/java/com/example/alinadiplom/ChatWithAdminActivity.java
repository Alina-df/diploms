package com.example.alinadiplom;

import android.os.Bundle;
import android.view.View;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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
    private String adminId;
    private String adminFio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_admin);

        // Получение данных (можно передавать из предыдущей активности)
        currentUserId = getIntent().getStringExtra("currentUserId");
        adminId = getIntent().getStringExtra("adminId");
        adminFio = getIntent().getStringExtra("adminFio");

        // UI
        toolbar = findViewById(R.id.toolbar_admin);
        recyclerView = findViewById(R.id.recycler_admin_chat);
        messageInput = findViewById(R.id.edit_message_admin);
        sendButton = findViewById(R.id.button_send_admin);

        if (adminFio != null) {
            toolbar.setTitle(adminFio);
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Настройка сообщений
        adapter = new ChatAdapter(currentUserId);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Слушаем только одну ветку — для текущего пользователя
        String chatPath = "adminMessages/" + currentUserId + "_" + adminId;
        chatRef = FirebaseDatabase.getInstance().getReference().child(chatPath);


        messagesListener = chatRef.orderByChild("timestamp")
                .addValueEventListener(new ValueEventListener() {
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
                        Toast.makeText(ChatWithAdminActivity.this,
                                "Ошибка загрузки сообщений", Toast.LENGTH_SHORT).show();
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
        String messageId = FirebaseDatabase.getInstance().getReference().push().getKey();

        if (messageId == null) return;

        ChatMessage message = new ChatMessage(
                text,
                currentUserId,
                adminId,
                System.currentTimeMillis(),
                false
        );

        String path1 = "adminMessages/" + currentUserId + "_" + adminId + "/" + messageId;
        String path2 = "adminMessages/" + adminId + "_" + currentUserId + "/" + messageId;

        DatabaseReference db = FirebaseDatabase.getInstance().getReference();
        db.child(path1).setValue(message);
        db.child(path2).setValue(message);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatRef != null && messagesListener != null) {
            chatRef.removeEventListener(messagesListener);
        }
    }
}
