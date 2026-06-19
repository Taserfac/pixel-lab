#!/bin/bash

# 创建目录结构
mkdir -p illustration photography design ui_design pixel_art ai_art

# 下载插画图片 (Illustration)
echo "正在下载插画图片..."
for i in {1..10}; do
  curl -L "https://picsum.photos/1920/1080?random=$i" -o "illustration/illustration_$i.jpg" 2>/dev/null
  echo "插画 $i/10 完成"
done

# 下载摄影图片 (Photography)
echo "正在下载摄影图片..."
for i in {1..10}; do
  curl -L "https://picsum.photos/1920/1080?random=$((i+10))" -o "photography/photography_$i.jpg" 2>/dev/null
  echo "摄影 $i/10 完成"
done

# 下载设计图片 (Design)
echo "正在下载设计图片..."
for i in {1..10}; do
  curl -L "https://picsum.photos/1920/1080?random=$((i+20))" -o "design/design_$i.jpg" 2>/dev/null
  echo "设计 $i/10 完成"
done

# 下载UI设计图片 (UI Design)
echo "正在下载UI设计图片..."
for i in {1..10}; do
  curl -L "https://picsum.photos/1920/1080?random=$((i+30))" -o "ui_design/ui_design_$i.jpg" 2>/dev/null
  echo "UI设计 $i/10 完成"
done

# 下载像素艺术图片 (Pixel Art)
echo "正在下载像素艺术图片..."
for i in {1..10}; do
  curl -L "https://picsum.photos/1920/1080?random=$((i+40))" -o "pixel_art/pixel_art_$i.jpg" 2>/dev/null
  echo "像素艺术 $i/10 完成"
done

# 下载AI艺术图片 (AI Art)
echo "正在下载AI艺术图片..."
for i in {1..10}; do
  curl -L "https://picsum.photos/1920/1080?random=$((i+50))" -o "ai_art/ai_art_$i.jpg" 2>/dev/null
  echo "AI艺术 $i/10 完成"
done

echo "所有图片下载完成！"
