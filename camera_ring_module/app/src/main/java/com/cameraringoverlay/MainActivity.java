package com.cameraringoverlay;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.cameraringoverlay.service.OverlayService;

public class MainActivity extends AppCompatActivity {
    
    private static final String PREFS_NAME = "CameraRingPrefs";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_RING_SIZE = "ring_size";
    private static final String KEY_RING_WIDTH = "ring_width";
    private static final String KEY_TRANSPARENCY = "transparency";
    
    private static final int REQUEST_OVERLAY_PERMISSION = 1000;
    
    private Switch enableSwitch;
    private SeekBar sizeSeekBar;
    private SeekBar widthSeekBar;
    private SeekBar transparencySeekBar;
    private TextView statusText;
    private Button testButton;
    
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        initViews();
        setupListeners();
        loadSettings();
        updateStatus();
    }

    private void initViews() {
        enableSwitch = findViewById(R.id.enableSwitch);
        sizeSeekBar = findViewById(R.id.sizeSeekBar);
        widthSeekBar = findViewById(R.id.widthSeekBar);
        transparencySeekBar = findViewById(R.id.transparencySeekBar);
        statusText = findViewById(R.id.statusText);
        testButton = findViewById(R.id.testButton);
        
        // 设置SeekBar范围
        sizeSeekBar.setMax(200); // 最大200dp
        sizeSeekBar.setMin(50);  // 最小50dp
        widthSeekBar.setMax(20); // 最大20dp宽度
        widthSeekBar.setMin(2);  // 最小2dp宽度
        transparencySeekBar.setMax(255); // 完全不透明
        transparencySeekBar.setMin(50);  // 最小透明度
    }

    private void setupListeners() {
        enableSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (checkOverlayPermission()) {
                    enableOverlay();
                } else {
                    requestOverlayPermission();
                    enableSwitch.setChecked(false);
                }
            } else {
                disableOverlay();
            }
            saveSettings();
        });
        
        sizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    saveSettings();
                    updateOverlay();
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        widthSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    saveSettings();
                    updateOverlay();
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        transparencySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    saveSettings();
                    updateOverlay();
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        testButton.setOnClickListener(v -> testRing());
    }

    private void loadSettings() {
        enableSwitch.setChecked(prefs.getBoolean(KEY_ENABLED, false));
        sizeSeekBar.setProgress(prefs.getInt(KEY_RING_SIZE, 120));
        widthSeekBar.setProgress(prefs.getInt(KEY_RING_WIDTH, 8));
        transparencySeekBar.setProgress(prefs.getInt(KEY_TRANSPARENCY, 200));
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_ENABLED, enableSwitch.isChecked());
        editor.putInt(KEY_RING_SIZE, sizeSeekBar.getProgress());
        editor.putInt(KEY_RING_WIDTH, widthSeekBar.getProgress());
        editor.putInt(KEY_TRANSPARENCY, transparencySeekBar.getProgress());
        editor.apply();
    }

    private boolean checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(this);
        }
        return true;
    }

    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            new AlertDialog.Builder(this)
                .setTitle("需要悬浮窗权限")
                .setMessage("为了显示摄像头圆环，需要悬浮窗权限。点击确定前往设置页面。")
                .setPositiveButton("确定", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
                })
                .setNegativeButton("取消", null)
                .show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (checkOverlayPermission()) {
                enableSwitch.setChecked(true);
                enableOverlay();
                saveSettings();
            } else {
                Toast.makeText(this, "需要悬浮窗权限才能显示圆环", Toast.LENGTH_SHORT).show();
            }
            updateStatus();
        }
    }

    private void enableOverlay() {
        Intent intent = new Intent(this, OverlayService.class);
        startService(intent);
        
        // 发送显示圆环广播
        Intent showIntent = new Intent("com.cameraringoverlay.SHOW_RING");
        sendBroadcast(showIntent);
        
        updateStatus();
    }

    private void disableOverlay() {
        // 发送隐藏圆环广播
        Intent hideIntent = new Intent("com.cameraringoverlay.HIDE_RING");
        sendBroadcast(hideIntent);
        
        Intent intent = new Intent(this, OverlayService.class);
        stopService(intent);
        
        updateStatus();
    }

    private void updateOverlay() {
        if (enableSwitch.isChecked()) {
            // 重新启动服务以应用新设置
            disableOverlay();
            enableOverlay();
        }
    }

    private void testRing() {
        // 显示测试圆环3秒
        Intent showIntent = new Intent("com.cameraringoverlay.SHOW_RING");
        sendBroadcast(showIntent);
        
        // 3秒后隐藏
        testButton.postDelayed(() -> {
            if (!enableSwitch.isChecked()) {
                Intent hideIntent = new Intent("com.cameraringoverlay.HIDE_RING");
                sendBroadcast(hideIntent);
            }
        }, 3000);
        
        Toast.makeText(this, "测试圆环显示3秒", Toast.LENGTH_SHORT).show();
    }

    private void updateStatus() {
        if (checkOverlayPermission()) {
            if (enableSwitch.isChecked()) {
                statusText.setText("状态：已启用");
                statusText.setTextColor(getColor(android.R.color.holo_green_dark));
            } else {
                statusText.setText("状态：已禁用");
                statusText.setTextColor(getColor(android.R.color.holo_orange_dark));
            }
        } else {
            statusText.setText("状态：需要悬浮窗权限");
            statusText.setTextColor(getColor(android.R.color.holo_red_dark));
        }
    }
}