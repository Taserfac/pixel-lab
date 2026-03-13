<!--
  【文件路径】src/components/common/MainLayout.vue
  【文件功能说明】主布局组件
  - 包含侧边栏菜单、顶部导航栏、主内容区
  - 所有业务页面的外层容器
-->

<template>
  <div class="main-layout">
    <!-- 侧边栏 -->
    <SideMenu />
    
    <!-- 主内容区 -->
    <div
      class="main-content"
      :class="{ collapsed: menuStore.collapsed }"
    >
      <!-- 顶部导航栏 -->
      <HeaderBar />
      
      <!-- 页面内容 -->
      <main class="page-content">
        <router-view v-slot="{ Component }">
          <transition
            name="fade"
            mode="out-in"
          >
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup>
import SideMenu from './SideMenu.vue'
import HeaderBar from './HeaderBar.vue'
import { useMenuStore } from '@/store/menu'

const menuStore = useMenuStore()
</script>

<style scoped>
.main-layout {
  display: flex;
  min-height: 100vh;
  background-color: var(--background);
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  margin-left: 240px;
  transition: margin-left 0.3s ease;
}

.main-content.collapsed {
  margin-left: 64px;
}

.page-content {
  flex: 1;
  padding: var(--space-6);
  overflow-y: auto;
}

@media (max-width: 768px) {
  .main-content {
    margin-left: 0;
  }
  
  .main-content.collapsed {
    margin-left: 0;
  }
}
</style>
