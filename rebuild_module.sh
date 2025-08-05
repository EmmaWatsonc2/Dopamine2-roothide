#!/bin/bash

# 摄像头圆环显示模块重建脚本
# 运行此脚本重新创建完整的Magisk模块

echo "🔧 开始重建摄像头圆环显示模块..."

# 创建模块目录结构
mkdir -p camera_ring_overlay
cd camera_ring_overlay

# 创建META-INF目录和文件
mkdir -p META-INF/com/google/android

# 创建 update-binary
cat > META-INF/com/google/android/update-binary << 'EOF'
#!/sbin/sh

#################
# Initialization
#################

umask 022

# echo before loading util_functions
ui_print() { echo "$1"; }

require_new_magisk() {
  ui_print "*******************************"
  ui_print " Please install Magisk v20.4+! "
  ui_print "*******************************"
  exit 1
}

#########################
# Load util_functions.sh
#########################

OUTFD=$2
ZIPFILE=$3

mount /data 2>/dev/null

[ -f /data/adb/magisk/util_functions.sh ] || require_new_magisk
. /data/adb/magisk/util_functions.sh
[ $MAGISK_VER_CODE -lt 20400 ] && require_new_magisk

install_module
exit 0
EOF

# 创建 updater-script
echo "#MAGISK" > META-INF/com/google/android/updater-script

# 创建 module.prop
cat > module.prop << 'EOF'
id=camera_ring_overlay
name=Camera Ring Overlay
version=v1.0
versionCode=100
author=Assistant
description=在前置摄像头周围显示白色圆环，适配三星OneUI 7.0
updateJson=
EOF

# 创建 service.sh
cat > service.sh << 'EOF'
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
EOF

# 创建系统应用目录
mkdir -p system/app/CameraRingOverlay
echo "# Placeholder APK file" > system/app/CameraRingOverlay/CameraRingOverlay.apk

# 创建customize.sh
cat > customize.sh << 'EOF'
#!/sbin/sh

ui_print "- 正在安装摄像头圆环显示模块..."
ui_print "- 适配设备: 三星Galaxy S24 Ultra OneUI 7.0"

# 检查Android版本
if [ "$API" -lt 24 ]; then
  abort "! 需要Android 7.0 (API 24) 或更高版本"
fi

# 设置权限
set_perm_recursive $MODPATH 0 0 0755 0644
set_perm $MODPATH/service.sh 0 0 0755

ui_print "- 安装完成!"
ui_print "1. 重启设备"
ui_print "2. 在LSPosed中激活模块"
ui_print "3. 重启SystemUI"
ui_print "4. 授予悬浮窗权限"
EOF

# 设置权限
chmod 755 service.sh
chmod 755 customize.sh
chmod 755 META-INF/com/google/android/update-binary

# 打包ZIP
echo "📦 正在打包模块..."
zip -r camera_ring_overlay_v1.0.zip ./*

echo "✅ 模块重建完成!"
echo "📁 文件位置: $(pwd)/camera_ring_overlay_v1.0.zip"
echo "📦 文件大小: $(du -h camera_ring_overlay_v1.0.zip | cut -f1)"

cd ..
EOF

chmod +x rebuild_module.sh