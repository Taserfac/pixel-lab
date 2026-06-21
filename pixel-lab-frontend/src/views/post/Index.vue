<template>
  <div class="post-page">
    <button class="back-button" type="button" @click="router.back()">
      <el-icon><ArrowLeft /></el-icon>
      返回
    </button>

    <div v-loading="loading" class="post-stage">
      <template v-if="post">
        <section class="post-visual">
          <img :src="post.url" :alt="postTitle">
        </section>

        <aside class="post-panel">
          <button class="author-card" type="button" @click="openAuthor">
            <el-avatar :size="46" :src="post.author_avatar">
              {{ authorName.charAt(0) }}
            </el-avatar>
            <span>
              <strong>{{ authorName }}</strong>
              <small>{{ formatDate(post.created_at) }}</small>
            </span>
            <span class="author-link">查看主页</span>
          </button>

          <div class="post-copy">
            <h1>{{ postTitle }}</h1>
            <p v-if="post.description">{{ post.description }}</p>
            <div v-if="tags.length" class="tag-list">
              <button
                v-for="tag in tags"
                :key="tag"
                type="button"
                @click="router.push({ path: '/community', query: { keyword: tag } })"
              >#{{ tag }}</button>
            </div>
          </div>

          <div v-if="isOwner" class="owner-row">
            <el-button plain @click="openEditor">
              <el-icon><Edit /></el-icon>
              编辑标题、标签与描述
            </el-button>
          </div>

          <div class="metric-row">
            <span><el-icon><View /></el-icon>{{ formatNumber(post.view_count) }} 浏览</span>
            <span><el-icon><Star /></el-icon>{{ formatNumber(post.like_count) }} 点赞</span>
            <span><el-icon><CollectionTag /></el-icon>{{ formatNumber(post.collect_count) }} 收藏</span>
            <span><el-icon><ChatDotRound /></el-icon>{{ formatNumber(post.comment_count) }} 评论</span>
          </div>

          <div class="action-row">
            <el-button
              :class="{ active: post.isLiked }"
              :loading="liking"
              @click="handleLike"
            >
              <el-icon><StarFilled v-if="post.isLiked" /><Star v-else /></el-icon>
              {{ post.isLiked ? '已点赞' : '点赞' }}
            </el-button>
            <el-button
              :class="{ active: post.isCollected }"
              :loading="collecting"
              @click="handleCollect"
            >
              <el-icon><CollectionTag /></el-icon>
              {{ post.isCollected ? '已收藏' : '收藏' }}
            </el-button>
            <el-button
              :loading="reporting"
              @click="handleReport"
            >
              <el-icon><Warning /></el-icon>
              举报
            </el-button>
          </div>

          <section class="comments-section">
            <div class="section-heading">
              <h2>评论</h2>
              <span>{{ post.comment_count || 0 }} 条</span>
            </div>
            <div class="comment-composer">
              <el-avatar :size="34" :src="userStore.userInfo?.avatar">
                {{ currentUserInitial }}
              </el-avatar>
              <div class="composer-main">
                <el-input
                  v-model="commentContent"
                  type="textarea"
                  :rows="2"
                  maxlength="500"
                  show-word-limit
                  :placeholder="replyTarget ? `回复 @${replyTarget.nickname}…` : '说说你对这件作品的想法…'"
                />
                <div
                  v-if="replyTarget"
                  class="replying-tip"
                >
                  <span>正在回复 @{{ replyTarget.nickname }}</span>
                  <button
                    type="button"
                    @click="cancelReply"
                  >
                    取消回复
                  </button>
                </div>
                <el-button
                  type="primary"
                  :loading="commenting"
                  :disabled="!commentContent.trim()"
                  @click="submitComment"
                >发表评论</el-button>
              </div>
            </div>

            <div v-if="comments.length" class="comment-list">
              <article v-for="comment in comments" :key="comment.id" class="comment-item">
                <button type="button" class="comment-avatar" @click="openCommentAuthor(comment)">
                  <el-avatar :size="36" :src="comment.avatar">
                    {{ (comment.nickname || '匿').charAt(0) }}
                  </el-avatar>
                </button>
                <div>
                  <div class="comment-meta">
                    <button type="button" @click="openCommentAuthor(comment)">
                      {{ comment.nickname || '匿名用户' }}
                    </button>
                    <div class="comment-tools">
                      <span>{{ formatDate(comment.created_at) }}</span>
                      <button
                        type="button"
                        class="reply-comment"
                        @click="setReplyTo(comment)"
                      >回复</button>
                      <button
                        v-if="canDeleteComment(comment)"
                        type="button"
                        class="delete-comment"
                        @click="removeComment(comment)"
                      >删除</button>
                    </div>
                  </div>
                  <p>{{ comment.content }}</p>
                  <div
                    v-if="comment.replies?.length"
                    class="reply-list"
                  >
                    <article
                      v-for="reply in comment.replies"
                      :key="reply.id"
                      class="reply-item"
                    >
                      <button type="button" class="comment-avatar" @click="openCommentAuthor(reply)">
                        <el-avatar :size="28" :src="reply.avatar">
                          {{ (reply.nickname || '匿').charAt(0) }}
                        </el-avatar>
                      </button>
                      <div>
                        <div class="comment-meta">
                          <button type="button" @click="openCommentAuthor(reply)">
                            {{ reply.nickname || '匿名用户' }}
                          </button>
                          <div class="comment-tools">
                            <span>{{ formatDate(reply.created_at) }}</span>
                            <button
                              type="button"
                              class="reply-comment"
                              @click="setReplyTo(reply, comment)"
                            >回复</button>
                            <button
                              v-if="canDeleteComment(reply)"
                              type="button"
                              class="delete-comment"
                              @click="removeComment(reply)"
                            >删除</button>
                          </div>
                        </div>
                        <p>
                          <span v-if="reply.replyToName" class="reply-to">回复 @{{ reply.replyToName }}：</span>{{ reply.content }}
                        </p>
                      </div>
                    </article>
                  </div>
                </div>
              </article>
            </div>
            <div v-else class="empty-comments">还没有评论，来留下第一句话吧。</div>
          </section>
        </aside>
      </template>

      <EmptyState
        v-else-if="!loading"
        title="作品不存在"
        description="该作品可能已被删除或设为私密。"
        action-text="返回社区"
        show-action
        @action="router.push('/community')"
      />
    </div>

    <el-dialog v-model="editVisible" title="编辑作品信息" width="min(520px, 92vw)">
      <el-form label-position="top">
        <el-form-item label="标题">
          <el-input v-model="editForm.title" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="标签">
          <el-select
            v-model="editForm.tags"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="选择或输入标签"
            style="width: 100%"
          >
            <el-option v-for="option in tagOptions" :key="option" :label="option" :value="option" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" :rows="5" maxlength="2000" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveMetadata">保存修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  ChatDotRound,
  CollectionTag,
  Edit,
  Star,
  StarFilled,
  View,
  Warning
} from '@element-plus/icons-vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { addComment, deleteComment, getComments, getImageDetail, reportImage, toggleCollect, toggleLike } from '@/api/community'
import { updateImageMetadata } from '@/api/image'
import { imageDisplayTitle } from '@/utils/common'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const liking = ref(false)
const collecting = ref(false)
const reporting = ref(false)
const commenting = ref(false)
const post = ref(null)
const comments = ref([])
const commentContent = ref('')
const replyTarget = ref(null)
const editVisible = ref(false)
const saving = ref(false)
const editForm = ref({ title: '', tags: [], description: '' })
const tagOptions = ['插画', '摄影', '设计', 'UI设计', '概念艺术', '动漫', '像素艺术', 'AI艺术', '人像摄影', '风景', '治愈系']

