<template>
  <div class="ranking-page">
    <div class="ranking-header">
      <h1 class="page-title">
        <el-icon class="title-icon"><Trophy /></el-icon>
        排行榜
      </h1>
    </div>

    <div class="tabs-container">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        :class="['tab-btn', { active: activeTab === tab.key }]"
        @click="changeTab(tab.key)"
      >
        {{ tab.label }}
      </button>
    </div>

    <div v-loading="loading" class="ranking-list">
      <template v-if="rankings.length > 0">
        <div
          v-for="(item, index) in rankings"
          :key="item.id"
          class="ranking-item"
        >
          <div
            class="rank-badge"
            :style="{ backgroundColor: getRankColor(index + 1) }"
          >
            <span class="rank-number">{{ index + 1 }}</span>
          </div>

          <div class="item-thumbnail">
            <img
              :src="item.thumbnail"
              :alt="item.title"
              loading="lazy"
            />
          </div>

          <div class="item-info">
            <h3 class="item-title">{{ item.title }}</h3>
            <p class="item-author">{{ item.author }}</p>
          </div>

          <div class="item-likes">
            <el-icon class="like-icon"><Star /></el-icon>
            <span class="like-count">{{ formatNumber(item.likes) }}</span>
          </div>
        </div>
      </template>

      <EmptyState
        v-else-if="!loading"
        description="暂无排行数据"
        icon="empty"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Trophy, Star } from '@element-plus/icons-vue'
import { getRankings } from '@/api/social'
import EmptyState from '@/components/common/EmptyState.vue'

const tabs = [
  { key: 'weekly', label: '周榜' },
  { key: 'monthly', label: '月榜' },
  { key: 'total', label: '总榜' }
]

const activeTab = ref('weekly')
const rankings = ref([])
const loading = ref(false)

const loadRankings = async () => {
  loading.value = true
  try {
    const response = await getRankings({ type: activeTab.value, page: 1, pageSize: 20 })
    rankings.value = (response?.list || response?.data || []).map(item => ({
      ...item,
      thumbnail: item.thumbnail || item.thumbnail_url || item.url,
      title: item.title || item.original_name || '未命名作品',
      author: item.author || item.author_name || '匿名创作者',
      likes: Number(item.likes ?? item.like_count ?? 0)
    }))
  } catch (error) {
    console.error('Failed to load rankings:', error)
    rankings.value = []
  } finally {
    loading.value = false
  }
}

const changeTab = (tab) => {
  activeTab.value = tab
  loadRankings()
}

const getRankColor = (rank) => {
  const colors = {
    1: '#FFD700',
    2: '#C0C0C0',
    3: '#CD7F32'
  }
  return colors[rank] || '#E8E8E8'
}

const formatNumber = (num) => {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  }
  if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'k'
  }
  return num.toString()
}

onMounted(() => {
  loadRankings()
})
</script>

<style scoped>
.ranking-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 24px;
}

.ranking-header {
  margin-bottom: 24px;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.title-icon {
  color: #16C784;
  font-size: 28px;
}

.tabs-container {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.tab-btn {
  padding: 8px 24px;
  border: none;
  border-radius: 6px;
  background: #f5f5f5;
  color: #666;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.tab-btn:hover {
  background: #e8e8e8;
}

.tab-btn.active {
  background: #16C784;
  color: white;
}

.ranking-list {
  min-height: 300px;
}

.ranking-item {
  display: flex;
  align-items: center;
  padding: 16px;
  margin-bottom: 12px;
  background: white;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.ranking-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.rank-badge {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.rank-number {
  font-size: 16px;
  font-weight: 700;
  color: white;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}

.item-thumbnail {
  width: 100px;
  height: 75px;
  margin-left: 16px;
  border-radius: 6px;
  overflow: hidden;
  flex-shrink: 0;
}

.item-thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.item-info {
  flex: 1;
  margin-left: 16px;
  min-width: 0;
}

.item-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0 0 4px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-author {
  font-size: 13px;
  color: #888;
  margin: 0;
}

.item-likes {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-left: 16px;
  flex-shrink: 0;
}

.like-icon {
  color: #FFD700;
  font-size: 18px;
}

.like-count {
  font-size: 15px;
  font-weight: 600;
  color: #666;
}

@media (max-width: 768px) {
  .ranking-page {
    padding: 16px;
  }

  .tabs-container {
    gap: 8px;
  }

  .tab-btn {
    padding: 6px 16px;
    font-size: 13px;
  }

  .ranking-item {
    padding: 12px;
  }

  .item-thumbnail {
    width: 80px;
    height: 60px;
  }

  .item-title {
    font-size: 14px;
  }

  .item-author {
    font-size: 12px;
  }

  .like-count {
    font-size: 13px;
  }
}

@media (max-width: 480px) {
  .item-thumbnail {
    width: 60px;
    height: 45px;
    margin-left: 12px;
  }

  .item-info {
    margin-left: 12px;
  }

  .item-likes {
    margin-left: 8px;
  }
}
</style>
