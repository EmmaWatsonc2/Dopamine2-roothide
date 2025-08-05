# 📱 摄像头圆环LSP模块

专为三星Galaxy S24 Ultra OneUI 7.0设计的LSPosed模块，可在前置摄像头周围显示白色圆环效果。

## 🎯 专为LSP框架设计

这是一个纯LSPosed模块APK，**不需要Magisk**，直接通过LSP框架工作。

## ✨ 功能特性

- 🎯 **直接Hook SystemUI** - 在系统UI进程中直接显示圆环
- 🔄 **自动启动** - 系统启动后自动激活
- 📱 **设备适配** - 专门适配三星S24 Ultra OneUI 7.0
- ⚙️ **权限管理** - 自动请求和管理悬浮窗权限
- 🛠️ **调试友好** - 详细的日志输出便于调试

## 📋 系统要求

- **设备**: 三星Galaxy S24 Ultra (推荐)
- **系统**: Android 7.0+ 
- **框架**: LSPosed 必需
- **权限**: 悬浮窗权限

## 🚀 安装步骤

### 1. 安装APK
```bash
# 下载并安装APK到设备
adb install camera_ring_lsp_v1.0.apk
```

### 2. LSPosed激活
1. 打开LSPosed应用
2. 进入"模块"页面
3. 激活"摄像头圆环LSP"模块
4. 选择作用域：`SystemUI (com.android.systemui)`
5. 重启SystemUI: `adb shell killall com.android.systemui`

### 3. 权限配置
1. 打开"摄像头圆环LSP"应用
2. 点击"授予悬浮窗权限"
3. 在设置中允许"显示在其他应用上层"
4. 返回应用，点击"启动圆环服务"

## 🔧 工作原理

### LSPosed Hook机制
```java
// Hook SystemUI Application
XposedHelpers.findAndHookMethod(Application.class, "onCreate", new XC_MethodHook() {
    @Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        // 在SystemUI启动时创建圆环覆盖层
        initializeRingOverlay();
        showCameraRing();
    }
});
```

### 核心组件
1. **XposedMainHook** - 主Hook类，拦截SystemUI启动
2. **CameraRingService** - 独立覆盖层服务
3. **DeviceUtils** - 设备适配工具
4. **BootCompletedReceiver** - 开机自启动

### 显示逻辑
```java
// 创建白色圆环
GradientDrawable ringDrawable = new GradientDrawable();
ringDrawable.setShape(GradientDrawable.OVAL);
ringDrawable.setStroke(dpToPx(8), Color.WHITE);
ringDrawable.setColor(Color.TRANSPARENT);
ringDrawable.setAlpha(200);

// 获取摄像头位置
DeviceUtils.CameraPosition cameraPos = DeviceUtils.getCameraPosition(context);

// 显示在窗口管理器中
windowManager.addView(ringOverlay, params);
```

## 📱 设备适配

### 三星S24 Ultra优化
- 前置摄像头位置: 屏幕顶部居中，60dp偏移
- 圆环大小: 120dp
- 状态栏高度自动调整
- OneUI 7.0界面兼容

### 其他设备支持
- 三星S24系列: 55dp偏移，110dp圆环
- 其他三星设备: 50dp偏移，100dp圆环  
- 通用Android设备: 45dp偏移，90dp圆环

## 🔍 调试日志

查看模块工作状态：
```bash
# 查看LSP Hook日志
adb logcat | grep CameraRingLSP

# 查看服务日志
adb logcat | grep CameraRingService

# 查看设备适配日志
adb logcat | grep DeviceUtils
```

## ⚠️ 故障排除

### 圆环不显示
1. **检查LSP激活**: 确认模块已在LSPosed中激活
2. **检查作用域**: 必须选择SystemUI作为作用域
3. **重启SystemUI**: `adb shell killall com.android.systemui`
4. **检查权限**: 确认已授予悬浮窗权限
5. **查看日志**: 检查logcat输出的错误信息

### Hook失败
```bash
# 检查SystemUI进程
adb shell ps | grep systemui

# 检查LSPosed状态
adb shell cat /data/data/org.lsposed.manager/conf/modules.list
```

### 位置不准确
- 在应用中查看设备检测信息
- 检查状态栏高度计算
- 考虑不同分辨率的适配

## 🔄 与Magisk模块的区别

| 特性 | LSP模块 | Magisk模块 |
|------|---------|------------|
| 依赖 | 仅需LSPosed | 需要Magisk+LSP |
| 安装 | 直接安装APK | 刷入ZIP文件 |
| 权限 | 应用级权限 | 系统级权限 |
| 调试 | 更容易调试 | 系统级调试 |
| 卸载 | 直接卸载APK | 在Magisk中移除 |

## 📁 项目结构

```
camera_ring_lsp_apk/
├── app/src/main/
│   ├── java/com/cameraringoverlay/lsp/
│   │   ├── XposedMainHook.java      # 主Hook类
│   │   ├── MainActivity.java        # 配置界面
│   │   ├── service/
│   │   │   └── CameraRingService.java   # 覆盖层服务
│   │   ├── utils/
│   │   │   └── DeviceUtils.java     # 设备适配
│   │   └── receiver/
│   │       └── BootCompletedReceiver.java  # 开机启动
│   ├── res/                         # 资源文件
│   ├── assets/
│   │   └── xposed_init             # LSP入口声明
│   └── AndroidManifest.xml        # 应用清单
└── build_apk.sh                   # 构建脚本
```

## 📄 许可证

MIT License - 仅供学习和个人使用

## 🤝 支持

如果遇到问题：
1. 查看应用内的设备信息
2. 检查logcat日志输出
3. 确认LSPosed配置正确
4. 重启SystemUI重新Hook

---

**注意**: 此模块专为LSPosed框架设计，效果比Magisk模块更稳定可靠！