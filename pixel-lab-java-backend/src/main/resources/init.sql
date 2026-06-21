CREATE DATABASE IF NOT EXISTS `pixel_lab`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `pixel_lab`;

CREATE TABLE IF NOT EXISTS `user` (
  `id` INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL,
  `password` VARCHAR(255) NOT NULL,
  `nickname` VARCHAR(50) DEFAULT NULL,
  `avatar` VARCHAR(255) DEFAULT NULL,
  `role` ENUM('user', 'admin') NOT NULL DEFAULT 'user',
  `status` TINYINT NOT NULL DEFAULT 1,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_username` (`username`),
  INDEX `idx_status` (`status`),
  INDEX `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `image` (
  `id` INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `user_id` INT UNSIGNED NOT NULL,
  `filename` VARCHAR(100) NOT NULL,
  `original_name` VARCHAR(255) DEFAULT NULL,
  `url` VARCHAR(500) NOT NULL,
  `thumbnail_url` VARCHAR(500) DEFAULT NULL,
  `width` INT UNSIGNED DEFAULT NULL,
  `height` INT UNSIGNED DEFAULT NULL,
  `size` INT UNSIGNED DEFAULT NULL,
  `format` VARCHAR(20) DEFAULT NULL,
  `is_public` TINYINT NOT NULL DEFAULT 0,
  `status` TINYINT NOT NULL DEFAULT 1,
  `title` VARCHAR(100) DEFAULT NULL,
  `description` TEXT DEFAULT NULL,
  `tags` VARCHAR(255) DEFAULT NULL,
  `view_count` INT UNSIGNED NOT NULL DEFAULT 0,
  `like_count` INT UNSIGNED NOT NULL DEFAULT 0,
  `comment_count` INT UNSIGNED NOT NULL DEFAULT 0,
  `collect_count` INT UNSIGNED NOT NULL DEFAULT 0,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_is_public` (`is_public`),
  INDEX `idx_created_at` (`created_at`),
  INDEX `idx_like_count` (`like_count`),
  INDEX `idx_view_count` (`view_count`),
  CONSTRAINT `fk_image_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `likes` (
  `id` INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `user_id` INT UNSIGNED NOT NULL,
  `image_id` INT UNSIGNED NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_user_image` (`user_id`, `image_id`),
  INDEX `idx_image_id` (`image_id`),
  INDEX `idx_user_id` (`user_id`),
  CONSTRAINT `fk_likes_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_likes_image` FOREIGN KEY (`image_id`) REFERENCES `image` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `collections` (
  `id` INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `user_id` INT UNSIGNED NOT NULL,
  `image_id` INT UNSIGNED NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_user_image` (`user_id`, `image_id`),
  INDEX `idx_image_id` (`image_id`),
  INDEX `idx_user_id` (`user_id`),
  CONSTRAINT `fk_collections_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_collections_image` FOREIGN KEY (`image_id`) REFERENCES `image` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `comments` (
  `id` INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `user_id` INT UNSIGNED NOT NULL,
  `image_id` INT UNSIGNED NOT NULL,
  `content` TEXT NOT NULL,
  `parent_id` INT UNSIGNED DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_image_id` (`image_id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_parent_id` (`parent_id`),
  INDEX `idx_created_at` (`created_at`),
  CONSTRAINT `fk_comments_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_comments_image` FOREIGN KEY (`image_id`) REFERENCES `image` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_comments_parent` FOREIGN KEY (`parent_id`) REFERENCES `comments` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `reports` (
  `id` INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `image_id` INT UNSIGNED NOT NULL,
  `reporter_id` INT UNSIGNED NOT NULL,
  `reason` VARCHAR(80) NOT NULL,
  `detail` VARCHAR(500) DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 0,
  `handler_id` INT UNSIGNED DEFAULT NULL,
  `handled_at` TIMESTAMP NULL DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_reporter_image_open` (`reporter_id`, `image_id`, `status`),
  INDEX `idx_image_id` (`image_id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_created_at` (`created_at`),
  CONSTRAINT `fk_reports_image` FOREIGN KEY (`image_id`) REFERENCES `image` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_reports_reporter` FOREIGN KEY (`reporter_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_reports_handler` FOREIGN KEY (`handler_id`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `tutorial_videos` (
  `id` INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `content_type` ENUM('video', 'article') NOT NULL DEFAULT 'video',
  `category` VARCHAR(40) NOT NULL,
  `title` VARCHAR(200) NOT NULL,
  `description` VARCHAR(500) DEFAULT NULL,
  `cover_url` VARCHAR(500) DEFAULT NULL,
  `source_url` VARCHAR(500) NOT NULL,
  `embed_url` VARCHAR(500) DEFAULT NULL,
  `source_name` VARCHAR(40) DEFAULT NULL,
  `author_name` VARCHAR(100) DEFAULT NULL,
  `duration` VARCHAR(30) DEFAULT NULL,
  `view_count` INT UNSIGNED NOT NULL DEFAULT 0,
  `published_at` VARCHAR(40) DEFAULT NULL,
  `sort_order` INT NOT NULL DEFAULT 100,
  `status` TINYINT NOT NULL DEFAULT 1,
  `crawled_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_tutorial_source_url` (`source_url`),
  INDEX `idx_category` (`category`),
  INDEX `idx_status` (`status`),
  INDEX `idx_sort_order` (`sort_order`),
  INDEX `idx_view_count` (`view_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `tag` (
  `id` INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL,
  `usage_count` INT UNSIGNED NOT NULL DEFAULT 0,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_name` (`name`),
  INDEX `idx_usage_count` (`usage_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `image_tag` (
  `image_id` INT UNSIGNED NOT NULL,
  `tag_id` INT UNSIGNED NOT NULL,
  PRIMARY KEY (`image_id`, `tag_id`),
  INDEX `idx_tag_id` (`tag_id`),
  CONSTRAINT `fk_image_tag_image` FOREIGN KEY (`image_id`) REFERENCES `image` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_image_tag_tag` FOREIGN KEY (`tag_id`) REFERENCES `tag` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `tag` (`name`) VALUES
('夜景'),('城市'),('人像'),('街拍'),('风景'),('建筑'),('室内'),('黑白'),('日落'),('夜拍'),
('航拍'),('微距'),('纪实'),('旅行摄影'),('生活记录'),
('插画'),('绘画'),('手绘'),('概念设计'),('平面设计'),('海报设计'),('数字艺术'),('抽象艺术'),
('涂鸦'),('创意设计'),('角色设计'),('场景设计'),('视觉设计'),('艺术创作'),
('AI艺术'),('AI绘画'),('AI生成'),('AI插画'),('AI设计'),('AI摄影风'),('AI概念图'),
('未来感'),('科幻风'),('虚拟角色'),('模型生成'),('AI人像'),('AI场景'),
('自然'),('城市夜景'),('街道'),('校园'),('海边'),('山景'),('森林'),('雨天'),('雪景'),
('室内场景'),('未来城市'),('工业风'),('废墟'),('乡村'),('公园'),
('半身像'),('全身像'),('多人'),('情侣'),('儿童'),('老人'),('表情特写'),('时尚'),
('街头人物'),('生活人像'),('肖像摄影'),('氛围人像'),('模特'),
('猫'),('狗'),('宠物'),('野生动物'),('鸟类'),('鱼类'),('昆虫'),('豹'),('狮子'),
('老虎'),('熊'),('鹿'),('海洋生物'),('动物特写'),('动物摄影'),
('美食'),('料理'),('甜点'),('饮品'),('咖啡'),('早餐'),('午餐'),('晚餐'),('水果'),
('烧烤'),('火锅'),('面食'),('蛋糕'),('西餐'),('中餐'),('街头小吃'),('精致料理'),('食物摄影'),
('极简'),('复古'),('赛博朋克'),('胶片风'),('电影感'),('写实'),('插画风'),('高对比'),
('柔光'),('低饱和'),('高饱和'),('梦幻'),('暗黑风'),('清新风'),('日系风'),('欧美风'),
('壁纸'),('灵感'),('作品集'),('展示'),('练习'),('设计稿'),('概念稿'),('分享'),
('草图'),('实验作品'),('项目展示'),('商业稿')
ON DUPLICATE KEY UPDATE `name` = `name`;

INSERT INTO `user` (`username`, `password`, `nickname`, `role`) VALUES
('admin', '$2a$10$UdhF1MQ3B9wl/hD7fmiAKecGwvKFXQpn3DetoiSijpwp5JwbiQoV6', 'admin', 'admin')
ON DUPLICATE KEY UPDATE `nickname` = 'admin';

SELECT 'Database initialized successfully' AS message;
SELECT 'Default admin: admin / admin123' AS info;
