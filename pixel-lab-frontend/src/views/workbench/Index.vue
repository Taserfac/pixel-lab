<!--
  【文件路径】src/views/workbench/Index.vue
  【功能说明】图像处理工作台
  - 图片导入、滤镜、参数调整、几何变换
  - 撤销/重做、保存/下载
-->

<template>
  <div class="workbench-page">
    <!-- 无图片时的引导 -->
    <div
      v-if="!currentImage"
      class="import-area"
    >
      <div
        class="import-box"
        @click="showImageSelector"
      >
        <el-icon :size="64">
          <Upload />
        </el-icon>
        <h3>导入图片开始编辑</h3>
        <p>从个人中心选择图片，或上传新图片</p>
        <div class="import-buttons">
          <el-button
            type="primary"
            @click.stop="showImageSelector"
          >
            从个人中心选择
          </el-button>
          <el-button @click.stop="triggerLocalUpload">
            本地上传
          </el-button>
        </div>
        <input
          ref="localFileInput"
          type="file"
          accept="image/*"
          style="display: none"
          @change="handleLocalUpload"
        >
      </div>
    </div>

    <!-- 编辑区域 -->
    <div
      v-else
      class="editor-container"
    >
      <div class="main-content">
        <!-- 左侧工具栏 -->
        <div class="toolbar">
          <el-collapse v-model="activeCollapse">
            <!-- 滤镜 -->
            <el-collapse-item
              title="滤镜效果"
              name="filters"
            >
              <div class="filter-grid">
                <div
                  v-for="filter in filters"
                  :key="filter.value"
                  class="filter-item"
                  :class="{ active: currentFilter === filter.value }"
                  @click="applyFilter(filter.value)"
                >
                  <div
                    class="filter-preview"
                    :style="getFilterPreviewStyle(filter.value)"
                  >
                    <img
                      :src="thumbnailUrl"
                      alt=""
                    >
                  </div>
                  <span>{{ filter.label }}</span>
                </div>
              </div>
            </el-collapse-item>

            <!-- 参数调整 -->
            <el-collapse-item
              title="参数调整"
              name="adjust"
            >
              <div class="adjust-item">
                <label>亮度</label>
                <el-slider
                  v-model="adjustments.brightness"
                  :min="-100"
                  :max="100"
                  :format-tooltip="val => val > 0 ? `+${val}` : val"
                  @input="drawImage"
                />
              </div>
              <div class="adjust-item">
                <label>对比度</label>
                <el-slider
                  v-model="adjustments.contrast"
                  :min="-100"
                  :max="100"
                  :format-tooltip="val => val > 0 ? `+${val}` : val"
                  @input="drawImage"
                />
              </div>
              <div class="adjust-item">
                <label>饱和度</label>
                <el-slider
                  v-model="adjustments.saturate"
                  :min="-100"
                  :max="100"
                  :format-tooltip="val => val > 0 ? `+${val}` : val"
                  @input="drawImage"
                />
              </div>
            </el-collapse-item>

            <!-- 几何变换 -->
            <el-collapse-item
              title="几何变换"
              name="transform"
            >
              <div class="transform-buttons">
                <el-button @click="rotateImage(90)">
                  <el-icon><RefreshRight /></el-icon>
                  旋转90°
                </el-button>
                <el-button @click="flipImage('horizontal')">
                  <el-icon class="flip-h"><Sort /></el-icon>
                  水平翻转
                </el-button>
                <el-button @click="flipImage('vertical')">
                  <el-icon><Sort /></el-icon>
                  垂直翻转
                </el-button>
              </div>
            </el-collapse-item>

            <!-- 像素画 -->
            <el-collapse-item
              title="像素画"
              name="pixel"
            >
              <div class="pixel-controls">
                <div class="adjust-item">
                  <label>像素块大小: {{ pixelSize }}px</label>
                  <el-slider
                    v-model="pixelSize"
                    :min="2"
                    :max="64"
                    :step="2"
                    @input="previewPixelate"
                  />
                </div>
                <div class="adjust-item">
                  <label>颜色数量: {{ colorCount }}色</label>
                  <el-slider
                    v-model="colorCount"
                    :min="2"
                    :max="256"
                    :step="1"
                    @input="previewPixelate"
                  />
                </div>
                <div class="pixel-buttons">
                  <el-button
                    type="primary"
                    size="small"
                    @click="applyPixelate"
                  >
                    应用
                  </el-button>
                  <el-button
                    size="small"
                    @click="resetPixelate"
                  >
                    重置
                  </el-button>
                </div>
              </div>
            </el-collapse-item>
          </el-collapse>
        </div>

        <!-- 中间画布区域 -->
        <div class="canvas-area">
          <div
            ref="canvasWrapper"
            class="canvas-wrapper"
          >
            <canvas
              ref="canvas"
              class="editor-canvas"
              :style="canvasStyle"
            />
          </div>
        </div>
      </div>

      <!-- 底部操作栏 -->
      <div class="action-bar">
        <div class="left-actions">
          <el-button
            :disabled="!canUndo"
            @click="undo"
          >
            <el-icon><Back /></el-icon>
            撤销
          </el-button>
          <el-button
            :disabled="!canRedo"
            @click="redo"
          >
            <el-icon><Right /></el-icon>
            重做
          </el-button>
          <el-button @click="resetImage">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </div>
        <div class="right-actions">
          <el-button @click="showImageSelector">
            <el-icon><FolderOpened /></el-icon>
            换一张
          </el-button>
          <el-button
            type="primary"
            @click="saveToGallery"
          >
            <el-icon><Upload /></el-icon>
            保存到个人中心
          </el-button>
          <el-button
            type="success"
            @click="downloadImage"
          >
            <el-icon><Download /></el-icon>
            下载
          </el-button>
        </div>
      </div>
    </div>

    <!-- 图片选择对话框 -->
    <el-dialog
      v-model="imageSelectorVisible"
      title="选择图片"
      width="800px"
    >
      <div
        v-loading="loadingImages"
        class="image-selector"
      >
        <div
          v-if="myImages.length > 0"
          class="image-grid"
        >
          <div
            v-for="image in myImages"
            :key="image.id"
            class="image-item"
            :class="{ selected: selectedImageId === image.id }"
            @click="selectedImageId = image.id"
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
        <el-button @click="imageSelectorVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :disabled="!selectedImageId"
          @click="confirmSelectImage"
        >
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Upload, Download, Back, Right, Refresh,
  RefreshRight, Sort, FolderOpened
} from '@element-plus/icons-vue'
import { getUserImages, uploadImage } from '@/api/image'
import EmptyState from '@/components/common/EmptyState.vue'

