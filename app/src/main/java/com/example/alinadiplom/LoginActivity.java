package com.example.alinadiplom;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.example.alinadiplom.security.CryptoHelper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LoginActivity extends AppCompatActivity {

    private EditText loginInput, password;
    private Button login;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        loginInput = findViewById(R.id.editNumber);  // Здесь может быть email или username
        password = findViewById(R.id.editPass);
        login = findViewById(R.id.buttonSubmit);

        login.setOnClickListener(v -> {
            String loginText = loginInput.getText().toString().trim();
            String passText = password.getText().toString();

            if (loginText.isEmpty() || passText.isEmpty()) {
                Toast.makeText(this, "Введите логин и пароль", Toast.LENGTH_SHORT).show();
                return;
            }

            if (loginText.contains("@")) {
                signInWithEmail(loginText, passText);
            } else {
                mDatabase.child("usernames").child(loginText).get()
                        .addOnCompleteListener(task -> {
                            if (!task.isSuccessful()) {
                                Toast.makeText(this, "Ошибка доступа к базе", Toast.LENGTH_SHORT).show();
                            } else {
                                if (task.getResult().exists()) {
                                    String uid = task.getResult().getValue(String.class);
                                    mDatabase.child("Users").child(uid).child("email").get()
                                            .addOnCompleteListener(emailTask -> {
                                                if (!emailTask.isSuccessful() || emailTask.getResult() == null) {
                                                    Toast.makeText(this, "Ошибка получения email", Toast.LENGTH_SHORT).show();
                                                } else {
                                                    String encryptedEmail = emailTask.getResult().getValue(String.class);
                                                    if (encryptedEmail == null || encryptedEmail.isEmpty()) {
                                                        Toast.makeText(this, "Email не найден", Toast.LENGTH_SHORT).show();
                                                    } else {
                                                        try {
                                                            String decryptedEmail = CryptoHelper.decrypt(encryptedEmail);
                                                            signInWithEmail(decryptedEmail, passText);
                                                        } catch (Exception e) {
                                                            signInWithEmail(encryptedEmail, passText);;
                                                        }
                                                    }
                                                }
                                            });
                                } else {
                                    Toast.makeText(this, "Логин не найден", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

            }
        });
    }

    private void signInWithEmail(String email, String pass) {
        mAuth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        startActivity(new Intent(this, MainNavActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Ошибка входа: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
