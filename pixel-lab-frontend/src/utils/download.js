/**
 * 【文件路径】src/utils/download.js
 * 【文件功能说明】文件下载工具函数
 * - 单文件下载
 * - 图片下载
 * - 批量打包下载（使用 JSZip）
 */

import JSZip from 'jszip'
import { saveAs } from 'file-saver'

/**
 * 文件下载
 * @param {string} url - 文件地址
 * @param {string} filename - 文件名
 */
export const downloadFile = (url, filename) => {
  const link = document.createElement('a')
  link.href = url
  link.download = filename || 'download'
  link.target = '_blank'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

/**
 * 下载 Blob 数据
 * @param {Blob} blob - Blob 数据
 * @param {string} filename - 文件名
 */
export const downloadBlob = (blob, filename) => {
  const url = window.URL.createObjectURL(blob)
  downloadFile(url, filename)
  window.URL.revokeObjectURL(url)
}

/**
 * 图片下载
 * @param {string} url - 图片地址
 * @param {string} filename - 文件名
 * @returns {Promise}
 */
export const downloadImage = async (url, filename) => {
  try {
    const response = await fetch(url)
    const blob = await response.blob()
    downloadBlob(blob, filename || 'image.png')
  } catch (error) {
    console.error('图片下载失败:', error)
    throw error
  }
}

/**
 * 批量打包下载
 * @param {Array} files - 文件列表 [{ name: string, url: string }]
 * @param {string} zipName - 压缩包名称
 * @returns {Promise}
 */
export const downloadBatch = async (files, zipName = 'download.zip') => {
  const zip = new JSZip()
  const folder = zip.folder('files')
  
  try {
    // 并行下载所有文件
    const promises = files.map(async (file) => {
      const response = await fetch(file.url)
      const blob = await response.blob()
      folder.file(file.name, blob)
    })
    
    await Promise.all(promises)
    
    // 生成压缩包
    const content = await zip.generateAsync({ type: 'blob' })
    saveAs(content, zipName)
  } catch (error) {
    console.error('批量下载失败:', error)
    throw error
  }
}

/**
 * Base64 图片下载
 * @param {string} base64 - Base64 数据
 * @param {string} filename - 文件名
 */
export const downloadBase64 = (base64, filename = 'image.png') => {
  const byteCharacters = atob(base64.split(',')[1])
  const byteNumbers = new Array(byteCharacters.length)
  
  for (let i = 0; i < byteCharacters.length; i++) {
    byteNumbers[i] = byteCharacters.charCodeAt(i)
  }
  
  const byteArray = new Uint8Array(byteNumbers)
  const blob = new Blob([byteArray], { type: 'image/png' })
  
  downloadBlob(blob, filename)
}