const postTitle = computed(() => imageDisplayTitle(post.value))
const isOwner = computed(() => Number(post.value?.user_id) === Number(userStore.userInfo?.id))
const authorName = computed(() => post.value?.author_name || '匿名创作者')
const currentUserInitial = computed(() => (
  userStore.userInfo?.nickname || userStore.userInfo?.username || '我'
).charAt(0))
const tags = computed(() => {
  const value = post.value?.tags
  if (Array.isArray(value)) return value
  return String(value || '').split(/[,，]/).map(tag => tag.trim()).filter(Boolean)
})

const loadPost = async () => {
  const id = route.params.id
  loading.value = true
  post.value = null
  comments.value = []
  try {
    const [detail, commentData] = await Promise.all([
      getImageDetail(id),
      getComments(id, { page: 1, pageSize: 100 })
    ])
    post.value = detail
    comments.value = normalizeComments(commentData.list || [])
  } catch (error) {
    console.error('加载作品失败:', error)
  } finally {
    loading.value = false
  }
}

const openAuthor = () => {
  if (post.value?.user_id) router.push(`/user/${post.value.user_id}`)
}

const openCommentAuthor = (comment) => {
  if (comment.user_id) router.push(`/user/${comment.user_id}`)
}

const handleLike = async () => {
  liking.value = true
  try {
    const result = await toggleLike(post.value.id)
    post.value.isLiked = result.liked
    post.value.like_count = Math.max(0, Number(post.value.like_count || 0) + (result.liked ? 1 : -1))
  } finally {
    liking.value = false
  }
}

const handleCollect = async () => {
  collecting.value = true
  try {
    const result = await toggleCollect(post.value.id)
    post.value.isCollected = result.collected
    post.value.collect_count = Math.max(0, Number(post.value.collect_count || 0) + (result.collected ? 1 : -1))
  } finally {
    collecting.value = false
  }
}

