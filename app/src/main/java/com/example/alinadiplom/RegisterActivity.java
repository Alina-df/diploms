package com.example.alinadiplom;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.alinadiplom.LoginActivity;
import com.example.alinadiplom.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {

    EditText email, password, fio, room, username, phone;
    Spinner spinnerFaculty, spinnerDorm;
    RadioGroup userTypeRadioGroup;
    Button register;

    FirebaseAuth mAuth;
    DatabaseReference mDatabase;
    FirebaseDatabase database;
    String formattedPhone = "";

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Инициализация
        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        mDatabase = database.getReference();

        email = findViewById(R.id.textEmail);
        password = findViewById(R.id.textPassword);
        fio = findViewById(R.id.textFio);
        room = findViewById(R.id.textRoom);
        username = findViewById(R.id.textUsername);
        phone = findViewById(R.id.textNumber);
        spinnerFaculty = findViewById(R.id.spinnerFaculty);
        spinnerDorm = findViewById(R.id.spinnerDorm);
        register = findViewById(R.id.buttonSubmit);
        userTypeRadioGroup = findViewById(R.id.userTypeRadioGroup);

        // Кастомная маска телефона через TextWatcher
        phone.addTextChangedListener(new TextWatcher() {
            private boolean isUpdating;
            private String oldText = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                if (isUpdating) {
                    isUpdating = false;
                    return;
                }
                String digits = s.toString().replaceAll("[^\\d]", "");
                if (digits.startsWith("8")) {
                    digits = digits.substring(1);
                }
                if (!digits.startsWith("7")) {
                    digits = "7" + digits;
                }
                if (digits.length() > 11) {
                    digits = digits.substring(0, 11);
                }
                String result = "+" + digits;

                isUpdating = true;
                phone.setText(result);
                phone.setSelection(result.length());

                formattedPhone = result;
            }

        });

        // Настройка спиннеров
        setupSpinner(spinnerFaculty, R.array.faculties);
        setupSpinner(spinnerDorm, R.array.dorms);

        // Обработка типа пользователя
        findViewById(R.id.residentFieldsLayout).setVisibility(View.VISIBLE);
        userTypeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioResident) {
                findViewById(R.id.residentFieldsLayout).setVisibility(View.VISIBLE);
            } else {
                findViewById(R.id.residentFieldsLayout).setVisibility(View.GONE);
            }
        });

        register.setOnClickListener(v -> attemptRegistration());
    }

    private void setupSpinner(Spinner spinner, int arrayResId) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                arrayResId, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void attemptRegistration() {
        String emailText = email.getText().toString().trim();
        String pass = password.getText().toString().trim();
        String fioText = fio.getText().toString().trim();
        String usernameText = username.getText().toString().trim();
        String phoneText = formattedPhone.trim();
        boolean isResident = userTypeRadioGroup.getCheckedRadioButtonId() == R.id.radioResident;

        // Валидация
        if (usernameText.isEmpty()) {
            showToast("Введите логин");
            return;
        }
        if (emailText.isEmpty() || !emailText.contains("@")) {
            showToast("Введите корректную почту");
            return;
        }
        if (pass.length() < 6) {
            showToast("Пароль должен быть не менее 6 символов");
            return;
        }
        if (fioText.isEmpty()) {
            showToast("Введите ФИО");
            return;
        }
        if (phoneText.isEmpty()) {
            showToast("Введите номер телефона");
            return;
        }

        if (isResident) {
            String facultyText = spinnerFaculty.getSelectedItem().toString();
            String dormText = spinnerDorm.getSelectedItem().toString();
            String roomText = room.getText().toString().trim();

            if (roomText.isEmpty() ||
                    facultyText.contains("Выберите") || dormText.contains("Выберите")) {
                showToast("Заполните все поля жителя");
                return;
            }
        }

        // Проверка логина
        mDatabase.child("usernames").child(usernameText).get().addOnCompleteListener(checkTask -> {
            if (checkTask.getResult().exists()) {
                showToast("Логин уже занят");
            } else {
                registerUser(emailText, pass, fioText, usernameText, phoneText, isResident);
            }
        });
    }

    private void registerUser(String emailText, String pass, String fioText, String usernameText, String phoneText, boolean isResident) {
        mAuth.createUserWithEmailAndPassword(emailText, pass)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        showToast("Ошибка: " + Objects.requireNonNull(task.getException()).getMessage());
                        return;
                    }

                    String uid = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("email", emailText);
                    userData.put("fio", fioText);
                    userData.put("username", usernameText);
                    userData.put("phone", phoneText);
                    userData.put("uid", uid);

                    if (isResident) {
                        String facultyText = spinnerFaculty.getSelectedItem().toString();
                        String dormText = spinnerDorm.getSelectedItem().toString();
                        String roomText = room.getText().toString().trim();

                        userData.put("role", "resident");
                        userData.put("faculty", facultyText);
                        userData.put("dorm", dormText);
                        userData.put("room", roomText);
                    } else {
                        userData.put("role", "employee");
                    }

                    // Сохраняем пользователя и логин
                    mDatabase.child("Users").child(uid).setValue(userData)
                            .addOnSuccessListener(unused -> {
                                mDatabase.child("usernames").child(usernameText).setValue(uid);

                                if (isResident) {
                                    String dorm = (String) userData.get("dorm");
                                    String room = ((String) userData.get("room")).replace("/", "\\");
                                    mDatabase.child("rooms")
                                            .child(dorm)
                                            .child(room)
                                            .child("residents")
                                            .child(uid)
                                            .setValue(fioText);
                                }

                                registrationSuccess();
                            })
                            .addOnFailureListener(e ->
                                    showToast("Ошибка сохранения данных: " + e.getMessage()));
                });
    }
    private void registrationSuccess() {
        showToast("Регистрация успешна");
        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
        finish();
    }
    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
