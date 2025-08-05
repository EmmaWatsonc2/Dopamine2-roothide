package com.cameraringoverlay.lsp.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.cameraringoverlay.lsp.service.CameraRingService;

public class BootCompletedReceiver extends BroadcastReceiver {
    private static final String TAG = "BootCompletedReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Received broadcast: " + action);
        
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
            Intent.ACTION_USER_PRESENT.equals(action)) {
            
            Log.d(TAG, "Boot completed or user present, starting camera ring service");
            
            // 延迟启动服务，等待系统完全加载
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    Intent serviceIntent = new Intent(context, CameraRingService.class);
                    context.startService(serviceIntent);
                    Log.d(TAG, "Camera ring service started");
                } catch (Exception e) {
                    Log.e(TAG, "Error starting camera ring service", e);
                }
            }, 10000); // 10秒延迟
        }
    }
}