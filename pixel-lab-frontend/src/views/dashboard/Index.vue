<template>
  <div class="home-page">
    <div class="home-content">
      <div class="primary-column">
        <section class="home-hero">
          <div class="hero-copy">
            <h1>{{ $t('dashboard.title') }}</h1>
            <p>{{ $t('dashboard.subtitle') }}</p>
            <div class="hero-actions">
              <el-button
                type="primary"
                size="large"
                @click="router.push('/personal')"
              >
                <el-icon><Upload /></el-icon>
                {{ $t('dashboard.uploadWork') }}
              </el-button>
              <el-button
                size="large"
                @click="router.push('/community')"
              >
                <el-icon><Compass /></el-icon>
                {{ $t('action.browseCommunity') }}
              </el-button>
            </div>
          </div>

          <div class="hero-preview" aria-hidden="true">
            <div
              v-for="(work, index) in previewWorks"
              :key="work.id || index"
              class="preview-card"
              :class="`preview-card-${index + 1}`"
            >
              <img
                v-if="work.url"
                :src="work.url"
                :alt="work.title || work.original_name || $t('dashboard.previewLabel')"
              >
            </div>
            <span
              v-for="(label, idx) in heroLabels"
              :key="label"
              class="hero-label"
              :class="heroLabelClasses[idx]"
            >{{ label }}</span>
            <span class="hero-like">
              <el-icon><Star /></el-icon>
            </span>
          </div>
        </section>

        <main class="feed-column">
          <div class="section-heading">
            <div>
              <h2>{{ $t('dashboard.featuredWorks') }}</h2>
              <p>图片优先的瀑布流浏览体验</p>
            </div>
            <el-button
              text
              @click="router.push('/community')"
            >
              {{ $t('action.viewMore') }} <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>

          <div class="feed-tabs" aria-label="作品分类">
            <button
              v-for="tab in feedTabs"
              :key="tab"
              type="button"
              :class="{ active: activeFeedTab === tab }"
              @click="activeFeedTab = tab"
            >
              {{ tab }}
            </button>
          </div>

          <div
            v-loading="loading"
            class="masonry-feed"
          >
            <template v-if="loading && !works.length">
              <SkeletonCard
                v-for="i in 8"
                :key="'skeleton-' + i"
                :style="{ '--post-ratio': getCardRatio(i - 1) }"
              />
            </template>
            <template v-else>
              <PostCard
                v-for="(work, index) in feedWorks"
                :key="work.id"
                :work="work"
                :style="{ '--post-ratio': getCardRatio(index) }"
                @select="openWork"
              />
            </template>
          </div>

          <EmptyState
            v-if="!loading && works.length > 0 && feedWorks.length === 0"
            :title="$t('dashboard.noPublicWorks')"
            :description="$t('dashboard.noPublicWorksDesc')"
            :action-text="$t('action.goToUpload')"
            show-action
            @action="router.push('/personal')"
          />
        </main>
      </div>

      <aside class="discovery-rail">
        <section class="rail-card creation-card">
          <div class="rail-title">
            <h3>
              <el-icon><Star /></el-icon>
              {{ $t('creation.creationCenter') }}
            </h3>
          </div>
          <div class="creation-list">
            <button
              v-for="action in creationActions"
              :key="action.label"
              type="button"
              class="creation-action"
              :class="`creation-${action.tone}`"
              @click="handleCreationAction(action)"
            >
              <span class="creation-icon">
                <el-icon>
                  <component :is="action.icon" />
                </el-icon>
              </span>
              <span>
                <strong>{{ action.label }}</strong>
                <small>{{ action.description }}</small>
              </span>
              <el-icon><ArrowRight /></el-icon>
            </button>
          </div>
        </section>

        <section class="rail-card tag-card">
          <div class="rail-title">
            <h3>{{ $t('dashboard.hotTags') }}</h3>
            <button type="button" @click="router.push('/community')">
              {{ $t('dashboard.more') }} <el-icon><ArrowRight /></el-icon>
            </button>
          </div>
          <div class="tag-list">
            <button
              v-for="tag in popularTags"
              :key="tag.name"
              type="button"
              class="tag-chip"
              :class="`tag-${tag.tone}`"
              @click="searchTag(tag)"
            >
              <span>#{{ tag.name }}</span>
              <strong>{{ tag.count }}</strong>
            </button>
          </div>
        </section>

        <section class="rail-card creator-card">
          <div class="rail-title">
            <h3>{{ $t('dashboard.recommendedCreators') }}</h3>
            <button type="button" @click="router.push('/community')">
              {{ $t('dashboard.more') }} <el-icon><ArrowRight /></el-icon>
            </button>
          </div>
          <div
            v-if="recommendedCreators.length"
            class="creator-grid"
          >
            <article
              v-for="creator in recommendedCreators"
              :key="creator.name"
              class="creator-item"
            >
              <el-avatar
                :size="46"
                :src="creator.avatar"
              >
                {{ creator.name.charAt(0) }}
              </el-avatar>
              <div class="creator-copy">
                <strong>{{ creator.name }}</strong>
                <span>{{ creator.role || $t('dashboard.worksCount', { count: creator.count }) }}</span>
              </div>
              <button
                type="button"
                @click="router.push('/community')"
              >
                {{ $t('action.follow') }}
              </button>
            </article>
          </div>
          <div
            v-else
            class="rail-empty"
          >
            {{ $t('dashboard.noRecommendedCreators') }}
          </div>
        </section>

        <section class="rail-card stats-card">
          <div class="rail-title">
            <h3>{{ $t('dashboard.communityStats') }}</h3>
            <button type="button" @click="router.push('/community')">
              {{ $t('dashboard.more') }} <el-icon><ArrowRight /></el-icon>
            </button>
          </div>
          <div class="community-stat-list">
            <div
              v-for="stat in communityStatItems"
              :key="stat.label"
              class="community-stat"
              :class="`community-${stat.tone}`"
            >
              <el-icon>
                <component :is="stat.icon" />
              </el-icon>
              <strong>{{ formatNumber(stat.value) }}</strong>
              <span>{{ stat.label }}</span>
            </div>
          </div>
        </section>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  ArrowRight,
  ChatDotRound,
  CollectionTag,
  Compass,
  EditPen,
  FolderOpened,
  Picture,
  Star,
  Upload,
  User
} from '@element-plus/icons-vue'
import EmptyState from '@/components/common/EmptyState.vue'
import SkeletonCard from '@/components/common/SkeletonCard.vue'
import PostCard from '@/components/community/PostCard.vue'
import { getUserStats } from '@/api/auth'
import { getActivities, getPublicImages } from '@/api/community'

