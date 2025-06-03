package com.example.alinadiplom;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.alinadiplom.model.Event;

public class EventDetailsActivity extends AppCompatActivity {

    private TextView titleTextView, descriptionTextView, dateTextView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        titleTextView = findViewById(R.id.titleTextView);
        descriptionTextView = findViewById(R.id.descriptionTextView);
        dateTextView = findViewById(R.id.dateTextView);

        Event event = (Event) getIntent().getSerializableExtra("event");

        if (event != null) {
            titleTextView.setText(event.getTitle());
            descriptionTextView.setText(event.getDescription());
            dateTextView.setText(event.getDate());
        }
    }
}
