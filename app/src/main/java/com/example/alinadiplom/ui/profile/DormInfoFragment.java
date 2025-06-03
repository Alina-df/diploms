package com.example.alinadiplom.ui.profile;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.alinadiplom.R;

public class DormInfoFragment extends Fragment {

    // ... существующий код ...

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dorm_info, container, false);

        // Инициализация кнопок
        Button btnDormRegulations = view.findViewById(R.id.btn_dorm_regulations);
        Button btnDormitoryRegulations = view.findViewById(R.id.btn_dormitory_regulations);

        // Обработчики кликов
        btnDormRegulations.setOnClickListener(v -> openPdfFragment(
                "Положение о студгородке",
                "https://www.s-vfu.ru/universitet/rukovodstvo-i-struktura/strukturnye-podrazdeleniya/ss/%D0%9F%D0%BE%D0%BB%D0%BE%D0%B6%D0%B5%D0%BD%D0%B8%D0%B5%20%D0%BE%20%D1%81%D1%82%D1%83%D0%B4%D0%B5%D0%BD%D1%87%D0%B5%D1%81%D0%BA%D0%BE%D0%BC%20%D0%BE%D0%B1%D1%89%D0%B5%D0%B6%D0%B8%D1%82%D0%B8%D0%B8%20%D0%A1%D0%92%D0%A4%D0%A3.pdf"
        ));

        btnDormitoryRegulations.setOnClickListener(v -> openPdfFragment(
                "Положение об общежитии",
                "https://www.s-vfu.ru/upload/iblock/9df/9dfe936930f8b64f59f5c6161291e1b5.pdf"
        ));

        return view;
    }

    private void openPdfFragment(String title, String pdfUrl) {
        PdfViewerFragment fragment = new PdfViewerFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("pdfUrl", pdfUrl);
        fragment.setArguments(args);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment_activity_main_nav, fragment) // заменяет текущий
                .addToBackStack(null) // добавляем в стек
                .commit();
    }

}