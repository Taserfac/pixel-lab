import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

const routes = [
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
  {
    path: '/',
    component: () => import('@/components/common/MainLayout.vue'),
    redirect: '/explore',
    children: [
      {
        path: 'explore',
        name: 'Explore',
        component: () => import('@/views/explore/Index.vue'),
        meta: { title: 'Explore' }
      },
      {
        path: 'discover',
        name: 'Discover',
        component: () => import('@/views/discover/Index.vue'),
        meta: { title: 'Discover' }
      },
      {
        path: 'publish',
        name: 'Publish',
        component: () => import('@/views/publish/Index.vue'),
        meta: { title: 'Publish' }
      },
      {
        path: 'studio/:id?',
        name: 'Studio',
        component: () => import('@/views/workbench/Index.vue'),
        meta: { title: 'Studio', immersive: true }
      },
      {
        path: 'post/:id',
        name: 'PostDetail',
        component: () => import('@/views/post/Index.vue'),
        meta: { title: '作品详情' }
      },
      {
        path: 'profile/:id?',
        name: 'Profile',
        component: () => import('@/views/personal/Index.vue'),
        meta: { title: 'Profile' }
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/settings/Index.vue'),
        meta: { title: '设置' }
      },
      {
        path: 'ranking',
        name: 'Ranking',
        component: () => import('@/views/ranking/Index.vue'),
        meta: { title: '排行榜' }
      },
      {
        path: 'stats',
        name: 'Stats',
        component: () => import('@/views/stats/Index.vue'),
        meta: { title: '数据统计' }
      },
      { path: 'community', redirect: '/discover' },
      { path: 'personal', redirect: '/profile' },
      { path: 'workbench', redirect: '/studio' },
      { path: 'draw', redirect: '/studio' }
    ]
  },
  {
    path: '/dashboard',
    component: () => import('@/components/admin/AdminLayout.vue'),
    meta: { title: '管理后台', admin: true },
    children: [
      {
        path: '',
        name: 'Dashboard',
        component: () => import('@/views/admin/Index.vue'),
        meta: { title: '管理后台', admin: true }
      }
    ]
  },
  { path: '/admin', redirect: '/dashboard' },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: { public: true, title: '页面未找到' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    return savedPosition || { top: 0 }
  }
})

router.beforeEach(async to => {
  const userStore = useUserStore()
  document.title = `${to.meta.title || 'Pixel Lab'} · Pixel Lab`

  if (to.meta.public) {
    if (userStore.isLoggedIn && ['/login', '/register'].includes(to.path)) return '/explore'
    return true
  }

  if (!userStore.isLoggedIn && !userStore.sessionChecked) {
    await userStore.fetchUserInfo().catch(() => {})
  }

  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (to.meta.admin && !userStore.isAdmin) {
    ElMessage.error('你没有访问 Dashboard 的权限')
    return '/explore'
  }

  return true
})

export default router
