package com.example.alinadiplom.ui.profile;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.alinadiplom.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;


public class ReportFragment extends Fragment {

    private FirebaseDatabase database;
    private DatabaseReference reportsRef;

    private Spinner spinnerTypeFilter;
    private Button buttonGenerateReport;

    private List<ReportItem> allReports = new ArrayList<>();

    private final String ALL_TYPES = "Все услуги";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_report, container, false);

        database = FirebaseDatabase.getInstance();
        reportsRef = database.getReference("Reports");

        spinnerTypeFilter = view.findViewById(R.id.spinnerTypeFilter);
        buttonGenerateReport = view.findViewById(R.id.buttonGenerateReport);

        setupSpinner();

        // Загрузка данных заранее, чтобы не ждать каждый раз
        loadReports();

        buttonGenerateReport.setOnClickListener(v -> {
            String selectedType = (String) spinnerTypeFilter.getSelectedItem();
            generateFilteredReport(selectedType);
        });

        return view;
    }

    private void setupSpinner() {
        List<String> types = new ArrayList<>();
        types.add(ALL_TYPES);
        types.add("Сантехник");
        types.add("Электрик");
        types.add("Столяр");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, types);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTypeFilter.setAdapter(adapter);
    }

    private void loadReports() {
        reportsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allReports.clear();

                if (!snapshot.exists()) return;

                for (DataSnapshot reportSnap : snapshot.getChildren()) {
                    DataSnapshot detailsSnap = reportSnap.child("details");

                    String problem = detailsSnap.child("problem").getValue(String.class);
                    String room = detailsSnap.child("room").getValue(String.class);
                    String status = detailsSnap.child("status").getValue(String.class);
                    Long timestamp = detailsSnap.child("timestamp").getValue(Long.class);
                    String type = detailsSnap.child("type").getValue(String.class);
                    String userId = detailsSnap.child("userId").getValue(String.class);
                    Long processedAt = reportSnap.child("processedAt").getValue(Long.class);
                    String processedBy = reportSnap.child("processedBy").getValue(String.class);
                    String requestId = reportSnap.child("requestId").getValue(String.class);

                    allReports.add(new ReportItem(problem, room, status, timestamp, type, userId, processedAt, processedBy, requestId));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Ошибка загрузки отчетов", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void generateFilteredReport(String selectedType) {
        List<ReportItem> filteredList;

        if (ALL_TYPES.equals(selectedType)) {
            filteredList = new ArrayList<>(allReports);
        } else {
            filteredList = new ArrayList<>();
            for (ReportItem item : allReports) {
                if (item.type != null && item.type.equals(selectedType)) {
                    filteredList.add(item);
                }
            }
        }

        if (filteredList.isEmpty()) {
            Toast.makeText(requireContext(), "Нет данных для выбранного типа услуги", Toast.LENGTH_SHORT).show();
            return;
        }

        createExcelFile(filteredList);
    }

    private void createExcelFile(List<ReportItem> reportItems) {
        new Thread(() -> {
            try {
                XSSFWorkbook workbook = new XSSFWorkbook();
                XSSFSheet sheet = workbook.createSheet("Reports");

                // Заголовки столбцов
                String[] headers = {"Поблема", "Комната", "Статус", "Тип"};

                // Создаем заголовочную строку
                XSSFRow headerRow = sheet.createRow(0);
                for (int i = 0; i < headers.length; i++) {
                    headerRow.createCell(i).setCellValue(headers[i]);
                }

                // Заполняем данные
                int rowNum = 1;
                for (ReportItem item : reportItems) {
                    XSSFRow row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(item.problem);
                    row.createCell(1).setCellValue(item.room);
                    row.createCell(2).setCellValue(item.status);
                    row.createCell(3).setCellValue(item.type);
                }

                // Сохраняем файл в память устройства
                String fileName = "Reports_" +(String) spinnerTypeFilter.getSelectedItem()+"_"+ System.currentTimeMillis() + ".xlsx";
                File downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!downloadsFolder.exists()) {
                    downloadsFolder.mkdirs();
                }
                File file = new File(downloadsFolder, fileName);
                FileOutputStream fos = new FileOutputStream(file);
                workbook.write(fos);
                fos.close();
                workbook.close();

                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Отчет сохранён: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
                });

            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Ошибка создания отчёта", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }


    // Класс для хранения данных одной заявки
    static class ReportItem {
        String problem, room, status, type, userId, processedBy, requestId;
        Long timestamp, processedAt;

        public ReportItem(String problem, String room, String status, Long timestamp,
                          String type, String userId, Long processedAt, String processedBy, String requestId) {
            this.problem = problem;
            this.room = room;
            this.status = status;
            this.timestamp = timestamp;
            this.type = type;
            this.userId = userId;
            this.processedAt = processedAt;
            this.processedBy = processedBy;
            this.requestId = requestId;
        }
    }
}
