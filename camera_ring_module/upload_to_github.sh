#!/bin/bash

# GitHub上传脚本
# 使用方法: ./upload_to_github.sh <your-github-repo-url>

if [ $# -eq 0 ]; then
    echo "❌ 请提供GitHub仓库URL"
    echo "使用方法: ./upload_to_github.sh https://github.com/yourusername/your-repo.git"
    exit 1
fi

REPO_URL=$1

echo "🚀 开始上传到GitHub仓库: $REPO_URL"

# 添加远程仓库
echo "📡 添加远程仓库..."
git remote add origin $REPO_URL

# 推送到GitHub
echo "⬆️ 推送代码到GitHub..."
git push -u origin master

# 创建并推送标签
echo "🏷️ 创建版本标签..."
git tag v1.0 -m "Initial release v1.0"
git push origin v1.0

echo "✅ 上传完成！"
echo ""
echo "🌐 你的仓库地址: $REPO_URL"
echo "📦 下载链接: ${REPO_URL%%.git}/releases"
echo ""
echo "📋 下一步操作:"
echo "1. 在GitHub仓库页面检查文件"
echo "2. 创建Release发布版本"
echo "3. 上传编译好的ZIP文件到Release"