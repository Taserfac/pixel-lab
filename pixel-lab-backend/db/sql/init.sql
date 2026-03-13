-- 【文件路径】db/sql/init.sql
-- 【文件功能说明】数据库初始化 SQL 脚本
-- 执行此脚本创建数据库和用户表

-- ============================================
-- 创建数据库
-- ============================================
CREATE DATABASE IF NOT EXISTS pixel_lab
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE pixel_lab;

-- ============================================
-- 用户表
-- ============================================
CREATE TABLE IF NOT EXISTS `user` (
  `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
  `username` VARCHAR(20) NOT NULL UNIQUE COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码（bcrypt加密）',
  `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `role` ENUM('user', 'admin') DEFAULT 'user' COMMENT '角色：user-普通用户，admin-管理员',
  `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '软删除标记：0-未删除，1-已删除',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_username` (`username`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================
-- 插入测试管理员账号（密码：admin123）
-- 生产环境请删除或修改
-- ============================================
-- INSERT INTO `user` (`username`, `password`, `nickname`, `role`) VALUES
-- ('admin', '$2a$10$YourHashedPasswordHere', '管理员', 'admin');
