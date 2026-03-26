<template>
  <el-dialog
    v-model="visible"
    title="裁剪图片"
    width="800px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="crop-container">
      <div class="crop-wrapper">
        <img
          ref="imageRef"
          :src="imageSrc"
          style="display: none;"
        >
      </div>
    </div>

    <div class="crop-options">
      <div class="aspect-ratios">
        <span class="label">比例：</span>
        <el-radio-group
          v-model="aspectRatio"
          @change="changeAspectRatio"
        >
          <el-radio-button value="free">
            自由
          </el-radio-button>
          <el-radio-button value="1:1">
            1:1
          </el-radio-button>
          <el-radio-button value="4:3">
            4:3
          </el-radio-button>
          <el-radio-button value="16:9">
            16:9
          </el-radio-button>
          <el-radio-button value="3:4">
            3:4
          </el-radio-button>
          <el-radio-button value="9:16">
            9:16
          </el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <template #footer>
      <el-button @click="handleClose">
        取消
      </el-button>
      <el-button
        type="primary"
        @click="handleConfirm"
      >
        确认裁剪
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import Cropper from 'cropperjs'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  imageSrc: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue', 'confirm'])

const visible = ref(false)
const imageRef = ref(null)
const aspectRatio = ref('free')
let cropper = null

// 监听弹窗显示
watch(() => props.modelValue, async (val) => {
  visible.value = val
  if (val && props.imageSrc) {
    await nextTick()
    initCropper()
  }
})

watch(visible, (val) => {
  emit('update:modelValue', val)
})

// 初始化裁剪器
const initCropper = () => {
  if (cropper) {
    cropper.destroy()
  }

  if (!imageRef.value) return

  cropper = new Cropper(imageRef.value, {
    viewMode: 1,
    dragMode: 'move',
    autoCropArea: 0.8,
    restore: false,
    guides: true,
    center: true,
    highlight: false,
    cropBoxMovable: true,
    cropBoxResizable: true,
    toggleDragModeOnDblclick: true,
    aspectRatio: getAspectRatio()
  })
}

// 获取比例数值
const getAspectRatio = () => {
  const ratios = {
    'free': NaN,
    '1:1': 1,
    '4:3': 4 / 3,
    '16:9': 16 / 9,
    '3:4': 3 / 4,
    '9:16': 9 / 16
  }
  return ratios[aspectRatio.value]
}

// 改变比例
const changeAspectRatio = () => {
  if (cropper) {
    cropper.setAspectRatio(getAspectRatio())
  }
}

// 确认裁剪
const handleConfirm = () => {
  if (!cropper) return

  const canvas = cropper.getCroppedCanvas({
    maxWidth: 4096,
    maxHeight: 4096,
    imageSmoothingEnabled: true,
    imageSmoothingQuality: 'high'
  })

  canvas.toBlob((blob) => {
    const url = URL.createObjectURL(blob)
    emit('confirm', { url, blob })
    handleClose()
  }, 'image/png')
}

// 关闭弹窗
const handleClose = () => {
  if (cropper) {
    cropper.destroy()
    cropper = null
  }
  visible.value = false
}
</script>

<style scoped>
.crop-container {
  width: 100%;
  height: 450px;
  background: #1a1a1a;
  display: flex;
  align-items: center;
  justify-content: center;
}

.crop-wrapper {
  width: 100%;
  height: 100%;
}

