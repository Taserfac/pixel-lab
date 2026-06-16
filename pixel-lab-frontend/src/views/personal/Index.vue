<template>
  <div class="personal-page">
    <section class="profile-hero">
      <div class="hero-main">
        <div class="avatar-ring">
          <el-avatar :size="104" :src="userInfo.avatar || defaultAvatar">
            {{ avatarText }}
          </el-avatar>
        </div>

        <div class="profile-copy">
          <div class="profile-kicker">
            <span>Creator Studio</span>
            <span class="role-pill">{{ userRoleLabel }}</span>
          </div>
          <h1>{{ userInfo.nickname || userInfo.username || 'Pixel Creator' }}</h1>
          <p class="username">@{{ userInfo.username || 'creator' }}</p>
          <p class="profile-desc">
            记录灵感、管理作品、查看互动反馈，把每一次像素创作整理成自己的视觉主页。
          </p>

          <div class="hero-actions">
            <el-button type="primary" @click="handleUpload">
              <el-icon><Upload /></el-icon>
              上传图片
            </el-button>
            <el-button @click="router.push('/workbench')">
              <el-icon><EditPen /></el-icon>
              去工作台
            </el-button>
            <el-button @click="router.push('/settings')">
              <el-icon><Setting /></el-icon>
              编辑资料
            </el-button>
          </div>
        </div>
      </div>

      <div class="hero-side">
        <div class="stat-grid">
          <div v-for="item in heroStats" :key="item.label" class="hero-stat">
            <span class="stat-value">{{ formatNumber(item.value) }}</span>
            <span class="stat-label">{{ item.label }}</span>
          </div>
        </div>
      </div>
    </section>

    <section class="spotlight-strip">
      <div class="section-title">
        <div>
          <span class="eyebrow">精选预览</span>
          <h2>最近作品</h2>
        </div>
        <el-button text @click="router.push('/draw')">
          <el-icon><EditPen /></el-icon>
          手绘创作
        </el-button>
      </div>

      <div v-if="featuredImages.length" class="featured-row">
        <button
          v-for="image in featuredImages"
          :key="image.id"
          class="featured-card"
          type="button"
          @click="viewImage(image)"
        >
          <img :src="image.url" :alt="imageTitle(image)" loading="lazy">
          <span>{{ imageTitle(image) }}</span>
        </button>
      </div>
      <div v-else class="soft-empty">
        暂无作品。上传第一张图片后，这里会展示你的最近创作。
      </div>
    </section>

    <input
      ref="fileInput"
      type="file"
      accept="image/*"
      class="visually-hidden"
      @change="onFileSelected"
    >

    <el-tabs v-model="activeTab" class="creator-tabs" @tab-change="handleTabChange">
      <el-tab-pane name="works">
        <template #label>
          <span class="tab-label"><el-icon><Picture /></el-icon>作品</span>
        </template>

        <section class="toolbar-panel">
          <div>
            <h2>我的作品流</h2>
            <p>{{ filteredImages.length }} 个作品正在展示</p>
          </div>
          <el-radio-group v-model="filterType" size="small">
            <el-radio-button label="all">全部</el-radio-button>
            <el-radio-button label="public">公开</el-radio-button>
            <el-radio-button label="private">私有</el-radio-button>
          </el-radio-group>
        </section>

        <div v-loading="loadingImages" class="feed-shell">
          <div v-if="filteredImages.length" class="masonry-grid">
            <article v-for="image in filteredImages" :key="image.id" class="work-card">
              <button class="work-cover" type="button" @click="viewImage(image)">
                <img :src="image.url" :alt="imageTitle(image)" loading="lazy">
                <span class="visibility-badge" :class="{ public: isPublic(image) }">
                  {{ isPublic(image) ? '公开' : '私有' }}
                </span>
                <span class="cover-overlay">
                  <el-icon><View /></el-icon>
                  预览
                </span>
              </button>

              <div class="work-meta">
                <div>
                  <h3 :title="imageTitle(image)">{{ imageTitle(image) }}</h3>
                  <p>{{ formatDate(image.created_at) }}</p>
                </div>
                <el-dropdown trigger="click" placement="bottom-end" @command="(command) => handleImageCommand(image, command)">
                  <button class="icon-button" type="button" aria-label="作品操作">
                    <el-icon><MoreFilled /></el-icon>
                  </button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="view">
                        <el-icon><View /></el-icon>
                        查看图片
                      </el-dropdown-item>
                      <el-dropdown-item command="open">
                        <el-icon><EditPen /></el-icon>
                        在工作台打开
                      </el-dropdown-item>
                      <el-dropdown-item command="visibility">
                        <el-icon>
                          <Lock v-if="isPublic(image)" />
                          <Unlock v-else />
                        </el-icon>
                        {{ isPublic(image) ? '设为私有' : '设为公开' }}
                      </el-dropdown-item>
                      <el-dropdown-item command="delete" divided>
                        <el-icon><Delete /></el-icon>
                        删除图片
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>

              <div class="metric-row">
                <span><el-icon><View /></el-icon>{{ formatNumber(image.view_count) }}</span>
                <span><el-icon><Star /></el-icon>{{ formatNumber(image.like_count) }}</span>
                <span><el-icon><FolderOpened /></el-icon>{{ formatNumber(image.collect_count) }}</span>
              </div>
            </article>
          </div>
          <div v-else class="empty-panel">
            <el-icon><Picture /></el-icon>
            <h3>还没有作品</h3>
            <p>上传图片后，作品会自动沉淀到你的个人主页。</p>
            <el-button type="primary" @click="handleUpload">上传图片</el-button>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane name="collections">
        <template #label>
          <span class="tab-label"><el-icon><FolderOpened /></el-icon>收藏</span>
        </template>
        <content-feed
          :items="collections"
          :loading="loadingCollections"
          empty-title="暂无收藏"
          empty-desc="去社区发现值得反复看的作品。"
          action-text="逛逛社区"
          @item-click="goToCommunity"
          @empty-action="goToCommunity"
        />
      </el-tab-pane>

      <el-tab-pane name="likes">
        <template #label>
          <span class="tab-label"><el-icon><Star /></el-icon>点赞</span>
        </template>
        <content-feed
          :items="likes"
          :loading="loadingLikes"
          empty-title="暂无点赞"
          empty-desc="遇到喜欢的作品，点亮它们后会出现在这里。"
          action-text="发现作品"
          @item-click="goToCommunity"
          @empty-action="goToCommunity"
        />
      </el-tab-pane>

      <el-tab-pane name="insights">
        <template #label>
          <span class="tab-label"><el-icon><TrendCharts /></el-icon>数据</span>
        </template>

        <section class="insights-grid">
          <div class="chart-card">
            <div class="section-title compact">
              <div>
                <span class="eyebrow">30 天趋势</span>
                <h2>互动走势</h2>
              </div>
            </div>
            <div ref="trendChartRef" class="trend-chart" />
          </div>

          <div class="hot-card">
            <div class="section-title compact">
              <div>
                <span class="eyebrow">Top Works</span>
                <h2>热门作品</h2>
              </div>
            </div>
            <div v-if="hotWorks.length" class="hot-list">
              <button v-for="work in hotWorks" :key="work.id" class="hot-item" type="button" @click="viewImage(work)">
                <img :src="work.url" :alt="imageTitle(work)" loading="lazy">
                <span class="hot-rank">#{{ hotWorks.indexOf(work) + 1 }}</span>
                <div>
                  <strong :title="imageTitle(work)">{{ imageTitle(work) }}</strong>
                  <small>{{ formatNumber(work.view_count) }} 浏览 · {{ formatNumber(work.like_count) }} 赞</small>
                </div>
              </button>
            </div>
            <div v-else class="soft-empty small">暂无热门作品数据。</div>
          </div>
        </section>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="previewVisible" title="图片预览" width="78%" center class="preview-dialog">
      <img v-if="previewImage" :src="previewImage.url" :alt="imageTitle(previewImage)" class="preview-image">
      <template #footer>
        <el-button @click="previewVisible = false">关闭</el-button>
        <el-button type="primary" @click="openInWorkbench(previewImage)">在工作台打开</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, defineComponent, h, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElButton, ElIcon, ElMessage, ElMessageBox } from 'element-plus'
