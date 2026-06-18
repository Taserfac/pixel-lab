<!--
  【文件路径】src/components/common/SideMenu.vue
  【功能说明】侧边栏菜单 - Neo-Brutalism 新野兽主义风格
-->

<template>
  <aside
    class="side-menu"
    :class="{ collapsed: menuStore.collapsed }"
  >
    <!-- Logo -->
    <div class="logo">
      <img class="logo-icon" :src="pixelLabLogo" alt="Pixel Lab logo">
      <span
        v-if="!menuStore.collapsed"
        class="logo-text"
      >Pixel Lab</span>
    </div>
    
    <!-- 菜单 -->
    <nav class="menu">
      <router-link
        v-for="route in menuRoutes"
        :key="route.path"
        :to="route.path.startsWith('/') ? route.path : '/' + route.path"
        class="menu-item"
        :class="{ active: activeRoute === route.path }"
      >
        <span class="menu-icon">
          <component :is="route.meta.icon" />
        </span>
        <span
          v-if="!menuStore.collapsed"
          class="menu-text"
        >{{ route.meta.title }}</span>
      </router-link>
    </nav>
    
    <!-- 底部折叠按钮 -->
    <div class="menu-footer">
      <button
        class="collapse-btn"
        @click="menuStore.toggleCollapse"
      >
        <span class="btn-text">
          {{ menuStore.collapsed ? '→' : '←' }}
        </span>
      </button>
    </div>
  </aside>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useMenuStore } from '@/store/menu'
import { useUserStore } from '@/store/user'
import router from '@/router'
import pixelLabLogo from '@/assets/images/pixel-lab-logo.svg'

const route = useRoute()
const menuStore = useMenuStore()
const userStore = useUserStore()

const activeRoute = computed(() => route.path)

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
</script>

<style scoped>
.side-menu {
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  width: 260px;
  background: var(--background-soft);
  border-right: 4px solid var(--border);
  display: flex;
  flex-direction: column;
  transition: width var(--transition-base);
  z-index: 100;
}

.side-menu.collapsed {
  width: 80px;
}

/* Logo */
.logo {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px;
  border-bottom: 4px solid var(--border);
  background: var(--primary);
}

.logo-icon {
  width: 48px;
  height: 48px;
  flex-shrink: 0;
  border-radius: 12px;
  box-shadow: 4px 4px 0 var(--border);
}

.logo-text {
  font-family: var(--font-mono);
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: white;
  white-space: nowrap;
  text-shadow: 2px 2px 0px var(--border);
}

/* 菜单 */
.menu {
  flex: 1;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow-y: auto;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border: 3px solid var(--border);
  background: var(--background-soft);
  color: var(--foreground);
  text-decoration: none;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  transition: all var(--transition-fast);
  cursor: pointer;
}

.menu-item:hover {
  background: var(--accent);
  transform: translate(-4px, -4px);
  box-shadow: 6px 6px 0px 0px var(--border);
}

.menu-item.active {
  background: var(--primary);
  color: white;
  box-shadow: 4px 4px 0px 0px var(--border);
}

.menu-icon {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.menu-icon :deep(svg) {
  width: 20px;
  height: 20px;
}

.menu-text {
  font-size: 14px;
  white-space: nowrap;
}

/* 底部 */
.menu-footer {
  padding: 16px;
  border-top: 4px solid var(--border);
}

.collapse-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px;
  border: 3px solid var(--border);
  background: var(--secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
  font-weight: 700;
}

.collapse-btn:hover {
  background: var(--accent);
  transform: translate(-2px, -2px);
  box-shadow: 4px 4px 0px 0px var(--border);
}

.btn-text {
  font-size: 20px;
  font-family: var(--font-mono);
}

/* 收起状态 */
.collapsed .logo {
  justify-content: center;
  padding: 24px 0;
}

.collapsed .logo-text {
  display: none;
}

.collapsed .menu {
  padding: 16px 8px;
}

.collapsed .menu-item {
  justify-content: center;
  padding: 16px 8px;
}

.collapsed .menu-text {
  display: none;
}

.collapsed .menu-footer {
  padding: 16px 8px;
}
</style>
