<template>
  <div class="stats-page">
    <!-- 时间范围选择 -->
    <div class="stats-header">
      <h1>数据统计</h1>
      <el-radio-group v-model="dateRange" @change="fetchData">
        <el-radio-button value="7">近 7 天</el-radio-button>
        <el-radio-button value="30">近 30 天</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 概览卡片 -->
    <div class="overview-cards">
      <div class="overview-card">
        <div class="card-icon works">
          <el-icon :size="24"><Picture /></el-icon>
        </div>
        <div class="card-content">
          <span class="card-value">{{ stats.works }}</span>
          <span class="card-label">作品数</span>
        </div>
      </div>
      <div class="overview-card">
        <div class="card-icon views">
          <el-icon :size="24"><View /></el-icon>
        </div>
        <div class="card-content">
          <span class="card-value">{{ formatNumber(stats.views) }}</span>
          <span class="card-label">浏览量</span>
        </div>
      </div>
      <div class="overview-card">
        <div class="card-icon likes">
          <el-icon :size="24"><Star /></el-icon>
        </div>
        <div class="card-content">
          <span class="card-value">{{ stats.likes }}</span>
          <span class="card-label">获赞数</span>
        </div>
      </div>
      <div class="overview-card">
        <div class="card-icon collects">
          <el-icon :size="24"><FolderOpened /></el-icon>
        </div>
        <div class="card-content">
          <span class="card-value">{{ stats.collects }}</span>
          <span class="card-label">收藏数</span>
        </div>
      </div>
    </div>

    <!-- 趋势图表 -->
    <div class="chart-section">
      <div class="section-title">互动趋势</div>
      <div class="chart-container" ref="trendChartRef" />
    </div>

    <!-- 热门作品 -->
    <div class="hot-works-section">
      <div class="section-title">热门作品 TOP 5</div>
      <div v-if="hotWorks.length > 0" class="works-list">
        <div v-for="(work, index) in hotWorks" :key="work.id" class="work-item">
          <span class="rank" :class="{ top: index < 3 }">{{ index + 1 }}</span>
          <img :src="work.url" :alt="imageDisplayTitle(work)" class="work-thumb" />
          <div class="work-info">
            <span class="work-title">{{ imageDisplayTitle(work, '未命名') }}</span>
            <span class="work-date">{{ formatDate(work.created_at) }}</span>
          </div>
          <div class="work-stats">
            <span><el-icon><View /></el-icon> {{ work.view_count || 0 }}</span>
            <span><el-icon><Star /></el-icon> {{ work.like_count || 0 }}</span>
            <span><el-icon><FolderOpened /></el-icon> {{ work.collect_count || 0 }}</span>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无作品数据" :image-size="80" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { Picture, View, Star, FolderOpened } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getUserStats } from '@/api/auth'
import { imageDisplayTitle } from '@/utils/common'

const dateRange = ref('7')
const stats = ref({ works: 0, views: 0, likes: 0, collects: 0 })
const hotWorks = ref([])
const trendData = ref([])
const trendChartRef = ref(null)
let trendChart = null

const toNumber = (value) => Number(value || 0)

const normalizeStats = (stats = {}) => ({
  works: toNumber(stats.works ?? stats.imageCount),
  views: toNumber(stats.views ?? stats.viewCount),
  likes: toNumber(stats.likes ?? stats.receivedLikeCount ?? stats.likeCount),
  collects: toNumber(stats.collects ?? stats.receivedCollectCount ?? stats.collectionCount)
})

const normalizeTrend = (trend = []) => trend.map(item => ({
  label: item.label || item.date || '',
  works: toNumber(item.works),
  views: toNumber(item.views),
  likes: toNumber(item.likes),
  collects: toNumber(item.collects)
}))

// 格式化数字
const formatNumber = (num) => {
  if (num >= 10000) return (num / 10000).toFixed(1) + 'w'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'k'
  return num
}

