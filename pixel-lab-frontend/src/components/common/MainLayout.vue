<template>
  <div class="community-layout" :class="{ 'fixed-editor-layout': isFixedEditor }">
    <DynamicBackground
      :style-name="themeStore.backgroundStyle"
      :motion="themeStore.backgroundMotion"
      :mode="dynamicBackgroundMode"
    />

    <header v-if="!isFullscreenEditor" class="top-bar">
      <router-link
        to="/dashboard"
        class="brand"
        aria-label="Pixel Lab 首页"
      >
        <BrandLogo
          size="md"
          tone="panel"
          shadow
        />
        <span class="brand-name">Pixel Lab</span>
        <span class="brand-tag">{{ $t('community.communityTag') }}</span>
      </router-link>

      <div
        class="search-bar"
        @mouseleave="scheduleSearchSuggestionsClose"
      >
        <el-input
          ref="searchInputRef"
          v-model="searchQuery"
          :placeholder="$t('community.searchPlaceholderFull')"
          :prefix-icon="Search"
          clearable
          @keyup.enter="handleSearch"
          @click="openSearchSuggestions"
        />
        <div
          v-if="showSearchSuggestions && (searchHistory.length || hotSearchTags.length)"
          class="search-suggestions"
        >
          <div
            v-if="searchHistory.length"
            class="suggestion-section"
          >
            <div class="suggestion-header">
              <span>最近搜索</span>
              <button
                type="button"
                @click="clearSearchHistory"
              >
                清除
              </button>
            </div>
            <button
              v-for="item in searchHistory"
              :key="item"
              type="button"
              class="suggestion-item"
              @click="quickSearch(item)"
            >
              <el-icon><Clock /></el-icon> {{ item }}
            </button>
          </div>
          <div
            v-if="hotSearchTags.length"
            class="suggestion-section"
          >
            <div class="suggestion-header">
              <span>热门搜索</span>
            </div>
            <div class="hot-tags">
              <button
                v-for="tag in hotSearchTags"
                :key="tag"
                type="button"
                class="hot-tag-item"
                @click="quickSearch(tag)"
              >
                #{{ tag }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <div class="top-actions">
        <el-popover
          placement="bottom-end"
          :width="360"
          trigger="click"
        >
          <template #reference>
            <button
              class="icon-action"
              type="button"
              :title="$t('notification.title')"
              :aria-label="$t('notification.title')"
            >
              <el-icon><Bell /></el-icon>
              <span
                v-if="unreadCount > 0"
                class="notice-badge"
              >{{ unreadCount }}</span>
            </button>
          </template>
          <div class="notification-panel">
            <div class="notification-header">
              <strong>{{ $t('notification.title') }}</strong>
              <button
                type="button"
                class="mark-read-btn"
                @click="markAllRead"
              >
                {{ $t('notification.markAllRead') }}
              </button>
            </div>
            <div class="notification-list">
              <div
                v-for="n in notifications"
                :key="n.id"
                class="notification-item"
                :class="{ unread: !n.read }"
                @click="openNotification(n)"
              >
                <span
                  class="notification-icon"
                  :style="{ color: getNotificationColor(n.type) }"
                >
                  <el-icon><component :is="getNotificationIcon(n.type)" /></el-icon>
                </span>
                <div class="notification-content">
                  <p>{{ n.text }}</p>
                  <span class="notification-time">{{ n.time }}</span>
                </div>
              </div>
              <div
                v-if="notifications.length === 0"
                class="notification-empty"
              >
                {{ $t('notification.noNotifications') }}
              </div>
            </div>
          </div>
        </el-popover>
        <el-popover
          placement="bottom-end"
          :width="324"
          trigger="click"
          popper-class="theme-menu-popper"
        >
          <template #reference>
            <button
              class="icon-action"
              type="button"
              :title="themeToggleLabel"
              :aria-label="themeToggleLabel"
            >
              <el-icon>
                <Sunny v-if="isDarkTheme" />
                <Moon v-else />
              </el-icon>
            </button>
          </template>
          <div class="theme-menu">
            <div class="theme-menu-head">
              <strong>主题外观</strong>
              <span>{{ isDarkTheme ? '夜间模式' : '白天模式' }}</span>
            </div>
            <div class="theme-row">
              <button
                v-for="option in themeModeOptions"
                :key="option.value"
                type="button"
                class="theme-chip"
                :class="{ active: themeStore.theme === option.value }"
                @click="themeStore.setTheme(option.value)"
              >
                <el-icon><component :is="option.icon" /></el-icon>
                {{ option.label }}
              </button>
            </div>

            <div class="theme-section-title">动态背景</div>
            <div class="background-style-grid">
              <button
                v-for="option in backgroundStyleOptions"
                :key="option.value"
                type="button"
                class="background-style-option"
                :class="[`preview-${option.value}`, { active: themeStore.backgroundStyle === option.value }]"
                @click="themeStore.setBackgroundStyle(option.value)"
              >
                <span class="style-preview" />
                <span>
                  <strong>{{ option.label }}</strong>
                  <small>{{ option.desc }}</small>
                </span>
              </button>
            </div>

            <div class="theme-section-title">动态效果</div>
            <div class="theme-row motion-row">
              <button
                v-for="option in backgroundMotionOptions"
                :key="option.value"
                type="button"
                class="theme-chip"
                :class="{ active: themeStore.backgroundMotion === option.value }"
                @click="themeStore.setBackgroundMotion(option.value)"
              >
                {{ option.label }}
              </button>
            </div>
          </div>
        </el-popover>
        <el-dropdown @command="handleCommand">
          <button
            class="user-avatar"
            type="button"
            aria-label="用户菜单"
          >
            <el-avatar
              :size="36"
              :src="userStore.userInfo?.avatar"
            >
              {{ userStore.userInfo?.nickname?.charAt(0) || 'U' }}
            </el-avatar>
            <el-icon class="avatar-caret"><ArrowDown /></el-icon>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">
                <el-icon><User /></el-icon>{{ $t('nav.personal') }}
              </el-dropdown-item>
              <el-dropdown-item command="settings">
                <el-icon><Setting /></el-icon>{{ $t('nav.settings') }}
              </el-dropdown-item>
              <el-dropdown-item
                v-if="userStore.isAdmin"
                command="admin"
              >
                <el-icon><Setting /></el-icon>{{ $t('nav.admin') }}
              </el-dropdown-item>
              <el-dropdown-item
                divided
                command="logout"
              >
                <el-icon><SwitchButton /></el-icon>{{ $t('auth.logout') }}
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <input
      ref="fileInput"
      class="visually-hidden"
      type="file"
      accept="image/*"
      @change="onFileSelected"
    >

    <el-dialog
      v-model="uploadSuccessVisible"
      class="upload-success-dialog"
      title="上传完成"
      width="520px"
      :close-on-click-modal="false"
      @closed="resetUploadSuccess"
    >
      <template v-if="!editingUploadedMetadata">
        <div class="upload-success-message">
          <span class="upload-success-icon">✓</span>
          <div>
            <strong>上传成功！</strong>
            <p>你可以继续编辑图片，或先补充标签和说明。</p>
          </div>
        </div>
      </template>
      <el-form v-else label-position="top" class="upload-metadata-form">
        <el-form-item label="标签">
          <el-select
            v-model="uploadedMetadata.tags"
            :loading="uploadTagOptionsLoading"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="选择已有标签，或输入新标签后按回车添加"
            style="width: 100%"
          >
            <el-option
              v-for="tag in sortedUploadTagOptions"
              :key="tag"
              :label="tag"
              :value="tag"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="说明">
          <el-input
            v-model="uploadedMetadata.description"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="写下这张图片的创作想法或内容说明..."
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <template v-if="editingUploadedMetadata">
          <el-button :disabled="savingUploadedMetadata" @click="editingUploadedMetadata = false">返回</el-button>
          <el-button type="primary" :loading="savingUploadedMetadata" @click="saveUploadedMetadata">保存</el-button>
        </template>
        <template v-else>
          <el-button type="danger" plain :loading="cancelingUpload" @click="cancelUploadedImage">取消上传</el-button>
          <el-button @click="leaveUploadDialog">留在个人中心</el-button>
          <el-button @click="openUploadedMetadataEditor">编辑标签和说明</el-button>
          <el-button type="primary" @click="openUploadedImageInWorkbench">进入图像工坊</el-button>
        </template>
      </template>
    </el-dialog>

    <main class="main-area" :class="{ 'editor-mode': isFullscreenEditor, 'workbench-mode': isWorkbench, 'workbench-editing': isWorkbenchEditing }">
      <router-view v-slot="{ Component }">
        <transition
          name="fade"
          mode="out-in"
        >
          <component
            :is="Component"
            @workbench-editing-change="setWorkbenchEditing"
          />
        </transition>
      </router-view>
    </main>

    <nav v-if="!isDockHidden" class="dock-nav" aria-label="底部导航">
      <router-link
        v-for="item in primaryNavItems"
        :key="item.path"
        :to="item.path"
        class="dock-item"
        :class="{ active: isMenuActive(item.path) }"
        :style="{ '--dock-accent': item.accent }"
      >
        <span class="dock-icon">
          <el-icon :size="22">
            <component :is="item.icon" />
          </el-icon>
        </span>
        <span class="dock-label">{{ item.label }}</span>
      </router-link>

      <el-popover
        v-model:visible="showCreateMenu"
        placement="top"
        :width="260"
        trigger="click"
        popper-class="creation-entry-popper"
      >
        <template #reference>
          <button
            class="dock-item"
            :class="{ active: isCreateRoute }"
            type="button"
            style="--dock-accent: #7C5AEF"
          >
            <span class="dock-icon">
              <el-icon :size="22"><EditPen /></el-icon>
            </span>
            <span class="dock-label">{{ $t('nav.create') }}</span>
          </button>
        </template>
        <div class="create-menu">
          <button type="button" @click="openCreation('/draw')">
            <span class="create-menu-icon canvas-icon"><el-icon><EditPen /></el-icon></span>
            <span><strong>{{ $t('nav.draw') }}</strong><small>自由绘制，记录灵感</small></span>
          </button>
          <button type="button" @click="openCreation('/workbench')">
            <span class="create-menu-icon studio-icon"><el-icon><Picture /></el-icon></span>
            <span><strong>{{ $t('nav.workbench') }}</strong><small>编辑图片，轻松出片</small></span>
          </button>
        </div>
      </el-popover>

      <router-link
        to="/personal"
        class="dock-item"
        :class="{ active: isMenuActive('/personal') }"
        style="--dock-accent: #FF8AB3"
      >
        <span class="dock-icon"><el-icon :size="22"><User /></el-icon></span>
        <span class="dock-label">{{ $t('nav.personal') }}</span>
      </router-link>

      <button
        class="dock-item dock-upload"
        type="button"
        :disabled="uploading"
        style="--dock-accent: var(--primary)"
        @click="triggerUpload"
      >
        <span class="dock-icon">
          <el-icon :size="22"><Upload /></el-icon>
        </span>
        <span class="dock-label">{{ $t('action.upload') }}</span>
      </button>
    </nav>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowDown,
  Bell,
  ChatDotRound,
  CollectionTag,
  Clock,
  Compass,
  EditPen,
  HomeFilled,
  InfoFilled,
  Moon,
  Picture,
  Search,
  Setting,
  Star,
  Sunny,
  SwitchButton,
  Upload,
  User,
  UserFilled,
  VideoCamera
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { useThemeStore } from '@/store/theme'
import { useNotificationStore } from '@/store/notification'
import { logout as logoutApi } from '@/api/auth'
import { deleteImage, updateImageMetadata, uploadImage } from '@/api/image'
import { getPublicImages } from '@/api/community'
import BrandLogo from '@/components/common/BrandLogo.vue'
import DynamicBackground from '@/components/common/DynamicBackground.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const themeStore = useThemeStore()
const notificationStore = useNotificationStore()
const { t } = useI18n()
const searchQuery = ref('')
const fileInput = ref(null)
const uploading = ref(false)
const uploadSuccessVisible = ref(false)
const editingUploadedMetadata = ref(false)
const savingUploadedMetadata = ref(false)
const cancelingUpload = ref(false)
const uploadedImage = ref(null)
const uploadedMetadata = ref({ tags: [], description: '' })
const uploadTagOptions = ref([])
const uploadTagOptionsLoading = ref(false)
const uploadTagOptionsLoaded = ref(false)
const sortedUploadTagOptions = computed(() => [...uploadTagOptions.value].sort((a, b) => a.localeCompare(b, 'zh-CN')))
const searchInputRef = ref(null)
const showSearchSuggestions = ref(false)
let searchSuggestionsCloseTimer
const showCreateMenu = ref(false)
const readSearchHistory = () => {
  try {
    const parsed = JSON.parse(localStorage.getItem('pixel_lab_search_history') || '[]')
    return Array.isArray(parsed) ? parsed.slice(0, 5) : []
  } catch {
    localStorage.removeItem('pixel_lab_search_history')
    return []
  }
}
const searchHistory = ref(readSearchHistory())
const hotSearchTags = ['插画', '摄影', 'UI设计', '像素艺术', 'AI艺术']
const notifications = computed(() => notificationStore.notifications.map(item => ({
  ...item,
  time: formatNotificationTime(item.time)
})))
const unreadCount = computed(() => notificationStore.unreadCount)

