/**
 * 【文件路径】src/main.js
 * 【文件功能说明】前端项目入口文件
 * - 创建 Vue 应用实例
 * - 注册全局插件（Element Plus、Vue Router、Pinia、ECharts）
 * - 挂载应用到 DOM
 */

import { createApp } from 'vue'
import App from './App.vue'

// Vue Router
import router from './router'

// Pinia 状态管理
import { createPinia } from 'pinia'

// Element Plus
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// 全局样式
import './assets/css/index.css'

// ECharts
import * as echarts from 'echarts'

// 用户 Store
import { useUserStore } from './store/user'
import { useThemeStore } from './store/theme'

const app = createApp(App)

// 注册 Pinia
app.use(createPinia())

// 初始化用户状态（恢复登录状态）
const userStore = useUserStore()
userStore.init()

const themeStore = useThemeStore()
themeStore.init()

// 注册 Vue Router
app.use(router)

// 注册 Element Plus，配置中文语言包
app.use(ElementPlus, {
  locale: zhCn,
  size: 'default',
  zIndex: 3000
})

// 注册所有 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 全局挂载 ECharts
app.config.globalProperties.$echarts = echarts

// 挂载应用
app.mount('#app')
