/**
 * 【文件路径】src/store/user.js
 * 【文件功能说明】用户状态管理 Store
 * - 管理用户信息、Session 登录状态
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
  
  // 保留 token 字段用于兼容旧代码，但新后端使用 HttpSession + JSESSIONID
  const token = ref('')
  const sessionChecked = ref(false)
  
  // 登录状态
  const isLoggedIn = computed(() => !!userInfo.value)
  
  // 是否管理员
  const isAdmin = computed(() => userInfo.value?.role === 'admin')
  
  // ==================== Actions ====================
  
  const setToken = (newToken) => {
    token.value = newToken || ''
    storage.removeItem('token')
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
    setToken('')
    setUserInfo(data.user)
    sessionChecked.value = true
  }
  
  /**
   * 登出
   */
  const logout = () => {
    token.value = ''
    userInfo.value = null
    storage.removeItem('token')
    storage.removeItem('userInfo')
    sessionChecked.value = true
  }
  
  /**
   * 获取用户信息
   * @returns {Promise}
   */
  const fetchUserInfo = async () => {
    try {
      const data = await getUserInfo()
      setUserInfo(data)
      sessionChecked.value = true
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
    const savedUserInfo = storage.getItem('userInfo', null)
    if (savedUserInfo) {
      userInfo.value = savedUserInfo
    }
    storage.removeItem('token')
  }
  
  return {
    // State
    userInfo,
    token,
    sessionChecked,
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