const notificationColorMap = { like: '#FF6B6B', collect: '#FFB454', comment: '#5B8DEF', reply: '#7C5AEF', follow: '#16C784', system: '#FFB454' }
const notificationIconMap = { like: Star, collect: CollectionTag, comment: ChatDotRound, reply: ChatDotRound, follow: UserFilled, system: InfoFilled }
const getNotificationIcon = (type) => notificationIconMap[type] || InfoFilled
const getNotificationColor = (type) => notificationColorMap[type] || '#FFB454'

const primaryNavItems = computed(() => [
    { path: '/dashboard', label: t('nav.home'), icon: HomeFilled, accent: '#16C784' },
    { path: '/community', label: t('nav.community'), icon: Compass, accent: '#5B8DEF' },
    { path: '/tutorials', label: '教学', icon: VideoCamera, accent: '#FFB454' }
])

const isDarkTheme = computed(() => themeStore.theme === 'dark')
const themeToggleLabel = computed(() => isDarkTheme.value ? '切换到白天模式' : '切换到夜间模式')
const themeModeOptions = [
  { label: '白天', value: 'light', icon: Sunny },
  { label: '夜间', value: 'dark', icon: Moon }
]
const backgroundStyleOptions = [
  { label: '像素网格', value: 'pixel-grid', desc: '发光像素与细网格' },
  { label: '柔和极光', value: 'aurora', desc: '绿色与紫色流光' },
  { label: '星点画布', value: 'star-canvas', desc: '粒子随鼠标牵引' },
  { label: '光谱扫描', value: 'spectrum', desc: '色带与扫描线' }
]
const backgroundMotionOptions = [
  { label: '完整', value: 'on' },
  { label: '轻量', value: 'reduced' },
  { label: '关闭', value: 'off' }
]

