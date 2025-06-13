package com.example.alinadiplom.adapter;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alinadiplom.R;
import com.example.alinadiplom.model.ServiceRequest;

import java.util.List;

public class ServiceRequestAdapter extends RecyclerView.Adapter<ServiceRequestAdapter.RequestViewHolder> {
    private List<ServiceRequest> requestList;

    public ServiceRequestAdapter(List<ServiceRequest> requestList) {
        this.requestList = requestList;
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_request, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        ServiceRequest request = requestList.get(position);
        holder.type.setText("Тип: " + request.getType());
        holder.problem.setText("Проблема: " + request.getProblem());
        holder.room.setText("Комната: " + request.getRoom());
        holder.status.setText("Статус: " + request.getStatus());

        String timeStr = DateFormat.format("dd.MM.yyyy HH:mm", request.getTimestamp()).toString();
        holder.time.setText("Время: " + timeStr);
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    static class RequestViewHolder extends RecyclerView.ViewHolder {
        TextView type, problem, room, status, time;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            type = itemView.findViewById(R.id.textType);
            problem = itemView.findViewById(R.id.textProblem);
            room = itemView.findViewById(R.id.textRoom);
            status = itemView.findViewById(R.id.textStatus);
            time = itemView.findViewById(R.id.textTime);
        }
    }
}
