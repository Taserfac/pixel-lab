/**
 * 【文件路径】controller/image.js
 * 【功能说明】图片控制器
 */

const imageService = require('../service/image')
const { success, error } = require('../utils/result')
const path = require('path')

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
    
    // 生成图片URL
    const url = `${baseUrl}/uploads/${file.filename}`
    
    // 获取图片尺寸（需要安装 sharp 库，先简化）
    const imageData = {
      userId,
      filename: file.filename,
      originalName: file.originalname,
      url,
      thumbnailUrl: url, // 暂时用原图作为缩略图
      width: null,
      height: null,
      size: file.size,
      format: path.extname(file.originalname).slice(1)
    }
    
    const imageId = await imageService.create(imageData)
    
    success(res, {
      id: imageId,
      url,
      originalName: file.originalname,
      size: file.size
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
