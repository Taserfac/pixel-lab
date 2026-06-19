<template>
  <div class="home-page">
    <section class="home-hero" @mouseenter="stopHeroRotation" @mouseleave="startHeroRotation">
      <!-- 左侧装饰底纹 -->
      <div class="hero-deco" aria-hidden="true">
        <svg class="deco-circle deco-c1" viewBox="0 0 200 200"><circle cx="100" cy="100" r="90" fill="none" stroke="currentColor" stroke-width="1.2" opacity="0.12"/></svg>
        <svg class="deco-circle deco-c2" viewBox="0 0 120 120"><circle cx="60" cy="60" r="55" fill="none" stroke="currentColor" stroke-width="1" opacity="0.08"/></svg>
        <svg class="deco-dots" viewBox="0 0 80 80"><circle cx="10" cy="10" r="2" fill="currentColor" opacity="0.1"/><circle cx="30" cy="10" r="2" fill="currentColor" opacity="0.07"/><circle cx="50" cy="10" r="2" fill="currentColor" opacity="0.1"/><circle cx="70" cy="10" r="2" fill="currentColor" opacity="0.07"/><circle cx="10" cy="30" r="2" fill="currentColor" opacity="0.07"/><circle cx="30" cy="30" r="2" fill="currentColor" opacity="0.1"/><circle cx="50" cy="30" r="2" fill="currentColor" opacity="0.07"/><circle cx="70" cy="30" r="2" fill="currentColor" opacity="0.1"/><circle cx="10" cy="50" r="2" fill="currentColor" opacity="0.1"/><circle cx="30" cy="50" r="2" fill="currentColor" opacity="0.07"/><circle cx="50" cy="50" r="2" fill="currentColor" opacity="0.1"/><circle cx="70" cy="50" r="2" fill="currentColor" opacity="0.07"/><circle cx="10" cy="70" r="2" fill="currentColor" opacity="0.07"/><circle cx="30" cy="70" r="2" fill="currentColor" opacity="0.1"/><circle cx="50" cy="70" r="2" fill="currentColor" opacity="0.07"/><circle cx="70" cy="70" r="2" fill="currentColor" opacity="0.1"/></svg>
        <svg class="deco-wave" viewBox="0 0 300 60"><path d="M0,30 Q30,0 60,30 Q90,60 120,30 Q150,0 180,30 Q210,60 240,30 Q270,0 300,30" fill="none" stroke="currentColor" stroke-width="1.5" opacity="0.08"/></svg>
      </div>

      <!-- 左侧文字区 -->
      <div class="hero-copy">
        <h1>{{ $t('dashboard.title') }}</h1>
        <p>{{ $t('dashboard.subtitle') }}</p>
        <Transition name="hero-text" mode="out-in">
          <div v-if="currentHeroWork" :key="currentHeroWork.id" class="hero-feature">
            <span class="hero-pill hero-pill--title">{{ workTitle(currentHeroWork) }}</span>
          </div>
        </Transition>
      </div>

      <!-- 右侧叠放卡片画廊 -->
      <div class="hero-gallery">
        <Transition name="hero-cards" mode="out-in">
          <div :key="activeHeroIndex" class="hero-card-stack">
            <!-- 后层卡片 3（最右） -->
            <button
              v-if="heroBackCard3"
              type="button"
              class="hero-card hero-card--back3"
              @click="switchToHero(heroBackCard3)"
            >
              <img :src="heroBackCard3.url" :alt="workTitle(heroBackCard3)">
            </button>
            <!-- 后层卡片 2 -->
            <button
              v-if="heroBackCard2"
              type="button"
              class="hero-card hero-card--back2"
              @click="switchToHero(heroBackCard2)"
            >
              <img :src="heroBackCard2.url" :alt="workTitle(heroBackCard2)">
            </button>
            <!-- 后层卡片 1 -->
            <button
              v-if="heroBackCard1"
              type="button"
              class="hero-card hero-card--back1"
              @click="switchToHero(heroBackCard1)"
            >
              <img :src="heroBackCard1.url" :alt="workTitle(heroBackCard1)">
            </button>
            <!-- 前层主卡片（点击进详情） -->
            <button
              v-if="currentHeroWork"
              type="button"
              class="hero-card hero-card--front"
              @click="openWork(currentHeroWork)"
            >
              <img :src="currentHeroWork.url" :alt="workTitle(currentHeroWork)">
            </button>
          </div>
        </Transition>

        <div class="hero-controls" aria-label="热门作品轮播">
          <button type="button" aria-label="上一张" @click="previousHero">‹</button>
          <span>{{ activeHeroIndex + 1 }} / {{ Math.min(featuredWorks.length, 4) }}</span>
          <button type="button" aria-label="下一张" @click="nextHero">›</button>
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
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  ArrowRight,
  ChatDotRound,
  CollectionTag,
  Star,
  User
} from '@element-plus/icons-vue'
import EmptyState from '@/components/common/EmptyState.vue'
import StableMasonry from '@/components/community/StableMasonry.vue'
import { getUserStats } from '@/api/auth'
import { getActivities, getPublicImages } from '@/api/community'

