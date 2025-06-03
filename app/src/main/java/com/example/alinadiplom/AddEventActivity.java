package com.example.alinadiplom;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.alinadiplom.model.Event;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AddEventActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_event);

        EditText eventNameEditText = findViewById(R.id.eventNameEditText);
        EditText eventDescriptionEditText = findViewById(R.id.eventDescriptionEditText);
        EditText eventDateEditText = findViewById(R.id.eventDateEditText);
        Button saveButton = findViewById(R.id.saveButton);

        saveButton.setOnClickListener(v -> {
            String title = eventNameEditText.getText().toString().trim();
            String description = eventDescriptionEditText.getText().toString().trim();
            String date = eventDateEditText.getText().toString().trim();

            if (title.isEmpty()) {
                Toast.makeText(this, "Введите название мероприятия", Toast.LENGTH_SHORT).show();
                return;
            }
            if (description.isEmpty()) {
                Toast.makeText(this, "Введите описание мероприятия", Toast.LENGTH_SHORT).show();
                return;
            }
            if (date.isEmpty()) {
                Toast.makeText(this, "Введите дату мероприятия", Toast.LENGTH_SHORT).show();
                return;
            }

            Event newEvent = new Event(title, description, date);

            DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("events");

            String key = dbRef.push().getKey();
            if (key != null) {
                dbRef.child(key).setValue(newEvent)
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(this, "Мероприятие успешно добавлено!", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Ошибка при добавлении мероприятия", Toast.LENGTH_SHORT).show();
                        });
            }
        });
    }
}
