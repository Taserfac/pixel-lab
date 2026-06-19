<template>
  <div class="community-page">
    <!-- 顶部搜索栏 -->
    <div class="search-bar">
      <el-input
        v-model="keyword"
        :placeholder="$t('community.searchPlaceholder')"
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
          {{ $t('community.latest') }}
        </el-button>
        <el-button
          :type="sortBy === 'popular' ? 'primary' : 'default'"
          @click="changeSort('popular')"
        >
          {{ $t('community.hottest') }}
        </el-button>
      </div>
    </div>

    <!-- 标签筛选栏 -->
    <div class="tag-filter-bar">
      <button
        v-for="tag in tagList"
        :key="tag"
        type="button"
        class="tag-filter-chip"
        :class="{ active: activeTag === tag }"
        @click="selectTag(tag)"
      >
        {{ tag === '全部' ? $t('community.all') : `#${tag}` }}
      </button>
    </div>

    <!-- 作品列表 -->
    <StableMasonry
      class="works-grid"
      :works="works"
      :loading="loading"
      :skeleton-count="12"
      @select="openPost"
      @tag-click="handleTagClick"
      @author-select="openCreator"
    />

    <!-- 无限滚动哨兵 -->
    <div
      v-if="hasMore"
      ref="scrollSentinel"
      class="scroll-sentinel"
    >
      <el-button
        v-if="loadingMore"
        :loading="true"
        text
      />
    </div>

    <!-- 空状态 -->
    <EmptyState
      v-if="!loading && works.length === 0"
      :title="$t('community.noWorks')"
      :description="$t('community.noWorksDesc')"
    />

    <!-- 图片灯箱 -->
    <!-- 作品详情弹窗 -->
    <el-dialog
      v-model="detailVisible"
      :title="currentWork?.title || currentWork?.original_name || $t('workDetail.details')"
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
              <span class="name">{{ currentWork.author_name || $t('workDetail.anonymous') }}</span>
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
            <span><el-icon><View /></el-icon> {{ currentWork.view_count || 0 }} {{ $t('workDetail.views') }}</span>
            <span>♡ {{ currentWork.like_count || 0 }} {{ $t('workDetail.likes') }}</span>
            <span><el-icon><CollectionTag /></el-icon> {{ currentWork.collect_count || 0 }} {{ $t('workDetail.collects') }}</span>
          </div>

          <div class="detail-actions">
            <el-button
              :class="{ 'liked': currentWork.isLiked }"
              @click="handleLike"
            >
              {{ currentWork.isLiked ? '♥' : '♡' }}
              {{ currentWork.isLiked ? $t('workDetail.liked') : $t('workDetail.unliked') }}
            </el-button>
            <el-button
              :class="{ 'collected': currentWork.isCollected }"
              @click="handleCollect"
            >
              <el-icon><FolderOpened /></el-icon>
              {{ currentWork.isCollected ? $t('workDetail.collected') : $t('workDetail.uncollected') }}
            </el-button>
            <el-button @click="handleReport">
              <el-icon><Warning /></el-icon> 举报
            </el-button>
            <el-button @click="handleShare">
              <el-icon><Share /></el-icon> {{ $t('action.share') }}
            </el-button>
          </div>

          <!-- 评论区 -->
          <div class="comments-section">
            <h4>{{ $t('community.comments') }} ({{ currentWork.comment_count || 0 }})</h4>

            <div class="comment-input">
              <el-input
                v-model="commentContent"
                type="textarea"
                :rows="2"
                :placeholder="replyTo ? `回复 @${replyTo}...` : $t('community.commentPlaceholder')"
                maxlength="500"
                show-word-limit
              />
              <div class="comment-input-actions">
                <el-button
                  v-if="replyTo"
                  text
                  @click="cancelReply"
                >
                  取消回复
                </el-button>
                <el-button
                  type="primary"
                  :disabled="!commentContent.trim()"
                  @click="submitComment"
                >
                  {{ $t('community.publish') }}
                </el-button>
              </div>
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
                    <span class="comment-author">{{ comment.nickname || $t('workDetail.anonymous') }}</span>
                    <span class="comment-time">{{ formatTime(comment.created_at) }}</span>
                  </div>
                  <p class="comment-text">
                    {{ comment.content }}
                  </p>
                  <button
                    type="button"
                    class="reply-btn"
                    @click="setReplyTo(comment)"
                  >
                    回复
                  </button>
                  <!-- 子评论 -->
                  <div
                    v-if="comment.replies && comment.replies.length"
                    class="comment-replies"
                  >
                    <div
                      v-for="reply in comment.replies"
                      :key="reply.id"
                      class="reply-item"
                    >
                      <el-avatar
                        :size="24"
                        :src="reply.avatar"
                      >
                        {{ reply.nickname?.charAt(0) || '?' }}
                      </el-avatar>
                      <div class="reply-content">
                        <span class="reply-author">{{ reply.nickname || $t('workDetail.anonymous') }}</span>
                        <span v-if="reply.replyTo" class="reply-to"> 回复 {{ reply.replyTo }}</span>
                        <p>{{ reply.content }}</p>
                        <span class="reply-time">{{ formatTime(reply.created_at) }}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <EmptyState
                v-if="comments.length === 0"
                :title="$t('community.noComments')"
                :description="$t('community.noCommentsDesc')"
                :show-action="false"
              />
            </div>
          </div>

          <!-- 相似作品推荐 -->
          <div
            v-if="similarWorks.length"
            class="similar-section"
          >
            <h4>相似作品</h4>
            <div class="similar-grid">
              <div
                v-for="sw in similarWorks"
                :key="sw.id"
                class="similar-item"
                @click="openDetail(sw)"
              >
                <img
                  :src="sw.url"
                  :alt="sw.title"
                >
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Search, View, CollectionTag, FolderOpened, Share, Warning } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import EmptyState from '@/components/common/EmptyState.vue'
import StableMasonry from '@/components/community/StableMasonry.vue'
import {
  getPublicImages,
  getImageDetail,
  toggleLike,
  toggleCollect,
  reportImage,
  getComments,
  addComment
} from '@/api/community'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const loading = ref(false)
const loadingMore = ref(false)
const keyword = ref('')
const sortBy = ref('latest')
const works = ref([])
const page = ref(1)
const pageSize = ref(12)
const total = ref(0)
const activeTag = ref('')
const normalizeTags = (value) => (
  Array.isArray(value) ? value : String(value || '').split(/[,，]/)
).map(tag => tag.trim()).filter(Boolean)
const tagList = computed(() => {
  const defaults = ['插画', '摄影', '设计', '像素艺术', 'AI艺术']
  const discovered = works.value.flatMap(work => normalizeTags(work.tags))
  return ['全部', ...new Set([...defaults, ...discovered])]
})

