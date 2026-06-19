<template>
  <div class="tutorial-page">
    <section class="tutorial-hero">
      <div>
        <span class="eyebrow">Art Academy</span>
        <h1>教学专区</h1>
        <p>精选提升美术造诣的视频课程，按观察、造型、色彩、构图、人体、艺术史和叙事能力组织学习路径。</p>
      </div>
      <div class="hero-metrics">
        <span><strong>{{ total }}</strong> 个视频</span>
        <span><strong>{{ categories.length }}</strong> 个分区</span>
      </div>
    </section>

    <section class="tutorial-toolbar">
      <div class="category-tabs">
        <button
          v-for="item in displayCategories"
          :key="item.category"
          type="button"
          :class="{ active: activeCategory === item.category }"
          @click="selectCategory(item.category)"
        >
          {{ item.category }}
          <span>{{ item.count }}</span>
        </button>
      </div>
      <el-input
        v-model="keyword"
        class="tutorial-search"
        clearable
        placeholder="搜索美术能力、作者、关键词"
        :prefix-icon="Search"
        @keyup.enter="fetchItems(true)"
        @clear="fetchItems(true)"
      />
    </section>

    <section
      v-loading="loading"
      class="tutorial-grid"
    >
      <article
        v-for="item in items"
        :key="item.id || item.source_url"
        class="tutorial-card"
      >
        <button
          type="button"
          class="tutorial-cover"
          @click="openItem(item)"
        >
          <img
            v-if="item.cover_url && !brokenCovers.has(item.source_url)"
            :src="normalizeCover(item.cover_url)"
            :alt="item.title"
            loading="lazy"
            referrerpolicy="no-referrer"
            @error="markCoverBroken(item)"
          >
          <span
            v-else
            class="cover-fallback"
          >
            <strong>{{ item.category }}</strong>
            <small>视频课程</small>
          </span>
          <span class="play-badge">
            <el-icon><VideoPlay /></el-icon>
          </span>
          <span
            v-if="item.duration"
            class="duration"
          >{{ item.duration }}</span>
        </button>
        <div class="tutorial-body">
          <div class="tutorial-meta">
            <span>{{ item.category }}</span>
            <span>{{ item.source_name || '公开来源' }}</span>
          </div>
          <h2>{{ item.title }}</h2>
          <p v-if="item.description">{{ stripHtml(item.description) }}</p>
          <div class="tutorial-footer">
            <span>{{ item.author_name || '创作者' }}</span>
            <span>{{ formatViews(item.view_count) }} 播放</span>
          </div>
        </div>
      </article>

      <EmptyState
        v-if="!loading && items.length === 0"
        title="暂无视频"
        description="运行爬虫脚本导入美术造诣类公开视频元数据后，这里会按分区展示。"
      />
    </section>

    <div
      v-if="items.length < total"
      class="load-more"
    >
      <el-button
        :loading="loadingMore"
        @click="loadMore"
      >
        加载更多
      </el-button>
    </div>

    <el-dialog
      v-model="viewerVisible"
      :title="currentItem?.title || '教学视频'"
      width="920px"
      class="tutorial-viewer"
    >
      <div
        v-if="currentItem"
        class="viewer-layout"
      >
        <iframe
          v-if="currentItem.embed_url"
          :src="currentItem.embed_url"
          allowfullscreen
          sandbox="allow-same-origin allow-scripts allow-popups allow-forms"
        />
        <a
          v-else
          class="external-preview"
          :href="currentItem.source_url"
          target="_blank"
          rel="noopener noreferrer"
        >
          <img
            v-if="currentItem.cover_url && !brokenCovers.has(currentItem.source_url)"
            :src="normalizeCover(currentItem.cover_url)"
            :alt="currentItem.title"
            referrerpolicy="no-referrer"
          >
          <span>打开原视频</span>
        </a>
        <div class="viewer-info">
          <span class="viewer-category">{{ currentItem.category }}</span>
          <h3>{{ currentItem.title }}</h3>
          <p v-if="currentItem.description">{{ stripHtml(currentItem.description) }}</p>
          <a
            :href="currentItem.source_url"
            target="_blank"
            rel="noopener noreferrer"
          >
            前往原平台
          </a>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { Search, VideoPlay } from '@element-plus/icons-vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { getTutorialCategories, getTutorialVideos } from '@/api/tutorials'

