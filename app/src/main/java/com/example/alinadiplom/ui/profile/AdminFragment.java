package com.example.alinadiplom.ui.profile;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.alinadiplom.R;

public class AdminFragment extends Fragment {


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin, container, false);

        Button buttonResidentRequests = view.findViewById(R.id.buttonResidentRequests);
        buttonResidentRequests.setOnClickListener(v -> {
            NavHostFragment.findNavController(AdminFragment.this)
                    .navigate(R.id.action_AdminFragment_to_RequestsManagementFragment);
        });

        return view;
    }

}