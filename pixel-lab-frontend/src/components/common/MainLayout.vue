<template>
  <div class="community-layout">
    <header class="top-bar">
      <router-link
        to="/dashboard"
        class="brand"
        aria-label="Pixel Lab 首页"
      >
        <div class="brand-icon">
          <span class="pixel-text">PX</span>
        </div>
        <span class="brand-name">Pixel Lab</span>
      </router-link>

      <div class="search-bar">
        <el-input
          v-model="searchQuery"
          placeholder="搜索作品、作者、标签"
          :prefix-icon="Search"
          clearable
          @keyup.enter="handleSearch"
        />
      </div>

      <div class="top-actions">
        <el-button
          type="primary"
          class="upload-action"
          :loading="uploading"
          @click="triggerUpload"
        >
          <el-icon><Upload /></el-icon>
          上传作品
        </el-button>
        <button
          class="icon-action"
          type="button"
          title="消息通知"
          aria-label="消息通知"
          @click="handleNotifications"
        >
          <el-icon><Bell /></el-icon>
        </button>
        <button
          class="icon-action"
          type="button"
          :title="themeToggleLabel"
          :aria-label="themeToggleLabel"
          @click="themeStore.toggleTheme"
        >
          <el-icon>
            <Sunny v-if="isDarkTheme" />
            <Moon v-else />
          </el-icon>
        </button>
        <el-dropdown @command="handleCommand">
          <button
            class="user-avatar"
            type="button"
            aria-label="用户菜单"
          >
            <el-avatar
              :size="36"
              :src="userStore.userInfo?.avatar"
            >
              {{ userStore.userInfo?.nickname?.charAt(0) || 'U' }}
            </el-avatar>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">
                <el-icon><User /></el-icon>个人中心
              </el-dropdown-item>
              <el-dropdown-item command="settings">
                <el-icon><Setting /></el-icon>设置
              </el-dropdown-item>
              <el-dropdown-item
                v-if="userStore.isAdmin"
                command="admin"
              >
                <el-icon><Setting /></el-icon>后台管理
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

    <input
      ref="fileInput"
      class="visually-hidden"
      type="file"
      accept="image/*"
      @change="onFileSelected"
    >

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

    <nav class="dock-nav" aria-label="底部导航">
      <router-link
        v-for="item in navItems"
        :key="item.path"
        :to="item.path"
        class="dock-item"
        :class="{ active: isMenuActive(item.path) }"
        :style="{ '--dock-accent': item.accent }"
      >
        <span class="dock-icon">
          <el-icon :size="22">
            <component :is="item.icon" />
          </el-icon>
        </span>
        <span class="dock-label">{{ item.label }}</span>
      </router-link>

      <button
        class="dock-item dock-upload"
        type="button"
        :disabled="uploading"
        style="--dock-accent: var(--primary)"
        @click="triggerUpload"
      >
        <span class="dock-icon">
          <el-icon :size="22"><Upload /></el-icon>
        </span>
        <span class="dock-label">上传</span>
      </button>
    </nav>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Bell,
  Compass,
  HomeFilled,
  Moon,
  Search,
  Setting,
  Sunny,
  SwitchButton,
  Upload,
  User
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { useThemeStore } from '@/store/theme'
import { logout as logoutApi } from '@/api/auth'
import { uploadImage } from '@/api/image'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const themeStore = useThemeStore()
const searchQuery = ref('')
const fileInput = ref(null)
const uploading = ref(false)

const navItems = computed(() => {
  const items = [
    { path: '/dashboard', label: '首页', icon: HomeFilled, accent: '#16C784' },
    { path: '/community', label: '社区', icon: Compass, accent: '#5B8DEF' },
    { path: '/personal', label: '我的', icon: User, accent: '#FF8AB3' }
  ]

  if (userStore.isAdmin) {
    items.push({ path: '/admin', label: '管理', icon: Setting, accent: '#FF4757' })
  }

  return items
})

const isDarkTheme = computed(() => themeStore.theme === 'dark')
const themeToggleLabel = computed(() => isDarkTheme.value ? '切换到白天模式' : '切换到夜间模式')

const isMenuActive = (path) => route.path === path || route.path.startsWith(`${path}/`)

const handleSearch = () => {
  const keyword = searchQuery.value.trim()
  if (!keyword) return
  router.push({ path: '/community', query: { keyword } })
}

const handleNotifications = () => {
  ElMessage.info('暂无新通知')
}

const triggerUpload = () => {
  fileInput.value?.click()
}

const onFileSelected = async (event) => {
  const file = event.target.files?.[0]
  if (!file) return

  if (!file.type.startsWith('image/')) {
    ElMessage.error('请选择图片文件')
    event.target.value = ''
    return
  }

  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 5MB')
    event.target.value = ''
    return
  }

  uploading.value = true
  try {
    await uploadImage(file)
    ElMessage.success('上传成功')
    router.push('/personal')
  } catch (error) {
    console.error('上传失败:', error)
    ElMessage.error('上传失败')
  } finally {
    uploading.value = false
    event.target.value = ''
  }
}

const handleCommand = (command) => {
  switch (command) {
  case 'profile':
    router.push('/personal')
    break
  case 'settings':
    router.push('/settings')
    break
  case 'admin':
    router.push('/admin')
    break
  case 'logout':
    ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(async () => {
      await logoutApi().catch(() => {})
      userStore.logout()
      router.push('/login')
      ElMessage.success('已退出登录')
    }).catch(() => {})
    break
  }
}
</script>

