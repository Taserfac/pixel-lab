import request from '@/utils/request'

// 获取公开作品列表
export function getPublicImages(params, config = {}) {
  return request.get('/api/community/images', { params, ...config })
}

// Search public users
export function getPublicUsers(params, config = {}) {
  return request.get('/api/community/users', { params, ...config })
}
// 获取系统标签与达到展示门槛的热门标签
export function getPublicTags(params = {}, config = {}) {
  return request.get('/api/community/tags', { params, ...config })
}


// 获取作品详情
export function getImageDetail(id) {
  return request.get(`/api/community/images/${id}`)
}

// 获取创作者公开主页
export function getUserProfile(id, params) {
  return request.get(`/api/community/users/${id}`, { params })
}

// 点赞/取消点赞
export function toggleLike(imageId) {
  return request.post('/api/community/like', { imageId })
}

// 收藏/取消收藏
export function toggleCollect(imageId) {
  return request.post('/api/community/collect', { imageId })
}

// 举报作品
export function reportImage(data) {
  return request.post('/api/community/reports', data)
}

// 获取评论列表
export function getComments(imageId, params) {
  return request.get(`/api/community/images/${imageId}/comments`, { params })
}

// 发表评论
export function addComment(data) {
  return request.post('/api/community/comments', data)
}

// 删除评论
export function deleteComment(id) {
  return request.delete(`/api/community/comments/${id}`)
}

// 获取已关注创作者
export function getFollowingCreators() {
  return request.get('/api/community/following')
}

// 获取用户收藏列表
export function getUserCollections(params) {
  return request.get('/api/community/collections', { params })
}

// 获取用户点赞列表
export function getUserLikes(params) {
  return request.get('/api/community/likes', { params })
}

// 获取社区动态
export function getActivities(params, config = {}) {
  return request.get('/api/community/activities', { params, ...config })
}
