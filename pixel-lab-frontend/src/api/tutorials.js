import request from '@/utils/request'

export function getTutorialVideos(params, config = {}) {
  return request.get('/api/tutorials/videos', { params, ...config })
}

export function getTutorialCategories(config = {}) {
  return request.get('/api/tutorials/categories', config)
}