const router = useRouter()
const { t } = useI18n()
const loading = ref(false)
const works = ref([])
const activities = ref([])
const activeFeedTab = ref('推荐')
const userStats = ref({
  works: 0,
  likes: 0,
  views: 0
})

const defaultFeedTabs = ['推荐', '最新', '关注', '插画', '摄影', '设计', '像素艺术', 'AI艺术']
const defaultPopularTags = [
  { name: '插画', count: '12.4万', tone: 'green' },
  { name: '摄影', count: '8.7万', tone: 'purple' },
  { name: 'UI设计', count: '7.9万', tone: 'blue' },
  { name: '像素艺术', count: '5.6万', tone: 'rose' },
  { name: 'AI艺术', count: '4.2万', tone: 'orange' }
]
const tagTones = ['green', 'purple', 'blue', 'rose', 'orange']
const cardRatios = ['4 / 5', '1 / 1', '5 / 4', '3 / 4', '4 / 3']
const sampleWorks = [
  {
    id: 'sample-1',
    title: '雾野织网',
    url: '/sample-images/test_1.jpg',
    author_name: '鹿与森',
    author_avatar: '/sample-images/test_5.jpg',
    like_count: 1200,
    comment_count: 86,
    collect_count: 300,
    tags: ['摄影'],
    isSample: true
  },
  {
    id: 'sample-2',
    title: '垂直城市',
    url: '/sample-images/test_2.jpg',
    author_name: '阿诺德',
    author_avatar: '/sample-images/test_3.jpg',
    like_count: 932,
    comment_count: 54,
    collect_count: 210,
    tags: ['摄影', '设计'],
    isSample: true
  },
  {
    id: 'sample-3',
    title: '光环窗口',
    url: '/sample-images/test_3.jpg',
    author_name: 'DesignLin',
    author_avatar: '/sample-images/test_2.jpg',
    like_count: 1500,
    comment_count: 67,
    collect_count: 420,
    tags: ['UI设计', '设计'],
    isSample: true
  },
  {
    id: 'sample-4',
    title: '山林午后',
    url: '/sample-images/test_4.jpg',
    author_name: 'PixelCat',
    author_avatar: '/sample-images/test_1.jpg',
    like_count: 889,
    comment_count: 42,
    collect_count: 190,
    tags: ['插画'],
    isSample: true
  },
  {
    id: 'sample-5',
    title: '紫色花园',
    url: '/sample-images/test_5.jpg',
    author_name: '星海贝壳',
    author_avatar: '/sample-images/test_5.jpg',
    like_count: 760,
    comment_count: 35,
    collect_count: 168,
    tags: ['插画', 'AI艺术'],
    isSample: true
  }
]
const sampleCreators = [
  { name: '鹿与森', role: '插画师', avatar: '/sample-images/test_5.jpg', count: 12 },
  { name: '阿诺德', role: '摄影师', avatar: '/sample-images/test_3.jpg', count: 9 },
  { name: 'DesignLin', role: 'UI/UX 设计师', avatar: '/sample-images/test_2.jpg', count: 15 },
  { name: 'PixelCat', role: '像素艺术家', avatar: '/sample-images/test_1.jpg', count: 8 }
]
const creationActions = computed(() => [
  { label: t('creation.uploadWork'), description: t('creation.uploadWorkDesc'), icon: Upload, tone: 'green', path: '/personal' },
  { label: t('creation.onlineCreate'), description: t('creation.onlineCreateDesc'), icon: EditPen, tone: 'blue', path: '/draw' },
  { label: t('creation.workbench'), description: t('creation.workbenchDesc'), icon: Picture, tone: 'purple', path: '/workbench' },
  { label: t('creation.myWorks'), description: t('creation.myWorksDesc'), icon: FolderOpened, tone: 'orange', path: '/personal' }
])

