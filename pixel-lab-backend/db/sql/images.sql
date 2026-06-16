-- 【文件路径】db/sql/images.sql
-- 【功能说明】图片资源相关表

-- ============================================
-- 图片表
-- ============================================
CREATE TABLE IF NOT EXISTS `image` (
  `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '图片ID',
  `user_id` INT UNSIGNED NOT NULL COMMENT '用户ID',
  `filename` VARCHAR(255) NOT NULL COMMENT '文件名',
  `original_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
  `url` VARCHAR(500) NOT NULL COMMENT '图片URL',
  `thumbnail_url` VARCHAR(500) DEFAULT NULL COMMENT '缩略图URL',
  `width` INT UNSIGNED DEFAULT NULL COMMENT '图片宽度',
  `height` INT UNSIGNED DEFAULT NULL COMMENT '图片高度',
  `size` INT UNSIGNED DEFAULT NULL COMMENT '文件大小(字节)',
  `format` VARCHAR(10) DEFAULT NULL COMMENT '图片格式(jpg/png等)',
  `is_public` TINYINT DEFAULT 0 COMMENT '是否公开：0-私有，1-公开',
  `status` TINYINT DEFAULT 1 COMMENT '状态：0-删除，1-正常',
  `title` VARCHAR(100) DEFAULT NULL COMMENT '作品标题',
  `description` TEXT DEFAULT NULL COMMENT '作品描述',
  `tags` VARCHAR(255) DEFAULT NULL COMMENT '标签，逗号分隔',
  `view_count` INT UNSIGNED DEFAULT 0 COMMENT '浏览次数',
  `like_count` INT UNSIGNED DEFAULT 0 COMMENT '点赞数',
  `comment_count` INT UNSIGNED DEFAULT 0 COMMENT '评论数',
  `collect_count` INT UNSIGNED DEFAULT 0 COMMENT '收藏数',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_is_public` (`is_public`),
  INDEX `idx_status` (`status`),
  INDEX `idx_tags` (`tags`),
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图片表';

-- ============================================
-- 用户统计表（用于个人中心数据展示）
-- ============================================
CREATE TABLE IF NOT EXISTS `user_stats` (
  `user_id` INT UNSIGNED PRIMARY KEY COMMENT '用户ID',
  `total_images` INT UNSIGNED DEFAULT 0 COMMENT '图片总数',
  `public_images` INT UNSIGNED DEFAULT 0 COMMENT '公开图片数',
  `total_likes` INT UNSIGNED DEFAULT 0 COMMENT '获得点赞数',
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户统计表';
