/**
 * 【文件路径】src/api/tag.js
 * 【功能说明】标签相关接口
 */

import request from '@/utils/request'

/**
 * 搜索标签（输入联想）
 * @param {string} keyword - 搜索关键词
 * @param {number} limit - 返回数量
 */
export function searchTags(keyword, limit = 20) {
  return request.get('/api/tags', { params: { keyword, limit } })
}

/**
 * 获取所有标签
 */
export function getAllTags() {
  return request.get('/api/tags/all')
}

/**
 * 获取热门标签（无关键词时）
 * @param {number} limit - 返回数量
 */
export function getHotTags(limit = 20) {
  return request.get('/api/tags', { params: { limit } })
}
