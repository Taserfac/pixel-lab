/**
 * 【文件路径】src/router/index.js
 * 【文件功能说明】Vue Router 配置
 * - 定义所有路由
 * - 配置路由守卫（登录校验、权限校验）
 * - 路由懒加载
 */

import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'

// ==================== 路由配置 ====================

const routes = [
  // 公开路由
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/Register.vue'),
    meta: { public: true, title: '注册' }
  },
  
  // 主布局路由（需要登录）
  {
    path: '/',
    component: () => import('@/components/common/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      // 数据统计（首页）
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/Index.vue'),
        meta: { title: '首页', icon: 'HomeFilled' }
      },
      // 图像处理工作台
      {
        path: 'workbench',
        name: 'Workbench',
        component: () => import('@/views/workbench/Index.vue'),
        meta: { title: '工作台', icon: 'Picture' }
      },
      // 手绘板
      {
        path: 'draw',
        name: 'Draw',
        component: () => import('@/views/draw/Index.vue'),
        meta: { title: '手绘板', icon: 'EditPen' }
      },
      // 社区广场
      {
        path: 'community',
        name: 'Community',
        component: () => import('@/views/community/Index.vue'),
        meta: { title: '社区广场', icon: 'ChatDotRound' }
      },
      // 个人中心
      {
        path: 'personal',
        name: 'Personal',
        component: () => import('@/views/personal/Index.vue'),
        meta: { title: '个人中心', icon: 'User' }
      },
      // 设置
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/settings/Index.vue'),
        meta: { title: '设置', icon: 'Setting', hideInMenu: true }
      },
      // 数据统计
      {
        path: 'stats',
        name: 'Stats',
        component: () => import('@/views/stats/Index.vue'),
        meta: { title: '数据统计', icon: 'TrendCharts' }
      },
      // 排行榜
      {
        path: 'ranking',
        name: 'Ranking',
        component: () => import('@/views/ranking/Index.vue'),
        meta: { title: '排行榜', icon: 'Trophy' }
      },
      // 后台管理（仅管理员）
      {
        path: 'admin',
        name: 'Admin',
        component: () => import('@/views/admin/Index.vue'),
        meta: { title: '后台管理', icon: 'Setting', admin: true }
      }
    ]
  },
  
  // 404 页面
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: { title: '页面未找到' }
  }
]

// ==================== 创建 Router 实例 ====================

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

// ==================== 路由守卫 ====================

// 白名单（无需登录）
const whiteList = ['/login', '/register']

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  
  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - Pixel Lab Pro` : 'Pixel Lab Pro'
  
  // 公开路由直接放行
  if (to.meta.public) {
    // 已登录用户访问登录/注册页，重定向到首页
    if (userStore.isLoggedIn && whiteList.includes(to.path)) {
      next('/')
      return
    }
    next()
    return
  }

  if (!userStore.isLoggedIn && !userStore.sessionChecked) {
    await userStore.fetchUserInfo().catch(() => {})
  }
  
  // 需要登录的路由
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    next('/login')
    return
  }
  
  // 需要管理员权限的路由
  if (to.meta.admin && !userStore.isAdmin) {
    ElMessage.error('您没有权限访问该页面')
    next('/')
    return
  }
  
  next()
})

router.afterEach((to) => {
  // 路由切换后的处理
  console.log(`[Router] Navigated to: ${to.path}`)
})

export default router