const handleReport = async () => {
  if (!post.value?.id) return
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
    reporting.value = true
    const detail = String(value || '').trim()
    await reportImage({
      imageId: post.value.id,
      reason: detail.slice(0, 80),
      detail
    })
    ElMessage.success('举报已提交，管理员会尽快处理')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.msg || '举报提交失败')
    }
  } finally {
    reporting.value = false
  }
}

const canDeleteComment = (comment) => (
  isOwner.value || Number(comment.user_id) === Number(userStore.userInfo?.id)
)

const normalizeComments = (list = []) => {
  const items = list.map(item => ({ ...item, replies: [] }))
  const byId = new Map(items.map(item => [Number(item.id), item]))
  const roots = []

  items.forEach(item => {
    const parentId = Number(item.parent_id || item.parentId || 0)
    if (parentId && byId.has(parentId)) {
      const parent = byId.get(parentId)
      item.replyToName = parent.nickname || '匿名用户'
      parent.replies.push(item)
    } else {
      roots.push(item)
    }
  })

  roots.forEach(comment => {
    comment.replies.sort((a, b) => new Date(a.created_at) - new Date(b.created_at))
  })
  return roots
}

const removeComment = async (comment) => {
  await ElMessageBox.confirm('确定删除这条评论吗？', '删除评论', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteComment(comment.id)
  const removedCount = 1 + (comment.replies?.length || 0)
  comments.value = comments.value
    .filter(item => item.id !== comment.id)
    .map(item => ({
      ...item,
      replies: (item.replies || []).filter(reply => reply.id !== comment.id)
    }))
  post.value.comment_count = Math.max(0, Number(post.value.comment_count || 0) - removedCount)
  ElMessage.success('评论已删除')
}

const setReplyTo = (comment, rootComment = comment) => {
  replyTarget.value = {
    id: rootComment.id,
    nickname: comment.nickname || '匿名用户'
  }
  commentContent.value = ''
}

const cancelReply = () => {
  replyTarget.value = null
  commentContent.value = ''
}

const openEditor = () => {
  editForm.value = {
    title: post.value.title || '',
    tags: Array.isArray(post.value.tags)
      ? [...post.value.tags]
      : String(post.value.tags || '').split(/[,，]/).map(tag => tag.trim()).filter(Boolean),
    description: post.value.description || ''
  }
  editVisible.value = true
}

const saveMetadata = async () => {
  saving.value = true
  try {
    const payload = {
      title: editForm.value.title.trim(),
      tags: editForm.value.tags.map(tag => tag.trim()).filter(Boolean).join(','),
      description: editForm.value.description.trim()
    }
    await updateImageMetadata(post.value.id, payload)
    Object.assign(post.value, payload)
    editVisible.value = false
    ElMessage.success('作品信息已更新')
  } finally {
    saving.value = false
  }
}

const submitComment = async () => {
  const content = commentContent.value.trim()
  if (!content) return
  commenting.value = true
  try {
    const payload = { imageId: post.value.id, content }
    if (replyTarget.value?.id) payload.parentId = replyTarget.value.id
    await addComment(payload)
    commentContent.value = ''
    replyTarget.value = null
    const result = await getComments(post.value.id, { page: 1, pageSize: 100 })
    comments.value = normalizeComments(result.list || [])
    post.value.comment_count = Number(post.value.comment_count || 0) + 1
    ElMessage.success('评论已发布')
  } finally {
    commenting.value = false
  }
}

const formatNumber = (value) => Number(value || 0).toLocaleString('zh-CN')
const formatDate = (value) => {
  if (!value) return ''
  return new Date(value).toLocaleDateString('zh-CN', { year: 'numeric', month: 'short', day: 'numeric' })
}

watch(() => route.params.id, loadPost, { immediate: true })
</script>

<style scoped>
.post-page {
  width: min(1480px, 100%);
  margin: 0 auto;
}

.back-button,
.author-card,
.comment-avatar,
.comment-meta button,
.tag-list button {
  border: 0;
  background: transparent;
  color: inherit;
  cursor: pointer;
}

.back-button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-bottom: var(--space-4);
  color: var(--foreground-muted);
  font-size: 14px;
}

.back-button:hover,
.comment-meta button:hover {
  color: var(--primary);
}

.post-stage {
  min-height: 560px;
}

.post-stage > :first-child:not(.el-loading-mask) {
  min-height: inherit;
}

.post-stage:has(.post-visual) {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(400px, 0.85fr);
  overflow: hidden;
  border-radius: var(--radius-lg);
  background: var(--background-card);
  box-shadow: var(--shadow-sm);
}

