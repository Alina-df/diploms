package com.example.alinadiplom;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddNoticeActivity extends AppCompatActivity {
    private static final String TAG = "AddNoticeActivity";
    EditText titleEdit, bodyEdit, tagsEdit;
    CheckBox adminNoticeCheckBox;
    Button saveButton;
    DatabaseReference dbRef, userRef;
    String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_notice);

        titleEdit = findViewById(R.id.noticeTitle);
        bodyEdit = findViewById(R.id.noticeBody);
        tagsEdit = findViewById(R.id.noticeTags);
        adminNoticeCheckBox = findViewById(R.id.adminNoticeCheckBox);
        saveButton = findViewById(R.id.saveNoticeBtn);

        dbRef = FirebaseDatabase.getInstance().getReference("notices");
        uid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        userRef = uid != null ? FirebaseDatabase.getInstance().getReference("Users").child(uid) : null;

        // Handle null user
        if (uid == null) {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initially hide admin checkbox and show tags
        adminNoticeCheckBox.setVisibility(View.GONE);
        tagsEdit.setVisibility(View.VISIBLE);

        // Check if user is admin
        userRef.child("role").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String role = snapshot.getValue(String.class);
                Log.d(TAG, "User role: " + role);
                if ("admin".equals(role)) {
                    adminNoticeCheckBox.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AddNoticeActivity.this, "Ошибка проверки прав: " + error.getMessage(), Toast.LENGTH_LONG).show();
                Log.e(TAG, "Role check error: " + error.getMessage());
            }
        });

        // Toggle tags visibility based on admin checkbox
        adminNoticeCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            tagsEdit.setVisibility(isChecked ? View.GONE : View.VISIBLE);
        });

        saveButton.setOnClickListener(v -> {
            String title = titleEdit.getText().toString().trim();
            String body = bodyEdit.getText().toString().trim();
            String tags = tagsEdit.getText().toString().trim();
            boolean isAdminNotice = adminNoticeCheckBox.isChecked();
            String id = dbRef.push().getKey();
            String date = new SimpleDateFormat("dd MMMM", Locale.getDefault()).format(new Date());

            if (title.isEmpty() || body.isEmpty()) {
                Toast.makeText(this, "Заполните заголовок и описание", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isAdminNotice && tags.isEmpty()) {
                Toast.makeText(this, "Добавьте хотя бы один хештег для личного объявления", Toast.LENGTH_SHORT).show();
                return;
            }

            // Format tags
            String formattedTags = "";
            if (!isAdminNotice && !tags.isEmpty()) {
                String[] tagArray = tags.split("\\s+");
                StringBuilder tagBuilder = new StringBuilder();
                for (String tag : tagArray) {
                    if (!tag.startsWith("#")) {
                        tagBuilder.append("#").append(tag);
                    } else {
                        tagBuilder.append(tag);
                    }
                    tagBuilder.append(" ");
                }
                formattedTags = tagBuilder.toString().trim();
                Log.d(TAG, "Formatted tags: " + formattedTags);
            }

            // Get user details for individual notices
            if (!isAdminNotice) {
                String finalFormattedTags = formattedTags;
                userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String userName = snapshot.child("userName").getValue(String.class);
                        String room = snapshot.child("room").getValue(String.class);
                        Log.d(TAG, "User data - userName: " + userName + ", room: " + room);
                        saveNotice(id, title, body, date, isAdminNotice, userName, room, finalFormattedTags);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(AddNoticeActivity.this, "Ошибка получения данных: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "User data fetch error: " + error.getMessage());
                    }
                });
            } else {
                saveNotice(id, title, body, date, isAdminNotice, null, null, null);
            }
        });
    }

    private void saveNotice(String id, String title, String body, String date, boolean isAdmin,
                            String userName, String room, String tags) {
        Map<String, Object> notice = new HashMap<>();
        notice.put("id", id);
        notice.put("title", title);
        notice.put("body", body);
        notice.put("date", date);
        notice.put("isAdmin", isAdmin);
        if (!isAdmin) {
            notice.put("userId", uid);
            notice.put("userName", userName != null ? userName : "Неизвестный пользователь");
            notice.put("room", room != null ? room : "");
            notice.put("tags", tags != null ? tags : "");
        }

        Log.d(TAG, "Saving notice: " + notice.toString());

        dbRef.child(id).setValue(notice)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Объявление сохранено", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Notice saved successfully, id: " + id);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Ошибка сохранения: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Save error: " + e.getMessage());
                });
    }
}