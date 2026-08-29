package com.uteq.software.labrumiologia;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.uteq.software.labrumiologia.data.EquipmentRepository;
import com.uteq.software.labrumiologia.model.EquipmentInfo;

import java.util.List;

public class EquipmentDetailActivity extends AppCompatActivity {
    private String equipmentId;
    private String equipmentLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_equipment_detail);

        equipmentId = getIntent().getStringExtra(DetectionActivity.EXTRA_EQUIPMENT_ID);
        equipmentLabel = getIntent().getStringExtra(DetectionActivity.EXTRA_EQUIPMENT_LABEL);
        float confidence = getIntent().getFloatExtra(DetectionActivity.EXTRA_CONFIDENCE, 0f);

        TextView title = findViewById(R.id.equipmentTitle);
        TextView function = findViewById(R.id.equipmentFunction);
        TextView components = findViewById(R.id.equipmentComponents);
        TextView usage = findViewById(R.id.equipmentUsage);
        TextView safety = findViewById(R.id.equipmentSafety);
        MaterialButton btnChat = findViewById(R.id.btnChat);

        EquipmentRepository repo = new EquipmentRepository(this);
        EquipmentInfo info = equipmentId != null ? repo.get(equipmentId) : null;

        if (info != null) {
            equipmentLabel = info.name;
            title.setText(info.name + " (" + Math.round(confidence * 100) + "%)");
            function.setText(info.function);
            components.setText(join(info.components));
            usage.setText(info.usage);
            safety.setText(info.safety);
        } else {
            title.setText((equipmentLabel != null ? equipmentLabel : equipmentId) + " (" + Math.round(confidence * 100) + "%)");
            function.setText("No hay ficha local para esta clase. Consulte al asistente RAG o al responsable del laboratorio.");
            components.setText("-");
            usage.setText("-");
            safety.setText("-");
        }

        btnChat.setOnClickListener(v -> {
            Intent i = new Intent(this, ChatActivity.class);
            i.putExtra(DetectionActivity.EXTRA_EQUIPMENT_ID, equipmentId);
            i.putExtra(DetectionActivity.EXTRA_EQUIPMENT_LABEL, equipmentLabel);
            startActivity(i);
        });
    }

    private static String join(List<String> items) {
        if (items == null || items.isEmpty()) return "-";
        StringBuilder sb = new StringBuilder();
        for (String s : items) {
            sb.append("• ").append(s).append('\n');
        }
        return sb.toString().trim();
    }
}
