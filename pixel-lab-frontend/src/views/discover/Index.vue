<template>
  <div class="discover-page page-shell">
    <header class="discover-header">
      <div>
        <h1 class="page-heading">Discover</h1>
        <p class="page-description">搜索所有公开作品，或从标签里找到下一份灵感。</p>
      </div>
      <form class="discover-search" @submit.prevent="applyFilters">
        <UiInput v-model="keyword" clearable placeholder="搜索作品、作者或标签">
          <template #prefix><el-icon><Search /></el-icon></template>
        </UiInput>
        <UiButton type="submit">搜索</UiButton>
      </form>
    </header>

    <div class="filter-bar">
      <div class="tag-filters" aria-label="标签筛选">
        <UiTag
          v-for="tag in tags"
          :key="tag.value"
          clickable
          :active="activeTag === tag.value"
          @click="selectTag(tag.value)"
        >{{ tag.label }}</UiTag>
      </div>
      <div class="sort-switch" aria-label="排序方式">
        <button type="button" :class="{ active: sort === 'latest' }" @click="changeSort('latest')">最新</button>
        <button type="button" :class="{ active: sort === 'popular' }" @click="changeSort('popular')">热门</button>
      </div>
    </div>

    <div v-if="loading && !works.length" class="works-grid">
      <SkeletonCard v-for="index in 12" :key="index" :style="{ '--post-ratio': cardRatio(index) }" />
    </div>
    <div v-else-if="works.length" class="works-grid">
      <PostCard
        v-for="(work, index) in works"
        :key="work.id"
        :work="work"
        :style="{ '--post-ratio': cardRatio(index) }"
        @select="openWork"
        @tag-click="selectTag"
      />
    </div>
    <EmptyState v-else title="没有找到匹配作品" description="试试其他关键词或清除标签筛选。" />

    <div v-if="hasMore" class="load-more">
      <UiButton variant="secondary" :loading="loadingMore" @click="loadMore">加载更多</UiButton>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import EmptyState from '@/components/common/EmptyState.vue'
import SkeletonCard from '@/components/common/SkeletonCard.vue'
import PostCard from '@/components/community/PostCard.vue'
import UiButton from '@/components/ui/UiButton.vue'
import UiInput from '@/components/ui/UiInput.vue'
import UiTag from '@/components/ui/UiTag.vue'
import { getPublicImages } from '@/api/community'

const route = useRoute()
const router = useRouter()
const keyword = ref(String(route.query.keyword || ''))
const activeTag = ref(String(route.query.tag || ''))
const sort = ref('latest')
const works = ref([])
const page = ref(1)
const pageSize = 20
const total = ref(0)
const loading = ref(false)
const loadingMore = ref(false)
const ratios = ['4 / 5', '1 / 1', '5 / 4', '3 / 4', '4 / 3']
const tags = [
  { label: '全部', value: '' },
  { label: '# 插画', value: '插画' },
  { label: '# 摄影', value: '摄影' },
  { label: '# UI 设计', value: 'UI设计' },
  { label: '# 像素艺术', value: '像素艺术' },
  { label: '# AI 辅助', value: 'AI辅助' }
]

const hasMore = computed(() => works.value.length < total.value)
const cardRatio = index => ratios[index % ratios.length]
const openWork = work => router.push(`/post/${work.id}`)

const syncQuery = () => {
  router.replace({
    path: '/discover',
    query: {
      ...(keyword.value.trim() ? { keyword: keyword.value.trim() } : {}),
      ...(activeTag.value ? { tag: activeTag.value } : {})
    }
  })
}

const fetchWorks = async append => {
  append ? (loadingMore.value = true) : (loading.value = true)
  try {
    const data = await getPublicImages({
      page: page.value,
      pageSize,
      keyword: keyword.value.trim(),
      tag: activeTag.value,
      sort: sort.value
    })
    const list = data?.list || []
    works.value = append ? [...works.value, ...list] : list
    total.value = Number(data?.pagination?.total ?? data?.total ?? list.length)
  } catch {
    if (!append) works.value = []
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

const applyFilters = () => {
  page.value = 1
  syncQuery()
  fetchWorks(false)
}

const selectTag = tag => {
  activeTag.value = tag
  applyFilters()
}

const changeSort = value => {
  sort.value = value
  page.value = 1
  fetchWorks(false)
}

const loadMore = () => {
  page.value += 1
  fetchWorks(true)
}

watch(() => route.query, query => {
  keyword.value = String(query.keyword || '')
  activeTag.value = String(query.tag || '')
}, { deep: true })

onMounted(() => fetchWorks(false))
</script>

<style scoped>
.discover-page { padding-bottom: var(--space-8); }

.discover-header {
  display: grid;
  grid-template-columns: 1fr minmax(360px, 560px);
  align-items: end;
  gap: var(--space-8);
  margin-bottom: var(--space-8);
}

.discover-search { display: grid; grid-template-columns: 1fr auto; gap: var(--space-2); }

.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  margin-bottom: var(--space-6);
}

.tag-filters { display: flex; flex-wrap: wrap; gap: var(--space-2); }

.sort-switch {
  flex: none;
  display: flex;
  gap: 2px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 3px;
  background: var(--card);
}

.sort-switch button {
  border: 0;
  border-radius: var(--radius-sm);
  padding: 6px 12px;
  color: var(--text-secondary);
  background: transparent;
  cursor: pointer;
}

.sort-switch button.active { color: var(--primary-active); background: var(--primary-soft); }
.works-grid { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); align-items: start; gap: var(--space-4); }
.works-grid :deep(.post-card) { margin: 0; }
.load-more { display: flex; justify-content: center; padding: var(--space-6) 0; }

@media (max-width: 1180px) {
  .works-grid { grid-template-columns: repeat(4, minmax(0, 1fr)); }
}

@media (max-width: 860px) {
  .discover-header { grid-template-columns: 1fr; gap: var(--space-4); }
  .filter-bar { align-items: flex-start; flex-direction: column; }
  .works-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: var(--space-3); }
}
</style>
