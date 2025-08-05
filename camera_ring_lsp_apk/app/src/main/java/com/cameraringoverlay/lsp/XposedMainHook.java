package com.cameraringoverlay.lsp;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.cameraringoverlay.lsp.utils.DeviceUtils;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XposedMainHook implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    
    private static final String TAG = "CameraRingLSP";
    private static WindowManager windowManager;
    private static View ringOverlay;
    private static boolean isRingShowing = false;
    private static Context systemContext;
    
    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        Log.d(TAG, "Camera Ring LSP module loaded in Zygote");
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        Log.d(TAG, "Handling package: " + lpparam.packageName);
        
        // Hook SystemUI进程
        if ("com.android.systemui".equals(lpparam.packageName)) {
            hookSystemUI(lpparam);
        }
        
        // Hook三星相机应用
        if (lpparam.packageName.contains("samsung") && lpparam.packageName.contains("camera")) {
            hookSamsungCamera(lpparam);
        }
        
        // Hook自己的应用
        if ("com.cameraringoverlay.lsp".equals(lpparam.packageName)) {
            hookSelfApp(lpparam);
        }
    }
    
    private void hookSystemUI(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Log.d(TAG, "Hooking SystemUI");
            
            // Hook SystemUI Application
            XposedHelpers.findAndHookMethod(Application.class, "onCreate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    systemContext = (Context) param.thisObject;
                    Log.d(TAG, "SystemUI Application created");
                    
                    // 延迟显示圆环
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        try {
                            initializeRingOverlay();
                            showCameraRing();
                        } catch (Exception e) {
                            Log.e(TAG, "Error initializing ring overlay", e);
                        }
                    }, 3000);
                }
            });
            
            // Hook StatusBar类
            try {
                Class<?> statusBarClass = XposedHelpers.findClass("com.android.systemui.statusbar.phone.StatusBar", lpparam.classLoader);
                XposedHelpers.findAndHookMethod(statusBarClass, "start", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Log.d(TAG, "StatusBar started");
                        
                        // 确保圆环显示
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            if (!isRingShowing) {
                                showCameraRing();
                            }
                        }, 2000);
                    }
                });
            } catch (ClassNotFoundException e) {
                Log.w(TAG, "StatusBar class not found, trying alternative");
                tryAlternativeStatusBarHook(lpparam);
            }
            
            // Hook NotificationPanelView（OneUI特殊处理）
            try {
                Class<?> panelViewClass = XposedHelpers.findClass("com.android.systemui.statusbar.phone.NotificationPanelView", lpparam.classLoader);
                XposedHelpers.findAndHookMethod(panelViewClass, "onFinishInflate", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Log.d(TAG, "NotificationPanelView inflated");
                        
                        // OneUI特殊处理
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            ensureRingVisible();
                        }, 1000);
                    }
                });
            } catch (Exception e) {
                Log.w(TAG, "Could not hook NotificationPanelView", e);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error hooking SystemUI", e);
        }
    }
    
    private void tryAlternativeStatusBarHook(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // 尝试Hook StatusBarManager
            Class<?> statusBarManagerClass = XposedHelpers.findClass("com.android.systemui.statusbar.StatusBarManager", lpparam.classLoader);
            XposedHelpers.findAndHookConstructor(statusBarManagerClass, Context.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Log.d(TAG, "StatusBarManager created");
                    
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        showCameraRing();
                    }, 3000);
                }
            });
        } catch (Exception e) {
            Log.w(TAG, "Alternative StatusBar hook failed", e);
        }
    }
    
    private void hookSamsungCamera(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Log.d(TAG, "Hooking Samsung Camera: " + lpparam.packageName);
            
            XposedHelpers.findAndHookMethod(Application.class, "onCreate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Log.d(TAG, "Samsung Camera app started");
                    
                    // 相机启动时强化圆环显示
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        enhanceRingForCamera();
                    }, 1000);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error hooking Samsung Camera", e);
        }
    }
    
    private void hookSelfApp(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Log.d(TAG, "Hooking self app");
            
            XposedHelpers.findAndHookMethod(Application.class, "onCreate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Context appContext = (Context) param.thisObject;
                    Log.d(TAG, "Camera Ring LSP app created");
                    
                    // 启动服务
                    Intent serviceIntent = new Intent(appContext, 
                        XposedHelpers.findClass("com.cameraringoverlay.lsp.service.CameraRingService", lpparam.classLoader));
                    appContext.startService(serviceIntent);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error hooking self app", e);
        }
    }
    
    private static void initializeRingOverlay() {
        if (systemContext == null) {
            Log.w(TAG, "System context not available");
            return;
        }
        
        try {
            windowManager = (WindowManager) systemContext.getSystemService(Context.WINDOW_SERVICE);
            Log.d(TAG, "WindowManager initialized");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing WindowManager", e);
        }
    }
    
    private static void showCameraRing() {
        if (windowManager == null || systemContext == null) {
            Log.w(TAG, "WindowManager or context not available");
            return;
        }
        
        if (isRingShowing) {
            Log.d(TAG, "Ring already showing");
            return;
        }
        
        try {
            // 检查悬浮窗权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(systemContext)) {
                    Log.w(TAG, "No overlay permission");
                    return;
                }
            }
            
            // 创建圆环视图
            createRingView();
            
            // 获取设备信息和位置
            DeviceUtils.CameraPosition cameraPos = DeviceUtils.getCameraPosition(systemContext);
            
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
            
            // 添加到窗口
            windowManager.addView(ringOverlay, params);
            isRingShowing = true;
            
            Log.d(TAG, "Camera ring shown at: " + params.x + "," + params.y + " size: " + cameraPos.size);
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing camera ring", e);
        }
    }
    
    private static void createRingView() {
        ringOverlay = new FrameLayout(systemContext);
        
        // 创建白色圆环
        GradientDrawable ringDrawable = new GradientDrawable();
        ringDrawable.setShape(GradientDrawable.OVAL);
        ringDrawable.setStroke(dpToPx(8), Color.WHITE);
        ringDrawable.setColor(Color.TRANSPARENT);
        ringDrawable.setAlpha(200);
        
        ringOverlay.setBackground(ringDrawable);
        
        Log.d(TAG, "Ring view created");
    }
    
    private static void enhanceRingForCamera() {
        if (!isRingShowing) {
            showCameraRing();
        } else {
            // 增强显示效果
            try {
                if (ringOverlay != null) {
                    GradientDrawable drawable = (GradientDrawable) ringOverlay.getBackground();
                    drawable.setAlpha(255); // 相机使用时更明显
                }
            } catch (Exception e) {
                Log.e(TAG, "Error enhancing ring", e);
            }
        }
    }
    
    private static void ensureRingVisible() {
        if (!isRingShowing) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                showCameraRing();
            }, 500);
        }
    }
    
    private static int dpToPx(int dp) {
        if (systemContext == null) return dp;
        
        float density = systemContext.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}