package com.uteq.software.labrumiologia.detection;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;

import com.uteq.software.labrumiologia.model.Detection;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * YOLOv8/YOLO11/YOLO26 TFLite detector (Ultralytics export: output [1, 4+nc, num_anchors]).
 */
public class YoloDetector implements AutoCloseable {
    public static final String MODEL_FILE = "model.tflite";
    public static final String LABELS_FILE = "labels.txt";
    public static final float CONF_THRESHOLD = 0.60f;
    public static final float IOU_THRESHOLD = 0.45f;
    public static final float MIN_CLASS_MARGIN = 0.18f;
    public static final int MAX_DETECTIONS = 2;

    private final Interpreter interpreter;
    private final List<String> labels;
    private final int numClasses;
    private final int inputSize;
    private final boolean nchw;
    private final ByteBuffer inputBuffer;
    private final int[] intValues;
    private final Paint letterboxPaint = new Paint(Paint.FILTER_BITMAP_FLAG);

    private float scale = 1f;
    private float padX = 0f;
    private float padY = 0f;
    private int srcWidth;
    private int srcHeight;

    public YoloDetector(Context context) throws IOException {
        interpreter = new Interpreter(loadModelFile(context, MODEL_FILE));
        labels = loadLabels(context, LABELS_FILE);
        numClasses = labels.size();
        int[] inShape = interpreter.getInputTensor(0).shape();
        // NHWC [1,H,W,3] or NCHW [1,3,H,W]
        if (inShape.length == 4 && inShape[1] == 3) {
            nchw = true;
            inputSize = inShape[2];
        } else if (inShape.length == 4) {
            nchw = false;
            inputSize = inShape[1];
        } else {
            nchw = false;
            inputSize = 640;
        }
        intValues = new int[inputSize * inputSize];
        inputBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4);
        inputBuffer.order(ByteOrder.nativeOrder());
    }

    public boolean isReady() {
        return interpreter != null && !labels.isEmpty();
    }

    public synchronized List<Detection> detect(Bitmap bitmap) {
        srcWidth = bitmap.getWidth();
        srcHeight = bitmap.getHeight();
        Bitmap letterboxed = letterbox(bitmap);
        fillInput(letterboxed);
        if (letterboxed != bitmap) letterboxed.recycle();

        int[] shape = interpreter.getOutputTensor(0).shape();
        int dim1 = shape.length >= 3 ? shape[1] : 0;
        int dim2 = shape.length >= 3 ? shape[2] : 0;
        boolean transposed = dim1 > 0 && dim1 < dim2;
        int nc = Math.min(numClasses, Math.max(1, (transposed ? dim1 : dim2) - 4));
        float[][][] output = new float[1][dim1][dim2];
        interpreter.run(inputBuffer, output);
        return postprocess(output[0], transposed ? dim2 : dim1, nc, transposed);
    }

    public int getSourceWidth() {
        return srcWidth;
    }

    public int getSourceHeight() {
        return srcHeight;
    }

    private List<Detection> postprocess(float[][] pred, int count, int nc, boolean transposed) {
        List<Detection> raw = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            if (!transposed && pred[i].length < 4 + nc) continue;
            float best = 0f;
            float second = 0f;
            int bestClass = -1;
            for (int c = 0; c < nc; c++) {
                float s = transposed ? pred[4 + c][i] : pred[i][4 + c];
                if (s > best) {
                    second = best;
                    best = s;
                    bestClass = c;
                } else if (s > second) {
                    second = s;
                }
            }
            if (bestClass < 0 || best < CONF_THRESHOLD || best - second < MIN_CLASS_MARGIN) continue;
            float cx = transposed ? pred[0][i] : pred[i][0];
            float cy = transposed ? pred[1][i] : pred[i][1];
            float w = transposed ? pred[2][i] : pred[i][2];
            float h = transposed ? pred[3][i] : pred[i][3];
            String id = labels.get(bestClass);
            raw.add(new Detection(id, displayName(id), best, toSource(boxFromCenter(cx, cy, w, h))));
        }
        return nms(raw);
    }

    private RectF toSource(RectF box) {
        return new RectF(
                clamp((box.left - padX) / scale, 0, srcWidth),
                clamp((box.top - padY) / scale, 0, srcHeight),
                clamp((box.right - padX) / scale, 0, srcWidth),
                clamp((box.bottom - padY) / scale, 0, srcHeight)
        );
    }

    /** Ultralytics may export boxes in pixels (0..imgsz) or normalized (0..1). */
    private RectF boxFromCenter(float cx, float cy, float w, float h) {
        if (cx <= 1.5f && cy <= 1.5f && w <= 2f && h <= 2f) {
            cx *= inputSize;
            cy *= inputSize;
            w *= inputSize;
            h *= inputSize;
        }
        return new RectF(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f);
    }

    private List<Detection> nms(List<Detection> detections) {
        Collections.sort(detections, (a, b) -> Float.compare(b.confidence, a.confidence));
        List<Detection> result = new ArrayList<>();
        boolean[] removed = new boolean[detections.size()];
        for (int i = 0; i < detections.size(); i++) {
            if (removed[i]) continue;
            Detection a = detections.get(i);
            result.add(a);
            if (result.size() >= MAX_DETECTIONS) break;
            for (int j = i + 1; j < detections.size(); j++) {
                if (removed[j]) continue;
                Detection b = detections.get(j);
                if (iou(a.box, b.box) > IOU_THRESHOLD) {
                    removed[j] = true;
                }
            }
        }
        return result;
    }

    private static float iou(RectF a, RectF b) {
        float interLeft = Math.max(a.left, b.left);
        float interTop = Math.max(a.top, b.top);
        float interRight = Math.min(a.right, b.right);
        float interBottom = Math.min(a.bottom, b.bottom);
        float inter = Math.max(0, interRight - interLeft) * Math.max(0, interBottom - interTop);
        float union = a.width() * a.height() + b.width() * b.height() - inter;
        return union <= 0 ? 0 : inter / union;
    }

    private Bitmap letterbox(Bitmap src) {
        scale = Math.min((float) inputSize / src.getWidth(), (float) inputSize / src.getHeight());
        int newW = Math.round(src.getWidth() * scale);
        int newH = Math.round(src.getHeight() * scale);
        padX = (inputSize - newW) / 2f;
        padY = (inputSize - newH) / 2f;
        Bitmap out = Bitmap.createBitmap(inputSize, inputSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        canvas.drawColor(Color.rgb(114, 114, 114));
        Matrix m = new Matrix();
        m.postScale(scale, scale);
        m.postTranslate(padX, padY);
        canvas.drawBitmap(src, m, letterboxPaint);
        return out;
    }

    private void fillInput(Bitmap bitmap) {
        inputBuffer.rewind();
        bitmap.getPixels(intValues, 0, inputSize, 0, 0, inputSize, inputSize);
        if (nchw) {
            for (int c = 0; c < 3; c++) {
                int shift = 16 - 8 * c;
                for (int i = 0; i < intValues.length; i++) {
                    inputBuffer.putFloat(((intValues[i] >> shift) & 0xFF) / 255f);
                }
            }
            return;
        }
        for (int pixel : intValues) {
            inputBuffer.putFloat(((pixel >> 16) & 0xFF) / 255f);
            inputBuffer.putFloat(((pixel >> 8) & 0xFF) / 255f);
            inputBuffer.putFloat((pixel & 0xFF) / 255f);
        }
    }

    private static String displayName(String classId) {
        String[] parts = classId.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(p.substring(0, 1).toUpperCase(Locale.ROOT));
            if (p.length() > 1) sb.append(p.substring(1));
        }
        return sb.toString();
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private static MappedByteBuffer loadModelFile(Context context, String file) throws IOException {
        AssetFileDescriptor fd = context.getAssets().openFd(file);
        try (FileInputStream is = new FileInputStream(fd.getFileDescriptor())) {
            FileChannel channel = is.getChannel();
            return channel.map(FileChannel.MapMode.READ_ONLY, fd.getStartOffset(), fd.getDeclaredLength());
        }
    }

    private static List<String> loadLabels(Context context, String file) throws IOException {
        List<String> list = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open(file)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) list.add(line);
            }
        }
        return list;
    }

    @Override
    public void close() {
        interpreter.close();
    }
}
