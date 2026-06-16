<template>
  <div class="bento-layout">
    <!-- 顶部导航栏 -->
    <header class="top-bar">
      <div class="brand">
        <div class="brand-icon">
          <span class="pixel-text">PX</span>
        </div>
        <span class="brand-name">Pixel Lab</span>
      </div>

      <div class="search-bar">
        <el-input
          v-model="searchQuery"
          placeholder="搜索作品、用户..."
          :prefix-icon="Search"
          @keyup.enter="handleSearch"
        />
      </div>

      <div class="top-actions">
        <el-button
          circle
          class="theme-toggle"
          :icon="isDarkTheme ? Sunny : Moon"
          :title="themeToggleLabel"
          :aria-label="themeToggleLabel"
          @click="themeStore.toggleTheme"
        />
        <el-dropdown @command="handleCommand">
          <div class="user-avatar">
            <el-avatar
              :size="36"
              :src="userStore.userInfo?.avatar"
            >
              {{ userStore.userInfo?.nickname?.charAt(0) || 'U' }}
            </el-avatar>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">
                <el-icon><User /></el-icon>个人中心
              </el-dropdown-item>
              <el-dropdown-item command="settings">
                <el-icon><Setting /></el-icon>设置
              </el-dropdown-item>
              <el-dropdown-item
                divided
                command="logout"
              >
                <el-icon><SwitchButton /></el-icon>退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <!-- 主内容区 -->
    <main class="main-area">
      <router-view v-slot="{ Component }">
        <transition
          name="fade"
          mode="out-in"
        >
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

    <!-- 底部 Dock 导航栏 -->
    <nav class="dock-nav">
      <router-link
        v-for="route in menuRoutes"
        :key="route.path"
        :to="route.path.startsWith('/') ? route.path : '/' + route.path"
        class="dock-item"
        :class="{ active: activeRoute === route.path }"
      >
        <span class="dock-icon">
          <el-icon :size="22">
            <component :is="route.meta.icon" />
          </el-icon>
        </span>
        <span class="dock-label">{{ route.meta.title }}</span>
      </router-link>
    </nav>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Moon, Sunny, User, Setting, SwitchButton } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { useThemeStore } from '@/store/theme'
import { logout as logoutApi } from '@/api/auth'
import router from '@/router'

const route = useRoute()
const routerInstance = useRouter()
const userStore = useUserStore()
const themeStore = useThemeStore()
const searchQuery = ref('')

const activeRoute = computed(() => route.path)
const isDarkTheme = computed(() => themeStore.theme === 'dark')
const themeToggleLabel = computed(() => isDarkTheme.value ? '切换到白天模式' : '切换到夜间模式')

const menuRoutes = computed(() => {
  const routes = router.getRoutes()
  const mainRoute = routes.find(r => r.path === '/')
  if (!mainRoute || !mainRoute.children) return []
  return mainRoute.children.filter(r => {
    if (!r.meta?.title) return false
    if (r.meta?.hideInMenu) return false
    // 管理员菜单只对管理员显示
    if (r.meta?.admin && !userStore.isAdmin) return false
    return true
  })
})

const handleSearch = () => {
  if (searchQuery.value.trim()) {
    ElMessage.info(`搜索: ${searchQuery.value}`)
  }
}

const handleCommand = (command) => {
  switch (command) {
  case 'profile':
    routerInstance.push('/personal')
    break
  case 'settings':
    routerInstance.push('/settings')
    break
  case 'logout':
    ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(async () => {
      await logoutApi().catch(() => {})
      userStore.logout()
      routerInstance.push('/login')
      ElMessage.success('已退出登录')
    }).catch(() => {})
    break
  }
}
</script>

<style scoped>
.bento-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: var(--background);
  position: relative;
  z-index: 1;
}

/* 顶部导航栏 */
.top-bar {
  height: 64px;
  padding: 0 var(--space-6);
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--background-soft);
  border-bottom: 1px solid var(--border);
  position: sticky;
  top: 0;
  z-index: 100;
  backdrop-filter: blur(12px);
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-icon {
  width: 40px;
  height: 40px;
  background: var(--primary);
  border-radius: var(--radius);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 0 20px var(--primary-glow);
}

.pixel-text {
  font-family: var(--font-mono);
  font-size: 14px;
  font-weight: 700;
  color: var(--background);
}

.brand-name {
  font-family: var(--font-sans);
  font-size: 18px;
  font-weight: 600;
  letter-spacing: -0.02em;
  color: var(--foreground);
}

.search-bar {
  flex: 1;
  max-width: 400px;
  margin: 0 var(--space-8);
}

.search-bar :deep(.el-input__wrapper) {
  background: var(--background-muted);
  border-radius: var(--radius-full);
}

.top-actions {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.top-actions .el-button.is-circle {
  background: var(--background-muted);
  border: 1px solid var(--border) !important;
  color: var(--foreground-muted);
  transition: all var(--transition-fast);
}

.top-actions .el-button.is-circle:hover {
  border-color: var(--primary) !important;
  color: var(--primary);
  background: var(--primary-muted);
}

.user-avatar {
  cursor: pointer;
  padding: 3px;
  background: var(--background-muted);
  border: 1px solid var(--border);
  border-radius: var(--radius-full);
  transition: all var(--transition-fast);
}

.user-avatar:hover {
  border-color: var(--primary);
  box-shadow: 0 0 16px var(--primary-glow);
}

/* 主内容区 */
.main-area {
  flex: 1;
  padding: var(--space-6);
  padding-bottom: 100px;
  overflow-y: auto;
}

/* 底部 Dock 导航栏 - 毛玻璃效果 */
.dock-nav {
  position: fixed;
  bottom: var(--space-6);
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: var(--space-1);
  padding: var(--space-2);
  background: rgba(26, 26, 26, 0.8);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg), 0 0 40px rgba(0, 0, 0, 0.3);
  z-index: 100;
}

[data-theme='light'] .dock-nav {
  background: rgba(255, 255, 255, 0.88);
  border-color: rgba(0, 0, 0, 0.1);
  box-shadow: var(--shadow-lg), 0 18px 48px rgba(0, 0, 0, 0.14);
}

.dock-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: var(--space-3) var(--space-4);
  min-width: 64px;
  text-decoration: none;
  color: var(--foreground-muted);
  border-radius: var(--radius);
  border: 1px solid transparent;
  transition: all var(--transition-fast);
}

.dock-item:hover {
  background: var(--background-muted);
  color: var(--foreground);
  transform: translateY(-2px);
}

.dock-item.active {
  background: var(--primary-muted);
  color: var(--primary);
  border-color: var(--border-glow);
}

.dock-item.active .dock-icon {
  filter: drop-shadow(0 0 8px var(--primary-glow));
}

.dock-label {
  font-size: 10px;
  font-weight: 500;
  letter-spacing: 0.02em;
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 响应式 */
@media (max-width: 768px) {
  .search-bar {
    display: none;
  }

  .brand-name {
    display: none;
  }

  .dock-nav {
    left: var(--space-4);
    right: var(--space-4);
    transform: none;
    justify-content: space-around;
  }

  .dock-item {
    min-width: auto;
    padding: var(--space-2);
  }

  .dock-label {
    display: none;
  }
}
</style>
