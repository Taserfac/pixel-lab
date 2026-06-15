<!--
  【文件路径】src/components/common/HeaderBar.vue
  【文件功能说明】顶部导航栏组件
  - 面包屑导航
  - 主题切换按钮
  - 用户信息下拉菜单
-->

<template>
  <header class="header-bar">
    <!-- 面包屑 -->
    <el-breadcrumb separator="/">
      <el-breadcrumb-item :to="{ path: '/' }">
        首页
      </el-breadcrumb-item>
      <el-breadcrumb-item v-if="route.meta.title">
        {{ route.meta.title }}
      </el-breadcrumb-item>
    </el-breadcrumb>
    
    <!-- 右侧操作区 -->
    <div class="actions">
      <!-- 主题切换 -->
      <el-button
        circle
        :icon="isDarkTheme ? Sunny : Moon"
        :title="themeToggleLabel"
        :aria-label="themeToggleLabel"
        @click="themeStore.toggleTheme"
      />
      
      <!-- 用户信息 -->
      <el-dropdown @command="handleCommand">
        <div class="user-info">
          <el-avatar
            :size="32"
            :src="userStore.userInfo?.avatar"
          >
            {{ userStore.userInfo?.nickname?.charAt(0) || 'U' }}
          </el-avatar>
          <span class="username">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
          <el-icon><ArrowDown /></el-icon>
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
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Moon, Sunny, ArrowDown, User, Setting, SwitchButton } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { useThemeStore } from '@/store/theme'
import { logout as logoutApi } from '@/api/auth'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const themeStore = useThemeStore()
const isDarkTheme = computed(() => themeStore.theme === 'dark')
const themeToggleLabel = computed(() => isDarkTheme.value ? '切换到白天模式' : '切换到夜间模式')

// 处理下拉菜单命令
const handleCommand = (command) => {
  switch (command) {
  case 'profile':
    router.push('/personal')
    break
  case 'settings':
    ElMessage.info('设置功能开发中...')
    break
  case 'logout':
    handleLogout()
    break
  }
}

// 退出登录
const handleLogout = () => {
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
}
</script>

<style scoped>
.header-bar {
  height: 70px;
  padding: 0 var(--space-6);
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: var(--background-soft);
  border-bottom: 4px solid var(--border);
}

.header-bar :deep(.el-breadcrumb) {
  font-size: 14px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.header-bar :deep(.el-breadcrumb__inner) {
  color: var(--foreground);
}

.header-bar :deep(.el-breadcrumb__separator) {
  color: var(--primary);
  font-weight: 700;
}

.actions {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.actions .el-button.is-circle {
  background: var(--secondary);
  border: 3px solid var(--border) !important;
  box-shadow: 4px 4px 0px 0px var(--border);
  color: var(--foreground);
}

.actions .el-button.is-circle:hover {
  transform: translate(-2px, -2px);
  box-shadow: 6px 6px 0px 0px var(--border);
}

.user-info {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  cursor: pointer;
  padding: var(--space-2) var(--space-4);
  background: var(--accent);
  border: 3px solid var(--border);
  box-shadow: 4px 4px 0px 0px var(--border);
  transition: all var(--transition-fast);
}

.user-info:hover {
  transform: translate(-2px, -2px);
  box-shadow: 6px 6px 0px 0px var(--border);
}

.user-info :deep(.el-avatar) {
  border: 2px solid var(--border);
}

.username {
  font-size: 14px;
  font-weight: 700;
  color: var(--foreground);
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

@media (max-width: 768px) {
  .username {
    display: none;
  }
}
</style>