import {
  Delete,
  EditPen,
  FolderOpened,
  Lock,
  MoreFilled,
  Picture,
  Setting,
  Star,
  TrendCharts,
  Unlock,
  Upload,
  View
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { useUserStore } from '@/store/user'
import { getUserStats } from '@/api/auth'
import { uploadImage, getUserImages, deleteImage, updateImageVisibility } from '@/api/image'
import { getUserCollections, getUserLikes } from '@/api/community'

const ContentFeed = defineComponent({
  name: 'ContentFeed',
  props: {
    items: { type: Array, default: () => [] },
    loading: { type: Boolean, default: false },
    emptyTitle: { type: String, default: '暂无内容' },
    emptyDesc: { type: String, default: '' },
    actionText: { type: String, default: '去看看' }
  },
  emits: ['item-click', 'empty-action'],
  setup(props, { emit }) {
    const title = (item) => item.title || item.original_name || item.filename || '未命名作品'
    const format = (value) => Number(value || 0).toLocaleString('zh-CN')
    return () => h('div', { class: 'feed-shell', directives: [] }, [
      props.items.length
        ? h('div', { class: 'masonry-grid' }, props.items.map(item => h('article', {
            class: 'work-card',
            key: item.id
          }, [
            h('button', {
              class: 'work-cover',
              type: 'button',
              onClick: () => emit('item-click', item.id)
            }, [
              h('img', { src: item.url, alt: title(item), loading: 'lazy' }),
              h('span', { class: 'cover-overlay' }, [
                h(ElIcon, null, () => h(View)),
                '查看详情'
              ])
            ]),
            h('div', { class: 'work-meta' }, [
              h('div', null, [
                h('h3', { title: title(item) }, title(item)),
                h('p', null, item.author_name ? `by ${item.author_name}` : '来自社区')
              ])
            ]),
            h('div', { class: 'metric-row' }, [
              h('span', null, [h(ElIcon, null, () => h(View)), format(item.view_count)]),
              h('span', null, [h(ElIcon, null, () => h(Star)), format(item.like_count)]),
              h('span', null, [h(ElIcon, null, () => h(FolderOpened)), format(item.collect_count)])
            ])
          ])))
        : h('div', { class: 'empty-panel' }, [
            h(ElIcon, null, () => h(Picture)),
            h('h3', null, props.emptyTitle),
            h('p', null, props.emptyDesc),
            h(ElButton, { type: 'primary', onClick: () => emit('empty-action') }, () => props.actionText)
          ])
    ])
  }
})

const router = useRouter()
const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo || {})

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
const fileInput = ref(null)
const activeTab = ref('works')
const filterType = ref('all')

