<template>
  <div class="dashboard-page page-shell">
    <header class="dashboard-header">
      <div>
        <h1 class="page-heading">Dashboard</h1>
        <p class="page-description">查看社区增长与内容状态，管理用户和公开作品。</p>
      </div>
      <span class="online-status"><i />{{ stats.sessions.online || 0 }} 人在线</span>
    </header>

    <section v-if="activeSection === 'overview'">
      <div class="metric-grid">
        <UiCard v-for="item in metrics" :key="item.label" padding="lg" class="metric-card">
          <span class="metric-icon" :class="item.tone"><el-icon><component :is="item.icon" /></el-icon></span>
          <div><strong>{{ formatNumber(item.value) }}</strong><span>{{ item.label }}</span><small v-if="item.today">今日 +{{ item.today }}</small></div>
        </UiCard>
      </div>

      <div class="chart-grid">
        <UiCard padding="lg" class="chart-card wide">
          <header><div><h2>用户增长</h2><p>近 30 天新增注册用户</p></div><span>Users</span></header>
          <div ref="userChartRef" class="chart" />
        </UiCard>
        <UiCard padding="lg" class="chart-card wide">
          <header><div><h2>作品数量</h2><p>近 30 天新增有效作品</p></div><span>Works</span></header>
          <div ref="imageChartRef" class="chart" />
        </UiCard>
        <UiCard padding="lg" class="chart-card tags-chart-card">
          <header><div><h2>标签分布</h2><p>公开作品常用标签</p></div><span>Tags</span></header>
          <div v-if="stats.tagDistribution.length" ref="tagChartRef" class="chart" />
          <EmptyState v-else title="暂无标签数据" description="作品添加标签后会在这里形成分布。" />
        </UiCard>
        <UiCard padding="lg" class="interaction-card">
          <header><div><h2>社区互动</h2><p>平台累计互动</p></div></header>
          <div class="interaction-list">
            <div v-for="item in interactionMetrics" :key="item.label"><span><el-icon><component :is="item.icon" /></el-icon>{{ item.label }}</span><strong>{{ formatNumber(item.value) }}</strong></div>
          </div>
        </UiCard>
      </div>
    </section>

    <section v-else-if="activeSection === 'users'" class="management-section">
      <div class="management-toolbar">
        <div><h2>用户管理</h2><p>查看账号状态并执行启用或封禁操作。</p></div>
        <form class="management-search" @submit.prevent="loadUsers(1)">
          <UiInput v-model="userSearch" clearable placeholder="搜索用户名或昵称" />
          <UiButton type="submit">搜索</UiButton>
        </form>
      </div>
      <UiCard padding="none" class="table-card">
        <el-table v-loading="loadingUsers" :data="users">
          <el-table-column label="用户" min-width="220">
            <template #default="{ row }"><div class="user-cell"><el-avatar :size="34" :src="row.avatar">{{ (row.nickname || row.username || '?').charAt(0) }}</el-avatar><span><strong>{{ row.nickname || row.username }}</strong><small>@{{ row.username }}</small></span></div></template>
          </el-table-column>
          <el-table-column prop="role" label="角色" width="110"><template #default="{ row }"><UiTag :active="row.role === 'admin'">{{ row.role === 'admin' ? '管理员' : '用户' }}</UiTag></template></el-table-column>
          <el-table-column label="状态" width="110"><template #default="{ row }"><span class="status-text" :class="{ disabled: Number(row.status) !== 1 }"><i />{{ Number(row.status) === 1 ? '正常' : '已封禁' }}</span></template></el-table-column>
          <el-table-column label="注册时间" min-width="150"><template #default="{ row }">{{ formatDate(row.created_at) }}</template></el-table-column>
          <el-table-column label="操作" width="120" align="right"><template #default="{ row }"><UiButton v-if="row.id !== currentUserId" :variant="Number(row.status) === 1 ? 'danger' : 'secondary'" size="sm" @click="toggleUser(row)">{{ Number(row.status) === 1 ? '封禁' : '启用' }}</UiButton><span v-else class="self-label">当前账号</span></template></el-table-column>
        </el-table>
      </UiCard>
      <el-pagination v-model:current-page="userPage" :page-size="12" :total="userTotal" layout="prev, pager, next" @current-change="loadUsers" />
    </section>

    <section v-else class="management-section">
      <div class="management-toolbar">
        <div><h2>作品管理</h2><p>管理公开内容与违规作品。</p></div>
        <form class="management-search" @submit.prevent="loadImages(1)">
          <UiInput v-model="imageSearch" clearable placeholder="搜索作品或创作者" />
          <UiButton type="submit">搜索</UiButton>
        </form>
      </div>
      <UiCard padding="none" class="table-card">
        <el-table v-loading="loadingImages" :data="images">
          <el-table-column label="作品" min-width="280"><template #default="{ row }"><div class="work-cell"><img :src="row.thumbnail_url || row.url" alt=""><span><strong>{{ row.title || row.original_name || '未命名作品' }}</strong><small>{{ row.author_name || row.username || '匿名创作者' }}</small></span></div></template></el-table-column>
          <el-table-column label="数据" min-width="180"><template #default="{ row }"><div class="row-metrics"><span><el-icon><View /></el-icon>{{ formatNumber(row.view_count) }}</span><span><el-icon><Star /></el-icon>{{ formatNumber(row.like_count) }}</span></div></template></el-table-column>
          <el-table-column label="公开" width="90"><template #default="{ row }"><UiTag :active="Number(row.is_public) === 1">{{ Number(row.is_public) === 1 ? '公开' : '私有' }}</UiTag></template></el-table-column>
          <el-table-column label="状态" width="100"><template #default="{ row }"><span class="status-text" :class="{ disabled: Number(row.status) !== 1 }"><i />{{ Number(row.status) === 1 ? '正常' : Number(row.status) === 2 ? '已封禁' : '已删除' }}</span></template></el-table-column>
          <el-table-column label="操作" width="190" align="right"><template #default="{ row }"><div class="row-actions"><UiButton variant="secondary" size="sm" @click="router.push(`/post/${row.id}`)">查看</UiButton><UiButton v-if="Number(row.status) !== 0" :variant="Number(row.status) === 1 ? 'danger' : 'secondary'" size="sm" @click="toggleImage(row)">{{ Number(row.status) === 1 ? '封禁' : '恢复' }}</UiButton></div></template></el-table-column>
        </el-table>
      </UiCard>
      <el-pagination v-model:current-page="imagePage" :page-size="12" :total="imageTotal" layout="prev, pager, next" @current-change="loadImages" />
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { ChatDotRound, CollectionTag, Picture, Star, User, View } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import EmptyState from '@/components/common/EmptyState.vue'
import UiButton from '@/components/ui/UiButton.vue'
import UiCard from '@/components/ui/UiCard.vue'
import UiInput from '@/components/ui/UiInput.vue'
import UiTag from '@/components/ui/UiTag.vue'
import { banImage, getImages, getStats, getUsers, updateUserStatus } from '@/api/admin'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const validSections = ['overview', 'users', 'images']
const normalizeSection = value => validSections.includes(value) ? value : 'overview'
const activeSection = ref(normalizeSection(route.query.section))
const userChartRef = ref(null)
const imageChartRef = ref(null)
const tagChartRef = ref(null)
const charts = []
const stats = reactive({
  users: { total: 0, today: 0 },
  images: { total: 0, today: 0, public: 0 },
  interactions: { views: 0, likes: 0, collects: 0, comments: 0 },
  sessions: { online: 0 },
  userGrowth: [],
  imageGrowth: [],
  tagDistribution: []
})
const users = ref([])
const loadingUsers = ref(false)
const userSearch = ref('')
const userPage = ref(1)
const userTotal = ref(0)
const images = ref([])
const loadingImages = ref(false)
const imageSearch = ref('')
const imagePage = ref(1)
const imageTotal = ref(0)

