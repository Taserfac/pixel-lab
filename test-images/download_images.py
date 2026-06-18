#!/usr/bin/env python3
"""
从免费图片源下载测试图片
支持类型：插画、摄影、设计、UI设计、像素艺术、AI艺术
"""

import os
import requests
import time
from pathlib import Path

# 图片类型配置
IMAGE_TYPES = {
    'illustration': {
        'name_cn': '插画',
        'keywords': ['illustration', 'art', 'drawing', 'painting', 'digital art'],
        'count': 10
    },
    'photography': {
        'name_cn': '摄影',
        'keywords': ['photography', 'photo', 'landscape', 'portrait', 'nature'],
        'count': 10
    },
    'design': {
        'name_cn': '设计',
        'keywords': ['design', 'graphic', 'creative', 'abstract', 'modern'],
        'count': 10
    },
    'ui_design': {
        'name_cn': 'UI设计',
        'keywords': ['ui', 'ux', 'interface', 'web design', 'app design'],
        'count': 10
    },
    'pixel_art': {
        'name_cn': '像素艺术',
        'keywords': ['pixel', '8bit', 'retro', 'game', 'pixel art'],
        'count': 10
    },
    'ai_art': {
        'name_cn': 'AI艺术',
        'keywords': ['ai art', 'artificial intelligence', 'generated', 'digital', 'futuristic'],
        'count': 10
    }
}

# 图片源配置
IMAGE_SOURCES = [
    {
        'name': 'Lorem Picsum',
        'url_template': 'https://picsum.photos/1920/1080?random={seed}',
        'headers': {}
    },
    {
        'name': 'Placeholder.com',
        'url_template': 'https://via.placeholder.com/1920x1080/{color}/ffffff?text={text}',
        'headers': {}
    }
]

def create_directories():
    """创建图片目录结构"""
    for img_type in IMAGE_TYPES.keys():
        Path(img_type).mkdir(exist_ok=True)
    print("✓ 目录结构创建完成")

def download_image(url, filepath, headers=None):
    """下载单张图片"""
    try:
        response = requests.get(url, headers=headers or {}, timeout=30, stream=True)
        response.raise_for_status()

        with open(filepath, 'wb') as f:
            for chunk in response.iter_content(chunk_size=8192):
                f.write(chunk)

        return True
    except Exception as e:
        print(f"  ✗ 下载失败: {e}")
        return False

def download_from_picsum(img_type, index):
    """从 Lorem Picsum 下载图片"""
    # 使用不同的随机种子确保每张图片不同
    seed = hash(f"{img_type}_{index}") % 10000
    url = f"https://picsum.photos/1920/1080?random={seed}"

    filepath = f"{img_type}/{img_type}_{index:02d}.jpg"
    return download_image(url, filepath)

def download_with_keyword_placeholder(img_type, index, keyword):
    """使用带关键词的占位图"""
    # 为不同类型使用不同颜色
    colors = {
        'illustration': 'FF6B6B',
        'photography': '4ECDC4',
        'design': '45B7D1',
        'ui_design': '96CEB4',
        'pixel_art': 'FFEAA7',
        'ai_art': 'DDA0DD'
    }

    color = colors.get(img_type, 'CCCCCC')
    text = f"{img_type}_{index}"
    url = f"https://via.placeholder.com/1920x1080/{color}/ffffff?text={text}"

    filepath = f"{img_type}/{img_type}_{index:02d}.png"
    return download_image(url, filepath)

def download_images():
    """下载所有图片"""
    print("开始下载图片...")
    print("=" * 50)

    total_downloaded = 0
    total_failed = 0

    for img_type, config in IMAGE_TYPES.items():
        print(f"\n📁 正在下载 {config['name_cn']} ({img_type})...")
        print("-" * 30)

        type_downloaded = 0
        type_failed = 0

        for i in range(1, config['count'] + 1):
            print(f"  下载 {i}/{config['count']}...", end=" ")

            # 尝试从 Lorem Picsum 下载
            success = download_from_picsum(img_type, i)

            if success:
                print("✓")
                type_downloaded += 1
            else:
                # 如果失败，使用占位图
                print("尝试备用源...", end=" ")
                keyword = config['keywords'][i % len(config['keywords'])]
                success = download_with_keyword_placeholder(img_type, i, keyword)

                if success:
                    print("✓ (备用)")
                    type_downloaded += 1
                else:
                    print("✗")
                    type_failed += 1

            # 添加延迟避免请求过快
            time.sleep(0.5)

        total_downloaded += type_downloaded
        total_failed += type_failed

        print(f"  {config['name_cn']}完成: {type_downloaded}张成功, {type_failed}张失败")

    print("\n" + "=" * 50)
    print(f"下载完成！总计: {total_downloaded}张成功, {total_failed}张失败")

    return total_failed == 0

def verify_downloads():
    """验证下载的图片"""
    print("\n验证下载的图片...")
    print("-" * 30)

    all_valid = True

    for img_type, config in IMAGE_TYPES.items():
        print(f"\n{config['name_cn']} ({img_type}):")

        for i in range(1, config['count'] + 1):
            filepath = f"{img_type}/{img_type}_{i:02d}"
            jpg_path = f"{filepath}.jpg"
            png_path = f"{filepath}.png"

            if os.path.exists(jpg_path):
                size = os.path.getsize(jpg_path)
                print(f"  ✓ {img_type}_{i:02d}.jpg ({size:,} bytes)")
            elif os.path.exists(png_path):
                size = os.path.getsize(png_path)
                print(f"  ✓ {img_type}_{i:02d}.png ({size:,} bytes)")
            else:
                print(f"  ✗ {img_type}_{i:02d} 缺失")
                all_valid = False

    return all_valid

def main():
    """主函数"""
    print("🖼️  图片下载工具")
    print("=" * 50)
    print("类型: 插画、摄影、设计、UI设计、像素艺术、AI艺术")
    print("每种: 10张图片")
    print("分辨率: 1920x1080")
    print()

    # 创建目录
    create_directories()

    # 下载图片
    success = download_images()

    # 验证下载
    if success:
        verify_downloads()
        print("\n✅ 所有图片下载成功！")
    else:
        print("\n⚠️  部分图片下载失败，请检查网络连接后重试")

    return 0 if success else 1

if __name__ == '__main__':
    exit(main())