const images = ref([])
const collections = ref([])
const likes = ref([])
const hotWorks = ref([])
const trendData = ref([])
const insightStats = ref({})

const loadingImages = ref(false)
const loadingCollections = ref(false)
const loadingLikes = ref(false)
const uploading = ref(false)

const previewVisible = ref(false)
const previewImage = ref(null)
const trendChartRef = ref(null)
let trendChart = null

const avatarText = computed(() => {
  const source = userInfo.value.nickname || userInfo.value.username || 'U'
  return source.charAt(0).toUpperCase()
})

const userRoleLabel = computed(() => userInfo.value.role === 'admin' ? '管理员' : '创作者')

const toNumber = (value) => Number(value || 0)

const formatNumber = (value) => {
  const num = toNumber(value)
  if (num >= 10000) return `${(num / 10000).toFixed(1)}w`
  if (num >= 1000) return `${(num / 1000).toFixed(1)}k`
  return String(num)
}

const imageTitle = (image = {}) => image.title || image.original_name || image.filename || '未命名作品'

const isPublic = (image = {}) => image.is_public === true || image.is_public === 1 || image.is_public === '1'

const normalizeStats = (stats = {}) => ({
  works: toNumber(stats.works ?? stats.imageCount),
  publicWorks: toNumber(stats.publicImageCount),
  receivedLikes: toNumber(stats.likes ?? stats.receivedLikeCount),
  receivedCollects: toNumber(stats.collects ?? stats.receivedCollectCount),
  views: toNumber(stats.views ?? stats.viewCount),
  likedWorks: toNumber(stats.likeCount),
  collectedWorks: toNumber(stats.collectionCount)
})

