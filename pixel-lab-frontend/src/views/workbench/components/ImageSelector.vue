<template>
  <el-dialog
    :model-value="visible"
    title="选择个人中心图片"
    width="800px"
    @update:model-value="$emit('update:visible', $event)"
  >
    <div
      v-loading="loading"
      class="image-selector"
    >
      <div
        v-if="images.length > 0"
        class="image-grid"
      >
        <div
          v-for="image in images"
          :key="image.id"
          class="image-item"
          :class="{ selected: selectedId === image.id }"
          @click="selectedId = image.id"
        >
          <img
            :src="image.url"
            :alt="image.original_name"
          >
          <div class="image-name">
            {{ image.original_name }}
          </div>
        </div>
      </div>
      <EmptyState
        v-else
        title="暂无图片"
        description="请先上传图片到个人中心"
      />
    </div>
    <template #footer>
      <el-button @click="$emit('update:visible', false)">
        取消
      </el-button>
      <el-button
        type="primary"
        :disabled="!selectedId"
        @click="confirm"
      >
        确定
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import EmptyState from '@/components/common/EmptyState.vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  images: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
})

const emit = defineEmits(['update:visible', 'confirm'])

const selectedId = ref(null)

// 重置选中状态
watch(() => props.visible, (val) => {
  if (val) {
    selectedId.value = null
  }
})

const confirm = () => {
  const image = props.images.find(img => img.id === selectedId.value)
  if (image) {
    emit('confirm', image)
    emit('update:visible', false)
  }
}
</script>

<style scoped>
.image-selector {
  max-height: min(62vh, 560px);
  overflow-y: auto;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: var(--space-4);
}

.image-item {
  cursor: pointer;
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--background-card);
  transition: all var(--transition-fast);
  box-shadow: var(--shadow-sm);
}

.image-item:hover {
  transform: translateY(-2px);
  border-color: var(--border-hover);
  box-shadow: var(--shadow);
}

.image-item.selected {
  border-color: var(--primary);
  box-shadow: var(--glow-sm);
}

.image-item img {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
}

.image-name {
  padding: var(--space-3);
  font-size: 13px;
  font-weight: 650;
  text-align: center;
  background: var(--background-card);
  border-top: 1px solid var(--border);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: var(--foreground);
}
</style>
