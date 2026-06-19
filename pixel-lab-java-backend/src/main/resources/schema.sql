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

-- 作品举报表
CREATE TABLE IF NOT EXISTS `reports` (
  `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `image_id` INT UNSIGNED NOT NULL COMMENT '被举报作品',
  `reporter_id` INT UNSIGNED NOT NULL COMMENT '举报人',
  `reason` VARCHAR(80) NOT NULL COMMENT '举报原因',
  `detail` VARCHAR(500) DEFAULT NULL COMMENT '补充说明',
  `status` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '状态 0=待处理 1=已处理 2=已驳回',
  `handler_id` INT UNSIGNED DEFAULT NULL COMMENT '处理管理员',
  `handled_at` TIMESTAMP NULL DEFAULT NULL COMMENT '处理时间',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_reporter_image_open` (`reporter_id`, `image_id`, `status`),
  KEY `idx_report_image` (`image_id`),
  KEY `idx_report_status` (`status`),
  KEY `idx_report_created` (`created_at`),
  CONSTRAINT `fk_report_image` FOREIGN KEY (`image_id`) REFERENCES `image` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_report_reporter` FOREIGN KEY (`reporter_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_report_handler` FOREIGN KEY (`handler_id`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 教学视频表：由爬虫写入公开可访问视频元数据，不存储视频文件
CREATE TABLE IF NOT EXISTS `tutorial_videos` (
  `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `content_type` ENUM('video', 'article') NOT NULL DEFAULT 'video' COMMENT '内容类型',
  `category` VARCHAR(40) NOT NULL COMMENT '教学分类',
  `title` VARCHAR(200) NOT NULL COMMENT '视频标题',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '视频简介',
  `cover_url` VARCHAR(500) DEFAULT NULL COMMENT '封面图',
  `source_url` VARCHAR(500) NOT NULL COMMENT '原平台链接',
  `embed_url` VARCHAR(500) DEFAULT NULL COMMENT '嵌入播放链接',
  `source_name` VARCHAR(40) DEFAULT NULL COMMENT '来源平台',
  `author_name` VARCHAR(100) DEFAULT NULL COMMENT '作者',
  `duration` VARCHAR(30) DEFAULT NULL COMMENT '时长',
  `view_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '播放量',
  `published_at` VARCHAR(40) DEFAULT NULL COMMENT '发布时间',
  `sort_order` INT NOT NULL DEFAULT 100 COMMENT '排序',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态 1=展示 0=隐藏',
  `crawled_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_tutorial_source_url` (`source_url`),
  KEY `idx_tutorial_category` (`category`),
  KEY `idx_tutorial_status` (`status`),
  KEY `idx_tutorial_sort` (`sort_order`),
  KEY `idx_tutorial_views` (`view_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