const normalizeTrend = (trend = []) => trend.map(item => ({
  label: item.label || item.date || '',
  views: toNumber(item.views),
  likes: toNumber(item.likes),
  collects: toNumber(item.collects),
  works: toNumber(item.works)
}))

const heroStats = computed(() => [
  { label: '作品', value: insightStats.value.works ?? images.value.length },
  { label: '公开', value: insightStats.value.publicWorks ?? images.value.filter(isPublic).length },
  { label: '获赞', value: insightStats.value.receivedLikes },
  { label: '收藏', value: insightStats.value.receivedCollects },
  { label: '浏览', value: insightStats.value.views }
])

const featuredImages = computed(() => images.value.slice(0, 6))

const filteredImages = computed(() => {
  if (filterType.value === 'public') return images.value.filter(isPublic)
  if (filterType.value === 'private') return images.value.filter(image => !isPublic(image))
  return images.value
})

const formatDate = (dateStr) => {
  if (!dateStr) return '未知时间'
  return new Date(dateStr).toLocaleDateString('zh-CN', {
    month: 'short',
    day: 'numeric'
  })
}

const fetchImages = async () => {
  loadingImages.value = true
  try {
    const res = await getUserImages({ pageSize: 80 })
    images.value = res.list || []
  } catch (error) {
    console.error('获取图片列表失败:', error)
  } finally {
    loadingImages.value = false
  }
}

const fetchStats = async () => {
  try {
    const res = await getUserStats({ days: 30 })
    insightStats.value = normalizeStats(res)
    trendData.value = normalizeTrend(res.trend || [])
    hotWorks.value = res.hotWorks || []
    await nextTick()
    renderTrendChart()
  } catch (error) {
    console.error('获取统计数据失败:', error)
    insightStats.value = normalizeStats({})
    trendData.value = []
    hotWorks.value = []
  }
}

const fetchCollections = async () => {
  loadingCollections.value = true
  try {
    const res = await getUserCollections({ pageSize: 50 })
    collections.value = res.list || []
  } catch (error) {
    console.error('获取收藏列表失败:', error)
  } finally {
    loadingCollections.value = false
  }
}

const fetchLikes = async () => {
  loadingLikes.value = true
  try {
    const res = await getUserLikes({ pageSize: 50 })
    likes.value = res.list || []
  } catch (error) {
    console.error('获取点赞列表失败:', error)
  } finally {
    loadingLikes.value = false
  }
}

const fetchInitialData = async () => {
  await Promise.all([
    fetchImages(),
    fetchStats(),
    fetchCollections(),
    fetchLikes()
  ])
}

const handleTabChange = async (tab) => {
  if (tab === 'insights') {
    await nextTick()
    renderTrendChart()
  }
}

const handleUpload = () => {
  fileInput.value?.click()
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
    await uploadImage(file)
    ElMessage.success('上传成功')
    await Promise.all([fetchImages(), fetchStats()])
  } catch (error) {
    console.error('上传失败:', error)
    ElMessage.error('上传失败')
  } finally {
    uploading.value = false
    event.target.value = ''
  }
}