const currentUserId = computed(() => userStore.userInfo?.id)
const metrics = computed(() => [
  { label: '用户总数', value: stats.users.total, today: stats.users.today, icon: User, tone: 'green' },
  { label: '作品总数', value: stats.images.total, today: stats.images.today, icon: Picture, tone: 'blue' },
  { label: '公开作品', value: stats.images.public, icon: View, tone: 'violet' },
  { label: '累计获赞', value: stats.interactions.likes, icon: Star, tone: 'orange' }
])
const interactionMetrics = computed(() => [
  { label: '浏览', value: stats.interactions.views, icon: View },
  { label: '点赞', value: stats.interactions.likes, icon: Star },
  { label: '收藏', value: stats.interactions.collects, icon: CollectionTag },
  { label: '评论', value: stats.interactions.comments, icon: ChatDotRound }
])

const formatNumber = value => {
  const number = Number(value || 0)
  if (number >= 10000) return `${(number / 10000).toFixed(1)}w`
  if (number >= 1000) return `${(number / 1000).toFixed(1)}k`
  return String(number)
}
const formatDate = value => value ? new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium' }).format(new Date(value)) : ''

const lineOption = (data, color, name) => {
  const source = data.length ? data : Array.from({ length: 7 }, (_, index) => ({ label: `第 ${index + 1} 天`, value: 0 }))
  return {
    animationDuration: 500,
    tooltip: { trigger: 'axis' },
    grid: { top: 24, right: 16, bottom: 28, left: 40 },
    xAxis: { type: 'category', boundaryGap: false, data: source.map(item => item.label), axisLine: { lineStyle: { color: '#eaeaea' } }, axisTick: { show: false }, axisLabel: { color: '#8b8f8c', fontSize: 10 } },
    yAxis: { type: 'value', minInterval: 1, axisLine: { show: false }, axisTick: { show: false }, splitLine: { lineStyle: { color: '#f0f1f2' } }, axisLabel: { color: '#8b8f8c', fontSize: 10 } },
    series: [{ name, type: 'line', smooth: true, symbolSize: 6, data: source.map(item => Number(item.value || 0)), lineStyle: { color, width: 3 }, itemStyle: { color }, areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: `${color}30` }, { offset: 1, color: `${color}02` }]) } }]
  }
}

