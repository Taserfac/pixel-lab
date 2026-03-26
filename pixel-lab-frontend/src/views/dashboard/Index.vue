<template>
  <div class="dashboard-grid">
    <!-- Hero 区域 -->
    <section class="hero-card">
      <div class="hero-content">
        <h1 class="hero-title">
          <span class="title-line">创造你的</span>
          <span class="title-highlight">像素艺术</span>
        </h1>
        <p class="hero-desc">
          上传图片，一键转换像素风格。探索无限创意可能。
        </p>
        <div class="hero-actions">
          <el-button
            type="primary"
            size="large"
            @click="$router.push('/workbench')"
          >
            <el-icon><EditPen /></el-icon>
            开始创作
          </el-button>
          <el-button
            size="large"
            @click="$router.push('/community')"
          >
            <el-icon><ChatDotRound /></el-icon>
            探索社区
          </el-button>
        </div>
      </div>
      <div class="hero-visual">
        <div class="pixel-showcase">
          <div
            v-for="i in 16"
            :key="i"
            class="pixel-block"
            :style="{ animationDelay: `${i * 0.1}s` }"
          />
        </div>
      </div>
    </section>

    <!-- 快捷操作卡片 -->
    <section class="quick-actions">
      <div
        class="action-card"
        @click="$router.push('/workbench')"
      >
        <div class="action-icon workbench">
          <el-icon :size="28">
            <Picture />
          </el-icon>
        </div>
        <div class="action-info">
          <h3>工作台</h3>
          <p>图片处理与像素化</p>
        </div>
      </div>
      <div
        class="action-card"
        @click="$router.push('/draw')"
      >
        <div class="action-icon draw">
          <el-icon :size="28">
            <EditPen />
          </el-icon>
        </div>
        <div class="action-info">
          <h3>手绘板</h3>
          <p>自由创作像素画</p>
        </div>
      </div>
      <div
        class="action-card"
        @click="$router.push('/community')"
      >
        <div class="action-icon community">
          <el-icon :size="28">
            <ChatDotRound />
          </el-icon>
        </div>
        <div class="action-info">
          <h3>社区</h3>
          <p>发现优秀作品</p>
        </div>
      </div>
    </section>

    <!-- 我的作品 -->
    <section class="my-works-card">
      <div class="section-header">
        <h2>我的作品</h2>
        <el-button
          text
          @click="$router.push('/personal')"
        >
          查看全部 <el-icon><ArrowRight /></el-icon>
        </el-button>
      </div>
      <div
        v-if="recentWorks.length > 0"
        class="works-grid"
      >
        <div
          v-for="work in recentWorks"
          :key="work.id"
          class="work-item"
        >
          <img
            :src="work.url"
            :alt="work.name"
          >
          <div class="work-overlay">
            <span>{{ work.name }}</span>
          </div>
        </div>
      </div>
      <EmptyState
        v-else
        title="暂无作品"
        description="开始创作你的第一个像素艺术吧！"
        :show-action="false"
      />
    </section>

    <!-- 统计数据 -->
    <section class="stats-card">
      <div class="stat-item">
        <span class="stat-value">{{ userStats.works }}</span>
        <span class="stat-label">作品数</span>
      </div>
      <div class="stat-item">
        <span class="stat-value">{{ userStats.likes }}</span>
        <span class="stat-label">获赞数</span>
      </div>
      <div class="stat-item">
        <span class="stat-value">{{ userStats.views }}</span>
        <span class="stat-label">浏览量</span>
      </div>
    </section>

    <!-- 社区动态 -->
    <section class="community-card">
      <div class="section-header">
        <h2>社区动态</h2>
        <el-button
          text
          @click="$router.push('/community')"
        >
          更多 <el-icon><ArrowRight /></el-icon>
        </el-button>
      </div>
      <div class="activity-list">
        <div
          v-for="activity in communityActivities"
          :key="activity.id"
          class="activity-item"
        >
          <el-avatar
            :size="36"
            :src="activity.avatar"
          >
            {{ activity.user.charAt(0) }}
          </el-avatar>
          <div class="activity-content">
            <p><strong>{{ activity.user }}</strong> 发布了 <em>{{ activity.title }}</em></p>
            <span class="activity-time">{{ activity.time }}</span>
          </div>
        </div>
        <div v-if="communityActivities.length === 0" class="no-activity">
          暂无社区动态
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Picture, EditPen, ChatDotRound, ArrowRight } from '@element-plus/icons-vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { getUserImages } from '@/api/image'
import { getUserStats } from '@/api/auth'
import { getActivities } from '@/api/community'

const recentWorks = ref([])
const userStats = ref({
  works: 0,
  likes: 0,
  views: 0
})
const communityActivities = ref([])

// 格式化时间
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

