/**
 * 【文件路径】src/i18n/index.js
 * 【文件功能说明】Vue I18n 国际化配置
 * - 创建 i18n 实例
 * - 配置默认语言为中文
 * - 导入中英文语言包
 */

import { createI18n } from 'vue-i18n'
import zhCN from './locales/zh-CN'
import enUS from './locales/en-US'

// 创建 i18n 实例
const i18n = createI18n({
  // 使用 Vue 3 Composition API
  legacy: false,
  // 默认语言为中文
  locale: localStorage.getItem('locale') || 'zh-CN',
  // 回退语言
  fallbackLocale: 'en-US',
  // 语言包
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS
  },
  // 开发模式下启用警告
  warnHtmlInMessage: import.meta.env.DEV ? 'warn' : 'off'
})

export default i18n
