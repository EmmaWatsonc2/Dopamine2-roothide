package com.cameraringoverlay.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.cameraringoverlay.service.OverlayService;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "CameraRingBootReceiver";
    private static final String PREFS_NAME = "CameraRingPrefs";
    private static final String KEY_ENABLED = "enabled";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Received broadcast: " + action);
        
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
            Intent.ACTION_MY_PACKAGE_REPLACED.equals(action) ||
            Intent.ACTION_PACKAGE_REPLACED.equals(action)) {
            
            // 检查是否启用了圆环显示
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            boolean enabled = prefs.getBoolean(KEY_ENABLED, false);
            
            if (enabled) {
                Log.d(TAG, "Starting overlay service on boot");
                
                // 启动覆盖层服务
                Intent serviceIntent = new Intent(context, OverlayService.class);
                context.startService(serviceIntent);
                
                // 延迟发送显示圆环广播（等待系统完全启动）
                new android.os.Handler().postDelayed(() -> {
                    Intent showIntent = new Intent("com.cameraringoverlay.SHOW_RING");
                    context.sendBroadcast(showIntent);
                    Log.d(TAG, "Sent show ring broadcast");
                }, 5000); // 5秒延迟
            }
        }
    }
}