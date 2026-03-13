/**
 * 【文件路径】middleware/upload.js
 * 【文件功能说明】文件上传中间件
 * - 基于 multer 封装
 * - 文件类型、大小限制
 */

const multer = require('multer')
const path = require('path')
const config = require('../config')

// 存储配置
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, config.upload.path)
  },
  filename: (req, file, cb) => {
    // 生成唯一文件名：时间戳_随机数.扩展名
    const uniqueSuffix = Date.now() + '_' + Math.round(Math.random() * 1E9)
    const ext = path.extname(file.originalname)
    cb(null, file.fieldname + '_' + uniqueSuffix + ext)
  }
})

// 文件过滤
const fileFilter = (req, file, cb) => {
  if (config.upload.allowedTypes.includes(file.mimetype)) {
    cb(null, true)
  } else {
    cb(new Error('不支持的文件类型'), false)
  }
}

// 创建 multer 实例
const upload = multer({
  storage,
  fileFilter,
  limits: {
    fileSize: config.upload.maxSize
  }
})

module.exports = upload