onMounted(async () => {
  try {
    // 并行请求
    const [imagesRes, statsRes, activitiesRes] = await Promise.all([
      getUserImages(),
      getUserStats().catch(() => ({ works: 0, likes: 0, views: 0 })),
      getActivities({ limit: 5 }).catch(() => [])
    ])

    recentWorks.value = (imagesRes.list || []).slice(0, 4)
    userStats.value = statsRes
    communityActivities.value = (activitiesRes || []).map(item => ({
      id: item.id,
      user: item.author_name || '匿名用户',
      avatar: item.author_avatar || '',
      title: item.title || item.original_name || '无标题作品',
      time: formatTime(item.created_at)
    }))
  } catch (error) {
    console.error('获取数据失败:', error)
  }
})
</script>

<style scoped>
.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
  max-width: 1440px;
  margin: 0 auto;
  padding: 0 16px;
}

/* Hero 卡片 */
.hero-card {
  grid-column: span 3;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 48px 56px;
  background: linear-gradient(135deg, var(--primary) 0%, var(--secondary) 100%);
  border: 4px solid var(--border);
  box-shadow: 12px 12px 0px 0px var(--border);
  min-height: 300px;
}

.hero-content {
  flex: 1;
}

.hero-title {
  font-family: var(--font-mono);
  font-size: 44px;
  font-weight: 700;
  line-height: 1.4;
  color: white;
}

.title-line {
  display: block;
}

.title-highlight {
  display: inline-block;
  background: white;
  color: var(--primary);
  padding: 4px 16px;
  margin-top: 8px;
  border: 3px solid var(--border);
  box-shadow: 4px 4px 0px 0px var(--border);
}

.hero-desc {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.9);
  margin: 24px 0 32px;
  max-width: 400px;
  line-height: 1.6;
}

.hero-actions {
  display: flex;
  gap: 16px;
}

.hero-visual {
  width: 180px;
  height: 180px;
  position: relative;
}

.pixel-showcase {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 6px;
  transform: rotate(12deg);
}

.pixel-block {
  width: 36px;
  height: 36px;
  background: white;
  border: 2px solid var(--border);
  animation: pixelPulse 2s infinite;
}

@keyframes pixelPulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.08); }
}

/* 快捷操作 */
.quick-actions {
  grid-column: span 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.action-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: var(--background-soft);
  border: 4px solid var(--border);
  box-shadow: 6px 6px 0px 0px var(--border);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.action-card:hover {
  transform: translate(-4px, -4px);
  box-shadow: 10px 10px 0px 0px var(--border);
}

.action-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 3px solid var(--border);
  flex-shrink: 0;
}

.action-icon.workbench { background: var(--primary); color: white; }
.action-icon.draw { background: var(--secondary); color: var(--foreground); }
.action-icon.community { background: var(--accent); color: var(--foreground); }

.action-info h3 {
  font-size: 15px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  margin-bottom: 4px;
}

.action-info p {
  font-size: 12px;
  color: var(--foreground-muted);
}

/* 我的作品 */
.my-works-card {
  grid-column: span 2;
  padding: 20px;
  background: var(--background-soft);
  border: 4px solid var(--border);
  box-shadow: 8px 8px 0px 0px var(--border);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-header h2 {
  font-size: 14px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.works-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.work-item {
  position: relative;
  aspect-ratio: 1;
  border: 3px solid var(--border);
  overflow: hidden;
}

.work-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.work-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 6px 8px;
  background: var(--primary);
  color: white;
  font-size: 11px;
  font-weight: 600;
  transform: translateY(100%);
  transition: transform var(--transition-fast);
}

.work-item:hover .work-overlay {
  transform: translateY(0);
}

/* 统计数据 */
.stats-card {
  grid-column: span 2;
  display: flex;
  justify-content: space-around;
  align-items: center;
  padding: 20px 24px;
  background: var(--background-soft);
  border: 4px solid var(--border);
  box-shadow: 8px 8px 0px 0px var(--border);
}

.stat-item {
  text-align: center;
}

.stat-value {
  display: block;
  font-family: var(--font-mono);
  font-size: 28px;
  font-weight: 700;
  color: var(--primary);
  margin-bottom: 2px;
}

.stat-label {
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: var(--foreground-muted);
}

/* 社区动态 */
.community-card {
  grid-column: span 2;
  padding: 24px;
  background: var(--background-soft);
  border: 4px solid var(--border);
  box-shadow: 8px 8px 0px 0px var(--border);
}

.activity-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.activity-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border: 3px solid var(--border);
  background: var(--background);
}

.activity-content p {
  font-size: 13px;
  margin-bottom: 2px;
}

.activity-time {
  font-size: 11px;
  color: var(--foreground-muted);
}

.no-activity {
  text-align: center;
  padding: 24px;
  color: var(--foreground-muted);
  font-size: 13px;
}

/* 响应式 */
@media (max-width: 1200px) {
  .dashboard-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .hero-card {
    grid-column: span 2;
  }

  .quick-actions {
    grid-column: span 2;
    flex-direction: row;
  }
}

@media (max-width: 768px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .hero-card {
    grid-column: span 1;
    flex-direction: column;
    text-align: center;
    padding: 32px 24px;
  }

  .hero-title {
    font-size: 32px;
  }

  .hero-visual {
    display: none;
  }

  .quick-actions {
    grid-column: span 1;
    flex-direction: column;
  }

  .my-works-card,
  .stats-card,
  .community-card {
    grid-column: span 1;
  }
}
</style>