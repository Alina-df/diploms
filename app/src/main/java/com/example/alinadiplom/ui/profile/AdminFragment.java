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
        Button ButtonAdminChoose = view.findViewById(R.id.buttonmakeadmin);
        Button buttonResidentAsk = view.findViewById(R.id.buttonResidentAsk);
        buttonResidentRequests.setOnClickListener(v -> {
            NavHostFragment.findNavController(AdminFragment.this)
                    .navigate(R.id.action_AdminFragment_to_RequestsManagementFragment);
        });
        ButtonAdminChoose.setOnClickListener(v -> {
            NavHostFragment.findNavController(AdminFragment.this)
                    .navigate(R.id.action_AdminFragment_to_navigation_admin_requests);
        });
        buttonResidentAsk.setOnClickListener(v -> {
            NavHostFragment.findNavController(AdminFragment.this)
                    .navigate(R.id.action_AdminFragment_to_AskedUsersListFragment);
        });
        Button buttonMakeReports = view.findViewById(R.id.buttonMakeReports);
        buttonMakeReports.setOnClickListener(v -> {
            NavHostFragment.findNavController(AdminFragment.this)
                    .navigate(R.id.action_AdminFragment_to_ReportFragment);
        });

        return view;
    }

}