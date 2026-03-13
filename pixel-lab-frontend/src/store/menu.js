/**
 * 【文件路径】src/store/menu.js
 * 【文件功能说明】菜单状态管理 Store
 * - 管理侧边栏折叠状态
 */

import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useMenuStore = defineStore('menu', () => {
  const collapsed = ref(false)
  
  const toggleCollapse = () => {
    collapsed.value = !collapsed.value
  }
  
  return {
    collapsed,
    toggleCollapse
  }
})
