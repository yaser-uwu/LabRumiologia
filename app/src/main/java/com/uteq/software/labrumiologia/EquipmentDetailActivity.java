package com.uteq.software.labrumiologia;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

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
        int pct = Math.round(getIntent().getFloatExtra(DetectionActivity.EXTRA_CONFIDENCE, 0f) * 100);

        TextView title = findViewById(R.id.equipmentTitle);
        TextView confidenceView = findViewById(R.id.equipmentConfidence);
        TextView function = findViewById(R.id.equipmentFunction);
        TextView components = findViewById(R.id.equipmentComponents);
        TextView usage = findViewById(R.id.equipmentUsage);
        TextView safety = findViewById(R.id.equipmentSafety);

        EquipmentInfo info = equipmentId != null ? new EquipmentRepository(this).get(equipmentId) : null;
        confidenceView.setText(getString(R.string.confidence_badge, pct));
        if (info != null) {
            equipmentLabel = info.name;
            title.setText(info.name);
            function.setText(info.function);
            components.setText(join(info.components));
            usage.setText(info.usage);
            safety.setText(info.safety);
        } else {
            title.setText(equipmentLabel != null ? equipmentLabel : equipmentId);
            function.setText(R.string.ficha_missing);
            components.setText("-");
            usage.setText("-");
            safety.setText("-");
        }

        findViewById(R.id.btnChat).setOnClickListener(v -> {
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
