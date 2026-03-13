<!--
  【文件路径】src/components/common/SideMenu.vue
  【文件功能说明】侧边栏菜单组件
  - 显示导航菜单
  - 支持折叠/展开
  - 菜单与路由联动
-->

<template>
  <aside
    class="side-menu"
    :class="{ collapsed: menuStore.collapsed }"
  >
    <!-- Logo -->
    <div class="logo">
      <span v-if="!menuStore.collapsed">Pixel Lab Pro</span>
      <span v-else>PL</span>
    </div>
    
    <!-- 菜单 -->
    <el-menu
      :default-active="activeRoute"
      :collapse="menuStore.collapsed"
      :collapse-transition="false"
      router
      class="menu"
    >
      <el-menu-item
        v-for="route in menuRoutes"
        :key="route.path"
        :index="route.path"
      >
        <el-icon>
          <component :is="route.meta.icon" />
        </el-icon>
        <template #title>
          {{ route.meta.title }}
        </template>
      </el-menu-item>
    </el-menu>
    
    <!-- 折叠按钮 -->
    <div
      class="collapse-btn"
      @click="menuStore.toggleCollapse"
    >
      <el-icon>
        <Fold v-if="!menuStore.collapsed" />
        <Expand v-else />
      </el-icon>
    </div>
  </aside>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Fold, Expand } from '@element-plus/icons-vue'
import { useMenuStore } from '@/store/menu'
import router from '@/router'

const route = useRoute()
const menuStore = useMenuStore()

// 当前激活的路由
const activeRoute = computed(() => route.path)

// 菜单路由（过滤掉需要管理员权限的路由，如果不是管理员）
const menuRoutes = computed(() => {
  const routes = router.getRoutes()
  const mainRoute = routes.find(r => r.path === '/')
  
  if (!mainRoute || !mainRoute.children) return []
  
  return mainRoute.children.filter(r => {
    // 过滤掉没有标题的路由
    if (!r.meta?.title) return false
    // 过滤掉需要管理员权限的路由（如果不是管理员）
    if (r.meta?.admin) return false
    return true
  })
})
</script>

<style scoped>
.side-menu {
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  width: 240px;
  background-color: var(--background-soft);
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
  z-index: 100;
}

.side-menu.collapsed {
  width: 64px;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid var(--border);
  font-size: 20px;
  font-weight: bold;
  color: var(--primary);
}

.logo img {
  height: 32px;
}

.menu {
  flex: 1;
  border-right: none;
  background-color: transparent;
}

.collapse-btn {
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-top: 1px solid var(--border);
  color: var(--foreground-muted);
  transition: all 0.2s;
}

.collapse-btn:hover {
  background-color: var(--background-muted);
  color: var(--primary);
}

@media (max-width: 768px) {
  .side-menu {
    transform: translateX(-100%);
  }
  
  .side-menu.collapsed {
    transform: translateX(-100%);
  }
}
</style>
