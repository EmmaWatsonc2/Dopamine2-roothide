package com.cameraringoverlay;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.cameraringoverlay.service.OverlayService;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XposedModule implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    
    private static final String TAG = "CameraRingOverlay";
    
    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        Log.d(TAG, "Camera Ring Overlay module loaded in Zygote");
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // Hook 系统UI进程来显示摄像头圆环
        if ("com.android.systemui".equals(lpparam.packageName)) {
            hookSystemUI(lpparam);
        }
        
        // Hook 三星相机应用
        if (lpparam.packageName.contains("samsung") && lpparam.packageName.contains("camera")) {
            hookSamsungCamera(lpparam);
        }
        
        // Hook 系统进程
        if ("android".equals(lpparam.packageName)) {
            hookSystemProcess(lpparam);
        }
    }
    
    private void hookSystemUI(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook SystemUI的Application类来启动覆盖层服务
            XposedHelpers.findAndHookMethod(Application.class, "onCreate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Context context = (Context) param.thisObject;
                    Log.d(TAG, "SystemUI Application created, starting overlay service");
                    
                    // 启动覆盖层服务
                    Intent intent = new Intent();
                    intent.setClassName("com.cameraringoverlay", "com.cameraringoverlay.service.OverlayService");
                    try {
                        context.startService(intent);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to start overlay service", e);
                    }
                }
            });
            
            // Hook StatusBar相关类来确保圆环显示在状态栏之上
            try {
                Class<?> statusBarClass = XposedHelpers.findClass("com.android.systemui.statusbar.phone.StatusBar", lpparam.classLoader);
                XposedHelpers.findAndHookMethod(statusBarClass, "start", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Log.d(TAG, "StatusBar started, ensuring camera ring overlay is visible");
                        // 这里可以添加确保覆盖层显示的逻辑
                    }
                });
            } catch (Exception e) {
                Log.w(TAG, "Could not hook StatusBar class", e);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error hooking SystemUI", e);
        }
    }
    
    private void hookSamsungCamera(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook 相机应用来检测前置摄像头使用
            XposedHelpers.findAndHookMethod(Application.class, "onCreate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Context context = (Context) param.thisObject;
                    Log.d(TAG, "Samsung Camera app started");
                    
                    // 当相机应用启动时，确保圆环可见
                    Intent intent = new Intent("com.cameraringoverlay.SHOW_RING");
                    context.sendBroadcast(intent);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error hooking Samsung Camera", e);
        }
    }
    
    private void hookSystemProcess(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook 系统进程以监听应用启动
            Log.d(TAG, "Hooking system process for camera ring overlay");
        } catch (Exception e) {
            Log.e(TAG, "Error hooking system process", e);
        }
    }
}