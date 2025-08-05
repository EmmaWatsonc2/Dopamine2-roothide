package com.cameraringoverlay.lsp;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.cameraringoverlay.lsp.service.CameraRingService;
import com.cameraringoverlay.lsp.utils.DeviceUtils;

public class MainActivity extends AppCompatActivity {
    
    private static final int REQUEST_OVERLAY_PERMISSION = 1000;
    
    private TextView statusText;
    private TextView deviceInfoText;
    private Button permissionButton;
    private Button testButton;
    private Button startServiceButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        updateStatus();
        showDeviceInfo();
    }

    private void initViews() {
        statusText = findViewById(R.id.statusText);
        deviceInfoText = findViewById(R.id.deviceInfoText);
        permissionButton = findViewById(R.id.permissionButton);
        testButton = findViewById(R.id.testButton);
        startServiceButton = findViewById(R.id.startServiceButton);
        
        permissionButton.setOnClickListener(v -> requestOverlayPermission());
        testButton.setOnClickListener(v -> testRing());
        startServiceButton.setOnClickListener(v -> startRingService());
    }

    private void updateStatus() {
        boolean hasPermission = checkOverlayPermission();
        
        if (hasPermission) {
            statusText.setText("✅ 状态：权限已授予，模块可正常工作");
            statusText.setTextColor(getColor(android.R.color.holo_green_dark));
            permissionButton.setText("权限已授予");
            permissionButton.setEnabled(false);
        } else {
            statusText.setText("❌ 状态：需要悬浮窗权限");
            statusText.setTextColor(getColor(android.R.color.holo_red_dark));
            permissionButton.setText("授予悬浮窗权限");
            permissionButton.setEnabled(true);
        }
    }

    private void showDeviceInfo() {
        StringBuilder info = new StringBuilder();
        info.append("设备信息：\n");
        info.append("制造商: ").append(Build.MANUFACTURER).append("\n");
        info.append("型号: ").append(Build.MODEL).append("\n");
        info.append("Android版本: ").append(Build.VERSION.RELEASE).append("\n");
        info.append("SDK版本: ").append(Build.VERSION.SDK_INT).append("\n\n");
        
        info.append("适配检测：\n");
        if (DeviceUtils.isSamsungS24Ultra()) {
            info.append("✅ 三星S24 Ultra - 完美支持\n");
        } else if (DeviceUtils.isSamsungS24()) {
            info.append("✅ 三星S24系列 - 良好支持\n");
        } else if (DeviceUtils.isSamsungDevice()) {
            info.append("🔶 三星设备 - 基础支持\n");
        } else {
            info.append("🔶 通用设备 - 基础支持\n");
        }
        
        if (DeviceUtils.isOneUI7()) {
            info.append("✅ OneUI 7.0 - 已优化\n");
        } else {
            info.append("🔶 其他系统版本\n");
        }
        
        DeviceUtils.CameraPosition pos = DeviceUtils.getCameraPosition(this);
        info.append("\n摄像头位置：\n");
        info.append("X: ").append(pos.x).append("px\n");
        info.append("Y: ").append(pos.y).append("px\n");
        info.append("圆环大小: ").append(pos.size).append("px");
        
        deviceInfoText.setText(info.toString());
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
                .setMessage("为了显示摄像头圆环，需要悬浮窗权限。\n\n请在设置中允许此应用显示在其他应用上层。")
                .setPositiveButton("去设置", (dialog, which) -> {
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
            updateStatus();
            if (checkOverlayPermission()) {
                Toast.makeText(this, "权限已授予！现在可以显示圆环了。", Toast.LENGTH_SHORT).show();
                startRingService();
            } else {
                Toast.makeText(this, "权限被拒绝，无法显示圆环。", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void testRing() {
        if (!checkOverlayPermission()) {
            Toast.makeText(this, "请先授予悬浮窗权限", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 启动服务来测试圆环
        Intent intent = new Intent(this, CameraRingService.class);
        startService(intent);
        
        Toast.makeText(this, "测试圆环已启动", Toast.LENGTH_SHORT).show();
    }

    private void startRingService() {
        if (!checkOverlayPermission()) {
            Toast.makeText(this, "请先授予悬浮窗权限", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Intent intent = new Intent(this, CameraRingService.class);
        startService(intent);
        
        Toast.makeText(this, "圆环服务已启动", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }
}