const fallbackCategories = [
  { category: '全部', count: 0 },
  { category: '造型基础', count: 0 },
  { category: '素描观察', count: 0 },
  { category: '色彩修养', count: 0 },
  { category: '构图审美', count: 0 },
  { category: '光影关系', count: 0 },
  { category: '透视空间', count: 0 },
  { category: '人体结构', count: 0 },
  { category: '动态速写', count: 0 },
  { category: '材质肌理', count: 0 },
  { category: '大师临摹', count: 0 },
  { category: '艺术史鉴赏', count: 0 },
  { category: '视觉叙事', count: 0 }
]

const items = ref([])
const categories = ref([])
const activeCategory = ref('全部')
const keyword = ref('')
const page = ref(1)
const pageSize = ref(12)
const total = ref(0)
const loading = ref(false)
const loadingMore = ref(false)
const viewerVisible = ref(false)
const currentItem = ref(null)
const brokenCovers = ref(new Set())

const displayCategories = computed(() => {
  const totalCount = categories.value.reduce((sum, item) => sum + Number(item.count || 0), 0)
  const source = categories.value.length ? categories.value : fallbackCategories.slice(1)
  return [{ category: '全部', count: totalCount }, ...source]
})

const fetchCategories = async () => {
  try {
    categories.value = await getTutorialCategories({ silent: true })
  } catch {
    categories.value = fallbackCategories.slice(1)
  }
}

const fetchItems = async (reset = false) => {
  if (reset) {
    page.value = 1
    items.value = []
  }
  const state = reset ? loading : loadingMore
  state.value = true
  try {
    const res = await getTutorialVideos({
      page: page.value,
      pageSize: pageSize.value,
      category: activeCategory.value === '全部' ? '' : activeCategory.value,
      keyword: keyword.value.trim()
    }, { silent: true })
    items.value = reset ? (res.list || []) : [...items.value, ...(res.list || [])]
    total.value = res.total || 0
  } finally {
    state.value = false
  }
}

const selectCategory = (category) => {
  activeCategory.value = category
  fetchItems(true)
}

const loadMore = () => {
  page.value += 1
  fetchItems(false)
}

const openItem = (item) => {
  currentItem.value = item
  viewerVisible.value = true
}

const markCoverBroken = (item) => {
  brokenCovers.value = new Set([...brokenCovers.value, item.source_url])
}

const stripHtml = (value = '') => String(value).replace(/<[^>]+>/g, '').trim()

const normalizeCover = (value = '') => {
  if (value.startsWith('//')) return `https:${value}`
  return value
}

const formatViews = (value) => {
  const number = Number(value || 0)
  if (number >= 10000) return `${(number / 10000).toFixed(1)}万`
  return number.toLocaleString('zh-CN')
}

onMounted(async () => {
  await Promise.all([
    fetchCategories(),
    fetchItems(true)
  ])
})
</script>

<style scoped>
.tutorial-page {
  width: min(1480px, 100%);
  margin: 0 auto;
}

.tutorial-hero {
  min-height: 260px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-8);
  padding: clamp(28px, 5vw, 64px);
  border-radius: var(--radius-xl);
  background:
    radial-gradient(circle at 86% 18%, rgba(91, 141, 239, 0.18), transparent 28%),
    linear-gradient(135deg, rgba(22, 199, 132, 0.18), rgba(255, 255, 255, 0.78) 52%, rgba(255, 180, 84, 0.15));
  box-shadow: var(--shadow-sm);
}

.eyebrow {
  color: var(--primary);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0;
  text-transform: uppercase;
}

.tutorial-hero h1 {
  margin: var(--space-3) 0;
  color: var(--foreground);
  font-size: clamp(36px, 6vw, 64px);
  line-height: 1.05;
}

.tutorial-hero p {
  max-width: 680px;
  margin: 0;
  color: var(--foreground-muted);
  font-size: 16px;
  line-height: 1.75;
}

.hero-metrics {
  min-width: 220px;
  display: grid;
  gap: var(--space-3);
}

.hero-metrics span {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-4);
  border-radius: var(--radius-md);
  background: var(--background-float);
}

.hero-metrics strong {
  color: var(--primary);
  font-size: 28px;
}

.tutorial-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  margin: var(--space-6) 0;
}