const hasMore = computed(() => works.value.length < total.value)

const detailVisible = ref(false)
const currentWork = ref(null)
const comments = ref([])
const commentContent = ref('')
const replyTo = ref('')
const similarWorks = ref([])

// 无限滚动
const scrollSentinel = ref(null)
let scrollObserver = null

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

const selectTag = (tag) => {
  if (tag === '全部' || tag === activeTag.value) {
    activeTag.value = ''
    keyword.value = ''
  } else {
    activeTag.value = tag
    keyword.value = tag
  }
  loadWorks(true)
}

const handleTagClick = (tag) => {
  activeTag.value = tag
  keyword.value = tag
  loadWorks(true)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const openPost = (work) => router.push(`/post/${work.id}`)

const openCreator = (work) => {
  const id = work.user_id || work.author_id
  if (id) router.push(`/user/${id}`)
}

const openDetail = async (work) => {
  detailVisible.value = true
  try {
    const res = await getImageDetail(work.id)
    currentWork.value = res
    const workInList = works.value.find(w => w.id === work.id)
    if (workInList && res.view_count) {
      workInList.view_count = res.view_count
    }
    loadComments(work.id)
    loadSimilarWorks(work.id)
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

const loadSimilarWorks = async (imageId) => {
  try {
    const { getSimilarWorks } = await import('@/api/social')
    const res = await getSimilarWorks(imageId, { limit: 4 })
    similarWorks.value = res.list || res || []
  } catch {
    similarWorks.value = []
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

const handleReport = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  if (!currentWork.value?.id) return
  try {
    const { value } = await ElMessageBox.prompt(
      '请填写举报原因，管理员会在后台处理。',
      '举报作品',
      {
        confirmButtonText: '提交举报',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputPlaceholder: '例如：侵权、违规内容、垃圾信息等',
        inputValidator: (input) => {
          const text = String(input || '').trim()
          if (!text) return '请填写举报原因'
          if (text.length > 500) return '举报原因不能超过 500 字'
          return true
        }
      }
    )
    const detail = String(value || '').trim()
    await reportImage({
      imageId: currentWork.value.id,
      reason: detail.slice(0, 80),
      detail
    })
    ElMessage.success('举报已提交，管理员会尽快处理')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.msg || '举报提交失败')
    }
  }
}

const handleShare = async () => {
  const url = window.location.origin + `/post/${currentWork.value.id}`
  const title = currentWork.value.title || 'Pixel Lab 作品'
  if (navigator.share) {
    try {
      await navigator.share({ title, url })
    } catch {
      // 用户取消系统分享时无需提示错误。
    }
  } else {
    try {
      await navigator.clipboard.writeText(url)
      ElMessage.success(t('common.copySuccess'))
    } catch {
      ElMessage.info('请手动复制链接: ' + url)
    }
  }
}

const setReplyTo = (comment) => {
  replyTo.value = comment.nickname || '匿名'
  commentContent.value = ''
}

const cancelReply = () => {
  replyTo.value = ''
  commentContent.value = ''
}

const submitComment = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    const data = {
      imageId: currentWork.value.id,
      content: commentContent.value
    }
    if (replyTo.value) {
      data.replyTo = replyTo.value
    }
    await addComment(data)
    ElMessage.success('评论成功')
    commentContent.value = ''
    replyTo.value = ''
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

const setupInfiniteScroll = () => {
  nextTick(() => {
    if (!scrollSentinel.value) return
    scrollObserver = new IntersectionObserver((entries) => {
      if (entries[0].isIntersecting && hasMore.value && !loadingMore.value && !loading.value) {
        page.value++
        loadWorks()
      }
    }, { rootMargin: '200px' })
    scrollObserver.observe(scrollSentinel.value)
  })
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
  setupInfiniteScroll()
  if (route.query.id) {
    openDetail({ id: route.query.id })
  }
})

onUnmounted(() => {
  scrollObserver?.disconnect()
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

.tag-filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-bottom: var(--space-6);
}

.tag-filter-chip {
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

.tag-filter-chip:hover,
.tag-filter-chip.active {
  color: var(--primary);
  background: var(--primary-muted);
}

.works-grid {
  min-height: 360px;
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

.comment-input-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-2);
  margin-top: var(--space-2);
}

.reply-btn {
  border: 0;
  background: transparent;
  color: var(--foreground-muted);
  font-size: 12px;
  cursor: pointer;
  padding: 0;
}

.reply-btn:hover {
  color: var(--primary);
}

.comment-replies {
  margin-top: var(--space-3);
  padding-left: var(--space-4);
  border-left: 2px solid var(--border);
}

.reply-item {
  display: flex;
  gap: var(--space-2);
  padding: var(--space-2) 0;
}

.reply-content {
  flex: 1;
  font-size: 12px;
}

.reply-author {
  font-weight: 600;
  color: var(--foreground);
}

.reply-to {
  color: var(--foreground-muted);
}

.reply-content p {
  margin: 2px 0;
  color: var(--foreground-muted);
  line-height: 1.5;
}

.reply-time {
  color: var(--foreground-subtle, var(--foreground-muted));
  font-size: 11px;
}

.scroll-sentinel {
  display: flex;
  justify-content: center;
  padding: var(--space-6) 0;
}

.similar-section {
  margin-top: var(--space-6);
  padding-top: var(--space-4);
  border-top: 1px solid var(--border);
}

.similar-section h4 {
  margin-bottom: var(--space-4);
  font-weight: 600;
}

.similar-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-3);
}

.similar-item {
  aspect-ratio: 1;
  border-radius: var(--radius-md);
  overflow: hidden;
  cursor: pointer;
  transition: transform var(--transition-fast);
}

.similar-item:hover {
  transform: scale(1.05);
}

.similar-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

@media (max-width: 768px) {
  .search-bar {
    align-items: stretch;
    flex-direction: column;
  }

  .search-bar .el-input {
    max-width: none;
  }

  .tag-filter-bar {
    gap: var(--space-1);
  }

  .tag-filter-chip {
    padding: 6px 12px;
    font-size: 12px;
  }

  .detail-content {
    flex-direction: column;
  }

  .detail-image {
    max-width: 100%;
  }
}

</style>
