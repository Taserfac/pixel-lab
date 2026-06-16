<template>
  <div class="home-page">
    <section class="home-hero">
      <div class="hero-copy">
        <h1>发现优秀创作</h1>
        <p>作品分享与灵感社区</p>
        <div class="hero-actions">
          <el-button
            type="primary"
            size="large"
            @click="router.push('/personal')"
          >
            <el-icon><Upload /></el-icon>
            上传作品
          </el-button>
          <el-button
            size="large"
            @click="router.push('/community')"
          >
            <el-icon><Compass /></el-icon>
            浏览社区
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
            :alt="work.title || work.original_name || '作品预览'"
          >
        </div>
      </div>
    </section>

    <div class="home-content">
      <main class="feed-column">
        <div class="section-heading">
          <div>
            <h2>精选作品</h2>
            <p>图片优先的瀑布流浏览体验</p>
          </div>
          <el-button
            text
            @click="router.push('/community')"
          >
            查看更多 <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>

        <div
          v-loading="loading"
          class="masonry-feed"
        >
          <PostCard
            v-for="(work, index) in feedWorks"
            :key="work.id"
            :work="work"
            :style="{ '--post-ratio': getCardRatio(index) }"
            @select="openWork"
          />
        </div>

        <EmptyState
          v-if="!loading && feedWorks.length === 0"
          title="暂无公开作品"
          description="上传并公开你的作品后，这里会形成社区作品流。"
          action-text="去上传"
          show-action
          @action="router.push('/personal')"
        />
      </main>

      <aside class="discovery-rail">
        <section class="rail-card tag-card">
          <div class="rail-title">
            <h3>热门标签</h3>
            <span>Trending</span>
          </div>
          <div class="tag-list">
            <button
              v-for="tag in popularTags"
              :key="tag"
              type="button"
              class="tag-chip"
              @click="searchTag(tag)"
            >
              #{{ tag }}
            </button>
          </div>
        </section>

        <section class="rail-card creator-card">
          <div class="rail-title">
            <h3>推荐创作者</h3>
            <span>Creators</span>
          </div>
          <div
            v-if="recommendedCreators.length"
            class="creator-list"
          >
            <div
              v-for="creator in recommendedCreators"
              :key="creator.name"
              class="creator-item"
            >
              <el-avatar
                :size="42"
                :src="creator.avatar"
              >
                {{ creator.name.charAt(0) }}
              </el-avatar>
              <div>
                <strong>{{ creator.name }}</strong>
                <span>{{ creator.count }} 件作品</span>
              </div>
              <button
                type="button"
                @click="router.push('/community')"
              >
                查看
              </button>
            </div>
          </div>
          <div
            v-else
            class="rail-empty"
          >
            暂无推荐创作者
          </div>
        </section>

        <section class="rail-card stats-card">
          <div class="rail-title">
            <h3>我的创作概览</h3>
            <span>Stats</span>
          </div>
          <div class="stat-list">
            <div
              v-for="stat in userStatItems"
              :key="stat.label"
              class="stat-item"
            >
              <span>{{ stat.label }}</span>
              <strong>{{ formatNumber(stat.value) }}</strong>
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
import { ArrowRight, Compass, Upload } from '@element-plus/icons-vue'
import EmptyState from '@/components/common/EmptyState.vue'
import PostCard from '@/components/community/PostCard.vue'
import { getUserStats } from '@/api/auth'
import { getActivities, getPublicImages } from '@/api/community'

const router = useRouter()
const loading = ref(false)
const works = ref([])
const activities = ref([])
const userStats = ref({
  works: 0,
  likes: 0,
  views: 0
})

const popularTags = ['像素艺术', '概念设计', '角色设定', '复古游戏', '赛博城市', '插画灵感', '公开作品', '灵感收藏']
const cardRatios = ['4 / 5', '1 / 1', '5 / 4', '3 / 4', '4 / 3']

const toNumber = (value) => Number(value || 0)

