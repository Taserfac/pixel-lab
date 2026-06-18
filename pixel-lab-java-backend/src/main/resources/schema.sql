-- Pixel Lab Social Feature Schema
-- 新增表：关注关系、通知

-- 关注关系表
CREATE TABLE IF NOT EXISTS `follows` (
  `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `follower_id` INT UNSIGNED NOT NULL COMMENT '关注者',
  `followed_id` INT UNSIGNED NOT NULL COMMENT '被关注者',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_follow` (`follower_id`, `followed_id`),
  KEY `idx_followed` (`followed_id`),
  CONSTRAINT `fk_follow_follower` FOREIGN KEY (`follower_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_follow_followed` FOREIGN KEY (`followed_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 通知表
CREATE TABLE IF NOT EXISTS `notifications` (
  `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT UNSIGNED NOT NULL COMMENT '接收者',
  `sender_id` INT UNSIGNED DEFAULT NULL COMMENT '发送者',
  `type` VARCHAR(20) NOT NULL COMMENT '通知类型: like/comment/follow/system',
  `content` VARCHAR(500) NOT NULL COMMENT '通知内容',
  `reference_id` INT UNSIGNED DEFAULT NULL COMMENT '关联ID（图片ID/评论ID等）',
  `reference_type` VARCHAR(20) DEFAULT NULL COMMENT '关联类型: image/comment',
  `is_read` TINYINT(1) DEFAULT 0 COMMENT '是否已读',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_notif_user` (`user_id`, `is_read`),
  KEY `idx_notif_created` (`created_at`),
  CONSTRAINT `fk_notif_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 图片说明/日记字段
ALTER TABLE `image` ADD COLUMN `description` TEXT DEFAULT NULL COMMENT '图片说明/日记' AFTER `tags`;

-- 作品集表
CREATE TABLE IF NOT EXISTS `albums` (
  `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT UNSIGNED NOT NULL COMMENT '创建者',
  `title` VARCHAR(100) NOT NULL COMMENT '作品集标题',
  `description` TEXT DEFAULT NULL COMMENT '作品集介绍',
  `cover_image_id` INT UNSIGNED DEFAULT NULL COMMENT '封面图片ID',
  `is_public` TINYINT(1) DEFAULT 1 COMMENT '是否公开',
  `status` TINYINT(1) DEFAULT 1 COMMENT '状态 1=正常 0=删除',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_album_user` (`user_id`),
  CONSTRAINT `fk_album_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 作品集-图片关联表
CREATE TABLE IF NOT EXISTS `album_images` (
  `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `album_id` INT UNSIGNED NOT NULL,
  `image_id` INT UNSIGNED NOT NULL,
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `description` TEXT DEFAULT NULL COMMENT '图片在作品集中的单独介绍',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_album_image` (`album_id`, `image_id`),
  KEY `idx_ai_image` (`image_id`),
  CONSTRAINT `fk_ai_album` FOREIGN KEY (`album_id`) REFERENCES `albums` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ai_image` FOREIGN KEY (`image_id`) REFERENCES `image` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户封禁字段
ALTER TABLE `user` ADD COLUMN `ban_days` INT DEFAULT NULL COMMENT '封禁天数，0=永久' AFTER `status`;
ALTER TABLE `user` ADD COLUMN `ban_reason` VARCHAR(255) DEFAULT NULL COMMENT '封禁原因' AFTER `ban_days`;
ALTER TABLE `user` ADD COLUMN `ban_end_at` TIMESTAMP NULL DEFAULT NULL COMMENT '封禁到期时间' AFTER `ban_reason`;