const viewImage = (image) => {
  previewImage.value = image
  previewVisible.value = true
}

const openInWorkbench = (image) => {
  if (image?.url) {
    localStorage.setItem('last-open-image', image.url)
  }
  previewVisible.value = false
  router.push('/workbench')
}

const handleImageCommand = (image, command) => {
  if (command === 'view') viewImage(image)
  if (command === 'open') openInWorkbench(image)
  if (command === 'visibility') toggleVisibility(image)
  if (command === 'delete') confirmDelete(image)
}

const toggleVisibility = async (image) => {
  try {
    const nextPublic = !isPublic(image)
    await updateImageVisibility(image.id, nextPublic)
    image.is_public = nextPublic ? 1 : 0
    await fetchStats()
    ElMessage.success(nextPublic ? '已设为公开' : '已设为私有')
  } catch (error) {
    console.error('更新可见性失败:', error)
    ElMessage.error('操作失败')
  }
}

const confirmDelete = (image) => {
  ElMessageBox.confirm(
    `确定删除「${imageTitle(image)}」吗？此操作不可恢复。`,
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
      await Promise.all([fetchImages(), fetchStats()])
    } catch (error) {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

const goToCommunity = (imageId) => {
  if (imageId) {
    router.push({ path: '/community', query: { id: imageId } })
  } else {
    router.push('/community')
  }
}

const renderTrendChart = () => {
  if (!trendChartRef.value) return

  if (trendChart) {
    trendChart.dispose()
  }

  trendChart = echarts.init(trendChartRef.value)
  const labels = trendData.value.map(item => item.label)

  trendChart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(10, 10, 10, 0.92)',
      borderColor: 'rgba(255, 255, 255, 0.12)',
      textStyle: { color: '#fff' }
    },
    legend: {
      top: 0,
      data: ['浏览', '获赞', '收藏'],
      textStyle: { color: '#a0a0a0' }
    },
    grid: {
      top: 44,
      left: 12,
      right: 16,
      bottom: 8,
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: labels,
      axisLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.12)' } },
      axisLabel: { color: '#a0a0a0' }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.08)' } },
      axisLabel: { color: '#a0a0a0' }
    },
    series: [
      {
        name: '浏览',
        type: 'line',
        smooth: true,
        data: trendData.value.map(item => item.views),
        lineStyle: { color: '#00d4ff', width: 3 },
        itemStyle: { color: '#00d4ff' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(0, 212, 255, 0.25)' },
            { offset: 1, color: 'rgba(0, 212, 255, 0.02)' }
          ])
        }
      },
      {
        name: '获赞',
        type: 'line',
        smooth: true,
        data: trendData.value.map(item => item.likes),
        lineStyle: { color: '#ff4f8b', width: 2 },
        itemStyle: { color: '#ff4f8b' }
      },
      {
        name: '收藏',
        type: 'line',
        smooth: true,
        data: trendData.value.map(item => item.collects),
        lineStyle: { color: '#00ff88', width: 2 },
        itemStyle: { color: '#00ff88' }
      }
    ]
  })
}

const handleResize = () => {
  trendChart?.resize()
}

watch(activeTab, async (tab) => {
  if (tab === 'insights') {
    await nextTick()
    renderTrendChart()
  }
})

onMounted(() => {
  fetchInitialData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  trendChart?.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.personal-page {
  width: min(1440px, 100%);
  margin: 0 auto;
  padding: var(--space-6);
}

.profile-hero,
.spotlight-strip,
.toolbar-panel,
.chart-card,
.hot-card {
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.06), rgba(255, 255, 255, 0.01)),
    var(--background-card);
  box-shadow: var(--shadow);
}

.profile-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 420px;
  gap: var(--space-6);
  padding: var(--space-8);
  overflow: hidden;
}