<style scoped>
.community-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--background);
  color: var(--foreground);
}

.top-bar {
  height: 72px;
  display: grid;
  grid-template-columns: max-content minmax(240px, 520px) max-content;
  align-items: center;
  gap: var(--space-6);
  padding: 0 clamp(var(--space-4), 4vw, var(--space-10));
  position: sticky;
  top: 0;
  z-index: 100;
  background: var(--background-float);
  box-shadow: 0 1px 0 var(--border), 0 16px 32px rgba(17, 24, 39, 0.04);
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
}

[data-theme='dark'] .top-bar {
  box-shadow: 0 1px 0 var(--border), 0 16px 32px rgba(0, 0, 0, 0.18);
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  color: inherit;
  text-decoration: none;
}

.brand-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  background: var(--primary);
  box-shadow: var(--glow-sm);
}

.pixel-text {
  color: white;
  font-family: var(--font-mono);
  font-size: 14px;
  font-weight: 800;
}

.brand-name {
  color: var(--foreground);
  font-size: 18px;
  font-weight: 800;
  letter-spacing: 0;
}

.search-bar {
  width: 100%;
  justify-self: center;
}

.search-bar :deep(.el-input__wrapper) {
  min-height: 42px;
  padding: 0 var(--space-4);
  background: var(--background-muted) !important;
}

.top-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--space-3);
}

.upload-action {
  min-width: 108px;
}

.icon-action,
.user-avatar {
  width: 42px;
  height: 42px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: var(--radius-full);
  background: var(--background-elevated);
  color: var(--foreground-muted);
  box-shadow: var(--shadow-sm);
  cursor: pointer;
  transition:
    color var(--transition-fast),
    transform var(--transition-fast),
    box-shadow var(--transition-fast);
}

.icon-action:hover,
.user-avatar:hover {
  color: var(--primary);
  transform: translateY(-1px);
  box-shadow: var(--shadow);
}

.main-area {
  flex: 1;
  width: 100%;
  padding: clamp(var(--space-5), 4vw, var(--space-10));
  padding-bottom: 112px;
}

.dock-nav {
  position: fixed;
  bottom: var(--space-6);
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-2);
  z-index: 100;
  border: 1px solid rgba(255, 255, 255, 0.62);
  border-radius: var(--radius-xl);
  background: rgba(255, 255, 255, 0.72);
  box-shadow: var(--shadow-md);
  backdrop-filter: blur(22px);
  -webkit-backdrop-filter: blur(22px);
}

[data-theme='dark'] .dock-nav {
  border-color: rgba(255, 255, 255, 0.08);
  background: rgba(24, 32, 28, 0.72);
}

.dock-item {
  --dock-accent: var(--primary);
  min-width: 64px;
  min-height: 58px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 4px;
  position: relative;
  border: 0;
  border-radius: var(--radius-lg);
  background: transparent;
  color: var(--foreground-muted);
  text-decoration: none;
  cursor: pointer;
  transition:
    color var(--transition-fast),
    transform var(--transition-fast),
    background var(--transition-fast),
    box-shadow var(--transition-fast);
}

.dock-item:hover {
  color: var(--dock-accent);
  transform: translateY(-3px);
  background: color-mix(in srgb, var(--dock-accent) 12%, transparent);
}

.dock-item:active {
  transform: translateY(0) scale(0.96);
}

.dock-item.active {
  color: var(--dock-accent);
  background: color-mix(in srgb, var(--dock-accent) 14%, white);
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--dock-accent) 18%, transparent);
}

[data-theme='dark'] .dock-item.active {
  background: color-mix(in srgb, var(--dock-accent) 16%, transparent);
}

.dock-item:focus-visible {
  outline: 3px solid color-mix(in srgb, var(--dock-accent) 24%, transparent);
  outline-offset: 2px;
}

.dock-upload {
  min-width: 70px;
  color: white;
  background: var(--primary);
  box-shadow: var(--glow-sm);
}

.dock-upload:hover {
  color: white;
  background: var(--primary-hover);
}

.dock-upload:disabled {
  cursor: wait;
  opacity: 0.72;
}

.dock-label {
  font-size: 11px;
  font-weight: 600;
  line-height: 1;
}

.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 900px) {
  .top-bar {
    grid-template-columns: max-content minmax(0, 1fr) max-content;
    gap: var(--space-4);
  }

  .brand-name {
    display: none;
  }

  .upload-action {
    display: none;
  }
}

@media (max-width: 720px) {
  .top-bar {
    grid-template-columns: max-content 1fr max-content;
    padding: 0 var(--space-4);
  }

  .search-bar {
    display: none;
  }

  .top-actions {
    gap: var(--space-2);
  }

  .icon-action {
    width: 38px;
    height: 38px;
  }

  .dock-nav {
    left: var(--space-4);
    right: var(--space-4);
    bottom: var(--space-4);
    transform: none;
    justify-content: space-around;
  }

  .dock-item {
    min-width: 0;
    flex: 1;
  }
}

@media (max-width: 420px) {
  .dock-label {
    display: none;
  }

  .dock-item {
    min-height: 50px;
  }
}
</style>
