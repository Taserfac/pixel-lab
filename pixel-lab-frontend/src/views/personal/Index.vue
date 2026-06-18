<template>
  <div class="profile-page page-shell">
    <UiCard padding="lg" class="profile-header">
      <div class="profile-identity">
        <el-avatar :size="88" :src="user.avatar">{{ userInitial }}</el-avatar>
        <div>
          <h1>{{ user.nickname || user.username || 'Pixel Creator' }}</h1>
          <p>@{{ user.username || 'creator' }}</p>
          <span>{{ user.bio || '记录灵感，分享作品，也享受每一次创作。' }}</span>
        </div>
      </div>
      <div class="profile-stats">
        <div><strong>{{ formatNumber(stats.works) }}</strong><span>作品</span></div>
        <div><strong>{{ formatNumber(stats.likes) }}</strong><span>获赞</span></div>
        <div><strong>{{ formatNumber(stats.collects) }}</strong><span>收藏</span></div>
      </div>
      <div class="profile-actions">
        <UiButton @click="router.push('/publish')"><template #icon><el-icon><Plus /></el-icon></template>发布作品</UiButton>
        <UiButton variant="secondary" @click="router.push('/settings')"><template #icon><el-icon><Setting /></el-icon></template>编辑资料</UiButton>
      </div>
    </UiCard>

    <nav class="profile-tabs" aria-label="个人内容分类">
      <button v-for="tab in tabs" :key="tab.value" type="button" :class="{ active: activeTab === tab.value }" @click="selectTab(tab.value)">
        <el-icon><component :is="tab.icon" /></el-icon>
        {{ tab.label }}
        <span>{{ tab.count }}</span>
      </button>
    </nav>

    <section v-if="activeTab === 'works'">
      <div class="works-toolbar">
        <div>
          <h2>我的作品</h2>
          <p>管理公开状态，或继续在 Studio 中编辑。</p>
        </div>
        <div class="visibility-filter">
          <button v-for="item in workFilters" :key="item.value" type="button" :class="{ active: workFilter === item.value }" @click="workFilter = item.value">{{ item.label }}</button>
        </div>
      </div>

      <div v-if="loadingWorks" class="profile-grid">
        <SkeletonCard v-for="index in 8" :key="index" />
      </div>
      <div v-else-if="filteredWorks.length" class="profile-grid">
        <UiCard v-for="work in filteredWorks" :key="work.id" padding="none" interactive class="profile-work">
          <button class="work-cover" type="button" @click="router.push(`/post/${work.id}`)">
            <img :src="work.thumbnail_url || work.url" :alt="workTitle(work)">
            <span :class="{ public: isPublic(work) }"><el-icon><Unlock v-if="isPublic(work)" /><Lock v-else /></el-icon>{{ isPublic(work) ? '公开' : '私有' }}</span>
          </button>
          <div class="work-info">
            <div>
              <h3>{{ workTitle(work) }}</h3>
              <p>{{ formatDate(work.created_at) }}</p>
            </div>
            <el-dropdown trigger="click" @command="command => handleWorkCommand(work, command)">
              <button class="more-button" type="button" aria-label="作品操作"><el-icon><MoreFilled /></el-icon></button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="view"><el-icon><View /></el-icon>查看</el-dropdown-item>
                  <el-dropdown-item command="studio"><el-icon><EditPen /></el-icon>在 Studio 中编辑</el-dropdown-item>
                  <el-dropdown-item command="editMeta"><el-icon><EditPen /></el-icon>编辑信息</el-dropdown-item>
                  <el-dropdown-item command="visibility"><el-icon><Lock /></el-icon>{{ isPublic(work) ? '设为私有' : '公开到社区' }}</el-dropdown-item>
                  <el-dropdown-item command="delete" divided><el-icon><Delete /></el-icon>删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
          <div class="work-metrics">
            <span><el-icon><View /></el-icon>{{ formatNumber(work.view_count) }}</span>
            <span><el-icon><Star /></el-icon>{{ formatNumber(work.like_count) }}</span>
            <span><el-icon><CollectionTag /></el-icon>{{ formatNumber(work.collect_count) }}</span>
          </div>
        </UiCard>
      </div>
      <EmptyState v-else title="这里还没有作品" description="发布第一件作品，开始建立你的创作主页。" action-text="发布作品" show-action @action="router.push('/publish')" />
    </section>

    <section v-else>
      <div class="works-toolbar">
        <div>
          <h2>{{ activeTab === 'collections' ? '我的收藏' : '点赞记录' }}</h2>
          <p>{{ activeTab === 'collections' ? '保存值得反复回看的作品。' : '回顾曾经点亮过的灵感。' }}</p>
        </div>
        <UiButton variant="secondary" @click="router.push('/discover')">继续发现</UiButton>
      </div>

      <div v-if="contentLoading" class="profile-grid">
        <SkeletonCard v-for="index in 8" :key="index" />
      </div>
      <div v-else-if="activeItems.length" class="profile-grid">
        <PostCard v-for="(item, index) in activeItems" :key="item.id" :work="item" :style="{ '--post-ratio': ratios[index % ratios.length] }" @select="router.push(`/post/${item.id}`)" />
      </div>
      <EmptyState v-else :title="activeTab === 'collections' ? '暂无收藏' : '暂无点赞记录'" description="去 Discover 看看大家的新作品吧。" action-text="去 Discover" show-action @action="router.push('/discover')" />
    </section>

    <!-- 编辑作品信息弹窗 -->
    <UiModal v-model="editMetaVisible" title="编辑作品信息" max-width="520px">
      <div class="edit-meta-form">
        <label><span>标题</span>
          <el-input v-model="editMetaForm.title" placeholder="作品标题" maxlength="50" />
        </label>
        <label><span>说明</span>
          <el-input v-model="editMetaForm.description" type="textarea" :rows="3" placeholder="添加作品说明..." maxlength="1000" show-word-limit />
        </label>
        <label><span>标签</span>
          <div class="tag-input-area">
            <el-tag
              v-for="(tag, i) in editMetaForm.tags"
              :key="i"
              closable
              size="small"
              @close="removeEditTag(i)"
            >{{ tag }}</el-tag>
            <el-input
              v-model="editMetaTagInput"
              size="small"
              style="width: 120px"
              placeholder="输入标签"
              @keyup.enter="addEditTag"
              @blur="addEditTag"
            />
          </div>
        </label>
      </div>
      <template #footer>
        <UiButton variant="secondary" @click="editMetaVisible = false">取消</UiButton>
        <UiButton :loading="editMetaSaving" @click="saveEditMeta">保存</UiButton>
      </template>
    </UiModal>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { CollectionTag, Delete, EditPen, Lock, MoreFilled, Picture, Plus, Setting, Star, Unlock, View } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import EmptyState from '@/components/common/EmptyState.vue'
