package com.example.alinadiplom.ui.profile;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.example.alinadiplom.R;

public class DormInfoFragment extends Fragment {
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String uid;
    private TextView textFloorRoom, textRoomType, textNeighbors;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dorm_info, container, false);
        textFloorRoom = view.findViewById(R.id.text_name);
        textRoomType = view.findViewById(R.id.text_room);
        textNeighbors = view.findViewById(R.id.text_neighbors);

        TextView btnDormRegulations = view.findViewById(R.id.btn_dorm_regulations);
        TextView btnDormitoryRegulations = view.findViewById(R.id.btn_dormitory_regulations);
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        if (mAuth.getCurrentUser() != null) {
            uid = mAuth.getCurrentUser().getUid();
            loadDormInfo();
        }
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
    private void loadDormInfo() {
        mDatabase.child("Users").child(uid).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                String dorm = snapshot.child("dorm").getValue(String.class);
                String room = snapshot.child("room").getValue(String.class);

                // Устанавливаем этаж/номер
                textFloorRoom.setText(dorm + ", " + room);

                // Получаем соседей по комнате
                if (dorm != null && room != null) {
                    String roomKey = room.replace("/", "\\");
                    mDatabase.child("rooms").child(dorm).child(roomKey).child("residents")
                            .get().addOnSuccessListener(residentSnap -> {
                                StringBuilder neighbors = new StringBuilder();
                                for (DataSnapshot resident : residentSnap.getChildren()) {
                                    String fio = resident.getValue(String.class);
                                    if (!resident.getKey().equals(uid)) { // исключаем самого себя
                                        neighbors.append(fio).append(", ");
                                    }
                                }

                                if (neighbors.length() > 2) {
                                    neighbors.setLength(neighbors.length() - 2); // убираем последнюю запятую
                                }

                                textNeighbors.setText(neighbors.toString());
                            });
                }
            }
        });

        // (по желанию) Подгрузи тип комнаты
        mDatabase.child("Users").child(uid).child("roomType").get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                textRoomType.setText(snapshot.getValue(String.class));
            }
        });
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