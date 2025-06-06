package com.example.alinadiplom.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnErrorListener;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.example.alinadiplom.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class PdfViewerFragment extends Fragment {

    private PDFView pdfView;
    private ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pdf_viewer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        pdfView = view.findViewById(R.id.pdfView);
        progressBar = view.findViewById(R.id.progressBar);

        if (getArguments() != null) {
            String title = getArguments().getString("title", "PDF Document");
            String pdfUrl = getArguments().getString("pdfUrl", "");

            // Устанавливаем заголовок активности
            if (getActivity() != null) {
                getActivity().setTitle(title);
            }
            trustAllCertificates();
            loadPdfFromUrl(pdfUrl);
        } else {
            showError("Данные документа не получены");
        }
    }

    private void loadPdfFromUrl(String pdfUrl) {
        trustAllCertificates();

        new Thread(() -> {
            try {
                URL url = new URL(pdfUrl);
                InputStream inputStream = url.openStream();

                File pdfFile = new File(requireContext().getFilesDir(), "temp.pdf");
                try (FileOutputStream output = new FileOutputStream(pdfFile)) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer)) > 0) {
                        output.write(buffer, 0, length);
                    }
                }

                requireActivity().runOnUiThread(() -> {
                    pdfView.fromFile(pdfFile)
                            .enableSwipe(true)
                            .swipeHorizontal(false)
                            .enableDoubletap(true)
                            .defaultPage(0)
                            .onLoad(nbPages -> progressBar.setVisibility(View.GONE))
                            .onError(error -> {
                                showError("Ошибка загрузки PDF: " + error.getMessage());
                                progressBar.setVisibility(View.GONE);
                            })
                            .load();
                });

            } catch (IOException e) {
                String finalMessage = e.getMessage();
                requireActivity().runOnUiThread(() -> {
                    if (finalMessage != null && finalMessage.contains("CertPathValidatorException")) {
                        showError("Не удалось загрузить PDF: недоверенный сертификат.");
                    } else {
                        showError("Ошибка сети: " + finalMessage);
                    }
                    progressBar.setVisibility(View.GONE);
                });
            }
        }).start();
    }




    private void showError(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        // Возвращаемся назад при ошибке
        requireActivity().getSupportFragmentManager().popBackStack();
    }
    private void trustAllCertificates() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    }
            };

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    public void onDestroyView() {
        // Закрываем PDF для освобождения ресурсов
        if (pdfView != null) {
            pdfView.recycle();
        }
        super.onDestroyView();
    }
}