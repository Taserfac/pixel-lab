<!--
  【文件路径】src/components/common/NotificationPanel.vue
  【文件功能说明】消息通知面板组件
  - 基于 el-popover 的通知弹出面板
  - 显示不同类型通知（点赞、评论、关注、系统）
  - 支持已读/未读状态和一键全部已读
  - 无通知时显示空状态
-->
<template>
  <el-popover
    :visible="modelValue"
    placement="bottom-end"
    :width="380"
    trigger="click"
    :popper-options="{ modifiers: [{ name: 'offset', options: { offset: [0, 8] } }] }"
    @update:visible="handleVisibleChange"
  >
    <template #reference>
      <div
        class="notification-trigger"
        @click="togglePanel"
      >
        <el-icon :size="20">
          <Bell />
        </el-icon>
        <span
          v-if="unreadCount > 0"
          class="unread-badge"
        >
          {{ unreadCount > 99 ? '99+' : unreadCount }}
        </span>
      </div>
    </template>

    <!-- 面板内容 -->
    <div class="notification-panel">
      <!-- 头部 -->
      <div class="panel-header">
        <span class="panel-title">消息通知</span>
        <button
          v-if="unreadCount > 0"
          class="mark-all-read-btn"
          @click="markAllRead"
        >
          全部已读
        </button>
      </div>

      <!-- 通知列表 -->
      <div
        v-if="notifications.length > 0"
        class="notification-list"
      >
        <div
          v-for="item in notifications"
          :key="item.id"
          class="notification-item"
          :class="{ unread: !item.read }"
          @click="markAsRead(item.id)"
        >
          <!-- 类型图标 -->
          <div
            class="notification-icon"
            :style="{ backgroundColor: getTypeConfig(item.type).bgColor }"
          >
            <el-icon
              :size="16"
              :color="getTypeConfig(item.type).color"
            >
              <component :is="getTypeConfig(item.type).icon" />
            </el-icon>
          </div>

          <!-- 内容 -->
          <div class="notification-content">
            <p class="notification-text">{{ item.text }}</p>
            <span class="notification-time">{{ formatTime(item.time) }}</span>
          </div>

          <!-- 未读指示点 -->
          <div
            v-if="!item.read"
            class="unread-dot"
          />
        </div>
      </div>

      <!-- 空状态 -->
      <div
        v-else
        class="empty-state"
      >
        <el-icon :size="40" color="var(--foreground-subtle)">
          <Bell />
        </el-icon>
        <p class="empty-text">暂无新通知</p>
      </div>
    </div>
  </el-popover>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Bell, ChatDotRound, CollectionTag, Star, UserFilled, InfoFilled } from '@element-plus/icons-vue'

// Props
const props = defineProps({
  // v-model 控制面板显示/隐藏
  modelValue: {
    type: Boolean,
    default: false
  }
})

// Emits
const emit = defineEmits(['update:modelValue'])

// 通知类型配置
const typeConfig = {
  like: {
    icon: Star,
    color: '#FF6B6B',
    bgColor: 'rgba(255, 107, 107, 0.1)'
  },
  collect: {
    icon: CollectionTag,
    color: '#FFB454',
    bgColor: 'rgba(255, 180, 84, 0.1)'
  },
  comment: {
    icon: ChatDotRound,
    color: '#5B8DEF',
    bgColor: 'rgba(91, 141, 239, 0.1)'
  },
  reply: {
    icon: ChatDotRound,
    color: '#7C5AEF',
    bgColor: 'rgba(124, 90, 239, 0.1)'
  },
  follow: {
    icon: UserFilled,
    color: '#16C784',
    bgColor: 'rgba(22, 199, 132, 0.1)'
  },
  system: {
    icon: InfoFilled,
    color: '#FFB454',
    bgColor: 'rgba(255, 180, 84, 0.1)'
  }
}
const getTypeConfig = (type) => typeConfig[type] || typeConfig.system