const toNumber = (value) => Number(value || 0)

const normalizeStats = (stats = {}) => ({
  works: toNumber(stats.works ?? stats.imageCount),
  likes: toNumber(stats.likes ?? stats.receivedLikeCount ?? stats.likeCount),
  views: toNumber(stats.views ?? stats.viewCount)
})

const sourceWorks = computed(() => works.value.length ? works.value : sampleWorks)

const feedTabs = computed(() => {
  if (!works.value.length) return defaultFeedTabs
  const tagSet = new Set()
  works.value.forEach(work => {
    (work.tags || []).forEach(tag => tagSet.add(tag))
  })
  const dynamicTabs = [...tagSet]
  return ['推荐', '最新', '关注', ...dynamicTabs.filter(t => !['推荐', '最新', '关注'].includes(t))]
})

const popularTags = computed(() => {
  if (!works.value.length) return defaultPopularTags
  const tagCount = {}
  works.value.forEach(work => {
    (work.tags || []).forEach(tag => {
      tagCount[tag] = (tagCount[tag] || 0) + 1
    })
  })
  return Object.entries(tagCount)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 5)
    .map(([name, count], i) => ({
      name,
      count: count >= 10000 ? `${(count / 10000).toFixed(1)}万` : String(count),
      tone: tagTones[i % tagTones.length]
    }))
})

const heroLabelClasses = ['hero-label-illustration', 'hero-label-photo', 'hero-label-pixel']
const heroLabels = computed(() => {
  const labels = []
  const seen = new Set()
  for (const work of previewWorks.value) {
    for (const tag of (work.tags || [])) {
      if (!seen.has(tag)) {
        seen.add(tag)
        labels.push(tag)
        if (labels.length >= 3) return labels
      }
    }
  }
  return labels.length ? labels : ['插画', '摄影', '像素艺术']
})
const feedWorks = computed(() => {
  const source = sourceWorks.value
  if (activeFeedTab.value === '推荐') return source
  if (activeFeedTab.value === '最新') return [...source].reverse()
  if (activeFeedTab.value === '关注') return source.slice(0, 4)

  const matched = source.filter((work) => {
    const tags = work.tags || []
    const haystack = `${work.title || ''} ${work.description || ''} ${work.original_name || ''}`
    return tags.includes(activeFeedTab.value) || haystack.includes(activeFeedTab.value)
  })

  return matched.length ? matched : source
})
const previewWorks = computed(() => {
  const source = sourceWorks.value.slice(0, 4)
  if (source.length) return source
  return sampleWorks.slice(0, 4)
})

