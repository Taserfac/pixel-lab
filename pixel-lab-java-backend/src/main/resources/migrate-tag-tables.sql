-- 标签系统迁移脚本
-- 执行方式: mysql -u root --default-character-set=utf8mb4 pixel_lab < migrate-tag-tables.sql

SET NAMES utf8mb4;
USE `pixel_lab`;

-- 1. 创建 tag 表
CREATE TABLE IF NOT EXISTS `tag` (
  `id` INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL,
  `usage_count` INT UNSIGNED NOT NULL DEFAULT 0,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_name` (`name`),
  INDEX `idx_usage_count` (`usage_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. 创建 image_tag 关联表
CREATE TABLE IF NOT EXISTS `image_tag` (
  `image_id` INT UNSIGNED NOT NULL,
  `tag_id` INT UNSIGNED NOT NULL,
  PRIMARY KEY (`image_id`, `tag_id`),
  INDEX `idx_tag_id` (`tag_id`),
  CONSTRAINT `fk_image_tag_image` FOREIGN KEY (`image_id`) REFERENCES `image` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_image_tag_tag` FOREIGN KEY (`tag_id`) REFERENCES `tag` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. 插入预置标签
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

-- 4. 将现有图片的旧标签迁移到 image_tag 表
INSERT IGNORE INTO `image_tag` (`image_id`, `tag_id`)
SELECT i.id, t.id
FROM `image` i
JOIN `tag` t ON FIND_IN_SET(t.name, REPLACE(REPLACE(REPLACE(i.tags, '#', ''), '，', ','), ' ', ''))
WHERE i.tags IS NOT NULL AND i.tags != ''
  AND t.name != '';

-- 5. 更新 tag 表的 usage_count
UPDATE `tag` t SET `usage_count` = (
  SELECT COUNT(*) FROM `image_tag` it WHERE it.tag_id = t.id
);

SELECT 'Tag system migration completed!' AS message;
