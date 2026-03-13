<!--
  【文件路径】src/views/admin/Index.vue
  【文件功能说明】后台管理页面（空白占位）
-->

<template>
  <div class="admin-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>后台管理</span>
          <el-tag type="danger">管理员</el-tag>
        </div>
      </template>
      <EmptyState
        title="后台管理"
        description="这里是后台管理页面，支持用户管理、内容审核等功能。功能开发中，敬请期待..."
        :show-action="false"
      />
    </el-card>
  </div>
</template>

<script setup>
import EmptyState from '@/components/common/EmptyState.vue'
</script>

<style scoped>
.admin-page {
  padding: var(--space-4);
}

.card-header {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}
</style>
