<template>
  <div class="profile-page">
    <section v-loading="loading" class="profile-hero">
      <template v-if="profile">
        <div class="profile-main">
          <el-avatar :size="112" :src="profile.avatar">
            {{ displayName.charAt(0) }}
          </el-avatar>
          <div class="profile-copy">
            <span class="eyebrow">PIXEL LAB CREATOR</span>
            <h1>{{ displayName }}</h1>
            <p class="username">@{{ profile.username }}</p>
            <p class="profile-desc">分享作品与灵感的 Pixel Lab 创作者 · {{ joinedText }}</p>
          </div>
        </div>
        <div class="profile-actions">
          <el-button v-if="isSelf" type="primary" @click="router.push('/personal')">
            管理我的主页
          </el-button>
          <el-button
            v-else
            type="primary"
            :plain="isFollowing"
            :loading="followLoading"
            @click="toggleFollow"
          >{{ isFollowing ? '已关注' : '关注' }}</el-button>
        </div>
      </template>
    </section>

    <section v-if="profile" class="stats-grid">
      <div v-for="item in statItems" :key="item.label" class="stat-card">
        <strong>{{ formatNumber(item.value) }}</strong>
        <span>{{ item.label }}</span>
      </div>
    </section>

    <section v-if="profile" class="works-section">
      <div class="section-heading">
        <div>
          <span class="eyebrow">PUBLIC WORKS</span>
          <h2>公开作品</h2>
        </div>
        <span>{{ stats.works || 0 }} 件作品</span>
      </div>

      <div v-if="works.length" class="masonry-grid">
        <PostCard
          v-for="(work, index) in works"
          :key="work.id"
          :work="work"
          :style="{ '--post-ratio': cardRatios[index % cardRatios.length] }"
          @select="openPost"
          @tag-click="searchTag"
          @author-select="() => {}"
        />
      </div>
      <EmptyState
        v-else-if="!loading"
        title="还没有公开作品"
        description="这位创作者暂时没有公开分享。"
        :show-action="false"
      />
    </section>

    <EmptyState
      v-if="!loading && !profile"
      title="用户不存在"
      description="该账号可能已停用或被删除。"
      action-text="返回首页"
      show-action
      @action="router.push('/dashboard')"
    />
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import EmptyState from '@/components/common/EmptyState.vue'
import PostCard from '@/components/community/PostCard.vue'
import { getUserProfile } from '@/api/community'
import { followUser, unfollowUser } from '@/api/social'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const followLoading = ref(false)
const profile = ref(null)
const stats = ref({})
const works = ref([])
const isFollowing = ref(false)
const cardRatios = ['4 / 5', '1 / 1', '5 / 4', '3 / 4', '4 / 3']

const displayName = computed(() => profile.value?.nickname || profile.value?.username || '创作者')
const isSelf = computed(() => Number(profile.value?.id) === Number(userStore.userInfo?.id))
const joinedText = computed(() => {
  if (!profile.value?.created_at) return '持续创作中'
  const date = new Date(profile.value.created_at)
  return `${date.getFullYear()} 年加入`
})
const statItems = computed(() => [
  { label: '作品', value: stats.value.works },
  { label: '获赞', value: stats.value.likes },
  { label: '浏览', value: stats.value.views },
  { label: '粉丝', value: stats.value.followers },
  { label: '关注', value: stats.value.following }
])

const loadProfile = async () => {
  loading.value = true
  profile.value = null
  works.value = []
  try {
    const result = await getUserProfile(route.params.id, { page: 1, pageSize: 40 })
    profile.value = result.user
    stats.value = result.stats || {}
    works.value = result.works?.list || []
    isFollowing.value = Boolean(result.isFollowing)
  } catch (error) {
    console.error('加载创作者主页失败:', error)
  } finally {
    loading.value = false
  }
}

const toggleFollow = async () => {
  followLoading.value = true
  try {
    const result = isFollowing.value
      ? await unfollowUser(profile.value.id)
      : await followUser(profile.value.id)
    isFollowing.value = result.following
    stats.value.followers = Math.max(0, Number(stats.value.followers || 0) + (result.following ? 1 : -1))
    ElMessage.success(result.following ? '关注成功' : '已取消关注')
  } finally {
    followLoading.value = false
  }
}

const openPost = (work) => router.push(`/post/${work.id}`)
const searchTag = (tag) => router.push({ path: '/community', query: { keyword: tag } })
const formatNumber = (value) => Number(value || 0).toLocaleString('zh-CN')

watch(() => route.params.id, loadProfile, { immediate: true })
</script>

<style scoped>
.profile-page {
  width: min(1400px, 100%);
  margin: 0 auto;
}

.profile-hero {
  min-height: 230px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-6);
  overflow: hidden;
  border-radius: var(--radius-lg);
  background:
    radial-gradient(circle at 82% 18%, rgba(91, 141, 239, 0.22), transparent 28%),
    radial-gradient(circle at 18% 88%, rgba(22, 199, 132, 0.2), transparent 30%),
    var(--background-card);
  box-shadow: var(--shadow-sm);
  padding: clamp(var(--space-6), 5vw, var(--space-10));
}

.profile-main { display: flex; align-items: center; gap: var(--space-6); min-width: 0; }
.profile-main :deep(.el-avatar) { flex: 0 0 auto; border: 5px solid var(--background-card); box-shadow: var(--shadow); font-size: 36px; font-weight: 800; }
.profile-copy { min-width: 0; }
.eyebrow { color: var(--primary); font-size: 11px; font-weight: 800; letter-spacing: 0.12em; }
.profile-copy h1 { margin: 7px 0 0; color: var(--foreground); font-size: clamp(30px, 4vw, 46px); line-height: 1.1; }
.username { margin: 6px 0 0; color: var(--foreground-muted); }
.profile-desc { margin: var(--space-4) 0 0; color: var(--foreground-muted); line-height: 1.6; }
.profile-actions { flex: 0 0 auto; }

.stats-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: var(--space-4);
  margin-top: var(--space-5);
}

.stat-card {
  display: flex;
  flex-direction: column;
  border-radius: var(--radius-lg);
  background: var(--background-card);
  box-shadow: var(--shadow-sm);
  padding: var(--space-5);
}

.stat-card strong { color: var(--foreground); font-size: 24px; }
.stat-card span { margin-top: 5px; color: var(--foreground-muted); font-size: 13px; }

.works-section { margin-top: var(--space-8); }
.section-heading { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: var(--space-5); }
.section-heading h2 { margin: 5px 0 0; color: var(--foreground); font-size: 26px; }
.section-heading > span { color: var(--foreground-muted); font-size: 13px; }
.masonry-grid { column-count: 4; column-gap: var(--space-5); }

@media (max-width: 980px) {
  .stats-grid { grid-template-columns: repeat(3, 1fr); }
  .masonry-grid { column-count: 3; }
}

@media (max-width: 700px) {
  .profile-hero,
  .profile-main { align-items: flex-start; flex-direction: column; }
  .profile-actions { width: 100%; }
  .profile-actions .el-button { width: 100%; }
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
  .masonry-grid { column-count: 2; column-gap: var(--space-3); }
}

@media (max-width: 480px) {
  .masonry-grid { column-count: 1; }
}
</style>
