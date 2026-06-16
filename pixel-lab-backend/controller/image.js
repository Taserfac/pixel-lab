/**
 * 【文件路径】controller/image.js
 * 【功能说明】图片控制器
 */

const imageService = require('../service/image')
const { success, error } = require('../utils/result')
const path = require('path')
const sharp = require('sharp')
const config = require('../config')

const IMAGE_CATEGORIES = new Set([
  'drawing',
  'edit',
  'ai',
  'pixel',
  'photo',
  'character',
  'landscape',
  'avatar',
  'other'
])

const normalizeCategory = (value) => {
  if (!value) return null
  const category = String(value).trim().toLowerCase()
  return IMAGE_CATEGORIES.has(category) ? category : null
}

const normalizeTags = (value) => {
  if (!value) return null
  const tags = String(value).trim()
  return tags.length > 255 ? tags.slice(0, 255) : tags
}

/**
 * 处理图片压缩
 */
async function processImage(filePath, originalName) {
  const { maxWidth, maxHeight, quality } = config.upload.image
  
  try {
    const image = sharp(filePath)
    const metadata = await image.metadata()
    
    // HEIC/HEIF 格式转换为 JPEG
    const ext = path.extname(originalName).toLowerCase()
    const isHeic = ext === '.heic' || ext === '.heif' || metadata.format === 'heic' || metadata.format === 'heif'
    
    if (isHeic) {
      // 转换 HEIC 为 JPEG
      const newPath = filePath.replace(/\.(heic|heif)$/i, '.jpg')
      await image
        .resize(maxWidth, maxHeight, {
          fit: 'inside',
          withoutEnlargement: true
        })
        .jpeg({ quality, mozjpeg: true })
        .toFile(newPath)
      
      // 删除原文件，重命名新文件
      const fs = require('fs')
      fs.unlinkSync(filePath)
      fs.renameSync(newPath, filePath)
      
      return fs.statSync(filePath).size
    }
    
    // 只有图片超过最大尺寸才压缩
    if (metadata.width > maxWidth || metadata.height > maxHeight) {
      await image
        .resize(maxWidth, maxHeight, {
          fit: 'inside',
          withoutEnlargement: true
        })
        .jpeg({ quality, mozjpeg: true })
        .toFile(filePath + '.tmp')
      
      // 替换原文件
      const fs = require('fs')
      fs.unlinkSync(filePath)
      fs.renameSync(filePath + '.tmp', filePath)
      
      return fs.statSync(filePath).size
    }
    
    return metadata.size
  } catch (err) {
    console.error('图片处理失败:', err)
    return null
  }
}

/**
 * 上传图片
 * POST /api/images/upload
 */
const upload = async (req, res) => {
  try {
    const file = req.file
    if (!file) {
      return error(res, '请选择要上传的图片', 400)
    }
    
    const userId = req.user.id
    const baseUrl = `${req.protocol}://${req.get('host')}`
    
    // 压缩图片
    const processedSize = await processImage(file.path, file.originalname)
    const finalSize = processedSize || file.size
    
    // 获取图片尺寸
    let width = null, height = null
    try {
      const metadata = await sharp(file.path).metadata()
      width = metadata.width
      height = metadata.height
    } catch (e) {
      // 忽略错误
    }
    
    // 生成图片URL
    const url = `${baseUrl}/uploads/${file.filename}`
    const tags = normalizeCategory(req.body.category) || normalizeTags(req.body.tags)
    
    const imageData = {
      userId,
      filename: file.filename,
      originalName: file.originalname,
      url,
      thumbnailUrl: url,
      width,
      height,
      size: finalSize,
      format: path.extname(file.originalname).slice(1),
      tags
    }
    
    const imageId = await imageService.create(imageData)
    
    success(res, {
      id: imageId,
      url,
      originalName: file.originalname,
      tags,
      size: finalSize,
      width,
      height
    }, '上传成功', 201)
  } catch (err) {
    console.error('上传图片失败:', err)
    error(res, '上传失败', 500)
  }
}

/**
 * 获取用户的图片列表
 * GET /api/images
 */
const getUserImages = async (req, res) => {
  try {
    const userId = req.user.id
    const page = parseInt(req.query.page) || 1
    const pageSize = parseInt(req.query.pageSize) || 20
    
    const images = await imageService.findByUserId(userId, { page, pageSize })
    const total = await imageService.countByUserId(userId)
    
    success(res, {
      list: images,
      pagination: {
        page,
        pageSize,
        total,
        totalPages: Math.ceil(total / pageSize)
      }
    })
  } catch (err) {
    console.error('获取图片列表失败:', err)
    error(res, '获取失败', 500)
  }
}

/**
 * 删除图片
 * DELETE /api/images/:id
 */
const deleteImage = async (req, res) => {
  try {
    const { id } = req.params
    const userId = req.user.id
    
    // 检查图片是否属于当前用户
    const image = await imageService.findById(id)
    if (!image) {
      return error(res, '图片不存在', 404)
    }
    
    if (image.user_id !== userId) {
      return error(res, '无权删除此图片', 403)
    }
    
    await imageService.deleteById(id)
    success(res, null, '删除成功')
  } catch (err) {
    console.error('删除图片失败:', err)
    error(res, '删除失败', 500)
  }
}

/**
 * 设置图片公开/私有
 * PATCH /api/images/:id/visibility
 */
const updateVisibility = async (req, res) => {
  try {
    const { id } = req.params
    const { isPublic } = req.body
    const userId = req.user.id
    
    const image = await imageService.findById(id)
    if (!image) {
      return error(res, '图片不存在', 404)
    }
    
    if (image.user_id !== userId) {
      return error(res, '无权修改', 403)
    }
    
    await imageService.update(id, { is_public: isPublic ? 1 : 0 })
    success(res, null, '更新成功')
  } catch (err) {
    console.error('更新图片可见性失败:', err)
    error(res, '更新失败', 500)
  }
}

module.exports = {
  upload,
  getUserImages,
  deleteImage,
  updateVisibility
}
