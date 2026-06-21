<template>
  <div class="workbench-page">
    <!-- 无图片时的引导 -->
    <ImportArea
      v-if="!currentImage"
      @select="showImageSelector"
      @upload="handleLocalUpload"
    />

    <!-- 编辑区域 -->
    <template v-else>
      <div class="editor-container">
        <div class="main-content">
          <ToolBar
            :filters="filters"
            :current-filter="currentFilter"
            :adjustments="adjustments"
            :pixel-size="pixelSize"
            :color-count="colorCount"
            :thumbnail-url="thumbnailUrl"
            :selected-text-layer="selectedTextLayer"
            @update-selected-text="updateSelectedTextLayer"
            @commit-selected-text="commitSelectedTextLayer"
            @apply-filter="applyFilter"
            @update-adjustment="updateAdjustment"
            @rotate="rotateImage"
            @flip="flipImage"
            @crop="openCropDialog"
            @apply-pixel="applyPixelate"
            @reset-pixel="resetPixelate"
            @add-text="addTextToCanvas"
            @add-watermark="addWatermarkToCanvas"
            @update:pixel-size="pixelSize = $event"
            @update:color-count="colorCount = $event"
          />

          <!-- 中间画布区域 -->
          <CanvasArea
            ref="canvasAreaRef"
            :canvas-style="canvasStyle"
            :zoom="viewZoom"
            v-model:preview-mode="previewMode"
            :image-name="currentImage?.name || ''"
            :canvas-size="currentCanvasSize"
            :text-layers="textLayers"
            :selected-text-id="selectedTextId"
            @select-text="selectedTextId = $event"
            @move-text="moveTextLayer"
            @commit-text-move="commitTextLayerMove"
            @delete-text="deleteTextLayer"
            @resize="fitCanvasToView"
            @zoom-in="zoomIn"
            @zoom-out="zoomOut"
            @fit="fitCanvasToView"
          />
        </div>

        <!-- 底部操作栏 -->
        <ActionBar
          :can-undo="canUndo"
          @back="goBack"
          :can-redo="canRedo"
          @undo="undo"
          @redo="redo"
          @reset="resetImage"
          @fit="fitCanvasToView"
          @change-image="showImageSelector"
          @draft="handleSaveDraft"
          @save="saveToGallery"
          @publish="openPublishDialog"
          @download="downloadImage"
        />
      </div>
    </template>

    <!-- 图片选择对话框 -->
    <ImageSelector
      v-model:visible="imageSelectorVisible"
      :images="myImages"
      :loading="loadingImages"
      @confirm="confirmSelectImage"
    />

    <!-- 裁剪对话框 -->
    <CropDialog
      v-model="cropDialogVisible"
      :image-src="originalImage?.src || ''"
      @confirm="handleCropConfirm"
    />

    <!-- 导出格式对话框 -->
    <el-dialog
      v-model="exportDialogVisible"
      title="导出设置"
      width="400px"
    >
      <div class="export-options">
        <div class="export-format-group">
          <label>导出格式</label>
          <el-radio-group v-model="exportFormat">
            <el-radio-button value="png">PNG</el-radio-button>
            <el-radio-button value="jpeg">JPG</el-radio-button>
            <el-radio-button value="webp">WebP</el-radio-button>
          </el-radio-group>
        </div>
        <div
          v-if="exportFormat !== 'png'"
          class="export-quality"
        >
          <label>质量 ({{ exportQuality }}%)</label>
          <el-slider
            v-model="exportQuality"
            :min="10"
            :max="100"
            :step="5"
          />
        </div>
      </div>
      <template #footer>
        <el-button @click="exportDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          @click="confirmExport"
        >
          下载
        </el-button>
      </template>
    </el-dialog>

    <!-- 发布到社区信息弹窗 -->
    <el-dialog
      v-model="publishDialogVisible"
      :title="artworkDialogMode === 'gallery' ? '保存到个人中心' : '发布到社区'"
      width="min(520px, 92vw)"
      class="publish-dialog"
      :close-on-click-modal="!publishing"
      :close-on-press-escape="!publishing"
    >
      <el-form
        label-position="top"
        class="publish-form"
      >
        <el-form-item label="标题">
          <el-input
            v-model="publishForm.title"
            maxlength="100"
            show-word-limit
            placeholder="给作品起一个标题"
          />
        </el-form-item>
        <el-form-item label="标签">
          <el-select
            v-model="publishForm.tags"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="输入标签后按回车添加"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="说明">
          <el-input
            v-model="publishForm.description"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="写下这张作品的创作想法、处理方式或灵感来源"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button
          :disabled="publishing"
          @click="publishDialogVisible = false"
        >
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="publishing"
          :disabled="!publishForm.title.trim()"
          @click="submitArtwork"
        >
          {{ artworkDialogMode === 'gallery' ? '确认保存' : '确认发布' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, nextTick, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { onBeforeRouteLeave, useRouter } from 'vue-router'
import ImportArea from './components/ImportArea.vue'
import ToolBar from './components/ToolBar.vue'
import CanvasArea from './components/CanvasArea.vue'
import ActionBar from './components/ActionBar.vue'
import ImageSelector from './components/ImageSelector.vue'
import CropDialog from './components/CropDialog.vue'
import { getUserImages, uploadImage, updateImageVisibility, updateImageMetadata } from '@/api/image'
import { useHistory } from './composables/useHistory'

const emit = defineEmits(['workbench-editing-change'])
const router = useRouter()
const DRAFT_STORAGE_KEY = 'pixel-lab:image-workbench-draft:v1'
const hasUnsavedChanges = ref(false)

const goBack = () => {
  if (router.options.history.state.back) router.back()
  else router.push('/dashboard')
}

// ========== 组件 refs ==========
const canvasAreaRef = ref(null)

// ========== 状态定义 ==========
const currentImage = ref(null)
watch(currentImage, image => emit('workbench-editing-change', Boolean(image)), { immediate: true })
const originalImage = ref(null)
const thumbnailUrl = ref('')
const viewZoom = ref(1)
const currentCanvasSize = reactive({ width: 0, height: 0 })
const baseImageDataUrl = ref('')
const previewMode = ref('compare')
const textLayers = ref([])
const selectedTextId = ref(null)
const selectedTextLayer = computed(() => {
  const layer = textLayers.value.find(item => item.id === selectedTextId.value)
  if (!layer) return null
  const canvas = canvasAreaRef.value?.canvas
  const minEdge = Math.min(canvas?.width || 1, canvas?.height || 1)
  return {
    ...layer,
    fontScale: layer.fontScale || (layer.fontSize / minEdge * 100)
  }
})

// 图片选择器
const imageSelectorVisible = ref(false)
const loadingImages = ref(false)
const myImages = ref([])

// 裁剪对话框
const cropDialogVisible = ref(false)

// 导出格式
const exportDialogVisible = ref(false)
const exportFormat = ref('png')
const exportQuality = ref(90)
const publishDialogVisible = ref(false)
const publishing = ref(false)
const artworkDialogMode = ref('community')
const publishForm = reactive({
  title: '',
  tags: [],
  description: ''
})

// 滤镜选项
const filters = [
  { label: '原图', value: 'none' },
  { label: '黑白', value: 'grayscale' },
  { label: '复古', value: 'sepia' },
  { label: '冷色', value: 'cool' },
  { label: '暖色', value: 'warm' },
  { label: '胶片', value: 'film' },
  { label: '反转', value: 'invert' },
  { label: '锐化', value: 'sharpen' }
]
const currentFilter = ref('none')

// 参数调整
const adjustments = reactive({
  brightness: 0,
  contrast: 0,
  saturate: 0,
  sharpen: 0
})

// 像素画参数
const pixelSize = ref(8)
const colorCount = ref(32)

// 历史记录
const { canUndo, canRedo, saveHistory, undo: undoHistory, redo: redoHistory } = useHistory()

// Canvas 样式
const canvasStyle = computed(() => {
  let filter = ''

  switch (currentFilter.value) {
  case 'grayscale':
    filter += 'grayscale(100%) '
    break
  case 'sepia':
    filter += 'sepia(100%) '
    break
  case 'invert':
    filter += 'invert(100%) '
    break
  case 'blur':
    filter += 'blur(3px) '
    break
  case 'sharpen':
    filter += 'contrast(118%) saturate(112%) '
    break
  case 'warm':
    filter += 'sepia(30%) saturate(140%) '
    break
  case 'cool':
    filter += 'saturate(80%) hue-rotate(20deg) '
    break
  case 'film':
    filter += 'contrast(108%) saturate(92%) sepia(18%) '
    break
  }

  const brightness = 100 + adjustments.brightness
  const contrast = 100 + adjustments.contrast + adjustments.sharpen * 0.25
  const saturate = 100 + adjustments.saturate

  filter += `brightness(${brightness}%) contrast(${contrast}%) saturate(${saturate}%)`

  return { filter }
})

// ========== 图片导入 ==========
const showImageSelector = async () => {
  imageSelectorVisible.value = true
  loadingImages.value = true
  try {
    const res = await getUserImages()
    myImages.value = res.list || []
  } catch (error) {
    console.error('获取图片列表失败:', error)
  } finally {
    loadingImages.value = false
  }
}

const confirmSelectImage = async (image) => {
  await loadImage(image.url, image.original_name)
}

const handleLocalUpload = async ({ url, name }) => {
  await loadImage(url, name)
}

const loadImage = async (url, name) => {
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.crossOrigin = 'anonymous'
    img.onload = () => {
      originalImage.value = img
      currentImage.value = { url, name }
      thumbnailUrl.value = url
      baseImageDataUrl.value = url
      textLayers.value = []
      selectedTextId.value = null

      nextTick(() => {
        initCanvas()
        resetAdjustments()
        saveHistory({ imageData: url, filter: 'none', adjustments: { ...adjustments } })
      })

      resolve()
    }
    img.onerror = () => {
      ElMessage.error('图片加载失败')
      reject()
    }
    img.src = url
  })
}