const isMenuActive = (path) => route.path === path || route.path.startsWith(`${path}/`)
const isCreateRoute = computed(() => ['/draw', '/workbench'].some(isMenuActive))
const isFullscreenEditor = computed(() => route.path === '/draw')
const isWorkbench = computed(() => route.path === '/workbench')
const isWorkbenchEditing = ref(false)
const setWorkbenchEditing = value => {
  isWorkbenchEditing.value = Boolean(value)
}
const isFixedEditor = computed(() => isFullscreenEditor.value || isWorkbench.value)
const isDockHidden = computed(() => isFullscreenEditor.value || (isWorkbench.value && isWorkbenchEditing.value))
const dynamicBackgroundMode = computed(() => {
  if (isFullscreenEditor.value) return 'hidden'
  if (isWorkbench.value) return 'subtle'
  return 'full'
})

const openSearchSuggestions = () => {
  if (searchSuggestionsCloseTimer) window.clearTimeout(searchSuggestionsCloseTimer)
  searchSuggestionsCloseTimer = undefined
  showSearchSuggestions.value = true
}

const scheduleSearchSuggestionsClose = () => {
  if (searchSuggestionsCloseTimer) window.clearTimeout(searchSuggestionsCloseTimer)
  searchSuggestionsCloseTimer = window.setTimeout(() => {
    showSearchSuggestions.value = false
    searchSuggestionsCloseTimer = undefined
  }, 160)
}

