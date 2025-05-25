package com.example.alinadiplom;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PeopleAdapter extends RecyclerView.Adapter<PeopleAdapter.PersonViewHolder> {

    private List<PersonItem> peopleList;

    public PeopleAdapter(List<PersonItem> peopleList) {
        this.peopleList = peopleList;
    }

    @NonNull
    @Override
    public PersonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_person, parent, false);
        return new PersonViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PersonViewHolder holder, int position) {
        PersonItem person = peopleList.get(position);
        holder.name.setText(person.getName());
        holder.room.setText(person.getRoom());
        holder.message.setText(person.getMessage());
        holder.tags.setText(person.getTags());
        holder.avatar.setImageResource(person.getAvatarResId());
    }

    @Override
    public int getItemCount() {
        return peopleList.size();
    }

    static class PersonViewHolder extends RecyclerView.ViewHolder {
        TextView name, room, message, tags;
        ImageView avatar;

        public PersonViewHolder(@NonNull View itemView) {
            super(itemView);
            room = itemView.findViewById(R.id.personRoom);
            name = itemView.findViewById(R.id.personName);
            message = itemView.findViewById(R.id.personMessage);
            tags = itemView.findViewById(R.id.personTags);
            avatar = itemView.findViewById(R.id.personPhoto);
        }
    }
}

