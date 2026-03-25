/**
 * 【文件路径】middleware/upload.js
 * 【功能说明】文件上传中间件
 * - 基于 multer 封装
 * - 文件类型、大小限制
 * - 文件扩展名校验
 * - 文件头校验（防止伪造类型）
 */

const multer = require('multer')
const path = require('path')
const fs = require('fs')
const config = require('../config')

// 允许的文件扩展名
const ALLOWED_EXTENSIONS = ['.jpg', '.jpeg', '.png', '.gif', '.webp']

// 文件头签名（Magic Numbers）
const FILE_SIGNATURES = {
  'image/jpeg': [0xFF, 0xD8, 0xFF],
  'image/png': [0x89, 0x50, 0x4E, 0x47],
  'image/gif': [0x47, 0x49, 0x46, 0x38],
  'image/webp': [0x52, 0x49, 0x46, 0x46] // RIFF (需要额外检查WEBP标识)
}

// 存储配置
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    const uploadPath = config.upload.path
    // 确保上传目录存在
    if (!fs.existsSync(uploadPath)) {
      fs.mkdirSync(uploadPath, { recursive: true })
    }
    cb(null, uploadPath)
  },
  filename: (req, file, cb) => {
    // 生成唯一文件名：时间戳_随机数.扩展名
    const uniqueSuffix = Date.now() + '_' + Math.round(Math.random() * 1E9)
    const ext = path.extname(file.originalname).toLowerCase()
    cb(null, file.fieldname + '_' + uniqueSuffix + ext)
  }
})

// 验证文件扩展名
const validateExtension = (filename) => {
  const ext = path.extname(filename).toLowerCase()
  return ALLOWED_EXTENSIONS.includes(ext)
}

// 验证文件头（Magic Numbers）
const validateFileHeader = (filePath, mimetype) => {
  try {
    const buffer = Buffer.alloc(8)
    const fd = fs.openSync(filePath, 'r')
    fs.readSync(fd, buffer, 0, 8, 0)
    fs.closeSync(fd)

    const signature = FILE_SIGNATURES[mimetype]
    if (!signature) return true // 如果没有定义签名，跳过校验

    for (let i = 0; i < signature.length; i++) {
      if (buffer[i] !== signature[i]) {
        // WebP 特殊处理：检查 RIFF....WEBP
        if (mimetype === 'image/webp') {
          const webpCheck = buffer.slice(0, 4).toString() === 'RIFF' &&
                           buffer.slice(8, 12).toString() === 'WEBP'
          return webpCheck
        }
        return false
      }
    }

    return true
  } catch (error) {
    console.error('文件头校验失败:', error)
    return false
  }
}

// 文件过滤
const fileFilter = (req, file, cb) => {
  // 1. 检查 MIME 类型
  if (!config.upload.allowedTypes.includes(file.mimetype)) {
    return cb(new Error(`不支持的文件类型: ${file.mimetype}`), false)
  }

  // 2. 检查文件扩展名
  if (!validateExtension(file.originalname)) {
    return cb(new Error(`不支持的文件扩展名: ${path.extname(file.originalname)}`), false)
  }

  // 3. 检查文件名是否包含危险字符
  const filename = path.basename(file.originalname)
  if (filename.includes('..') || filename.includes('/') || filename.includes('\\')) {
    return cb(new Error('文件名包含非法字符'), false)
  }

  cb(null, true)
}

// 创建 multer 实例
const upload = multer({
  storage,
  fileFilter,
  limits: {
    fileSize: config.upload.maxSize,
    files: 1 // 单次最多上传1个文件
  }
})

// 增强版上传中间件（包含文件头校验）
const uploadWithValidation = (fieldName) => {
  return [
    upload.single(fieldName),
    (req, res, next) => {
      if (!req.file) {
        return next()
      }

      // 文件头校验
      if (!validateFileHeader(req.file.path, req.file.mimetype)) {
        // 删除不合法的文件
        fs.unlinkSync(req.file.path)
        return res.status(400).json({
          code: 400,
          msg: '文件内容与类型不匹配，可能是伪造的图片文件',
          data: null
        })
      }

      // 附加文件信息到 req
      req.fileInfo = {
        originalName: req.file.originalname,
        mimeType: req.file.mimetype,
        size: req.file.size,
        path: req.file.path
      }

      next()
    }
  ]
}

module.exports = upload
module.exports.uploadWithValidation = uploadWithValidation