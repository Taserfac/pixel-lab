<template>
  <article
    class="post-card"
    role="button"
    tabindex="0"
    @click="emit('select', work)"
    @keydown.enter.prevent="emit('select', work)"
    @keydown.space.prevent="emit('select', work)"
  >
    <div class="post-cover">
      <img
        v-if="coverUrl"
        :src="coverUrl"
        :alt="title"
        loading="lazy"
        decoding="async"
      >
      <div
        v-else
        class="post-placeholder"
      >
        <el-icon><Picture /></el-icon>
      </div>
    </div>

    <div class="post-body">
      <h3 :title="title">
        {{ title }}
      </h3>

      <div class="post-footer">
        <div class="post-author">
          <el-avatar
            :size="24"
            :src="work.author_avatar"
          >
            {{ authorInitial }}
          </el-avatar>
          <span>{{ authorName }}</span>
        </div>

        <div class="post-stats">
          <span>
            <el-icon><Star /></el-icon>
            {{ formatNumber(work.like_count) }}
          </span>
          <span>
            <el-icon><ChatDotRound /></el-icon>
            {{ formatNumber(work.comment_count) }}
          </span>
          <span>
            <el-icon><CollectionTag /></el-icon>
            {{ formatNumber(work.collect_count) }}
          </span>
        </div>
      </div>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import { ChatDotRound, CollectionTag, Picture, Star } from '@element-plus/icons-vue'

const props = defineProps({
  work: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['select'])

const title = computed(() => (
  props.work.title ||
  props.work.original_name ||
  props.work.filename ||
  props.work.name ||
  '未命名作品'
))

const coverUrl = computed(() => props.work.url || props.work.image_url || '')
const authorName = computed(() => props.work.author_name || props.work.nickname || '匿名创作者')
const authorInitial = computed(() => authorName.value.charAt(0).toUpperCase())

const formatNumber = (value) => {
  const number = Number(value || 0)
  if (number >= 10000) return `${(number / 10000).toFixed(1)}w`
  if (number >= 1000) return `${(number / 1000).toFixed(1)}k`
  return String(number)
}
</script>

<style scoped>
.post-card {
  --post-ratio: 4 / 3;
  display: inline-block;
  width: 100%;
  margin: 0 0 var(--space-5);
  overflow: hidden;
  border-radius: var(--radius-lg);
  background: var(--background-card);
  box-shadow: var(--shadow-sm);
  cursor: pointer;
  break-inside: avoid;
  transition:
    transform var(--transition-base),
    box-shadow var(--transition-base);
}

.post-card:hover {
  transform: translateY(-5px);
  box-shadow: var(--shadow-md);
}

.post-card:focus-visible {
  outline: 3px solid var(--primary-muted);
  outline-offset: 3px;
}

.post-cover {
  position: relative;
  aspect-ratio: var(--post-ratio);
  overflow: hidden;
  background: var(--background-muted);
}

.post-cover img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
  transition: transform var(--transition-slow);
}

.post-card:hover .post-cover img {
  transform: scale(1.045);
}

.post-placeholder {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  color: var(--foreground-subtle);
  font-size: 36px;
}

.post-body {
  padding: var(--space-4) var(--space-4) var(--space-5);
}

.post-body h3 {
  overflow: hidden;
  color: var(--foreground);
  font-size: 16px;
  font-weight: 700;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.post-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  margin-top: var(--space-3);
}

.post-author {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: var(--space-2);
  color: var(--foreground-muted);
  font-size: 13px;
}

.post-author span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.post-stats {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: var(--space-2);
  color: var(--foreground-muted);
  font-size: 12px;
}

.post-stats span {
  display: inline-flex;
  align-items: center;
  gap: 3px;
}

.post-stats .el-icon {
  font-size: 13px;
}

@media (max-width: 640px) {
  .post-footer {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
