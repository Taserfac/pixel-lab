<template>
  <el-dialog
    v-model="visible"
    title="裁剪图片"
    width="min(920px, 94vw)"
    class="crop-dialog"
    append-to-body
    align-center
    :close-on-click-modal="false"
    :close-on-press-escape="!submitting"
    :show-close="!submitting"
    @opened="handleOpened"
    @closed="handleClosed"
  >
    <div ref="stageRef" class="crop-stage">
      <img
        ref="imageRef"
        :src="imageSrc"
        crossorigin="anonymous"
        alt="待裁剪图片"
        class="crop-image"
        @load="handleImageLoad"
      >
      <div
        v-if="selectionReady"
        class="crop-selection"
        :style="selectionStyle"
        @pointerdown="startSelection($event, 'move')"
      >
        <span
          v-for="handle in resizeHandles"
          :key="handle"
          class="crop-handle"
          :class="`crop-handle--${handle}`"
          @pointerdown.stop="startSelection($event, handle)"
        />
      </div>
    </div>

    <div class="crop-options">
      <span class="crop-options__label">比例</span>
      <el-radio-group v-model="aspectRatio" @change="changeAspectRatio">
        <el-radio-button value="free">自由</el-radio-button>
        <el-radio-button value="1:1">1:1</el-radio-button>
        <el-radio-button value="4:3">4:3</el-radio-button>
        <el-radio-button value="16:9">16:9</el-radio-button>
        <el-radio-button value="3:4">3:4</el-radio-button>
        <el-radio-button value="9:16">9:16</el-radio-button>
      </el-radio-group>
    </div>

    <template #footer>
      <el-button :disabled="submitting" @click="visible = false">取消</el-button>
      <el-button
        type="primary"
        :loading="submitting"
        :disabled="!selectionReady"
        @click="handleConfirm"
      >
        确认裁剪
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  imageSrc: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue', 'confirm'])

const visible = ref(false)
const stageRef = ref(null)
const imageRef = ref(null)
const aspectRatio = ref('free')
const selectionReady = ref(false)
const submitting = ref(false)
const imageBounds = reactive({ x: 0, y: 0, width: 0, height: 0 })
const selection = reactive({ x: 0, y: 0, width: 0, height: 0 })
const resizeHandles = ['nw', 'ne', 'sw', 'se']
let dragState = null
let resizeObserver = null

const selectionStyle = computed(() => ({
  left: `${selection.x}px`,
  top: `${selection.y}px`,
  width: `${selection.width}px`,
  height: `${selection.height}px`
}))

watch(() => props.modelValue, val => {
  visible.value = val
})

watch(visible, val => emit('update:modelValue', val))

const getAspectRatio = () => ({
  free: NaN,
  '1:1': 1,
  '4:3': 4 / 3,
  '16:9': 16 / 9,
  '3:4': 3 / 4,
  '9:16': 9 / 16
})[aspectRatio.value]

const updateImageBounds = () => {
  if (!stageRef.value || !imageRef.value) return false
  const stageRect = stageRef.value.getBoundingClientRect()
  const imageRect = imageRef.value.getBoundingClientRect()
  imageBounds.x = imageRect.left - stageRect.left
  imageBounds.y = imageRect.top - stageRect.top
  imageBounds.width = imageRect.width
  imageBounds.height = imageRect.height
  return imageBounds.width > 0 && imageBounds.height > 0
}

const resetSelection = () => {
  if (!updateImageBounds()) return
  const ratio = getAspectRatio()
  let width = imageBounds.width * 0.72
  let height = imageBounds.height * 0.72

  if (Number.isFinite(ratio)) {
    if (width / height > ratio) width = height * ratio
    else height = width / ratio
  }

  selection.x = imageBounds.x + (imageBounds.width - width) / 2
  selection.y = imageBounds.y + (imageBounds.height - height) / 2
  selection.width = width
  selection.height = height
  selectionReady.value = true
}

const handleImageLoad = () => {
  nextTick(() => requestAnimationFrame(resetSelection))
}

