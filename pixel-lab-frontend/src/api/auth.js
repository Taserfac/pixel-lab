/**
 * 【文件路径】src/api/auth.js
 * 【文件功能说明】用户认证相关 API
 * - 登录、注册、获取用户信息
 */

import { get, post, patch } from '@/utils/request'

/**
 * 用户登录
 * @param {object} data - { username, password }
 * @returns {Promise}
 */
export const login = (data) => {
  return post('/api/auth/login', data)
}

/**
 * 用户退出登录
 * @returns {Promise}
 */
export const logout = () => {
  return post('/api/auth/logout')
}

/**
 * 用户注册
 * @param {object} data - { username, password }
 * @returns {Promise}
 */
export const register = (data) => {
  return post('/api/auth/register', data)
}

/**
 * 获取用户信息
 * @returns {Promise}
 */
export const getUserInfo = () => {
  return get('/api/auth/userinfo')
}

/**
 * 获取用户统计数据
 * @returns {Promise}
 */
export const getUserStats = () => {
  return get('/api/auth/stats')
}

/**
 * 更新用户资料
 * @param {object} data - { nickname, avatar }
 * @returns {Promise}
 */
export const updateProfile = (data) => {
  return patch('/api/auth/profile', data)
}

/**
 * 修改密码
 * @param {object} data - { oldPassword, newPassword }
 * @returns {Promise}
 */
export const changePassword = (data) => {
  return patch('/api/auth/password', data)
}