const router = useRouter()
const { t } = useI18n()
const loading = ref(false)
const works = ref([])
const activities = ref([])
const activeFeedTab = ref('推荐')
const activeHeroIndex = ref(0)
let heroTimer
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
    .slice(0, 5)
))
const currentHeroWork = computed(() => (
  featuredWorks.value[activeHeroIndex.value % Math.max(featuredWorks.value.length, 1)] || null
))
const heroBackCard1 = computed(() => {
  const len = featuredWorks.value.length
  if (len < 2) return null
  return featuredWorks.value[(activeHeroIndex.value + 1) % len]
})
const heroBackCard2 = computed(() => {
  const len = featuredWorks.value.length
  if (len < 3) return null
  return featuredWorks.value[(activeHeroIndex.value + 2) % len]
})
const heroBackCard3 = computed(() => {
  const len = featuredWorks.value.length
  if (len < 4) return null
  return featuredWorks.value[(activeHeroIndex.value + 3) % len]
})

/* 点击非首图 → 切换为首图 */
const switchToHero = (work) => {
  const idx = featuredWorks.value.findIndex(w => w.id === work.id)
  if (idx >= 0) activeHeroIndex.value = idx
}

const feedTabs = computed(() => {
  if (!works.value.length) return defaultFeedTabs
  const tagSet = new Set()
  works.value.forEach(work => {
    normalizeTags(work.tags).forEach(tag => tagSet.add(tag))
  })
  const dynamicTabs = [...tagSet]
  return ['推荐', '最新', '关注', ...dynamicTabs.filter(t => !['推荐', '最新', '关注'].includes(t))]
})

