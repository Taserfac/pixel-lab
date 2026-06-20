<template>
  <el-dialog
    v-model="visible"
    title="裁剪图片"
    width="min(1080px, 94vw)"
    :close-on-click-modal="false"
    class="crop-dialog"
    @close="handleClose"
  >
    <div class="crop-container">
      <div
        ref="cropWrapperRef"
        class="crop-wrapper"
      >
        <img
          ref="imageRef"
          :src="imageSrc"
          alt="待裁剪图片"
          class="crop-image"
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
const cropWrapperRef = ref(null)
const aspectRatio = ref('free')
let cropper = null
const FIXED_CROP_COVERAGE = 0.55

// 监听弹窗显示
watch(() => props.modelValue, async (val) => {
  visible.value = val
  if (val && props.imageSrc) {
    await nextTick()
    await initCropper()
  }
})

watch(visible, (val) => {
  emit('update:modelValue', val)
})

// 初始化裁剪器
const initCropper = async () => {
  if (cropper) {
    cropper.destroy()
  }

  if (!imageRef.value || !cropWrapperRef.value) return

  if (!imageRef.value.complete) {
    await new Promise((resolve, reject) => {
      imageRef.value.onload = resolve
      imageRef.value.onerror = reject
    })
  }

  await imageRef.value.decode?.().catch(() => {})

  cropper = new Cropper(imageRef.value, {
    container: cropWrapperRef.value,
    template: `
      <cropper-canvas background>
        <cropper-image rotatable scalable skewable translatable></cropper-image>
        <cropper-shade hidden></cropper-shade>
        <cropper-handle action="select" plain></cropper-handle>
        <cropper-selection initial-coverage="0.55" movable resizable>
          <cropper-grid role="grid" bordered covered></cropper-grid>
          <cropper-crosshair centered></cropper-crosshair>
          <cropper-handle action="move" theme-color="rgba(255, 255, 255, 0.35)"></cropper-handle>
          <cropper-handle action="n-resize"></cropper-handle>
          <cropper-handle action="e-resize"></cropper-handle>
          <cropper-handle action="s-resize"></cropper-handle>
          <cropper-handle action="w-resize"></cropper-handle>
          <cropper-handle action="ne-resize"></cropper-handle>
          <cropper-handle action="nw-resize"></cropper-handle>
          <cropper-handle action="se-resize"></cropper-handle>
          <cropper-handle action="sw-resize"></cropper-handle>
        </cropper-selection>
      </cropper-canvas>
    `
  })

  await nextTick()
  requestAnimationFrame(() => {
    changeAspectRatio()
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
  const selection = cropper?.getCropperSelection?.()
  if (!selection) return

  const ratio = getAspectRatio()
  selection.aspectRatio = ratio
  applyFixedCropBox(ratio)
}

const applyFixedCropBox = (ratio) => {
  const selection = cropper?.getCropperSelection?.()
  const cropperCanvas = cropper?.getCropperCanvas?.()
  if (!selection || !cropperCanvas) return

  const canvasWidth = cropperCanvas.offsetWidth
  const canvasHeight = cropperCanvas.offsetHeight
  if (!canvasWidth || !canvasHeight) return

  let nextWidth = canvasWidth * FIXED_CROP_COVERAGE
  let nextHeight = canvasHeight * FIXED_CROP_COVERAGE

  if (Number.isFinite(ratio)) {
    if (nextWidth / nextHeight > ratio) {
      nextWidth = nextHeight * ratio
    } else {
      nextHeight = nextWidth / ratio
    }
  }

  const nextX = (canvasWidth - nextWidth) / 2
  const nextY = (canvasHeight - nextHeight) / 2

  selection.$change(nextX, nextY, nextWidth, nextHeight, ratio, true)
}

// 确认裁剪
const handleConfirm = async () => {
  if (!cropper) return

  const selection = cropper.getCropperSelection?.()
  if (!selection || !selection.width || !selection.height) return

  const maxEdge = 4096
  const scale = Math.min(maxEdge / selection.width, maxEdge / selection.height, 1)
  const canvas = await selection.$toCanvas(scale < 1
    ? {
      width: Math.round(selection.width * scale),
      height: Math.round(selection.height * scale)
    }
    : undefined)

  canvas.toBlob((blob) => {
    if (!blob) return
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
  height: min(62vh, 560px);
  min-height: 360px;
  background: var(--background);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.crop-wrapper {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.crop-image {
  display: block;
  max-width: 100%;
  max-height: 100%;
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
  flex-wrap: wrap;
}

.aspect-ratios .label {
  font-weight: 600;
  color: var(--foreground-muted, #666);
}

:deep(.el-radio-button__inner) {
  padding: 8px 16px;
}

:deep(cropper-canvas) {
  width: 100%;
  height: 100%;
}

:deep(cropper-image) {
  max-width: none;
  max-height: none;
}

:deep(.cropper-view-box) {
  outline: 2px solid #39c5bb;
  outline-color: var(--primary, #39c5bb);
}

:deep(.cropper-face) {
  background-color: var(--primary);
}

:deep(.cropper-line) {
  background-color: var(--primary);
}

:deep(.cropper-point) {
  background-color: var(--primary);
  width: 8px;
  height: 8px;
  opacity: 1;
}

:deep(.cropper-bg) {
  background-image: url("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQAQMAAAAlPW0iAAAAA3NCSVQICAjb4U/gAAAABlBMVEXMzMz////TjRV2AAAACXBIWXMAAArrAAAK6wGCiw1aAAAAHHRFWHRTb2Z0d2FyZQBBZG9iZSBGaXJld29ya3MgQ1M26LyyjAAAABFJREFUCJlj+M/AgBVhF/0PAH6/D/HkDxOGAAAAAElFTkSuQmCC");
}

@media (max-width: 640px) {
  .crop-container {
    height: 52vh;
    min-height: 280px;
  }

  .aspect-ratios {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
