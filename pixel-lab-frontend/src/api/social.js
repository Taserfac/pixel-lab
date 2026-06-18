import request from '@/utils/request'

// ==================== Follow ====================

// 关注用户
export function followUser(userId) {
  return request.post('/api/social/follow', { userId })
}

// 取消关注
export function unfollowUser(userId) {
  return request.delete(`/api/social/follow/${userId}`)
}

// 获取关注列表
export function getFollowing(params) {
  return request.get('/api/social/following', { params })
}

// 获取粉丝列表
export function getFollowers(params) {
  return request.get('/api/social/followers', { params })
}

// 检查关注状态
export function checkFollowStatus(userId) {
  return request.get(`/api/social/follow/status/${userId}`)
}

// ==================== Notifications ====================

// 获取通知列表
export function getNotifications(params, config = {}) {
  return request.get('/api/social/notifications', { ...config, params })
}

// 标记通知已读
export function markNotificationRead(id) {
  return request.patch(`/api/social/notifications/${id}/read`)
}

// 全部标记已读
export function markAllRead() {
  return request.post('/api/social/notifications/read-all')
}

// 获取未读数量
export function getUnreadCount(config = {}) {
  return request.get('/api/social/notifications/unread-count', config)
}

// ==================== Rankings ====================

// 获取排行榜
export function getRankings(params) {
  return request.get('/api/social/rankings', { params })
}

// ==================== Similar Works ====================

// 获取相似作品
export function getSimilarWorks(imageId, params) {
  return request.get(`/api/community/images/${imageId}/similar`, { params })
}

// ==================== Collections ====================

// 获取收藏夹列表
export function getCollections(params) {
  return request.get('/api/social/collections', { params })
}

// 获取收藏夹详情
export function getCollectionDetail(id) {
  return request.get(`/api/social/collections/${id}`)
}

// 创建收藏夹
export function createCollection(data) {
  return request.post('/api/social/collections', data)
}

// ==================== Templates ====================

// 获取模板列表
export function getTemplates() {
  return request.get('/api/templates')
}