// ========== 裁剪 ==========
const openCropDialog = () => {
  if (!originalImage.value) {
    ElMessage.warning('请先选择或上传图片')
    return
  }
  cropDialogVisible.value = true
}

const handleCropConfirm = async ({ url, blob }) => {
  // 用裁剪后的图片替换当前图片
  await loadImage(url, `cropped_${currentImage.value?.name || 'image.png'}`)
  hasUnsavedChanges.value = true
  ElMessage.success('裁剪成功')
}

// ========== 画布操作 ==========
const initCanvas = () => {
  const canvas = canvasAreaRef.value?.canvas
  if (!canvas || !originalImage.value) return

  const maxEdge = 2400

  let width = originalImage.value.width
  let height = originalImage.value.height

  const ratio = Math.min(maxEdge / width, maxEdge / height, 1)

  if (ratio < 1) {
    width = Math.round(width * ratio)
    height = Math.round(height * ratio)
  }

  width = Math.max(Math.round(width), 1)
  height = Math.max(Math.round(height), 1)

  canvas.width = width
  canvas.height = height

  drawImage()
  updateCanvasSize()
  nextTick(fitCanvasToView)
}

const drawImage = () => {
  const canvas = canvasAreaRef.value?.canvas
  if (!canvas || !originalImage.value) return

  const ctx = canvas.getContext('2d')
  ctx.clearRect(0, 0, canvas.width, canvas.height)
  ctx.drawImage(
    originalImage.value,
    0, 0,
    originalImage.value.width,
    originalImage.value.height,
    0, 0,
    canvas.width,
    canvas.height
  )
  updateCanvasSize()
}

