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
          :icon="themeStore.theme === 'light' ? Moon : Sunny"
          @click="themeStore.toggleTheme"
        />
        <el-dropdown @command="handleCommand">
          <div class="user-avatar">
            <el-avatar :size="36" :src="userStore.userInfo?.avatar">
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
              <el-dropdown-item divided command="logout">
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
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

    <!-- 底部 Dock 导航栏 -->
    <nav class="dock-nav">
      <router-link
        v-for="route in menuRoutes"
        :key="route.path"
        :to="route.path"
        class="dock-item"
        :class="{ active: activeRoute === route.path }"
      >
        <span class="dock-icon">
          <el-icon :size="24">
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
import router from '@/router'

const route = useRoute()
const routerInstance = useRouter()
const userStore = useUserStore()
const themeStore = useThemeStore()
const searchQuery = ref('')

const activeRoute = computed(() => route.path)

const menuRoutes = computed(() => {
  const routes = router.getRoutes()
  const mainRoute = routes.find(r => r.path === '/')
  if (!mainRoute || !mainRoute.children) return []
  return mainRoute.children.filter(r => {
    if (!r.meta?.title) return false
    if (r.meta?.admin) return false
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
      ElMessage.info('设置功能开发中...')
      break
    case 'logout':
      ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
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
}

/* 顶部导航栏 */
.top-bar {
  height: 72px;
  padding: 0 var(--space-6);
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--background-soft);
  border-bottom: 4px solid var(--border);
  position: sticky;
  top: 0;
  z-index: 100;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-icon {
  width: 44px;
  height: 44px;
  background: var(--primary);
  border: 3px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: center;
}

.pixel-text {
  font-family: var(--font-mono);
  font-size: 16px;
  font-weight: 700;
  color: white;
}

.brand-name {
  font-family: var(--font-mono);
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--foreground);
}

.search-bar {
  flex: 1;
  max-width: 400px;
  margin: 0 var(--space-8);
}

.search-bar :deep(.el-input__wrapper) {
  background: var(--accent);
  border-radius: 0;
}

.top-actions {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.top-actions .el-button.is-circle {
  background: var(--secondary);
  border: 3px solid var(--border) !important;
  box-shadow: 4px 4px 0px 0px var(--border);
  color: var(--foreground);
}

.user-avatar {
  cursor: pointer;
  padding: 4px;
  background: var(--accent);
  border: 3px solid var(--border);
  box-shadow: 4px 4px 0px 0px var(--border);
  transition: all var(--transition-fast);
}

.user-avatar:hover {
  transform: translate(-2px, -2px);
  box-shadow: 6px 6px 0px 0px var(--border);
}

/* 主内容区 */
.main-area {
  flex: 1;
  padding: var(--space-6);
  padding-bottom: 100px;
  overflow-y: auto;
}

/* 底部 Dock 导航栏 */
.dock-nav {
  position: fixed;
  bottom: var(--space-6);
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: var(--space-2);
  padding: var(--space-3) var(--space-4);
  background: var(--background-soft);
  border: 4px solid var(--border);
  box-shadow: 8px 8px 0px 0px var(--border);
  z-index: 100;
}

.dock-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-3) var(--space-4);
  min-width: 72px;
  text-decoration: none;
  color: var(--foreground);
  border: 3px solid transparent;
  transition: all var(--transition-fast);
}

.dock-item:hover {
  background: var(--accent);
  transform: translateY(-4px);
  box-shadow: 4px 4px 0px 0px var(--border);
}

.dock-item.active {
  background: var(--primary);
  color: white;
  box-shadow: 4px 4px 0px 0px var(--border);
}

.dock-label {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
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