const renderCharts = async () => {
  await nextTick()
  charts.splice(0).forEach(chart => chart.dispose())
  if (userChartRef.value) {
    const chart = echarts.init(userChartRef.value)
    chart.setOption(lineOption(stats.userGrowth, '#19c37d', '新增用户'))
    charts.push(chart)
  }
  if (imageChartRef.value) {
    const chart = echarts.init(imageChartRef.value)
    chart.setOption(lineOption(stats.imageGrowth, '#4f7cff', '新增作品'))
    charts.push(chart)
  }
  if (tagChartRef.value && stats.tagDistribution.length) {
    const chart = echarts.init(tagChartRef.value)
    chart.setOption({
      tooltip: { trigger: 'item' },
      legend: { orient: 'vertical', right: 0, top: 'middle', textStyle: { color: '#666', fontSize: 10 } },
      color: ['#19c37d', '#4f7cff', '#f0a020', '#9467e8', '#e85d5d', '#36a5a5', '#8fb339', '#7c8798'],
      series: [{ type: 'pie', radius: ['42%', '70%'], center: ['35%', '50%'], avoidLabelOverlap: true, itemStyle: { borderColor: '#fff', borderWidth: 3, borderRadius: 6 }, label: { show: false }, data: stats.tagDistribution }]
    })
    charts.push(chart)
  }
}

const loadStats = async () => {
  try {
    Object.assign(stats, await getStats())
    await renderCharts()
  } catch (error) {
    console.error('加载 Dashboard 数据失败:', error)
  }
}

const loadUsers = async page => {
  if (page) userPage.value = page
  loadingUsers.value = true
  try {
    const data = await getUsers({ page: userPage.value, pageSize: 12, keyword: userSearch.value.trim() })
    users.value = data?.list || []
    userTotal.value = Number(data?.total || 0)
  } finally {
    loadingUsers.value = false
  }
}

const loadImages = async page => {
  if (page) imagePage.value = page
  loadingImages.value = true
  try {
    const data = await getImages({ page: imagePage.value, pageSize: 12, keyword: imageSearch.value.trim() })
    images.value = data?.list || []
    imageTotal.value = Number(data?.total || 0)
  } finally {
    loadingImages.value = false
  }
}

const toggleUser = async row => {
  const enabling = Number(row.status) !== 1
  try {
    await ElMessageBox.confirm(`${enabling ? '启用' : '封禁'}用户“${row.nickname || row.username}”？`, '用户状态', { type: 'warning' })
    await updateUserStatus(row.id, enabling ? 1 : 0)
    row.status = enabling ? 1 : 0
    ElMessage.success(enabling ? '用户已启用' : '用户已封禁')
  } catch {
    // Cancelled or handled by the request interceptor.
  }
}

const toggleImage = async row => {
  const restoring = Number(row.status) !== 1
  await banImage(row.id, restoring ? 1 : 2)
  row.status = restoring ? 1 : 2
  ElMessage.success(restoring ? '作品已恢复' : '作品已封禁')
}

const handleResize = () => charts.forEach(chart => chart.resize())
watch(() => route.query.section, value => {
  const section = normalizeSection(value)
  activeSection.value = section
  if (section === 'overview') nextTick(renderCharts)
  if (section === 'users' && !users.value.length) loadUsers(1)
  if (section === 'images' && !images.value.length) loadImages(1)
})

