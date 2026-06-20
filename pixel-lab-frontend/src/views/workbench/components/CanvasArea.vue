<template>
  <div class="canvas-area">
    <div class="canvas-header">
      <div class="canvas-meta">
        <span class="canvas-name">{{ imageName || '未命名图片' }}</span>
        <span class="canvas-size">{{ canvasSizeText }}</span>
      </div>
      <div class="canvas-view-actions">
        <el-button
          circle
          title="缩小"
          @click="$emit('zoom-out')"
        >
          <el-icon><Minus /></el-icon>
        </el-button>
        <button
          class="zoom-value"
          type="button"
          title="适应窗口"
          @click="$emit('fit')"
        >
          {{ Math.round(zoom * 100) }}%
        </button>
        <el-button
          circle
          title="放大"
          @click="$emit('zoom-in')"
        >
          <el-icon><Plus /></el-icon>
        </el-button>
      </div>
    </div>
    <div
      ref="wrapperRef"
      class="canvas-wrapper"
    >
      <canvas
        ref="canvasRef"
        class="editor-canvas"
        :style="displayStyle"
      />
      <div class="compare-switch" aria-label="原图和效果图对比">
        <button
          type="button"
          :class="{ active: previewMode === 'original' }"
          @click="$emit('update:previewMode', 'original')"
        >
          原图
        </button>
        <button
          type="button"
          :class="{ active: previewMode === 'compare' }"
          @click="$emit('update:previewMode', 'compare')"
        >
          效果图对比
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { Minus, Plus } from '@element-plus/icons-vue'

const props = defineProps({
  canvasStyle: { type: Object, default: () => ({}) },
  zoom: { type: Number, default: 1 },
  previewMode: { type: String, default: 'effect' },
  imageName: { type: String, default: '' },
  canvasSize: { type: Object, default: () => ({ width: 0, height: 0 }) }
})

const emit = defineEmits(['canvas-ready', 'resize', 'zoom-in', 'zoom-out', 'fit', 'update:previewMode'])

const wrapperRef = ref(null)
const canvasRef = ref(null)

const canvasSizeText = computed(() => {
  const width = Math.round(props.canvasSize.width || 0)
  const height = Math.round(props.canvasSize.height || 0)
  return width && height ? `${width} × ${height}px` : '等待画布'
})

const displayStyle = computed(() => ({
  ...(props.previewMode === 'original' ? {} : props.canvasStyle),
  width: props.canvasSize.width ? `${Math.round(props.canvasSize.width * props.zoom)}px` : undefined,
  height: props.canvasSize.height ? `${Math.round(props.canvasSize.height * props.zoom)}px` : undefined
}))

// 暴露 refs 给父组件
defineExpose({
  canvas: canvasRef,
  wrapper: wrapperRef
})

// 监听窗口大小变化
onMounted(() => {
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})

const handleResize = () => {
  emit('resize')
}
</script>

<style scoped>
.canvas-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(220, 226, 232, 0.82);
  border-radius: 22px;
  box-shadow: 0 18px 44px rgba(17, 24, 39, 0.08);
  min-width: 0;
  overflow: hidden;
}

.canvas-header {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  min-height: 58px;
  padding: 12px 20px;
  border-bottom: 1px solid rgba(229, 233, 238, 0.92);
  background: rgba(255, 255, 255, 0.92);
}

.canvas-meta {
  min-width: 0;
  display: flex;
  align-items: baseline;
  gap: var(--space-3);
}

.canvas-name {
  max-width: 340px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--foreground);
  font-weight: 800;
}

.canvas-size {
  color: var(--foreground-muted);
  font-size: 12px;
  font-family: var(--font-mono);
}

.canvas-view-actions {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  flex: 0 0 auto;
}

.canvas-view-actions :deep(.el-button) {
  width: 36px;
  height: 36px;
  min-height: 36px;
  padding: 0 !important;
  border: 0;
  background: #fff;
  box-shadow: 0 8px 18px rgba(17, 24, 39, 0.08);
}

.zoom-value {
  min-width: 64px;
  height: 36px;
  border: 0;
  border-radius: var(--radius-full);
  background: #f5f7f9;
  color: var(--foreground);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.canvas-wrapper {
  position: relative;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: clamp(38px, 5vw, 72px) clamp(28px, 6vw, 92px) 70px;
  overflow: auto;
  background:
    linear-gradient(45deg, rgba(230, 234, 239, 0.62) 25%, transparent 25%),
    linear-gradient(-45deg, rgba(230, 234, 239, 0.62) 25%, transparent 25%),
    linear-gradient(45deg, transparent 75%, rgba(230, 234, 239, 0.62) 75%),
    linear-gradient(-45deg, transparent 75%, rgba(230, 234, 239, 0.62) 75%);
  background-color: #fbfcfd;
  background-position: 0 0, 0 12px, 12px -12px, -12px 0;
  background-size: 24px 24px;
}

.editor-canvas {
  flex: 0 0 auto;
  max-width: none;
  max-height: none;
  border: 1px solid rgba(204, 214, 224, 0.8);
  border-radius: 12px;
  box-shadow: 0 20px 46px rgba(17, 24, 39, 0.16);
  transition:
    width var(--transition-fast),
    height var(--transition-fast);
}

.compare-switch {
  position: absolute;
  left: 50%;
  bottom: 18px;
  display: inline-flex;
  align-items: center;
  gap: 2px;
  transform: translateX(-50%);
  padding: 4px;
  border: 1px solid rgba(229, 233, 238, 0.92);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 12px 26px rgba(17, 24, 39, 0.12);
  backdrop-filter: blur(12px);
}

.compare-switch button {
  min-width: 74px;
  min-height: 34px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: #4b5563;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}

.compare-switch button.active {
  background: linear-gradient(135deg, #13c77f, #08aa68);
  color: #fff;
  box-shadow: 0 8px 18px rgba(18, 199, 127, 0.22);
}

@media (max-width: 760px) {
  .canvas-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .canvas-meta {
    width: 100%;
    justify-content: space-between;
  }

  .canvas-name {
    max-width: 62vw;
  }
}
</style>
