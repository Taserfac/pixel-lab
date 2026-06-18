<template>
  <UiCard
    as="article"
    padding="none"
    interactive
    class="post-card"
    role="button"
    tabindex="0"
    @click="emit('select', work)"
    @keydown.enter.prevent="emit('select', work)"
    @keydown.space.prevent="emit('select', work)"
  >
    <div class="post-cover">
      <img v-if="coverUrl" :src="coverUrl" :alt="title" loading="lazy" decoding="async">
      <div v-else class="post-placeholder"><el-icon><Picture /></el-icon></div>
      <span v-if="work.isLiked" class="liked-mark" aria-label="已点赞"><el-icon><StarFilled /></el-icon></span>
    </div>

    <div class="post-body">
      <h3 :title="title">{{ title }}</h3>
      <div v-if="tags.length" class="post-tags">
        <UiTag v-for="tag in tags" :key="tag" clickable @click.stop="emit('tag-click', tag)">#{{ tag }}</UiTag>
      </div>
      <footer class="post-footer">
        <div class="post-author">
          <el-avatar :size="24" :src="work.author_avatar">{{ authorInitial }}</el-avatar>
          <span>{{ authorName }}</span>
        </div>
        <div class="post-stats" aria-label="作品互动数据">
          <span><el-icon><Star /></el-icon>{{ formatNumber(work.like_count) }}</span>
          <span><el-icon><ChatDotRound /></el-icon>{{ formatNumber(work.comment_count) }}</span>
        </div>
      </footer>
    </div>
  </UiCard>
</template>

<script setup>
import { computed } from 'vue'
import { ChatDotRound, Picture, Star, StarFilled } from '@element-plus/icons-vue'
import UiCard from '@/components/ui/UiCard.vue'
import UiTag from '@/components/ui/UiTag.vue'

const props = defineProps({
  work: { type: Object, required: true }
})

const emit = defineEmits(['select', 'tag-click'])
const title = computed(() => props.work.title || props.work.original_name || props.work.filename || '未命名作品')
const coverUrl = computed(() => props.work.thumbnail_url || props.work.url || props.work.image_url || '')
const tags = computed(() => {
  const value = props.work.tags
  if (Array.isArray(value)) return value.filter(Boolean).slice(0, 3)
  return String(value || '').split(',').map(tag => tag.trim()).filter(Boolean).slice(0, 3)
})
const authorName = computed(() => props.work.author_name || props.work.nickname || '匿名创作者')
const authorInitial = computed(() => authorName.value.charAt(0).toUpperCase())

const formatNumber = value => {
  const number = Number(value || 0)
  if (number >= 10000) return `${(number / 10000).toFixed(1)}w`
  if (number >= 1000) return `${(number / 1000).toFixed(1)}k`
  return String(number)
}
</script>

<style scoped>
.post-card {
  width: 100%;
  display: inline-block;
  overflow: hidden;
  margin: 0 0 var(--space-4);
  break-inside: avoid;
  cursor: pointer;
}

.post-card:focus-visible { outline: 3px solid var(--primary-soft); outline-offset: 3px; }

.post-cover {
  position: relative;
  aspect-ratio: var(--post-ratio, 4 / 5);
  overflow: hidden;
  background: var(--surface-muted);
}

.post-cover img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
  transition: transform var(--transition-slow);
}

.post-card:hover .post-cover img { transform: scale(1.035); }

.post-placeholder {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  color: var(--text-tertiary);
  font-size: 36px;
}

.liked-mark {
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  position: absolute;
  top: var(--space-3);
  right: var(--space-3);
  border-radius: 50%;
  color: var(--primary);
  background: var(--surface-glass);
  backdrop-filter: blur(10px);
}

.post-body { padding: var(--space-3) var(--space-3) var(--space-4); }

.post-body h3 {
  overflow: hidden;
  margin: 0;
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 700;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.post-tags { display: flex; flex-wrap: wrap; gap: var(--space-1); margin-top: var(--space-2); }
.post-tags :deep(.ui-tag) { min-height: 24px; padding: 2px 8px; font-size: 10px; }

.post-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  margin-top: var(--space-3);
  color: var(--text-secondary);
  font-size: 11px;
}

.post-author,
.post-stats,
.post-stats span { display: flex; align-items: center; }
.post-author { min-width: 0; gap: var(--space-2); }
.post-author span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.post-stats { flex: none; gap: var(--space-2); }
.post-stats span { gap: 3px; }
</style>