onMounted(() => {
  loadStats()
  if (activeSection.value === 'users') loadUsers(1)
  if (activeSection.value === 'images') loadImages(1)
  window.addEventListener('resize', handleResize)
})
onBeforeUnmount(() => {
  charts.forEach(chart => chart.dispose())
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.dashboard-page { max-width: 1360px; padding-bottom: var(--space-8); }
.dashboard-header { display: flex; align-items: center; justify-content: space-between; gap: var(--space-6); margin-bottom: var(--space-6); }
.online-status { display: inline-flex; align-items: center; gap: var(--space-2); border: 1px solid var(--border); border-radius: var(--radius-full); padding: 6px 12px; color: var(--text-secondary); background: var(--card); font-size: 11px; }
.online-status i,
.status-text i { width: 7px; height: 7px; border-radius: 50%; background: var(--success); }

.metric-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: var(--space-4); margin-bottom: var(--space-4); }
.metric-card { display: flex; align-items: center; gap: var(--space-4); }
.metric-icon { width: 46px; height: 46px; display: grid; place-items: center; flex: none; border-radius: var(--radius-lg); font-size: 21px; }
.metric-icon.green { color: var(--primary-active); background: var(--primary-soft); }
.metric-icon.blue { color: #4f7cff; background: rgba(79,124,255,.1); }
.metric-icon.violet { color: #9467e8; background: rgba(148,103,232,.1); }
.metric-icon.orange { color: #d88912; background: rgba(240,160,32,.12); }
.metric-card > div { display: grid; }
.metric-card strong { font-size: 23px; line-height: 1.2; }
.metric-card span { color: var(--text-secondary); font-size: 11px; }
.metric-card small { color: var(--primary-active); font-size: 10px; }

.chart-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: var(--space-4); }
.chart-card header,
.interaction-card header { display: flex; align-items: flex-start; justify-content: space-between; gap: var(--space-4); }
.chart-card h2,
.interaction-card h2 { margin: 0; font-size: 16px; }
.chart-card p,
.interaction-card p { margin: 2px 0 0; color: var(--text-tertiary); font-size: 10px; }
.chart-card header > span { color: var(--text-tertiary); font: 10px var(--font-mono); text-transform: uppercase; letter-spacing: .1em; }
.chart { height: 270px; margin-top: var(--space-3); }
.interaction-list { display: grid; gap: var(--space-3); margin-top: var(--space-6); }
.interaction-list div { display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid var(--border); padding-bottom: var(--space-3); }
.interaction-list span { display: inline-flex; align-items: center; gap: var(--space-2); color: var(--text-secondary); font-size: 12px; }
.interaction-list strong { font-size: 17px; }

.management-section { display: grid; gap: var(--space-4); }
.management-toolbar { display: flex; align-items: end; justify-content: space-between; gap: var(--space-6); }
.management-toolbar h2 { margin: 0; font-size: 20px; }
.management-toolbar p { margin: 2px 0 0; color: var(--text-secondary); font-size: 11px; }
.management-search { width: min(460px, 100%); display: grid; grid-template-columns: 1fr auto; gap: var(--space-2); }
.table-card { overflow: hidden; }
.table-card :deep(.el-table) { --el-table-border-color: var(--border); --el-table-header-bg-color: var(--surface-muted); --el-table-row-hover-bg-color: var(--primary-soft); color: var(--text-primary); background: var(--card); }
.user-cell,
.work-cell,
.row-metrics,
.row-actions { display: flex; align-items: center; gap: var(--space-3); }
.user-cell > span,
.work-cell > span { min-width: 0; display: grid; }
.user-cell small,
.work-cell small { color: var(--text-tertiary); font-size: 10px; }
.work-cell img { width: 54px; height: 42px; border-radius: var(--radius-sm); object-fit: cover; background: var(--surface-muted); }
.row-metrics span { display: inline-flex; align-items: center; gap: 3px; color: var(--text-secondary); font-size: 11px; }
.status-text { display: inline-flex; align-items: center; gap: 5px; color: var(--success); font-size: 11px; }
.status-text.disabled { color: var(--danger); }
.status-text.disabled i { background: var(--danger); }
.self-label { color: var(--text-tertiary); font-size: 11px; }
.management-section :deep(.el-pagination) { justify-self: end; }

@media (max-width: 980px) {
  .metric-grid { grid-template-columns: repeat(2, 1fr); }
  .chart-grid { grid-template-columns: 1fr; }
}

@media (max-width: 680px) {
  .dashboard-header,
  .management-toolbar { align-items: flex-start; flex-direction: column; }
  .metric-grid { grid-template-columns: 1fr; }
  .management-search { width: 100%; }
}
</style>
