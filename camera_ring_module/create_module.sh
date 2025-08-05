#!/bin/bash

# 摄像头圆环显示 Magisk模块打包脚本

echo "开始打包摄像头圆环显示Magisk模块..."

# 设置变量
MODULE_NAME="camera_ring_overlay"
MODULE_VERSION="v1.0"
OUTPUT_DIR="output"
ZIP_NAME="${MODULE_NAME}_${MODULE_VERSION}.zip"

# 创建输出目录
mkdir -p "$OUTPUT_DIR"

# 清理之前的构建
rm -rf "$OUTPUT_DIR/*"

echo "正在构建Android应用..."

# 构建APK (需要Android SDK)
if command -v ./gradlew &> /dev/null; then
    echo "使用Gradle构建APK..."
    ./gradlew assembleRelease
    
    if [ -f "app/build/outputs/apk/release/app-release.apk" ]; then
        echo "APK构建成功"
        # 复制APK到系统应用目录
        mkdir -p "system/app/CameraRingOverlay"
        cp "app/build/outputs/apk/release/app-release.apk" "system/app/CameraRingOverlay/CameraRingOverlay.apk"
    else
        echo "警告: APK构建失败，将使用占位符"
        mkdir -p "system/app/CameraRingOverlay"
        echo "# Placeholder APK file" > "system/app/CameraRingOverlay/CameraRingOverlay.apk"
    fi
else
    echo "警告: 未找到Gradle，跳过APK构建"
    mkdir -p "system/app/CameraRingOverlay"
    echo "# Placeholder APK file" > "system/app/CameraRingOverlay/CameraRingOverlay.apk"
fi

echo "正在打包模块文件..."

# 创建模块结构
cp -r META-INF "$OUTPUT_DIR/"
cp module.prop "$OUTPUT_DIR/"
cp service.sh "$OUTPUT_DIR/"
cp customize.sh "$OUTPUT_DIR/"
cp -r system "$OUTPUT_DIR/" 2>/dev/null || true

# 创建ZIP文件
cd "$OUTPUT_DIR"
zip -r "../$ZIP_NAME" ./*
cd ..

echo "模块打包完成: $ZIP_NAME"

# 显示文件信息
if [ -f "$ZIP_NAME" ]; then
    echo "文件大小: $(du -h "$ZIP_NAME" | cut -f1)"
    echo "MD5: $(md5sum "$ZIP_NAME" | cut -d' ' -f1)"
    
    echo ""
    echo "安装说明:"
    echo "1. 将 $ZIP_NAME 传输到设备"
    echo "2. 在Magisk中选择'模块'->'从本地安装'"
    echo "3. 选择 $ZIP_NAME 文件"
    echo "4. 重启设备"
    echo "5. 在LSPosed中激活模块"
    echo "6. 打开'摄像头圆环显示'应用进行配置"
else
    echo "错误: 模块打包失败"
    exit 1
fi

echo "打包完成!"