const updateCanvasSize = () => {
  const canvas = canvasAreaRef.value?.canvas
  currentCanvasSize.width = canvas?.width || 0
  currentCanvasSize.height = canvas?.height || 0
}

const clamp = (value, min, max) => Math.min(Math.max(value, min), max)

const fitCanvasToView = () => {
  const canvas = canvasAreaRef.value?.canvas
  const wrapper = canvasAreaRef.value?.wrapper
  if (!canvas || !wrapper || !canvas.width || !canvas.height) return

  const wrapperStyle = window.getComputedStyle(wrapper)
  const horizontalPadding = parseFloat(wrapperStyle.paddingLeft) + parseFloat(wrapperStyle.paddingRight)
  const verticalPadding = parseFloat(wrapperStyle.paddingTop) + parseFloat(wrapperStyle.paddingBottom)
  const maxWidth = Math.max(wrapper.clientWidth - horizontalPadding - 2, 80)
  const maxHeight = Math.max(wrapper.clientHeight - verticalPadding - 2, 80)
  const ratio = Math.min(maxWidth / canvas.width, maxHeight / canvas.height)
  const fittedRatio = Math.floor(ratio * 100) / 100
  viewZoom.value = clamp(fittedRatio, 0.01, 2)
}

const zoomIn = () => {
  viewZoom.value = clamp(Number((viewZoom.value + 0.1).toFixed(2)), 0.08, 3)
}

const zoomOut = () => {
  viewZoom.value = clamp(Number((viewZoom.value - 0.1).toFixed(2)), 0.08, 3)
}

const buildRenderedCanvas = () => {
  const canvas = canvasAreaRef.value?.canvas
  if (!canvas) return null

  const rendered = document.createElement('canvas')
  rendered.width = canvas.width
  rendered.height = canvas.height

  const ctx = rendered.getContext('2d')
  ctx.filter = canvasStyle.value.filter || 'none'
  ctx.drawImage(canvas, 0, 0)
  ctx.filter = 'none'
  textLayers.value.forEach(layer => drawTextLayer(ctx, layer))

  return rendered
}