const recommendedCreators = computed(() => {
  const creatorMap = new Map()

  feedWorks.value.forEach((work) => {
    const name = work.author_name || work.nickname
    if (!name) return
    const cached = creatorMap.get(name) || {
      name,
      avatar: work.author_avatar || '',
      count: 0
    }
    cached.count += 1
    creatorMap.set(name, cached)
  })

  activities.value.forEach((activity) => {
    const name = activity.author_name
    if (!name || creatorMap.has(name)) return
    creatorMap.set(name, {
      name,
      avatar: activity.author_avatar || '',
      role: activity.action || '社区创作者',
      count: 1
    })
  })

  const creators = Array.from(creatorMap.values()).slice(0, 4)
  return creators.length ? creators : sampleCreators
})

const communityStatItems = computed(() => {
  if (!works.value.length) {
    return [
      { label: t('dashboard.todayHot'), value: 124000, icon: Star, tone: 'green' },
      { label: t('dashboard.activeCreators'), value: 8632, icon: User, tone: 'blue' },
      { label: t('dashboard.hotWorks'), value: 247000, icon: CollectionTag, tone: 'orange' }
    ]
  }

  const likes = works.value.reduce((sum, work) => sum + toNumber(work.like_count), 0)
  const comments = works.value.reduce((sum, work) => sum + toNumber(work.comment_count), 0)
  const creatorCount = new Set(works.value.map(work => work.author_name || work.nickname).filter(Boolean)).size

  return [
    { label: t('dashboard.todayHot'), value: likes || userStats.value.likes, icon: Star, tone: 'green' },
    { label: t('dashboard.activeCreators'), value: creatorCount || recommendedCreators.value.length, icon: User, tone: 'blue' },
    { label: t('dashboard.communityDiscussion'), value: comments, icon: ChatDotRound, tone: 'orange' }
  ]
})

const getCardRatio = (index) => cardRatios[index % cardRatios.length]

const formatNumber = (value) => {
  const number = toNumber(value)
  if (number >= 10000) return `${(number / 10000).toFixed(1)}万`
  if (number >= 1000) return number.toLocaleString('zh-CN')
  return number.toLocaleString('zh-CN')
}

const openWork = (work) => {
  if (work.isSample) {
    router.push('/community')
    return
  }
  router.push({ path: '/community', query: { id: work.id } })
}

const searchTag = (tag) => {
  router.push({ path: '/community', query: { keyword: tag.name || tag } })
}

const handleCreationAction = (action) => {
  router.push(action.path)
}

