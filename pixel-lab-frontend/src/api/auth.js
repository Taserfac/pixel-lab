/**
 * 【文件路径】src/api/auth.js
 * 【文件功能说明】用户认证相关 API
 * - 登录、注册、获取用户信息
 */

import { get, post } from '@/utils/request'

/**
 * 用户登录
 * @param {object} data - { username, password }
 * @returns {Promise}
 */
export const login = (data) => {
  return post('/api/auth/login', data)
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
