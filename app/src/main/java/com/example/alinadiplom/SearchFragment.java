package com.example.alinadiplom;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.google.android.flexbox.FlexboxLayout;

public class SearchFragment extends Fragment {

    private EditText searchInput;
    private ImageButton clearButton;
    private FlexboxLayout tagsContainer;
    private LinearLayout historyContainer;

    private final String[] tags = {
            "#таро", "#нумерология", "#ноготочки",
            "#мат.анализ", "#программирование", "#питон"
    };

    public SearchFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        searchInput = view.findViewById(R.id.searchInput);
        clearButton = view.findViewById(R.id.clearButton);
        tagsContainer = view.findViewById(R.id.tagsContainer);
        historyContainer = view.findViewById(R.id.historyContainer);

        clearButton.setOnClickListener(v -> searchInput.setText(""));

        loadTags();
        loadSearchHistory(); // можно позже заменить на реальные данные

        return view;
    }

    private void loadTags() {
        tagsContainer.removeAllViews();
        for (String tag : tags) {
            TextView tagView = new TextView(requireContext());
            tagView.setText(tag);
            tagView.setTextSize(14);
            tagView.setBackgroundResource(R.drawable.tag_background);
            tagView.setPadding(24, 12, 24, 12);

            tagView.setOnClickListener(v -> {
                searchInput.setText(tag);
                performSearch(tag);
            });

            FlexboxLayout.LayoutParams params = new FlexboxLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(12, 12, 12, 12);
            tagView.setLayoutParams(params);

            tagsContainer.addView(tagView);
        }
    }

    private void loadSearchHistory() {
        String[] history = {"Кузнецова Екатерина", "Петров Семён", "питон", "замена окон"};
        for (String item : history) {
            View historyItem = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_history, historyContainer, false);
            TextView text = historyItem.findViewById(R.id.history_item_text);
            text.setText(item);

            historyItem.setOnClickListener(v -> {
                searchInput.setText(item);
                performSearch(item);
            });

            historyContainer.addView(historyItem);
        }
    }

    private void performSearch(String query) {
        if (TextUtils.isEmpty(query)) return;

        // TODO: Здесь логика фильтрации людей, объявлений, мероприятий
        // можно использовать SharedViewModel, Firebase запрос или просто колбэк
    }
}