import SkeletonCard from '@/components/common/SkeletonCard.vue'
import PostCard from '@/components/community/PostCard.vue'
import UiButton from '@/components/ui/UiButton.vue'
import UiCard from '@/components/ui/UiCard.vue'
import UiModal from '@/components/ui/UiModal.vue'
import { getUserStats } from '@/api/auth'
import { getUserCollections, getUserLikes } from '@/api/community'
import { deleteImage, getUserImages, updateImageMetadata, updateImageVisibility } from '@/api/image'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const user = computed(() => userStore.userInfo || {})
const userInitial = computed(() => (user.value.nickname || user.value.username || 'P').charAt(0).toUpperCase())
const activeTab = ref('works')
const workFilter = ref('all')
const works = ref([])
const collections = ref([])
const likes = ref([])
const loadingWorks = ref(false)
const loadingCollections = ref(false)
const loadingLikes = ref(false)
const stats = reactive({ works: 0, likes: 0, collects: 0 })

// 编辑元数据弹窗
const editMetaVisible = ref(false)
const editMetaWork = ref(null)
const editMetaForm = reactive({ title: '', description: '', tags: [] })
const editMetaTagInput = ref('')
const editMetaSaving = ref(false)
const ratios = ['4 / 5', '1 / 1', '5 / 4', '3 / 4']
const workFilters = [
  { label: '全部', value: 'all' },
  { label: '公开', value: 'public' },
  { label: '私有', value: 'private' }
]

const filteredWorks = computed(() => works.value.filter(item => {
  if (workFilter.value === 'public') return isPublic(item)
  if (workFilter.value === 'private') return !isPublic(item)
  return true
}))

const tabs = computed(() => [
  { label: '我的作品', value: 'works', icon: Picture, count: works.value.length },
  { label: '收藏', value: 'collections', icon: CollectionTag, count: collections.value.length },
  { label: '点赞', value: 'likes', icon: Star, count: likes.value.length }
])

const activeItems = computed(() => activeTab.value === 'collections' ? collections.value : likes.value)
const contentLoading = computed(() => activeTab.value === 'collections' ? loadingCollections.value : loadingLikes.value)
const isPublic = work => work.is_public === true || Number(work.is_public) === 1
const workTitle = work => work.title || work.original_name || work.filename || '未命名作品'
const formatNumber = value => Number(value || 0).toLocaleString('zh-CN')
const formatDate = value => value ? new Intl.DateTimeFormat('zh-CN', { month: 'short', day: 'numeric', year: 'numeric' }).format(new Date(value)) : ''