.crop-options {
  margin-top: var(--space-4);
  padding: var(--space-4);
  background: var(--background-soft, #f5f5f5);
  border-radius: var(--radius, 8px);
}

.aspect-ratios {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.aspect-ratios .label {
  font-weight: 600;
  color: var(--foreground-muted, #666);
}

:deep(.el-radio-button__inner) {
  padding: 8px 16px;
}

/* Cropper 样式 */
:deep(.cropper-container) {
  width: 100% !important;
  height: 100% !important;
  direction: ltr;
}

:deep(.cropper-container img) {
  display: block;
  image-orientation: 0deg;
  max-height: none !important;
  max-width: none !important;
  min-height: 0 !important;
  min-width: 0 !important;
  height: 100%;
  width: 100%;
}

:deep(.cropper-wrap-box),
:deep(.cropper-canvas),
:deep(.cropper-drag-box),
:deep(.cropper-crop-box),
:deep(.cropper-modal) {
  bottom: 0;
  left: 0;
  position: absolute;
  right: 0;
  top: 0;
}

:deep(.cropper-wrap-box),
:deep(.cropper-canvas) {
  overflow: hidden;
}

:deep(.cropper-drag-box) {
  background-color: #fff;
  opacity: 0;
}

:deep(.cropper-modal) {
  background-color: rgba(0, 0, 0, 0.5);
}

:deep(.cropper-view-box) {
  display: block;
  height: 100%;
  outline: 2px solid #39c5bb;
  outline-color: var(--primary, #39c5bb);
  overflow: hidden;
  width: 100%;
}

:deep(.cropper-dashed) {
  border: 0 dashed #eee;
  display: block;
  opacity: 0.5;
  position: absolute;
}

:deep(.cropper-dashed.dashed-h) {
  border-bottom-width: 1px;
  border-top-width: 1px;
  height: 33.33333%;
  left: 0;
  top: 33.33333%;
  width: 100%;
}

:deep(.cropper-dashed.dashed-v) {
  border-left-width: 1px;
  border-right-width: 1px;
  height: 100%;
  left: 33.33333%;
  top: 0;
  width: 33.33333%;
}

:deep(.cropper-center) {
  display: block;
  height: 0;
  left: 50%;
  opacity: 0.75;
  position: absolute;
  top: 50%;
  width: 0;
}

:deep(.cropper-center::before),
:deep(.cropper-center::after) {
  background-color: #eee;
  content: " ";
  display: block;
  position: absolute;
}

:deep(.cropper-center::before) {
  height: 1px;
  left: -3px;
  top: 0;
  width: 7px;
}

:deep(.cropper-center::after) {
  height: 7px;
  left: 0;
  top: -3px;
  width: 1px;
}

:deep(.cropper-face),
:deep(.cropper-line,
.cropper-point) {
  display: block;
  height: 100%;
  opacity: 0.1;
  position: absolute;
  width: 100%;
}

:deep(.cropper-face) {
  background-color: #fff;
  left: 0;
  top: 0;
}

:deep(.cropper-line) {
  background-color: #39c5bb;
}

:deep(.cropper-line.line-e) {
  cursor: ew-resize;
  right: -3px;
  top: 0;
  width: 5px;
}

:deep(.cropper-line.line-n) {
  cursor: ns-resize;
  height: 5px;
  left: 0;
  top: -3px;
}

:deep(.cropper-line.line-w) {
  cursor: ew-resize;
  left: -3px;
  top: 0;
  width: 5px;
}

:deep(.cropper-line.line-s) {
  bottom: -3px;
  cursor: ns-resize;
  height: 5px;
  left: 0;
}

:deep(.cropper-point) {
  background-color: #39c5bb;
  height: 5px;
  opacity: 0.75;
  width: 5px;
}

:deep(.cropper-point.point-e) {
  cursor: ew-resize;
  margin-top: -3px;
  right: -3px;
  top: 50%;
}

:deep(.cropper-point.point-n) {
  cursor: ns-resize;
  left: 50%;
  margin-left: -3px;
  top: -3px;
}

:deep(.cropper-point.point-w) {
  cursor: ew-resize;
  left: -3px;
  margin-top: -3px;
  top: 50%;
}

:deep(.cropper-point.point-s) {
  bottom: -3px;
  cursor: s-resize;
  left: 50%;
  margin-left: -3px;
}

:deep(.cropper-point.point-ne) {
  cursor: nesw-resize;
  right: -3px;
  top: -3px;
}

:deep(.cropper-point.point-nw) {
  cursor: nwse-resize;
  left: -3px;
  top: -3px;
}

:deep(.cropper-point.point-sw) {
  bottom: -3px;
  cursor: nesw-resize;
  left: -3px;
}

:deep(.cropper-point.point-se) {
  bottom: -3px;
  cursor: nwse-resize;
  height: 20px;
  opacity: 1;
  right: -3px;
  width: 20px;
}

@media (min-width: 768px) {
  :deep(.cropper-point.point-se) {
    height: 15px;
    width: 15px;
  }
}

@media (min-width: 992px) {
  :deep(.cropper-point.point-se) {
    height: 10px;
    width: 10px;
  }
}

@media (min-width: 1200px) {
  :deep(.cropper-point.point-se) {
    height: 5px;
    opacity: 0.75;
    width: 5px;
  }
}

:deep(.cropper-point.point-se::before) {
  background-color: #39c5bb;
  bottom: -50%;
  content: " ";
  display: block;
  height: 200%;
  opacity: 0;
  position: absolute;
  right: -50%;
  width: 200%;
}

:deep(.cropper-invisible) {
  opacity: 0;
}

:deep(.cropper-bg) {
  background-image: url("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQAQMAAAAlPW0iAAAAA3NCSVQICAjb4U/gAAAABlBMVEXMzMz////TjRV2AAAACXBIWXMAAArrAAAK6wGCiw1aAAAAHHRFWHRTb2Z0d2FyZQBBZG9iZSBGaXJld29ya3MgQ1M26LyyjAAAABFJREFUCJlj+M/AgBVhF/0PAH6/D/HkDxOGAAAAAElFTkSuQmCC");
}

:deep(.cropper-hide) {
  display: block;
  height: 0;
  position: absolute;
  width: 0;
}

:deep(.cropper-hidden) {
  display: none !important;
}

:deep(.cropper-move) {
  cursor: move;
}

:deep(.cropper-crop) {
  cursor: crosshair;
}

:deep(.cropper-disabled .cropper-drag-box),
:deep(.cropper-disabled .cropper-face),
:deep(.cropper-disabled .cropper-line),
:deep(.cropper-disabled .cropper-point) {
  cursor: not-allowed;
}
</style>