const handleOpened = () => {
  resizeObserver = new ResizeObserver(() => {
    if (selectionReady.value) resetSelection()
  })
  if (stageRef.value) resizeObserver.observe(stageRef.value)
  if (imageRef.value?.complete && imageRef.value.naturalWidth) handleImageLoad()
}

const changeAspectRatio = () => resetSelection()

const clamp = (value, min, max) => Math.min(Math.max(value, min), max)

const startSelection = (event, action) => {
  if (event.button !== 0 || !selectionReady.value || !stageRef.value) return
  event.preventDefault()
  dragState = {
    action,
    pointerId: event.pointerId,
    startX: event.clientX,
    startY: event.clientY,
    selection: { ...selection }
  }
  window.addEventListener('pointermove', moveSelection)
  window.addEventListener('pointerup', stopSelection, { once: true })
  window.addEventListener('pointercancel', stopSelection, { once: true })
}

const moveSelection = (event) => {
  if (!dragState || event.pointerId !== dragState.pointerId) return
  const dx = event.clientX - dragState.startX
  const dy = event.clientY - dragState.startY
  const start = dragState.selection
  const right = imageBounds.x + imageBounds.width
  const bottom = imageBounds.y + imageBounds.height

  if (dragState.action === 'move') {
    selection.x = clamp(start.x + dx, imageBounds.x, right - start.width)
    selection.y = clamp(start.y + dy, imageBounds.y, bottom - start.height)
    return
  }

  const west = dragState.action.includes('w')
  const north = dragState.action.includes('n')
  const anchorX = west ? start.x + start.width : start.x
  const anchorY = north ? start.y + start.height : start.y
  const pointerX = clamp(start.x + (west ? 0 : start.width) + dx, imageBounds.x, right)
  const pointerY = clamp(start.y + (north ? 0 : start.height) + dy, imageBounds.y, bottom)
  const maxWidth = west ? anchorX - imageBounds.x : right - anchorX
  const maxHeight = north ? anchorY - imageBounds.y : bottom - anchorY
  let width = clamp(Math.abs(pointerX - anchorX), 32, maxWidth)
  let height = clamp(Math.abs(pointerY - anchorY), 32, maxHeight)
  const ratio = getAspectRatio()

  if (Number.isFinite(ratio)) {
    width = Math.min(Math.max(width, height * ratio), maxWidth, maxHeight * ratio)
    height = width / ratio
  }

  selection.width = width
  selection.height = height
  selection.x = west ? anchorX - width : anchorX
  selection.y = north ? anchorY - height : anchorY
}

const stopSelection = () => {
  dragState = null
  window.removeEventListener('pointermove', moveSelection)
  window.removeEventListener('pointerup', stopSelection)
  window.removeEventListener('pointercancel', stopSelection)
}

const canvasToBlob = canvas => new Promise((resolve, reject) => {
  canvas.toBlob(blob => blob ? resolve(blob) : reject(new Error('empty crop result')), 'image/png')
})

const handleConfirm = async () => {
  if (submitting.value || !selectionReady.value || !imageRef.value) return
  submitting.value = true

  try {
    const sourceWidth = imageRef.value.naturalWidth
    const sourceHeight = imageRef.value.naturalHeight
    if (!sourceWidth || !sourceHeight || !imageBounds.width || !imageBounds.height) {
      throw new Error('image is not ready')
    }

    const scaleX = sourceWidth / imageBounds.width
    const scaleY = sourceHeight / imageBounds.height
    const sourceX = Math.max(0, (selection.x - imageBounds.x) * scaleX)
    const sourceY = Math.max(0, (selection.y - imageBounds.y) * scaleY)
    const cropWidth = Math.min(selection.width * scaleX, sourceWidth - sourceX)
    const cropHeight = Math.min(selection.height * scaleY, sourceHeight - sourceY)
    const outputScale = Math.min(4096 / cropWidth, 4096 / cropHeight, 1)
    const canvas = document.createElement('canvas')
    canvas.width = Math.max(1, Math.round(cropWidth * outputScale))
    canvas.height = Math.max(1, Math.round(cropHeight * outputScale))
    const context = canvas.getContext('2d')
    context.imageSmoothingEnabled = true
    context.imageSmoothingQuality = 'high'
    context.drawImage(
      imageRef.value,
      sourceX, sourceY, cropWidth, cropHeight,
      0, 0, canvas.width, canvas.height
    )

    const blob = await canvasToBlob(canvas)
    const url = URL.createObjectURL(blob)
    emit('confirm', { url, blob })
    visible.value = false
  } catch (error) {
    console.error('Failed to crop image:', error)
    ElMessage.error('裁剪失败，请重新载入图片后再试')
  } finally {
    submitting.value = false
  }
}