const loadWorks = async () => {
  loadingWorks.value = true
  try {
    const data = await getUserImages({ page: 1, pageSize: 100 })
    works.value = data?.list || []
    stats.works = Number(data?.pagination?.total ?? works.value.length)
  } catch {
    works.value = []
  } finally {
    loadingWorks.value = false
  }
}

const loadStats = async () => {
  try {
    const data = await getUserStats()
    stats.works = Number(data?.works ?? data?.imageCount ?? stats.works)
    stats.likes = Number(data?.receivedLikeCount ?? data?.likes ?? data?.likeCount ?? 0)
    stats.collects = Number(data?.receivedCollectCount ?? data?.collects ?? data?.collectionCount ?? 0)
  } catch {
    // Profile remains useful when stats are temporarily unavailable.
  }
}

const loadCollections = async () => {
  if (collections.value.length) return
  loadingCollections.value = true
  try {
    const data = await getUserCollections({ page: 1, pageSize: 100 })
    collections.value = data?.list || []
  } catch {
    collections.value = []
  } finally {
    loadingCollections.value = false
  }
}

const loadLikes = async () => {
  if (likes.value.length) return
  loadingLikes.value = true
  try {
    const data = await getUserLikes({ page: 1, pageSize: 100 })
    likes.value = data?.list || []
  } catch {
    likes.value = []
  } finally {
    loadingLikes.value = false
  }
}

const selectTab = value => {
  activeTab.value = value
  if (value === 'collections') loadCollections()
  if (value === 'likes') loadLikes()
}