// ========== 状态定义 ==========

const canvas = ref(null)
const canvasWrapper = ref(null)
const localFileInput = ref(null)

// 当前编辑的图片
const currentImage = ref(null)
const originalImage = ref(null) // 原始图片对象
const thumbnailUrl = ref('') // 用于滤镜预览的缩略图

// 图片选择器
const imageSelectorVisible = ref(false)
const loadingImages = ref(false)
const myImages = ref([])
const selectedImageId = ref(null)

// 工具栏状态
const activeCollapse = ref(['filters', 'adjust', 'transform', 'pixel'])

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
const pixelPreviewApplied = ref(false)

// 历史记录
const historyStack = ref([])
const historyIndex = ref(-1)
const maxHistory = 20

const canUndo = computed(() => historyIndex.value > 0)
const canRedo = computed(() => historyIndex.value < historyStack.value.length - 1)

// Canvas 样式（使用 CSS filter 实现实时预览）
const canvasStyle = computed(() => {
  let filter = ''
  
  // 滤镜效果
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
  
  // 参数调整
  const brightness = 100 + adjustments.brightness
  const contrast = 100 + adjustments.contrast
  const saturate = 100 + adjustments.saturate
  
  filter += `brightness(${brightness}%) contrast(${contrast}%) saturate(${saturate}%)`
  
  return { filter }
})

// ========== 图片导入 ==========

