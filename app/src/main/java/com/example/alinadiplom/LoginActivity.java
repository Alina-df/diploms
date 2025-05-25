package com.example.alinadiplom;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    EditText number, password;
    Button login;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        number = findViewById(R.id.editNumber);
        password = findViewById(R.id.editPass);
        login = findViewById(R.id.buttonSubmit);

        login.setOnClickListener(v -> {
            String email = number.getText().toString() + "@example.com";
            String pass = password.getText().toString();

            mAuth.signInWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            startActivity(new Intent(this, MainNavActivity.class));
                            finish();
                        } else {
                            Toast.makeText(this, "Ошибка входа", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

}