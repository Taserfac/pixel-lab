/**
 * 【文件路径】utils/validate.js
 * 【文件功能说明】参数校验工具
 * - 基于 Joi 封装
 * - 常用校验规则
 */

const Joi = require('joi')

// 用户名校验（4-20位字母数字下划线）
const usernameSchema = Joi.string()
  .pattern(/^[a-zA-Z0-9_]{4,20}$/)
  .required()
  .messages({
    'string.pattern.base': '用户名只能包含4-20位字母、数字、下划线',
    'string.empty': '用户名不能为空'
  })

// 密码校验（6-20位）
const passwordSchema = Joi.string()
  .min(6)
  .max(20)
  .required()
  .messages({
    'string.min': '密码长度至少6位',
    'string.max': '密码长度最多20位',
    'string.empty': '密码不能为空'
  })

// 注册校验
const registerSchema = Joi.object({
  username: usernameSchema,
  password: passwordSchema
})

// 登录校验
const loginSchema = Joi.object({
  username: usernameSchema,
  password: passwordSchema
})

/**
 * 校验数据
 * @param {object} schema - Joi schema
 * @param {object} data - 要校验的数据
 * @returns {object} { error, value }
 */
const validate = (schema, data) => {
  return schema.validate(data, {
    abortEarly: false, // 返回所有错误
    stripUnknown: true // 移除未知字段
  })
}

module.exports = {
  validate,
  schemas: {
    register: registerSchema,
    login: loginSchema,
    username: usernameSchema,
    password: passwordSchema
  }
}
