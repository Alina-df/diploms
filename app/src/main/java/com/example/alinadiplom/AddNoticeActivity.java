package com.example.alinadiplom;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddNoticeActivity extends AppCompatActivity {
    EditText titleEdit, bodyEdit;
    Button saveButton;
    DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_notice);

        titleEdit = findViewById(R.id.noticeTitle);
        bodyEdit = findViewById(R.id.noticeBody);
        saveButton = findViewById(R.id.saveNoticeBtn);

        dbRef = FirebaseDatabase.getInstance().getReference("notices");

        saveButton.setOnClickListener(v -> {
            String title = titleEdit.getText().toString().trim();
            String body = bodyEdit.getText().toString().trim();
            String id = dbRef.push().getKey();
            String date = new SimpleDateFormat("dd MMMM", Locale.getDefault()).format(new Date());

            if (!title.isEmpty() && !body.isEmpty()) {
                Map<String, String> notice = new HashMap<>();
                notice.put("id", id);
                notice.put("title", title);
                notice.put("body", body);
                notice.put("date", date);
                dbRef.child(id).setValue(notice);
                finish();
            } else {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
