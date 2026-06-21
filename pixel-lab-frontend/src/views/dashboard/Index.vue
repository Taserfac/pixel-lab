<template>
  <div class="home-page">
    <section class="home-hero">
      <div class="hero-copy">
        <h1>{{ $t('dashboard.title') }}</h1>
        <p>{{ $t('dashboard.subtitle') }}</p>
        <div v-if="currentHeroWork" class="hero-feature">
          <strong>{{ workTitle(currentHeroWork) }}</strong>
          <div class="hero-meta">
            <span v-if="heroAuthor(currentHeroWork)">{{ heroAuthor(currentHeroWork) }}</span>
            <span v-if="primaryTag(currentHeroWork)">#{{ primaryTag(currentHeroWork) }}</span>
            <span>
              <el-icon><View /></el-icon>
              {{ formatNumber(currentHeroWork.view_count) }} 次浏览
            </span>
          </div>
          <button type="button" class="hero-cta" @click="openWork(currentHeroWork)">
            查看作品 <el-icon><ArrowRight /></el-icon>
          </button>
        </div>
      </div>

      <div class="hero-showcase">
        <Transition name="hero-swap" mode="out-in">
          <button
            v-if="currentHeroWork"
            :key="currentHeroWork.id || currentHeroWork.url"
            type="button"
            class="hero-preview"
            @click="openWork(currentHeroWork)"
          >
            <img :src="cardImageUrl(currentHeroWork)" :alt="workTitle(currentHeroWork)">
            <span v-if="primaryTag(currentHeroWork)" class="hero-preview-tag">#{{ primaryTag(currentHeroWork) }}</span>
            <span class="hero-preview-title">{{ workTitle(currentHeroWork) }}</span>
          </button>
        </Transition>
        <div class="hero-controls" aria-label="热门作品轮播">
          <button type="button" aria-label="上一张" @click="previousHero">
            <el-icon><ArrowLeft /></el-icon>
          </button>
          <span>{{ activeHeroIndex + 1 }} / {{ featuredWorks.length }}</span>
          <button type="button" aria-label="下一张" @click="nextHero">
            <el-icon><ArrowRight /></el-icon>
          </button>
        </div>
      </div>
    </section>
    <div class="home-content">
      <div class="primary-column">
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
              @click="selectFeedTab(tab)"
            >
              {{ tab }}
            </button>
          </div>

          <StableMasonry
            class="masonry-feed"
            :works="feedWorks"
            :loading="loading"
            @select="openWork"
            @author-select="openCreator"
          />

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
        <section class="rail-card tag-card">
          <div class="rail-title">
            <h3>{{ $t('dashboard.hotTags') }}</h3>
            <button type="button" @click="router.push('/tags')">
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
            <button type="button" @click="router.push('/creators')">
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
              :class="{ clickable: creator.id }"
              @click="openCreator(creator)"
            >
              <el-avatar
                :size="40"
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
                @click.stop="openCreator(creator)"
              >
                主页
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
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  ArrowLeft,
  ArrowRight,
  ChatDotRound,
  CollectionTag,
  Star,
  User,
  View
} from '@element-plus/icons-vue'
import EmptyState from '@/components/common/EmptyState.vue'
import StableMasonry from '@/components/community/StableMasonry.vue'
import { getUserStats } from '@/api/auth'
import { getActivities, getPublicImages, getPublicTags } from '@/api/community'
import { cardImageUrl } from '@/utils/media'

const router = useRouter()
const { t } = useI18n()
const loading = ref(false)
const works = ref([])
const activities = ref([])
const publicTags = ref({ systemTags: [], trendingTags: [] })
const activeFeedTab = ref('推荐')
const activeHeroIndex = ref(0)
let heroTimer
const userStats = ref({
  works: 0,
  likes: 0,
  views: 0
})

const defaultSystemTags = ['摄影', '插画', 'AI艺术', '设计', '旅行', '像素艺术', '城市', '生活']
const tagTones = ['green', 'purple', 'blue', 'rose', 'orange']
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
const toNumber = (value) => Number(value || 0)
const normalizeTags = (value) => {
  if (Array.isArray(value)) return value
  return String(value || '').split(/[,，]/).map(tag => tag.trim()).filter(Boolean)
}

const normalizeStats = (stats = {}) => ({
  works: toNumber(stats.works ?? stats.imageCount),
  likes: toNumber(stats.likes ?? stats.receivedLikeCount ?? stats.likeCount),
  views: toNumber(stats.views ?? stats.viewCount)
})

