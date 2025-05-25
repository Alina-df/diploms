package com.example.alinadiplom;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {

    EditText number, password, fio, university, faculty, dorm, room;
    CheckBox checkAdminRequest;
    Button register;
    FirebaseAuth mAuth;
    DatabaseReference mDatabase;
    FirebaseDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // init
        mAuth     = FirebaseAuth.getInstance();
        number    = findViewById(R.id.textNumber);
        password  = findViewById(R.id.textPassword);
        fio       = findViewById(R.id.textFio);
        university= findViewById(R.id.textUniversity);
        faculty   = findViewById(R.id.textFaculty);
        dorm      = findViewById(R.id.textDormi);
        room      = findViewById(R.id.textRoom);
        checkAdminRequest = findViewById(R.id.checkAdminRequest);
        register  = findViewById(R.id.buttonSubmit);

        register.setOnClickListener(v -> {
            String numberText = number.getText().toString().trim();
            String pass       = password.getText().toString().trim();
            String fioText    = fio.getText().toString().trim();

            // Валидация
            if (numberText.isEmpty()) {
                Toast.makeText(this, "Введите номер", Toast.LENGTH_SHORT).show();
                return;
            }
            if (pass.length() < 6) {
                Toast.makeText(this, "Пароль должен быть не менее 6 символов", Toast.LENGTH_SHORT).show();
                return;
            }
            if (fioText.isEmpty()) {
                Toast.makeText(this, "Введите ФИО", Toast.LENGTH_SHORT).show();
                return;
            }

            String email = numberText + "@example.com";
            mAuth.createUserWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            String err = Objects.requireNonNull(task.getException()).getMessage();
                            Toast.makeText(this, "Ошибка: " + err, Toast.LENGTH_LONG).show();
                            return;
                        }
                        // успешно зарегистрировались
                        String uid = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
                        // собираем данные
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("fio",        fioText);
                        userData.put("university", university.getText().toString().trim());
                        userData.put("faculty",    faculty.getText().toString().trim());
                        userData.put("dorm",       dorm.getText().toString().trim());
                        userData.put("room",       room.getText().toString().trim());
                        userData.put("number",     numberText);
                        userData.put("role",       "resident");  // по умолчанию

                        database = FirebaseDatabase.getInstance();
                        mDatabase = database.getReference();

                        // 1) пишем в Users
                        // после успешного сохранения в Users:
                        mDatabase.child("Users")
                                .child(uid)
                                .setValue(userData)
                                .addOnCompleteListener(userSaveTask -> {
                                    if (!userSaveTask.isSuccessful()) {
                                        Toast.makeText(this, "Ошибка сохранения профиля", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    DatabaseReference adminsRef = mDatabase.child("admins");
                                    adminsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snap) {
                                            if (!snap.exists()) {
                                                Map<String, Object> adminData = new HashMap<>();
                                                adminData.put("fio",    fioText);
                                                adminData.put("number", numberText);
                                                adminData.put("role",   "admin");
                                                adminsRef.child(uid).setValue(adminData);
                                            } else if (checkAdminRequest.isChecked()) {
                                                Map<String, Object> req = new HashMap<>();
                                                req.put("fio",       fioText);
                                                req.put("number",    numberText);
                                                req.put("timestamp", ServerValue.TIMESTAMP);
                                                mDatabase.child("pendingAdmins")
                                                        .child(uid)
                                                        .setValue(req);
                                            }
                                            Toast.makeText(RegisterActivity.this,
                                                    "Регистрация успешна!",
                                                    Toast.LENGTH_SHORT).show();
                                            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                            finish();
                                        }
                                        @Override public void onCancelled(@NonNull DatabaseError e) {
                                            Toast.makeText(RegisterActivity.this,
                                                    "Регистрация завершена, но не смогли проверить админа",
                                                    Toast.LENGTH_LONG).show();
                                            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                            finish();
                                        }
                                    });
                                    // ====== КОНЕЦ НОВОГО БЛОКА ======
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Ошибка БД: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });

                    });
        });
    }
}