.post-visual {
  min-height: 720px;
  display: grid;
  place-items: center;
  border-right: 1px solid var(--border);
  background:
    radial-gradient(circle at 18% 18%, var(--primary-muted), transparent 34%),
    radial-gradient(circle at 82% 78%, var(--secondary-muted), transparent 38%),
    linear-gradient(145deg, var(--background-elevated), var(--background-muted));
  padding: clamp(16px, 3vw, 44px);
}

.post-visual img {
  max-width: 100%;
  max-height: 78vh;
  display: block;
  object-fit: contain;
  border-radius: var(--radius-sm);
  box-shadow: var(--shadow-md);
}

.post-panel {
  min-width: 0;
  max-height: calc(100vh - 150px);
  overflow-y: auto;
  padding: var(--space-6);
}

.author-card {
  width: 100%;
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: 0 0 var(--space-5);
  text-align: left;
}

.author-card > span:nth-child(2) {
  min-width: 0;
  flex: 1;
}

.author-card strong,
.author-card small {
  display: block;
}

.author-card strong { color: var(--foreground); font-size: 15px; }
.author-card small { margin-top: 4px; color: var(--foreground-muted); }
.author-link { color: var(--primary); font-size: 12px; }

.post-copy {
  border-top: 1px solid var(--border-light);
  padding: var(--space-5) 0;
}

.post-copy h1 {
  margin: 0;
  color: var(--foreground);
  font-size: clamp(24px, 3vw, 34px);
  line-height: 1.25;
}

.post-copy p {
  margin: var(--space-4) 0 0;
  color: var(--foreground-muted);
  line-height: 1.75;
  white-space: pre-wrap;
}

.tag-list { display: flex; flex-wrap: wrap; gap: 8px; margin-top: var(--space-4); }
.tag-list button { border-radius: var(--radius-full); background: var(--primary-muted); color: var(--primary); padding: 5px 10px; }

.metric-row {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-4);
  color: var(--foreground-muted);
  font-size: 13px;
}

.metric-row span { display: inline-flex; align-items: center; gap: 5px; }
.action-row { display: flex; gap: var(--space-3); margin: var(--space-5) 0; }
.action-row .el-button { flex: 1; margin: 0; }
.action-row .active { border-color: var(--primary); background: var(--primary-muted); color: var(--primary); }

.comments-section { border-top: 1px solid var(--border-light); padding-top: var(--space-5); }
.section-heading { display: flex; align-items: center; justify-content: space-between; }
.section-heading h2 { margin: 0; color: var(--foreground); font-size: 18px; }
.section-heading span { color: var(--foreground-muted); font-size: 13px; }
.comment-composer { display: flex; align-items: flex-start; gap: var(--space-3); margin-top: var(--space-4); }
.composer-main { min-width: 0; flex: 1; }
.composer-main .el-button { float: right; margin-top: var(--space-2); }
.comment-list { clear: both; display: grid; gap: var(--space-5); padding-top: var(--space-6); }
.comment-item { display: grid; grid-template-columns: 36px minmax(0, 1fr); gap: var(--space-3); }
.comment-avatar { padding: 0; }
.comment-meta { display: flex; align-items: center; justify-content: space-between; gap: var(--space-2); }
.comment-meta button { padding: 0; color: var(--foreground); font-weight: 700; }
.comment-meta span { color: var(--foreground-subtle); font-size: 12px; }
.comment-item p { margin: 6px 0 0; color: var(--foreground-muted); line-height: 1.6; white-space: pre-wrap; }
.empty-comments { clear: both; padding: var(--space-8) 0; color: var(--foreground-muted); text-align: center; }

.replying-tip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
  margin-top: var(--space-2);
  color: var(--foreground-muted);
  font-size: 12px;
}

.replying-tip button,
.comment-tools .reply-comment {
  border: 0;
  background: transparent;
  color: var(--primary);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.reply-list {
  display: grid;
  gap: var(--space-3);
  margin-top: var(--space-3);
  padding-left: var(--space-4);
  border-left: 2px solid var(--border-light);
}

.reply-item {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr);
  gap: var(--space-2);
}

.reply-to {
  color: var(--foreground);
  font-weight: 700;
}

.owner-row {
  display: flex;
  justify-content: flex-end;
  margin-bottom: var(--space-4);
}

.comment-tools {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
}

.comment-tools .delete-comment {
  color: var(--error);
  font-size: 12px;
  font-weight: 600;
}

@media (max-width: 980px) {
  .post-stage:has(.post-visual) { grid-template-columns: 1fr; }
  .post-visual { min-height: 420px; border-right: 0; border-bottom: 1px solid var(--border); }
  .post-panel { max-height: none; }
}

@media (max-width: 560px) {
  .post-visual { min-height: 320px; padding: 12px; }
  .post-panel { padding: var(--space-4); }
  .metric-row { gap: var(--space-3); }
}
</style>
