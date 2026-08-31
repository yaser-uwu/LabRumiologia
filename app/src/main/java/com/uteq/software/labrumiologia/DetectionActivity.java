package com.uteq.software.labrumiologia;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.uteq.software.labrumiologia.detection.YoloDetector;
import com.uteq.software.labrumiologia.model.Detection;
import com.uteq.software.labrumiologia.ui.DetectionAdapter;
import com.uteq.software.labrumiologia.ui.DetectionOverlayView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class DetectionActivity extends AppCompatActivity {
    public static final String EXTRA_EQUIPMENT_ID = "equipment_id";
    public static final String EXTRA_EQUIPMENT_LABEL = "equipment_label";
    public static final String EXTRA_CONFIDENCE = "confidence";

    private static final int REQ_CAMERA = 100;

    private PreviewView previewView;
    private DetectionOverlayView overlayView;
    private TextView statusText;
    private MaterialButton btnInfo;
    private DetectionAdapter adapter;

    private YoloDetector detector;
    private ExecutorService analysisExecutor;
    private final AtomicBoolean busy = new AtomicBoolean(false);

    private final List<Detection> latestDetections = new ArrayList<>();
    private int selectedIndex = -1;
    private boolean modelAvailable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detection);

        previewView = findViewById(R.id.previewView);
        previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);
        previewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        overlayView = findViewById(R.id.overlayView);
        statusText = findViewById(R.id.statusText);
        btnInfo = findViewById(R.id.btnInfo);
        RecyclerView list = findViewById(R.id.detectionsList);
        list.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        adapter = new DetectionAdapter(this::selectDetection);
        list.setAdapter(adapter);

        overlayView.setOnDetectionTapListener(this::selectDetection);
        btnInfo.setOnClickListener(v -> openDetail());
        analysisExecutor = Executors.newSingleThreadExecutor();

        try {
            detector = new YoloDetector(this);
            modelAvailable = detector.isReady();
            statusText.setText(R.string.detecting);
        } catch (Exception e) {
            modelAvailable = false;
            statusText.setText(R.string.model_missing);
            Toast.makeText(this, R.string.model_missing, Toast.LENGTH_LONG).show();
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
        }
    }

    private void selectDetection(Detection detection, int index) {
        selectedIndex = index;
        overlayView.setSelectedIndex(index);
        adapter.submit(new ArrayList<>(latestDetections), selectedIndex);
        btnInfo.setEnabled(true);
        statusText.setText(detection.label + " · " + Math.round(detection.confidence * 100) + "%");
    }

    private void openDetail() {
        if (selectedIndex < 0 || selectedIndex >= latestDetections.size()) {
            Toast.makeText(this, R.string.select_equipment, Toast.LENGTH_SHORT).show();
            return;
        }
        Detection d = latestDetections.get(selectedIndex);
        Intent i = new Intent(this, EquipmentDetailActivity.class);
        i.putExtra(EXTRA_EQUIPMENT_ID, d.classId);
        i.putExtra(EXTRA_EQUIPMENT_LABEL, d.label);
        i.putExtra(EXTRA_CONFIDENCE, d.confidence);
        startActivity(i);
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                bindCamera(provider);
            } catch (Exception e) {
                statusText.setText("Error al iniciar cámara: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCamera(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(
                this,
                new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build(),
                preview
        );
        previewView.removeCallbacks(frameLoop);
        previewView.post(frameLoop);
    }

    private void scheduleNext() {
        if (!isDestroyed() && previewView != null) previewView.postDelayed(frameLoop, 80);
    }

    private final Runnable frameLoop = () -> {
        if (isDestroyed() || isFinishing()) return;
        if (!modelAvailable || detector == null || !busy.compareAndSet(false, true)) {
            scheduleNext();
            return;
        }
        Bitmap frame = previewView.getBitmap();
        if (frame == null) {
            busy.set(false);
            scheduleNext();
            return;
        }
        analysisExecutor.execute(() -> {
            try {
                List<Detection> detections = detector.detect(frame);
                int w = detector.getSourceWidth();
                int h = detector.getSourceHeight();
                frame.recycle();
                runOnUiThread(() -> {
                    if (!isDestroyed()) showDetections(detections, w, h);
                });
            } catch (Exception e) {
                runOnUiThread(() -> statusText.setText("Error de inferencia: " + e.getMessage()));
            } finally {
                busy.set(false);
                scheduleNext();
            }
        });
    };

    private void showDetections(List<Detection> detections, int srcW, int srcH) {
        latestDetections.clear();
        latestDetections.addAll(detections);
        if (selectedIndex >= latestDetections.size()) {
            selectedIndex = latestDetections.isEmpty() ? -1 : 0;
        } else if (selectedIndex < 0 && !latestDetections.isEmpty()) {
            selectedIndex = 0;
        }
        btnInfo.setEnabled(selectedIndex >= 0);
        overlayView.setImageSize(srcW, srcH);
        overlayView.setDetections(latestDetections, selectedIndex);
        adapter.submit(latestDetections, selectedIndex);
        if (latestDetections.isEmpty()) {
            statusText.setText(R.string.no_detections);
        } else if (selectedIndex >= 0) {
            Detection d = latestDetections.get(selectedIndex);
            statusText.setText(d.label + " · " + Math.round(d.confidence * 100) + "%");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (previewView != null) previewView.removeCallbacks(frameLoop);
        if (analysisExecutor != null) analysisExecutor.shutdown();
        if (detector != null) detector.close();
    }
}