.hero-main {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: var(--space-6);
}

.avatar-ring {
  flex: 0 0 auto;
  padding: 5px;
  border-radius: var(--radius-full);
  background: conic-gradient(from 180deg, #ff4f8b, #ffb800, #00ff88, #00d4ff, #ff4f8b);
}

.avatar-ring :deep(.el-avatar) {
  border: 5px solid var(--background-card);
  background: var(--background-muted);
  color: var(--foreground);
  font-size: 36px;
  font-weight: 800;
}

.profile-copy {
  min-width: 0;
}

.profile-kicker,
.eyebrow {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  color: var(--primary);
  font-family: var(--font-mono);
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
}

.role-pill,
.visibility-badge {
  border: 1px solid var(--border-light);
  border-radius: var(--radius-full);
  background: var(--background-muted);
  color: var(--foreground-muted);
  padding: 4px 10px;
  font-size: 12px;
  line-height: 1;
}

.profile-copy h1 {
  margin: var(--space-2) 0 0;
  color: var(--foreground);
  font-size: clamp(32px, 5vw, 56px);
  line-height: 1;
  letter-spacing: 0;
}

.username {
  margin-top: var(--space-2);
  color: var(--foreground-muted);
  font-size: 16px;
}

.profile-desc {
  max-width: 660px;
  margin: var(--space-4) 0 0;
  color: var(--foreground-muted);
  font-size: 15px;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  margin-top: var(--space-6);
}

.hero-side {
  display: flex;
  align-items: stretch;
}

.stat-grid {
  width: 100%;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-3);
}

.hero-stat {
  min-height: 96px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.035);
  padding: var(--space-4);
}

.hero-stat:last-child {
  grid-column: span 2;
}

.stat-value {
  color: var(--foreground);
  font-family: var(--font-mono);
  font-size: 28px;
  font-weight: 800;
  line-height: 1;
}

.stat-label {
  margin-top: var(--space-2);
  color: var(--foreground-muted);
  font-size: 12px;
}

.spotlight-strip {
  margin-top: var(--space-5);
  padding: var(--space-6);
}

.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  margin-bottom: var(--space-5);
}

.section-title.compact {
  margin-bottom: var(--space-4);
}

.section-title h2,
.toolbar-panel h2 {
  margin: 0;
  color: var(--foreground);
  font-size: 22px;
  line-height: 1.2;
}

.featured-row {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: var(--space-3);
}

.featured-card {
  position: relative;
  aspect-ratio: 4 / 5;
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--background-muted);
  cursor: pointer;
}

.featured-card img,
.work-cover img,
.hot-item img,
.preview-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.featured-card span {
  position: absolute;
  inset: auto var(--space-2) var(--space-2);
  overflow: hidden;
  border-radius: var(--radius);
  background: rgba(0, 0, 0, 0.62);
  color: #fff;
  padding: 6px 8px;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.creator-tabs {
  margin-top: var(--space-5);
}

.creator-tabs :deep(.el-tabs__header) {
  margin-bottom: var(--space-5);
}

.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.toolbar-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  margin-bottom: var(--space-5);
  padding: var(--space-5) var(--space-6);
}

.toolbar-panel p {
  margin-top: 4px;
  color: var(--foreground-muted);
}

.feed-shell {
  min-height: 320px;
}

.masonry-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: var(--space-4);
}

.work-card {
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  background: var(--background-card);
  box-shadow: var(--shadow-sm);
  transition:
    border-color var(--transition-fast),
    transform var(--transition-fast),
    box-shadow var(--transition-fast);
}

.work-card:hover {
  border-color: var(--border-hover);
  box-shadow: var(--glow-sm);
  transform: translateY(-3px);
}

.work-cover {
  position: relative;
  display: block;
  width: 100%;
  aspect-ratio: 1;
  overflow: hidden;
  border: 0;
  background: var(--background-muted);
  cursor: pointer;
}

