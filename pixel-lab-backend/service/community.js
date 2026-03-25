/**
 * 【文件路径】service/community.js
 * 【功能说明】社区服务层
 */

const db = require('../db')

/**
 * 获取公开作品列表
 */
async function getPublicImages({ page = 1, pageSize = 20, keyword = '', sortBy = 'latest' }) {
  const offset = (page - 1) * pageSize
  let whereClause = 'WHERE i.is_public = 1 AND i.status = 1'
  const params = []

  if (keyword) {
    whereClause += ' AND (i.title LIKE ? OR i.tags LIKE ? OR u.nickname LIKE ?)'
    const likeKeyword = `%${keyword}%`
    params.push(likeKeyword, likeKeyword, likeKeyword)
  }

  let orderBy = 'i.created_at DESC'
  if (sortBy === 'popular') {
    orderBy = 'i.like_count DESC, i.view_count DESC'
  } else if (sortBy === 'views') {
    orderBy = 'i.view_count DESC'
  }

  const countResult = await db.query(
    `SELECT COUNT(*) as total FROM image i LEFT JOIN user u ON i.user_id = u.id ${whereClause}`,
    params
  )
  const total = countResult[0]?.total || 0

  const rows = await db.query(
    `SELECT i.*, u.nickname as author_name, u.avatar as author_avatar
     FROM image i
     LEFT JOIN user u ON i.user_id = u.id
     ${whereClause}
     ORDER BY ${orderBy}
     LIMIT ? OFFSET ?`,
    [...params, pageSize, offset]
  )

  return {
    list: rows,
    total,
    page,
    pageSize,
    totalPages: Math.ceil(total / pageSize)
  }
}

/**
 * 获取作品详情
 */
async function getImageDetail(imageId, userId = null) {
  const rows = await db.query(
    `SELECT i.*, u.nickname as author_name, u.avatar as author_avatar
     FROM image i
     LEFT JOIN user u ON i.user_id = u.id
     WHERE i.id = ?`,
    [imageId]
  )

  if (!rows || rows.length === 0) {
    return null
  }

  const image = rows[0]

  await db.query(
    'UPDATE image SET view_count = view_count + 1 WHERE id = ?',
    [imageId]
  )
  image.view_count = (image.view_count || 0) + 1

  if (userId) {
    const likeRows = await db.query(
      'SELECT id FROM likes WHERE user_id = ? AND image_id = ?',
      [userId, imageId]
    )
    image.isLiked = likeRows && likeRows.length > 0

    const collectRows = await db.query(
      'SELECT id FROM collections WHERE user_id = ? AND image_id = ?',
      [userId, imageId]
    )
    image.isCollected = collectRows && collectRows.length > 0
  } else {
    image.isLiked = false
    image.isCollected = false
  }

  return image
}

/**
 * 点赞/取消点赞
 */
async function toggleLike(userId, imageId) {
  const existing = await db.query(
    'SELECT id FROM likes WHERE user_id = ? AND image_id = ?',
    [userId, imageId]
  )

  if (existing && existing.length > 0) {
    await db.query('DELETE FROM likes WHERE user_id = ? AND image_id = ?', [userId, imageId])
    await db.query('UPDATE image SET like_count = GREATEST(0, like_count - 1) WHERE id = ?', [imageId])
    return { liked: false }
  } else {
    await db.query('INSERT INTO likes (user_id, image_id) VALUES (?, ?)', [userId, imageId])
    await db.query('UPDATE image SET like_count = like_count + 1 WHERE id = ?', [imageId])
    return { liked: true }
  }
}

/**
 * 收藏/取消收藏
 */
async function toggleCollect(userId, imageId) {
  const existing = await db.query(
    'SELECT id FROM collections WHERE user_id = ? AND image_id = ?',
    [userId, imageId]
  )

  if (existing && existing.length > 0) {
    await db.query('DELETE FROM collections WHERE user_id = ? AND image_id = ?', [userId, imageId])
    await db.query('UPDATE image SET collect_count = GREATEST(0, collect_count - 1) WHERE id = ?', [imageId])
    return { collected: false }
  } else {
    await db.query('INSERT INTO collections (user_id, image_id) VALUES (?, ?)', [userId, imageId])
    await db.query('UPDATE image SET collect_count = collect_count + 1 WHERE id = ?', [imageId])
    return { collected: true }
  }
}

/**
 * 获取评论列表
 */
async function getComments(imageId, { page = 1, pageSize = 20 }) {
  const offset = (page - 1) * pageSize

  const countResult = await db.query(
    'SELECT COUNT(*) as total FROM comments WHERE image_id = ?',
    [imageId]
  )
  const total = countResult[0]?.total || 0

  const rows = await db.query(
    `SELECT c.*, u.nickname, u.avatar
     FROM comments c
     LEFT JOIN user u ON c.user_id = u.id
     WHERE c.image_id = ?
     ORDER BY c.created_at DESC
     LIMIT ? OFFSET ?`,
    [imageId, pageSize, offset]
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
 * 发表评论
 */
async function addComment(userId, imageId, content, parentId = null) {
  const result = await db.insert(
    'INSERT INTO comments (user_id, image_id, content, parent_id) VALUES (?, ?, ?, ?)',
    [userId, imageId, content, parentId]
  )

  await db.query('UPDATE image SET comment_count = comment_count + 1 WHERE id = ?', [imageId])

  return result
}

/**
 * 删除评论
 */
async function deleteComment(userId, commentId) {
  const rows = await db.query(
    'SELECT * FROM comments WHERE id = ? AND user_id = ?',
    [commentId, userId]
  )

  if (!rows || rows.length === 0) {
    return false
  }

  const comment = rows[0]

  await db.query('DELETE FROM comments WHERE id = ?', [commentId])
  await db.query('UPDATE image SET comment_count = GREATEST(0, comment_count - 1) WHERE id = ?', [comment.image_id])

  return true
}

/**
 * 获取用户收藏列表
 */
async function getUserCollections(userId, { page = 1, pageSize = 20 }) {
  const offset = (page - 1) * pageSize

  const countResult = await db.query(
    'SELECT COUNT(*) as total FROM collections WHERE user_id = ?',
    [userId]
  )
  const total = countResult[0]?.total || 0

  const rows = await db.query(
    `SELECT i.*, u.nickname as author_name, c.created_at as collected_at
     FROM collections c
     LEFT JOIN image i ON c.image_id = i.id
     LEFT JOIN user u ON i.user_id = u.id
     WHERE c.user_id = ?
     ORDER BY c.created_at DESC
     LIMIT ? OFFSET ?`,
    [userId, pageSize, offset]
  )

  return {
    list: rows || [],
    total,
    page,
    pageSize,
    totalPages: Math.ceil(total / pageSize)
  }
}

module.exports = {
  getPublicImages,
  getImageDetail,
  toggleLike,
  toggleCollect,
  getComments,
  addComment,
  deleteComment,
  getUserCollections
}