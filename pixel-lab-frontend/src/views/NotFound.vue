<!--
  【文件路径】src/views/NotFound.vue
  【文件功能说明】404 页面未找到
-->

<template>
  <div class="not-found-page">
    <div class="content">
      <h1 class="code">404</h1>
      <h2 class="title">页面未找到</h2>
      <p class="description">抱歉，您访问的页面不存在或已被移除</p>
      <el-button type="primary" @click="goHome">返回首页</el-button>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const goHome = () => {
  router.push('/')
}
</script>

<style scoped>
.not-found-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--background);
}

.content {
  text-align: center;
}

.code {
  font-size: 120px;
  font-weight: 700;
  color: var(--primary);
  margin-bottom: var(--space-4);
  line-height: 1;
}

.title {
  font-size: 24px;
  font-weight: 500;
  color: var(--foreground);
  margin-bottom: var(--space-2);
}

.description {
  font-size: 14px;
  color: var(--foreground-muted);
  margin-bottom: var(--space-6);
}
</style>
