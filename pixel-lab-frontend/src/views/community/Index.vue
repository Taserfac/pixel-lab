<template>
  <div class="community-page">
    <!-- 顶部搜索栏 -->
    <div class="search-bar">
      <el-input
        v-model="keyword"
        placeholder="搜索作品、标签..."
        :prefix-icon="Search"
        clearable
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      />
      <div class="sort-buttons">
        <el-button
          :type="sortBy === 'latest' ? 'primary' : 'default'"
          @click="changeSort('latest')"
        >
          最新
        </el-button>
        <el-button
          :type="sortBy === 'popular' ? 'primary' : 'default'"
          @click="changeSort('popular')"
        >
          最热
        </el-button>
      </div>
    </div>

    <!-- 作品列表 -->
    <div
      v-loading="loading"
      class="works-grid"
    >
      <PostCard
        v-for="(work, index) in works"
        :key="work.id"
        :work="work"
        :style="{ '--post-ratio': getCardRatio(index) }"
        @select="openDetail"
      />
    </div>

    <!-- 空状态 -->
    <EmptyState
      v-if="!loading && works.length === 0"
      title="暂无作品"
      description="还没有公开的作品，快来上传你的第一个作品吧！"
    />

    <!-- 加载更多 -->
    <div
      v-if="hasMore"
      class="load-more"
    >
      <el-button
        :loading="loadingMore"
        @click="loadMore"
      >
        加载更多
      </el-button>
    </div>

    <!-- 作品详情弹窗 -->
    <el-dialog
      v-model="detailVisible"
      :title="currentWork?.title || currentWork?.original_name || '作品详情'"
      width="800px"
      class="detail-dialog"
    >
      <div
        v-if="currentWork"
        class="detail-content"
      >
        <div class="detail-image">
          <img
            :src="currentWork.url"
            alt=""
          >
        </div>
        <div class="detail-side">
          <div class="detail-author">
            <el-avatar
              :size="40"
              :src="currentWork.author_avatar"
            >
              {{ currentWork.author_name?.charAt(0) || '?' }}
            </el-avatar>
            <div class="author-info">
              <span class="name">{{ currentWork.author_name || '匿名' }}</span>
              <span class="time">{{ formatTime(currentWork.created_at) }}</span>
            </div>
          </div>

          <div
            v-if="currentWork.description"
            class="detail-desc"
          >
            {{ currentWork.description }}
          </div>

          <div class="detail-stats">
            <span><el-icon><View /></el-icon> {{ currentWork.view_count || 0 }} 浏览</span>
            <span>♡ {{ currentWork.like_count || 0 }} 点赞</span>
            <span><el-icon><CollectionTag /></el-icon> {{ currentWork.collect_count || 0 }} 收藏</span>
          </div>

          <div class="detail-actions">
            <el-button
              :class="{ 'liked': currentWork.isLiked }"
              @click="handleLike"
            >
              {{ currentWork.isLiked ? '♥' : '♡' }}
              {{ currentWork.isLiked ? '已点赞' : '点赞' }}
            </el-button>
            <el-button
              :class="{ 'collected': currentWork.isCollected }"
              @click="handleCollect"
            >
              <el-icon><FolderOpened /></el-icon>
              {{ currentWork.isCollected ? '已收藏' : '收藏' }}
            </el-button>
          </div>

          <!-- 评论区 -->
          <div class="comments-section">
            <h4>评论 ({{ currentWork.comment_count || 0 }})</h4>

            <div class="comment-input">
              <el-input
                v-model="commentContent"
                type="textarea"
                :rows="2"
                placeholder="写下你的评论..."
                maxlength="500"
                show-word-limit
              />
              <el-button
                type="primary"
                :disabled="!commentContent.trim()"
                @click="submitComment"
              >
                发表
              </el-button>
            </div>

            <div class="comments-list">
              <div
                v-for="comment in comments"
                :key="comment.id"
                class="comment-item"
              >
                <el-avatar
                  :size="32"
                  :src="comment.avatar"
                >
                  {{ comment.nickname?.charAt(0) || '?' }}
                </el-avatar>
                <div class="comment-content">
                  <div class="comment-header">
                    <span class="comment-author">{{ comment.nickname || '匿名' }}</span>
                    <span class="comment-time">{{ formatTime(comment.created_at) }}</span>
                  </div>
                  <p class="comment-text">
                    {{ comment.content }}
                  </p>
                </div>
              </div>

              <EmptyState
                v-if="comments.length === 0"
                title="暂无评论"
                description="快来发表第一条评论吧！"
                :show-action="false"
              />
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Search, View, CollectionTag, FolderOpened } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import EmptyState from '@/components/common/EmptyState.vue'
import PostCard from '@/components/community/PostCard.vue'
import {
  getPublicImages,
  getImageDetail,
  toggleLike,
  toggleCollect,
  getComments,
  addComment
} from '@/api/community'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const route = useRoute()

