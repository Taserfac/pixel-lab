<!--
  【文件路径】src/components/common/EmptyState.vue
  【文件功能说明】通用空状态组件
  - 支持自定义图片、文字、按钮
  - 用于列表为空、搜索无结果等场景
-->

<template>
  <div
    class="empty-state"
    :class="{ 'has-action': showAction }"
  >
    <!-- 图片 -->
    <div class="image-wrapper">
      <img
        v-if="image"
        :src="image"
        :alt="description"
        class="image"
      >
      <el-icon
        v-else
        :size="imageSize"
        class="default-icon"
      >
        <Box />
      </el-icon>
    </div>
    
    <!-- 标题 -->
    <h3
      v-if="title"
      class="title"
    >
      {{ title }}
    </h3>
    
    <!-- 描述 -->
    <p
      v-if="description"
      class="description"
    >
      {{ description }}
    </p>
    
    <!-- 操作按钮 -->
    <div
      v-if="showAction"
      class="action"
    >
      <el-button
        type="primary"
        @click="handleAction"
      >
        {{ actionText }}
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { Box } from '@element-plus/icons-vue'

// eslint-disable-next-line no-unused-vars
const props = defineProps({
  // 自定义图片地址
  image: {
    type: String,
    default: ''
  },
  // 图片尺寸
  imageSize: {
    type: Number,
    default: 64
  },
  // 标题
  title: {
    type: String,
    default: '暂无数据'
  },
  // 描述文字
  description: {
    type: String,
    default: ''
  },
  // 是否显示操作按钮
  showAction: {
    type: Boolean,
    default: false
  },
  // 操作按钮文字
  actionText: {
    type: String,
    default: '去创建'
  }
})

const emit = defineEmits(['action'])

// 处理操作按钮点击
const handleAction = () => {
  emit('action')
}
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-12) var(--space-6);
  text-align: center;
}

.image-wrapper {
  margin-bottom: var(--space-4);
}

.image {
  width: 120px;
  height: 120px;
  object-fit: contain;
}

.default-icon {
  color: var(--foreground-subtle);
}

.title {
  font-size: 16px;
  font-weight: 500;
  color: var(--foreground);
  margin-bottom: var(--space-2);
}

.description {
  font-size: 14px;
  color: var(--foreground-muted);
  margin-bottom: var(--space-6);
  max-width: 400px;
}

.empty-state.has-action .description {
  margin-bottom: var(--space-6);
}
</style>
