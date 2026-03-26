/**
 * 【文件路径】service/admin.js
 * 【功能说明】后台管理服务层
 */

const db = require('../db')

/**
 * 获取用户列表
 */
async function getUsers({ page = 1, pageSize = 20, keyword = '' }) {
  const offset = (page - 1) * pageSize
  let whereClause = 'WHERE 1=1'
  const params = []

  if (keyword) {
    whereClause += ' AND (username LIKE ? OR nickname LIKE ?)'
    const likeKeyword = `%${keyword}%`
    params.push(likeKeyword, likeKeyword)
  }

  const countResult = await db.query(
    `SELECT COUNT(*) as total FROM user ${whereClause}`,
    params
  )
  const total = countResult[0]?.total || 0

  const rows = await db.query(
    `SELECT id, username, nickname, avatar, role, status, created_at 
     FROM user 
     ${whereClause}
     ORDER BY created_at DESC 
     LIMIT ? OFFSET ?`,
    [...params, pageSize, offset]
  )

  return {
    list: rows || [],
    total,
    page,
    pageSize,
    totalPages: Math.ceil(total / pageSize)
  }
}

/**
 * 更新用户状态
 */
async function updateUserStatus(userId, status) {
  const affectedRows = await db.update(
    'UPDATE user SET status = ? WHERE id = ?',
    [status, userId]
  )
  return affectedRows > 0
}

/**
 * 更新用户角色
 */
async function updateUserRole(userId, role) {
  const affectedRows = await db.update(
    'UPDATE user SET role = ? WHERE id = ?',
    [role, userId]
  )
  return affectedRows > 0
}

/**
 * 获取所有作品列表
 */
async function getImages({ page = 1, pageSize = 20, keyword = '', status = '' }) {
  const offset = (page - 1) * pageSize
  let whereClause = 'WHERE 1=1'
  const params = []

  if (keyword) {
    whereClause += ' AND (i.title LIKE ? OR i.original_name LIKE ? OR u.nickname LIKE ?)'
    const likeKeyword = `%${keyword}%`
    params.push(likeKeyword, likeKeyword, likeKeyword)
  }

  if (status !== '') {
    whereClause += ' AND i.status = ?'
    params.push(parseInt(status))
  }

  const countResult = await db.query(
    `SELECT COUNT(*) as total FROM image i LEFT JOIN user u ON i.user_id = u.id ${whereClause}`,
    params
  )
  const total = countResult[0]?.total || 0

  const rows = await db.query(
    `SELECT i.*, u.username, u.nickname as author_name 
     FROM image i 
     LEFT JOIN user u ON i.user_id = u.id 
     ${whereClause}
     ORDER BY i.created_at DESC 
     LIMIT ? OFFSET ?`,
    [...params, pageSize, offset]
  )

  return {
    list: rows || [],
    total,
    page,
    pageSize,
    totalPages: Math.ceil(total / pageSize)
  }
}

/**
 * 删除作品
 */
async function deleteImage(imageId) {
  const affectedRows = await db.update(
    'UPDATE image SET status = 0 WHERE id = ?',
    [imageId]
  )
  return affectedRows > 0
}

/**
 * 获取平台统计
 */
async function getPlatformStats() {
  // 用户总数
  const usersResult = await db.query('SELECT COUNT(*) as count FROM user WHERE is_deleted = 0')
  const totalUsers = usersResult[0]?.count || 0

  // 今日新增用户
  const todayUsersResult = await db.query(
    "SELECT COUNT(*) as count FROM user WHERE is_deleted = 0 AND DATE(created_at) = CURDATE()"
  )
  const todayUsers = todayUsersResult[0]?.count || 0

  // 作品总数
  const imagesResult = await db.query('SELECT COUNT(*) as count FROM image WHERE status = 1')
  const totalImages = imagesResult[0]?.count || 0

  // 今日新增作品
  const todayImagesResult = await db.query(
    "SELECT COUNT(*) as count FROM image WHERE status = 1 AND DATE(created_at) = CURDATE()"
  )
  const todayImages = todayImagesResult[0]?.count || 0

  // 公开作品数
  const publicImagesResult = await db.query(
    'SELECT COUNT(*) as count FROM image WHERE status = 1 AND is_public = 1'
  )
  const publicImages = publicImagesResult[0]?.count || 0

  // 总浏览量
  const viewsResult = await db.query(
    'SELECT COALESCE(SUM(view_count), 0) as total FROM image WHERE status = 1'
  )
  const totalViews = viewsResult[0]?.total || 0

  // 总点赞数
  const likesResult = await db.query('SELECT COUNT(*) as count FROM likes')
  const totalLikes = likesResult[0]?.count || 0

  // 总收藏数
  const collectsResult = await db.query('SELECT COUNT(*) as count FROM collections')
  const totalCollects = collectsResult[0]?.count || 0

  // 总评论数
  const commentsResult = await db.query('SELECT COUNT(*) as count FROM comments')
  const totalComments = commentsResult[0]?.count || 0

  return {
    users: { total: totalUsers, today: todayUsers },
    images: { total: totalImages, today: todayImages, public: publicImages },
    interactions: {
      views: totalViews,
      likes: totalLikes,
      collects: totalCollects,
      comments: totalComments
    }
  }
}

module.exports = {
  getUsers,
  updateUserStatus,
  updateUserRole,
  getImages,
  deleteImage,
  getPlatformStats
}