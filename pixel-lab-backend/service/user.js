/**
 * 【文件路径】service/user.js
 * 【文件功能说明】用户服务层
 * - 用户数据库操作封装
 * - 与控制器层解耦
 */

const db = require('../db')

/**
 * 根据用户名查找用户
 * @param {string} username - 用户名
 * @returns {Promise<object|null>} 用户信息
 */
const findByUsername = async (username) => {
  const sql = 'SELECT * FROM `user` WHERE `username` = ? AND `is_deleted` = 0'
  const users = await db.query(sql, [username])
  return users.length > 0 ? users[0] : null
}

/**
 * 根据 ID 查找用户
 * @param {number} id - 用户ID
 * @returns {Promise<object|null>} 用户信息
 */
const findById = async (id) => {
  const sql = 'SELECT `id`, `username`, `nickname`, `avatar`, `role`, `status`, `created_at` FROM `user` WHERE `id` = ? AND `is_deleted` = 0'
  const users = await db.query(sql, [id])
  return users.length > 0 ? users[0] : null
}

/**
 * 创建用户
 * @param {object} userData - 用户数据
 * @returns {Promise<number>} 新用户ID
 */
const create = async (userData) => {
  const sql = 'INSERT INTO `user` (`username`, `password`, `nickname`, `role`) VALUES (?, ?, ?, ?)'
  const { username, password, nickname, role = 'user' } = userData
  return await db.insert(sql, [username, password, nickname || username, role])
}

/**
 * 更新用户信息
 * @param {number} id - 用户ID
 * @param {object} userData - 更新的数据
 * @returns {Promise<number>} 影响的行数
 */
const update = async (id, userData) => {
  const allowedFields = ['nickname', 'avatar', 'password']
  const fields = []
  const values = []

  for (const [key, value] of Object.entries(userData)) {
    if (allowedFields.includes(key)) {
      fields.push(`\`${key}\` = ?`)
      values.push(value)
    }
  }

  if (fields.length === 0) return 0

  values.push(id)
  const sql = `UPDATE \`user\` SET ${fields.join(', ')} WHERE \`id\` = ?`
  return await db.update(sql, values)
}

module.exports = {
  findByUsername,
  findById,
  create,
  update
}
