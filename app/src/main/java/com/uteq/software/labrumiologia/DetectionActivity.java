package com.uteq.software.labrumiologia;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Bundle;
import android.util.Size;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
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

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
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
        Size target = new Size(1280, 720);
        Preview preview = new Preview.Builder().setTargetResolution(target).build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis analysis = new ImageAnalysis.Builder()
                .setTargetResolution(target)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build();
        analysis.setAnalyzer(analysisExecutor, this::analyze);

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(
                this,
                new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build(),
                preview,
                analysis
        );
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void analyze(@NonNull ImageProxy image) {
        if (!modelAvailable || detector == null || !busy.compareAndSet(false, true)) {
            image.close();
            return;
        }
        try {
            Bitmap bitmap = yuvToBitmap(image);
            if (bitmap == null) return;
            int rotation = image.getImageInfo().getRotationDegrees();
            if (rotation != 0) {
                Matrix m = new Matrix();
                m.postRotate(rotation);
                Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
                if (rotated != bitmap) bitmap.recycle();
                bitmap = rotated;
            }
            List<Detection> detections = detector.detect(bitmap);
            int srcW = detector.getSourceWidth();
            int srcH = detector.getSourceHeight();
            bitmap.recycle();

            runOnUiThread(() -> {
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
            });
        } catch (Exception e) {
            runOnUiThread(() -> statusText.setText("Error de inferencia: " + e.getMessage()));
        } finally {
            busy.set(false);
            image.close();
        }
    }

    private static Bitmap yuvToBitmap(ImageProxy image) {
        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();
        byte[] nv21 = new byte[ySize + uSize + vSize];
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 90, out);
        byte[] jpeg = out.toByteArray();
        return android.graphics.BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);
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
        if (analysisExecutor != null) analysisExecutor.shutdown();
        if (detector != null) detector.close();
    }
}