const popularTags = computed(() => {
  if (!works.value.length) return defaultPopularTags
  const tagCount = {}
  works.value.forEach(work => {
    normalizeTags(work.tags).forEach(tag => {
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

const workTitle = (work) => work?.title || work?.original_name || work?.filename || '未命名作品'

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
  router.push({ path: '/community', query: { keyword: tag.name || tag } })
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

/* ===== 横幅整体 ===== */
.home-hero {
  height: 320px;
  min-height: 320px;
  max-height: 320px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(340px, 420px);
  align-items: stretch;
  gap: clamp(var(--space-4), 3vw, var(--space-8));
  overflow: hidden;
  position: relative;
  border-radius: var(--radius-xl);
  background:
    radial-gradient(circle at 18% 20%, rgba(22, 199, 132, 0.12), transparent 40%),
    radial-gradient(circle at 80% 70%, rgba(91, 141, 239, 0.08), transparent 35%),
    linear-gradient(145deg, rgba(255, 255, 255, 0.97), rgba(240, 244, 255, 0.92)),
    var(--background-card);
  box-shadow: var(--shadow);
  padding: clamp(var(--space-6), 3.5vw, var(--space-9));
  margin-bottom: var(--space-8);
}

/* ===== 左侧装饰底纹 ===== */
.hero-deco {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  overflow: hidden;
}

.deco-circle {
  position: absolute;
  color: var(--primary);
}

.deco-c1 {
  width: 260px;
  height: 260px;
  top: -40px;
  left: -30px;
  animation: decoSpin 40s linear infinite;
}

.deco-c2 {
  width: 140px;
  height: 140px;
  bottom: 20px;
  left: 45%;
  color: var(--secondary);
  animation: decoSpin 30s linear infinite reverse;
}

.deco-dots {
  position: absolute;
  width: 100px;
  height: 100px;
  top: 15px;
  left: 35%;
  color: var(--foreground);
}

.deco-wave {
  position: absolute;
  width: 240px;
  bottom: 10px;
  left: 5%;
  color: var(--primary);
}

@keyframes decoSpin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* ===== 左侧文字区 ===== */
.hero-copy {
  align-self: center;
  position: relative;
  z-index: 1;
  padding-left: clamp(var(--space-4), 3vw, var(--space-8));
}

.hero-copy h1 {
  color: var(--foreground);
  font-size: clamp(32px, 3.8vw, 52px);
  font-weight: 800;
  line-height: 1.08;
  letter-spacing: -0.02em;
}

.hero-copy > p {
  max-width: 380px;
  margin-top: var(--space-3);
  color: var(--foreground-muted);
  font-size: 15px;
  line-height: 1.7;
}

/* ===== 作品标题胶囊 ===== */
.hero-feature {
  margin-top: var(--space-5);
}

.hero-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  border-radius: var(--radius-full);
  padding: 7px 16px;
  font-weight: 700;
  white-space: nowrap;
  border: 0;
  cursor: pointer;
  background: rgba(22, 199, 132, 0.1);
  color: var(--primary);
  transition: background var(--transition-fast);
}

.hero-pill:hover {
  background: rgba(22, 199, 132, 0.18);
}

/* ===== 左侧文字切换动画 ===== */
.hero-text-enter-active {
  transition: all 0.35s cubic-bezier(0.23, 1, 0.32, 1);
}
.hero-text-leave-active {
  transition: all 0.2s cubic-bezier(0.55, 0, 1, 0.45);
}
.hero-text-enter-from {
  opacity: 0;
  transform: translateY(10px);
}
.hero-text-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* ===== 右侧叠放卡片画廊 ===== */
.hero-gallery {
  position: relative;
  z-index: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  margin-left: -120px;
}

.hero-card-stack {
  position: relative;
  width: 100%;
  height: 250px;
}

/* ===== 卡片切换动画 ===== */
.hero-cards-enter-active {
  transition: all 0.45s cubic-bezier(0.23, 1, 0.32, 1);
}
.hero-cards-leave-active {
  transition: all 0.25s cubic-bezier(0.55, 0, 1, 0.45);
}
.hero-cards-enter-from {
  opacity: 0;
  transform: translateX(30px) scale(0.96);
}
.hero-cards-leave-to {
  opacity: 0;
  transform: translateX(-30px) scale(0.96);
}

.hero-card {
  position: absolute;
  overflow: hidden;
  border: 0;
  border-radius: var(--radius-lg);
  background: var(--background-muted);
  padding: 0;
  cursor: pointer;
  transition: transform 0.5s cubic-bezier(0.23, 1, 0.32, 1),
              box-shadow 0.5s cubic-bezier(0.23, 1, 0.32, 1);
}

.hero-card img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

/* 前层主卡片（左侧） */
.hero-card--front {
  width: 320px;
  height: 220px;
  z-index: 4;
  bottom: 40px;
  left: 0;
  border-radius: var(--radius-lg);
  box-shadow:
    0 8px 30px rgba(0, 0, 0, 0.12),
    0 2px 8px rgba(0, 0, 0, 0.06);
  transform: rotate(1deg);
}

.hero-card--front:hover {
  transform: rotate(0deg) translateY(-4px) scale(1.02);
  box-shadow:
    0 16px 48px rgba(0, 0, 0, 0.18),
    0 4px 12px rgba(0, 0, 0, 0.08);
}

/* 中层卡片 1 */
.hero-card--back1 {
  width: 270px;
  height: 188px;
  z-index: 3;
  bottom: 50px;
  left: 140px;
  border-radius: var(--radius-md);
  box-shadow:
    0 6px 20px rgba(0, 0, 0, 0.08),
    0 2px 6px rgba(0, 0, 0, 0.04);
  transform: rotate(-2deg) translateY(4px);
  opacity: 0.92;
}

.hero-card--back1:hover {
  transform: rotate(-1deg) translateY(0) scale(1.01);
  opacity: 1;
}

/* 中层卡片 2 */
.hero-card--back2 {
  width: 225px;
  height: 158px;
  z-index: 2;
  bottom: 62px;
  left: 245px;
  border-radius: var(--radius-md);
  box-shadow:
    0 4px 14px rgba(0, 0, 0, 0.06);
  transform: rotate(3deg) translateY(8px);
  opacity: 0.82;
}

.hero-card--back2:hover {
  transform: rotate(1.5deg) translateY(4px) scale(1.01);
  opacity: 0.95;
}

/* 后层卡片 3（最右） */
.hero-card--back3 {
  width: 185px;
  height: 132px;
  z-index: 1;
  bottom: 72px;
  left: 330px;
  border-radius: var(--radius-sm);
  box-shadow:
    0 3px 10px rgba(0, 0, 0, 0.05);
  transform: rotate(-4deg) translateY(12px);
  opacity: 0.7;
}

.hero-card--back3:hover {
  transform: rotate(-3deg) translateY(10px) scale(1.01);
  opacity: 0.9;
}

/* ===== 轮播控制 ===== */
.hero-controls {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--space-3);
  color: var(--foreground-muted);
  font-size: 12px;
}

.hero-controls button {
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  border: 1px solid var(--border);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.8);
  color: var(--foreground);
  font-size: 20px;
  cursor: pointer;
  transition: background var(--transition-fast);
}

.hero-controls button:hover {
  background: var(--primary-muted);
  color: var(--primary);
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


@media (max-width: 900px) {
  .home-hero {
    grid-template-columns: 1fr;
    height: auto;
    min-height: 260px;
    max-height: none;
    padding: var(--space-6);
  }

  .hero-gallery {
    display: none;
  }

  .hero-deco {
    opacity: 0.5;
  }

  .hero-copy h1 {
    font-size: clamp(26px, 6vw, 38px);
  }

  .discovery-rail {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .home-hero {
    border-radius: var(--radius-lg);
    padding: var(--space-5);
  }

  .hero-copy > p {
    font-size: 13px;
  }

  .hero-pill {
    font-size: 12px;
    padding: 6px 12px;
  }
}
</style>
