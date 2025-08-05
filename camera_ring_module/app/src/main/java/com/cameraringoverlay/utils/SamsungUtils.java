package com.cameraringoverlay.utils;

import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

public class SamsungUtils {
    private static final String TAG = "SamsungUtils";
    
    // 三星Galaxy S24 Ultra 前置摄像头位置
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
    
    /**
     * 获取前置摄像头在屏幕上的位置
     */
    public static CameraPosition getFrontCameraPosition(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;
        float density = metrics.density;
        
        Log.d(TAG, "Screen: " + screenWidth + "x" + screenHeight + ", density: " + density);
        
        // 根据设备型号和OneUI版本调整位置
        if (isSamsungS24Ultra()) {
            return getS24UltraCameraPosition(screenWidth, screenHeight, density);
        } else if (isSamsungS24()) {
            return getS24CameraPosition(screenWidth, screenHeight, density);
        } else if (isSamsungDevice()) {
            return getSamsungGenericPosition(screenWidth, screenHeight, density);
        } else {
            // 通用位置
            return getGenericCameraPosition(screenWidth, screenHeight, density);
        }
    }
    
    private static CameraPosition getS24UltraCameraPosition(int screenWidth, int screenHeight, float density) {
        // 三星S24 Ultra 前置摄像头位置（基于OneUI 7.0）
        int x = screenWidth / 2; // 居中
        int y = (int) (55 * density); // 从顶部55dp
        int size = (int) (120 * density); // 圆环大小120dp
        
        Log.d(TAG, "S24 Ultra camera position: " + x + "," + y + ", size: " + size);
        return new CameraPosition(x, y, size);
    }
    
    private static CameraPosition getS24CameraPosition(int screenWidth, int screenHeight, float density) {
        // 三星S24 前置摄像头位置
        int x = screenWidth / 2;
        int y = (int) (50 * density);
        int size = (int) (110 * density);
        
        return new CameraPosition(x, y, size);
    }
    
    private static CameraPosition getSamsungGenericPosition(int screenWidth, int screenHeight, float density) {
        // 通用三星设备位置
        int x = screenWidth / 2;
        int y = (int) (45 * density);
        int size = (int) (100 * density);
        
        return new CameraPosition(x, y, size);
    }
    
    private static CameraPosition getGenericCameraPosition(int screenWidth, int screenHeight, float density) {
        // 通用Android设备位置
        int x = screenWidth / 2;
        int y = (int) (40 * density);
        int size = (int) (90 * density);
        
        return new CameraPosition(x, y, size);
    }
    
    /**
     * 检查是否为三星S24 Ultra
     */
    public static boolean isSamsungS24Ultra() {
        String model = Build.MODEL.toLowerCase();
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        
        return manufacturer.contains("samsung") && 
               (model.contains("s24 ultra") || 
                model.contains("sm-s928") ||
                model.contains("s928"));
    }
    
    /**
     * 检查是否为三星S24系列
     */
    public static boolean isSamsungS24() {
        String model = Build.MODEL.toLowerCase();
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        
        return manufacturer.contains("samsung") && 
               (model.contains("s24") || 
                model.contains("sm-s92"));
    }
    
    /**
     * 检查是否为三星设备
     */
    public static boolean isSamsungDevice() {
        return Build.MANUFACTURER.toLowerCase().contains("samsung");
    }
    
    /**
     * 检查OneUI版本
     */
    public static String getOneUIVersion() {
        try {
            String version = Build.VERSION.INCREMENTAL;
            if (version.contains("7.0")) {
                return "7.0";
            } else if (version.contains("6.1")) {
                return "6.1";
            } else if (version.contains("6.0")) {
                return "6.0";
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting OneUI version", e);
        }
        return "unknown";
    }
    
    /**
     * 获取状态栏高度
     */
    public static int getStatusBarHeight(Context context) {
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return context.getResources().getDimensionPixelSize(resourceId);
        }
        return (int) (24 * context.getResources().getDisplayMetrics().density); // 默认24dp
    }
    
    /**
     * 检查是否为OneUI 7.0
     */
    public static boolean isOneUI7() {
        return "7.0".equals(getOneUIVersion()) || Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE;
    }
    
    /**
     * 获取导航栏高度
     */
    public static int getNavigationBarHeight(Context context) {
        int resourceId = context.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return context.getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }
}