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

    <section v-if="profile && albums.length" class="albums-section">
      <div class="section-heading">
        <div>
          <span class="eyebrow">PUBLIC ALBUMS</span>
          <h2>公开作品集</h2>
        </div>
        <span>{{ albums.length }} 个作品集</span>
      </div>
      <div class="album-grid">
        <button v-for="album in albums" :key="album.id" type="button" class="album-card" @click="openAlbum(album)">
          <span class="album-cover">
            <img v-if="album.cover_thumbnail_url || album.cover_url" :src="album.cover_thumbnail_url || album.cover_url" :alt="album.title">
            <span v-else class="album-cover-placeholder">作品集</span>
            <small>{{ album.image_count || 0 }} 张</small>
          </span>
          <span class="album-copy">
            <strong>{{ album.title }}</strong>
            <span v-if="album.description">{{ album.description }}</span>
          </span>
        </button>
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

      <StableMasonry
        v-if="works.length"
        :works="works"
        :max-columns="4"
        :loading="loading"
        @select="openPost"
        @tag-click="searchTag"
        @author-select="() => {}"
      />
      <EmptyState
        v-else-if="!loading"
        title="还没有公开作品"
        description="这位创作者暂时没有公开分享。"
        :show-action="false"
      />
    </section>

    <el-dialog v-model="albumDialogVisible" :title="selectedAlbum?.title || '作品集'" width="min(900px, 92vw)">
      <div v-loading="albumLoading" class="album-dialog-content">
        <p v-if="selectedAlbum?.description" class="album-dialog-description">{{ selectedAlbum.description }}</p>
        <div v-if="selectedAlbum?.images?.length" class="album-image-grid">
          <figure v-for="image in selectedAlbum.images" :key="image.id" class="album-image-item">
            <img :src="cardImageUrl(image)" :alt="imageDisplayTitle(image)" loading="lazy" decoding="async">
            <figcaption>
              <strong>{{ imageDisplayTitle(image) }}</strong>
              <span v-if="image.album_description">{{ image.album_description }}</span>
            </figcaption>
          </figure>
        </div>
        <EmptyState
          v-else-if="!albumLoading"
          title="作品集为空"
          description="这个作品集暂时还没有图片。"
          :show-action="false"
        />
      </div>
    </el-dialog>

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
import StableMasonry from '@/components/community/StableMasonry.vue'
import { getUserProfile } from '@/api/community'
import { followUser, unfollowUser } from '@/api/social'
import { useUserStore } from '@/store/user'
import { getAlbumDetail } from '@/api/album'
import { cardImageUrl } from '@/utils/media'
import { imageDisplayTitle } from '@/utils/common'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const followLoading = ref(false)
const profile = ref(null)
const stats = ref({})
const works = ref([])
const isFollowing = ref(false)
const albums = ref([])
const selectedAlbum = ref(null)
const albumDialogVisible = ref(false)
const albumLoading = ref(false)

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
  albums.value = []
  try {
    const result = await getUserProfile(route.params.id, { page: 1, pageSize: 40 })
    profile.value = result.user
    stats.value = result.stats || {}
    works.value = result.works?.list || []
    albums.value = result.albums || []
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
const openAlbum = async (album) => {
  albumDialogVisible.value = true
  albumLoading.value = true
  selectedAlbum.value = null
  try {
    selectedAlbum.value = await getAlbumDetail(album.id)
  } catch (error) {
    console.error('加载作品集失败:', error)
    albumDialogVisible.value = false
    ElMessage.error('作品集加载失败')
  } finally {
    albumLoading.value = false
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

.albums-section { margin-top: var(--space-8); }

.album-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(230px, 1fr));
  gap: var(--space-4);
}

.album-card {
  min-width: 0;
  overflow: hidden;
  border: 0;
  border-radius: var(--radius-lg);
  background: var(--background-card);
  color: inherit;
  padding: 0;
  text-align: left;
  box-shadow: var(--shadow-sm);
  cursor: pointer;
  transition: transform var(--transition-fast), box-shadow var(--transition-fast);
}

.album-card:hover,
.album-card:focus-visible {
  transform: translateY(-3px);
  box-shadow: var(--shadow);
}

.album-cover {
  position: relative;
  display: block;
  aspect-ratio: 16 / 10;
  overflow: hidden;
  background: var(--background-muted);
}

.album-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.album-cover-placeholder {
  display: grid;
  height: 100%;
  place-items: center;
  color: var(--foreground-muted);
}

.album-cover small {
  position: absolute;
  right: var(--space-3);
  bottom: var(--space-3);
  border-radius: var(--radius-full);
  background: rgba(20, 26, 23, 0.72);
  color: #fff;
  padding: 4px 9px;
}

.album-copy {
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: var(--space-4);
}

.album-copy strong { color: var(--foreground); }

.album-copy > span,
.album-dialog-description,
.album-image-item figcaption span { color: var(--foreground-muted); }

.album-image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: var(--space-4);
}

.album-image-item { margin: 0; }

.album-image-item img {
  width: 100%;
  aspect-ratio: 4 / 3;
  border-radius: var(--radius);
  object-fit: cover;
}

.album-image-item figcaption {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: var(--space-2);
}

@media (max-width: 980px) {
  .stats-grid { grid-template-columns: repeat(3, 1fr); }
}

@media (max-width: 700px) {
  .profile-hero,
  .profile-main { align-items: flex-start; flex-direction: column; }
  .profile-actions { width: 100%; }
  .profile-actions .el-button { width: 100%; }
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
