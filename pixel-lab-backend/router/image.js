/**
 * 【文件路径】router/image.js
 * 【功能说明】图片路由
 */

const express = require('express')
const router = express.Router()
const imageController = require('../controller/image')
const { authMiddleware } = require('../middleware/auth')
const upload = require('../middleware/upload')

// 上传图片
router.post('/upload', authMiddleware, upload.single('image'), imageController.upload)

// 获取用户图片列表
router.get('/', authMiddleware, imageController.getUserImages)

// 删除图片
router.delete('/:id', authMiddleware, imageController.deleteImage)

// 更新图片可见性
router.post('/:id/visibility', authMiddleware, imageController.updateVisibility)

module.exports = router
