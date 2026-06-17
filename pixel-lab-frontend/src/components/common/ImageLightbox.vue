<!--
  【文件路径】src/components/common/ImageLightbox.vue
  【文件功能说明】全屏图片预览/灯箱组件
  - 支持 v-model 控制显示/隐藏
  - 支持缩放、旋转、下载操作
  - 键盘 Escape 关闭，点击遮罩关闭
  - 平滑淡入淡出过渡动画
-->

<template>
  <Teleport to="body">
    <Transition name="lightbox">
      <div
        v-if="modelValue"
        class="lightbox-overlay"
        @click.self="close"
      >
        <!-- 关闭按钮 -->
        <button
          class="lightbox-close"
          @click="close"
          aria-label="关闭预览"
        >
          <el-icon :size="24"><Close /></el-icon>
        </button>

        <!-- 图片容器 -->
        <div class="lightbox-image-wrapper">
          <img
            ref="imgRef"
            :src="url"
            :alt="alt"
            class="lightbox-image"
            :style="imageStyle"
            @load="onImageLoad"
          />
        </div>

        <!-- 底部工具栏 -->
        <div class="lightbox-toolbar" @click.stop>
          <button
            class="toolbar-btn"
            title="缩小"
            @click="zoomOut"
          >
            <el-icon :size="18"><ZoomOut /></el-icon>
          </button>

          <span class="toolbar-zoom-label">{{ zoomPercent }}%</span>

          <button
            class="toolbar-btn"
            title="放大"
            @click="zoomIn"
          >
            <el-icon :size="18"><ZoomIn /></el-icon>
          </button>

          <div class="toolbar-divider" />

          <button
            class="toolbar-btn"
            title="顺时针旋转"
            @click="rotateRight"
          >
            <el-icon :size="18"><RefreshRight /></el-icon>
          </button>

          <div class="toolbar-divider" />

          <button
            class="toolbar-btn"
            title="下载图片"
            @click="download"
          >
            <el-icon :size="18"><Download /></el-icon>
          </button>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { Close, ZoomIn, ZoomOut, RefreshRight, Download } from '@element-plus/icons-vue'

const props = defineProps({
  /** v-model: 控制灯箱的显示与隐藏 */
  modelValue: {
    type: Boolean,
    default: false
  },
  /** 图片地址 */
  url: {
    type: String,
    default: ''
  },
  /** 图片替代文字 */
  alt: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue'])

const imgRef = ref(null)
const zoom = ref(1)
const rotation = ref(0)
const imageLoaded = ref(false)

const ZOOM_STEP = 0.25
const ZOOM_MIN = 0.25
const ZOOM_MAX = 4

const zoomPercent = computed(() => Math.round(zoom.value * 100))

const imageStyle = computed(() => ({
  transform: `scale(${zoom.value}) rotate(${rotation.value}deg)`,
  transition: 'transform 0.25s cubic-bezier(0.4, 0, 0.2, 1)'
}))

function close() {
  emit('update:modelValue', false)
}

function reset() {
  zoom.value = 1
  rotation.value = 0
  imageLoaded.value = false
}

function zoomIn() {
  if (zoom.value < ZOOM_MAX) {
    zoom.value = Math.min(zoom.value + ZOOM_STEP, ZOOM_MAX)
  }
}

function zoomOut() {
  if (zoom.value > ZOOM_MIN) {
    zoom.value = Math.max(zoom.value - ZOOM_STEP, ZOOM_MIN)
  }
}

function rotateRight() {
  rotation.value = (rotation.value + 90) % 360
}

function download() {
  if (!props.url) return
  const link = document.createElement('a')
  link.href = props.url
  link.download = props.url.split('/').pop() || 'image'
  link.target = '_blank'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

function onImageLoad() {
  imageLoaded.value = true
}

function handleKeydown(e) {
  if (!props.modelValue) return
  if (e.key === 'Escape') {
    e.preventDefault()
    close()
  }
}

// 监听显示状态变化
watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      reset()
      document.addEventListener('keydown', handleKeydown)
      document.body.style.overflow = 'hidden'
    } else {
      document.removeEventListener('keydown', handleKeydown)
      document.body.style.overflow = ''
    }
  }
)
</script>

<style scoped>
/* ========== 遮罩层 ========== */
.lightbox-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.78);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  user-select: none;
}

/* ========== 关闭按钮 ========== */
.lightbox-close {
  position: absolute;
  top: var(--space-5);
  right: var(--space-5);
  z-index: 10;
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: var(--radius-full);
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
  cursor: pointer;
  transition: background var(--transition-fast);
}

.lightbox-close:hover {
  background: rgba(255, 255, 255, 0.24);
}

/* ========== 图片容器 ========== */
.lightbox-image-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 90vw;
  height: 90vh;
  overflow: hidden;
}

.lightbox-image {
  max-width: 90vw;
  max-height: 90vh;
  object-fit: contain;
  will-change: transform;
}

/* ========== 底部工具栏 ========== */
.lightbox-toolbar {
  position: absolute;
  bottom: var(--space-5);
  left: 50%;
  transform: translateX(-50%);
  z-index: 10;
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-4);
  background: var(--background-elevated);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  border: 2px solid var(--border);
}

.toolbar-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--foreground);
  cursor: pointer;
  transition: background var(--transition-fast), color var(--transition-fast);
}

.toolbar-btn:hover {
  background: var(--primary-muted);
  color: var(--primary);
}

.toolbar-zoom-label {
  min-width: 48px;
  text-align: center;
  font-size: 13px;
  font-weight: 600;
  color: var(--foreground-muted);
  font-family: var(--font-mono);
}

.toolbar-divider {
  width: 1px;
  height: 20px;
  background: var(--border);
}

/* ========== 过渡动画 ========== */
.lightbox-enter-active,
.lightbox-leave-active {
  transition: opacity 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.lightbox-enter-active .lightbox-image,
.lightbox-leave-active .lightbox-image {
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1),
              opacity 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.lightbox-enter-from,
.lightbox-leave-to {
  opacity: 0;
}

.lightbox-enter-from .lightbox-image {
  opacity: 0;
  transform: scale(0.92);
}

.lightbox-leave-to .lightbox-image {
  opacity: 0;
  transform: scale(0.92);
}
</style>
