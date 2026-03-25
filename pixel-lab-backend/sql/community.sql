-- 社区功能相关表

-- 1. 作品表（扩展现有 image 表，添加社区相关字段）
ALTER TABLE image 
ADD COLUMN title VARCHAR(100) DEFAULT NULL COMMENT '作品标题',
ADD COLUMN description TEXT DEFAULT NULL COMMENT '作品描述',
ADD COLUMN tags VARCHAR(255) DEFAULT NULL COMMENT '标签，逗号分隔',
ADD COLUMN view_count INT DEFAULT 0 COMMENT '浏览次数',
ADD COLUMN like_count INT DEFAULT 0 COMMENT '点赞数',
ADD COLUMN comment_count INT DEFAULT 0 COMMENT '评论数',
ADD COLUMN collect_count INT DEFAULT 0 COMMENT '收藏数';

-- 2. 点赞表
CREATE TABLE IF NOT EXISTS likes (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_id INT NOT NULL COMMENT '点赞用户ID',
  image_id INT NOT NULL COMMENT '作品ID',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_image (user_id, image_id),
  INDEX idx_image_id (image_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点赞表';

-- 3. 评论表
CREATE TABLE IF NOT EXISTS comments (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_id INT NOT NULL COMMENT '评论用户ID',
  image_id INT NOT NULL COMMENT '作品ID',
  content TEXT NOT NULL COMMENT '评论内容',
  parent_id INT DEFAULT NULL COMMENT '父评论ID（用于回复）',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_image_id (image_id),
  INDEX idx_user_id (user_id),
  INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- 4. 收藏表
CREATE TABLE IF NOT EXISTS collections (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_id INT NOT NULL COMMENT '收藏用户ID',
  image_id INT NOT NULL COMMENT '作品ID',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_image (user_id, image_id),
  INDEX idx_image_id (image_id),
  INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏表';