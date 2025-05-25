package com.example.alinadiplom;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class Load_activity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_load);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        // Если пользователь вошёл — переход на MainActivity
        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(this, MainNavActivity.class));
            finish();
        }

        // Если не вошёл — показать кнопки login и register
        findViewById(R.id.loginbtn).setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class))
        );

        findViewById(R.id.registerbtn).setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class))
        );

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

}
