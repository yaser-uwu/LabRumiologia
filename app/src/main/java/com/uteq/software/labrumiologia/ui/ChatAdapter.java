package com.uteq.software.labrumiologia.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.uteq.software.labrumiologia.R;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.Holder> {
    public static class Message {
        public final String role;
        public final String body;
        public final String sources;

        public Message(String role, String body, String sources) {
            this.role = role;
            this.body = body;
            this.sources = sources;
        }
    }

    private final List<Message> items = new ArrayList<>();

    public void add(Message message) {
        items.add(message);
        notifyItemInserted(items.size() - 1);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Message m = items.get(position);
        holder.role.setText(m.role);
        holder.body.setText(m.body);
        if (m.sources != null && !m.sources.isEmpty()) {
            holder.sources.setVisibility(View.VISIBLE);
            holder.sources.setText(m.sources);
        } else {
            holder.sources.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView role;
        final TextView body;
        final TextView sources;

        Holder(@NonNull View itemView) {
            super(itemView);
            role = itemView.findViewById(R.id.messageRole);
            body = itemView.findViewById(R.id.messageBody);
            sources = itemView.findViewById(R.id.messageSources);
        }
    }
}
