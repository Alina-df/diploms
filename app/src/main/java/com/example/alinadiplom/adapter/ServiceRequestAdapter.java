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

public class ServiceRequestAdapter
        extends RecyclerView.Adapter<ServiceRequestAdapter.RequestViewHolder> {

    public interface OnLongClickListener {
        void onLongClick(ServiceRequest req);
    }

    private final List<ServiceRequest> requestList;
    private final OnLongClickListener longClickListener;

    public ServiceRequestAdapter(List<ServiceRequest> list,
                                 OnLongClickListener listener) {
        this.requestList = list;
        this.longClickListener = listener;
    }

    @NonNull @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_request, parent, false);
        return new RequestViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder h, int pos) {
        ServiceRequest r = requestList.get(pos);
        h.type.setText("Тип: " + r.getType());
        h.problem.setText("Проблема: " + r.getProblem());
        h.room.setText("Комната: " + r.getRoom());
        h.status.setText("Статус: " + r.getStatus());

        String timeStr = DateFormat.format("dd.MM.yyyy HH:mm", r.getTimestamp())
                .toString();
        h.time.setText("Время: " + timeStr);

        h.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) longClickListener.onLongClick(r);
            return true;
        });
    }

    @Override public int getItemCount() { return requestList.size(); }

    static class RequestViewHolder extends RecyclerView.ViewHolder {
        TextView type, problem, room, status, time;
        RequestViewHolder(@NonNull View v) {
            super(v);
            type    = v.findViewById(R.id.textType);
            problem = v.findViewById(R.id.textProblem);
            room    = v.findViewById(R.id.textRoom);
            status  = v.findViewById(R.id.textStatus);
            time    = v.findViewById(R.id.textTime);
        }
    }
}