const normalizeStats = (stats = {}) => ({
  works: toNumber(stats.works ?? stats.imageCount),
  likes: toNumber(stats.likes ?? stats.receivedLikeCount ?? stats.likeCount),
  views: toNumber(stats.views ?? stats.viewCount)
})

const feedWorks = computed(() => works.value)
const previewWorks = computed(() => {
  const source = feedWorks.value.slice(0, 3)
  if (source.length) return source
  return [{ id: 'empty-1' }, { id: 'empty-2' }, { id: 'empty-3' }]
})

const userStatItems = computed(() => [
  { label: '作品', value: userStats.value.works },
  { label: '获赞', value: userStats.value.likes },
  { label: '浏览', value: userStats.value.views }
])

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
      count: 1
    })
  })

  return Array.from(creatorMap.values()).slice(0, 4)
})

const getCardRatio = (index) => cardRatios[index % cardRatios.length]

const formatNumber = (value) => {
  const number = toNumber(value)
  if (number >= 10000) return `${(number / 10000).toFixed(1)}w`
  if (number >= 1000) return `${(number / 1000).toFixed(1)}k`
  return String(number)
}

const openWork = (work) => {
  router.push({ path: '/community', query: { id: work.id } })
}

const searchTag = (tag) => {
  router.push({ path: '/community', query: { keyword: tag } })
}

onMounted(async () => {
  loading.value = true
  try {
    const [imagesRes, statsRes, activitiesRes] = await Promise.all([
      getPublicImages({ page: 1, pageSize: 16, sortBy: 'popular' }).catch(() => ({ list: [] })),
      getUserStats().catch(() => ({ works: 0, likes: 0, views: 0 })),
      getActivities({ limit: 8 }).catch(() => [])
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
  width: min(1440px, 100%);
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
}

.preview-card img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.preview-card-1 {
  width: 52%;
  aspect-ratio: 4 / 5;
  left: 2%;
  bottom: 0;
}

.preview-card-2 {
  width: 48%;
  aspect-ratio: 1;
  right: 0;
  top: 0;
}

.preview-card-3 {
  width: 44%;
  aspect-ratio: 5 / 4;
  right: 12%;
  bottom: 8%;
}

.home-content {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: var(--space-8);
  margin-top: var(--space-8);
  align-items: start;
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

.masonry-feed {
  min-height: 360px;
  column-count: 3;
  column-gap: var(--space-5);
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

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-top: var(--space-4);
}

.tag-chip {
  border: 0;
  border-radius: var(--radius-full);
  background: var(--background-muted);
  color: var(--foreground-muted);
  padding: 8px 12px;
  font-size: 12px;
  cursor: pointer;
  transition:
    color var(--transition-fast),
    background var(--transition-fast),
    transform var(--transition-fast);
}

.tag-chip:hover {
  color: var(--primary);
  background: var(--primary-muted);
  transform: translateY(-1px);
}

.creator-list {
  display: grid;
  gap: var(--space-3);
  margin-top: var(--space-4);
}

.creator-item {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) max-content;
  align-items: center;
  gap: var(--space-3);
}

.creator-item strong,
.creator-item span {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.creator-item strong {
  color: var(--foreground);
  font-size: 14px;
}

.creator-item span,
.rail-empty {
  color: var(--foreground-muted);
  font-size: 12px;
}

.creator-item button {
  border: 0;
  border-radius: var(--radius-full);
  background: var(--primary-muted);
  color: var(--primary);
  padding: 6px 10px;
  font-size: 12px;
  cursor: pointer;
}

.stat-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-2);
  margin-top: var(--space-4);
}

.stat-item {
  border-radius: var(--radius-md);
  background: var(--background-muted);
  padding: var(--space-3);
  text-align: center;
}

.stat-item span,
.stat-item strong {
  display: block;
}

.stat-item span {
  color: var(--foreground-muted);
  font-size: 12px;
}

.stat-item strong {
  margin-top: 2px;
  color: var(--foreground);
  font-size: 18px;
  font-weight: 800;
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

  .hero-preview {
    display: none;
  }

  .masonry-feed {
    column-count: 2;
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
    column-count: 1;
  }
}
</style>
