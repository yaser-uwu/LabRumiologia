package com.uteq.software.labrumiologia.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.uteq.software.labrumiologia.R;
import com.uteq.software.labrumiologia.model.Detection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DetectionAdapter extends RecyclerView.Adapter<DetectionAdapter.Holder> {
    public interface Listener {
        void onClick(Detection detection, int index);
    }

    private final List<Detection> items = new ArrayList<>();
    private int selected = -1;
    private final Listener listener;

    public DetectionAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Detection> detections, int selectedIndex) {
        items.clear();
        if (detections != null) items.addAll(detections);
        selected = selectedIndex;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_detection, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Detection d = items.get(position);
        holder.label.setText(d.label);
        holder.confidence.setText(String.format(Locale.getDefault(), "%.0f%%", d.confidence * 100f));
        boolean on = position == selected;
        holder.card.setStrokeWidth(on ? 3 : 0);
        holder.card.setStrokeColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.accent));
        holder.itemView.setOnClickListener(v -> listener.onClick(d, holder.getBindingAdapterPosition()));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final TextView label;
        final TextView confidence;

        Holder(@NonNull View itemView) {
            super(itemView);
            card = (MaterialCardView) itemView;
            label = itemView.findViewById(R.id.itemLabel);
            confidence = itemView.findViewById(R.id.itemConfidence);
        }
    }
}