const showImageSelector = async () => {
  imageSelectorVisible.value = true
  selectedImageId.value = null
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

const confirmSelectImage = async () => {
  const image = myImages.value.find(img => img.id === selectedImageId.value)
  if (!image) return
  
  imageSelectorVisible.value = false
  await loadImage(image.url, image.original_name)
}

const triggerLocalUpload = () => {
  localFileInput.value?.click()
}

const handleLocalUpload = async (e) => {
  const file = e.target.files[0]
  if (!file) return
  
  if (!file.type.startsWith('image/')) {
    ElMessage.error('请选择图片文件')
    return
  }
  
  const url = URL.createObjectURL(file)
  await loadImage(url, file.name)
  e.target.value = ''
}

const loadImage = async (url, name) => {
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.crossOrigin = 'anonymous'
    img.onload = () => {
      originalImage.value = img
      currentImage.value = { url, name }
      thumbnailUrl.value = url
      
      // 初始化画布
      nextTick(() => {
        initCanvas()
        resetAdjustments()
        saveHistory()
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
  if (!canvas.value || !originalImage.value) return
  
  const wrapper = canvasWrapper.value
  // 获取容器的实际尺寸，减去一点 padding
  const maxWidth = Math.max(wrapper.clientWidth - 20, 400)
  const maxHeight = Math.max(wrapper.clientHeight - 20, 300)
  
  let width = originalImage.value.width
  let height = originalImage.value.height
  
  // 等比缩放，让图片尽可能填满容器
  const ratio = Math.min(maxWidth / width, maxHeight / height)
  
  if (ratio < 1) {
    // 图片比容器大，缩小
    width = width * ratio
    height = height * ratio
  }
  
  // 确保尺寸至少是合理的值
  width = Math.max(width, 200)
  height = Math.max(height, 150)
  
  canvas.value.width = width
  canvas.value.height = height
  
  drawImage()
}

const drawImage = () => {
  if (!canvas.value || !originalImage.value) return
  
  const ctx = canvas.value.getContext('2d')
  
  // 清除画布
  ctx.clearRect(0, 0, canvas.value.width, canvas.value.height)
  
  // 直接绘制图片（滤镜通过 CSS canvasStyle 实现）
  ctx.drawImage(
    originalImage.value,
    0, 0,
    originalImage.value.width,
    originalImage.value.height,
    0, 0,
    canvas.value.width,
    canvas.value.height
  )
}

const getCanvasFilter = () => {
  let filter = ''
  
  // 滤镜效果
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
  case 'sharpen':
    // 锐化需要单独处理
    break
  }
  
  // 参数调整
  const brightness = 100 + adjustments.brightness
  const contrast = 100 + adjustments.contrast
  const saturate = 100 + adjustments.saturate
  
  filter += `brightness(${brightness}%) contrast(${contrast}%) saturate(${saturate}%)`
  
  return filter
}

const getFilterPreviewStyle = (filterValue) => {
  let filter = ''
  
  // 滤镜效果
  switch (filterValue) {
  case 'grayscale':
    filter = 'grayscale(100%) '
    break
  case 'sepia':
    filter = 'sepia(100%) '
    break
  case 'invert':
    filter = 'invert(100%) '
    break
  case 'blur':
    filter = 'blur(2px) '
    break
  case 'warm':
    filter = 'sepia(30%) saturate(140%) '
    break
  case 'cool':
    filter = 'saturate(80%) hue-rotate(20deg) '
    break
  default:
    filter = ''
  }
  
  // 也应用当前的参数调整
  const brightness = 100 + adjustments.brightness
  const contrast = 100 + adjustments.contrast
  const saturate = 100 + adjustments.saturate
  
  filter += `brightness(${brightness}%) contrast(${contrast}%) saturate(${saturate}%)`
  
  return { filter }
}

// ========== 滤镜与调整 ==========

const applyFilter = (filter) => {
  currentFilter.value = filter
  // 清除像素化预览
  pixelPreviewApplied.value = false
  drawImage()
  saveHistory()
}

const applyAdjustments = () => {
  // 如果有像素化预览，先清除
  if (pixelPreviewApplied.value) {
    pixelPreviewApplied.value = false
  }
  drawImage()
}

const resetAdjustments = () => {
  adjustments.brightness = 0
  adjustments.contrast = 0
  adjustments.saturate = 0
  currentFilter.value = 'none'
  pixelPreviewApplied.value = false
}

// ========== 几何变换 ==========

const rotateImage = (degree) => {
  if (!canvas.value || !originalImage.value) return
  
  // 创建临时画布保存当前状态
  const tempCanvas = document.createElement('canvas')
  const tempCtx = tempCanvas.getContext('2d')
  
  tempCanvas.width = canvas.value.width
  tempCanvas.height = canvas.value.height
  tempCtx.drawImage(canvas.value, 0, 0)
  
  // 旋转
  const radian = degree * Math.PI / 180
  const isVertical = degree % 180 !== 0
  
  const newWidth = isVertical ? canvas.value.height : canvas.value.width
  const newHeight = isVertical ? canvas.value.width : canvas.value.height
  
  canvas.value.width = newWidth
  canvas.value.height = newHeight
  
  const ctx = canvas.value.getContext('2d')
  ctx.clearRect(0, 0, newWidth, newHeight)
  ctx.save()
  ctx.translate(newWidth / 2, newHeight / 2)
  ctx.rotate(radian)
  
  if (isVertical) {
    ctx.drawImage(tempCanvas, -tempCanvas.width / 2, -tempCanvas.height / 2)
  } else {
    ctx.drawImage(tempCanvas, -tempCanvas.width / 2, -tempCanvas.height / 2)
  }
  
  ctx.restore()
  
  // 更新原图
  updateOriginalFromCanvas()
  saveHistory()
}

const flipImage = (direction) => {
  if (!canvas.value || !originalImage.value) return
  
  const tempCanvas = document.createElement('canvas')
  const tempCtx = tempCanvas.getContext('2d')
  
  tempCanvas.width = canvas.value.width
  tempCanvas.height = canvas.value.height
  tempCtx.drawImage(canvas.value, 0, 0)
  
  const ctx = canvas.value.getContext('2d')
  ctx.clearRect(0, 0, canvas.value.width, canvas.value.height)
  ctx.save()
  
  if (direction === 'horizontal') {
    ctx.translate(canvas.value.width, 0)
    ctx.scale(-1, 1)
  } else {
    ctx.translate(0, canvas.value.height)
    ctx.scale(1, -1)
  }
  
  ctx.drawImage(tempCanvas, 0, 0)
  ctx.restore()
  
  // 更新原图
  updateOriginalFromCanvas()
  saveHistory()
}

const updateOriginalFromCanvas = () => {
  // 从当前画布创建新图片
  const img = new Image()
  img.src = canvas.value.toDataURL('image/png')
  img.onload = () => {
    originalImage.value = img
  }
}

// ========== 像素画功能 ==========

// 像素化预览（实时）
const previewPixelate = () => {
  if (!canvas.value || !originalImage.value) return
  
  // 获取原始图像数据
  const tempCanvas = document.createElement('canvas')
  tempCanvas.width = originalImage.value.width
  tempCanvas.height = originalImage.value.height
  const tempCtx = tempCanvas.getContext('2d')
  tempCtx.drawImage(originalImage.value, 0, 0)
  
  // 应用像素化
  const pixelatedData = pixelateImage(tempCtx, tempCanvas.width, tempCanvas.height)
  
  // 绘制到主画布
  const ctx = canvas.value.getContext('2d')
  ctx.clearRect(0, 0, canvas.value.width, canvas.value.height)
  
  // 创建临时画布存放像素化结果
  const pixelCanvas = document.createElement('canvas')
  pixelCanvas.width = tempCanvas.width
  pixelCanvas.height = tempCanvas.height
  const pixelCtx = pixelCanvas.getContext('2d')
  pixelCtx.putImageData(pixelatedData, 0, 0)
  
  // 缩放绘制到主画布
  ctx.drawImage(pixelCanvas, 0, 0, tempCanvas.width, tempCanvas.height, 0, 0, canvas.value.width, canvas.value.height)
  
  pixelPreviewApplied.value = true
}

// 像素化算法
const pixelateImage = (ctx, width, height) => {
  const imageData = ctx.getImageData(0, 0, width, height)
  const data = imageData.data
  
  // 计算实际像素块大小（根据图片分辨率缩放）
  const scale = width / canvas.value.width
  const actualPixelSize = Math.max(1, Math.round(pixelSize.value * scale))
  
  // 颜色量化 - 构建调色板
  const colorPalette = buildColorPalette(data, colorCount.value)
  
  // 像素化处理
  for (let y = 0; y < height; y += actualPixelSize) {
    for (let x = 0; x < width; x += actualPixelSize) {
      // 获取像素块的平均颜色
      const avgColor = getBlockAverageColor(data, x, y, actualPixelSize, width, height)
      
      // 量化到调色板
      const quantizedColor = findClosestColor(avgColor, colorPalette)
      
      // 填充整个像素块
      for (let py = y; py < y + actualPixelSize && py < height; py++) {
        for (let px = x; px < x + actualPixelSize && px < width; px++) {
          const idx = (py * width + px) * 4
          data[idx] = quantizedColor.r
          data[idx + 1] = quantizedColor.g
          data[idx + 2] = quantizedColor.b
        }
      }
    }
  }
  
  return imageData
}

// 构建颜色调色板（中位切分法简化版）
const buildColorPalette = (data, numColors) => {
  const colors = []
  
  // 收集所有颜色
  for (let i = 0; i < data.length; i += 4) {
    colors.push({ r: data[i], g: data[i + 1], b: data[i + 2] })
  }
  
  // 简单采样
  const step = Math.max(1, Math.floor(colors.length / numColors))
  const palette = []
  for (let i = 0; i < colors.length && palette.length < numColors; i += step) {
    palette.push(colors[i])
  }
  
  return palette
}

// 获取像素块的平均颜色
const getBlockAverageColor = (data, startX, startY, blockSize, width, height) => {
  let r = 0, g = 0, b = 0, count = 0
  
  for (let y = startY; y < startY + blockSize && y < height; y++) {
    for (let x = startX; x < startX + blockSize && x < width; x++) {
      const idx = (y * width + x) * 4
      r += data[idx]
      g += data[idx + 1]
      b += data[idx + 2]
      count++
    }
  }
  
  return {
    r: Math.round(r / count),
    g: Math.round(g / count),
    b: Math.round(b / count)
  }
}

// 找到调色板中最接近的颜色
const findClosestColor = (color, palette) => {
  let minDist = Infinity
  let closest = palette[0]
  
  for (const p of palette) {
    const dist = Math.sqrt(
      Math.pow(color.r - p.r, 2) +
      Math.pow(color.g - p.g, 2) +
      Math.pow(color.b - p.b, 2)
    )
    if (dist < minDist) {
      minDist = dist
      closest = p
    }
  }
  
  return closest
}

// 应用像素化效果
const applyPixelate = () => {
  if (!canvas.value || !originalImage.value) return
  
  // 创建原始分辨率的画布
  const exportCanvas = document.createElement('canvas')
  exportCanvas.width = originalImage.value.width
  exportCanvas.height = originalImage.value.height
  const exportCtx = exportCanvas.getContext('2d')
  exportCtx.drawImage(originalImage.value, 0, 0)
  
  // 应用像素化
  const pixelatedData = pixelateImage(exportCtx, exportCanvas.width, exportCanvas.height)
  exportCtx.putImageData(pixelatedData, 0, 0)
  
  // 更新原图
  const img = new Image()
  img.src = exportCanvas.toDataURL('image/png')
  img.onload = () => {
    originalImage.value = img
    // 重绘主画布
    drawImage()
    // 保存历史记录
    saveHistory()
    pixelPreviewApplied.value = false
    ElMessage.success('已应用像素画效果')
  }
}

// 重置像素画预览
const resetPixelate = () => {
  if (!originalImage.value) return
  
  // 重置参数
  pixelSize.value = 8
  colorCount.value = 32
  pixelPreviewApplied.value = false
  
  // 恢复原始图像
  drawImage()
}

// ========== 历史记录 ==========

const saveHistory = () => {
  if (!canvas.value) return
  
  // 删除当前位置之后的历史
  historyStack.value = historyStack.value.slice(0, historyIndex.value + 1)
  
  // 保存当前状态
  historyStack.value.push({
    imageData: canvas.value.toDataURL('image/png'),
    filter: currentFilter.value,
    adjustments: { ...adjustments }
  })
  
  // 限制历史记录数量
  if (historyStack.value.length > maxHistory) {
    historyStack.value.shift()
  }
  
  historyIndex.value = historyStack.value.length - 1
}

const undo = () => {
  if (!canUndo.value) return
  historyIndex.value--
  restoreHistory()
}

const redo = () => {
  if (!canRedo.value) return
  historyIndex.value++
  restoreHistory()
}

const restoreHistory = () => {
  const state = historyStack.value[historyIndex.value]
  if (!state) return
  
  currentFilter.value = state.filter
  Object.assign(adjustments, state.adjustments)
  
  const img = new Image()
  img.onload = () => {
    canvas.value.width = img.width
    canvas.value.height = img.height
    const ctx = canvas.value.getContext('2d')
    ctx.drawImage(img, 0, 0)
    
    // 更新原图
    originalImage.value = img
  }
  img.src = state.imageData
}

const resetImage = () => {
  if (!originalImage.value) return
  
  resetAdjustments()
  initCanvas()
  saveHistory()
  ElMessage.success('已重置')
}

// ========== 导出保存 ==========

// 创建原始分辨率的画布用于导出
const createExportCanvas = () => {
  if (!originalImage.value) return null
  
  const exportCanvas = document.createElement('canvas')
  exportCanvas.width = originalImage.value.width
  exportCanvas.height = originalImage.value.height
  
  const ctx = exportCanvas.getContext('2d')
  
  // 构建滤镜字符串
  const parts = []
  
  // 滤镜效果
  if (currentFilter.value === 'grayscale') {
    parts.push('grayscale(100%)')
  } else if (currentFilter.value === 'sepia') {
    parts.push('sepia(100%)')
  } else if (currentFilter.value === 'invert') {
    parts.push('invert(100%)')
  } else if (currentFilter.value === 'blur') {
    parts.push('blur(3px)')
  } else if (currentFilter.value === 'warm') {
    parts.push('sepia(30%)')
    parts.push('saturate(140%)')
  } else if (currentFilter.value === 'cool') {
    parts.push('saturate(80%)')
    parts.push('hue-rotate(20deg)')
  }
  
  // 参数调整
  parts.push(`brightness(${100 + adjustments.brightness}%)`)
  parts.push(`contrast(${100 + adjustments.contrast}%)`)
  parts.push(`saturate(${100 + adjustments.saturate}%)`)
  
  // 设置滤镜
  ctx.filter = parts.join(' ')
  
  // 绘制图片
  ctx.drawImage(originalImage.value, 0, 0)
  
  return exportCanvas
}

const saveToGallery = async () => {
  if (!canvas.value) return
  
  try {
    // 创建导出画布（原始分辨率）
    const exportCanvas = document.createElement('canvas')
    exportCanvas.width = originalImage.value.width
    exportCanvas.height = originalImage.value.height
    const exportCtx = exportCanvas.getContext('2d')
    
    // 先绘制原始图片
    exportCtx.drawImage(originalImage.value, 0, 0)
    
    // 获取图像数据并应用滤镜
    const imageData = exportCtx.getImageData(0, 0, exportCanvas.width, exportCanvas.height)
    const data = imageData.data
    
    // 应用滤镜效果
    for (let i = 0; i < data.length; i += 4) {
      let r = data[i]
      let g = data[i + 1]
      let b = data[i + 2]
      
      // 黑白
      if (currentFilter.value === 'grayscale') {
        const gray = 0.299 * r + 0.587 * g + 0.114 * b
        r = g = b = gray
      }
      // 复古
      else if (currentFilter.value === 'sepia') {
        const tr = 0.393 * r + 0.769 * g + 0.189 * b
        const tg = 0.349 * r + 0.686 * g + 0.168 * b
        const tb = 0.272 * r + 0.534 * g + 0.131 * b
        r = Math.min(255, tr)
        g = Math.min(255, tg)
        b = Math.min(255, tb)
      }
      // 反转
      else if (currentFilter.value === 'invert') {
        r = 255 - r
        g = 255 - g
        b = 255 - b
      }
      // 暖色
      else if (currentFilter.value === 'warm') {
        r = Math.min(255, r * 1.2)
        g = Math.min(255, g * 1.1)
      }
      // 冷色
      else if (currentFilter.value === 'cool') {
        b = Math.min(255, b * 1.2)
        r = r * 0.9
        g = g * 0.95
      }
      
      // 应用亮度、对比度、饱和度
      const brightness = adjustments.brightness / 100
      const contrast = (adjustments.contrast + 100) / 100
      const saturation = (adjustments.saturate + 100) / 100
      
      // 亮度
      r += brightness * 255
      g += brightness * 255
      b += brightness * 255
      
      // 对比度
      r = ((r / 255 - 0.5) * contrast + 0.5) * 255
      g = ((g / 255 - 0.5) * contrast + 0.5) * 255
      b = ((b / 255 - 0.5) * contrast + 0.5) * 255
      
      // 饱和度
      const gray = 0.299 * r + 0.587 * g + 0.114 * b
      r = gray + saturation * (r - gray)
      g = gray + saturation * (g - gray)
      b = gray + saturation * (b - gray)
      
      // 限制范围
      data[i] = Math.max(0, Math.min(255, r))
      data[i + 1] = Math.max(0, Math.min(255, g))
      data[i + 2] = Math.max(0, Math.min(255, b))
    }
    
    exportCtx.putImageData(imageData, 0, 0)
    
    // 转换为 Blob
    const blob = await new Promise(resolve => {
      exportCanvas.toBlob(resolve, 'image/png', 1.0)
    })
    
    const file = new File([blob], `edited_${currentImage.value.name || 'image.png'}`, {
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
  if (!canvas.value || !originalImage.value) return
  
  // 创建导出画布（原始分辨率）
  const exportCanvas = document.createElement('canvas')
  exportCanvas.width = originalImage.value.width
  exportCanvas.height = originalImage.value.height
  const exportCtx = exportCanvas.getContext('2d')
  
  // 先绘制原始图片
  exportCtx.drawImage(originalImage.value, 0, 0)
  
  // 获取图像数据并应用滤镜
  const imageData = exportCtx.getImageData(0, 0, exportCanvas.width, exportCanvas.height)
  const data = imageData.data
  
  // 应用滤镜效果
  for (let i = 0; i < data.length; i += 4) {
    let r = data[i]
    let g = data[i + 1]
    let b = data[i + 2]
    
    // 黑白
    if (currentFilter.value === 'grayscale') {
      const gray = 0.299 * r + 0.587 * g + 0.114 * b
      r = g = b = gray
    }
    // 复古
    else if (currentFilter.value === 'sepia') {
      const tr = 0.393 * r + 0.769 * g + 0.189 * b
      const tg = 0.349 * r + 0.686 * g + 0.168 * b
      const tb = 0.272 * r + 0.534 * g + 0.131 * b
      r = Math.min(255, tr)
      g = Math.min(255, tg)
      b = Math.min(255, tb)
    }
    // 反转
    else if (currentFilter.value === 'invert') {
      r = 255 - r
      g = 255 - g
      b = 255 - b
    }
    // 暖色
    else if (currentFilter.value === 'warm') {
      r = Math.min(255, r * 1.2)
      g = Math.min(255, g * 1.1)
    }
    // 冷色
    else if (currentFilter.value === 'cool') {
      b = Math.min(255, b * 1.2)
      r = r * 0.9
      g = g * 0.95
    }
    
    // 应用亮度、对比度、饱和度
    const brightness = adjustments.brightness / 100
    const contrast = (adjustments.contrast + 100) / 100
    const saturation = (adjustments.saturate + 100) / 100
    
    // 亮度
    r += brightness * 255
    g += brightness * 255
    b += brightness * 255
    
    // 对比度
    r = ((r / 255 - 0.5) * contrast + 0.5) * 255
    g = ((g / 255 - 0.5) * contrast + 0.5) * 255
    b = ((b / 255 - 0.5) * contrast + 0.5) * 255
    
    // 饱和度
    const gray = 0.299 * r + 0.587 * g + 0.114 * b
    r = gray + saturation * (r - gray)
    g = gray + saturation * (g - gray)
    b = gray + saturation * (b - gray)
    
    // 限制范围
    data[i] = Math.max(0, Math.min(255, r))
    data[i + 1] = Math.max(0, Math.min(255, g))
    data[i + 2] = Math.max(0, Math.min(255, b))
  }
  
  exportCtx.putImageData(imageData, 0, 0)
  
  const link = document.createElement('a')
  link.download = `edited_${currentImage.value.name || 'image.png'}`
  link.href = exportCanvas.toDataURL('image/png', 1.0)
  link.click()
  ElMessage.success('下载成功')
}

// ========== 生命周期 ==========

onMounted(() => {
  // 监听窗口大小变化
  window.addEventListener('resize', () => {
    if (currentImage.value) {
      initCanvas()
    }
  })
})
</script>

<style scoped>
.workbench-page {
  height: calc(100vh - 60px);
  display: flex;
  flex-direction: column;
  padding: var(--space-6);
  overflow: hidden;
}

/* 导入区域 */
.import-area {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.import-box {
  text-align: center;
  padding: 60px 80px;
  background: var(--background-soft);
  border: 4px solid var(--border);
  box-shadow: 12px 12px 0px 0px var(--border);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.import-box:hover {
  transform: translate(-4px, -4px);
  box-shadow: 16px 16px 0px 0px var(--border);
}

.import-box h3 {
  margin: var(--space-4) 0 var(--space-2);
  font-size: 28px;
  font-weight: 700;
  font-family: var(--font-mono);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.import-box p {
  color: var(--foreground-muted);
  margin-bottom: var(--space-6);
  font-size: 16px;
}

.import-buttons {
  display: flex;
  gap: var(--space-4);
  justify-content: center;
}

/* 编辑器布局 */
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

/* 左侧工具栏 */
.toolbar {
  width: 300px;
  flex-shrink: 0;
  background: var(--background-soft);
  border: 4px solid var(--border);
  box-shadow: 8px 8px 0px 0px var(--border);
  overflow-y: auto;
  max-height: 100%;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-3);
}

.filter-item {
  text-align: center;
  padding: var(--space-4);
  border: 3px solid var(--border);
  background: var(--background-soft);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.filter-item:hover {
  background: var(--accent);
  transform: translate(-2px, -2px);
  box-shadow: 4px 4px 0px 0px var(--border);
}

.filter-item.active {
  background: var(--primary);
  box-shadow: 4px 4px 0px 0px var(--border);
}

.filter-item.active span {
  color: white;
}

.filter-preview {
  width: 80px;
  height: 80px;
  margin: 0 auto var(--space-2);
  border: 3px solid var(--border);
  overflow: hidden;
}

.filter-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.filter-item span {
  font-size: 13px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--foreground);
}

.adjust-item {
  margin-bottom: var(--space-5);
  padding: var(--space-4);
  border: 3px solid var(--border);
  background: var(--background-muted);
}

.adjust-item label {
  display: block;
  font-size: 14px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--foreground);
  margin-bottom: var(--space-3);
}

.transform-buttons {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.transform-buttons .el-button {
  width: 100% !important;
  margin-left: 0 !important;
  padding: 14px 16px !important;
  font-size: 14px !important;
}

.transform-buttons .el-icon {
  margin-right: 8px;
}

.flip-h {
  transform: rotate(90deg);
}

/* 像素画控制 */
.pixel-controls {
  padding: 0;
}

.pixel-buttons {
  display: flex;
  gap: var(--space-3);
  margin-top: var(--space-4);
}

.pixel-buttons .el-button {
  flex: 1;
}

/* 中间画布区域 */
.canvas-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--background-soft);
  border: 4px solid var(--border);
  box-shadow: 8px 8px 0px 0px var(--border);
  min-width: 0;
  overflow: hidden;
}

.canvas-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-6);
}

.editor-canvas {
  max-width: 100%;
  max-height: 100%;
  border: 4px solid var(--border);
  box-shadow: 6px 6px 0px 0px var(--border);
}

/* 底部操作栏 */
.action-bar {
  display: flex;
  justify-content: space-between;
  padding: var(--space-5);
  background: var(--background-soft);
  border: 4px solid var(--border);
  box-shadow: 6px 6px 0px 0px var(--border);
  margin-top: var(--space-5);
  flex-shrink: 0;
}

.left-actions,
.right-actions {
  display: flex;
  gap: var(--space-3);
}

/* 图片选择器 */
.image-selector {
  max-height: 400px;
  overflow-y: auto;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: var(--space-4);
}

.image-item {
  cursor: pointer;
  overflow: hidden;
  border: 4px solid var(--border);
  transition: all var(--transition-fast);
}

.image-item:hover {
  transform: translate(-4px, -4px);
  box-shadow: 6px 6px 0px 0px var(--border);
}

.image-item.selected {
  border-color: var(--primary);
  box-shadow: 6px 6px 0px 0px var(--primary);
}

.image-item img {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
}

.image-name {
  padding: var(--space-3);
  font-size: 14px;
  font-weight: 700;
  text-align: center;
  background: var(--background-muted);
  border-top: 4px solid var(--border);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}
</style>