/**
 * 【文件路径】router/index.js
 * 【文件功能说明】路由入口
 * - 统一挂载所有路由模块
 */

const express = require('express')
const router = express.Router()

// 认证路由
const authRouter = require('./auth')
const imageRouter = require('./image')
const communityRouter = require('./community')

// 挂载路由
router.use('/auth', authRouter)
router.use('/images', imageRouter)
router.use('/community', communityRouter)

// 健康检查
router.get('/health', (req, res) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString() })
})

module.exports = router
