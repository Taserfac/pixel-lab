<!--
  【文件路径】src/views/personal/Index.vue
  【功能说明】个人中心页面
  - 用户信息展示
  - 我的图片库
  - 我的收藏/点赞
  - 上传图片
-->

<template>
  <div class="personal-page">
    <!-- 用户信息卡片 -->
    <el-card class="user-card">
      <div class="user-info">
        <div class="avatar">
          <el-avatar
            :size="80"
            :src="userInfo.avatar || defaultAvatar"
          >
            {{ userInfo.username?.charAt(0).toUpperCase() }}
          </el-avatar>
        </div>
        <div class="info">
          <h2>{{ userInfo.nickname || userInfo.username }}</h2>
          <p class="username">
            @{{ userInfo.username }}
          </p>
          <div class="stats">
            <div class="stat-item">
              <span class="number">{{ stats.totalImages }}</span>
              <span class="label">图片</span>
            </div>
            <div class="stat-item">
              <span class="number">{{ stats.publicImages }}</span>
              <span class="label">公开</span>
            </div>
            <div class="stat-item">
              <span class="number">{{ stats.likes }}</span>
              <span class="label">点赞</span>
            </div>
            <div class="stat-item">
              <span class="number">{{ stats.collections }}</span>
              <span class="label">收藏</span>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 操作栏 -->
    <div class="action-bar">
      <el-button
        type="primary"
        @click="handleUpload"
      >
        <el-icon><Upload /></el-icon>
        上传图片
      </el-button>
      <input
        ref="fileInput"
        type="file"
        accept="image/*"
        style="display: none"
        @change="onFileSelected"
      >
    </div>

    <!-- Tab 切换 -->
    <el-tabs
      v-model="activeTab"
      class="content-tabs"
      @tab-change="handleTabChange"
    >
      <el-tab-pane
        label="我的图片"
        name="images"
      >
        <el-card v-loading="loading">
          <template #header>
            <div class="card-header">
              <span>我的图片</span>
              <el-radio-group
                v-model="filterType"
                size="small"
              >
                <el-radio-button label="all">
                  全部
                </el-radio-button>
                <el-radio-button label="public">
                  公开
                </el-radio-button>
                <el-radio-button label="private">
                  私有
                </el-radio-button>
              </el-radio-group>
            </div>
          </template>

          <div
            v-if="filteredImages.length > 0"
            class="image-grid"
          >
            <div
              v-for="image in filteredImages"
              :key="image.id"
              class="image-item"
            >
              <div class="image-wrapper">
                <img
                  :src="image.url"
                  :alt="image.original_name"
                  loading="lazy"
                >
                <el-dropdown
                  class="image-actions-dropdown"
                  trigger="click"
                  placement="bottom-end"
                  @command="(command) => handleImageCommand(image, command)"
                >
                  <button
                    class="image-menu-btn"
                    type="button"
                    aria-label="图片设置"
                  >
                    <el-icon><MoreFilled /></el-icon>
                  </button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="view">
                        <el-icon><View /></el-icon>
                        查看图片
                      </el-dropdown-item>
                      <el-dropdown-item command="visibility">
                        <el-icon>
                          <Lock v-if="image.is_public" />
                          <Unlock v-else />
                        </el-icon>
                        {{ image.is_public ? '设为私有' : '设为公开' }}
                      </el-dropdown-item>
                      <el-dropdown-item
                        command="delete"
                        divided
                      >
                        <el-icon><Delete /></el-icon>
                        删除图片
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
              <div class="image-info">
                <p
                  class="name"
                  :title="image.original_name"
                >
                  {{ image.original_name }}
                </p>
                <p class="date">
                  {{ formatDate(image.created_at) }}
                </p>
              </div>
            </div>
          </div>

          <EmptyState
            v-else
            title="暂无图片"
            description="点击上方「上传图片」按钮添加你的第一张图片"
            :show-action="true"
            action-text="上传图片"
            @action="handleUpload"
          />
        </el-card>
      </el-tab-pane>

      <el-tab-pane
        label="我的收藏"
        name="collections"
      >
        <el-card v-loading="loadingCollections">
          <template #header>
            <span>我的收藏 ({{ collectionsTotal }})</span>
          </template>

          <div
            v-if="collections.length > 0"
            class="image-grid"
          >
            <div
              v-for="image in collections"
              :key="image.id"
              class="image-item"
              @click="goToCommunity(image.id)"
            >
              <div class="image-wrapper">
                <img
                  :src="image.url"
                  :alt="image.title || image.original_name"
                  loading="lazy"
                >
              </div>
              <div class="image-info">
                <p
                  class="name"
                  :title="image.title || image.original_name"
                >
                  {{ image.title || image.original_name }}
                </p>
                <p class="author">
                  by {{ image.author_name || '匿名' }}
                </p>
              </div>
            </div>
          </div>

          <EmptyState
            v-else
            title="暂无收藏"
            description="去社区广场发现喜欢的作品吧"
            :show-action="true"
            action-text="逛逛社区"
            @action="goToCommunity()"
          />
        </el-card>
      </el-tab-pane>

      <el-tab-pane
        label="我的点赞"
        name="likes"
      >
        <el-card v-loading="loadingLikes">
          <template #header>
            <span>我的点赞 ({{ likesTotal }})</span>
          </template>

          <div
            v-if="likes.length > 0"
            class="image-grid"
          >
            <div
              v-for="image in likes"
              :key="image.id"
              class="image-item"
              @click="goToCommunity(image.id)"
            >
              <div class="image-wrapper">
                <img
                  :src="image.url"
                  :alt="image.title || image.original_name"
                  loading="lazy"
                >
              </div>
              <div class="image-info">
                <p
                  class="name"
                  :title="image.title || image.original_name"
                >
                  {{ image.title || image.original_name }}
                </p>
                <p class="author">
                  by {{ image.author_name || '匿名' }}
                </p>
              </div>
            </div>
          </div>

          <EmptyState
            v-else
            title="暂无点赞"
            description="去社区广场发现喜欢的作品吧"
            :show-action="true"
            action-text="逛逛社区"
            @action="goToCommunity()"
          />
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 图片预览对话框 -->
    <el-dialog
      v-model="previewVisible"
      title="图片预览"
      width="80%"
      center
    >
      <img
        :src="previewImage?.url"
        style="width: 100%; max-height: 60vh; object-fit: contain"
      >
      <template #footer>
        <el-button @click="previewVisible = false">
          关闭
        </el-button>
        <el-button
          type="primary"
          @click="openInWorkbench"
        >
          在工作台打开
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, View, Lock, Unlock, Delete, MoreFilled } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { uploadImage, getUserImages, deleteImage, updateImageVisibility } from '@/api/image'
import { getUserCollections, getUserLikes } from '@/api/community'
import EmptyState from '@/components/common/EmptyState.vue'

