package com.example.alinadiplom.ui.settings;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import com.example.alinadiplom.R;
import com.google.firebase.auth.FirebaseAuth;

public class AccountSettingsFragment extends Fragment {


    private Button btnLogout,btnEditProfile,btnResetPassword;
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account_settings, container, false);

        btnLogout = view.findViewById(R.id.btnLogout);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnResetPassword = view.findViewById(R.id.btnResetPassword);

        mAuth = FirebaseAuth.getInstance();


        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            requireActivity().finish();
        });

        btnEditProfile.setOnClickListener(v -> {
            // Переход к AccountFragment
            androidx.navigation.Navigation.findNavController(view)
                    .navigate(R.id.action_AccountSettingsFragment_to_AccountFragment);
        });

        btnResetPassword.setOnClickListener(v -> {
            // Переход к ResetPasswordFragment
            androidx.navigation.Navigation.findNavController(view)
                    .navigate(R.id.action_AccountSettingsFragment_to_ResetPasswordFragment);
        });

        return view;
    }

}