const loading = ref(false)
const loadingMore = ref(false)
const keyword = ref('')
const sortBy = ref('latest')
const works = ref([])
const page = ref(1)
const pageSize = ref(12) // 减少每页数量，提升加载速度
const total = ref(0)
const cardRatios = ['4 / 5', '1 / 1', '5 / 4', '3 / 4', '4 / 3']

const hasMore = computed(() => works.value.length < total.value)

const detailVisible = ref(false)
const currentWork = ref(null)
const comments = ref([])
const commentContent = ref('')

const loadWorks = async (reset = false) => {
  if (reset) {
    page.value = 1
    works.value = []
  }
  if (reset) {
    loading.value = true
  } else {
    loadingMore.value = true
  }
  try {
    const res = await getPublicImages({
      page: page.value,
      pageSize: pageSize.value,
      keyword: keyword.value,
      sortBy: sortBy.value
    })
    works.value = reset ? res.list : [...works.value, ...res.list]
    total.value = res.total
  } catch (error) {
    console.error('加载失败:', error)
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

const handleSearch = () => loadWorks(true)

const changeSort = (sort) => {
  sortBy.value = sort
  loadWorks(true)
}

const loadMore = () => {
  page.value++
  loadWorks()
}

const getCardRatio = (index) => cardRatios[index % cardRatios.length]

const openDetail = async (work) => {
  detailVisible.value = true
  try {
    const res = await getImageDetail(work.id)
    currentWork.value = res
    // 同步更新列表中的浏览量（详情页会+1）
    const workInList = works.value.find(w => w.id === work.id)
    if (workInList && res.view_count) {
      workInList.view_count = res.view_count
    }
    loadComments(work.id)
  } catch (error) {
    console.error('获取详情失败:', error)
  }
}

const loadComments = async (imageId) => {
  try {
    const res = await getComments(imageId, { page: 1, pageSize: 50 })
    comments.value = res.list
  } catch (error) {
    console.error('加载评论失败:', error)
  }
}

const handleLike = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    const res = await toggleLike(currentWork.value.id)
    currentWork.value.isLiked = res.liked
    currentWork.value.like_count += res.liked ? 1 : -1
    // 同步更新列表中的数据
    const workInList = works.value.find(w => w.id === currentWork.value.id)
    if (workInList) {
      workInList.isLiked = res.liked
      workInList.like_count = currentWork.value.like_count
    }
    ElMessage.success(res.liked ? '点赞成功' : '取消点赞')
  } catch (error) {
    console.error('操作失败:', error)
  }
}

const handleCollect = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    const res = await toggleCollect(currentWork.value.id)
    currentWork.value.isCollected = res.collected
    currentWork.value.collect_count += res.collected ? 1 : -1
    // 同步更新列表中的数据
    const workInList = works.value.find(w => w.id === currentWork.value.id)
    if (workInList) {
      workInList.isCollected = res.collected
      workInList.collect_count = currentWork.value.collect_count
    }
    ElMessage.success(res.collected ? '收藏成功' : '取消收藏')
  } catch (error) {
    console.error('操作失败:', error)
  }
}