// 格式化日期
const formatDate = (date) => {
  if (!date) return ''
  return new Date(date).toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

// 获取数据
const fetchData = async () => {
  try {
    const statsRes = await getUserStats({ days: dateRange.value })

    stats.value = normalizeStats(statsRes)

    // 热门作品按浏览量排序
    trendData.value = normalizeTrend(statsRes.trend || [])
    hotWorks.value = statsRes.hotWorks || []

    // 初始化图表
    await nextTick()
    initTrendChart()
  } catch (error) {
    console.error('获取数据失败:', error)
  }
}

// 初始化趋势图表
const initTrendChart = () => {
  if (!trendChartRef.value) return

  if (trendChart) {
    trendChart.dispose()
  }

  trendChart = echarts.init(trendChartRef.value)

  const source = trendData.value
  const dateLabels = source.map(item => item.label)
  const viewsData = source.map(item => item.views)
  const likesData = source.map(item => item.likes)
  const collectsData = source.map(item => item.collects)

  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(0,0,0,0.8)',
      borderColor: '#333',
      textStyle: { color: '#fff' }
    },
    legend: {
      data: ['浏览量', '点赞数', '收藏数'],
      textStyle: { color: 'var(--foreground-muted)' },
      top: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '40px',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: dateLabels,
      axisLine: { lineStyle: { color: 'var(--border)' } },
      axisLabel: { color: 'var(--foreground-muted)' }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      splitLine: { lineStyle: { color: 'var(--border)', opacity: 0.3 } },
      axisLabel: { color: 'var(--foreground-muted)' }
    },
    series: [
      {
        name: '浏览量',
        type: 'line',
        smooth: true,
        data: viewsData,
        lineStyle: { color: '#39c5bb', width: 2 },
        itemStyle: { color: '#39c5bb' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(57, 197, 187, 0.3)' },
            { offset: 1, color: 'rgba(57, 197, 187, 0.05)' }
          ])
        }
      },
      {
        name: '点赞数',
        type: 'line',
        smooth: true,
        data: likesData,
        lineStyle: { color: '#f59e0b', width: 2 },
        itemStyle: { color: '#f59e0b' }
      },
      {
        name: '收藏数',
        type: 'line',
        smooth: true,
        data: collectsData,
        lineStyle: { color: '#8b5cf6', width: 2 },
        itemStyle: { color: '#8b5cf6' }
      }
    ]
  }

  trendChart.setOption(option)
}

// 窗口大小变化时重绘图表
const handleResize = () => {
  trendChart?.resize()
}

onMounted(() => {
  fetchData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  trendChart?.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.stats-page {
  padding: var(--space-6);
  max-width: 1200px;
  margin: 0 auto;
}

.stats-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-6);
}

.stats-header h1 {
  font-size: 24px;
  font-weight: 700;
}

/* 概览卡片 */
.overview-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4);
  margin-bottom: var(--space-6);
}

.overview-card {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-5);
  background: var(--background-soft);
  border: 3px solid var(--border);
  box-shadow: 4px 4px 0 var(--border);
}

.card-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 3px solid var(--border);
}

.card-icon.works { background: var(--primary); color: white; }
.card-icon.views { background: #39c5bb; color: white; }
.card-icon.likes { background: #f59e0b; color: white; }
.card-icon.collects { background: #8b5cf6; color: white; }

.card-content {
  display: flex;
  flex-direction: column;
}

.card-value {
  font-size: 28px;
  font-weight: 700;
  font-family: var(--font-mono);
  color: var(--foreground);
}

.card-label {
  font-size: 12px;
  color: var(--foreground-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

/* 图表区域 */
.chart-section {
  background: var(--background-soft);
  border: 3px solid var(--border);
  box-shadow: 4px 4px 0 var(--border);
  padding: var(--space-5);
  margin-bottom: var(--space-6);
}

.section-title {
  font-size: 14px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: var(--space-4);
  color: var(--foreground-muted);
}

.chart-container {
  height: 300px;
}

/* 热门作品 */
.hot-works-section {
  background: var(--background-soft);
  border: 3px solid var(--border);
  box-shadow: 4px 4px 0 var(--border);
  padding: var(--space-5);
}

.works-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.work-item {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-3);
  background: var(--background);
  border: 2px solid var(--border);
}

.rank {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 14px;
  color: var(--foreground-muted);
  background: var(--background-muted);
  border: 2px solid var(--border);
}

.rank.top {
  background: var(--primary);
  color: white;
  border-color: var(--primary);
}

.work-thumb {
  width: 48px;
  height: 48px;
  object-fit: cover;
  border: 2px solid var(--border);
}

.work-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.work-title {
  font-weight: 600;
  font-size: 14px;
}

.work-date {
  font-size: 12px;
  color: var(--foreground-muted);
}

.work-stats {
  display: flex;
  gap: var(--space-4);
  font-size: 12px;
  color: var(--foreground-muted);
}

.work-stats span {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 响应式 */
@media (max-width: 768px) {
  .overview-cards {
    grid-template-columns: repeat(2, 1fr);
  }

  .work-stats {
    display: none;
  }
}
</style>
