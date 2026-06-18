import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import router from '@/router'
import * as storage from '@/utils/storage'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 10000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json'
  }
})

request.interceptors.request.use(
  (config) => {
    const token = storage.getItem('token', '')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response) => {
    const { data } = response

    if (data.code >= 200 && data.code < 300) {
      return data.data
    }

    ElMessage.error(data.msg || '请求失败')
    return Promise.reject(new Error(data.msg || '请求失败'))
  },
  (error) => {
    const { response } = error

    if (response) {
      const { status, data } = response
      const serverMessage = data?.msg
      const requestUrl = response.config?.url || ''
      const isLoginRequest = requestUrl.includes('/api/auth/login')

      if (status === 401) {
        if (isLoginRequest) {
          ElMessage.error(serverMessage || '用户名或密码错误，请重新输入')
          return Promise.reject(error)
        }

        ElMessage.error('登录已过期，请重新登录')
        const userStore = useUserStore()
        userStore.logout()
        router.push('/login')
      } else if (status === 403) {
        ElMessage.error(serverMessage || '没有权限访问该资源')
      } else if (status === 404) {
        ElMessage.error(serverMessage || '请求的资源不存在')
      } else if (status === 500 && !serverMessage) {
        ElMessage.error('服务器内部错误')
      } else {
        ElMessage.error(serverMessage || `请求失败 (${status})`)
      }
    } else {
      ElMessage.error('网络连接失败，请检查后端是否已启动')
    }

    return Promise.reject(error)
  }
)

export const get = (url, params = {}) => request.get(url, { params })

export const post = (url, data = {}) => request.post(url, data)

export const put = (url, data = {}) => request.put(url, data)

export const del = (url, params = {}) => request.delete(url, { params })

export const patch = (url, data = {}) => request.patch(url, data)

export default request
