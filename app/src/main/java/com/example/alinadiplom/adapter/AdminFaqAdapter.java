package com.example.alinadiplom.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alinadiplom.R;
import com.example.alinadiplom.model.FaqRequest;

import java.util.List;

public class AdminFaqAdapter extends RecyclerView.Adapter<AdminFaqAdapter.FaqViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(FaqRequest item);
    }

    private List<FaqRequest> faqList;
    private final OnItemClickListener listener;

    public AdminFaqAdapter(List<FaqRequest> faqList, OnItemClickListener listener) {
        this.faqList = faqList;
        this.listener = listener;
    }

    public void updateData(List<FaqRequest> newList) {
        faqList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FaqViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_faq, parent, false);
        return new FaqViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull FaqViewHolder holder, int position) {
        FaqRequest item = faqList.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return faqList.size();
    }

    static class FaqViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvQuestion, tvAnswer;

        public FaqViewHolder(@NonNull View itemView) {
            super(itemView);
            tvQuestion = itemView.findViewById(R.id.tvFaqQuestion);
            tvAnswer = itemView.findViewById(R.id.tvFaqAnswer);
        }

        public void bind(FaqRequest item, OnItemClickListener listener) {
            tvQuestion.setText(item.getQuestion());
            tvAnswer.setText(item.isAnswered() ? item.getAnswer() : "Ответ не дан");

            itemView.setOnClickListener(v -> listener.onItemClick(item));
        }
    }
}
