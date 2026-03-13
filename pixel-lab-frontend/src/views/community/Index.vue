<!--
  【文件路径】src/views/community/Index.vue
  【文件功能说明】社区广场页面（空白占位）
-->

<template>
  <div class="community-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>社区广场</span>
        </div>
      </template>
      <EmptyState
        title="社区广场"
        description="这里是社区广场，支持作品分享、点赞评论等功能。功能开发中，敬请期待..."
        :show-action="false"
      />
    </el-card>
  </div>
</template>

<script setup>
import EmptyState from '@/components/common/EmptyState.vue'
</script>

<style scoped>
.community-page {
  padding: var(--space-4);
}
</style>
