<template>
  <div class="admin-shell">
    <button v-if="drawerOpen" class="admin-backdrop" type="button" aria-label="关闭后台菜单" @click="drawerOpen = false" />

    <aside class="admin-sidebar" :class="{ open: drawerOpen }">
      <router-link class="admin-brand" to="/dashboard" @click="drawerOpen = false">
        <span class="admin-brand-mark" aria-hidden="true"><i v-for="index in 7" :key="index" /></span>
        <span><strong>Pixel Lab</strong><small>管理后台</small></span>
      </router-link>

      <nav class="admin-nav" aria-label="后台导航">
        <p>工作台</p>
        <router-link
          v-for="item in navigation"
          :key="item.key"
          :to="{ path: '/dashboard', query: { section: item.key } }"
          :class="{ active: activeSection === item.key }"
          @click="drawerOpen = false"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </router-link>
      </nav>

      <div class="admin-account">
        <div class="admin-user">
          <el-avatar :size="36" :src="userStore.userInfo?.avatar">{{ userInitial }}</el-avatar>
          <span><strong>{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</strong><small>管理员</small></span>
        </div>
        <button type="button" @click="router.push('/explore')"><el-icon><Back /></el-icon>返回用户端</button>
        <button type="button" @click="logout"><el-icon><SwitchButton /></el-icon>退出登录</button>
      </div>
    </aside>

    <section class="admin-workspace">
      <header class="admin-topbar">
        <button class="menu-toggle" type="button" aria-label="打开后台菜单" @click="drawerOpen = true"><el-icon><Expand /></el-icon></button>
        <div>
          <small>管理后台 / {{ currentLabel }}</small>
          <strong>{{ currentLabel }}</strong>
        </div>
        <time>{{ currentDate }}</time>
      </header>
      <main class="admin-main"><router-view /></main>
    </section>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Back, DataAnalysis, Expand, Files, SwitchButton, User } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { logout as logoutApi } from '@/api/auth'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const drawerOpen = ref(false)
const navigation = [
  { key: 'overview', label: '数据概览', icon: DataAnalysis },
  { key: 'users', label: '用户管理', icon: User },
  { key: 'images', label: '作品管理', icon: Files }
]

const activeSection = computed(() => navigation.some(item => item.key === route.query.section) ? route.query.section : 'overview')
const currentLabel = computed(() => navigation.find(item => item.key === activeSection.value)?.label || '数据概览')
const userInitial = computed(() => (userStore.userInfo?.nickname || userStore.userInfo?.username || 'A').charAt(0).toUpperCase())
const currentDate = new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'short' }).format(new Date())

const logout = async () => {
  try {
    await ElMessageBox.confirm('确定退出管理员账号吗？', '退出登录', { type: 'warning', confirmButtonText: '退出', cancelButtonText: '取消' })
    await logoutApi().catch(() => {})
    userStore.logout()
    await router.replace('/login')
    ElMessage.success('已退出登录')
  } catch {
    // 管理员取消退出。
  }
}
</script>

<style scoped>
.admin-shell { min-height: 100vh; display: grid; grid-template-columns: 248px minmax(0, 1fr); color: var(--text-primary); background: #f3f5f4; }
.admin-sidebar { height: 100vh; display: flex; flex-direction: column; position: sticky; top: 0; z-index: 210; border-right: 1px solid var(--border); padding: 22px 16px 16px; background: #fff; }
.admin-brand { display: flex; align-items: center; gap: 12px; margin: 0 8px 28px; color: var(--text-primary); text-decoration: none; }
.admin-brand > span:last-child { display: grid; line-height: 1.15; }
.admin-brand strong { font-size: 16px; }
.admin-brand small { margin-top: 4px; color: var(--text-tertiary); font-size: 10px; font-weight: 600; letter-spacing: .08em; }
.admin-brand-mark { width: 32px; height: 32px; display: grid; grid-template-columns: repeat(3, 8px); grid-template-rows: repeat(3, 8px); gap: 2px; flex: none; }
.admin-brand-mark i { border-radius: 2px; background: var(--primary); }
.admin-brand-mark i:nth-child(2),
.admin-brand-mark i:nth-child(4) { opacity: 0; }
.admin-nav { display: grid; gap: 5px; }
.admin-nav p { margin: 0 12px 7px; color: var(--text-tertiary); font-size: 10px; font-weight: 700; letter-spacing: .12em; }
.admin-nav a { min-height: 43px; display: flex; align-items: center; gap: 11px; border-radius: var(--radius-md); padding: 0 12px; color: var(--text-secondary); text-decoration: none; font-size: 13px; font-weight: 620; transition: color var(--transition-fast), background var(--transition-fast); }
.admin-nav a:hover { color: var(--primary-active); background: var(--primary-soft); }
.admin-nav a.active { color: #fff; background: var(--primary); }
.admin-account { display: grid; gap: 5px; margin-top: auto; border-top: 1px solid var(--border); padding-top: 14px; }
.admin-user { display: flex; align-items: center; gap: 10px; margin-bottom: 6px; padding: 6px 8px; }
.admin-user > span { min-width: 0; display: grid; }
.admin-user strong { overflow: hidden; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.admin-user small { color: var(--text-tertiary); font-size: 10px; }
.admin-account button { min-height: 36px; display: flex; align-items: center; gap: 9px; border: 0; border-radius: var(--radius-md); padding: 0 10px; color: var(--text-secondary); background: transparent; font-size: 11px; cursor: pointer; }
.admin-account button:hover { color: var(--primary-active); background: var(--primary-soft); }
.admin-workspace { min-width: 0; }
.admin-topbar { height: 72px; display: flex; align-items: center; gap: 12px; position: sticky; top: 0; z-index: 100; border-bottom: 1px solid var(--border); padding: 0 clamp(20px, 3vw, 40px); background: rgba(255,255,255,.88); backdrop-filter: blur(18px); }
.admin-topbar > div { display: grid; }
.admin-topbar small { color: var(--text-tertiary); font-size: 10px; }
.admin-topbar strong { font-size: 14px; }
.admin-topbar time { margin-left: auto; color: var(--text-secondary); font-size: 11px; }
.menu-toggle { width: 38px; height: 38px; display: none; place-items: center; border: 1px solid var(--border); border-radius: var(--radius-md); color: var(--text-secondary); background: #fff; cursor: pointer; }
.admin-main { padding: clamp(20px, 3vw, 40px); }
.admin-backdrop { display: none; }

@media (max-width: 820px) {
  .admin-shell { grid-template-columns: 1fr; }
  .admin-sidebar { width: min(82vw, 280px); position: fixed; left: 0; transform: translateX(-105%); box-shadow: var(--shadow-lg); transition: transform var(--transition-normal); }
  .admin-sidebar.open { transform: translateX(0); }
  .admin-backdrop { display: block; position: fixed; inset: 0; z-index: 200; border: 0; background: rgba(21,28,25,.34); backdrop-filter: blur(2px); }
  .menu-toggle { display: grid; }
  .admin-topbar { padding-inline: 14px; }
  .admin-topbar time { display: none; }
  .admin-main { padding: 18px 14px 32px; }
}
</style>
