<template>
  <div class="post-page page-shell">
    <div v-if="loading" class="post-loading">
      <el-skeleton animated :rows="8" />
    </div>

    <template v-else-if="work">
      <UiButton variant="ghost" class="back-button" @click="router.back()">
        <template #icon><el-icon><ArrowLeft /></el-icon></template>
        返回
      </UiButton>

      <div class="post-layout">
        <UiCard padding="none" class="image-panel">
          <img :src="work.url" :alt="work.title || work.original_name || '作品图片'">
        </UiCard>

        <aside class="post-details">
          <div class="author-row">
            <el-avatar :size="42" :src="work.author_avatar">{{ authorInitial }}</el-avatar>
            <div>
              <strong>{{ work.author_name || '匿名创作者' }}</strong>
              <span>{{ formatDate(work.created_at) }}</span>
            </div>
          </div>

          <div>
            <h1>{{ work.title || work.original_name || '未命名作品' }}</h1>
            <p v-if="work.description">{{ work.description }}</p>
          </div>

          <div v-if="tags.length" class="detail-tags">
            <UiTag v-for="tag in tags" :key="tag" clickable @click="openTag(tag)">#{{ tag }}</UiTag>
          </div>

          <div class="engagement-row">
            <UiButton :variant="work.isLiked ? 'primary' : 'secondary'" @click="like">
              <template #icon><el-icon><StarFilled v-if="work.isLiked" /><Star v-else /></el-icon></template>
              {{ work.like_count || 0 }}
            </UiButton>
            <UiButton :variant="work.isCollected ? 'primary' : 'secondary'" @click="collect">
              <template #icon><el-icon><CollectionTag /></el-icon></template>
              {{ work.collect_count || 0 }}
            </UiButton>
          </div>

          <UiCard padding="md" class="comment-panel">
            <h2>评论 <span>{{ comments.length }}</span></h2>
            <form class="comment-form" @submit.prevent="submitComment">
              <UiInput v-model="comment" placeholder="说说你的想法" :maxlength="300" />
              <UiButton type="submit" :disabled="!comment.trim()" :loading="submittingComment">发送</UiButton>
            </form>
            <div v-if="comments.length" class="comment-list">
              <article v-for="item in comments" :key="item.id" class="comment-row">
                <el-avatar :size="30" :src="item.avatar">{{ (item.nickname || '?').charAt(0) }}</el-avatar>
                <div>
                  <strong>{{ item.nickname || '匿名用户' }}</strong>
                  <p>{{ item.content }}</p>
                  <small>{{ formatDate(item.created_at) }}</small>
                </div>
              </article>
            </div>
            <p v-else class="comment-empty">还没有评论，来留下第一条吧。</p>
          </UiCard>
        </aside>
      </div>
    </template>

    <EmptyState v-else title="作品不存在" description="它可能已被删除或设为私有。" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, CollectionTag, Star, StarFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import EmptyState from '@/components/common/EmptyState.vue'
import UiButton from '@/components/ui/UiButton.vue'
import UiCard from '@/components/ui/UiCard.vue'
import UiInput from '@/components/ui/UiInput.vue'
import UiTag from '@/components/ui/UiTag.vue'
import { addComment, getComments, getImageDetail, toggleCollect, toggleLike } from '@/api/community'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const work = ref(null)
const comments = ref([])
const comment = ref('')
const submittingComment = ref(false)

const authorInitial = computed(() => (work.value?.author_name || '?').charAt(0).toUpperCase())
const tags = computed(() => String(work.value?.tags || '').split(',').map(tag => tag.trim()).filter(Boolean))
const formatDate = value => value ? new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium' }).format(new Date(value)) : ''
const openTag = tag => router.push({ path: '/discover', query: { tag } })

const load = async () => {
  loading.value = true
  try {
    const [detail, commentData] = await Promise.all([
      getImageDetail(route.params.id),
      getComments(route.params.id, { page: 1, pageSize: 50 }).catch(() => ({ list: [] }))
    ])
    work.value = detail
    comments.value = commentData?.list || []
  } catch {
    work.value = null
  } finally {
    loading.value = false
  }
}

const like = async () => {
  const result = await toggleLike(work.value.id)
  work.value.isLiked = result?.liked ?? !work.value.isLiked
  work.value.like_count = result?.likeCount ?? Math.max(0, Number(work.value.like_count || 0) + (work.value.isLiked ? 1 : -1))
}

const collect = async () => {
  const result = await toggleCollect(work.value.id)
  work.value.isCollected = result?.collected ?? !work.value.isCollected
  work.value.collect_count = result?.collectCount ?? Math.max(0, Number(work.value.collect_count || 0) + (work.value.isCollected ? 1 : -1))
}

const submitComment = async () => {
  const content = comment.value.trim()
  if (!content) return
  submittingComment.value = true
  try {
    const created = await addComment({ imageId: Number(route.params.id), content })
    comments.value.unshift(created || { id: Date.now(), content, nickname: '我', created_at: new Date().toISOString() })
    comment.value = ''
    ElMessage.success('评论已发布')
  } finally {
    submittingComment.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.post-page { max-width: 1260px; }
.post-loading { max-width: 900px; margin: var(--space-12) auto; }
.back-button { margin-bottom: var(--space-4); }
.post-layout { display: grid; grid-template-columns: minmax(0, 1.5fr) minmax(340px, 0.7fr); gap: var(--space-6); align-items: start; }
.image-panel { overflow: hidden; }
.image-panel img { width: 100%; max-height: calc(100vh - 190px); display: block; object-fit: contain; background: var(--surface-muted); }
.post-details { display: grid; gap: var(--space-6); }
.author-row { display: flex; align-items: center; gap: var(--space-3); }
.author-row > div { display: grid; }
.author-row span { color: var(--text-tertiary); font-size: 11px; }
.post-details h1 { margin: 0; font-size: clamp(26px, 3vw, 38px); line-height: 1.16; }
.post-details p { color: var(--text-secondary); white-space: pre-wrap; }
.detail-tags,
.engagement-row { display: flex; flex-wrap: wrap; gap: var(--space-2); }
.comment-panel h2 { margin: 0 0 var(--space-4); font-size: 16px; }
.comment-panel h2 span { color: var(--text-tertiary); font-size: 12px; }
.comment-form { display: grid; grid-template-columns: 1fr auto; gap: var(--space-2); }
.comment-list { display: grid; gap: var(--space-4); margin-top: var(--space-6); }
.comment-row { display: flex; align-items: flex-start; gap: var(--space-2); }
.comment-row > div { min-width: 0; }
.comment-row strong { font-size: 12px; }
.comment-row p { margin: 2px 0; color: var(--text-primary); font-size: 13px; }
.comment-row small,
.comment-empty { color: var(--text-tertiary); font-size: 11px; }

@media (max-width: 900px) {
  .post-layout { grid-template-columns: 1fr; }
  .image-panel img { max-height: none; }
}
</style>
