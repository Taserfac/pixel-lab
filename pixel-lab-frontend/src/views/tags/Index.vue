<template>
  <div class="tags-page">
    <header class="page-heading">
      <div>
        <h1>{{ selectedTag ? `探索 #${selectedTag}` : '发现热门标签' }}</h1>
        <p>从热门主题和代表作品中，找到下一份创作灵感。</p>
      </div>
      <div class="tag-tabs" role="tablist" aria-label="标签分类">
        <button
          type="button"
          :class="{ active: activeTab === 'all' }"
          @click="activeTab = 'all'"
        >全部标签</button>
        <button
          type="button"
          :class="{ active: activeTab === 'system' }"
          @click="activeTab = 'system'"
        >官方标签</button>
        <button
          type="button"
          :class="{ active: activeTab === 'trending' }"
          @click="activeTab = 'trending'"
        >趋势标签</button>
      </div>
    </header>

    <div v-loading="loading" class="tag-grid">
      <article
        v-for="tag in displayedTags"
        :key="tag.name"
        class="tag-card"
        :class="{ selected: tag.name === selectedTag }"
      >
        <button class="tag-cover" type="button" @click="openTag(tag)">
          <img v-if="tag.representativeWorks[0]?.url" :src="tag.representativeWorks[0].url" :alt="tag.name">
          <span v-else>#</span>
          <em>#{{ tag.name }}</em>
        </button>

        <div class="tag-body">
          <div class="tag-identity">
            <div>
              <span>{{ tag.isSystem ? '官方标签' : '趋势标签' }}</span>
              <h2>#{{ tag.name }}</h2>
              <p>{{ tag.workCount }} 件公开作品</p>
            </div>
            <el-button plain @click="openTag(tag)">浏览作品</el-button>
          </div>

          <div class="tag-metrics">
            <span><strong>{{ formatNumber(tag.workCount) }}</strong>作品</span>
            <span><strong>{{ formatNumber(tag.likeCount) }}</strong>获赞</span>
            <span><strong>{{ formatNumber(tag.collectCount) }}</strong>收藏</span>
          </div>

          <div v-if="tag.representativeWorks.length" class="representative-works">
            <button
              v-for="work in tag.representativeWorks"
              :key="work.id || work.url"
              type="button"
              @click="openWork(work)"
            >
              <img :src="work.url" :alt="work.title || `${tag.name}代表作品`">
            </button>
          </div>
        </div>
      </article>
    </div>

    <EmptyState
      v-if="!loading && !displayedTags.length"
      title="暂时没有可展示的标签"
      description="公开作品积累到一定数量后，热门标签会出现在这里。"
      action-text="去社区发现"
      show-action
      @action="router.push('/community')"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import EmptyState from '@/components/common/EmptyState.vue'
import { getPublicImages, getPublicTags } from '@/api/community'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const tags = ref([])
const activeTab = ref('all')
const selectedTag = computed(() => String(route.query.tag || '').trim())

