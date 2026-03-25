/**
 * 【文件路径】app.js
 * 【文件功能说明】后端项目入口文件
 * - Express 应用配置
 * - 中间件挂载
 * - 路由挂载
 * - 错误处理
 */

require('express-async-errors')
const express = require('express')
const cors = require('cors')
const path = require('path')
const config = require('./config')
const { testConnection } = require('./db')
const routes = require('./router')
const errorHandler = require('./middleware/error-handler')
const { authMiddleware } = require('./middleware/auth')

// 创建 Express 应用
const app = express()

// ============================================
// 中间件配置
// ============================================

// CORS 跨域
app.use(cors(config.cors))

// 解析 JSON 请求体
app.use(express.json())

// 解析 URL 编码请求体
app.use(express.urlencoded({ extended: true }))

// 静态文件服务
app.use('/uploads', express.static(path.join(__dirname, 'uploads')))

// ============================================
// 路由配置
// ============================================

// API 路由（不在全局使用认证中间件，由各路由自行决定）
app.use('/api', routes)

// ============================================
// 错误处理
// ============================================

// 404 处理
app.use((req, res) => {
  res.status(404).json({
    code: 404,
    msg: '接口不存在',
    data: null
  })
})

// 全局错误处理
app.use(errorHandler)

// ============================================
// 启动服务
// ============================================

const startServer = async () => {
  // 测试数据库连接
  const dbConnected = await testConnection()
  
  if (!dbConnected) {
    console.error('[Server] 数据库连接失败，请检查配置')
    process.exit(1)
  }

  // 启动服务
  app.listen(config.server.port, () => {
    console.log(`[Server] 服务已启动，监听端口: ${config.server.port}`)
    console.log(`[Server] 环境: ${config.server.env}`)
    console.log(`[Server] API 地址: http://localhost:${config.server.port}/api`)
  })
}

startServer()
