/**
 * 【文件路径】middleware/error-handler.js
 * 【文件功能说明】全局异常处理中间件
 * - 捕获所有接口异常
 * - 统一返回错误信息
 * - 区分错误类型
 */

const { error } = require('../utils/result')

/**
 * 全局错误处理中间件
 */
const errorHandler = (err, req, res, next) => {
  console.error('[Error]', err)

  // Joi 校验错误
  if (err.name === 'ValidationError') {
    const messages = err.details.map(d => d.message).join(', ')
    return error(res, `参数校验失败: ${messages}`, 400)
  }

  // MySQL 错误
  if (err.code === 'ER_DUP_ENTRY') {
    return error(res, '数据已存在', 409)
  }

  if (err.code === 'ER_NO_REFERENCED_ROW') {
    return error(res, '关联数据不存在', 400)
  }

  // JWT 错误
  if (err.name === 'JsonWebTokenError') {
    return error(res, '无效的令牌', 401)
  }

  if (err.name === 'TokenExpiredError') {
    return error(res, '令牌已过期', 401)
  }

  // 文件上传错误
  if (err.code === 'LIMIT_FILE_SIZE') {
    return error(res, '文件大小超出限制', 400)
  }

  if (err.code === 'LIMIT_FILE_TYPES') {
    return error(res, '不支持的文件类型', 400)
  }

  // 默认服务器错误
  error(res, err.message || '服务器内部错误', 500)
}

module.exports = errorHandler
