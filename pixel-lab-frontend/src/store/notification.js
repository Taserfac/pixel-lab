/**
 * 【文件路径】src/store/notification.js
 * 【文件功能说明】通知状态管理 Store
 * - 管理通知列表、未读数量
 * - 提供获取通知、标记已读、实时添加通知等方法
 */

import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  getNotifications as fetchNotificationsApi,
  getUnreadCount as fetchUnreadCountApi,
  markNotificationRead as markReadApi,
  markAllRead as markAllReadApi
} from '@/api/social'
import { useUserStore } from '@/store/user'

export const useNotificationStore = defineStore('notification', () => {
  // ==================== State ====================

  const notifications = ref([])
  const unreadCount = ref(0)
  const loading = ref(false)

  // ==================== Actions ====================

  /**
   * 获取通知列表
   * @param {object} params - { page, pageSize }
   */
  const fetchNotifications = async (params) => {
    const userStore = useUserStore()
    if (!userStore.isLoggedIn) return

    loading.value = true
    try {
      const data = await fetchNotificationsApi(params)
      notifications.value = data.list || data.records || data || []
      return data
    } catch (error) {
      console.error('获取通知列表失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  /**
   * 获取未读数量
   */
  const fetchUnreadCount = async () => {
    const userStore = useUserStore()
    if (!userStore.isLoggedIn) return

    try {
      const data = await fetchUnreadCountApi()
      unreadCount.value = typeof data === 'number' ? data : (data.count || 0)
      return data
    } catch (error) {
      console.error('获取未读数量失败:', error)
      throw error
    }
  }

  /**
   * 标记单条通知已读
   * @param {string|number} id
   */
  const markRead = async (id) => {
    try {
      await markReadApi(id)

      const notification = notifications.value.find(n => n.id === id)
      if (notification && !notification.read) {
        notification.read = true
        if (unreadCount.value > 0) {
          unreadCount.value--
        }
      }
    } catch (error) {
      console.error('标记通知已读失败:', error)
      throw error
    }
  }

  /**
   * 全部标记已读
   */
  const markAllAsRead = async () => {
    try {
      await markAllReadApi()

      notifications.value.forEach(n => {
        n.read = true
      })
      unreadCount.value = 0
    } catch (error) {
      console.error('全部标记已读失败:', error)
      throw error
    }
  }

  /**
   * 实时添加通知（WebSocket 等场景调用）
   * @param {object} notification
   */
  const addNotification = (notification) => {
    notifications.value.unshift(notification)
    if (!notification.read) {
      unreadCount.value++
    }
  }

  return {
    // State
    notifications,
    unreadCount,
    loading,

    // Actions
    fetchNotifications,
    fetchUnreadCount,
    markRead,
    markAllRead: markAllAsRead,
    addNotification
  }
})
