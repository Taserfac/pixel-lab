/**
 * 【文件路径】service/profile.js
 * 【功能说明】用户个人资料服务层
 */

const db = require('../db')

/**
 * 更新用户资料
 */
async function updateProfile(userId, data) {
  const allowedFields = ['nickname', 'avatar']
  const fields = []
  const values = []

  for (const [key, value] of Object.entries(data)) {
    if (allowedFields.includes(key) && value !== undefined) {
      fields.push(`\`${key}\` = ?`)
      values.push(value)
    }
  }

  if (fields.length === 0) return 0

  values.push(userId)
  const sql = `UPDATE \`user\` SET ${fields.join(', ')} WHERE \`id\` = ?`
  return await db.update(sql, values)
}

/**
 * 修改密码
 */
async function changePassword(userId, newPassword) {
  const sql = 'UPDATE `user` SET `password` = ? WHERE `id` = ?'
  return await db.update(sql, [newPassword, userId])
}

module.exports = {
  updateProfile,
  changePassword
}