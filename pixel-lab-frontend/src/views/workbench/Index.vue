<!--
  【文件路径】src/views/workbench/Index.vue
  【文件功能说明】图像处理工作台页面（空白占位）
  - 后续开发图像处理功能
-->

<template>
  <div class="workbench-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>图像处理工作台</span>
        </div>
      </template>
      <EmptyState
        title="图像处理工作台"
        description="这里是图像处理工作台，支持图片编辑、滤镜、裁剪等功能。功能开发中，敬请期待..."
        :show-action="false"
      />
    </el-card>
  </div>
</template>

<script setup>
import EmptyState from '@/components/common/EmptyState.vue'
</script>

<style scoped>
.workbench-page {
  padding: var(--space-4);
}

.card-header {
  font-weight: 500;
}
</style>
