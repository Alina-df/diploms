package com.example.alinadiplom.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alinadiplom.R;
import com.example.alinadiplom.model.ServiceRequest;

import java.util.List;

public class RequestsManagementAdapter extends RecyclerView.Adapter<RequestsManagementAdapter.RequestViewHolder> {
    private final List<ServiceRequest> requestList;
    private final OnAcceptClickListener listener;

    public interface OnAcceptClickListener {
        void onAccept(ServiceRequest request);

        void onReport(ServiceRequest r);
    }

    public RequestsManagementAdapter(List<ServiceRequest> list, OnAcceptClickListener listener) {
        this.requestList = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_request_admin, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        ServiceRequest req = requestList.get(position);
        holder.room.setText("Комната: " + req.getRoom());
        holder.problem.setText("Проблема: " + req.getProblem());
        holder.type.setText("Тип: " + req.getType());
        holder.status.setText("Статус: " + req.getStatus());

        boolean isPending = "Ожидает обработки".equals(req.getStatus());
        holder.acceptButton.setVisibility(isPending ? View.VISIBLE : View.GONE);
        holder.reportButton.setVisibility(isPending ? View.GONE : View.VISIBLE);

        holder.acceptButton.setOnClickListener(v -> listener.onAccept(req));
        holder.reportButton.setOnClickListener(v -> listener.onReport(req));
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    static class RequestViewHolder extends RecyclerView.ViewHolder {
        TextView room, problem, type, status;
        Button acceptButton, reportButton;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            room          = itemView.findViewById(R.id.textRoomAdmin);
            problem       = itemView.findViewById(R.id.textProblemAdmin);
            type          = itemView.findViewById(R.id.textTypeAdmin);
            status        = itemView.findViewById(R.id.textStatusAdmin);
            acceptButton  = itemView.findViewById(R.id.buttonAcceptRequest);
            reportButton  = itemView.findViewById(R.id.buttonReportRequest);
        }
    }
}
