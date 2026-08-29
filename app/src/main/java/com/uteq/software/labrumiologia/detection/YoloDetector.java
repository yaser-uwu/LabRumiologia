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
 * YOLOv8 TFLite detector (Ultralytics export: output [1, 4+nc, num_anchors]).
 */
public class YoloDetector implements AutoCloseable {
    public static final String MODEL_FILE = "model.tflite";
    public static final String LABELS_FILE = "labels.txt";
    public static final float CONF_THRESHOLD = 0.45f;
    public static final float IOU_THRESHOLD = 0.45f;

    private final Interpreter interpreter;
    private final List<String> labels;
    private final int numClasses;
    private final int inputSize;
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
            inputSize = inShape[2];
        } else if (inShape.length == 4) {
            inputSize = inShape[1];
        } else {
            inputSize = 640;
        }
        intValues = new int[inputSize * inputSize];
        inputBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4);
        inputBuffer.order(ByteOrder.nativeOrder());
    }

    public boolean isReady() {
        return interpreter != null && !labels.isEmpty();
    }

    public List<String> getLabels() {
        return labels;
    }

    public synchronized List<Detection> detect(Bitmap bitmap) {
        srcWidth = bitmap.getWidth();
        srcHeight = bitmap.getHeight();
        Bitmap letterboxed = letterbox(bitmap);
        fillInput(letterboxed);
        if (letterboxed != bitmap) {
            letterboxed.recycle();
        }

        int[] shape = interpreter.getOutputTensor(0).shape();
        // Ultralytics: [1, 4+nc, N] or [1, N, 4+nc]
        float[][][] output;
        if (shape.length == 3 && shape[1] == 4 + numClasses) {
            output = new float[1][shape[1]][shape[2]];
            interpreter.run(inputBuffer, output);
            return postprocessTransposed(output[0], shape[2]);
        } else if (shape.length == 3 && shape[2] == 4 + numClasses) {
            output = new float[1][shape[1]][shape[2]];
            interpreter.run(inputBuffer, output);
            return postprocessRows(output[0], shape[1]);
        } else {
            // Fallback: try [1, 4+nc, N]
            int channels = 4 + numClasses;
            int anchors = shape.length >= 3 ? shape[shape.length - 1] : 8400;
            if (shape.length == 3 && shape[1] != channels) {
                anchors = shape[1];
                channels = shape[2];
            }
            float[][][] out = new float[1][Math.min(channels, shape[1])][anchors];
            try {
                interpreter.run(inputBuffer, out);
                if (out[0].length == 4 + numClasses) {
                    return postprocessTransposed(out[0], anchors);
                }
                return postprocessRows(out[0], out[0].length);
            } catch (Exception e) {
                return Collections.emptyList();
            }
        }
    }

    /** Map model letterbox coords to original bitmap coords. */
    public RectF mapToSource(RectF modelBox) {
        float left = (modelBox.left - padX) / scale;
        float top = (modelBox.top - padY) / scale;
        float right = (modelBox.right - padX) / scale;
        float bottom = (modelBox.bottom - padY) / scale;
        return new RectF(
                clamp(left, 0, srcWidth),
                clamp(top, 0, srcHeight),
                clamp(right, 0, srcWidth),
                clamp(bottom, 0, srcHeight)
        );
    }

    public int getSourceWidth() {
        return srcWidth;
    }

    public int getSourceHeight() {
        return srcHeight;
    }

    private List<Detection> postprocessTransposed(float[][] pred, int numAnchors) {
        // pred[0..3][i] = cx,cy,w,h ; pred[4..4+nc)[i] = class scores
        List<Detection> raw = new ArrayList<>();
        for (int i = 0; i < numAnchors; i++) {
            float bestScore = 0f;
            int bestClass = -1;
            for (int c = 0; c < numClasses; c++) {
                float score = pred[4 + c][i];
                if (score > bestScore) {
                    bestScore = score;
                    bestClass = c;
                }
            }
            if (bestScore < CONF_THRESHOLD || bestClass < 0) continue;
            float cx = pred[0][i];
            float cy = pred[1][i];
            float w = pred[2][i];
            float h = pred[3][i];
            RectF box = new RectF(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f);
            String classId = labels.get(bestClass);
            raw.add(new Detection(classId, displayName(classId), bestScore, mapToSource(box)));
        }
        return nms(raw);
    }

    private List<Detection> postprocessRows(float[][] pred, int numRows) {
        List<Detection> raw = new ArrayList<>();
        for (int i = 0; i < numRows; i++) {
            float[] row = pred[i];
            if (row.length < 4 + numClasses) continue;
            float bestScore = 0f;
            int bestClass = -1;
            for (int c = 0; c < numClasses; c++) {
                float score = row[4 + c];
                if (score > bestScore) {
                    bestScore = score;
                    bestClass = c;
                }
            }
            if (bestScore < CONF_THRESHOLD || bestClass < 0) continue;
            float cx = row[0], cy = row[1], w = row[2], h = row[3];
            RectF box = new RectF(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f);
            String classId = labels.get(bestClass);
            raw.add(new Detection(classId, displayName(classId), bestScore, mapToSource(box)));
        }
        return nms(raw);
    }

    private List<Detection> nms(List<Detection> detections) {
        Collections.sort(detections, (a, b) -> Float.compare(b.confidence, a.confidence));
        List<Detection> result = new ArrayList<>();
        boolean[] removed = new boolean[detections.size()];
        for (int i = 0; i < detections.size(); i++) {
            if (removed[i]) continue;
            Detection a = detections.get(i);
            result.add(a);
            for (int j = i + 1; j < detections.size(); j++) {
                if (removed[j]) continue;
                Detection b = detections.get(j);
                if (a.classId.equals(b.classId) && iou(a.box, b.box) > IOU_THRESHOLD) {
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
        int idx = 0;
        for (int y = 0; y < inputSize; y++) {
            for (int x = 0; x < inputSize; x++) {
                int pixel = intValues[idx++];
                inputBuffer.putFloat(((pixel >> 16) & 0xFF) / 255f);
                inputBuffer.putFloat(((pixel >> 8) & 0xFF) / 255f);
                inputBuffer.putFloat((pixel & 0xFF) / 255f);
            }
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
