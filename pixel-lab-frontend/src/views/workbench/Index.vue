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
          <!-- 左侧工具栏 -->
          <ToolBar
            :filters="filters"
            :current-filter="currentFilter"
            :adjustments="adjustments"
            :pixel-size="pixelSize"
            :color-count="colorCount"
            :thumbnail-url="thumbnailUrl"
            @apply-filter="applyFilter"
            @update-adjustment="updateAdjustment"
            @rotate="rotateImage"
            @flip="flipImage"
            @apply-pixel="applyPixelate"
            @reset-pixel="resetPixelate"
            @update:pixelSize="pixelSize = $event"
            @update:colorCount="colorCount = $event"
          />

          <!-- 中间画布区域 -->
          <CanvasArea
            ref="canvasAreaRef"
            :canvas-style="canvasStyle"
            @resize="initCanvas"
          />
        </div>

        <!-- 底部操作栏 -->
        <ActionBar
          :can-undo="canUndo"
          :can-redo="canRedo"
          @undo="undo"
          @redo="redo"
          @reset="resetImage"
          @change-image="showImageSelector"
          @save="saveToGallery"
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
  </div>
</template>

<script setup>
import { ref, reactive, computed, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import ImportArea from './components/ImportArea.vue'
import ToolBar from './components/ToolBar.vue'
import CanvasArea from './components/CanvasArea.vue'
import ActionBar from './components/ActionBar.vue'
import ImageSelector from './components/ImageSelector.vue'
import { getUserImages, uploadImage } from '@/api/image'
import { useHistory } from './composables/useHistory'

// ========== 组件 refs ==========
const canvasAreaRef = ref(null)

// ========== 状态定义 ==========
const currentImage = ref(null)
const originalImage = ref(null)
const thumbnailUrl = ref('')

// 图片选择器
const imageSelectorVisible = ref(false)
const loadingImages = ref(false)
const myImages = ref([])

// 滤镜选项
const filters = [
  { label: '原图', value: 'none' },
  { label: '黑白', value: 'grayscale' },
  { label: '复古', value: 'sepia' },
  { label: '反转', value: 'invert' },
  { label: '模糊', value: 'blur' },
  { label: '锐化', value: 'sharpen' },
  { label: '暖色', value: 'warm' },
  { label: '冷色', value: 'cool' }
]
const currentFilter = ref('none')

// 参数调整
const adjustments = reactive({
  brightness: 0,
  contrast: 0,
  saturate: 0
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
  case 'warm':
    filter += 'sepia(30%) saturate(140%) '
    break
  case 'cool':
    filter += 'saturate(80%) hue-rotate(20deg) '
    break
  }

  const brightness = 100 + adjustments.brightness
  const contrast = 100 + adjustments.contrast
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

// ========== 画布操作 ==========
const initCanvas = () => {
  const canvas = canvasAreaRef.value?.canvas
  const wrapper = canvasAreaRef.value?.wrapper
  if (!canvas || !originalImage.value || !wrapper) return

  const maxWidth = Math.max(wrapper.clientWidth - 20, 400)
  const maxHeight = Math.max(wrapper.clientHeight - 20, 300)

  let width = originalImage.value.width
  let height = originalImage.value.height

  const ratio = Math.min(maxWidth / width, maxHeight / height)

  if (ratio < 1) {
    width = width * ratio
    height = height * ratio
  }

  width = Math.max(width, 200)
  height = Math.max(height, 150)

  canvas.width = width
  canvas.height = height

  drawImage()
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
}

const resetAdjustments = () => {
  adjustments.brightness = 0
  adjustments.contrast = 0
  adjustments.saturate = 0
  currentFilter.value = 'none'
}

const saveToHistory = () => {
  const canvas = canvasAreaRef.value?.canvas
  if (!canvas) return
  saveHistory({
    imageData: canvas.toDataURL('image/png'),
    filter: currentFilter.value,
    adjustments: { ...adjustments }
  })
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
const applyPixelate = () => {
  const canvas = canvasAreaRef.value?.canvas
  if (!canvas || !originalImage.value) return

  // 简化版像素化
  const ctx = canvas.getContext('2d')
  const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height)
  
  const size = pixelSize.value
  const data = imageData.data

  for (let y = 0; y < canvas.height; y += size) {
    for (let x = 0; x < canvas.width; x += size) {
      let r = 0, g = 0, b = 0, count = 0

      for (let py = y; py < y + size && py < canvas.height; py++) {
        for (let px = x; px < x + size && px < canvas.width; px++) {
          const idx = (py * canvas.width + px) * 4
          r += data[idx]
          g += data[idx + 1]
          b += data[idx + 2]
          count++
        }
      }

      r = Math.round(r / count)
      g = Math.round(g / count)
      b = Math.round(b / count)

      for (let py = y; py < y + size && py < canvas.height; py++) {
        for (let px = x; px < x + size && px < canvas.width; px++) {
          const idx = (py * canvas.width + px) * 4
          data[idx] = r
          data[idx + 1] = g
          data[idx + 2] = b
        }
      }
    }
  }

  ctx.putImageData(imageData, 0, 0)
  updateOriginalFromCanvas()
  saveToHistory()
  ElMessage.success('已应用像素画效果')
}

const resetPixelate = () => {
  pixelSize.value = 8
  colorCount.value = 32
  drawImage()
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

  const canvas = canvasAreaRef.value?.canvas
  if (!canvas) return

  const img = new Image()
  img.onload = () => {
    canvas.width = img.width
    canvas.height = img.height
    const ctx = canvas.getContext('2d')
    ctx.drawImage(img, 0, 0)
    originalImage.value = img
  }
  img.src = state.imageData
}

const resetImage = () => {
  resetAdjustments()
  initCanvas()
  saveToHistory()
  ElMessage.success('已重置')
}

// ========== 导出保存 ==========
const saveToGallery = async () => {
  const canvas = canvasAreaRef.value?.canvas
  if (!canvas) return

  try {
    const blob = await new Promise(resolve => {
      canvas.toBlob(resolve, 'image/png', 1.0)
    })

    const file = new File([blob], `edited_${currentImage.value?.name || 'image.png'}`, {
      type: 'image/png'
    })

    await uploadImage(file)
    ElMessage.success('已保存到个人中心')
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败')
  }
}

const downloadImage = () => {
  const canvas = canvasAreaRef.value?.canvas
  if (!canvas) return

  const link = document.createElement('a')
  link.download = `edited_${currentImage.value?.name || 'image.png'}`
  link.href = canvas.toDataURL('image/png', 1.0)
  link.click()
  ElMessage.success('下载成功')
}
</script>

<style scoped>
.workbench-page {
  height: calc(100vh - 72px - 80px);
  display: flex;
  flex-direction: column;
  padding: var(--space-6);
  overflow: hidden;
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
  gap: var(--space-6);
  min-height: 0;
  overflow: hidden;
}
</style>