const router = useRouter()
const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo || {})

// 默认头像
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

// 统计数据
const stats = ref({
  totalImages: 0,
  publicImages: 0,
  likes: 0,
  collections: 0
})

// Tab
const activeTab = ref('images')

// 图片列表
const images = ref([])
const loading = ref(false)
const filterType = ref('all')

// 收藏列表
const collections = ref([])
const collectionsTotal = ref(0)
const loadingCollections = ref(false)

// 点赞列表
const likes = ref([])
const likesTotal = ref(0)
const loadingLikes = ref(false)

// 文件输入
const fileInput = ref(null)

// 预览
const previewVisible = ref(false)
const previewImage = ref(null)

// 过滤后的图片
const filteredImages = computed(() => {
  if (filterType.value === 'public') {
    return images.value.filter(img => img.is_public)
  }
  if (filterType.value === 'private') {
    return images.value.filter(img => !img.is_public)
  }
  return images.value
})

// Tab 切换
const handleTabChange = (tab) => {
  if (tab === 'collections') {
    fetchCollections()
  } else if (tab === 'likes') {
    fetchLikes()
  }
}

// 获取图片列表
const fetchImages = async () => {
  loading.value = true
  try {
    const res = await getUserImages()
    images.value = res.list || []
    stats.value.totalImages = res.pagination?.total || 0
    stats.value.publicImages = images.value.filter(img => img.is_public).length
  } catch (error) {
    console.error('获取图片列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 获取收藏列表
const fetchCollections = async () => {
  loadingCollections.value = true
  try {
    const res = await getUserCollections({ pageSize: 50 })
    collections.value = res.list || []
    collectionsTotal.value = res.total || 0
    stats.value.collections = res.total || 0
  } catch (error) {
    console.error('获取收藏列表失败:', error)
  } finally {
    loadingCollections.value = false
  }
}

// 获取点赞列表
const fetchLikes = async () => {
  loadingLikes.value = true
  try {
    const res = await getUserLikes({ pageSize: 50 })
    likes.value = res.list || []
    likesTotal.value = res.total || 0
    stats.value.likes = res.total || 0
  } catch (error) {
    console.error('获取点赞列表失败:', error)
  } finally {
    loadingLikes.value = false
  }
}

// 上传图片
const handleUpload = () => {
  fileInput.value?.click()
}

const onFileSelected = async (e) => {
  const file = e.target.files[0]
  if (!file) return
  
  // 检查文件类型
  if (!file.type.startsWith('image/')) {
    ElMessage.error('请选择图片文件')
    return
  }
  
  // 检查文件大小（5MB）
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过5MB')
    return
  }
  
  try {
    await uploadImage(file)
    ElMessage.success('上传成功')
    fetchImages()
  } catch (error) {
    console.error('上传失败:', error)
    ElMessage.error('上传失败')
  }
  
  // 清空 input
  e.target.value = ''
}

// 查看图片
const viewImage = (image) => {
  previewImage.value = image
  previewVisible.value = true
}

const handleImageCommand = (image, command) => {
  if (command === 'view') {
    viewImage(image)
  } else if (command === 'visibility') {
    toggleVisibility(image)
  } else if (command === 'delete') {
    confirmDelete(image)
  }
}

// 在工作台打开
const openInWorkbench = (image) => {
  try {
    if (image && image.url) {
      // 在工作台中加载图片逻辑可在 future 继续增强
      localStorage.setItem('last-open-image', image.url)
    }
    router.push('/workbench')
  } catch (error) {
    console.error('跳转工作台失败', error)
    ElMessage.error('跳转工作台失败')
  } finally {
    previewVisible.value = false
  }
}

// 切换可见性
const toggleVisibility = async (image) => {
  try {
    await updateImageVisibility(image.id, !image.is_public)
    image.is_public = !image.is_public
    stats.value.publicImages = images.value.filter(img => img.is_public).length
    ElMessage.success(image.is_public ? '已设为公开' : '已设为私有')
  } catch (error) {
    console.error('更新失败:', error)
    ElMessage.error('操作失败')
  }
}

// 删除图片
const confirmDelete = (image) => {
  ElMessageBox.confirm(
    '确定要删除这张图片吗？此操作不可恢复。',
    '确认删除',
    {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await deleteImage(image.id)
      ElMessage.success('删除成功')
      fetchImages()
    } catch (error) {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

// 跳转到社区
const goToCommunity = (imageId) => {
  if (imageId) {
    router.push({ path: '/community', query: { id: imageId } })
  } else {
    router.push('/community')
  }
}

// 格式化日期
const formatDate = (dateStr) => {
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN')
}

onMounted(() => {
  fetchImages()
  // 预加载统计数据
  fetchCollections()
  fetchLikes()
})
</script>

<style scoped>
.personal-page {
  padding: var(--space-6);
}

.user-card {
  margin-bottom: var(--space-5);
}

.user-card :deep(.el-card__body) {
  padding: var(--space-6);
}

.user-info {
  display: flex;
  align-items: center;
  gap: var(--space-6);
}

.user-info :deep(.el-avatar) {
  border: 4px solid var(--border);
  box-shadow: 6px 6px 0px 0px var(--border);
}

.user-info .info h2 {
  margin: 0 0 var(--space-2) 0;
  font-size: 28px;
  font-weight: 700;
  font-family: var(--font-mono);
  text-transform: uppercase;
  letter-spacing: 0.02em;
}

.user-info .info .username {
  color: var(--foreground-muted);
  margin-bottom: var(--space-4);
  font-size: 14px;
}

.stats {
  display: flex;
  gap: var(--space-4);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--space-3) var(--space-4);
  background: var(--accent);
  border: 3px solid var(--border);
  box-shadow: 4px 4px 0px 0px var(--border);
}

.stat-item .number {
  font-size: 24px;
  font-weight: 700;
  font-family: var(--font-mono);
  color: var(--foreground);
}

.stat-item .label {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--foreground-muted);
}

.action-bar {
  margin-bottom: var(--space-5);
}

.content-tabs {
  margin-bottom: var(--space-5);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header span {
  font-size: 18px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: var(--space-4);
}

.image-item {
  overflow: hidden;
  background: var(--background-soft);
  border: 4px solid var(--border);
  box-shadow: 6px 6px 0px 0px var(--border);
  transition: all var(--transition-fast);
  cursor: pointer;
}

.image-item:hover {
  transform: translate(-4px, -4px);
  box-shadow: 10px 10px 0px 0px var(--border);
}

.image-wrapper {
  position: relative;
  aspect-ratio: 1;
  overflow: hidden;
}

.image-wrapper img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform var(--transition-base);
}

.image-wrapper:hover img {
  transform: scale(1.05);
}

.image-actions-dropdown {
  position: absolute;
  top: var(--space-2);
  right: var(--space-2);
  z-index: 3;
}

.image-menu-btn {
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(255, 255, 255, 0.72);
  border-radius: var(--radius-full);
  color: #ffffff;
  background: rgba(10, 10, 10, 0.72);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
  cursor: pointer;
  transition:
    background var(--transition-fast),
    border-color var(--transition-fast),
    color var(--transition-fast),
    transform var(--transition-fast);
}

.image-menu-btn:hover,
.image-menu-btn:focus-visible {
  color: var(--background);
  background: var(--primary);
  border-color: var(--primary);
  transform: translateY(-1px);
  outline: none;
}

.image-info {
  padding: var(--space-3);
  border-top: 4px solid var(--border);
}

.image-info .name {
  margin: 0;
  font-size: 13px;
  font-weight: 700;
  color: var(--foreground);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.image-info .date,
.image-info .author {
  margin: var(--space-1) 0 0 0;
  font-size: 12px;
  color: var(--foreground-muted);
}

@media (max-width: 768px) {
  .user-info {
    flex-direction: column;
    text-align: center;
  }
  
  .stats {
    flex-wrap: wrap;
    justify-content: center;
  }
  
  .image-grid {
    grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
    gap: var(--space-3);
  }
}
</style>
