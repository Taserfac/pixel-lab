<!--
  【文件路径】src/components/common/PageLoading.vue
  【文件功能说明】页面加载状态组件
  - 全屏加载遮罩
  - 支持自定义提示文字
-->

<template>
  <div
    v-if="visible"
    class="page-loading"
  >
    <div class="loading-content">
      <el-icon
        class="loading-icon"
        :size="48"
      >
        <Loading />
      </el-icon>
      <p class="loading-text">
        {{ text }}
      </p>
    </div>
  </div>
</template>

<script setup>
import { Loading } from '@element-plus/icons-vue'

defineProps({
  // 是否显示
  visible: {
    type: Boolean,
    default: true
  },
  // 提示文字
  text: {
    type: String,
    default: '加载中...'
  }
})
</script>

<style scoped>
.page-loading {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: var(--background);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.loading-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-4);
}

.loading-icon {
  color: var(--primary);
  animation: rotate 1s linear infinite;
}

.loading-text {
  font-size: 14px;
  color: var(--foreground-muted);
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
