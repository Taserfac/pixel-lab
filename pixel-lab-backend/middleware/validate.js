/**
 * 【文件路径】middleware/validate.js
 * 【文件功能说明】参数校验中间件
 * - 基于 Joi 的校验中间件封装
 */

const { error } = require('../utils/result')

/**
 * 创建校验中间件
 * @param {object} schema - Joi schema
 * @returns {Function} Express 中间件
 */
const validate = (schema) => {
  return (req, res, next) => {
    const { error: validationError, value } = schema.validate(req.body, {
      abortEarly: false,
      stripUnknown: true
    })

    if (validationError) {
      const messages = validationError.details.map(d => d.message).join(', ')
      return error(res, `参数校验失败: ${messages}`, 400)
    }

    // 用校验后的值替换原请求体
    req.body = value
    next()
  }
}

module.exports = validate
