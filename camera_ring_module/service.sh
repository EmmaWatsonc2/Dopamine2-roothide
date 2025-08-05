#!/system/bin/sh

# Camera Ring Overlay Service Script
# 等待系统启动完成
sleep 30

# 检查LSPosed是否可用
if [ -f "/data/adb/lspd/manager.apk" ]; then
    # LSPosed环境
    echo "LSPosed detected, module will activate through Xposed framework"
else
    # fallback: 直接启动overlay服务
    echo "Starting camera ring overlay service"
    /system/bin/app_process32 /system/bin com.cameraringoverlay.service.OverlayService &
fi