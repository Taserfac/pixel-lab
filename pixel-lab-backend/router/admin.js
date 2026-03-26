/**
 * 【文件路径】router/admin.js
 * 【功能说明】后台管理路由
 */

const express = require('express')
const router = express.Router()
const adminController = require('../controller/admin')
const { authMiddleware, adminMiddleware } = require('../middleware/auth')

// 所有接口都需要管理员权限
router.use(authMiddleware)
router.use(adminMiddleware)

// 用户管理
router.get('/users', adminController.getUsers)
router.patch('/users/:id/status', adminController.updateUserStatus)
router.patch('/users/:id/role', adminController.updateUserRole)

// 作品管理
router.get('/images', adminController.getImages)
router.delete('/images/:id', adminController.deleteImage)

// 平台统计
router.get('/stats', adminController.getStats)

module.exports = router