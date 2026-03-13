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
        :icon="themeStore.theme === 'light' ? Moon : Sunny"
        title="切换主题"
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
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Moon, Sunny, ArrowDown, User, Setting, SwitchButton } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { useThemeStore } from '@/store/theme'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const themeStore = useThemeStore()

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
  }).then(() => {
    userStore.logout()
    router.push('/login')
    ElMessage.success('已退出登录')
  }).catch(() => {})
}
</script>

<style scoped>
.header-bar {
  height: 60px;
  padding: 0 var(--space-6);
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: var(--background);
  border-bottom: 1px solid var(--border);
}

.actions {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.user-info {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  cursor: pointer;
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius);
  transition: background-color 0.2s;
}

.user-info:hover {
  background-color: var(--background-muted);
}

.username {
  font-size: 14px;
  color: var(--foreground);
}

@media (max-width: 768px) {
  .username {
    display: none;
  }
}
</style>