const sourceWorks = computed(() => works.value.length ? works.value : sampleWorks)
const featuredWorks = computed(() => (
  [...sourceWorks.value]
    .sort((a, b) => toNumber(b.view_count) - toNumber(a.view_count))
    .slice(0, 4)
))
const currentHeroWork = computed(() => (
  featuredWorks.value[activeHeroIndex.value % Math.max(featuredWorks.value.length, 1)] || null
))

const feedTabs = computed(() => {
  const systemTags = publicTags.value.systemTags.length ? publicTags.value.systemTags : defaultSystemTags
  const trendingNames = publicTags.value.trendingTags.map(tag => tag.name)
  return ['推荐', '最新', '关注', ...new Set([...systemTags, ...trendingNames])]
})

const popularTags = computed(() => {
  const source = publicTags.value.trendingTags.length
    ? publicTags.value.trendingTags
    : (publicTags.value.systemTags.length ? publicTags.value.systemTags : defaultSystemTags)
      .map(name => ({ name, usageCount: 0 }))
  return source.slice(0, 5)
    .map((tag, i) => ({
      name: tag.name,
      count: String(tag.usageCount || 0),
      tone: tagTones[i % tagTones.length]
    }))
})

const feedWorks = computed(() => {
  const source = sourceWorks.value
  if (activeFeedTab.value === '推荐') return source
  if (activeFeedTab.value === '最新') return [...source].reverse()
  if (activeFeedTab.value === '关注') return source.slice(0, 4)

  const matched = source.filter((work) => {
    const tags = normalizeTags(work.tags)
    const haystack = `${work.title || ''} ${work.description || ''} ${work.original_name || ''}`
    return tags.includes(activeFeedTab.value) || haystack.includes(activeFeedTab.value)
  })

  return matched.length ? matched : source
})