onMounted(async () => {
  loading.value = true
  try {
    const [imagesRes, statsRes, activitiesRes] = await Promise.all([
      getPublicImages({ page: 1, pageSize: 16, sortBy: 'popular' }, { silent: true }).catch(() => ({ list: [] })),
      getUserStats({}, { silent: true }).catch(() => ({ works: 0, likes: 0, views: 0 })),
      getActivities({ limit: 8 }, { silent: true }).catch(() => [])
    ])

    works.value = imagesRes.list || []
    userStats.value = normalizeStats(statsRes)
    activities.value = activitiesRes || []
  } catch (error) {
    console.error('获取首页数据失败:', error)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.home-page {
  width: min(1820px, 100%);
  margin: 0 auto;
}

.home-hero {
  min-height: 280px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 430px);
  align-items: center;
  gap: clamp(var(--space-6), 5vw, var(--space-12));
  overflow: hidden;
  position: relative;
  border-radius: var(--radius-xl);
  background:
    linear-gradient(135deg, rgba(22, 199, 132, 0.14), rgba(91, 141, 239, 0.1)),
    var(--background-card);
  box-shadow: var(--shadow);
  padding: clamp(var(--space-8), 5vw, var(--space-12));
}

.hero-copy h1 {
  color: var(--foreground);
  font-size: clamp(34px, 5vw, 56px);
  font-weight: 800;
  line-height: 1.05;
  letter-spacing: 0;
}

.hero-copy p {
  max-width: 460px;
  margin-top: var(--space-4);
  color: var(--foreground-muted);
  font-size: 16px;
  line-height: 1.8;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  margin-top: var(--space-8);
}

.hero-preview {
  min-height: 230px;
  position: relative;
}

.preview-card {
  position: absolute;
  overflow: hidden;
  border-radius: var(--radius-lg);
  background:
    linear-gradient(135deg, rgba(22, 199, 132, 0.24), rgba(91, 141, 239, 0.2)),
    var(--background-muted);
  box-shadow: var(--shadow-md);
  transform: rotate(var(--preview-rotate, 0deg));
  transition: transform var(--transition-base);
}

.preview-card img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.preview-card-1 {
  --preview-rotate: -4deg;
  width: 32%;
  aspect-ratio: 4 / 5;
  left: 0;
  bottom: 5%;
  z-index: 2;
}

.preview-card-2 {
  --preview-rotate: 2deg;
  width: 56%;
  aspect-ratio: 4 / 3;
  left: 26%;
  top: 3%;
  z-index: 3;
}

.preview-card-3 {
  --preview-rotate: 4deg;
  width: 34%;
  aspect-ratio: 5 / 4;
  right: 0;
  top: 0;
  z-index: 2;
}

.preview-card-4 {
  --preview-rotate: 5deg;
  width: 38%;
  aspect-ratio: 5 / 4;
  right: 3%;
  bottom: 0;
  z-index: 4;
}

.hero-label,
.hero-like {
  position: absolute;
  z-index: 5;
  border-radius: var(--radius-full);
  box-shadow: var(--shadow-sm);
  font-size: 12px;
  font-weight: 700;
}

.hero-label {
  padding: 6px 12px;
}

.hero-label-illustration {
  top: 26%;
  left: 28%;
  background: rgba(22, 199, 132, 0.22);
  color: var(--primary);
}

.hero-label-photo {
  top: 16%;
  right: 0;
  background: rgba(91, 141, 239, 0.2);
  color: var(--secondary);
}

.hero-label-pixel {
  right: 10%;
  bottom: -4px;
  background: rgba(255, 138, 179, 0.24);
  color: #D94980;
}

.hero-like {
  left: 34%;
  bottom: 1%;
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.92);
  color: var(--error);
}

.home-content {
  display: grid;
  grid-template-columns: minmax(0, 1fr) clamp(360px, 28vw, 500px);
  gap: var(--space-8);
  align-items: start;
}

.primary-column {
  min-width: 0;
  display: grid;
  gap: var(--space-8);
}

.section-heading,
.rail-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
}

.section-heading {
  margin-bottom: var(--space-5);
}

.section-heading h2,
.rail-title h3 {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--foreground);
  font-size: 18px;
  font-weight: 800;
}

.section-heading p,
.rail-title span {
  margin-top: 2px;
  color: var(--foreground-muted);
  font-size: 12px;
}

.rail-title button {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  border: 0;
  background: transparent;
  color: var(--foreground-muted);
  font-size: 12px;
  cursor: pointer;
}

.rail-title button:hover {
  color: var(--primary);
}

.feed-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-bottom: var(--space-5);
}

.feed-tabs button {
  border: 0;
  border-radius: var(--radius-full);
  background: transparent;
  color: var(--foreground-muted);
  padding: 8px 14px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition:
    color var(--transition-fast),
    background var(--transition-fast);
}

.feed-tabs button:hover,
.feed-tabs button.active {
  color: var(--primary);
  background: var(--primary-muted);
}

.masonry-feed {
  min-height: 360px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--space-5);
}

.masonry-feed :deep(.post-card) {
  margin: 0;
}

.discovery-rail {
  display: grid;
  gap: var(--space-5);
  position: sticky;
  top: 96px;
}

.rail-card {
  border-radius: var(--radius-lg);
  background: var(--background-card);
  box-shadow: var(--shadow-sm);
  padding: var(--space-5);
}

.creation-list {
  display: grid;
  gap: var(--space-3);
  margin-top: var(--space-4);
}

.creation-action {
  width: 100%;
  min-height: 58px;
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) 18px;
  align-items: center;
  gap: var(--space-3);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--background-elevated);
  color: var(--foreground);
  padding: var(--space-2) var(--space-3);
  cursor: pointer;
  text-align: left;
  transition:
    border-color var(--transition-fast),
    box-shadow var(--transition-fast),
    transform var(--transition-fast);
}

