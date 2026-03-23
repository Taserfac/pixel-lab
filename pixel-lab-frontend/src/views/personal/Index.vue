<!--
  【文件路径】src/views/personal/Index.vue
  【功能说明】个人中心页面
  - 用户信息展示
  - 我的图片库
  - 上传图片
-->

<template>
  <div class="personal-page">
    <!-- 用户信息卡片 -->
    <el-card class="user-card">
      <div class="user-info">
        <div class="avatar">
          <el-avatar :size="80" :src="userInfo.avatar || defaultAvatar">
            {{ userInfo.username?.charAt(0).toUpperCase() }}
          </el-avatar>
        </div>
        <div class="info">
          <h2>{{ userInfo.nickname || userInfo.username }}</h2>
          <p class="username">@{{ userInfo.username }}</p>
          <div class="stats">
            <div class="stat-item">
              <span class="number">{{ stats.totalImages }}</span>
              <span class="label">图片</span>
            </div>
            <div class="stat-item">
              <span class="number">{{ stats.publicImages }}</span>
              <span class="label">公开</span>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 操作栏 -->
    <div class="action-bar">
      <el-button type="primary" @click="handleUpload">
        <el-icon><Upload /></el-icon>
        上传图片
      </el-button>
      <input
        ref="fileInput"
        type="file"
        accept="image/*"
        style="display: none"
        @change="onFileSelected"
      />
    </div>

    <!-- 图片列表 -->
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>我的图片</span>
          <el-radio-group v-model="filterType" size="small">
            <el-radio-button label="all">全部</el-radio-button>
            <el-radio-button label="public">公开</el-radio-button>
            <el-radio-button label="private">私有</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <div v-if="filteredImages.length > 0" class="image-grid">
        <div
          v-for="image in filteredImages"
          :key="image.id"
          class="image-item"
        >
          <div class="image-wrapper">
            <img :src="image.url" :alt="image.original_name" />
            <div class="image-overlay">
              <div class="actions">
                <el-button
                  circle
                  size="small"
                  @click="viewImage(image)"
                >
                  <el-icon><View /></el-icon>
                </el-button>
                <el-button
                  circle
                  size="small"
                  :type="image.is_public ? 'success' : 'info'"
                  @click="toggleVisibility(image)"
                >
                  <el-icon>
                    <Unlock v-if="image.is_public" />
                    <Lock v-else />
                  </el-icon>
                </el-button>
                <el-button
                  circle
                  size="small"
                  type="danger"
                  @click="confirmDelete(image)"
                >
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </div>
          </div>
          <div class="image-info">
            <p class="name" :title="image.original_name">
              {{ image.original_name }}
            </p>
            <p class="date">{{ formatDate(image.created_at) }}</p>
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
      />
      <template #footer>
        <el-button @click="previewVisible = false">关闭</el-button>
        <el-button type="primary" @click="openInWorkbench">
          在工作台打开
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, View, Lock, Unlock, Delete } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { uploadImage, getUserImages, deleteImage, updateImageVisibility } from '@/api/image'
import EmptyState from '@/components/common/EmptyState.vue'

const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo || {})

// 默认头像
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

// 统计数据
const stats = ref({
  totalImages: 0,
  publicImages: 0
})

// 图片列表
const images = ref([])
const loading = ref(false)
const filterType = ref('all')

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
  
  // 检查文件大小（10MB）
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过10MB')
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

// 在工作台打开
const openInWorkbench = () => {
  // TODO: 跳转到工作台并加载图片
  ElMessage.info('功能开发中...')
  previewVisible.value = false
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

// 格式化日期
const formatDate = (dateStr) => {
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN')
}

onMounted(() => {
  fetchImages()
})
</script>

<style scoped>
.personal-page {
  padding: var(--space-4);
}

.user-card {
  margin-bottom: var(--space-4);
}

.user-info {
  display: flex;
  align-items: center;
  gap: var(--space-6);
}

.user-info .info h2 {

  margin: 0 0 var(--space-2) 0;
  font-size: 24px;
}

.user-info .info .username {
  color: var(--foreground-muted);
  margin-bottom: var(--space-4);
}

.stats {
  display: flex;
  gap: var(--space-8);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-item .number {
  font-size: 20px;
  font-weight: 600;
  color: var(--primary);
}

.stat-item .label {
  font-size: 14px;
  color: var(--foreground-muted);
}

.action-bar {
  margin-bottom: var(--space-4);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: var(--space-4);
}

.image-item {
  border-radius: var(--radius-lg);
  overflow: hidden;
  background: var(--background-elevated);
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
  transition: transform 0.3s;
}

.image-wrapper:hover img {
  transform: scale(1.05);
}

.image-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.image-wrapper:hover .image-overlay {
  opacity: 1;
}

.image-overlay .actions {
  display: flex;
  gap: var(--space-2);
}

.image-info {
  padding: var(--space-3);
}

.image-info .name {
  margin: 0;
  font-size: 14px;
  color: var(--foreground);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.image-info .date {
  margin: var(--space-1) 0 0 0;
  font-size: 12px;
  color: var(--foreground-muted);
}
</style>
