/**
 * 【文件路径】src/store/user.js
 * 【文件功能说明】用户状态管理 Store
 * - 管理用户信息、token、登录状态
 * - 提供登录、登出、获取用户信息等方法
 * - 数据持久化到 localStorage
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as storage from '@/utils/storage'
import { getUserInfo } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  // ==================== State ====================
  
  // 用户信息
  const userInfo = ref(storage.getItem('userInfo', null))
  
  // Token
  const token = ref(storage.getItem('token', ''))
  
  // 登录状态
  const isLoggedIn = computed(() => !!token.value)
  
  // 是否管理员
  const isAdmin = computed(() => userInfo.value?.role === 'admin')
  
  // ==================== Actions ====================
  
  /**
   * 设置 token
   * @param {string} newToken
   */
  const setToken = (newToken) => {
    token.value = newToken
    storage.setItem('token', newToken)
  }
  
  /**
   * 设置用户信息
   * @param {object} info
   */
  const setUserInfo = (info) => {
    userInfo.value = info
    storage.setItem('userInfo', info)
  }
  
  /**
   * 登录成功后的处理
   * @param {object} data - 登录接口返回的数据
   */
  const login = (data) => {
    setToken(data.token)
    setUserInfo(data.user)
  }
  
  /**
   * 登出
   */
  const logout = () => {
    token.value = ''
    userInfo.value = null
    storage.removeItem('token')
    storage.removeItem('userInfo')
  }
  
  /**
   * 获取用户信息
   * @returns {Promise}
   */
  const fetchUserInfo = async () => {
    try {
      const data = await getUserInfo()
      setUserInfo(data)
      return data
    } catch (error) {
      // 获取失败，清除登录状态
      logout()
      throw error
    }
  }
  
  /**
   * 更新用户信息
   * @param {object} data
   */
  const updateUserInfo = (data) => {
    userInfo.value = { ...userInfo.value, ...data }
    storage.setItem('userInfo', userInfo.value)
  }
  
  /**
   * 初始化（页面刷新时调用）
   */
  const init = () => {
    const savedToken = storage.getItem('token', '')
    const savedUserInfo = storage.getItem('userInfo', null)
    
    if (savedToken) {
      token.value = savedToken
    }
    if (savedUserInfo) {
      userInfo.value = savedUserInfo
    }
  }
  
  return {
    // State
    userInfo,
    token,
    isLoggedIn,
    isAdmin,
    
    // Actions
    setToken,
    setUserInfo,
    login,
    logout,
    fetchUserInfo,
    updateUserInfo,
    init
  }
})