function formatNotificationTime(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const diff = Date.now() - date.getTime()
  const minutes = Math.max(0, Math.floor(diff / 60000))
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}小时前`
  const days = Math.floor(hours / 24)
  if (days < 7) return `${days}天前`
  return `${date.getMonth() + 1}月${date.getDate()}日`
}

const handleSearch = () => {
  const keyword = searchQuery.value.trim()
  if (!keyword) return
  saveSearchHistory(keyword)
  router.push({ path: '/community', query: { keyword } })
}

const quickSearch = (keyword) => {
  searchQuery.value = keyword
  saveSearchHistory(keyword)
  showSearchSuggestions.value = false
  router.push({ path: '/community', query: { keyword } })
}

const saveSearchHistory = (keyword) => {
  const history = searchHistory.value.filter(h => h !== keyword)
  history.unshift(keyword)
  searchHistory.value = history.slice(0, 5)
  localStorage.setItem('pixel_lab_search_history', JSON.stringify(searchHistory.value))
}

const clearSearchHistory = () => {
  searchHistory.value = []
  localStorage.removeItem('pixel_lab_search_history')
}

const markAllRead = async () => {
  await notificationStore.markAllRead().catch(() => {})
}

const markNotificationRead = async (id) => {
  const item = notificationStore.notifications.find(n => n.id === id)
  if (!item?.read) await notificationStore.markRead(id).catch(() => {})
}

const openNotification = async (notification) => {
  if (!notification) return
  await markNotificationRead(notification.id)
  const referenceId = notification.reference_id || notification.referenceId
  const referenceType = notification.reference_type || notification.referenceType
  if (!referenceId) return
  if (referenceType === 'image' || ['like', 'collect', 'comment', 'reply'].includes(notification.type)) {
    router.push(`/post/${referenceId}`)
  } else if (referenceType === 'user' || notification.type === 'follow') {
    router.push(`/user/${referenceId}`)
  }
}

const openCreation = (path) => {
  showCreateMenu.value = false
  router.push(path)
}

const triggerUpload = () => {
  fileInput.value?.click()
}

const resetUploadSuccess = () => {
  editingUploadedMetadata.value = false
  savingUploadedMetadata.value = false
  cancelingUpload.value = false
  uploadedImage.value = null
  uploadedMetadata.value = { tags: [], description: '' }
}

const cancelUploadedImage = async () => {
  if (!uploadedImage.value?.id) return

  try {
    await ElMessageBox.confirm('取消上传后，这张图片将被删除，且无法恢复。', '确认取消上传', {
      confirmButtonText: '删除图片',
      cancelButtonText: '保留图片',
      type: 'warning'
    })
  } catch {
    return
  }

  cancelingUpload.value = true
  try {
    await deleteImage(uploadedImage.value.id)
    uploadSuccessVisible.value = false
    ElMessage.success('已取消上传')
  } catch (error) {
    console.error('取消上传失败:', error)
    ElMessage.error('取消上传失败')
  } finally {
    cancelingUpload.value = false
  }
}
const leaveUploadDialog = () => {
  uploadSuccessVisible.value = false
  router.push('/personal')
}

const openUploadedImageInWorkbench = () => {
  if (uploadedImage.value?.url) {
    localStorage.setItem('pixel_lab_workbench_image', uploadedImage.value.url)
  }
  uploadSuccessVisible.value = false
  router.push('/workbench')
}

const imageTagList = (image = {}) => (
  Array.isArray(image.tags)
    ? image.tags
    : String(image.tags || '').split(',')
).map(tag => tag.trim()).filter(Boolean)

const fetchUploadTagOptions = async () => {
  if (uploadTagOptionsLoaded.value || uploadTagOptionsLoading.value) return

  uploadTagOptionsLoading.value = true
  try {
    const pageSize = 200
    const firstPage = await getPublicImages({ page: 1, pageSize }, { silent: true })
    const works = [...(firstPage.list || [])]
    const totalPages = Math.max(1, Number(firstPage.totalPages || 1))
    for (let page = 2; page <= totalPages; page += 1) {
      const result = await getPublicImages({ page, pageSize }, { silent: true })
      works.push(...(result.list || []))
    }
    uploadTagOptions.value = [...new Set(works.flatMap(imageTagList))]
    uploadTagOptionsLoaded.value = true
  } catch (error) {
    console.error('获取全站标签失败:', error)
    ElMessage.error('获取全站标签失败')
  } finally {
    uploadTagOptionsLoading.value = false
  }
}

const openUploadedMetadataEditor = () => {
  editingUploadedMetadata.value = true
  fetchUploadTagOptions()
}

const saveUploadedMetadata = async () => {
  if (!uploadedImage.value?.id) return

  savingUploadedMetadata.value = true
  try {
    const tags = [...new Set(uploadedMetadata.value.tags.map(tag => tag.trim()).filter(Boolean))]
    await updateImageMetadata(uploadedImage.value.id, {
      title: '',
      description: uploadedMetadata.value.description.trim(),
      tags: tags.join(',')
    })
    uploadedMetadata.value.tags = tags
    uploadTagOptions.value = [...new Set([...uploadTagOptions.value, ...tags])]
    editingUploadedMetadata.value = false
    ElMessage.success('标签和说明已保存')
  } catch (error) {
    console.error('更新图片信息失败:', error)
    ElMessage.error('标签和说明保存失败')
  } finally {
    savingUploadedMetadata.value = false
  }
}

const onFileSelected = async (event) => {
  const file = event.target.files?.[0]
  if (!file) return

  if (!file.type.startsWith('image/')) {
    ElMessage.error('请选择图片文件')
    event.target.value = ''
    return
  }

  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 5MB')
    event.target.value = ''
    return
  }

  uploading.value = true
  try {
    const res = await uploadImage(file)
    ElMessage.success('上传成功')
    uploadedImage.value = {
      id: res?.id || res?.data?.id,
      url: res?.url || res?.data?.url || ''
    }
    uploadedMetadata.value = { tags: [], description: '' }
    uploadSuccessVisible.value = true
  } catch (error) {
    console.error('上传失败:', error)
    ElMessage.error('上传失败')
  } finally {
    uploading.value = false
    event.target.value = ''
  }
}

const handleCommand = (command) => {
  switch (command) {
  case 'profile':
    router.push('/personal')
    break
  case 'settings':
    router.push('/settings')
    break
  case 'admin':
    router.push('/admin')
    break
  case 'logout':
    ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(async () => {
      await logoutApi().catch(() => {})
      userStore.logout()
      notificationStore.reset()
      router.push('/login')
      ElMessage.success('已退出登录')
    }).catch(() => {})
    break
  }
}

onMounted(async () => {
  notificationStore.reset()
  await Promise.all([
    notificationStore.fetchNotifications({ page: 1, pageSize: 20 }, { silent: true }),
    notificationStore.fetchUnreadCount({ silent: true })
  ]).catch(() => {})
})

onBeforeUnmount(() => {
  if (searchSuggestionsCloseTimer) window.clearTimeout(searchSuggestionsCloseTimer)
})
</script>

<style scoped>
.community-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  position: relative;
  isolation: isolate;
  background: var(--background);
  color: var(--foreground);
}

.community-layout.fixed-editor-layout {
  height: 100vh;
  min-height: 0;
  overflow: hidden;
}

.top-bar {
  height: 72px;
  display: flex;
  align-items: center;
  gap: var(--space-6);
  padding: 0 clamp(var(--space-4), 4vw, var(--space-10));
  position: sticky;
  top: 0;
  z-index: 100;
  background: var(--background-float);
  box-shadow: 0 1px 0 var(--border), 0 16px 32px rgba(17, 24, 39, 0.04);
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
}

[data-theme='dark'] .top-bar {
  box-shadow: 0 1px 0 var(--border), 0 16px 32px rgba(0, 0, 0, 0.18);
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  color: inherit;
  text-decoration: none;
}

.brand-name {
  overflow: hidden;
  color: var(--foreground);
  font-size: 18px;
  font-weight: 800;
  letter-spacing: 0;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.brand-tag {
  border-radius: var(--radius-full);
  background: var(--primary-muted);
  color: var(--primary);
  padding: 4px 9px;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
  white-space: nowrap;
}

.search-bar {
  flex: 1;
  max-width: 520px;
  margin: 0 auto;
  position: relative;
}

.search-bar :deep(.el-input__wrapper) {
  min-height: 42px;
  padding: 0 var(--space-4);
  background: var(--background-muted) !important;
}

.top-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--space-3);
  flex-shrink: 0;
}

.icon-action,
.user-avatar {
  width: 42px;
  height: 42px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: var(--radius-full);
  background: var(--background-elevated);
  color: var(--foreground-muted);
  box-shadow: var(--shadow-sm);
  cursor: pointer;
  position: relative;
  transition:
    color var(--transition-fast),
    transform var(--transition-fast),
    box-shadow var(--transition-fast);
}

.icon-action:hover,
.user-avatar:hover {
  color: var(--primary);
  transform: translateY(-1px);
  box-shadow: var(--shadow);
}

.theme-menu {
  display: grid;
  gap: 14px;
  color: var(--foreground);
}

.theme-menu-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.theme-menu-head strong {
  font-size: 15px;
}

.theme-menu-head span,
.theme-section-title,
.background-style-option small {
  color: var(--foreground-muted);
  font-size: 12px;
}

.theme-section-title {
  margin-bottom: -4px;
  font-weight: 700;
}

.theme-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.motion-row {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.theme-chip {
  min-height: 38px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--background-muted);
  color: var(--foreground-muted);
  font-weight: 700;
  cursor: pointer;
  transition:
    border-color var(--transition-fast),
    background var(--transition-fast),
    color var(--transition-fast),
    transform var(--transition-fast);
}

.theme-chip:hover,
.background-style-option:hover {
  transform: translateY(-1px);
  border-color: var(--border-hover);
  color: var(--primary);
}

.theme-chip.active {
  border-color: var(--border-glow);
  background: var(--primary-muted);
  color: var(--primary);
}

.background-style-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 9px;
}

.background-style-option {
  min-width: 0;
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr);
  align-items: center;
  gap: 9px;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: var(--background-card);
  color: var(--foreground);
  padding: 8px;
  text-align: left;
  cursor: pointer;
  transition:
    border-color var(--transition-fast),
    background var(--transition-fast),
    color var(--transition-fast),
    transform var(--transition-fast),
    box-shadow var(--transition-fast);
}

.background-style-option.active {
  border-color: var(--border-glow);
  background: color-mix(in srgb, var(--primary) 9%, var(--background-card));
  box-shadow: inset 0 0 0 1px rgba(22, 199, 132, 0.08);
}

.background-style-option strong,
.background-style-option small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.background-style-option strong {
  font-size: 13px;
  line-height: 1.25;
}

.background-style-option small {
  margin-top: 2px;
  line-height: 1.2;
}

.style-preview {
  width: 44px;
  height: 34px;
  display: block;
  overflow: hidden;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.58);
  box-shadow: inset 0 0 0 1px rgba(20, 26, 23, 0.04);
}

.preview-pixel-grid .style-preview {
  background:
    radial-gradient(circle at 28% 36%, rgba(22, 199, 132, 0.75) 0 3px, transparent 4px),
    linear-gradient(rgba(20, 26, 23, 0.09) 1px, transparent 1px),
    linear-gradient(90deg, rgba(20, 26, 23, 0.09) 1px, transparent 1px),
    #eef9f4;
  background-size: auto, 9px 9px, 9px 9px, auto;
}

.preview-aurora .style-preview {
  background:
    radial-gradient(circle at 22% 22%, rgba(255, 255, 255, 0.74), transparent 28%),
    conic-gradient(from 150deg, rgba(22, 199, 132, 0.84), rgba(124, 90, 239, 0.72), rgba(91, 141, 239, 0.5), rgba(22, 199, 132, 0.82));
}

.preview-star-canvas .style-preview {
  background:
    radial-gradient(circle at 20% 30%, rgba(22, 199, 132, 0.9) 0 2px, transparent 3px),
    radial-gradient(circle at 64% 42%, rgba(124, 90, 239, 0.82) 0 2px, transparent 3px),
    radial-gradient(circle at 78% 72%, rgba(91, 141, 239, 0.72) 0 2px, transparent 3px),
    #edf6ff;
}

.preview-spectrum .style-preview {
  background:
    repeating-linear-gradient(90deg, transparent 0 7px, rgba(22, 199, 132, 0.18) 7px 8px),
    linear-gradient(135deg, rgba(22, 199, 132, 0.64), rgba(124, 90, 239, 0.34), rgba(255, 180, 84, 0.28));
}

:global(.theme-menu-popper.el-popover.el-popper) {
  border: 1px solid var(--border);
  border-radius: 20px;
  background: var(--background-float);
  box-shadow: var(--shadow-md);
  padding: 14px;
  backdrop-filter: blur(22px);
  -webkit-backdrop-filter: blur(22px);
}

:global(.theme-menu-popper .el-popper__arrow::before) {
  border-color: var(--border);
  background: var(--background-float);
}

.notice-badge {
  position: absolute;
  top: 4px;
  right: 4px;
  min-width: 18px;
  height: 18px;
  display: grid;
  place-items: center;
  border-radius: var(--radius-full);
  background: var(--error);
  color: white;
  font-size: 10px;
  font-weight: 700;
  line-height: 1;
  padding: 0 4px;
}

.search-suggestions {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  z-index: 200;
  border-radius: var(--radius-lg);
  background: var(--background-card);
  box-shadow: var(--shadow-md);
  border: 1px solid var(--border);
  overflow: hidden;
}

.suggestion-section {
  padding: var(--space-3);
}

.suggestion-section + .suggestion-section {
  border-top: 1px solid var(--border);
}

.suggestion-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-2);
}

.suggestion-header span {
  font-size: 12px;
  font-weight: 700;
  color: var(--foreground-muted);
}

.suggestion-header button {
  border: 0;
  background: transparent;
  color: var(--foreground-muted);
  font-size: 12px;
  cursor: pointer;
}

.suggestion-header button:hover {
  color: var(--primary);
}

.suggestion-item {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  width: 100%;
  padding: var(--space-2);
  border: 0;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--foreground);
  font-size: 13px;
  text-align: left;
  cursor: pointer;
}

.suggestion-item span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.suggestion-item:hover {
  background: var(--background-muted);
}

.suggestion-item .el-icon {
  color: var(--foreground-muted);
  font-size: 14px;
}

.hot-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.hot-tag-item {
  border: 0;
  border-radius: var(--radius-full);
  background: var(--primary-muted);
  color: var(--primary);
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: background var(--transition-fast);
}

.hot-tag-item:hover {
  background: color-mix(in srgb, var(--primary) 20%, transparent);
}

.notification-panel {
  margin: calc(var(--space-2) * -1);
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-3) var(--space-4);
  border-bottom: 1px solid var(--border);
}

.notification-header strong {
  font-size: 14px;
}

.mark-read-btn {
  border: 0;
  background: transparent;
  color: var(--primary);
  font-size: 12px;
  cursor: pointer;
}

.notification-list {
  max-height: 360px;
  overflow-y: auto;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  transition: background var(--transition-fast);
  cursor: pointer;
}

.notification-item:hover {
  background: var(--background-muted);
}

.notification-item.unread {
  background: var(--primary-muted);
}

.notification-icon {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  display: grid;
  place-items: center;
  border-radius: var(--radius-full);
  background: var(--background-muted);
  font-size: 16px;
}

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-content p {
  margin: 0;
  font-size: 13px;
  line-height: 1.5;
  color: var(--foreground);
}

.notification-time {
  font-size: 11px;
  color: var(--foreground-muted);
}

.notification-empty {
  padding: var(--space-8);
  text-align: center;
  color: var(--foreground-muted);
  font-size: 13px;
}

.user-avatar {
  width: auto;
  min-width: 54px;
  gap: 4px;
  padding: 0 8px 0 3px;
}

.avatar-caret {
  color: var(--foreground-muted);
  font-size: 12px;
}

.main-area {
  flex: 1;
  width: 100%;
  padding: clamp(var(--space-5), 4vw, var(--space-10));
  padding-bottom: 112px;
  position: relative;
  z-index: 1;
}
.main-area.workbench-mode {
  flex: 0 0 auto;
  height: calc(100vh - 72px);
  min-height: 0;
  padding: 12px clamp(12px, 3vw, 36px);
  overflow: hidden;
}

.main-area.workbench-mode:not(.workbench-editing) {
  padding-bottom: 104px;
}

.main-area.editor-mode {
  height: 100vh;
  min-height: 0;
  padding: 0;
  overflow: hidden;
}

.dock-nav {
  position: fixed;
  bottom: var(--space-6);
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-2);
  z-index: 100;
  border: 1px solid rgba(255, 255, 255, 0.62);
  border-radius: var(--radius-xl);
  background: rgba(255, 255, 255, 0.72);
  box-shadow: var(--shadow-md);
  backdrop-filter: blur(22px);
  -webkit-backdrop-filter: blur(22px);
  max-width: calc(100vw - var(--space-8));
}

[data-theme='dark'] .dock-nav {
  border-color: rgba(255, 255, 255, 0.08);
  background: rgba(24, 32, 28, 0.72);
}

.dock-item {
  --dock-accent: var(--primary);
  min-width: 64px;
  min-height: 58px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 4px;
  position: relative;
  border: 0;
  border-radius: var(--radius-lg);
  background: transparent;
  color: var(--foreground-muted);
  text-decoration: none;
  cursor: pointer;
  overflow: hidden;
  transition:
    color var(--transition-fast),
    transform var(--transition-fast),
    background var(--transition-fast),
    box-shadow var(--transition-fast);
}

.dock-item:hover {
  color: var(--dock-accent);
  transform: translateY(-3px);
  background: color-mix(in srgb, var(--dock-accent) 12%, transparent);
}

.dock-item:active {
  transform: translateY(0) scale(0.96);
}

.dock-item.active {
  color: var(--dock-accent);
  background: color-mix(in srgb, var(--dock-accent) 14%, white);
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--dock-accent) 18%, transparent);
}

[data-theme='dark'] .dock-item.active {
  background: color-mix(in srgb, var(--dock-accent) 16%, transparent);
}

.dock-item:focus-visible {
  outline: 3px solid color-mix(in srgb, var(--dock-accent) 24%, transparent);
  outline-offset: 2px;
}

.dock-upload {
  min-width: 70px;
  color: white;
  background: var(--primary);
  box-shadow: var(--glow-sm);
}

.dock-upload:hover {
  color: white;
  background: var(--primary-hover);
}

.dock-upload:disabled {
  cursor: wait;
  opacity: 0.72;
}

.dock-label {
  max-width: 72px;
  overflow: hidden;
  font-size: 11px;
  font-weight: 600;
  line-height: 1;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.create-menu {
  display: grid;
  gap: 8px;
}

.create-menu button {
  width: 100%;
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  align-items: center;
  gap: 12px;
  border: 0;
  border-radius: 14px;
  background: transparent;
  color: var(--foreground);
  padding: 10px;
  text-align: left;
  cursor: pointer;
  transition: background var(--transition-fast), transform var(--transition-fast);
}

.create-menu button:hover {
  background: var(--background-muted);
  transform: translateY(-1px);
}

.create-menu-icon {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border-radius: 13px;
  font-size: 20px;
}

.canvas-icon {
  background: var(--primary-muted);
  color: var(--primary);
}

.studio-icon {
  background: rgba(124, 90, 239, 0.12);
  color: #7C5AEF;
}

.create-menu strong,
.create-menu small {
  display: block;
}

.create-menu strong {
  font-size: 14px;
}

.create-menu small {
  margin-top: 3px;
  overflow: hidden;
  color: var(--foreground-muted);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:global(.creation-entry-popper.el-popover.el-popper) {
  border: 1px solid var(--border);
  border-radius: 18px;
  background: var(--background-card);
  box-shadow: var(--shadow-md);
  padding: 10px;
}

:global(.creation-entry-popper .el-popper__arrow::before) {
  border-color: var(--border);
  background: var(--background-card);
}

.upload-success-message {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 4px 0;
}

.upload-success-icon {
  width: 28px;
  height: 28px;
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 50%;
  background: var(--success);
  color: #fff;
  font-size: 18px;
  font-weight: 700;
}

.upload-success-message strong {
  color: var(--foreground);
  font-size: 15px;
}

.upload-success-message p {
  margin: 5px 0 0;
  color: var(--foreground-muted);
  line-height: 1.6;
}

.upload-metadata-form {
  padding-top: 2px;
}

:deep(.upload-success-dialog .el-dialog__footer) {
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
}

:deep(.upload-success-dialog .el-dialog__footer .el-button) {
  margin-left: 0;
}
.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 900px) {
  .top-bar {
    gap: var(--space-4);
  }

  .brand-name {
    display: none;
  }

  .brand-tag {
    display: none;
  }

}

@media (max-width: 720px) {
  .top-bar {
    padding: 0 var(--space-4);
  }

  .search-bar {
    display: none;
  }

  .top-actions {
    gap: var(--space-2);
  }

  .icon-action {
    width: 38px;
    height: 38px;
  }

  .dock-nav {
    left: var(--space-4);
    right: var(--space-4);
    bottom: var(--space-4);
    transform: none;
    justify-content: space-around;
    max-width: none;
  }

  .dock-item {
    min-width: 0;
    flex: 1;
  }
}

@media (max-width: 420px) {
  .dock-label {
    display: none;
  }

  .dock-item {
    min-height: 50px;
  }
}
</style>
