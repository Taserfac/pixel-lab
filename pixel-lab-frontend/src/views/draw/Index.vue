<!--
  【文件路径】src/views/draw/Index.vue
  【文件功能说明】手绘板页面（空白占位）
-->

<template>
  <div class="draw-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>手绘板</span>
        </div>
      </template>
      <EmptyState
        title="手绘板"
        description="这里是手绘板，支持自由绘画、图层管理等功能。功能开发中，敬请期待..."
        :show-action="false"
      />
    </el-card>
  </div>
</template>

<script setup>
import EmptyState from '@/components/common/EmptyState.vue'
</script>

<style scoped>
.draw-page {
  padding: var(--space-4);
}
</style>
