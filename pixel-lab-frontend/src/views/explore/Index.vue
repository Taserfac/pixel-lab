<template>
  <div class="explore-page page-shell">
    <header class="explore-intro">
      <div class="intro-copy">
        <span class="pixel-accent" aria-hidden="true"><i /><i /></span>
        <h1 class="page-heading">灵感在这里发生</h1>
        <p class="page-description">发现优秀作品，记录灵感，也让下一次创作更快发生。</p>
        <UiButton size="lg" @click="router.push('/studio')">
          <template #icon><el-icon><EditPen /></el-icon></template>
          开始创作
        </UiButton>
      </div>

      <div class="feed-switch" aria-label="作品排序">
        <button
          v-for="tab in tabs"
          :key="tab.value"
          type="button"
          :class="{ active: activeTab === tab.value }"
          @click="activeTab = tab.value"
        >{{ tab.label }}</button>
      </div>
    </header>

    <section aria-live="polite" aria-busy="loading">
      <div v-if="loading" class="masonry-feed">
        <SkeletonCard v-for="index in 10" :key="index" :style="{ '--post-ratio': cardRatio(index) }" />
      </div>
      <div v-else-if="works.length" class="masonry-feed">
        <PostCard
          v-for="(work, index) in works"
          :key="work.id"
          :work="work"
          :style="{ '--post-ratio': cardRatio(index) }"
          @select="openWork"
          @tag-click="openTag"
        />
      </div>
      <EmptyState
        v-else
        title="这里还没有公开作品"
        description="发布第一件作品，让创作流动起来。"
        action-text="去发布"
        show-action
        @action="router.push('/publish')"
      />
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { EditPen } from '@element-plus/icons-vue'
import EmptyState from '@/components/common/EmptyState.vue'
import SkeletonCard from '@/components/common/SkeletonCard.vue'
import PostCard from '@/components/community/PostCard.vue'
import UiButton from '@/components/ui/UiButton.vue'
import { getPublicImages } from '@/api/community'

const router = useRouter()
const loading = ref(false)
const activeTab = ref('recommended')
const works = ref([])
const tabs = [
  { label: '推荐', value: 'recommended' },
  { label: '最新', value: 'latest' }
]
const ratios = ['3 / 5', '4 / 3', '4 / 5', '1 / 1', '3 / 5', '5 / 4']

const cardRatio = index => ratios[index % ratios.length]
const openWork = work => router.push(`/post/${work.id}`)
const openTag = tag => router.push({ path: '/discover', query: { tag } })

const loadWorks = async () => {
  loading.value = true
  try {
    const data = await getPublicImages({ page: 1, pageSize: 24, sort: activeTab.value })
    works.value = data?.list || []
  } catch {
    works.value = []
  } finally {
    loading.value = false
  }
}

watch(activeTab, loadWorks)
onMounted(loadWorks)
</script>

<style scoped>
.explore-page { padding-bottom: var(--space-8); }

.explore-intro {
  min-height: 210px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--space-8);
  padding: var(--space-8) 0 var(--space-6);
}

.intro-copy { position: relative; padding-left: var(--space-12); }
.intro-copy .page-description { margin-bottom: var(--space-6); }

.pixel-accent {
  width: 24px;
  height: 24px;
  display: block;
  position: absolute;
  top: 5px;
  left: 0;
}

.pixel-accent i {
  width: 11px;
  height: 11px;
  display: block;
  position: absolute;
  border-radius: 2px;
  background: var(--primary);
}

.pixel-accent i:last-child { top: 12px; left: 12px; opacity: 0.65; }

.feed-switch {
  display: flex;
  align-items: center;
  gap: var(--space-6);
  padding-bottom: var(--space-2);
}

.feed-switch button {
  position: relative;
  border: 0;
  padding: var(--space-2) 0;
  color: var(--text-secondary);
  background: transparent;
  font-weight: 680;
  cursor: pointer;
}

.feed-switch button:hover,
.feed-switch button.active { color: var(--primary-active); }

.feed-switch button.active::after {
  content: '';
  height: 3px;
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  border-radius: var(--radius-full);
  background: var(--primary);
}

.masonry-feed {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  align-items: start;
  gap: var(--space-4);
}
.masonry-feed :deep(.post-card) { margin: 0; }

@media (max-width: 1180px) {
  .masonry-feed { grid-template-columns: repeat(4, minmax(0, 1fr)); }
}

@media (max-width: 920px) {
  .masonry-feed { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}

@media (max-width: 720px) {
  .explore-intro { min-height: 0; align-items: flex-start; flex-direction: column; padding-top: var(--space-4); }
  .intro-copy { padding-left: var(--space-8); }
  .feed-switch { align-self: flex-end; }
  .masonry-feed { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: var(--space-3); }
}
</style>
