package org.newagecoding.yitaptap;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TapAccessibilityService extends AccessibilityService {
    private static volatile TapAccessibilityService instance;

    public static TapAccessibilityService getInstance() {
        return instance;
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private WindowManager windowManager;
    private View targetView;
    private WindowManager.LayoutParams targetParams;
    private View panelView;
    private Button startStopButton;
    private boolean running;
    private int performed;
    private long sessionId;
    private TapSettings.Config activeConfig = new TapSettings.Config(100L, 25L, 0, 0L);

    private final Runnable tapRunnable = this::performTapCycle;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        showOverlay();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
        stopTapping();
    }

    @Override
    public void onDestroy() {
        stopTapping();
        removeOverlayViews();
        if (instance == this) instance = null;
        super.onDestroy();
    }

    public void showOverlay() {
        if (windowManager == null) return;
        if (targetView == null) addTarget();
        if (panelView == null) addPanel();
        refreshControllerState();
    }

    public void hideOverlay() {
        stopTapping();
        removeOverlayViews();
    }

    public void startTapping() {
        if (running) return;
        showOverlay();
        activeConfig = TapSettings.load(this);
        performed = 0;
        sessionId++;
        running = true;
        setTargetTouchable(false);
        refreshControllerState();
        handler.removeCallbacks(tapRunnable);
        handler.postDelayed(tapRunnable, activeConfig.startDelayMs);
    }

    public void stopTapping() {
        running = false;
        sessionId++;
        handler.removeCallbacks(tapRunnable);
        setTargetTouchable(true);
        refreshControllerState();
    }

    public void refreshControllerState() {
        if (startStopButton != null) {
            startStopButton.setText(running ? "DETENER" : "INICIAR");
        }
    }

    private void performTapCycle() {
        if (!running) return;
        if (activeConfig.repetitions > 0 && performed >= activeConfig.repetitions) {
            stopTapping();
            return;
        }
        if (targetParams == null) {
            stopTapping();
            return;
        }

        int size = dp(64);
        float x = targetParams.x + size / 2f;
        float y = targetParams.y + size / 2f;
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path, 0L, activeConfig.durationMs))
                .build();

        final long thisSession = sessionId;
        boolean accepted = dispatchGesture(gesture, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                if (!running || thisSession != sessionId) return;
                performed++;
                if (activeConfig.repetitions > 0 && performed >= activeConfig.repetitions) {
                    stopTapping();
                    return;
                }
                handler.postDelayed(tapRunnable, activeConfig.intervalMs);
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                if (thisSession == sessionId) stopTapping();
            }
        }, null);

        if (!accepted && thisSession == sessionId) stopTapping();
    }

    private void addTarget() {
        final int size = dp(64);
        int screenW = getResources().getDisplayMetrics().widthPixels;
        int screenH = getResources().getDisplayMetrics().heightPixels;
        int defaultX = (screenW - size) / 2;
        int defaultY = (screenH - size) / 2;

        targetParams = new WindowManager.LayoutParams(
                size,
                size,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        targetParams.gravity = Gravity.TOP | Gravity.START;
        targetParams.x = clamp(Math.round(TapSettings.prefs(this).getFloat(TapSettings.KEY_TARGET_X, defaultX)), 0, Math.max(0, screenW - size));
        targetParams.y = clamp(Math.round(TapSettings.prefs(this).getFloat(TapSettings.KEY_TARGET_Y, defaultY)), 0, Math.max(0, screenH - size));

        TextView target = new TextView(this);
        target.setText("◎");
        target.setTextSize(42f);
        target.setGravity(Gravity.CENTER);
        target.setTextColor(Color.rgb(185, 130, 255));
        target.setBackground(roundedBackground(Color.argb(90, 70, 35, 105), dp(32), Color.rgb(185, 130, 255), dp(2)));
        target.setOnTouchListener(new TargetDragListener());
        targetView = target;
        windowManager.addView(target, targetParams);
    }

    private void addPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.HORIZONTAL);
        panel.setGravity(Gravity.CENTER_VERTICAL);
        panel.setPadding(dp(10), dp(7), dp(8), dp(7));
        panel.setBackground(roundedBackground(Color.argb(235, 25, 23, 32), dp(16), Color.rgb(98, 72, 125), dp(1)));

        TextView name = new TextView(this);
        name.setText("YiTapTap");
        name.setTextSize(14f);
        name.setTextColor(Color.WHITE);
        name.setPadding(dp(4), 0, dp(8), 0);
        panel.addView(name);

        startStopButton = new Button(this);
        startStopButton.setText("INICIAR");
        startStopButton.setAllCaps(false);
        startStopButton.setOnClickListener(v -> {
            if (running) stopTapping(); else startTapping();
        });
        panel.addView(startStopButton);

        Button close = new Button(this);
        close.setText("×");
        close.setTextSize(20f);
        close.setAllCaps(false);
        close.setOnClickListener(v -> hideOverlay());
        panel.addView(close, new LinearLayout.LayoutParams(dp(52), LinearLayout.LayoutParams.WRAP_CONTENT));

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = dp(12);
        params.y = dp(72);
        panelView = panel;
        windowManager.addView(panel, params);
    }

    private void setTargetTouchable(boolean touchable) {
        if (targetView == null || targetParams == null || windowManager == null) return;
        targetParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                (touchable ? 0 : WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        try {
            windowManager.updateViewLayout(targetView, targetParams);
        } catch (Exception ignored) {
        }
    }

    private void removeOverlayViews() {
        if (windowManager == null) return;
        safeRemove(targetView);
        safeRemove(panelView);
        targetView = null;
        targetParams = null;
        panelView = null;
        startStopButton = null;
    }

    private void safeRemove(View view) {
        if (view == null) return;
        try {
            windowManager.removeView(view);
        } catch (Exception ignored) {
        }
    }

    private final class TargetDragListener implements View.OnTouchListener {
        private float downRawX;
        private float downRawY;
        private int startX;
        private int startY;

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            if (running || targetParams == null) return false;
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downRawX = event.getRawX();
                    downRawY = event.getRawY();
                    startX = targetParams.x;
                    startY = targetParams.y;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    int maxX = Math.max(0, getResources().getDisplayMetrics().widthPixels - dp(64));
                    int maxY = Math.max(0, getResources().getDisplayMetrics().heightPixels - dp(64));
                    targetParams.x = clamp(Math.round(startX + event.getRawX() - downRawX), 0, maxX);
                    targetParams.y = clamp(Math.round(startY + event.getRawY() - downRawY), 0, maxY);
                    windowManager.updateViewLayout(view, targetParams);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    TapSettings.prefs(TapAccessibilityService.this).edit()
                            .putFloat(TapSettings.KEY_TARGET_X, targetParams.x)
                            .putFloat(TapSettings.KEY_TARGET_Y, targetParams.y)
                            .apply();
                    return true;
                default:
                    return false;
            }
        }
    }

    private GradientDrawable roundedBackground(int fill, int radius, int stroke, int strokeWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(fill);
        drawable.setCornerRadius(radius);
        drawable.setStroke(strokeWidth, stroke);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