// ========== 滤镜与调整 ==========
const applyFilter = (filter) => {
  currentFilter.value = filter
  drawImage()
  saveToHistory()
}

const updateAdjustment = ({ key, value }) => {
  adjustments[key] = value
  drawImage()
  hasUnsavedChanges.value = true
}

const resetAdjustments = () => {
  adjustments.brightness = 0
  adjustments.contrast = 0
  adjustments.saturate = 0
  adjustments.sharpen = 0
  currentFilter.value = 'none'
}

const saveToHistory = () => {
  const canvas = canvasAreaRef.value?.canvas
  if (!canvas) return
  saveHistory({
    imageData: canvas.toDataURL('image/png'),
    filter: currentFilter.value,
    adjustments: { ...adjustments },
    textLayers: textLayers.value.map(layer => ({ ...layer }))
  })
  hasUnsavedChanges.value = true
}

// ========== 几何变换 ==========
const rotateImage = (degree) => {
  const canvas = canvasAreaRef.value?.canvas
  if (!canvas || !originalImage.value) return

  const tempCanvas = document.createElement('canvas')
  const tempCtx = tempCanvas.getContext('2d')

  tempCanvas.width = canvas.width
  tempCanvas.height = canvas.height
  tempCtx.drawImage(canvas, 0, 0)

  const radian = degree * Math.PI / 180
  const isVertical = degree % 180 !== 0

  const newWidth = isVertical ? canvas.height : canvas.width
  const newHeight = isVertical ? canvas.width : canvas.height

  canvas.width = newWidth
  canvas.height = newHeight

  const ctx = canvas.getContext('2d')
  ctx.clearRect(0, 0, newWidth, newHeight)
  ctx.save()
  ctx.translate(newWidth / 2, newHeight / 2)
  ctx.rotate(radian)
  ctx.drawImage(tempCanvas, -tempCanvas.width / 2, -tempCanvas.height / 2)
  ctx.restore()

  updateOriginalFromCanvas()
  updateCanvasSize()
  nextTick(fitCanvasToView)
  saveToHistory()
}

const flipImage = (direction) => {
  const canvas = canvasAreaRef.value?.canvas
  if (!canvas || !originalImage.value) return

  const tempCanvas = document.createElement('canvas')
  const tempCtx = tempCanvas.getContext('2d')

  tempCanvas.width = canvas.width
  tempCanvas.height = canvas.height
  tempCtx.drawImage(canvas, 0, 0)

  const ctx = canvas.getContext('2d')
  ctx.clearRect(0, 0, canvas.width, canvas.height)
  ctx.save()

  if (direction === 'horizontal') {
    ctx.translate(canvas.width, 0)
    ctx.scale(-1, 1)
  } else {
    ctx.translate(0, canvas.height)
    ctx.scale(1, -1)
  }

  ctx.drawImage(tempCanvas, 0, 0)
  ctx.restore()

  updateOriginalFromCanvas()
  updateCanvasSize()
  saveToHistory()
}

const updateOriginalFromCanvas = () => {
  const canvas = canvasAreaRef.value?.canvas
  if (!canvas) return
  const img = new Image()
  img.src = canvas.toDataURL('image/png')
  img.onload = () => {
    originalImage.value = img
  }
}

// ========== 像素画功能 ==========
const drawTextLayer = (ctx, layer) => {
  ctx.save()
  ctx.font = `700 ${layer.fontSize}px system-ui, sans-serif`
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.globalAlpha = layer.opacity
  ctx.lineJoin = 'round'
  ctx.lineWidth = Math.max(2, layer.fontSize * 0.09)
  ctx.strokeStyle = layer.strokeColor || '#000000'
  ctx.fillStyle = layer.color || '#ffffff'
  if (layer.strokeEnabled) ctx.strokeText(layer.text, layer.x, layer.y)
  ctx.fillText(layer.text, layer.x, layer.y)
  ctx.restore()
}

const getTextLayerPosition = (layer, position) => {
  const canvas = canvasAreaRef.value?.canvas
  if (!canvas) return { x: layer.x || 0, y: layer.y || 0 }

  const ctx = canvas.getContext('2d')
  const margin = Math.max(12, Math.round(Math.min(canvas.width, canvas.height) * 0.04))
  ctx.save()
  ctx.font = `700 ${layer.fontSize}px system-ui, sans-serif`
  const textWidth = ctx.measureText(layer.text).width
  ctx.restore()

  const halfWidth = Math.min(textWidth / 2, Math.max(canvas.width / 2 - margin, 0))
  const halfHeight = Math.min(layer.fontSize / 2, Math.max(canvas.height / 2 - margin, 0))
  const horizontal = position[1]
  const vertical = position[0]
  return {
    x: horizontal === 'l' ? margin + halfWidth : horizontal === 'r' ? canvas.width - margin - halfWidth : canvas.width / 2,
    y: vertical === 't' ? margin + halfHeight : vertical === 'b' ? canvas.height - margin - halfHeight : canvas.height / 2
  }
}

