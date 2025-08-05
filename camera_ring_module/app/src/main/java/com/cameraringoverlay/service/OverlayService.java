package com.cameraringoverlay.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class OverlayService extends Service {
    private static final String TAG = "CameraRingOverlay";
    
    private WindowManager windowManager;
    private View overlayView;
    private boolean isShowing = false;
    
    // 三星S24U前置摄像头位置（根据实际设备调整）
    private static final int CAMERA_X_OFFSET = 0; // 居中
    private static final int CAMERA_Y_OFFSET = 50; // 从顶部偏移
    private static final int RING_SIZE = 120; // 圆环大小
    private static final int RING_WIDTH = 8; // 圆环宽度
    
    private BroadcastReceiver showRingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.cameraringoverlay.SHOW_RING".equals(intent.getAction())) {
                showRing();
            } else if ("com.cameraringoverlay.HIDE_RING".equals(intent.getAction())) {
                hideRing();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "OverlayService created");
        
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        // 注册广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.cameraringoverlay.SHOW_RING");
        filter.addAction("com.cameraringoverlay.HIDE_RING");
        registerReceiver(showRingReceiver, filter);
        
        // 检查权限并显示圆环
        if (hasOverlayPermission()) {
            createOverlayView();
            showRing();
        } else {
            Log.w(TAG, "No overlay permission");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "OverlayService started");
        return START_STICKY; // 服务被杀死后自动重启
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "OverlayService destroyed");
        
        hideRing();
        
        try {
            unregisterReceiver(showRingReceiver);
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering receiver", e);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private boolean hasOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(this);
        }
        return true; // 旧版本默认有权限
    }

    private void createOverlayView() {
        if (overlayView != null) {
            return;
        }

        // 创建圆环视图
        overlayView = new FrameLayout(this);
        
        // 创建白色圆环drawable
        GradientDrawable ringDrawable = new GradientDrawable();
        ringDrawable.setShape(GradientDrawable.OVAL);
        ringDrawable.setStroke(dpToPx(RING_WIDTH), Color.WHITE);
        ringDrawable.setColor(Color.TRANSPARENT);
        
        // 设置圆环透明度（可调整）
        ringDrawable.setAlpha(200); // 0-255
        
        overlayView.setBackground(ringDrawable);
        
        // 设置圆环大小
        int ringSize = dpToPx(RING_SIZE);
        overlayView.setLayoutParams(new FrameLayout.LayoutParams(ringSize, ringSize));
    }

    private void showRing() {
        if (isShowing || overlayView == null) {
            return;
        }

        try {
            // 获取屏幕尺寸
            DisplayMetrics metrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(metrics);
            
            // 计算前置摄像头位置
            int screenWidth = metrics.widthPixels;
            int xPosition = (screenWidth / 2) - (dpToPx(RING_SIZE) / 2) + dpToPx(CAMERA_X_OFFSET);
            int yPosition = dpToPx(CAMERA_Y_OFFSET);
            
            // 设置窗口参数
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                dpToPx(RING_SIZE),
                dpToPx(RING_SIZE),
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O 
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
                    : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT
            );
            
            params.gravity = Gravity.TOP | Gravity.LEFT;
            params.x = xPosition;
            params.y = yPosition;
            
            // 添加到窗口管理器
            windowManager.addView(overlayView, params);
            isShowing = true;
            
            Log.d(TAG, "Camera ring overlay shown at position: " + xPosition + "," + yPosition);
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing overlay", e);
        }
    }

    private void hideRing() {
        if (!isShowing || overlayView == null) {
            return;
        }

        try {
            windowManager.removeView(overlayView);
            isShowing = false;
            Log.d(TAG, "Camera ring overlay hidden");
        } catch (Exception e) {
            Log.e(TAG, "Error hiding overlay", e);
        }
    }
    
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}