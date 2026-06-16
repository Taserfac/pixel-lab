/**
 * 【文件路径】src/api/image.js
 * 【功能说明】图片相关接口
 */

import request from '@/utils/request'

/**
 * 上传图片
 * @param {File} file - 图片文件
 */
export const uploadImage = (file, options = {}) => {
  const formData = new FormData()
  formData.append('image', file)
  if (options.category) {
    formData.append('category', options.category)
  }
  if (options.tags) {
    formData.append('tags', options.tags)
  }
  
  return request.post('/api/images/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 获取用户图片列表
 * @param {Object} params - 查询参数
 */
export const getUserImages = (params = {}) => {
  return request.get('/api/images', { params })
}

/**
 * 删除图片
 * @param {number} id - 图片ID
 */
export const deleteImage = (id) => {
  return request.delete(`/api/images/${id}`)
}

/**
 * 更新图片可见性
 * @param {number} id - 图片ID
 * @param {boolean} isPublic - 是否公开
 */
export const updateImageVisibility = (id, isPublic) => {
  return request.post(`/api/images/${id}/visibility`, { isPublic })
}
