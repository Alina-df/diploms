package com.example.alinadiplom.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.alinadiplom.R;

public class HelpFragment extends Fragment {

    private Button btnAskAdmin, btnViewFAQ;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_help, container, false);

        btnAskAdmin = view.findViewById(R.id.btnAskAdmin);
        btnViewFAQ = view.findViewById(R.id.btnViewFAQ);

        btnViewFAQ.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_HelpFragment_to_FaqInfoFragment));
        return view;

    }
}
