import request from '@/utils/request'

// 获取公开作品列表
export function getPublicImages(params) {
  return request.get('/community/images', { params })
}

// 获取作品详情
export function getImageDetail(id) {
  return request.get(`/community/images/${id}`)
}

// 点赞/取消点赞
export function toggleLike(imageId) {
  return request.post('/community/like', { imageId })
}

// 收藏/取消收藏
export function toggleCollect(imageId) {
  return request.post('/community/collect', { imageId })
}

// 获取评论列表
export function getComments(imageId, params) {
  return request.get(`/community/images/${imageId}/comments`, { params })
}

// 发表评论
export function addComment(data) {
  return request.post('/community/comments', data)
}

// 删除评论
export function deleteComment(id) {
  return request.delete(`/community/comments/${id}`)
}

// 获取用户收藏列表
export function getUserCollections(params) {
  return request.get('/community/collections', { params })
}