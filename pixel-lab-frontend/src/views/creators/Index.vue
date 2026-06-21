<template>
  <div class="creators-page">
    <header class="page-heading">
      <div>
        <h1>发现创作者</h1>
        <p>从代表作品和真实互动中，找到值得持续关注的创作者。</p>
      </div>
      <div class="creator-tabs" role="tablist" aria-label="创作者分类">
        <button
          type="button"
          :class="{ active: activeTab === 'recommended' }"
          @click="setTab('recommended')"
        >推荐创作者</button>
        <button
          type="button"
          :class="{ active: activeTab === 'following' }"
          @click="setTab('following')"
        >已关注</button>
      </div>
    </header>

    <div v-loading="loading" class="creator-list">
      <article
        v-for="creator in displayedCreators"
        :key="creator.id"
        class="creator-card"
      >
        <button class="creator-cover" type="button" @click="openCreator(creator)">
          <img v-if="creator.representativeWorks[0]?.url" :src="cardImageUrl(creator.representativeWorks[0])" alt="" loading="lazy" decoding="async">
          <span v-else>{{ creator.name.charAt(0) }}</span>
        </button>

        <div class="creator-body">
          <div class="creator-identity">
            <el-avatar :size="52" :src="avatarImageUrl(creator.avatar)">{{ creator.name.charAt(0) }}</el-avatar>
            <div>
              <h2>{{ creator.name }}</h2>
              <p>{{ creator.workCount }} 件公开作品</p>
            </div>
            <el-button plain @click="openCreator(creator)">查看主页</el-button>
          </div>

          <div class="creator-metrics">
            <span><strong>{{ formatNumber(creator.likeCount) }}</strong>获赞</span>
            <span><strong>{{ formatNumber(creator.collectCount) }}</strong>收藏</span>
            <span><strong>{{ formatNumber(creator.commentCount) }}</strong>评论</span>
          </div>

          <div v-if="creator.representativeWorks.length" class="representative-works">
            <button
              v-for="work in creator.representativeWorks"
              :key="work.id || work.url"
              type="button"
              @click="openWork(work)"
            >
              <img :src="cardImageUrl(work)" :alt="work.title || '代表作品'" loading="lazy" decoding="async">
            </button>
          </div>
        </div>
      </article>
    </div>

    <EmptyState
      v-if="!loading && !displayedCreators.length"
      :title="activeTab === 'following' ? '还没有关注创作者' : '暂时没有推荐创作者'"
      :description="activeTab === 'following' ? '浏览社区作品，在创作者主页关注喜欢的人。' : '有公开作品后，推荐会出现在这里。'"
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
import { getFollowingCreators, getPublicImages } from '@/api/community'
import { avatarImageUrl, cardImageUrl } from '@/utils/media'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const recommended = ref([])
const following = ref([])
const activeTab = computed(() => route.query.tab === 'following' ? 'following' : 'recommended')
const displayedCreators = computed(() => activeTab.value === 'following' ? following.value : recommended.value)

const number = (value) => Number(value || 0)
const normalizeRecommended = (works) => {
  const creators = new Map()
  works.forEach((work) => {
    const id = work.user_id || work.author_id
    if (!id) return
    const creator = creators.get(id) || {
      id,
      name: work.author_name || work.nickname || '匿名创作者',
      avatar: work.author_avatar || '',
      workCount: 0,
      likeCount: 0,
      collectCount: 0,
      commentCount: 0,
      representativeWorks: []
    }
    creator.workCount += 1
    creator.likeCount += number(work.like_count)
    creator.collectCount += number(work.collect_count)
    creator.commentCount += number(work.comment_count)
    creator.representativeWorks.push(work)
    creators.set(id, creator)
  })

  return [...creators.values()]
    .map(creator => ({
      ...creator,
      representativeWorks: creator.representativeWorks
        .sort((a, b) => number(b.like_count) - number(a.like_count))
        .slice(0, 3)
    }))
    .sort((a, b) => (b.likeCount + b.collectCount) - (a.likeCount + a.collectCount))
}

const normalizeFollowing = (creators) => creators.map(creator => ({
  id: creator.id,
  name: creator.nickname || creator.username || '创作者',
  avatar: creator.avatar || '',
  workCount: number(creator.work_count),
  likeCount: number(creator.like_count),
  collectCount: number(creator.collect_count),
  commentCount: number(creator.comment_count),
  representativeWorks: creator.representative_url
    ? [{ id: null, url: creator.representative_url, title: `${creator.nickname || creator.username} 的代表作品` }]
    : []
}))

const setTab = (tab) => router.replace({ query: tab === 'following' ? { tab } : {} })
const openCreator = (creator) => router.push(`/user/${creator.id}`)
const openWork = (work) => work.id ? router.push(`/post/${work.id}`) : undefined
const formatNumber = (value) => number(value).toLocaleString('zh-CN')

onMounted(async () => {
  loading.value = true
  try {
    const [worksData, followingData] = await Promise.all([
      getPublicImages({ page: 1, pageSize: 50, sortBy: 'popular' }, { silent: true }).catch(() => ({ list: [] })),
      getFollowingCreators().catch(() => [])
    ])
    recommended.value = normalizeRecommended(worksData.list || [])
    following.value = normalizeFollowing(followingData || [])
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.creators-page {
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

.creator-tabs {
  display: flex;
  gap: var(--space-2);
}

.creator-tabs button {
  border: 0;
  border-radius: var(--radius-full);
  background: transparent;
  color: var(--foreground-muted);
  padding: 9px 16px;
  font-weight: 600;
  cursor: pointer;
}

.creator-tabs button.active {
  background: var(--primary-muted);
  color: var(--primary);
}

.creator-list {
  min-height: 360px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-5);
}

.creator-card {
  display: grid;
  grid-template-columns: 190px minmax(0, 1fr);
  overflow: hidden;
  border-radius: var(--radius-lg);
  background: var(--background-card);
  box-shadow: var(--shadow-sm);
}

.creator-cover {
  min-height: 260px;
  border: 0;
  background: var(--background-muted);
  padding: 0;
  cursor: pointer;
}

.creator-cover img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.creator-cover > span {
  height: 100%;
  display: grid;
  place-items: center;
  color: var(--primary);
  font-size: 64px;
  font-weight: 800;
}

.creator-body {
  min-width: 0;
  display: flex;
  flex-direction: column;
  padding: var(--space-5);
}

.creator-identity {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: var(--space-3);
}

.creator-identity h2 { margin: 0; color: var(--foreground); font-size: 18px; }
.creator-identity p { margin-top: 4px; color: var(--foreground-muted); font-size: 12px; }

.creator-metrics {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  margin: var(--space-5) 0;
}

.creator-metrics span {
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--border-light);
  color: var(--foreground-muted);
  font-size: 12px;
}

.creator-metrics span:last-child { border-right: 0; padding-left: var(--space-3); }
.creator-metrics span:nth-child(2) { padding-left: var(--space-3); }
.creator-metrics strong { color: var(--foreground); font-size: 18px; }

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

.representative-works img { width: 100%; height: 100%; display: block; object-fit: cover; }

@media (max-width: 1050px) {
  .creator-list { grid-template-columns: 1fr; }
}

@media (max-width: 640px) {
  .page-heading { align-items: flex-start; flex-direction: column; }
  .creator-card { grid-template-columns: 1fr; }
  .creator-cover { min-height: 210px; max-height: 260px; }
}
</style>
