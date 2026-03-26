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

/**
 * 获取用户统计数据
 * @param {number} userId - 用户ID
 * @returns {Promise<object>} 统计数据
 */
const getStats = async (userId) => {
  // 作品数
  const worksResult = await db.query(
    'SELECT COUNT(*) as count FROM `image` WHERE `user_id` = ? AND `status` = 1',
    [userId]
  )
  const works = worksResult[0]?.count || 0

  // 获赞数（用户所有作品的点赞总数）
  const likesResult = await db.query(
    `SELECT COALESCE(SUM(i.like_count), 0) as total 
     FROM \`image\` i 
     WHERE i.user_id = ? AND i.status = 1`,
    [userId]
  )
  const likes = likesResult[0]?.total || 0

  // 浏览量（用户所有作品的浏览总数）
  const viewsResult = await db.query(
    `SELECT COALESCE(SUM(i.view_count), 0) as total 
     FROM \`image\` i 
     WHERE i.user_id = ? AND i.status = 1`,
    [userId]
  )
  const views = viewsResult[0]?.total || 0

  // 收藏数（用户所有作品的收藏总数）
  const collectsResult = await db.query(
    `SELECT COALESCE(SUM(i.collect_count), 0) as total 
     FROM \`image\` i 
     WHERE i.user_id = ? AND i.status = 1`,
    [userId]
  )
  const collects = collectsResult[0]?.total || 0

  return { works, likes, views, collects }
}

module.exports = {
  findByUsername,
  findById,
  create,
  update,
  getStats
}
