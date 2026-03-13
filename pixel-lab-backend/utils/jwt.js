/**
 * 【文件路径】utils/jwt.js
 * 【文件功能说明】JWT Token 工具
 * - Token 生成
 * - Token 验证
 */

const jwt = require('jsonwebtoken')
const config = require('../config')

/**
 * 生成 JWT Token
 * @param {object} payload - 要编码的数据
 * @returns {string} JWT Token
 */
const generateToken = (payload) => {
  return jwt.sign(payload, config.jwt.secret, {
    expiresIn: config.jwt.expiresIn
  })
}

/**
 * 验证 JWT Token
 * @param {string} token - JWT Token
 * @returns {object|null} 解码后的数据或 null
 */
const verifyToken = (token) => {
  try {
    return jwt.verify(token, config.jwt.secret)
  } catch (error) {
    return null
  }
}

module.exports = {
  generateToken,
  verifyToken
}
