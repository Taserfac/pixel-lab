/**
 * 【文件路径】config/index.js
 * 【文件功能说明】全局配置文件
 * - 从环境变量读取配置
 * - 统一导出所有配置项
 */

require('dotenv').config()

module.exports = {
  // 服务器配置
  server: {
    port: process.env.PORT || 3000,
    env: process.env.NODE_ENV || 'development'
  },

  // 数据库配置
  db: {
    host: process.env.DB_HOST || 'localhost',
    port: parseInt(process.env.DB_PORT) || 3306,
    user: process.env.DB_USER || 'root',
    password: process.env.DB_PASSWORD || '',
    database: process.env.DB_NAME || 'pixel_lab',
    // 连接池配置
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0
  },

  // JWT 配置
  jwt: {
    secret: process.env.JWT_SECRET || 'your-secret-key',
    expiresIn: process.env.JWT_EXPIRES_IN || '7d'
  },

  // 文件上传配置
  upload: {
    path: process.env.UPLOAD_PATH || './uploads',
    maxSize: parseInt(process.env.MAX_FILE_SIZE) || 5 * 1024 * 1024, // 5MB
    allowedTypes: (process.env.ALLOWED_TYPES || 'image/jpeg,image/png,image/gif,image/webp,image/heic,image/heif').split(','),
    // 图片处理配置
    image: {
      maxWidth: 1920,
      maxHeight: 1920,
      quality: 85,
      thumbnailSize: 300
    }
  },

  // CORS 配置
  cors: {
    origin: process.env.CORS_ORIGIN || '*',
    methods: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS'],
    allowedHeaders: ['Content-Type', 'Authorization']
  }
}