const submitComment = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    await addComment({
      imageId: currentWork.value.id,
      content: commentContent.value
    })
    ElMessage.success('评论成功')
    commentContent.value = ''
    loadComments(currentWork.value.id)
    currentWork.value.comment_count++
  } catch (error) {
    console.error('评论失败:', error)
  }
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)} 分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)} 小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)} 天前`
  return date.toLocaleDateString()
}

watch(
  () => route.query.keyword,
  (value) => {
    keyword.value = value ? String(value) : ''
    loadWorks(true)
  }
)

watch(
  () => route.query.id,
  (value) => {
    if (value) openDetail({ id: value })
  }
)

onMounted(async () => {
  if (route.query.keyword) {
    keyword.value = String(route.query.keyword)
  }

  await loadWorks(true)

  if (route.query.id) {
    openDetail({ id: route.query.id })
  }
})
</script>

<style scoped>
.community-page {
  padding: var(--space-6);
  max-width: 1400px;
  margin: 0 auto;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  max-width: 900px;
  margin-bottom: var(--space-8);
  padding: var(--space-3);
  border-radius: var(--radius-xl);
  background: var(--background-card);
  box-shadow: var(--shadow-sm);
}

.search-bar .el-input {
  flex: 1;
  max-width: 400px;
}

.sort-buttons {
  display: flex;
  gap: var(--space-2);
}

.works-grid {
  min-height: 360px;
  column-count: 4;
  column-gap: var(--space-5);
}

.load-more {
  text-align: center;
  margin-top: var(--space-8);
}

/* 详情弹窗 */
.detail-content {
  display: flex;
  gap: var(--space-6);
}

.detail-image {
  flex: 1;
  max-width: 400px;
}

.detail-image img {
  width: 100%;
  border-radius: var(--radius);
}

.detail-side {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.detail-author {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.author-info {
  display: flex;
  flex-direction: column;
}

.author-info .name {
  font-weight: 500;
}

.author-info .time {
  font-size: 12px;
  color: var(--foreground-muted);
}

.detail-desc {
  padding: var(--space-4);
  background: var(--background-muted);
  border-radius: var(--radius);
  font-size: 14px;
  line-height: 1.6;
}

.detail-stats {
  display: flex;
  gap: var(--space-4);
  color: var(--foreground-muted);
  font-size: 13px;
}

.detail-stats span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.detail-actions {
  display: flex;
  gap: var(--space-3);
}

.detail-actions :deep(.el-button.liked) {
  background: var(--error) !important;
  border-color: var(--error) !important;
  color: white !important;
  box-shadow: 0 0 20px rgba(255, 71, 87, 0.3) !important;
}

.detail-actions :deep(.el-button.collected) {
  background: var(--warning) !important;
  border-color: var(--warning) !important;
  color: var(--background) !important;
  box-shadow: 0 0 20px rgba(255, 184, 0, 0.3) !important;
}

.comments-section {
  margin-top: var(--space-4);
  padding-top: var(--space-4);
  border-top: 1px solid var(--border);
}

.comments-section h4 {
  margin-bottom: var(--space-4);
  font-weight: 500;
}

.comment-input {
  display: flex;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
}

.comment-input .el-textarea {
  flex: 1;
}

.comments-list {
  max-height: 300px;
  overflow-y: auto;
}

.comment-item {
  display: flex;
  gap: var(--space-3);
  padding: var(--space-3);
  margin-bottom: var(--space-3);
  background: var(--background-muted);
  border-radius: var(--radius);
}

.comment-content {
  flex: 1;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: var(--space-1);
}

.comment-author {
  font-weight: 500;
  font-size: 13px;
}

.comment-time {
  font-size: 12px;
  color: var(--foreground-muted);
}

.comment-text {
  font-size: 13px;
  line-height: 1.5;
  color: var(--foreground-muted);
}

@media (max-width: 768px) {
  .search-bar {
    align-items: stretch;
    flex-direction: column;
  }

  .search-bar .el-input {
    max-width: none;
  }

  .works-grid {
    column-count: 2;
    column-gap: var(--space-3);
  }

  .detail-content {
    flex-direction: column;
  }

  .detail-image {
    max-width: 100%;
  }
}

@media (max-width: 520px) {
  .works-grid {
    column-count: 1;
  }
}
</style>
