/**
 * 【文件路径】src/utils/storage.js
 * 【文件功能说明】localStorage 封装工具
 * - 提供 set、get、remove、clear 方法
 * - 支持设置过期时间
 * - 自动处理 JSON 序列化/反序列化
 */

const STORAGE_PREFIX = 'pixel_lab_'

/**
 * 设置存储项
 * @param {string} key - 键名
 * @param {*} value - 值
 * @param {number} expire - 过期时间（毫秒），可选
 */
export const setItem = (key, value, expire = null) => {
  const data = {
    value,
    expire: expire ? Date.now() + expire : null
  }
  localStorage.setItem(STORAGE_PREFIX + key, JSON.stringify(data))
}

/**
 * 获取存储项
 * @param {string} key - 键名
 * @param {*} defaultValue - 默认值
 * @returns {*} 存储的值或默认值
 */
export const getItem = (key, defaultValue = null) => {
  const item = localStorage.getItem(STORAGE_PREFIX + key)
  
  if (!item) {
    return defaultValue
  }
  
  try {
    const data = JSON.parse(item)
    
    // 检查是否过期
    if (data.expire && Date.now() > data.expire) {
      removeItem(key)
      return defaultValue
    }
    
    return data.value
  } catch {
    return defaultValue
  }
}

/**
 * 移除存储项
 * @param {string} key - 键名
 */
export const removeItem = (key) => {
  localStorage.removeItem(STORAGE_PREFIX + key)
}

/**
 * 清空所有存储项（仅清除本项目前缀的）
 */
export const clear = () => {
  const keys = Object.keys(localStorage)
  keys.forEach(key => {
    if (key.startsWith(STORAGE_PREFIX)) {
      localStorage.removeItem(key)
    }
  })
}

/**
 * 获取所有存储项
 * @returns {object}
 */
export const getAll = () => {
  const result = {}
  const keys = Object.keys(localStorage)
  
  keys.forEach(key => {
    if (key.startsWith(STORAGE_PREFIX)) {
      const originalKey = key.replace(STORAGE_PREFIX, '')
      result[originalKey] = getItem(originalKey)
    }
  })
  
  return result
}
