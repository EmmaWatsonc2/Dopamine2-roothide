#!/sbin/sh

##########################################################################################
# 自定义安装脚本
##########################################################################################

# 检查架构
if [ "$ARCH" != "arm" ] && [ "$ARCH" != "arm64" ] && [ "$ARCH" != "x86" ] && [ "$ARCH" != "x64" ]; then
  abort "! 不支持的架构: $ARCH"
fi

ui_print "- 正在安装摄像头圆环显示模块..."

# 检查Android版本
if [ "$API" -lt 24 ]; then
  abort "! 需要Android 7.0 (API 24) 或更高版本"
fi

ui_print "- Android版本: $API"

# 检查是否为三星设备
MANUFACTURER=$(getprop ro.product.manufacturer)
MODEL=$(getprop ro.product.model)

ui_print "- 设备制造商: $MANUFACTURER"
ui_print "- 设备型号: $MODEL"

if [ "$MANUFACTURER" = "samsung" ] || [ "$MANUFACTURER" = "Samsung" ]; then
  ui_print "- 检测到三星设备，将使用专用适配"
  
  if echo "$MODEL" | grep -i "s24" > /dev/null; then
    ui_print "- 检测到Galaxy S24系列，使用优化配置"
  fi
else
  ui_print "- 非三星设备，使用通用配置"
fi

# 检查LSPosed
if [ -f "/data/adb/lspd/manager.apk" ]; then
  ui_print "- 检测到LSPosed环境"
  LSPD_AVAILABLE=true
else
  ui_print "- 未检测到LSPosed，建议安装LSPosed以获得最佳体验"
  LSPD_AVAILABLE=false
fi

# 设置权限
ui_print "- 设置文件权限..."
set_perm_recursive $MODPATH 0 0 0755 0644
set_perm $MODPATH/service.sh 0 0 0755

# 复制APK到系统应用目录
if [ -f "$MODPATH/system/app/CameraRingOverlay/CameraRingOverlay.apk" ]; then
  ui_print "- 安装系统应用..."
  set_perm $MODPATH/system/app/CameraRingOverlay/CameraRingOverlay.apk 0 0 0644
fi

ui_print "- 安装完成!"
ui_print ""
ui_print "使用说明:"
ui_print "1. 重启设备"
if [ "$LSPD_AVAILABLE" = true ]; then
  ui_print "2. 在LSPosed中激活'摄像头圆环显示'模块"
  ui_print "3. 重启SystemUI或设备"
fi
ui_print "4. 打开'摄像头圆环显示'应用进行配置"
ui_print "5. 授予悬浮窗权限"
ui_print ""
ui_print "适配设备: 三星Galaxy S24 Ultra OneUI 7.0"