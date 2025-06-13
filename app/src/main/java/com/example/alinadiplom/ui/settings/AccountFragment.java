package com.example.alinadiplom.ui.settings;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.alinadiplom.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;


public class AccountFragment extends Fragment {

    private EditText etFio, etPhone, etEmail, etRoom;
    private Spinner etDorm;
    private Button btnSaveChanges, btnLogout;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String uid;
    private ArrayAdapter<CharSequence> adapter;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        etFio = view.findViewById(R.id.etFio);
        etPhone = view.findViewById(R.id.etPhone);
        etEmail = view.findViewById(R.id.etEmail);
        etDorm = view.findViewById(R.id.etDorm);
        etRoom = view.findViewById(R.id.etRoom);

        btnSaveChanges = view.findViewById(R.id.btnSaveChanges);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        if (mAuth.getCurrentUser() != null) {
            uid = mAuth.getCurrentUser().getUid();
            loadUserData();
        }
        adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.dorms,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        etDorm.setAdapter(adapter);
        btnSaveChanges.setOnClickListener(v -> saveChanges());

        return view;
    }

    private void loadUserData() {
        mDatabase.child("Users").child(uid).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                etFio.setText(snapshot.child("fio").getValue(String.class));
                etPhone.setText(snapshot.child("phone").getValue(String.class));
                etEmail.setText(snapshot.child("email").getValue(String.class));
                etRoom.setText(snapshot.child("room").getValue(String.class));
                String dorm = snapshot.child("dorm").getValue(String.class);
                if (dorm != null) {

                    int spinnerPosition = adapter.getPosition(dorm);
                    if (spinnerPosition >= 0) {
                        etDorm.setSelection(spinnerPosition);
                    }
                }

            }
        });
    }

    private void saveChanges() {
        String fio = etFio.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String room = etRoom.getText().toString().trim();
        String dorm = etDorm.getSelectedItem().toString();
        if (dorm.equals("Выберите общежитие")) {
            Toast.makeText(getContext(), "Выберите общежитие", Toast.LENGTH_SHORT).show();
            return;
        }

        if (fio.isEmpty() || phone.isEmpty() || email.isEmpty() || dorm.isEmpty() || room.isEmpty()) {
            Toast.makeText(getContext(), "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!email.equals(mAuth.getCurrentUser().getEmail())) {
            Toast.makeText(getContext(), "Изменение email пока не поддерживается", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("fio", fio);
        updates.put("phone", phone);
        updates.put("dorm", dorm);
        updates.put("room", room);

        String newRoomKey = room.replace("/", "\\");

        mDatabase.child("Users").child(uid).get().addOnSuccessListener(snapshot -> {
            String oldDorm = snapshot.child("dorm").getValue(String.class);
            String oldRoom = snapshot.child("room").getValue(String.class);
            if (oldDorm != null && oldRoom != null) {
                String oldRoomKey = oldRoom.replace("/", "\\");
                mDatabase.child("rooms").child(oldDorm).child(oldRoomKey).child("residents").child(uid).removeValue();
            }
            mDatabase.child("Users").child(uid).updateChildren(updates).addOnSuccessListener(unused -> {


                mDatabase.child("rooms").child(dorm).child(newRoomKey).child("residents").child(uid).setValue(fio);
                mDatabase.child("notices").get().addOnSuccessListener(noticesSnapshot -> {
                    for (DataSnapshot noticeSnapshot : noticesSnapshot.getChildren()) {
                        String authorId = noticeSnapshot.child("userId").getValue(String.class);
                        if (uid.equals(authorId)) {
                            noticeSnapshot.getRef().child("userName").setValue(fio);
                        }
                    }
                });

                Toast.makeText(getContext(), "Данные успешно обновлены", Toast.LENGTH_SHORT).show();

            }).addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Ошибка обновления: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
        });

    }
}
