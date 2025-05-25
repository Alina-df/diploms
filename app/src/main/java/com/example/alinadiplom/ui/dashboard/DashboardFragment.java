package com.example.alinadiplom.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.alinadiplom.databinding.FragmentDashboardBinding;

public class DashboardFragment extends Fragment {

private FragmentDashboardBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        DashboardViewModel dashboardViewModel =
                new ViewModelProvider(this).get(DashboardViewModel.class);

    binding = FragmentDashboardBinding.inflate(inflater, container, false);
    View root = binding.getRoot();
        // По клику на "Занять" — скрываем первый экран, показываем второй
        binding.buttonOccupy.setOnClickListener(v -> {
            binding.groupView.setVisibility(View.GONE);
            binding.laundryView.setVisibility(View.VISIBLE);
        });

        // Если нужно, добавить обработку клика по "Начать стирку" или "Вкл. уведомление"
        binding.buttonStartWash.setOnClickListener(v -> {
            // TODO: запустить процесс стирки
        });
        binding.buttonToggleNotify.setOnClickListener(v -> {
            // TODO: включить/выключить уведомления
        });

        return root;
    }

@Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}