const addTextLayer = ({ text, opacity = 100, position = 'mc', watermark = false, fontScale = 10, color = '#ffffff', strokeEnabled = false, strokeColor = '#000000' }) => {
  const canvas = canvasAreaRef.value?.canvas
  if (!canvas || !text) return

  const minEdge = Math.min(canvas.width, canvas.height)
  const normalizedScale = clamp(Number(fontScale) || (watermark ? 5 : 10), 2, 30)
  const layer = {
    id: `text-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
    text,
    x: canvas.width / 2,
    y: canvas.height / 2,
    fontSize: Math.max(12, Math.round(minEdge * normalizedScale / 100)),
    fontScale: normalizedScale,
    color: color || '#ffffff',
    strokeEnabled: Boolean(strokeEnabled),
    strokeColor: strokeColor || '#000000',
    opacity: Math.max(0.1, Math.min(opacity / 100, 1)),
    position,
    watermark
  }
  Object.assign(layer, getTextLayerPosition(layer, position))
  textLayers.value.push(layer)
  selectedTextId.value = layer.id
  saveToHistory()
  ElMessage.success(watermark ? '水印已添加，可继续调整样式' : '文字已添加，可继续调整样式')
}

const addTextToCanvas = payload => addTextLayer({ ...payload, opacity: 100 })
const addWatermarkToCanvas = payload => addTextLayer({ ...payload, watermark: true })

const updateSelectedTextLayer = (patch) => {
  const layer = textLayers.value.find(item => item.id === selectedTextId.value)
  const canvas = canvasAreaRef.value?.canvas
  if (!layer || !canvas) return

  if (patch.fontScale !== undefined) {
    layer.fontScale = clamp(Number(patch.fontScale), 2, 30)
    layer.fontSize = Math.max(12, Math.round(Math.min(canvas.width, canvas.height) * layer.fontScale / 100))
  }
  if (patch.color !== undefined) layer.color = patch.color || '#ffffff'
  if (patch.strokeEnabled !== undefined) layer.strokeEnabled = Boolean(patch.strokeEnabled)
  if (patch.strokeColor !== undefined) layer.strokeColor = patch.strokeColor || '#000000'
  if (patch.opacity !== undefined && layer.watermark) layer.opacity = clamp(Number(patch.opacity), 0.1, 1)
  if (patch.position !== undefined) layer.position = patch.position

  if (layer.position) Object.assign(layer, getTextLayerPosition(layer, layer.position))
  layer.x = clamp(layer.x, 0, canvas.width)
  layer.y = clamp(layer.y, 0, canvas.height)
  hasUnsavedChanges.value = true
}

const commitSelectedTextLayer = () => {
  if (!selectedTextId.value) return
  saveToHistory()
}

const moveTextLayer = ({ id, x, y }) => {
  const layer = textLayers.value.find(item => item.id === id)
  const canvas = canvasAreaRef.value?.canvas
  if (!layer || !canvas) return
  layer.x = clamp(x, 0, canvas.width)
  layer.y = clamp(y, 0, canvas.height)
  layer.position = null
  hasUnsavedChanges.value = true
}

const commitTextLayerMove = (id) => {
  if (!textLayers.value.some(layer => layer.id === id)) return
  saveToHistory()
}

const deleteTextLayer = (id = selectedTextId.value) => {
  const index = textLayers.value.findIndex(layer => layer.id === id)
  if (index < 0) return
  textLayers.value.splice(index, 1)
  selectedTextId.value = null
  saveToHistory()
  ElMessage.success('文字已删除')
}

const handleTextLayerKeydown = (event) => {
  const target = event.target
  if (['INPUT', 'TEXTAREA'].includes(target?.tagName) || target?.isContentEditable) return
  if ((event.key === 'Delete' || event.key === 'Backspace') && selectedTextId.value) {
    event.preventDefault()
    deleteTextLayer()
  }
}

const applyPixelate = () => {
  const canvas = canvasAreaRef.value?.canvas
  const renderedCanvas = buildRenderedCanvas()
  if (!canvas || !renderedCanvas || !originalImage.value) return

  const size = Math.max(2, Math.round(pixelSize.value))
  const scaledWidth = Math.max(1, Math.ceil(canvas.width / size))
  const scaledHeight = Math.max(1, Math.ceil(canvas.height / size))
  const pixelCanvas = document.createElement('canvas')
  pixelCanvas.width = scaledWidth
  pixelCanvas.height = scaledHeight

  const pixelCtx = pixelCanvas.getContext('2d')
  pixelCtx.imageSmoothingEnabled = true
  pixelCtx.imageSmoothingQuality = 'low'
  pixelCtx.drawImage(renderedCanvas, 0, 0, scaledWidth, scaledHeight)

  quantizeCanvasColors(pixelCanvas, colorCount.value)

  const ctx = canvas.getContext('2d')
  ctx.clearRect(0, 0, canvas.width, canvas.height)
  ctx.imageSmoothingEnabled = false
  ctx.drawImage(pixelCanvas, 0, 0, canvas.width, canvas.height)
  ctx.imageSmoothingEnabled = true
  textLayers.value = []
  selectedTextId.value = null

  resetAdjustments()
  updateOriginalFromCanvas()
  saveToHistory()
  ElMessage.success('已应用像素画效果')
}

const quantizeCanvasColors = (canvas, colors) => {
  const ctx = canvas.getContext('2d')
  const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height)
  const data = imageData.data
  const maxColors = Math.min(Math.max(2, Math.round(colors)), 256)
  const opaquePixels = []

  for (let i = 0; i < data.length; i += 4) {
    if (data[i + 3] < 8) continue
    opaquePixels.push([data[i], data[i + 1], data[i + 2], data[i + 3]])
  }

  if (opaquePixels.length === 0) return

  const palette = buildMedianCutPalette(opaquePixels, maxColors)

  for (let i = 0; i < data.length; i += 4) {
    if (data[i + 3] < 8) {
      data[i + 3] = 0
      continue
    }

    const nearest = findNearestColor([data[i], data[i + 1], data[i + 2], data[i + 3]], palette)
    data[i] = nearest[0]
    data[i + 1] = nearest[1]
    data[i + 2] = nearest[2]
    data[i + 3] = nearest[3]
  }

  ctx.putImageData(imageData, 0, 0)
}

const buildMedianCutPalette = (pixels, maxColors) => {
  let boxes = [pixels]

  while (boxes.length < maxColors) {
    let splitIndex = -1
    let splitRange = -1

    boxes.forEach((box, index) => {
      if (box.length < 2) return
      const range = getBoxRange(box).range
      if (range > splitRange) {
        splitRange = range
        splitIndex = index
      }
    })

    if (splitIndex === -1) break

    const box = boxes[splitIndex]
    const { channel } = getBoxRange(box)
    box.sort((a, b) => a[channel] - b[channel])

    const middle = Math.floor(box.length / 2)
    boxes.splice(splitIndex, 1, box.slice(0, middle), box.slice(middle))
  }

  return boxes.map(getAverageColor)
}

const getBoxRange = (pixels) => {
  const min = [255, 255, 255, 255]
  const max = [0, 0, 0, 0]

  pixels.forEach(pixel => {
    for (let channel = 0; channel < 4; channel++) {
      min[channel] = Math.min(min[channel], pixel[channel])
      max[channel] = Math.max(max[channel], pixel[channel])
    }
  })

  const ranges = max.map((value, index) => value - min[index])
  let channel = 0
  for (let index = 1; index < 3; index++) {
    if (ranges[index] > ranges[channel]) channel = index
  }

  return { channel, range: ranges[channel] }
}

const getAverageColor = (pixels) => {
  const total = pixels.reduce((sum, pixel) => {
    sum[0] += pixel[0]
    sum[1] += pixel[1]
    sum[2] += pixel[2]
    sum[3] += pixel[3]
    return sum
  }, [0, 0, 0, 0])

  return total.map(value => Math.round(value / pixels.length))
}

const findNearestColor = (pixel, palette) => {
  let nearest = palette[0]
  let nearestDistance = Number.POSITIVE_INFINITY

  palette.forEach(color => {
    const distance =
      (pixel[0] - color[0]) ** 2 +
      (pixel[1] - color[1]) ** 2 +
      (pixel[2] - color[2]) ** 2 +
      ((pixel[3] - color[3]) ** 2 * 0.25)

    if (distance < nearestDistance) {
      nearest = color
      nearestDistance = distance
    }
  })

  return nearest
}

const resetPixelate = () => {
  pixelSize.value = 8
  colorCount.value = 32
}

// ========== 历史记录 ==========
const undo = () => {
  const state = undoHistory()
  if (state) restoreHistory(state)
}

const redo = () => {
  const state = redoHistory()
  if (state) restoreHistory(state)
}

const restoreHistory = (state) => {
  currentFilter.value = state.filter
  Object.assign(adjustments, state.adjustments)
  textLayers.value = (state.textLayers || []).map(layer => ({ ...layer }))
  selectedTextId.value = null

  const canvas = canvasAreaRef.value?.canvas
  if (!canvas) return

  const img = new Image()
  img.crossOrigin = 'anonymous'
  img.onload = () => {
    canvas.width = img.width
    canvas.height = img.height
    const ctx = canvas.getContext('2d')
    ctx.drawImage(img, 0, 0)
    originalImage.value = img
    updateCanvasSize()
    nextTick(fitCanvasToView)
    hasUnsavedChanges.value = true
  }
  img.src = state.imageData
}

const resetImage = () => {
  resetAdjustments()
  const state = {
    imageData: baseImageDataUrl.value || currentImage.value?.url,
    filter: 'none',
    adjustments: { ...adjustments },
    textLayers: []
  }

  if (!state.imageData) return

  restoreHistory(state)
  saveHistory(state)
  hasUnsavedChanges.value = true
  ElMessage.success('已重置')
}

// ========== 草稿 ==========
const createDraftPayload = () => {
  const canvas = canvasAreaRef.value?.canvas
  if (!canvas || !currentImage.value) return null

  return {
    version: 1,
    savedAt: Date.now(),
    imageName: currentImage.value.name,
    imageData: canvas.toDataURL('image/png'),
    filter: currentFilter.value,
    adjustments: { ...adjustments },
    textLayers: textLayers.value.map(layer => ({ ...layer })),
    pixelSize: pixelSize.value,
    colorCount: colorCount.value
  }
}

const clearStoredDraft = () => {
  localStorage.removeItem(DRAFT_STORAGE_KEY)
}

const saveDraftLocally = (markSaved = true) => {
  const draft = createDraftPayload()
  if (!draft) return false

  try {
    localStorage.setItem(DRAFT_STORAGE_KEY, JSON.stringify(draft))
    if (markSaved) hasUnsavedChanges.value = false
    return true
  } catch (error) {
    console.error('保存图片草稿失败:', error)
    return false
  }
}

const handleSaveDraft = () => {
  if (saveDraftLocally()) ElMessage.success('草稿已保存')
  else ElMessage.error('草稿保存失败，请检查浏览器存储空间')
}

const readStoredDraft = () => {
  try {
    const rawDraft = localStorage.getItem(DRAFT_STORAGE_KEY)
    if (!rawDraft) return null
    const draft = JSON.parse(rawDraft)
    if (draft?.version !== 1 || !draft.imageData) throw new Error('Invalid draft data')
    return draft
  } catch (error) {
    console.error('读取图片草稿失败:', error)
    clearStoredDraft()
    return null
  }
}

const restoreStoredDraft = async (draft) => {
  await loadImage(draft.imageData, draft.imageName || '图片草稿.png')
  await nextTick()
  currentFilter.value = draft.filter || 'none'
  Object.assign(adjustments, {
    brightness: 0,
    contrast: 0,
    saturate: 0,
    sharpen: 0,
    ...draft.adjustments
  })
  pixelSize.value = Number(draft.pixelSize) || 8
  colorCount.value = Number(draft.colorCount) || 32
  textLayers.value = (draft.textLayers || []).map(layer => ({ ...layer }))
  selectedTextId.value = null
  hasUnsavedChanges.value = false
}

const offerDraftRestore = async () => {
  const draft = readStoredDraft()
  if (!draft) return

  try {
    await ElMessageBox.confirm(
      `发现 ${new Date(draft.savedAt).toLocaleString()} 保存的图片草稿，是否恢复？`,
      '恢复草稿',
      {
        confirmButtonText: '恢复草稿',
        cancelButtonText: '放弃草稿',
        type: 'info',
        closeOnClickModal: false
      }
    )
    await restoreStoredDraft(draft)
    ElMessage.success('草稿已恢复')
  } catch {
    clearStoredDraft()
  }
}

const handleBeforeUnload = (event) => {
  if (!hasUnsavedChanges.value) return
  saveDraftLocally(false)
  event.preventDefault()
  event.returnValue = ''
}

onBeforeRouteLeave(async () => {
  if (!hasUnsavedChanges.value) return true

  try {
    await ElMessageBox.confirm('当前图片尚未保存，是否保存为草稿后退出？', '保存草稿', {
      confirmButtonText: '保存草稿并退出',
      cancelButtonText: '不保存',
      distinguishCancelAndClose: true,
      closeOnClickModal: false,
      type: 'warning'
    })
    if (!saveDraftLocally()) {
      ElMessage.error('草稿保存失败，请检查浏览器存储空间')
      return false
    }
    return true
  } catch (action) {
    if (action === 'cancel') return true
    return false
  }
})

onMounted(async () => {
  const pendingImage = localStorage.getItem('pixel_lab_workbench_image')
  if (pendingImage) {
    localStorage.removeItem('pixel_lab_workbench_image')
    await loadImage(pendingImage, 'uploaded_image.png')
  } else {
    await offerDraftRestore()
  }
  window.addEventListener('beforeunload', handleBeforeUnload)
  window.addEventListener('keydown', handleTextLayerKeydown)
})

onUnmounted(() => {
  emit('workbench-editing-change', false)
  window.removeEventListener('beforeunload', handleBeforeUnload)
  window.removeEventListener('keydown', handleTextLayerKeydown)
})
// ========== 导出保存 ==========
const createEditedImageFile = async () => {
  const canvas = buildRenderedCanvas()
  if (!canvas) return null

  const blob = await new Promise(resolve => {
    canvas.toBlob(resolve, 'image/png', 1.0)
  })

  return new File([blob], `edited_${currentImage.value?.name || 'image.png'}`, {
    type: 'image/png'
  })
}

const getUploadedImageId = (res) => res?.id || res?.data?.id
const filenameWithoutExt = (name = '') => name.replace(/\.[^.]+$/, '')

const openArtworkDialog = (mode) => {
  artworkDialogMode.value = mode
  publishForm.title = filenameWithoutExt(currentImage.value?.name || '未命名作品')
  publishForm.tags = []
  publishForm.description = ''
  publishDialogVisible.value = true
}

const saveToGallery = () => openArtworkDialog('gallery')

const downloadImage = () => {
  exportDialogVisible.value = true
}

const confirmExport = () => {
  const canvas = buildRenderedCanvas()
  if (!canvas) return

  const mimeType = `image/${exportFormat.value}`
  const quality = exportFormat.value === 'png' ? undefined : exportQuality.value / 100
  const ext = exportFormat.value === 'jpeg' ? 'jpg' : exportFormat.value

  const link = document.createElement('a')
  link.download = `edited_${currentImage.value?.name || 'image'}.${ext}`
  link.href = canvas.toDataURL(mimeType, quality)
  link.click()
  exportDialogVisible.value = false
  ElMessage.success('下载成功')
}

const openPublishDialog = () => openArtworkDialog('community')

const submitArtwork = async () => {
  if (!publishForm.title.trim()) {
    ElMessage.warning('请先填写作品标题')
    return
  }

  publishing.value = true
  try {
    const file = await createEditedImageFile()
    if (!file) return

    const res = await uploadImage(file)
    const imageId = getUploadedImageId(res)
    if (!imageId) {
      ElMessage.warning('图片已上传，但未拿到作品ID，无法保存作品信息')
      return
    }

    const tags = [...new Set(publishForm.tags.map(tag => tag.trim()).filter(Boolean))]
    await updateImageMetadata(imageId, {
      title: publishForm.title.trim(),
      tags: tags.join(','),
      description: publishForm.description.trim()
    })
    if (artworkDialogMode.value === 'community') {
      await updateImageVisibility(imageId, true)
    }
    hasUnsavedChanges.value = false
    clearStoredDraft()
    publishDialogVisible.value = false
    ElMessage.success(artworkDialogMode.value === 'gallery' ? '已保存到个人中心' : '已发布到社区')
  } catch (error) {
    console.error('保存作品失败:', error)
    ElMessage.error(artworkDialogMode.value === 'gallery' ? '保存失败' : '发布到社区失败')
  } finally {
    publishing.value = false
  }
}
</script>

<style scoped>
.workbench-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: clamp(var(--space-3), 1.5vw, var(--space-5));
  overflow: hidden;
  min-height: 0;
  border-radius: 24px;
  background:
    radial-gradient(circle at 8% 8%, rgba(22, 199, 132, 0.08), transparent 28%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.72), rgba(246, 248, 250, 0.88));
}

.editor-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.main-content {
  flex: 1;
  display: flex;
  gap: clamp(var(--space-3), 1.4vw, var(--space-5));
  min-height: 0;
  overflow: hidden;
}

.export-options {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.export-format-group label,
.export-quality label {
  display: block;
  margin-bottom: var(--space-2);
  font-weight: 600;
  font-size: 14px;
}

@media (max-width: 1024px) {

  .main-content {
    flex-direction: column;
  }
}

@media (max-width: 640px) {
  .workbench-page {
    padding: var(--space-4);
  }
}
</style>