.creation-action:hover {
  border-color: var(--border-hover);
  box-shadow: var(--shadow-sm);
  transform: translateY(-1px);
}

.creation-icon {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border-radius: var(--radius-md);
  background: var(--primary-muted);
  color: var(--primary);
  font-size: 20px;
}

.creation-blue .creation-icon {
  background: var(--secondary-muted);
  color: var(--secondary);
}

.creation-purple .creation-icon {
  background: rgba(139, 92, 246, 0.12);
  color: #7C5AEF;
}

.creation-orange .creation-icon {
  background: var(--accent-muted);
  color: var(--accent);
}

.creation-action strong,
.creation-action small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.creation-action strong {
  font-size: 14px;
  font-weight: 700;
}

.creation-action small {
  margin-top: 2px;
  color: var(--foreground-muted);
  font-size: 12px;
}

.tag-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-2);
  margin-top: var(--space-4);
}

.tag-chip {
  border: 0;
  border-radius: var(--radius-sm);
  background: var(--background-muted);
  color: var(--foreground-muted);
  padding: 8px 10px;
  font-size: 11px;
  cursor: pointer;
  text-align: left;
  transition:
    color var(--transition-fast),
    background var(--transition-fast),
    transform var(--transition-fast);
}

.tag-chip span,
.tag-chip strong {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tag-chip strong {
  margin-top: 2px;
  font-weight: 600;
}

.tag-chip:hover {
  color: var(--primary);
  background: var(--primary-muted);
  transform: translateY(-1px);
}

.tag-green {
  background: rgba(22, 199, 132, 0.1);
  color: var(--primary);
}

.tag-purple {
  background: rgba(139, 92, 246, 0.1);
  color: #7C5AEF;
}

.tag-blue {
  background: rgba(91, 141, 239, 0.12);
  color: var(--secondary);
}

.tag-rose {
  background: rgba(255, 71, 87, 0.1);
  color: var(--error);
}

.tag-orange {
  background: rgba(255, 180, 84, 0.14);
  color: #D66D21;
}

.creator-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--space-3);
  margin-top: var(--space-4);
}

.creator-item {
  min-width: 0;
  display: flex;
  align-items: center;
  flex-direction: column;
  gap: var(--space-2);
  border-radius: var(--radius-md);
  background: var(--background-muted);
  padding: var(--space-3) var(--space-2);
  text-align: center;
}

.creator-copy,
.creator-copy strong,
.creator-copy span {
  display: block;
  width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.creator-copy strong {
  color: var(--foreground);
  font-size: 12px;
}

.creator-copy span,
.rail-empty {
  color: var(--foreground-muted);
  font-size: 11px;
}

.creator-item button {
  border: 0;
  border-radius: var(--radius-full);
  background: var(--primary-muted);
  color: var(--primary);
  padding: 5px 12px;
  font-size: 12px;
  cursor: pointer;
}

.community-stat-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  margin-top: var(--space-4);
}

.community-stat {
  display: flex;
  align-items: center;
  flex-direction: column;
  gap: 4px;
  border-right: 1px solid var(--border);
  padding: var(--space-2);
  text-align: center;
}

.community-stat:last-child {
  border-right: 0;
}

.community-stat .el-icon {
  font-size: 20px;
}

.community-stat strong {
  color: var(--foreground);
  font-size: 18px;
  font-weight: 800;
  line-height: 1.2;
}

.community-stat span {
  color: var(--foreground-muted);
  font-size: 12px;
}

.community-green .el-icon {
  color: var(--primary);
}

.community-blue .el-icon {
  color: var(--secondary);
}

.community-orange .el-icon {
  color: var(--accent);
}

@media (max-width: 1180px) {
  .home-content {
    grid-template-columns: 1fr;
  }

  .discovery-rail {
    position: static;
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 1540px) {
  .masonry-feed {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .home-hero {
    grid-template-columns: 1fr;
  }

  .hero-preview {
    display: none;
  }

  .masonry-feed {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .discovery-rail {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .home-hero {
    border-radius: var(--radius-lg);
    padding: var(--space-6);
  }

  .hero-actions {
    flex-direction: column;
  }

  .masonry-feed {
    grid-template-columns: 1fr;
  }
}
</style>