const handleWorkCommand = async (work, command) => {
  if (command === 'view') return router.push(`/post/${work.id}`)
  if (command === 'studio') return router.push(`/studio/${work.id}`)
  if (command === 'editMeta') return openEditMeta(work)
  if (command === 'visibility') {
    const next = !isPublic(work)
    await updateImageVisibility(work.id, next)
    work.is_public = next ? 1 : 0
    ElMessage.success(next ? '作品已公开到社区' : '作品已设为私有')
    return
  }
  if (command === 'delete') {
    try {
      await ElMessageBox.confirm(`确定删除《${workTitle(work)}》吗？此操作无法撤销。`, '删除作品', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' })
      await deleteImage(work.id)
      works.value = works.value.filter(item => item.id !== work.id)
      stats.works = Math.max(0, stats.works - 1)
      ElMessage.success('作品已删除')
    } catch {
      // User cancelled or the request interceptor already displayed the error.
    }
  }
}

// 编辑作品元数据（标题、说明、标签）
const openEditMeta = work => {
  editMetaWork.value = work
  editMetaForm.title = work.title || ''
  editMetaForm.description = work.description || ''
  editMetaForm.tags = work.tags ? (typeof work.tags === 'string' ? work.tags.split(',').filter(Boolean) : [...work.tags]) : []
  editMetaTagInput.value = ''
  editMetaVisible.value = true
}

const saveEditMeta = async () => {
  if (!editMetaWork.value) return
  editMetaSaving.value = true
  try {
    await updateImageMetadata(editMetaWork.value.id, {
      title: editMetaForm.title,
      description: editMetaForm.description,
      tags: editMetaForm.tags
    })
    // 更新本地数据
    editMetaWork.value.title = editMetaForm.title
    editMetaWork.value.description = editMetaForm.description
    editMetaWork.value.tags = editMetaForm.tags.join(',')
    editMetaVisible.value = false
    ElMessage.success('作品信息已更新')
  } catch {
    ElMessage.error('更新失败，请重试')
  } finally {
    editMetaSaving.value = false
  }
}

const addEditTag = () => {
  const tag = editMetaTagInput.value.trim()
  if (tag && !editMetaForm.tags.includes(tag)) {
    editMetaForm.tags.push(tag)
  }
  editMetaTagInput.value = ''
}

const removeEditTag = index => {
  editMetaForm.tags.splice(index, 1)
}

onMounted(() => {
  loadWorks()
  loadStats()
})
</script>

<style scoped>
.profile-page { max-width: 1240px; padding-bottom: var(--space-8); }

.profile-header {
  display: grid;
  grid-template-columns: 1fr auto auto;
  align-items: center;
  gap: var(--space-8);
}
.profile-identity { min-width: 0; display: flex; align-items: center; gap: var(--space-4); }
.profile-identity h1 { margin: 0; font-size: 26px; line-height: 1.25; }
.profile-identity p { margin: 2px 0; color: var(--primary-active); font-size: 12px; }
.profile-identity span { display: block; color: var(--text-secondary); font-size: 13px; }
.profile-stats { display: flex; gap: var(--space-8); }
.profile-stats div { display: grid; justify-items: center; }
.profile-stats strong { font-size: 20px; }
.profile-stats span { color: var(--text-tertiary); font-size: 11px; }
.profile-actions { display: flex; gap: var(--space-2); }

.profile-tabs { display: flex; gap: var(--space-2); margin: var(--space-8) 0; border-bottom: 1px solid var(--border); }
.profile-tabs button { min-height: 46px; display: inline-flex; align-items: center; gap: var(--space-2); position: relative; border: 0; padding: 0 var(--space-4); color: var(--text-secondary); background: transparent; font-weight: 650; cursor: pointer; }
.profile-tabs button:hover,
.profile-tabs button.active { color: var(--primary-active); }
.profile-tabs button.active::after { content: ''; height: 3px; position: absolute; right: 10px; bottom: -1px; left: 10px; border-radius: var(--radius-full); background: var(--primary); }
.profile-tabs button span { min-width: 20px; border-radius: var(--radius-full); padding: 1px 6px; color: var(--text-tertiary); background: var(--surface-muted); font-size: 10px; }

.works-toolbar { display: flex; align-items: end; justify-content: space-between; gap: var(--space-4); margin-bottom: var(--space-6); }
.works-toolbar h2 { margin: 0; font-size: 20px; }
.works-toolbar p { margin: 2px 0 0; color: var(--text-secondary); font-size: 12px; }
.visibility-filter { display: flex; gap: 2px; border: 1px solid var(--border); border-radius: var(--radius-md); padding: 3px; background: var(--card); }
.visibility-filter button { border: 0; border-radius: var(--radius-sm); padding: 6px 12px; color: var(--text-secondary); background: transparent; cursor: pointer; }
.visibility-filter button.active { color: var(--primary-active); background: var(--primary-soft); }

.profile-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: var(--space-4); align-items: start; }
.profile-work { min-width: 0; overflow: hidden; }
.work-cover { width: 100%; display: block; position: relative; overflow: hidden; border: 0; padding: 0; background: var(--surface-muted); cursor: pointer; }
.work-cover img { width: 100%; aspect-ratio: 4 / 3; display: block; object-fit: cover; transition: transform var(--transition-slow); }
.profile-work:hover .work-cover img { transform: scale(1.035); }
.work-cover > span { display: inline-flex; align-items: center; gap: 3px; position: absolute; top: var(--space-2); left: var(--space-2); border-radius: var(--radius-full); padding: 3px 8px; color: var(--text-secondary); background: var(--surface-glass); font-size: 10px; backdrop-filter: blur(10px); }
.work-cover > span.public { color: var(--primary-active); }
.work-info { display: flex; align-items: center; justify-content: space-between; gap: var(--space-2); padding: var(--space-3); }
.work-info > div { min-width: 0; }
.work-info h3 { overflow: hidden; margin: 0; font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }
.work-info p { margin: 2px 0 0; color: var(--text-tertiary); font-size: 10px; }
.more-button { width: 32px; height: 32px; display: grid; place-items: center; border: 0; border-radius: var(--radius-md); color: var(--text-secondary); background: transparent; cursor: pointer; }
.more-button:hover { color: var(--primary-active); background: var(--primary-soft); }
.work-metrics { display: flex; gap: var(--space-3); padding: 0 var(--space-3) var(--space-3); color: var(--text-tertiary); font-size: 10px; }
.work-metrics span { display: inline-flex; align-items: center; gap: 3px; }

@media (max-width: 1020px) {
  .profile-header { grid-template-columns: 1fr auto; }
  .profile-actions { grid-column: 1 / -1; }
  .profile-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}

@media (max-width: 720px) {
  .profile-header { grid-template-columns: 1fr; }
  .profile-stats { justify-content: space-around; }
  .profile-actions { grid-column: auto; }
  .profile-tabs { overflow-x: auto; }
  .profile-tabs button { white-space: nowrap; }
  .works-toolbar { align-items: flex-start; flex-direction: column; }
  .profile-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}

.edit-meta-form { display: grid; gap: var(--space-4); }
.edit-meta-form label { display: grid; gap: var(--space-2); }
.edit-meta-form label > span { font-size: 12px; font-weight: 600; color: var(--text-secondary); }
.tag-input-area { display: flex; flex-wrap: wrap; gap: var(--space-1); align-items: center; padding: var(--space-2); border: 1px solid var(--border); border-radius: var(--radius-md); min-height: 40px; }
</style>
