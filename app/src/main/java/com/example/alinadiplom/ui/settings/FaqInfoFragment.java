package com.example.alinadiplom.ui.settings;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.alinadiplom.R;
import com.example.alinadiplom.adapter.FaqAdapter;
import com.example.alinadiplom.model.FaqItem;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FaqInfoFragment extends Fragment {

    private RecyclerView faqRecyclerView;
    private FaqAdapter adapter;
    private List<FaqItem> faqList = new ArrayList<>();

    private DatabaseReference faqRef;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_faq_info, container, false);

        faqRecyclerView = view.findViewById(R.id.faqRecyclerView);
        faqRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new FaqAdapter(faqList);
        faqRecyclerView.setAdapter(adapter);

        faqRef = FirebaseDatabase.getInstance().getReference("faq");

        loadFaq();

        return view;
    }

    private void loadFaq() {
        faqRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                faqList.clear();
                for (DataSnapshot item : snapshot.getChildren()) {
                    FaqItem faq = item.getValue(FaqItem.class);
                    if (faq != null) {
                        faqList.add(faq);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Ошибка загрузки FAQ", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
