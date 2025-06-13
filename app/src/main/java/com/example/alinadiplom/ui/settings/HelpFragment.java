package com.example.alinadiplom.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.alinadiplom.ChatWithAdminActivity;
import com.example.alinadiplom.R;
import com.example.alinadiplom.security.CryptoHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class HelpFragment extends Fragment {

    private Button btnAskAdmin, btnViewFAQ;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_help, container, false);

        btnAskAdmin = view.findViewById(R.id.btnAskAdmin);
        btnViewFAQ = view.findViewById(R.id.btnViewFAQ);

        btnViewFAQ.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_HelpFragment_to_FaqInfoFragment));
        btnAskAdmin.setOnClickListener(v -> openChatWithAdmin());

        return view;

    }
    private void openChatWithAdmin() {
        String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference usersRef = FirebaseDatabase.getInstance()
                .getReference("Users");

        usersRef.orderByChild("role").equalTo("admin")
                .limitToFirst(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.hasChildren()) {
                            Toast.makeText(getContext(),
                                    "Администратор не найден", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        DataSnapshot adminSnap = snapshot.getChildren().iterator().next();
                        String adminUid = adminSnap.getKey();
                        String encryptedFio = adminSnap.child("fio").getValue(String.class);
                        String adminFio = "(Админ)";

                        try {
                            if (encryptedFio != null && !encryptedFio.isEmpty()) {
                                adminFio = CryptoHelper.decrypt(encryptedFio);
                            }
                        } catch (Exception ignored) { }

                        Intent intent = new Intent(getContext(), ChatWithAdminActivity.class);
                        intent.putExtra("currentUserId", currentUid);
                        intent.putExtra("adminId", adminUid);
                        intent.putExtra("adminFio", adminFio);
                        startActivity(intent);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(),
                                "Ошибка при поиске администратора: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }


}
