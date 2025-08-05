package com.cameraringoverlay.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.cameraringoverlay.utils.SamsungUtils;

public class EnhancedOverlayService extends Service {
    private static final String TAG = "EnhancedOverlayService";
    private static final String PREFS_NAME = "CameraRingPrefs";
    
    private WindowManager windowManager;
    private View overlayView;
    private boolean isShowing = false;
    private SharedPreferences prefs;
    
    private BroadcastReceiver configReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("com.cameraringoverlay.SHOW_RING".equals(action)) {
                showRing();
            } else if ("com.cameraringoverlay.HIDE_RING".equals(action)) {
                hideRing();
            } else if ("com.cameraringoverlay.UPDATE_CONFIG".equals(action)) {
                updateConfiguration();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "EnhancedOverlayService created");
        
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        // 注册广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.cameraringoverlay.SHOW_RING");
        filter.addAction("com.cameraringoverlay.HIDE_RING");
        filter.addAction("com.cameraringoverlay.UPDATE_CONFIG");
        registerReceiver(configReceiver, filter);
        
        // 检查权限并显示圆环
        if (hasOverlayPermission()) {
            createOverlayView();
            if (prefs.getBoolean("enabled", false)) {
                showRing();
            }
        } else {
            Log.w(TAG, "No overlay permission");
        }
        
        // 输出设备信息
        logDeviceInfo();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "EnhancedOverlayService started");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "EnhancedOverlayService destroyed");
        
        hideRing();
        
        try {
            unregisterReceiver(configReceiver);
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
        return true;
    }

    private void createOverlayView() {
        if (overlayView != null) {
            try {
                windowManager.removeView(overlayView);
            } catch (Exception e) {
                // Ignore
            }
        }

        // 获取配置
        int ringSize = prefs.getInt("ring_size", 120);
        int ringWidth = prefs.getInt("ring_width", 8);
        int transparency = prefs.getInt("transparency", 200);
        
        // 创建圆环视图
        overlayView = new FrameLayout(this);
        
        // 创建白色圆环drawable
        GradientDrawable ringDrawable = new GradientDrawable();
        ringDrawable.setShape(GradientDrawable.OVAL);
        ringDrawable.setStroke(dpToPx(ringWidth), Color.WHITE);
        ringDrawable.setColor(Color.TRANSPARENT);
        ringDrawable.setAlpha(transparency);
        
        overlayView.setBackground(ringDrawable);
        
        Log.d(TAG, "Created overlay view with size: " + ringSize + ", width: " + ringWidth + ", transparency: " + transparency);
    }

    private void showRing() {
        if (isShowing || overlayView == null) {
            return;
        }

        try {
            // 使用三星适配工具获取摄像头位置
            SamsungUtils.CameraPosition cameraPos = SamsungUtils.getFrontCameraPosition(this);
            
            // 获取配置的圆环大小
            int configuredSize = prefs.getInt("ring_size", 120);
            int ringSize = dpToPx(configuredSize);
            
            // 计算位置（圆环居中于摄像头）
            int xPosition = cameraPos.x - (ringSize / 2);
            int yPosition = cameraPos.y - (ringSize / 2);
            
            // 考虑状态栏高度
            int statusBarHeight = SamsungUtils.getStatusBarHeight(this);
            yPosition += statusBarHeight;
            
            // 设置窗口参数
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                ringSize,
                ringSize,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O 
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
                    : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            );
            
            params.gravity = Gravity.TOP | Gravity.LEFT;
            params.x = xPosition;
            params.y = yPosition;
            
            // 添加到窗口管理器
            windowManager.addView(overlayView, params);
            isShowing = true;
            
            Log.d(TAG, "Camera ring overlay shown at position: " + xPosition + "," + yPosition + 
                  " (size: " + ringSize + ", statusBar: " + statusBarHeight + ")");
            
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
    
    private void updateConfiguration() {
        Log.d(TAG, "Updating configuration");
        
        boolean wasShowing = isShowing;
        
        if (wasShowing) {
            hideRing();
        }
        
        createOverlayView();
        
        if (wasShowing && prefs.getBoolean("enabled", false)) {
            showRing();
        }
    }
    
    private void logDeviceInfo() {
        Log.d(TAG, "Device info:");
        Log.d(TAG, "- Model: " + Build.MODEL);
        Log.d(TAG, "- Manufacturer: " + Build.MANUFACTURER);
        Log.d(TAG, "- Android version: " + Build.VERSION.RELEASE);
        Log.d(TAG, "- SDK version: " + Build.VERSION.SDK_INT);
        Log.d(TAG, "- Is Samsung: " + SamsungUtils.isSamsungDevice());
        Log.d(TAG, "- Is S24 Ultra: " + SamsungUtils.isSamsungS24Ultra());
        Log.d(TAG, "- OneUI version: " + SamsungUtils.getOneUIVersion());
        Log.d(TAG, "- Status bar height: " + SamsungUtils.getStatusBarHeight(this));
        
        SamsungUtils.CameraPosition pos = SamsungUtils.getFrontCameraPosition(this);
        Log.d(TAG, "- Camera position: " + pos.x + "," + pos.y + " (size: " + pos.size + ")");
    }
    
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}