package com.example.alinadiplom;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AdminRequestsAdapter extends RecyclerView.Adapter<AdminRequestsAdapter.ViewHolder> {

    public interface OnActionListener {
        void onApprove(AdminRequest request);
        void onDecline(AdminRequest request);
    }

    private List<AdminRequest> requests = new ArrayList<>();
    private final OnActionListener listener;

    public AdminRequestsAdapter(OnActionListener listener) {
        this.listener = listener;
    }

    // Метод для обновления списка с использованием DiffUtil
    public void updateRequests(List<AdminRequest> newRequests) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffCallback(requests, newRequests));
        requests.clear();
        requests.addAll(newRequests);
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AdminRequest request = requests.get(position);
        holder.tvFio.setText(request.fio != null ? request.fio : "Не указано");
        holder.tvNumber.setText(request.number != null ? request.number : "Не указано");

        // Отображение времени запроса
        if (request.timestamp != 0) {
            String timeAgo = DateUtils.getRelativeTimeSpanString(
                    request.timestamp,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
            ).toString();
            holder.tvTimestamp.setText("Запрос создан: " + timeAgo);
        } else {
            holder.tvTimestamp.setText("Время не указано");
        }

        // Обработчики кнопок
        holder.btnApprove.setOnClickListener(v -> {
            if (listener != null) {
                listener.onApprove(request);
            }
        });
        holder.btnDecline.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDecline(request);
            }
        });
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFio, tvNumber, tvTimestamp;
        Button btnApprove, btnDecline;

        ViewHolder(View itemView) {
            super(itemView);
            tvFio = itemView.findViewById(R.id.tvFio);
            tvNumber = itemView.findViewById(R.id.tvNumber);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnDecline = itemView.findViewById(R.id.btnDecline);
        }
    }

    static class DiffCallback extends DiffUtil.Callback {
        private final List<AdminRequest> oldList;
        private final List<AdminRequest> newList;

        DiffCallback(List<AdminRequest> oldList, List<AdminRequest> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).uid.equals(newList.get(newItemPosition).uid);
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            AdminRequest oldRequest = oldList.get(oldItemPosition);
            AdminRequest newRequest = newList.get(newItemPosition);
            return (oldRequest.fio != null && oldRequest.fio.equals(newRequest.fio)) &&
                    (oldRequest.number != null && oldRequest.number.equals(newRequest.number)) &&
                    oldRequest.timestamp == newRequest.timestamp;
        }
    }
}