.work-cover img {
  transition: transform var(--transition-base);
}

.work-card:hover .work-cover img {
  transform: scale(1.05);
}

.visibility-badge {
  position: absolute;
  top: var(--space-2);
  left: var(--space-2);
  background: rgba(0, 0, 0, 0.68);
}

.visibility-badge.public {
  border-color: rgba(0, 255, 136, 0.45);
  color: var(--primary);
}

.cover-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  background: rgba(0, 0, 0, 0.48);
  color: #fff;
  font-weight: 700;
  opacity: 0;
  transition: opacity var(--transition-fast);
}

.work-cover:hover .cover-overlay {
  opacity: 1;
}

.work-meta {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-3);
  padding: var(--space-4);
}

.work-meta h3 {
  max-width: 170px;
  overflow: hidden;
  color: var(--foreground);
  font-size: 14px;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.work-meta p {
  margin-top: 4px;
  color: var(--foreground-muted);
  font-size: 12px;
}

.icon-button {
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--border);
  border-radius: var(--radius-full);
  background: var(--background-muted);
  color: var(--foreground);
  cursor: pointer;
}

.metric-row {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  border-top: 1px solid var(--border);
  padding: 0 var(--space-4) var(--space-4);
  color: var(--foreground-muted);
  font-size: 12px;
}

.metric-row span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.empty-panel,
.soft-empty {
  min-height: 260px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 1px dashed var(--border-light);
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.025);
  color: var(--foreground-muted);
  padding: var(--space-8);
  text-align: center;
}

.empty-panel .el-icon {
  margin-bottom: var(--space-4);
  color: var(--foreground-subtle);
  font-size: 52px;
}

.empty-panel h3 {
  color: var(--foreground);
  font-size: 20px;
}

.empty-panel p {
  margin: var(--space-2) 0 var(--space-5);
}

.soft-empty.small {
  min-height: 180px;
}

.insights-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(320px, 0.8fr);
  gap: var(--space-5);
}

.chart-card,
.hot-card {
  padding: var(--space-6);
}

.trend-chart {
  width: 100%;
  height: 360px;
}

.hot-list {
  display: grid;
  gap: var(--space-3);
}

.hot-item {
  position: relative;
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  align-items: center;
  gap: var(--space-3);
  min-height: 84px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--background-muted);
  color: var(--foreground);
  padding: var(--space-2);
  cursor: pointer;
  text-align: left;
}

.hot-item img {
  width: 72px;
  height: 72px;
  border-radius: var(--radius);
}

.hot-rank {
  position: absolute;
  top: 6px;
  left: 6px;
  border-radius: var(--radius-full);
  background: var(--primary);
  color: var(--background);
  padding: 2px 7px;
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 800;
}

.hot-item strong,
.hot-item small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hot-item small {
  margin-top: 4px;
  color: var(--foreground-muted);
}

.preview-dialog :deep(.el-dialog__body) {
  height: min(68vh, 720px);
}

.preview-image {
  max-height: 68vh;
  object-fit: contain;
}

.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
}

@media (max-width: 1180px) {
  .profile-hero,
  .insights-grid {
    grid-template-columns: 1fr;
  }

  .featured-row {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .personal-page {
    padding: var(--space-4);
  }

  .profile-hero {
    padding: var(--space-5);
  }

  .hero-main {
    flex-direction: column;
    align-items: flex-start;
  }

  .stat-grid,
  .featured-row,
  .masonry-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .hero-stat:last-child {
    grid-column: span 2;
  }

  .toolbar-panel,
  .section-title {
    align-items: flex-start;
    flex-direction: column;
  }

  .trend-chart {
    height: 300px;
  }
}

@media (max-width: 520px) {
  .stat-grid,
  .featured-row,
  .masonry-grid {
    grid-template-columns: 1fr;
  }

  .hero-stat:last-child {
    grid-column: auto;
  }
}
</style>