const handleClosed = () => {
  resizeObserver?.disconnect()
  resizeObserver = null
  stopSelection()
  selectionReady.value = false
  submitting.value = false
}

onBeforeUnmount(handleClosed)
</script>

<style scoped>
.crop-stage {
  position: relative;
  min-height: 240px;
  height: min(52vh, 500px);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: 16px;
  background:
    linear-gradient(45deg, #d9dde2 25%, transparent 25%),
    linear-gradient(-45deg, #d9dde2 25%, transparent 25%),
    linear-gradient(45deg, transparent 75%, #d9dde2 75%),
    linear-gradient(-45deg, transparent 75%, #d9dde2 75%);
  background-color: #eef1f4;
  background-position: 0 0, 0 10px, 10px -10px, -10px 0;
  background-size: 20px 20px;
  touch-action: none;
}

.crop-image {
  display: block;
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  pointer-events: none;
  user-select: none;
}

.crop-selection {
  position: absolute;
  box-sizing: border-box;
  border: 2px solid #1da1ff;
  box-shadow: 0 0 0 9999px rgba(0, 0, 0, 0.42);
  cursor: move;
}

.crop-selection::before,
.crop-selection::after {
  content: '';
  position: absolute;
  pointer-events: none;
}

.crop-selection::before {
  inset: 33.333% 0;
  border-top: 1px dashed rgba(255, 255, 255, 0.7);
  border-bottom: 1px dashed rgba(255, 255, 255, 0.7);
}

.crop-selection::after {
  inset: 0 33.333%;
  border-left: 1px dashed rgba(255, 255, 255, 0.7);
  border-right: 1px dashed rgba(255, 255, 255, 0.7);
}

.crop-handle {
  position: absolute;
  width: 12px;
  height: 12px;
  border: 2px solid #fff;
  border-radius: 3px;
  background: #1da1ff;
}

.crop-handle--nw { left: -7px; top: -7px; cursor: nwse-resize; }
.crop-handle--ne { right: -7px; top: -7px; cursor: nesw-resize; }
.crop-handle--sw { left: -7px; bottom: -7px; cursor: nesw-resize; }
.crop-handle--se { right: -7px; bottom: -7px; cursor: nwse-resize; }

.crop-options {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 14px;
  flex-wrap: wrap;
}

.crop-options__label {
  color: var(--foreground-muted);
  font-weight: 700;
}

:global(.crop-dialog.el-dialog) {
  max-height: calc(100vh - 48px);
  margin: 0 !important;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

:global(.crop-dialog .el-dialog__header),
:global(.crop-dialog .el-dialog__footer) {
  flex: 0 0 auto;
}

:global(.crop-dialog .el-dialog__body) {
  min-height: 0;
  overflow: hidden;
  padding-top: 10px;
  padding-bottom: 10px;
}

:global(.crop-dialog .el-dialog__footer) {
  padding-top: 10px;
}

@media (max-height: 700px) {
  .crop-stage {
    min-height: 140px;
    height: 36vh;
  }

  .crop-options {
    margin-top: 10px;
  }
}

@media (max-width: 640px) {
  .crop-stage {
    min-height: 220px;
    height: 44vh;
  }

  .crop-options {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>