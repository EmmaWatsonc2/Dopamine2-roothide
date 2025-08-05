package com.cameraringoverlay.lsp.utils;

import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

public class DeviceUtils {
    private static final String TAG = "DeviceUtils";
    
    public static class CameraPosition {
        public int x;
        public int y;
        public int size;
        
        public CameraPosition(int x, int y, int size) {
            this.x = x;
            this.y = y;
            this.size = size;
        }
    }
    
    public static CameraPosition getCameraPosition(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;
        float density = metrics.density;
        
        Log.d(TAG, "Screen: " + screenWidth + "x" + screenHeight + ", density: " + density);
        Log.d(TAG, "Device: " + Build.MANUFACTURER + " " + Build.MODEL);
        
        // 根据设备型号适配
        if (isSamsungS24Ultra()) {
            return getS24UltraCameraPosition(screenWidth, screenHeight, density);
        } else if (isSamsungS24()) {
            return getS24CameraPosition(screenWidth, screenHeight, density);
        } else if (isSamsungDevice()) {
            return getSamsungGenericPosition(screenWidth, screenHeight, density);
        } else {
            return getGenericCameraPosition(screenWidth, screenHeight, density);
        }
    }
    
    private static CameraPosition getS24UltraCameraPosition(int screenWidth, int screenHeight, float density) {
        // S24 Ultra 前置摄像头位置（OneUI 7.0优化）
        int x = screenWidth / 2;
        int y = (int) (60 * density); // 考虑状态栏和刘海
        int size = (int) (120 * density);
        
        Log.d(TAG, "S24 Ultra position: " + x + "," + y + ", size: " + size);
        return new CameraPosition(x, y, size);
    }
    
    private static CameraPosition getS24CameraPosition(int screenWidth, int screenHeight, float density) {
        int x = screenWidth / 2;
        int y = (int) (55 * density);
        int size = (int) (110 * density);
        
        return new CameraPosition(x, y, size);
    }
    
    private static CameraPosition getSamsungGenericPosition(int screenWidth, int screenHeight, float density) {
        int x = screenWidth / 2;
        int y = (int) (50 * density);
        int size = (int) (100 * density);
        
        return new CameraPosition(x, y, size);
    }
    
    private static CameraPosition getGenericCameraPosition(int screenWidth, int screenHeight, float density) {
        int x = screenWidth / 2;
        int y = (int) (45 * density);
        int size = (int) (90 * density);
        
        return new CameraPosition(x, y, size);
    }
    
    public static boolean isSamsungS24Ultra() {
        String model = Build.MODEL.toLowerCase();
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        
        return manufacturer.contains("samsung") && 
               (model.contains("s24 ultra") || 
                model.contains("sm-s928") ||
                model.contains("s928"));
    }
    
    public static boolean isSamsungS24() {
        String model = Build.MODEL.toLowerCase();
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        
        return manufacturer.contains("samsung") && 
               (model.contains("s24") || 
                model.contains("sm-s92"));
    }
    
    public static boolean isSamsungDevice() {
        return Build.MANUFACTURER.toLowerCase().contains("samsung");
    }
    
    public static int getStatusBarHeight(Context context) {
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return context.getResources().getDimensionPixelSize(resourceId);
        }
        return (int) (24 * context.getResources().getDisplayMetrics().density);
    }
    
    public static boolean isOneUI7() {
        // 检测OneUI 7.0
        String version = Build.VERSION.INCREMENTAL;
        return version.contains("7.0") || Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE;
    }
}