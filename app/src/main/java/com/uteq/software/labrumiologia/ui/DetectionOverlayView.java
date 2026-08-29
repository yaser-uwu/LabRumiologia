package com.uteq.software.labrumiologia.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.uteq.software.labrumiologia.model.Detection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DetectionOverlayView extends View {
    public interface OnDetectionTapListener {
        void onDetectionTapped(Detection detection, int index);
    }

    private final Paint boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final List<Detection> detections = new ArrayList<>();
    private final List<RectF> viewBoxes = new ArrayList<>();
    private int selectedIndex = -1;
    private int imageWidth = 1;
    private int imageHeight = 1;
    private OnDetectionTapListener tapListener;

    public DetectionOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(5f);
        boxPaint.setColor(Color.parseColor("#EEFF41"));
        selectedPaint.setStyle(Paint.Style.STROKE);
        selectedPaint.setStrokeWidth(8f);
        selectedPaint.setColor(Color.parseColor("#FF6F00"));
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(36f);
        bgPaint.setColor(Color.parseColor("#99000000"));
    }

    public void setOnDetectionTapListener(OnDetectionTapListener listener) {
        this.tapListener = listener;
    }

    public void setImageSize(int width, int height) {
        imageWidth = Math.max(1, width);
        imageHeight = Math.max(1, height);
    }

    public void setDetections(List<Detection> list, int selected) {
        detections.clear();
        if (list != null) detections.addAll(list);
        selectedIndex = selected;
        rebuildViewBoxes();
        invalidate();
    }

    public void setSelectedIndex(int index) {
        selectedIndex = index;
        invalidate();
    }

    private void rebuildViewBoxes() {
        viewBoxes.clear();
        float viewW = getWidth();
        float viewH = getHeight();
        if (viewW <= 0 || viewH <= 0) return;

        float scale = Math.max(viewW / imageWidth, viewH / imageHeight);
        float scaledW = imageWidth * scale;
        float scaledH = imageHeight * scale;
        float dx = (viewW - scaledW) / 2f;
        float dy = (viewH - scaledH) / 2f;

        for (Detection d : detections) {
            RectF src = d.box;
            viewBoxes.add(new RectF(
                    src.left * scale + dx,
                    src.top * scale + dy,
                    src.right * scale + dx,
                    src.bottom * scale + dy
            ));
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        rebuildViewBoxes();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (viewBoxes.size() != detections.size()) {
            rebuildViewBoxes();
        }
        for (int i = 0; i < detections.size(); i++) {
            Detection d = detections.get(i);
            RectF box = viewBoxes.get(i);
            Paint paint = (i == selectedIndex) ? selectedPaint : boxPaint;
            canvas.drawRect(box, paint);
            String label = String.format(Locale.getDefault(), "%s %.0f%%", d.label, d.confidence * 100f);
            float textWidth = textPaint.measureText(label);
            float top = Math.max(box.top - 44f, 0);
            canvas.drawRect(box.left, top, box.left + textWidth + 16f, top + 44f, bgPaint);
            canvas.drawText(label, box.left + 8f, top + 32f, textPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && tapListener != null) {
            float x = event.getX();
            float y = event.getY();
            for (int i = 0; i < viewBoxes.size(); i++) {
                if (viewBoxes.get(i).contains(x, y)) {
                    tapListener.onDetectionTapped(detections.get(i), i);
                    return true;
                }
            }
        }
        return super.onTouchEvent(event);
    }
}
