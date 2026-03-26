/**
 * 【文件路径】router/auth.js
 * 【文件功能说明】认证路由
 * - 注册、登录、获取用户信息
 */

const express = require('express')
const router = express.Router()
const authController = require('../controller/auth')
const profileController = require('../controller/profile')
const { authMiddleware } = require('../middleware/auth')
const validate = require('../middleware/validate')
const { schemas } = require('../utils/validate')

/**
 * @route   POST /api/auth/register
 * @desc    用户注册
 * @access  Public
 */
router.post('/register', validate(schemas.register), authController.register)

/**
 * @route   POST /api/auth/login
 * @desc    用户登录
 * @access  Public
 */
router.post('/login', validate(schemas.login), authController.login)

/**
 * @route   GET /api/auth/userinfo
 * @desc    获取当前用户信息
 * @access  Private
 */
router.get('/userinfo', authMiddleware, authController.getUserInfo)

/**
 * @route   GET /api/auth/stats
 * @desc    获取用户统计数据
 * @access  Private
 */
router.get('/stats', authMiddleware, authController.getUserStats)

/**
 * @route   PATCH /api/auth/profile
 * @desc    更新用户资料
 * @access  Private
 */
router.patch('/profile', authMiddleware, profileController.updateProfile)

/**
 * @route   PATCH /api/auth/password
 * @desc    修改密码
 * @access  Private
 */
router.patch('/password', authMiddleware, profileController.changePassword)

module.exports = router
