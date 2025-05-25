package com.example.alinadiplom;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AdminRequestsAdapter
        extends RecyclerView.Adapter<AdminRequestsAdapter.ViewHolder> {

    public interface OnActionListener {
        void onApprove(AdminRequest req);
        void onDecline(AdminRequest req);
    }

    private List<AdminRequest> requests;
    private OnActionListener listener;

    public AdminRequestsAdapter(List<AdminRequest> requests,
                                OnActionListener listener) {
        this.requests = requests;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        AdminRequest req = requests.get(pos);
        h.tvFio.setText(req.fio);
        h.tvNumber.setText(req.number);
        h.btnApprove.setOnClickListener(v -> listener.onApprove(req));
        h.btnDecline.setOnClickListener(v -> listener.onDecline(req));
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFio, tvNumber;
        Button btnApprove, btnDecline;
        ViewHolder(View item) {
            super(item);
            tvFio      = item.findViewById(R.id.tvFio);
            tvNumber   = item.findViewById(R.id.tvNumber);
            btnApprove = item.findViewById(R.id.btnApprove);
            btnDecline = item.findViewById(R.id.btnDecline);
        }
    }
}
