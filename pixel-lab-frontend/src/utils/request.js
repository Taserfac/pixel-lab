/**
 * 【文件路径】src/utils/request.js
 * 【文件功能说明】Axios 请求封装
 * - 统一配置基础地址、超时时间
 * - 请求拦截器：统一携带 Cookie 会话
 * - 响应拦截器：统一处理响应数据、错误处理
 * - 封装 get、post、put、delete 方法
 */

import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import router from '@/router'

// 创建 axios 实例
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 10000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const { data } = response

    // 如果响应成功，直接返回数据 (200-299 都是成功)
    if (data.code >= 200 && data.code < 300) {
      return data.data
    }

    // 业务错误，显示错误信息
    ElMessage.error(data.msg || '请求失败')
    return Promise.reject(new Error(data.msg || '请求失败'))
  },
  (error) => {
    const { response } = error

    if (response) {
      const { status, data } = response
      const serverMessage = data?.msg

      if (status === 401) {
        // Session 失效，清除本地用户信息并跳转到登录页
        ElMessage.error('登录已过期，请重新登录')
        const userStore = useUserStore()
        userStore.logout()
        router.push('/login')
      } else if (status === 403) {
        ElMessage.error('没有权限访问该资源')
      } else if (status === 404) {
        ElMessage.error('请求的资源不存在')
      } else if (status === 500 && !serverMessage) {
        ElMessage.error('服务器内部错误')
      } else {
        ElMessage.error(serverMessage || `请求失败 (${status})`)
      }
    } else {
      // 网络错误
      ElMessage.error('网络连接失败，请检查网络设置')
    }

    return Promise.reject(error)
  }
)

/**
 * GET 请求
 * @param {string} url - 请求地址
 * @param {object} params - 请求参数
 * @returns {Promise}
 */
export const get = (url, params = {}) => {
  return request.get(url, { params })
}

/**
 * POST 请求
 * @param {string} url - 请求地址
 * @param {object} data - 请求体数据
 * @returns {Promise}
 */
export const post = (url, data = {}) => {
  return request.post(url, data)
}

/**
 * PUT 请求
 * @param {string} url - 请求地址
 * @param {object} data - 请求体数据
 * @returns {Promise}
 */
export const put = (url, data = {}) => {
  return request.put(url, data)
}

/**
 * DELETE 请求
 * @param {string} url - 请求地址
 * @param {object} params - 请求参数
 * @returns {Promise}
 */
export const del = (url, params = {}) => {
  return request.delete(url, { params })
}

/**
 * PATCH 请求
 * @param {string} url - 请求地址
 * @param {object} data - 请求体数据
 * @returns {Promise}
 */
export const patch = (url, data = {}) => {
  return request.patch(url, data)
}

export default request
