<template>
  <div class="app-shell" :class="{ 'is-immersive': route.meta.immersive }">
    <header class="app-header">
      <router-link class="brand" to="/explore" aria-label="返回 Explore">
        <span class="brand-mark" aria-hidden="true">
          <i v-for="index in 7" :key="index" />
        </span>
        <span>Pixel Lab</span>
      </router-link>

      <form class="global-search" role="search" @submit.prevent="search">
        <el-icon><Search /></el-icon>
        <input v-model="searchQuery" type="search" placeholder="搜索作品或标签" aria-label="搜索作品或标签">
        <kbd>Enter</kbd>
      </form>

      <div class="header-actions">
        <el-popover placement="bottom-end" :width="320" trigger="click">
          <template #reference>
            <button class="header-action" type="button" aria-label="通知">
              <el-icon><Bell /></el-icon>
              <span v-if="unreadCount" class="notice-dot" />
            </button>
          </template>
          <div class="notification-popover">
            <div class="notification-title">
              <strong>通知</strong>
              <button type="button" @click="markAllRead">全部已读</button>
            </div>
            <button
              v-for="item in notifications"
              :key="item.id"
              class="notification-row"
              :class="{ unread: !item.read }"
              type="button"
              @click="item.read = true"
            >
              <span>{{ item.text }}</span>
              <small>{{ item.time }}</small>
            </button>
          </div>
        </el-popover>

        <button class="header-action" type="button" :aria-label="themeLabel" :title="themeLabel" @click="themeStore.toggleTheme">
          <el-icon><Sunny v-if="isDark" /><Moon v-else /></el-icon>
        </button>

        <el-dropdown trigger="click" @command="handleCommand">
          <button class="account-button" type="button" aria-label="用户菜单">
            <el-avatar :size="34" :src="userStore.userInfo?.avatar">
              {{ userInitial }}
            </el-avatar>
            <el-icon><ArrowDown /></el-icon>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile"><el-icon><User /></el-icon>个人资料</el-dropdown-item>
              <el-dropdown-item command="settings"><el-icon><Setting /></el-icon>设置</el-dropdown-item>
              <el-dropdown-item v-if="userStore.isAdmin" command="dashboard"><el-icon><DataLine /></el-icon>控制台</el-dropdown-item>
              <el-dropdown-item divided command="logout"><el-icon><SwitchButton /></el-icon>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <main class="app-main">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

    <nav class="bottom-nav" aria-label="主导航">
      <router-link
        v-for="item in navigation"
        :key="item.key"
        :to="item.to"
        class="nav-item"
        :class="{ 'nav-item--publish': item.key === 'publish', active: isActive(item) }"
      >
        <span class="nav-icon"><el-icon><component :is="item.icon" /></el-icon></span>
        <span>{{ item.label }}</span>
      </router-link>
    </nav>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowDown,
  Bell,
  Brush,
  Compass,
  DataLine,
  Moon,
  Plus,
  Search,
  Setting,
  Sunny,
  SwitchButton,
  User
} from '@element-plus/icons-vue'
import { useThemeStore } from '@/store/theme'
import { useUserStore } from '@/store/user'
import { logout as logoutApi } from '@/api/auth'

const route = useRoute()
const router = useRouter()
const themeStore = useThemeStore()
const userStore = useUserStore()
const searchQuery = ref(String(route.query.keyword || ''))

const navigation = [
  { key: 'explore', label: '探索', to: '/explore', icon: Compass },
  { key: 'discover', label: '发现', to: '/discover', icon: Search },
  { key: 'publish', label: '发布', to: '/publish', icon: Plus },
  { key: 'studio', label: '创作', to: '/studio', icon: Brush },
  { key: 'profile', label: '我的', to: '/profile', icon: User }
]

const notifications = ref([
  { id: 1, text: '有人喜欢了你的作品《云端漫游》', time: '2 分钟前', read: false },
  { id: 2, text: '你的作品已通过社区审核', time: '1 小时前', read: false },
  { id: 3, text: '本周创作挑战已经开始', time: '昨天', read: true }
])

const unreadCount = computed(() => notifications.value.filter(item => !item.read).length)
const isDark = computed(() => themeStore.theme === 'dark')
const themeLabel = computed(() => isDark.value ? '切换为浅色模式' : '切换为深色模式')
const userInitial = computed(() => (userStore.userInfo?.nickname || userStore.userInfo?.username || 'U').charAt(0).toUpperCase())

const isActive = item => route.path === item.to || route.path.startsWith(`${item.to}/`)

const search = () => {
  const keyword = searchQuery.value.trim()
  router.push({ path: '/discover', query: keyword ? { keyword } : {} })
}

const markAllRead = () => notifications.value.forEach(item => { item.read = true })

const handleCommand = async command => {
  if (command === 'profile') return router.push('/profile')
  if (command === 'settings') return router.push('/settings')
  if (command === 'dashboard') return router.push('/dashboard')
  if (command !== 'logout') return

  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '退出登录', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await logoutApi().catch(() => {})
    userStore.logout()
    await router.push('/login')
    ElMessage.success('已退出登录')
  } catch {
    // User cancelled the confirmation.
  }
}
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  color: var(--text-primary);
  background: var(--bg);
}

