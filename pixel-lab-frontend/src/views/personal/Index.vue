<!--
  【文件路径】src/views/personal/Index.vue
  【文件功能说明】个人中心页面（空白占位）
-->

<template>
  <div class="personal-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>个人中心</span>
        </div>
      </template>
      <EmptyState
        title="个人中心"
        description="这里是个人中心，支持查看个人信息、修改资料等功能。功能开发中，敬请期待..."
        :show-action="false"
      />
    </el-card>
  </div>
</template>

<script setup>
import EmptyState from '@/components/common/EmptyState.vue'
</script>

<style scoped>
.personal-page {
  padding: var(--space-4);
}
</style>