.category-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.category-tabs button {
  min-height: 42px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 0 16px;
  border: 0;
  border-radius: var(--radius-full);
  background: var(--background-card);
  color: var(--foreground-muted);
  font-weight: 700;
  cursor: pointer;
  box-shadow: var(--shadow-sm);
}

.category-tabs button.active {
  background: var(--primary);
  color: white;
}

.category-tabs span {
  font-size: 12px;
  opacity: 0.76;
}

.tutorial-search {
  width: min(360px, 100%);
}

.tutorial-grid {
  min-height: 300px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-5);
}

.tutorial-card {
  min-width: 0;
  overflow: hidden;
  border-radius: var(--radius-lg);
  background: var(--background-card);
  box-shadow: var(--shadow-sm);
}

.tutorial-cover {
  width: 100%;
  aspect-ratio: 16 / 9;
  position: relative;
  display: block;
  padding: 0;
  overflow: hidden;
  border: 0;
  background: var(--background-muted);
  cursor: pointer;
}

.tutorial-cover img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
  transition: transform var(--transition-base);
}

.tutorial-card:hover .tutorial-cover img {
  transform: scale(1.04);
}

.cover-fallback {
  height: 100%;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 8px;
  color: var(--primary);
  background:
    radial-gradient(circle at 24% 24%, rgba(22, 199, 132, 0.18), transparent 32%),
    linear-gradient(135deg, rgba(247, 250, 248, 0.96), rgba(230, 238, 234, 0.9));
}

.cover-fallback strong {
  font-size: 22px;
  font-weight: 900;
}

.cover-fallback small {
  color: var(--foreground-muted);
  font-size: 12px;
  font-weight: 800;
}

.play-badge {
  width: 46px;
  height: 46px;
  position: absolute;
  left: 50%;
  top: 50%;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.9);
  color: var(--primary);
  transform: translate(-50%, -50%);
  box-shadow: var(--shadow-sm);
}

.duration {
  position: absolute;
  right: 10px;
  bottom: 10px;
  padding: 3px 7px;
  border-radius: var(--radius-sm);
  background: rgba(20, 26, 23, 0.72);
  color: white;
  font-size: 12px;
}

.tutorial-body {
  display: grid;
  gap: var(--space-2);
  padding: var(--space-4);
}

.tutorial-meta,
.tutorial-footer {
  display: flex;
  justify-content: space-between;
  gap: var(--space-3);
  color: var(--foreground-subtle);
  font-size: 12px;
}

.tutorial-meta span:first-child {
  color: var(--primary);
  font-weight: 800;
}

.tutorial-body h2 {
  min-height: 48px;
  margin: 0;
  color: var(--foreground);
  font-size: 16px;
  line-height: 1.5;
}

.tutorial-body p {
  display: -webkit-box;
  min-height: 40px;
  margin: 0;
  overflow: hidden;
  color: var(--foreground-muted);
  font-size: 13px;
  line-height: 1.55;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.load-more {
  display: flex;
  justify-content: center;
  margin: var(--space-6) 0;
}

.viewer-layout {
  display: grid;
  gap: var(--space-4);
}

.viewer-layout iframe,
.external-preview {
  width: 100%;
  aspect-ratio: 16 / 9;
  border: 0;
  border-radius: var(--radius-md);
  background: #111;
}

.external-preview {
  position: relative;
  display: grid;
  place-items: center;
  overflow: hidden;
  color: white;
  text-decoration: none;
}

.external-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0.62;
}

.external-preview span {
  position: absolute;
  padding: 10px 16px;
  border-radius: var(--radius-full);
  background: var(--primary);
  font-weight: 800;
}

.viewer-info {
  display: grid;
  gap: var(--space-2);
}

.viewer-category {
  width: fit-content;
  padding: 4px 10px;
  border-radius: var(--radius-full);
  background: var(--primary-muted);
  color: var(--primary);
  font-size: 12px;
  font-weight: 800;
}

.viewer-info h3,
.viewer-info p {
  margin: 0;
}

.viewer-info p {
  color: var(--foreground-muted);
  line-height: 1.7;
}

.viewer-info a {
  width: fit-content;
  color: var(--primary);
  font-weight: 800;
}

@media (max-width: 1100px) {
  .tutorial-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .tutorial-hero,
  .tutorial-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .tutorial-grid {
    grid-template-columns: 1fr;
  }

  .tutorial-search {
    width: 100%;
  }
}
</style>