.app-header {
  height: 72px;
  display: grid;
  grid-template-columns: minmax(190px, 1fr) minmax(280px, 560px) minmax(190px, 1fr);
  align-items: center;
  gap: var(--space-6);
  position: sticky;
  top: 0;
  z-index: 110;
  padding: 0 clamp(var(--space-4), 3vw, var(--space-8));
  border-bottom: 1px solid var(--border);
  background: var(--surface-glass);
  backdrop-filter: blur(18px) saturate(1.25);
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: var(--space-3);
  width: max-content;
  color: var(--text-primary);
  text-decoration: none;
  font-size: 19px;
  font-weight: 780;
  letter-spacing: -0.03em;
}

.brand-mark {
  width: 32px;
  height: 32px;
  display: grid;
  grid-template-columns: repeat(3, 8px);
  grid-template-rows: repeat(3, 8px);
  gap: 2px;
}

.brand-mark i {
  border-radius: 2px;
  background: var(--primary);
}

.brand-mark i:nth-child(2),
.brand-mark i:nth-child(4) { opacity: 0; }

.global-search {
  min-height: 42px;
  display: flex;
  align-items: center;
  gap: var(--space-2);
  border: 1px solid var(--border);
  border-radius: var(--radius-full);
  padding: 0 var(--space-4);
  color: var(--text-tertiary);
  background: color-mix(in srgb, var(--card) 74%, transparent);
  transition: border-color var(--transition-fast), box-shadow var(--transition-fast), background var(--transition-fast);
}

.global-search:focus-within {
  border-color: var(--primary-border);
  background: var(--card);
  box-shadow: 0 0 0 3px var(--primary-soft);
}

.global-search input {
  width: 100%;
  min-width: 0;
  border: 0;
  outline: 0;
  color: var(--text-primary);
  background: transparent;
}

.global-search kbd {
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  padding: 1px 6px;
  color: var(--text-tertiary);
  background: var(--card);
  font: 10px var(--font-mono);
}

.header-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--space-2);
}

.header-action,
.account-button {
  border: 0;
  color: var(--text-secondary);
  background: transparent;
  cursor: pointer;
}

.header-action {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  position: relative;
  border-radius: var(--radius-md);
  font-size: 18px;
  transition: color var(--transition-fast), background var(--transition-fast), transform var(--transition-fast);
}

.header-action:hover {
  transform: translateY(-2px);
  color: var(--primary);
  background: var(--primary-soft);
}

.header-action:active { transform: scale(0.98); }

.notice-dot {
  width: 7px;
  height: 7px;
  position: absolute;
  top: 9px;
  right: 9px;
  border: 2px solid var(--card);
  border-radius: 50%;
  background: var(--danger);
}

.account-button {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  padding: 3px;
  border-radius: var(--radius-full);
}

.account-button:hover { background: var(--surface-muted); }

.notification-popover { margin: -12px; }

.notification-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-3) var(--space-4);
  border-bottom: 1px solid var(--border);
}

.notification-title button {
  border: 0;
  color: var(--primary-active);
  background: transparent;
  font-size: 12px;
  cursor: pointer;
}

.notification-row {
  width: 100%;
  display: grid;
  gap: 2px;
  border: 0;
  padding: var(--space-3) var(--space-4);
  color: var(--text-secondary);
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.notification-row:hover,
.notification-row.unread { background: var(--primary-soft); }
.notification-row.unread span { color: var(--text-primary); font-weight: 620; }
.notification-row small { color: var(--text-tertiary); }

.app-main {
  min-height: calc(100vh - 72px);
  padding: clamp(var(--space-6), 4vw, var(--space-12));
  padding-bottom: 124px;
}

.is-immersive .app-main {
  padding: 0 0 88px;
}

.is-immersive .app-header {
  display: none;
}

.is-immersive .app-main {
  min-height: 100vh;
}

.bottom-nav {
  min-width: min(92vw, 650px);
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  position: fixed;
  left: 50%;
  bottom: var(--space-6);
  z-index: 100;
  transform: translateX(-50%);
  overflow: visible;
  border: 1px solid rgba(234, 234, 234, 0.86);
  border-radius: 18px;
  padding: var(--space-2);
  background: var(--surface-glass);
  box-shadow: var(--shadow-md);
  backdrop-filter: blur(20px) saturate(1.3);
}

.nav-item {
  min-height: 54px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  position: relative;
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 12px;
  font-weight: 620;
  transition: color var(--transition-fast), background var(--transition-fast), transform var(--transition-fast);
}

.nav-item:hover {
  transform: translateY(-2px);
  color: var(--primary-active);
  background: var(--primary-soft);
}

.nav-item:active { transform: scale(0.98); }
.nav-item.active { color: var(--primary-active); }

.nav-item.active::after {
  content: '';
  width: 22px;
  height: 3px;
  position: absolute;
  bottom: 2px;
  border-radius: var(--radius-full);
  background: var(--primary);
}

.nav-icon {
  display: inline-flex;
  font-size: 19px;
}

.nav-item--publish .nav-icon {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  margin-top: -24px;
  border: 5px solid var(--bg);
  border-radius: 50%;
  color: #fff;
  background: var(--primary);
  box-shadow: var(--shadow-md);
}

@media (max-width: 820px) {
  .app-header { grid-template-columns: 1fr auto; }
  .global-search { display: none; }
  .brand { font-size: 17px; }
  .app-main { padding: var(--space-6) var(--space-4) 112px; }
  .bottom-nav { min-width: 0; width: calc(100% - 24px); bottom: var(--space-3); }
  .nav-item { flex-direction: column; gap: 2px; font-size: 10px; }
}

@media (max-width: 420px) {
  .app-header { height: 64px; padding-inline: var(--space-3); }
  .brand span:last-child { display: none; }
  .header-action:nth-of-type(2) { display: none; }
}
</style>
