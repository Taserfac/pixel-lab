/**
 * 【文件路径】src/api/admin.js
 * 【功能说明】后台管理 API
 */

import { get, post, del, patch } from '@/utils/request'

// ========== 用户管理 ==========

/**
 * 获取用户列表
 */
export const getUsers = (params) => {
  return get('/api/admin/users', params)
}

/**
 * 更新用户状态
 */
export const updateUserStatus = (userId, status) => {
  return patch(`/api/admin/users/${userId}/status`, { status })
}

/**
 * 更新用户角色
 */
export const updateUserRole = (userId, role) => {
  return patch(`/api/admin/users/${userId}/role`, { role })
}

// ========== 作品管理 ==========

/**
 * 获取作品列表
 */
export const getImages = (params) => {
  return get('/api/admin/images', params)
}

/**
 * 删除作品
 */
export const deleteImage = (imageId) => {
  return del(`/api/admin/images/${imageId}`)
}

// ========== 平台统计 ==========

/**
 * 获取平台统计数据
 */
export const getStats = () => {
  return get('/api/admin/stats')
}