// 模拟通知数据
const notifications = ref([
  {
    id: 1,
    type: 'like',
    text: '用户 Alex 赞了你的作品《像素风景画》',
    time: new Date(Date.now() - 5 * 60 * 1000),
    read: false
  },
  {
    id: 2,
    type: 'comment',
    text: '用户 Mia 评论了你的作品："颜色搭配真棒！"',
    time: new Date(Date.now() - 30 * 60 * 1000),
    read: false
  },
  {
    id: 3,
    type: 'follow',
    text: '用户 Oliver 关注了你',
    time: new Date(Date.now() - 2 * 60 * 60 * 1000),
    read: true
  },
  {
    id: 4,
    type: 'system',
    text: '你的作品《像素风景画》已通过审核，已发布到社区',
    time: new Date(Date.now() - 5 * 60 * 60 * 1000),
    read: true
  }
])

// 未读数量
const unreadCount = computed(() => {
  return notifications.value.filter(n => !n.read).length
})

// 切换面板显示
const togglePanel = () => {
  emit('update:modelValue', !props.modelValue)
}

// 处理 popover 可见性变化
const handleVisibleChange = (visible) => {
  emit('update:modelValue', visible)
}

// 标记单条为已读
const markAsRead = (id) => {
  const item = notifications.value.find(n => n.id === id)
  if (item) {
    item.read = true
  }
}

// 标记全部为已读
const markAllRead = () => {
  notifications.value.forEach(n => {
    n.read = true
  })
}

// 格式化相对时间
const formatTime = (date) => {
  const now = new Date()
  const diff = now - new Date(date)
  const minutes = Math.floor(diff / (1000 * 60))
  const hours = Math.floor(diff / (1000 * 60 * 60))
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`

  const month = date.getMonth() + 1
  const day = date.getDate()
  return `${month}月${day}日`
}
</script>

<style scoped>
/* 触发按钮 */
.notification-trigger {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: var(--radius-full);
  background: var(--background-soft);
  color: var(--foreground-muted);
  cursor: pointer;
  transition: all var(--transition-fast) var(--ease-out);
}

.notification-trigger:hover {
  background: var(--background-muted);
  color: var(--foreground);
  transform: scale(1.05);
}

/* 未读徽章 */
.unread-badge {
  position: absolute;
  top: 2px;
  right: 0;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: var(--radius-full);
  background: var(--error);
  color: #ffffff;
  font-size: 11px;
  font-weight: 600;
  line-height: 18px;
  text-align: center;
  transform: translate(30%, -30%);
}

/* 面板容器 */
.notification-panel {
  padding: 0;
}

/* 头部 */
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-4) var(--space-5);
  border-bottom: 1px solid var(--border-light);
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--foreground);
}

.mark-all-read-btn {
  padding: 0;
  border: none;
  background: transparent;
  color: var(--primary);
  font-size: 13px;
  cursor: pointer;
  transition: color var(--transition-fast);
}

.mark-all-read-btn:hover {
  color: var(--primary-hover);
}

/* 通知列表 */
.notification-list {
  max-height: 400px;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: var(--border) transparent;
}

.notification-list::-webkit-scrollbar {
  width: 4px;
}

.notification-list::-webkit-scrollbar-track {
  background: transparent;
}

.notification-list::-webkit-scrollbar-thumb {
  border-radius: var(--radius-full);
  background: var(--border);
}

/* 通知项 */
.notification-item {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
  padding: var(--space-4) var(--space-5);
  border-bottom: 1px solid var(--border-light);
  cursor: pointer;
  transition: background var(--transition-fast);
}

.notification-item:last-child {
  border-bottom: none;
}

.notification-item:hover {
  background: var(--background-soft);
}

.notification-item.unread {
  background: var(--background-elevated);
}

/* 类型图标 */
.notification-icon {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: var(--radius-sm);
}

/* 内容区域 */
.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-text {
  margin: 0 0 var(--space-1) 0;
  font-size: 13px;
  line-height: 1.5;
  color: var(--foreground);
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.notification-time {
  font-size: 12px;
  color: var(--foreground-subtle);
}

/* 未读指示点 */
.unread-dot {
  flex-shrink: 0;
  width: 8px;
  height: 8px;
  margin-top: 6px;
  border-radius: var(--radius-full);
  background: var(--primary);
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-12) var(--space-6);
}

.empty-text {
  margin: var(--space-3) 0 0 0;
  font-size: 14px;
  color: var(--foreground-subtle);
}
</style>
