package com.cameraringoverlay.lsp.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.cameraringoverlay.lsp.utils.DeviceUtils;

public class CameraRingService extends Service {
    private static final String TAG = "CameraRingService";
    
    private WindowManager windowManager;
    private View ringOverlay;
    private boolean isShowing = false;
    
    private BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_SCREEN_ON.equals(action)) {
                Log.d(TAG, "Screen turned on, showing ring");
                showRing();
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                Log.d(TAG, "Screen turned off, hiding ring");
                hideRing();
            } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
                Log.d(TAG, "User present, ensuring ring visible");
                showRing();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "CameraRingService created");
        
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        // 注册屏幕状态监听
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(screenReceiver, filter);
        
        // 延迟显示圆环
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (hasOverlayPermission()) {
                showRing();
            }
        }, 2000);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "CameraRingService started");
        
        if (!isShowing && hasOverlayPermission()) {
            showRing();
        }
        
        return START_STICKY; // 服务被杀死后自动重启
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "CameraRingService destroyed");
        
        hideRing();
        
        try {
            unregisterReceiver(screenReceiver);
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

    private void showRing() {
        if (isShowing || windowManager == null) {
            return;
        }

        try {
            if (!hasOverlayPermission()) {
                Log.w(TAG, "No overlay permission");
                return;
            }
            
            createRingView();
            
            // 获取摄像头位置
            DeviceUtils.CameraPosition cameraPos = DeviceUtils.getCameraPosition(this);
            
            // 设置窗口参数
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                cameraPos.size,
                cameraPos.size,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O 
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
                    : WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT
            );
            
            params.gravity = Gravity.TOP | Gravity.LEFT;
            params.x = cameraPos.x - (cameraPos.size / 2);
            params.y = cameraPos.y - (cameraPos.size / 2);
            
            // 添加状态栏高度偏移
            params.y += DeviceUtils.getStatusBarHeight(this);
            
            windowManager.addView(ringOverlay, params);
            isShowing = true;
            
            Log.d(TAG, "Camera ring shown at: " + params.x + "," + params.y + " size: " + cameraPos.size);
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing ring", e);
        }
    }

    private void hideRing() {
        if (!isShowing || ringOverlay == null) {
            return;
        }

        try {
            windowManager.removeView(ringOverlay);
            isShowing = false;
            Log.d(TAG, "Camera ring hidden");
        } catch (Exception e) {
            Log.e(TAG, "Error hiding ring", e);
        }
    }

    private void createRingView() {
        if (ringOverlay != null) {
            try {
                windowManager.removeView(ringOverlay);
            } catch (Exception e) {
                // Ignore
            }
        }

        ringOverlay = new FrameLayout(this);
        
        // 创建白色圆环
        GradientDrawable ringDrawable = new GradientDrawable();
        ringDrawable.setShape(GradientDrawable.OVAL);
        ringDrawable.setStroke(dpToPx(8), Color.WHITE);
        ringDrawable.setColor(Color.TRANSPARENT);
        ringDrawable.setAlpha(200);
        
        ringOverlay.setBackground(ringDrawable);
        
        Log.d(TAG, "Ring view created");
    }
    
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}