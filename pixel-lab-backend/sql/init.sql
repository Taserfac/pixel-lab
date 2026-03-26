-- ============================================
-- Pixel Lab Pro 数据库初始化脚本
-- ============================================
-- 使用方法：mysql -u root -p < init.sql
-- 或在 MySQL 客户端中执行：source init.sql
-- ============================================

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS `pixel_lab` 
DEFAULT CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE `pixel_lab`;

-- ============================================
-- 1. 用户表
-- ============================================
CREATE TABLE IF NOT EXISTS `user` (
  `id` INT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码（bcrypt加密）',
  `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `role` ENUM('user', 'admin') NOT NULL DEFAULT 'user' COMMENT '角色：普通用户/管理员',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1=正常, 0=禁用',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除：0=正常, 1=已删除',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY `uk_username` (`username`),
  INDEX `idx_status` (`status`),
  INDEX `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================
-- 2. 图片表
-- ============================================
CREATE TABLE IF NOT EXISTS `image` (
  `id` INT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '图片ID',
  `user_id` INT UNSIGNED NOT NULL COMMENT '所属用户ID',
  `filename` VARCHAR(100) NOT NULL COMMENT '服务器文件名',
  `original_name` VARCHAR(255) DEFAULT NULL COMMENT '原始文件名',
  `url` VARCHAR(500) NOT NULL COMMENT '图片访问URL',
  `thumbnail_url` VARCHAR(500) DEFAULT NULL COMMENT '缩略图URL',
  `width` INT UNSIGNED DEFAULT NULL COMMENT '图片宽度',
  `height` INT UNSIGNED DEFAULT NULL COMMENT '图片高度',
  `size` INT UNSIGNED DEFAULT NULL COMMENT '文件大小（字节）',
  `format` VARCHAR(20) DEFAULT NULL COMMENT '图片格式（jpg/png/gif/webp）',
  `is_public` TINYINT NOT NULL DEFAULT 0 COMMENT '是否公开：0=私有, 1=公开',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1=正常, 0=已删除',
  
  -- 社区相关字段
  `title` VARCHAR(100) DEFAULT NULL COMMENT '作品标题',
  `description` TEXT DEFAULT NULL COMMENT '作品描述',
  `tags` VARCHAR(255) DEFAULT NULL COMMENT '标签（逗号分隔）',
  `view_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '浏览次数',
  `like_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '点赞数',
  `comment_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '评论数',
  `collect_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '收藏数',
  
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_is_public` (`is_public`),
  INDEX `idx_created_at` (`created_at`),
  INDEX `idx_like_count` (`like_count`),
  INDEX `idx_view_count` (`view_count`),
  CONSTRAINT `fk_image_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图片表';

-- ============================================
-- 3. 点赞表
-- ============================================
CREATE TABLE IF NOT EXISTS `likes` (
  `id` INT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '点赞ID',
  `user_id` INT UNSIGNED NOT NULL COMMENT '点赞用户ID',
  `image_id` INT UNSIGNED NOT NULL COMMENT '图片ID',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  
  UNIQUE KEY `uk_user_image` (`user_id`, `image_id`),
  INDEX `idx_image_id` (`image_id`),
  INDEX `idx_user_id` (`user_id`),
  CONSTRAINT `fk_likes_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_likes_image` FOREIGN KEY (`image_id`) REFERENCES `image` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='点赞表';

-- ============================================
-- 4. 收藏表
-- ============================================
CREATE TABLE IF NOT EXISTS `collections` (
  `id` INT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '收藏ID',
  `user_id` INT UNSIGNED NOT NULL COMMENT '收藏用户ID',
  `image_id` INT UNSIGNED NOT NULL COMMENT '图片ID',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  
  UNIQUE KEY `uk_user_image` (`user_id`, `image_id`),
  INDEX `idx_image_id` (`image_id`),
  INDEX `idx_user_id` (`user_id`),
  CONSTRAINT `fk_collections_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_collections_image` FOREIGN KEY (`image_id`) REFERENCES `image` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';

-- ============================================
-- 5. 评论表
-- ============================================
CREATE TABLE IF NOT EXISTS `comments` (
  `id` INT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '评论ID',
  `user_id` INT UNSIGNED NOT NULL COMMENT '评论用户ID',
  `image_id` INT UNSIGNED NOT NULL COMMENT '图片ID',
  `content` TEXT NOT NULL COMMENT '评论内容',
  `parent_id` INT UNSIGNED DEFAULT NULL COMMENT '父评论ID（用于回复）',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1=正常, 0=已删除',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  
  INDEX `idx_image_id` (`image_id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_parent_id` (`parent_id`),
  INDEX `idx_created_at` (`created_at`),
  CONSTRAINT `fk_comments_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_comments_image` FOREIGN KEY (`image_id`) REFERENCES `image` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_comments_parent` FOREIGN KEY (`parent_id`) REFERENCES `comments` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';

-- ============================================
-- 6. 插入默认管理员账号
-- ============================================
-- 密码：admin123（bcrypt加密后的值）
INSERT INTO `user` (`username`, `password`, `nickname`, `role`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '管理员', 'admin')
ON DUPLICATE KEY UPDATE `nickname` = '管理员';

-- ============================================
-- 完成
-- ============================================
SELECT '✅ 数据库初始化完成！' AS message;
SELECT '📊 数据库: pixel_lab' AS info;
SELECT '👤 默认管理员: admin / admin123' AS info;