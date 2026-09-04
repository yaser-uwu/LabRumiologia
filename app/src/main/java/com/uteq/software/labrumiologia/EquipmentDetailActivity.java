package com.uteq.software.labrumiologia;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.uteq.software.labrumiologia.data.EquipmentRepository;
import com.uteq.software.labrumiologia.model.EquipmentInfo;

import java.io.IOException;
import java.io.InputStream;
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
        View imageCard = findViewById(R.id.equipmentImageCard);
        ImageView imageView = findViewById(R.id.equipmentImage);
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

        // Siempre preferir la foto de catálogo (encuadre limpio) frente al recorte de cámara.
        if (!bindCatalogPhoto(imageView, equipmentId)) {
            imageCard.setVisibility(View.GONE);
        } else {
            imageView.setContentDescription(getString(R.string.equipment_photo));
        }

        findViewById(R.id.btnChat).setOnClickListener(v -> {
            Intent i = new Intent(this, ChatActivity.class);
            i.putExtra(DetectionActivity.EXTRA_EQUIPMENT_ID, equipmentId);
            i.putExtra(DetectionActivity.EXTRA_EQUIPMENT_LABEL, equipmentLabel);
            startActivity(i);
        });
    }

    private boolean bindCatalogPhoto(ImageView imageView, String classId) {
        if (classId == null) return false;
        String assetPath = "equipment_photos/" + classId + ".jpg";
        try (InputStream in = getAssets().open(assetPath)) {
            Bitmap ref = BitmapFactory.decodeStream(in);
            if (ref == null) return false;
            imageView.setImageBitmap(ref);
            return true;
        } catch (IOException e) {
            return false;
        }
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
