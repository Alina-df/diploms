package com.example.alinadiplom;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
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

    EditText number, password, fio, room;
    Spinner spinnerUniversity, spinnerFaculty, spinnerDorm;
    CheckBox checkAdminRequest;
    Button register;
    RadioGroup userTypeRadioGroup;
    FirebaseAuth mAuth;
    DatabaseReference mDatabase;
    FirebaseDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Инициализация
        mAuth = FirebaseAuth.getInstance();
        number = findViewById(R.id.textNumber);
        password = findViewById(R.id.textPassword);
        fio = findViewById(R.id.textFio);
        room = findViewById(R.id.textRoom);
        spinnerUniversity = findViewById(R.id.spinnerUniversity);
        spinnerFaculty = findViewById(R.id.spinnerFaculty);
        spinnerDorm = findViewById(R.id.spinnerDorm);
        checkAdminRequest = findViewById(R.id.checkAdminRequest);
        register = findViewById(R.id.buttonSubmit);
        userTypeRadioGroup = findViewById(R.id.userTypeRadioGroup);

        // Настройка Spinner
        ArrayAdapter<CharSequence> universityAdapter = ArrayAdapter.createFromResource(this,
                R.array.universities, android.R.layout.simple_spinner_item);
        universityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUniversity.setAdapter(universityAdapter);

        ArrayAdapter<CharSequence> facultyAdapter = ArrayAdapter.createFromResource(this,
                R.array.faculties, android.R.layout.simple_spinner_item);
        facultyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFaculty.setAdapter(facultyAdapter);

        ArrayAdapter<CharSequence> dormAdapter = ArrayAdapter.createFromResource(this,
                R.array.dorms, android.R.layout.simple_spinner_item);
        dormAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDorm.setAdapter(dormAdapter);

        // По умолчанию показываем поля для жителей
        findViewById(R.id.residentFieldsLayout).setVisibility(View.VISIBLE);

        // Обработка выбора типа пользователя
        userTypeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioResident) {
                findViewById(R.id.residentFieldsLayout).setVisibility(View.VISIBLE);
            } else if (checkedId == R.id.radioEmployee) {
                findViewById(R.id.residentFieldsLayout).setVisibility(View.GONE);
            }
        });

        register.setOnClickListener(v -> {
            String numberText = number.getText().toString().trim();
            String pass = password.getText().toString().trim();
            String fioText = fio.getText().toString().trim();
            boolean isResident = userTypeRadioGroup.getCheckedRadioButtonId() == R.id.radioResident;

            // Валидация общих полей
            if (numberText.isEmpty()) {
                Toast.makeText(this, "Введите номер телефона", Toast.LENGTH_SHORT).show();
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

            // Валидация полей для жителей
            if (isResident) {
                String roomText = room.getText().toString().trim();
                String universityText = spinnerUniversity.getSelectedItem().toString();
                String facultyText = spinnerFaculty.getSelectedItem().toString();
                String dormText = spinnerDorm.getSelectedItem().toString();

                Toast.makeText(this, "Выбранное общежитие: " + dormText, Toast.LENGTH_SHORT).show();

                if (roomText.isEmpty()) {
                    Toast.makeText(this, "Введите номер комнаты", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (universityText.equals("Выберите университет")) {
                    Toast.makeText(this, "Выберите университет", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (facultyText.equals("Выберите факультет")) {
                    Toast.makeText(this, "Выберите факультет", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (dormText.equals("Выберите общежитие")) {
                    Toast.makeText(this, "Выберите общежитие", Toast.LENGTH_SHORT).show();
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
                            // Успешно зарегистрировались
                            String uid = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
                            // Собираем данные
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("fio", fioText);
                            userData.put("number", numberText);
                            userData.put("role", "resident");
                            userData.put("university", universityText);
                            userData.put("faculty", facultyText);
                            userData.put("dorm", dormText);
                            userData.put("room", roomText);

                            Toast.makeText(this, "Сохраняем данные: " + userData.toString(), Toast.LENGTH_LONG).show();

                            database = FirebaseDatabase.getInstance();
                            mDatabase = database.getReference();

                            // 1) Сохраняем данные пользователя в Users
                            mDatabase.child("Users")
                                    .child(uid)
                                    .setValue(userData)
                                    .addOnCompleteListener(userSaveTask -> {
                                        if (!userSaveTask.isSuccessful()) {
                                            Toast.makeText(this, "Ошибка сохранения профиля", Toast.LENGTH_SHORT).show();
                                            return;
                                        }

                                        // 2) Добавляем в rooms
                                        String safeRoomKey = roomText.replace("/", "\\");
                                        // Обновляем путь для учета общежития
                                        DatabaseReference roomRef = mDatabase.child("rooms")
                                                .child(dormText)
                                                .child(safeRoomKey)
                                                .child("residents")
                                                .child(uid);
                                        roomRef.setValue(fioText).addOnCompleteListener(roomTask -> {
                                            if (!roomTask.isSuccessful()) {
                                                Toast.makeText(this, "Ошибка добавления в комнату", Toast.LENGTH_SHORT).show();
                                                return;
                                            }
                                            proceedWithAdminCheck(uid, fioText, numberText);
                                        }).addOnFailureListener(e -> {
                                            Toast.makeText(this, "Ошибка добавления в комнату: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        });
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Ошибка БД: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    });
                        });
            } else {
                String email = numberText + "@example.com";
                mAuth.createUserWithEmailAndPassword(email, pass)
                        .addOnCompleteListener(task -> {
                            if (!task.isSuccessful()) {
                                String err = Objects.requireNonNull(task.getException()).getMessage();
                                Toast.makeText(this, "Ошибка: " + err, Toast.LENGTH_LONG).show();
                                return;
                            }
                            String uid = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("fio", fioText);
                            userData.put("number", numberText);
                            userData.put("role", "employee");

                            database = FirebaseDatabase.getInstance();
                            mDatabase = database.getReference();

                            mDatabase.child("Users")
                                    .child(uid)
                                    .setValue(userData)
                                    .addOnCompleteListener(userSaveTask -> {
                                        if (!userSaveTask.isSuccessful()) {
                                            Toast.makeText(this, "Ошибка сохранения профиля", Toast.LENGTH_SHORT).show();
                                            return;
                                        }
                                        proceedWithAdminCheck(uid, fioText, numberText);
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Ошибка БД: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    });
                        });
            }
        });
    }

    // Проверка и добавление админа
    private void proceedWithAdminCheck(String uid, String fioText, String numberText) {
        DatabaseReference adminsRef = mDatabase.child("admins");
        adminsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                if (!snap.exists()) {
                    Map<String, Object> adminData = new HashMap<>();
                    adminData.put("fio", fioText);
                    adminData.put("number", numberText);
                    adminData.put("role", "admin");
                    adminsRef.child(uid).setValue(adminData);
                } else if (checkAdminRequest.isChecked()) {
                    Map<String, Object> req = new HashMap<>();
                    req.put("fio", fioText);
                    req.put("number", numberText);
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

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
                Toast.makeText(RegisterActivity.this,
                        "Регистрация завершена, но не смогли проверить админа",
                        Toast.LENGTH_LONG).show();
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                finish();
            }
        });
    }
}