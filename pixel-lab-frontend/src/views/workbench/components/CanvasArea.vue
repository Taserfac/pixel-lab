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
    <div ref="viewportRef" class="canvas-viewport">
      <div
        ref="wrapperRef"
        class="canvas-wrapper"
        :class="{ panning: isPanning }"
        @wheel.prevent="handleWheel"
        @pointerdown="startPan"
        @pointermove="movePan"
        @pointerup="stopPan"
        @pointercancel="stopPan"
        @auxclick.prevent
      >
        <canvas
          ref="canvasRef"
          class="editor-canvas"
          :style="displayStyle"
        />
      </div>
      <button
        class="compare-switch"
        type="button"
        :class="{ active: previewMode === 'original' }"
        aria-label="按住查看原图"
        @pointerdown="showOriginal"
        @pointerup="showEffect"
        @pointerleave="showEffect"
        @pointercancel="showEffect"
        @keydown.space.prevent="showOriginal"
        @keyup.space.prevent="showEffect"
        @keydown.enter.prevent="showOriginal"
        @keyup.enter.prevent="showEffect"
        @contextmenu.prevent
      >
        按住查看原图
      </button>
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

const viewportRef = ref(null)
const wrapperRef = ref(null)
const canvasRef = ref(null)
const isPanning = ref(false)
let panState = null
let resizeObserver = null

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

const handleWheel = (event) => {
  emit(event.deltaY < 0 ? 'zoom-in' : 'zoom-out')
}

const startPan = (event) => {
  if (event.button !== 1 || !wrapperRef.value) return
  event.preventDefault()
  isPanning.value = true
  panState = {
    pointerId: event.pointerId,
    x: event.clientX,
    y: event.clientY,
    scrollLeft: wrapperRef.value.scrollLeft,
    scrollTop: wrapperRef.value.scrollTop
  }
  wrapperRef.value.setPointerCapture?.(event.pointerId)
}

const movePan = (event) => {
  if (!isPanning.value || !panState || event.pointerId !== panState.pointerId) return
  wrapperRef.value.scrollLeft = panState.scrollLeft - (event.clientX - panState.x)
  wrapperRef.value.scrollTop = panState.scrollTop - (event.clientY - panState.y)
}

const stopPan = (event) => {
  if (!panState || event.pointerId !== panState.pointerId) return
  if (wrapperRef.value?.hasPointerCapture?.(event.pointerId)) {
    wrapperRef.value.releasePointerCapture(event.pointerId)
  }
  isPanning.value = false
  panState = null
}
const showOriginal = (event) => {
  if (event?.pointerId !== undefined) {
    event.currentTarget?.setPointerCapture?.(event.pointerId)
  }
  emit('update:previewMode', 'original')
}

const showEffect = (event) => {
  if (event?.pointerId !== undefined && event.currentTarget?.hasPointerCapture?.(event.pointerId)) {
    event.currentTarget.releasePointerCapture(event.pointerId)
  }
  emit('update:previewMode', 'compare')
}

onMounted(() => {
  resizeObserver = new ResizeObserver(() => emit('resize'))
  if (viewportRef.value) resizeObserver.observe(viewportRef.value)
  window.addEventListener('blur', showEffect)
})

onUnmounted(() => {
  resizeObserver?.disconnect()
  window.removeEventListener('blur', showEffect)
})
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

.canvas-viewport {
  position: relative;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.canvas-wrapper {
  width: 100%;
  height: 100%;
  box-sizing: border-box;
  display: flex;
  align-items: flex-start;
  justify-content: flex-start;
  padding: 14px;
  cursor: default;
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

.canvas-wrapper.panning {
  cursor: grabbing;
  user-select: none;
}

.editor-canvas {
  flex: 0 0 auto;
  margin: auto;
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
  right: 18px;
  bottom: 18px;
  z-index: 2;
  padding: 4px;
  border: 1px solid rgba(229, 233, 238, 0.92);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 12px 26px rgba(17, 24, 39, 0.12);
  backdrop-filter: blur(12px);
}

.compare-switch {
  min-width: 138px;
  min-height: 38px;
  border: 0;
  color: #fff;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
  user-select: none;
  touch-action: none;
  background: linear-gradient(135deg, #13c77f, #08aa68);
}

.compare-switch.active {
  transform: scale(0.98);
  background: linear-gradient(135deg, #0da96b, #078c57);
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
