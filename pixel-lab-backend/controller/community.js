/**
 * 【文件路径】controller/community.js
 * 【功能说明】社区控制器
 */

const communityService = require('../service/community')
const { success, error } = require('../utils/result')

/**
 * 获取公开作品列表
 */
async function getPublicImages(req, res) {
  const { page = 1, pageSize = 20, keyword = '', sortBy = 'latest', category = '' } = req.query
  const userId = req.user?.id

  const result = await communityService.getPublicImages({
    page: parseInt(page),
    pageSize: parseInt(pageSize),
    keyword,
    sortBy,
    category,
    userId
  })

  success(res, result)
}

/**
 * 获取作品详情
 */
async function getImageDetail(req, res) {
  const { id } = req.params
  const userId = req.user?.id

  const image = await communityService.getImageDetail(parseInt(id), userId)

  if (!image) {
    return error(res, '作品不存在', 404)
  }

  success(res, image)
}

/**
 * 点赞/取消点赞
 */
async function toggleLike(req, res) {
  const { imageId } = req.body
  const userId = req.user.id

  if (!imageId) {
    return error(res, '缺少作品ID', 400)
  }

  const result = await communityService.toggleLike(userId, parseInt(imageId))
  success(res, result)
}

/**
 * 收藏/取消收藏
 */
async function toggleCollect(req, res) {
  const { imageId } = req.body
  const userId = req.user.id

  if (!imageId) {
    return error(res, '缺少作品ID', 400)
  }

  const result = await communityService.toggleCollect(userId, parseInt(imageId))
  success(res, result)
}

/**
 * 获取评论列表
 */
async function getComments(req, res) {
  const { imageId } = req.params
  const { page = 1, pageSize = 20 } = req.query

  const result = await communityService.getComments(parseInt(imageId), {
    page: parseInt(page),
    pageSize: parseInt(pageSize)
  })

  success(res, result)
}

/**
 * 发表评论
 */
async function addComment(req, res) {
  const { imageId, content, parentId } = req.body
  const userId = req.user.id

  if (!imageId || !content) {
    return error(res, '缺少必要参数', 400)
  }

  if (content.length > 500) {
    return error(res, '评论内容不能超过500字', 400)
  }

  const commentId = await communityService.addComment(
    userId,
    parseInt(imageId),
    content,
    parentId ? parseInt(parentId) : null
  )

  success(res, { id: commentId })
}

/**
 * 删除评论
 */
async function deleteComment(req, res) {
  const { id } = req.params
  const userId = req.user.id

  const result = await communityService.deleteComment(userId, parseInt(id))

  if (!result) {
    return error(res, '评论不存在或无权删除', 404)
  }

  success(res, null)
}

/**
 * 获取用户收藏列表
 */
async function getUserCollections(req, res) {
  const userId = req.user.id
  const { page = 1, pageSize = 20 } = req.query

  const result = await communityService.getUserCollections(userId, {
    page: parseInt(page),
    pageSize: parseInt(pageSize)
  })

  success(res, result)
}

/**
 * 获取用户点赞列表
 */
async function getUserLikes(req, res) {
  const userId = req.user.id
  const { page = 1, pageSize = 20 } = req.query

  const result = await communityService.getUserLikes(userId, {
    page: parseInt(page),
    pageSize: parseInt(pageSize)
  })

  success(res, result)
}

/**
 * 获取社区动态
 */
async function getActivities(req, res) {
  const { limit = 10 } = req.query

  const activities = await communityService.getRecentActivities({
    limit: parseInt(limit)
  })

  success(res, activities)
}

module.exports = {
  getPublicImages,
  getImageDetail,
  toggleLike,
  toggleCollect,
  getComments,
  addComment,
  deleteComment,
  getUserCollections,
  getUserLikes,
  getActivities
}
