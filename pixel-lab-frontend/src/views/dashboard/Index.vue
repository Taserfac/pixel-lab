<!--
  【文件路径】src/views/dashboard/Index.vue
  【文件功能说明】数据统计页面（空白占位）
-->

<template>
  <div class="dashboard-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>数据统计</span>
        </div>
      </template>
      <EmptyState
        title="数据统计"
        description="这里是数据统计页面，支持查看作品数据、用户统计等功能。功能开发中，敬请期待..."
        :show-action="false"
      />
    </el-card>
  </div>
</template>

<script setup>
import EmptyState from '@/components/common/EmptyState.vue'
</script>

<style scoped>
.dashboard-page {
  padding: var(--space-4);
}
</style>