const number = value => Number(value || 0)
const normalizeTags = value => (
  Array.isArray(value) ? value : String(value || '').split(/[,，]/)
).map(tag => tag.trim().replace(/^#+/, '')).filter(Boolean)

const displayedTags = computed(() => {
  const filtered = tags.value.filter(tag => {
    if (activeTab.value === 'system') return tag.isSystem
    if (activeTab.value === 'trending') return tag.isTrending
    return true
  })

  return [...filtered].sort((a, b) => {
    if (a.name === selectedTag.value) return -1
    if (b.name === selectedTag.value) return 1
    return b.score - a.score
  })
})

const buildTagCards = (tagData, works) => {
  const systemNames = new Set(tagData.systemTags || [])
  const trendingByName = new Map(
    (tagData.trendingTags || []).map(tag => [tag.name, tag])
  )
  const allowedNames = [...new Set([...systemNames, ...trendingByName.keys()])]

  const cards = allowedNames.map(name => {
    const matchingWorks = works.filter(work => normalizeTags(work.tags).includes(name))
    const trending = trendingByName.get(name)
    return {
      name,
      isSystem: systemNames.has(name),
      isTrending: Boolean(trending),
      workCount: number(trending?.usageCount) || matchingWorks.length,
      likeCount: matchingWorks.reduce((sum, work) => sum + number(work.like_count), 0),
      collectCount: matchingWorks.reduce((sum, work) => sum + number(work.collect_count), 0),
      score: number(trending?.score),
      representativeWorks: [...matchingWorks]
        .sort((a, b) => number(b.like_count) - number(a.like_count))
        .slice(0, 3)
    }
  })

  const populated = cards.filter(tag => tag.workCount > 0 || tag.name === selectedTag.value)
  return populated.length ? populated : cards
}

const openTag = tag => router.push({ path: '/community', query: { keyword: tag.name } })
const openWork = work => router.push(`/post/${work.id}`)
const formatNumber = value => number(value).toLocaleString('zh-CN')

onMounted(async () => {
  loading.value = true
  try {
    const [tagData, worksData] = await Promise.all([
      getPublicTags({ limit: 50 }, { silent: true }).catch(() => ({ systemTags: [], trendingTags: [] })),
      getPublicImages({ page: 1, pageSize: 50, sortBy: 'popular' }, { silent: true }).catch(() => ({ list: [] }))
    ])
    tags.value = buildTagCards(tagData, worksData.list || [])
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.tags-page {
  width: min(1400px, 100%);
  margin: 0 auto;
}

.page-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--space-6);
  margin-bottom: var(--space-7);
}

.page-heading h1 {
  margin: 0;
  color: var(--foreground);
  font-size: clamp(30px, 4vw, 46px);
}

.page-heading p {
  margin-top: var(--space-2);
  color: var(--foreground-muted);
}

.tag-tabs {
  display: flex;
  gap: var(--space-2);
}

.tag-tabs button {
  border: 0;
  border-radius: var(--radius-full);
  background: transparent;
  color: var(--foreground-muted);
  padding: 9px 16px;
  font-weight: 600;
  cursor: pointer;
}

.tag-tabs button.active {
  background: var(--primary-muted);
  color: var(--primary);
}

.tag-grid {
  min-height: 360px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-5);
}

.tag-card {
  display: grid;
  grid-template-columns: 190px minmax(0, 1fr);
  overflow: hidden;
  border: 1px solid transparent;
  border-radius: var(--radius-lg);
  background: var(--background-card);
  box-shadow: var(--shadow-sm);
  transition: transform 0.2s ease, border-color 0.2s ease;
}

.tag-card:hover,
.tag-card.selected {
  border-color: color-mix(in srgb, var(--primary) 44%, transparent);
  transform: translateY(-2px);
}

.tag-cover {
  min-height: 260px;
  position: relative;
  overflow: hidden;
  border: 0;
  background: var(--primary-muted);
  padding: 0;
  cursor: pointer;
}

.tag-cover::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 45%, rgba(0, 0, 0, 0.68));
}

.tag-cover img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.tag-cover > span {
  height: 100%;
  display: grid;
  place-items: center;
  color: var(--primary);
  font-size: 72px;
  font-weight: 800;
}

.tag-cover em {
  position: absolute;
  z-index: 1;
  left: var(--space-4);
  right: var(--space-4);
  bottom: var(--space-4);
  overflow: hidden;
  color: white;
  font-size: 18px;
  font-style: normal;
  font-weight: 700;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tag-body {
  min-width: 0;
  display: flex;
  flex-direction: column;
  padding: var(--space-5);
}

.tag-identity {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}

.tag-identity > div > span {
  color: var(--primary);
  font-size: 12px;
  font-weight: 700;
}

.tag-identity h2 { margin: 3px 0 0; color: var(--foreground); font-size: 19px; }
.tag-identity p { margin-top: 4px; color: var(--foreground-muted); font-size: 12px; }

.tag-metrics {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  margin: var(--space-5) 0;
}

.tag-metrics span {
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--border-light);
  color: var(--foreground-muted);
  font-size: 12px;
}

.tag-metrics span:last-child { border-right: 0; padding-left: var(--space-3); }
.tag-metrics span:nth-child(2) { padding-left: var(--space-3); }
.tag-metrics strong { color: var(--foreground); font-size: 18px; }

.representative-works {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-2);
  margin-top: auto;
}

.representative-works button {
  aspect-ratio: 1;
  overflow: hidden;
  border: 0;
  border-radius: var(--radius-sm);
  background: var(--background-muted);
  padding: 0;
  cursor: pointer;
}

.representative-works img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

@media (max-width: 1050px) {
  .tag-grid { grid-template-columns: 1fr; }
}

@media (max-width: 640px) {
  .page-heading { align-items: flex-start; flex-direction: column; }
  .tag-tabs { max-width: 100%; overflow-x: auto; }
  .tag-tabs button { flex: 0 0 auto; }
  .tag-card { grid-template-columns: 1fr; }
  .tag-cover { min-height: 210px; max-height: 260px; }
}
</style>
