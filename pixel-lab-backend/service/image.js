/**
 * 【文件路径】service/image.js
 * 【功能说明】图片服务层
 */

const { query, insert, update: dbUpdate, remove } = require('../db')

/**
 * 创建图片记录
 */
const create = async (imageData) => {
  const { userId, filename, originalName, url, thumbnailUrl, width, height, size, format } = imageData
  
  return await insert(
    `INSERT INTO image 
     (user_id, filename, original_name, url, thumbnail_url, width, height, size, format) 
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`,
    [userId, filename, originalName, url, thumbnailUrl, width, height, size, format]
  )
}

/**
 * 根据ID查找图片
 */
const findById = async (id) => {
  const rows = await query(
    'SELECT * FROM image WHERE id = ? AND status = 1',
    [id]
  )
  return rows[0] || null
}

/**
 * 获取用户的图片列表
 */
const findByUserId = async (userId, options = {}) => {
  const page = parseInt(options.page) || 1
  const pageSize = parseInt(options.pageSize) || 20
  const isPublic = options.isPublic
  const offset = (page - 1) * pageSize
  
  let sql = 'SELECT * FROM image WHERE user_id = ? AND status = 1'
  let params = [userId]
  
  if (isPublic !== undefined) {
    sql += ' AND is_public = ?'
    params.push(isPublic)
  }
  
  sql += ' ORDER BY created_at DESC LIMIT ? OFFSET ?'
  params.push(pageSize, offset)
  
  return await query(sql, params)
}

/**
 * 获取用户图片总数
 */
const countByUserId = async (userId) => {
  const rows = await query(
    'SELECT COUNT(*) as count FROM image WHERE user_id = ? AND status = 1',
    [userId]
  )
  return rows[0].count
}

/**
 * 更新图片信息
 */
const update = async (id, updateData) => {
  const allowedFields = ['is_public', 'status']
  const fields = []
  const values = []
  
  for (const [key, value] of Object.entries(updateData)) {
    if (allowedFields.includes(key)) {
      fields.push(`${key} = ?`)
      values.push(value)
    }
  }
  
  if (fields.length === 0) return false
  
  values.push(id)
  const sql = `UPDATE image SET ${fields.join(', ')} WHERE id = ?`
  
  const affectedRows = await dbUpdate(sql, values)
  return affectedRows > 0
}

/**
 * 删除图片（软删除）
 */
const deleteById = async (id) => {
  const affectedRows = await remove(
    'UPDATE image SET status = 0 WHERE id = ?',
    [id]
  )
  return affectedRows > 0
}

module.exports = {
  create,
  findById,
  findByUserId,
  countByUserId,
  update,
  deleteById
}