const recommendedCreators = computed(() => {
  const creatorMap = new Map()

  feedWorks.value.forEach((work) => {
    const name = work.author_name || work.nickname
    if (!name) return
    const cached = creatorMap.get(name) || {
      id: work.user_id || work.author_id,
      name,
      avatar: work.author_avatar || '',
      count: 0
    }
    cached.count += 1
    cached.id ||= work.user_id || work.author_id
    creatorMap.set(name, cached)
  })

  activities.value.forEach((activity) => {
    const name = activity.author_name
    if (!name || creatorMap.has(name)) return
    creatorMap.set(name, {
      id: activity.author_id,
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

const looksLikeFileName = (value = '') => {
  const text = String(value).trim()
  return /\.(jpe?g|png|gif|webp|bmp|svg)$/i.test(text)
    || /^[a-f0-9]{16,}(\.[a-z0-9]+)?$/i.test(text)
    || /^image_\d+_\d+/i.test(text)
}

const primaryTag = (work) => normalizeTags(work?.tags)[0] || ''

const heroAuthor = (work) => work?.author_name || work?.nickname || ''

const workTitle = (work) => {
  const title = String(work?.title || '').trim()
  if (title && !looksLikeFileName(title)) return title

  const description = String(work?.description || '').trim()
  if (description && !looksLikeFileName(description)) {
    return description.length > 24 ? `${description.slice(0, 24)}...` : description
  }

  const tag = primaryTag(work)
  if (tag) return `${tag}精选作品`
  return '精选创作'
}

const stopHeroRotation = () => {
  if (heroTimer) window.clearInterval(heroTimer)
  heroTimer = undefined
}

const nextHero = () => {
  if (featuredWorks.value.length < 2) return
  activeHeroIndex.value = (activeHeroIndex.value + 1) % featuredWorks.value.length
}

const previousHero = () => {
  if (featuredWorks.value.length < 2) return
  activeHeroIndex.value = (activeHeroIndex.value - 1 + featuredWorks.value.length) % featuredWorks.value.length
}

const startHeroRotation = () => {
  stopHeroRotation()
  if (featuredWorks.value.length > 1) heroTimer = window.setInterval(nextHero, 5000)
}

const selectFeedTab = (tab) => {
  if (tab === '关注') {
    router.push({ path: '/creators', query: { tab: 'following' } })
    return
  }
  activeFeedTab.value = tab
}

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
  router.push(`/post/${work.id}`)
}

const openCreator = (creator) => {
  const id = creator.user_id || creator.author_id || creator.id
  if (id) router.push(`/user/${id}`)
}

const searchTag = (tag) => {
  router.push({ path: '/tags', query: { tag: tag.name || tag } })
}

onMounted(async () => {
  loading.value = true
  try {
    const [imagesRes, statsRes, activitiesRes, tagsRes] = await Promise.all([
      getPublicImages({ page: 1, pageSize: 16, sortBy: 'popular' }, { silent: true }).catch(() => ({ list: [] })),
      getUserStats({}, { silent: true }).catch(() => ({ works: 0, likes: 0, views: 0 })),
      getActivities({ limit: 8 }, { silent: true }).catch(() => []),
      getPublicTags({ limit: 20 }, { silent: true }).catch(() => ({ systemTags: [], trendingTags: [] }))
    ])

    works.value = imagesRes.list || []
    userStats.value = normalizeStats(statsRes)
    activities.value = activitiesRes || []
    publicTags.value = tagsRes
  } catch (error) {
    console.error('获取首页数据失败:', error)
  } finally {
    loading.value = false
    startHeroRotation()
  }
})

onBeforeUnmount(stopHeroRotation)
</script>

<style scoped>
.home-page {
  width: min(1820px, 100%);
  margin: 0 auto;
}

.home-hero {
  min-height: 320px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(420px, 34vw);
  align-items: stretch;
  gap: clamp(var(--space-8), 5vw, var(--space-16));
  overflow: hidden;
  position: relative;
  border-radius: var(--radius-xl);
  background:
    radial-gradient(circle at 8% 18%, rgba(22, 199, 132, 0.18), transparent 34%),
    linear-gradient(115deg, rgba(255, 255, 255, 0.98) 0%, rgba(244, 249, 247, 0.92) 45%, rgba(91, 141, 239, 0.14) 100%),
    var(--background-card);
  box-shadow: var(--shadow);
  padding: clamp(var(--space-8), 4vw, var(--space-12));
  margin-bottom: var(--space-8);
}

:global([data-theme='dark'] .home-hero) {
  border: 1px solid rgba(255, 255, 255, 0.07);
  background:
    radial-gradient(circle at 10% 18%, rgba(25, 212, 142, 0.18), transparent 34%),
    radial-gradient(circle at 78% 8%, rgba(124, 90, 239, 0.18), transparent 30%),
    linear-gradient(115deg, rgba(31, 43, 37, 0.96) 0%, rgba(22, 31, 27, 0.94) 52%, rgba(29, 36, 49, 0.9) 100%),
    var(--background-card);
  box-shadow: 0 22px 60px rgba(0, 0, 0, 0.32);
}

.hero-copy {
  align-self: center;
  min-width: 0;
}

.hero-copy h1 {
  color: var(--foreground);
  font-size: clamp(34px, 4vw, 54px);
  font-weight: 800;
  line-height: 1.05;
}

.hero-copy > p {
  max-width: 460px;
  margin-top: var(--space-3);
  color: var(--foreground-muted);
  font-size: 16px;
  line-height: 1.8;
}

.hero-feature {
  display: flex;
  align-items: flex-start;
  flex-direction: column;
  gap: var(--space-4);
  margin-top: var(--space-6);
}

.hero-feature strong {
  max-width: 620px;
  overflow-wrap: anywhere;
  color: var(--foreground);
  font-size: 22px;
  line-height: 1.35;
}

.hero-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.hero-meta span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border-radius: var(--radius-full);
  background: rgba(255, 255, 255, 0.7);
  color: var(--foreground-muted);
  padding: 6px 10px;
  font-size: 12px;
  font-weight: 600;
}

:global([data-theme='dark'] .hero-meta span) {
  background: rgba(255, 255, 255, 0.08);
  color: var(--foreground-muted);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.04);
}

.hero-cta {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 0;
  border-radius: var(--radius-full);
  background: var(--primary);
  color: white;
  padding: 10px 18px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: var(--glow-sm);
  transition: background var(--transition-fast), transform var(--transition-fast), box-shadow var(--transition-fast);
}

.hero-cta:hover {
  background: var(--primary-hover);
  transform: translateY(-1px);
  box-shadow: var(--glow);
}

.hero-showcase {
  min-width: 0;
  display: grid;
  grid-template-rows: minmax(0, 240px) auto;
  gap: var(--space-3);
}

.hero-preview {
  width: 100%;
  height: 240px;
  min-height: 0;
  overflow: hidden;
  position: relative;
  border: 0;
  border-radius: var(--radius-lg);
  background:
    linear-gradient(135deg, rgba(20, 26, 23, 0.08), rgba(91, 141, 239, 0.14)),
    var(--background-muted);
  box-shadow: var(--shadow-md);
  padding: 0;
  cursor: pointer;
}

:global([data-theme='dark'] .hero-preview) {
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.06), rgba(91, 141, 239, 0.1)),
    #121915;
  box-shadow: 0 20px 48px rgba(0, 0, 0, 0.36);
}

.hero-preview::after {
  content: '';
  position: absolute;
  inset: 42% 0 0;
  background: linear-gradient(180deg, transparent, rgba(0, 0, 0, 0.52));
  pointer-events: none;
}

.hero-preview img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
  transition: transform var(--transition-slow);
}

