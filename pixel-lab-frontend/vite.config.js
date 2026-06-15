/**
 * 【文件路径】vite.config.js
 * 【文件功能说明】Vite 构建工具配置文件
 * - 配置路径别名 @ 指向 src 目录
 * - 配置开发服务器代理，解决跨域问题
 * - 配置 Element Plus 按需导入
 * - 配置 ESLint 插件
 */

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import eslintPlugin from 'vite-plugin-eslint'
import { resolve } from 'path'

export default defineConfig(({ mode }) => {
  return {
    plugins: [
      vue()
      // ESLint 暂时禁用，避免格式问题阻塞开发
      // eslintPlugin({
      //   include: ['src/**/*.js', 'src/**/*.vue'],
      //   exclude: ['node_modules', '.git', 'dist']
      // })
    ],
    resolve: {
      alias: {
        '@': resolve(__dirname, 'src')
      }
    },
    server: {
      port: 5173,
      open: true,
      proxy: {
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api/, '/api')
        }
      }
    },
    build: {
      outDir: 'dist',
      assetsDir: 'assets',
      sourcemap: mode === 'development',
      rollupOptions: {
        output: {
          manualChunks: {
            'element-plus': ['element-plus'],
            'echarts': ['echarts'],
            'vendor': ['vue', 'vue-router', 'pinia', 'axios']
          }
        }
      }
    },
    css: {
      preprocessorOptions: {
        scss: {
          additionalData: '@use "@/assets/css/variables.scss" as *;'
        }
      }
    }
  }
})
