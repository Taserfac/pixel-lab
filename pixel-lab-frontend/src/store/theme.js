/**
 * 【文件路径】src/store/theme.js
 * 【文件功能说明】主题状态管理 Store
 * - 管理亮色/暗色主题
 * - 主题切换持久化到 localStorage
 * - 自动同步 Element Plus 主题
 */

import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import * as storage from '@/utils/storage'

export const useThemeStore = defineStore('theme', () => {
  // ==================== State ====================
  
  // 当前主题：'light' | 'dark'
  const theme = ref(storage.getItem('theme', 'light'))
  
  // 是否跟随系统
  const followSystem = ref(storage.getItem('followSystem', false))

  const backgroundStyles = ['pixel-grid', 'aurora', 'star-canvas', 'spectrum']
  const backgroundMotions = ['on', 'reduced', 'off']

  // 动态背景偏好
  const backgroundStyle = ref(storage.getItem('backgroundStyle', 'pixel-grid'))
  const backgroundMotion = ref(storage.getItem('backgroundMotion', 'reduced'))
  
  // ==================== Actions ====================
  
  /**
   * 设置主题
   * @param {'light' | 'dark'} newTheme
   */
  const setTheme = (newTheme) => {
    const normalizedTheme = newTheme === 'light' ? 'light' : 'dark'
    theme.value = normalizedTheme
    storage.setItem('theme', normalizedTheme)
    applyTheme(normalizedTheme)
  }
  
  /**
   * 切换主题
   */
  const toggleTheme = () => {
    const newTheme = theme.value === 'light' ? 'dark' : 'light'
    setTheme(newTheme)
  }
  
  /**
   * 应用主题到 DOM
   * @param {'light' | 'dark'} themeValue
   */
  const applyTheme = (themeValue) => {
    const html = document.documentElement
    const normalizedTheme = themeValue === 'light' ? 'light' : 'dark'
    
    html.setAttribute('data-theme', normalizedTheme)

    if (normalizedTheme === 'dark') {
      html.classList.add('dark')
    } else {
      html.classList.remove('dark')
    }
  }
  
  /**
   * 设置是否跟随系统
   * @param {boolean} value
   */
  const setFollowSystem = (value) => {
    followSystem.value = value
    storage.setItem('followSystem', value)
    
    if (value) {
      // 监听系统主题变化
      const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
      const handleChange = (e) => {
        setTheme(e.matches ? 'dark' : 'light')
      }
      
      mediaQuery.addEventListener('change', handleChange)
      // 立即应用当前系统主题
      setTheme(mediaQuery.matches ? 'dark' : 'light')
    }
  }

  const setBackgroundStyle = (style) => {
    const normalizedStyle = backgroundStyles.includes(style) ? style : 'pixel-grid'
    backgroundStyle.value = normalizedStyle
    storage.setItem('backgroundStyle', normalizedStyle)
  }

  const setBackgroundMotion = (motion) => {
    const normalizedMotion = backgroundMotions.includes(motion) ? motion : 'reduced'
    backgroundMotion.value = normalizedMotion
    storage.setItem('backgroundMotion', normalizedMotion)
  }
  
  /**
   * 初始化主题
   */
  const init = () => {
    const savedTheme = storage.getItem('theme', 'light')
    const savedFollowSystem = storage.getItem('followSystem', false)
    const savedBackgroundStyle = storage.getItem('backgroundStyle', 'pixel-grid')
    const savedBackgroundMotion = storage.getItem('backgroundMotion', 'reduced')
    
    theme.value = savedTheme
    followSystem.value = savedFollowSystem
    backgroundStyle.value = backgroundStyles.includes(savedBackgroundStyle) ? savedBackgroundStyle : 'pixel-grid'
    backgroundMotion.value = backgroundMotions.includes(savedBackgroundMotion) ? savedBackgroundMotion : 'reduced'
    
    if (savedFollowSystem) {
      setFollowSystem(true)
    } else {
      applyTheme(savedTheme)
    }
  }
  
  // 监听主题变化，自动应用
  watch(theme, (newTheme) => {
    applyTheme(newTheme)
  })
  
  return {
    // State
    theme,
    followSystem,
    backgroundStyle,
    backgroundMotion,
    
    // Actions
    setTheme,
    toggleTheme,
    setFollowSystem,
    setBackgroundStyle,
    setBackgroundMotion,
    init
  }
})
