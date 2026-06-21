/**
 * 【文件路径】src/store/tag.js
 * 【功能说明】标签状态管理 Store
 * - 缓存全量标签列表
 * - 提供搜索和获取标签的方法
 */

import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getAllTags, searchTags, getHotTags } from '@/api/tag'

export const useTagStore = defineStore('tag', () => {
  // ==================== State ====================

  // 全量标签缓存
  const allTags = ref([])
  // 热门标签
  const hotTags = ref([])
  // 是否已加载
  const loaded = ref(false)

  // ==================== Actions ====================

  /**
   * 获取并缓存所有标签
   */
  const fetchAllTags = async () => {
    if (loaded.value && allTags.value.length > 0) {
      return allTags.value
    }
    try {
      const data = await getAllTags()
      allTags.value = data || []
      loaded.value = true
      return allTags.value
    } catch (error) {
      console.error('获取标签列表失败:', error)
      return []
    }
  }

  /**
   * 获取热门标签
   */
  const fetchHotTags = async (limit = 10) => {
    try {
      const data = await getHotTags(limit)
      hotTags.value = data || []
      return hotTags.value
    } catch (error) {
      console.error('获取热门标签失败:', error)
      return []
    }
  }

  /**
   * 搜索标签（优先本地过滤，回退 API）
   */
  const search = async (keyword, limit = 20) => {
    if (!keyword || !keyword.trim()) {
      // 无关键词时返回热门标签
      if (allTags.value.length > 0) {
        return allTags.value.slice(0, limit)
      }
      return await fetchHotTags(limit)
    }
    const kw = keyword.trim().toLowerCase()
    // 优先本地过滤
    if (allTags.value.length > 0) {
      const filtered = allTags.value.filter(t =>
        t.name.toLowerCase().includes(kw)
      )
      // 按热度排序
      filtered.sort((a, b) => (b.usage_count || 0) - (a.usage_count || 0))
      return filtered.slice(0, limit)
    }
    // 回退 API
    try {
      return await searchTags(keyword, limit)
    } catch (error) {
      console.error('搜索标签失败:', error)
      return []
    }
  }

  /**
   * 清除缓存
   */
  const clearCache = () => {
    allTags.value = []
    hotTags.value = []
    loaded.value = false
  }

  return {
    allTags,
    hotTags,
    loaded,
    fetchAllTags,
    fetchHotTags,
    search,
    clearCache
  }
})
