/**
 * 【文件路径】middleware/auth.js
 * 【文件功能说明】JWT 身份认证中间件
 * - Token 校验
 * - 用户信息挂载
 * - 白名单配置
 */

const { verifyToken } = require('../utils/jwt')
const { error } = require('../utils/result')

// 白名单（无需登录的接口）
const whiteList = [
  '/auth/login',
  '/auth/register'
]

/**
 * JWT 认证中间件
 */
const authMiddleware = (req, res, next) => {
  // 白名单直接放行
  if (whiteList.includes(req.path)) {
    return next()
  }

  // 获取 token
  const authHeader = req.headers.authorization
  
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return error(res, '未提供有效的认证令牌', 401)
  }

  const token = authHeader.substring(7)
  
  // 验证 token
  const decoded = verifyToken(token)
  
  if (!decoded) {
    return error(res, '登录已过期，请重新登录', 401)
  }

  // 将用户信息挂载到 req
  req.user = decoded
  
  next()
}

/**
 * 可选认证中间件
 * 有 token 则解析用户信息，没有 token 也继续执行
 */
const optionalAuthMiddleware = (req, res, next) => {
  const authHeader = req.headers.authorization
  
  if (authHeader && authHeader.startsWith('Bearer ')) {
    const token = authHeader.substring(7)
    const decoded = verifyToken(token)
    if (decoded) {
      req.user = decoded
    }
  }
  
  next()
}

/**
 * 管理员权限校验中间件
 */
const adminMiddleware = (req, res, next) => {
  if (!req.user) {
    return error(res, '请先登录', 401)
  }

  if (req.user.role !== 'admin') {
    return error(res, '您没有权限执行此操作', 403)
  }

  next()
}

module.exports = {
  authMiddleware,
  optionalAuthMiddleware,
  adminMiddleware
}