.hero-preview:hover img {
  transform: scale(1.025);
}

.hero-swap-enter-active,
.hero-swap-leave-active {
  transition: opacity 220ms var(--ease-out), transform 220ms var(--ease-out);
}

.hero-swap-enter-from {
  opacity: 0;
  transform: translateX(16px) scale(0.985);
}

.hero-swap-leave-to {
  opacity: 0;
  transform: translateX(-12px) scale(0.985);
}

@media (prefers-reduced-motion: reduce) {
  .hero-swap-enter-active,
  .hero-swap-leave-active {
    transition: opacity 120ms linear;
  }

  .hero-swap-enter-from,
  .hero-swap-leave-to {
    transform: none;
  }
}

.hero-preview-tag,
.hero-preview-title {
  position: absolute;
  z-index: 1;
  left: var(--space-4);
}

.hero-preview-tag {
  top: var(--space-4);
  border-radius: var(--radius-full);
  background: rgba(255, 255, 255, 0.84);
  color: var(--foreground);
  padding: 5px 10px;
  font-size: 12px;
  font-weight: 700;
}

:global([data-theme='dark'] .hero-preview-tag),
:global([data-theme='dark'] .hero-controls button) {
  background: rgba(18, 25, 21, 0.82);
  color: var(--foreground);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.08);
}

.hero-preview-title {
  right: var(--space-4);
  bottom: var(--space-4);
  display: -webkit-box;
  overflow: hidden;
  color: white;
  font-size: 15px;
  font-weight: 800;
  line-height: 1.3;
  text-align: left;
  text-shadow: 0 1px 12px rgba(0, 0, 0, 0.36);
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.hero-controls {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--space-3);
  color: var(--foreground-muted);
  font-size: 12px;
}

.hero-controls button {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border: 1px solid var(--border);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.8);
  color: var(--foreground);
  font-size: 15px;
  cursor: pointer;
  transition: background var(--transition-fast), color var(--transition-fast), transform var(--transition-fast), box-shadow var(--transition-fast);
}

.hero-controls button:hover {
  background: var(--primary);
  color: #fff;
  transform: translateY(-1px);
  box-shadow: var(--glow-sm);
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

.feed-column {
  width: 100%;
  min-width: 0;
  max-width: 100%;
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
  width: 100%;
  max-width: 100%;
  display: flex;
  flex-wrap: nowrap;
  gap: var(--space-2);
  margin-bottom: var(--space-5);
  overflow: hidden;
}

.feed-tabs button {
  flex: 0 0 auto;
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
  padding: var(--space-4);
  min-width: 0;
}

:global([data-theme='dark'] .rail-card) {
  border: 1px solid rgba(255, 255, 255, 0.06);
  background: rgba(24, 33, 29, 0.86);
  box-shadow: 0 16px 42px rgba(0, 0, 0, 0.26);
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

:global([data-theme='dark'] .tag-purple) {
  background: rgba(151, 117, 255, 0.16);
  color: #B69BFF;
}

:global([data-theme='dark'] .tag-blue) {
  background: rgba(91, 141, 239, 0.16);
  color: #8EB1FF;
}

:global([data-theme='dark'] .tag-rose) {
  background: rgba(255, 91, 117, 0.14);
  color: #FF9AAD;
}

:global([data-theme='dark'] .tag-orange) {
  background: rgba(255, 180, 84, 0.16);
  color: #FFC47A;
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

:global([data-theme='dark'] .creator-item) {
  background: rgba(255, 255, 255, 0.045);
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
  transition: background var(--transition-fast), color var(--transition-fast), transform var(--transition-fast);
}

.creator-item button:hover {
  background: var(--primary);
  color: #fff;
  transform: translateY(-1px);
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


@media (max-width: 900px) {
  .home-hero {
    grid-template-columns: 1fr;
  }

  .home-hero {
    height: auto;
    min-height: 240px;
    max-height: none;
  }

  .hero-showcase {
    grid-template-rows: 190px auto;
  }

  .hero-preview {
    height: 190px;
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

  .hero-copy h1 {
    font-size: 32px;
  }

  .hero-copy > p {
    font-size: 14px;
    line-height: 1.7;
  }

  .hero-controls {
    justify-content: space-between;
  }

  .tag-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
