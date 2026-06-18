<template>
  <div class="studio-page">
    <header class="studio-header">
      <router-link class="studio-brand" to="/explore">
        <span class="mini-mark" aria-hidden="true"><i /><i /><i /><i /></span>
        <strong>Pixel Lab</strong>
      </router-link>
      <span class="studio-divider" />
      <h1>图片编辑</h1>

      <div class="history-actions">
        <UiButton variant="ghost" size="sm" :disabled="!canUndo" title="撤销 (Ctrl+Z)" @click="undo">
          <template #icon><el-icon><RefreshLeft /></el-icon></template>撤销
        </UiButton>
        <UiButton variant="ghost" size="sm" :disabled="!canRedo" title="重做 (Ctrl+Y)" @click="redo">
          <template #icon><el-icon><RefreshRight /></el-icon></template>重做
        </UiButton>
      </div>

      <div class="document-actions">
        <UiButton variant="secondary" size="sm" @click="openImagePicker">
          <template #icon><el-icon><FolderOpened /></el-icon></template>打开
        </UiButton>
        <UiButton size="sm" :disabled="!hasDocument" @click="exportVisible = true">
          <template #icon><el-icon><Download /></el-icon></template>导出
        </UiButton>
      </div>
    </header>

    <div class="studio-workspace">
      <aside class="tool-rail" aria-label="编辑工具">
        <button
          v-for="item in tools"
          :key="item.value"
          type="button"
          :class="{ active: activeTool === item.value }"
          :disabled="!hasDocument"
          :title="`${item.label}${item.shortcut ? ` (${item.shortcut})` : ''}`"
          @click="setTool(item.value)"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </button>
      </aside>

      <main class="canvas-stage" ref="stageRef" @contextmenu.prevent>
        <!-- 底层：固定底图 -->
        <div v-show="hasDocument" class="canvas-layer bg-layer">
          <canvas ref="bgCanvasRef" />
        </div>
        <!-- 上层：编辑层（涂鸦/文字/马赛克/裁剪框） -->
        <div v-show="hasDocument" class="canvas-layer top-layer">
          <canvas ref="canvasRef" />
        </div>

        <UiCard v-if="!hasDocument" padding="lg" class="empty-document">
          <span class="empty-icon"><el-icon><Picture /></el-icon></span>
          <h2>选择一张图片开始编辑</h2>
          <p>导入本地图片，或从个人中心选择已有作品。</p>
          <div>
            <UiButton @click="localInput?.click()">
              <template #icon><el-icon><Upload /></el-icon></template>导入图片
            </UiButton>
            <UiButton variant="secondary" @click="openImagePicker">选择作品</UiButton>
          </div>
        </UiCard>

        <input ref="localInput" class="visually-hidden" type="file" accept="image/jpeg,image/png,image/webp" @change="onLocalFile">

        <div class="zoom-controls" aria-label="画布缩放">
          <button type="button" :disabled="!hasDocument || zoom <= 0.4" @click="changeZoom(-0.1)"><el-icon><ZoomOut /></el-icon></button>
          <span>{{ Math.round(zoom * 100) }}%</span>
          <button type="button" :disabled="!hasDocument || zoom >= 2" @click="changeZoom(0.1)"><el-icon><ZoomIn /></el-icon></button>
          <button type="button" :disabled="!hasDocument" @click="fitImage"><el-icon><FullScreen /></el-icon></button>
        </div>
      </main>

      <aside class="inspector">
        <!-- 裁剪面板 -->
        <section v-if="activeTool === 'crop'" class="inspector-section">
          <header><h2>裁剪</h2></header>
          <div class="crop-ratio-list">
            <button v-for="r in cropRatios" :key="r.label" type="button" :class="{ active: cropRatio === r.value }" @click="setCropRatio(r.value)">{{ r.label }}</button>
          </div>
          <div class="tool-confirm-actions">
            <UiButton size="sm" @click="confirmTool">保存</UiButton>
            <UiButton size="sm" variant="secondary" @click="cancelTool">取消</UiButton>
          </div>
        </section>

        <!-- 调色面板 -->
        <section v-if="activeTool === 'adjust'" class="inspector-section">
          <header><h2>调色</h2></header>
          <div class="adjustments">
            <label><span>亮度</span><strong>{{ adjustments.brightness }}</strong></label>
            <el-slider v-model="adjustments.brightness" :min="-100" :max="100" @input="previewAdjustments" />
            <label><span>对比度</span><strong>{{ adjustments.contrast }}</strong></label>
            <el-slider v-model="adjustments.contrast" :min="-100" :max="100" @input="previewAdjustments" />
            <label><span>饱和度</span><strong>{{ adjustments.saturation }}</strong></label>
            <el-slider v-model="adjustments.saturation" :min="-100" :max="100" @input="previewAdjustments" />
            <label><span>色温</span><strong>{{ adjustments.temperature }}</strong></label>
            <el-slider v-model="adjustments.temperature" :min="-100" :max="100" @input="previewAdjustments" />
          </div>
          <div class="tool-confirm-actions">
            <UiButton size="sm" @click="confirmTool">保存</UiButton>
            <UiButton size="sm" variant="secondary" @click="cancelTool">取消</UiButton>
          </div>
        </section>

        <!-- 滤镜面板 -->
        <section v-if="activeTool === 'filter'" class="inspector-section">
          <header><h2>滤镜</h2></header>
          <div class="filter-grid">
            <button v-for="item in filterPresets" :key="item.value" type="button" :class="['filter-item', { active: activeFilter === item.value }]" @click="previewFilter(item.value)">
              <span class="filter-thumb" :class="`filter-preview-${item.value}`" />
              <small>{{ item.label }}</small>
            </button>
          </div>
          <div class="tool-confirm-actions">
            <UiButton size="sm" @click="confirmTool">保存</UiButton>
            <UiButton size="sm" variant="secondary" @click="cancelTool">取消</UiButton>
          </div>
        </section>

        <!-- 文字面板 -->
        <section v-if="activeTool === 'text'" class="inspector-section">
          <header><h2>添加文字</h2></header>
          <el-input v-model="textContent" type="textarea" :rows="3" placeholder="输入文字..." />
          <label class="control-row" style="margin-top: var(--space-3)">
            <span>字号</span><strong>{{ textSize }}px</strong>
          </label>
          <el-slider v-model="textSize" :min="12" :max="120" />
          <div class="color-control" style="margin-top: var(--space-3)">
            <input v-model="textColor" type="color" aria-label="文字颜色">
            <input v-model="textColor" type="text" maxlength="7" aria-label="颜色值">
          </div>
          <label class="control-row" style="margin-top: var(--space-3)">
            <span>字体</span>
          </label>
          <el-select v-model="textFont" size="small" style="width: 100%">
            <el-option v-for="f in fontOptions" :key="f.value" :label="f.label" :value="f.value" />
          </el-select>
          <div style="display: flex; gap: var(--space-2); margin-top: var(--space-3)">
            <UiButton size="sm" @click="addText">添加</UiButton>
            <UiButton size="sm" variant="secondary" :disabled="!hasSelectedText" @click="deleteSelectedText">删除选中文字</UiButton>
          </div>
          <div style="display: flex; gap: var(--space-2); margin-top: var(--space-2)">
            <UiButton size="sm" variant="secondary" @click="confirmTool">保存</UiButton>
            <UiButton size="sm" variant="secondary" @click="cancelTool">取消</UiButton>
          </div>
          <p style="color: var(--text-tertiary); font-size: 10px; margin: var(--space-2) 0 0">点击"添加"将文字放到图片上，可拖拽调整位置。选中文字后点击"删除选中文字"移除。</p>
        </section>

        <!-- 涂鸦笔面板 -->
        <section v-if="activeTool === 'doodle'" class="inspector-section">
          <header><h2>涂鸦笔</h2></header>
          <div class="doodle-modes">
            <button type="button" :class="{ active: doodleMode === 'draw' }" @click="doodleMode = 'draw'; setDoodleBrush()">
              <el-icon><EditPen /></el-icon>画笔
            </button>
            <button type="button" :class="{ active: doodleMode === 'eraser' }" @click="doodleMode = 'eraser'; setDoodleBrush()">
              <el-icon><Remove /></el-icon>橡皮
            </button>
          </div>
          <div v-if="doodleMode === 'draw'" class="color-control" style="margin-top: var(--space-3)">
            <input v-model="doodleColor" type="color" aria-label="画笔颜色">
            <input v-model="doodleColor" type="text" maxlength="7" aria-label="颜色值">
          </div>
          <label class="control-row" style="margin-top: var(--space-3)">
            <span>{{ doodleMode === 'eraser' ? '橡皮大小' : '画笔大小' }}</span><strong>{{ doodleSize }}px</strong>
          </label>
          <el-slider v-model="doodleSize" :min="1" :max="80" @input="setDoodleBrush" />
          <div class="brush-presets">
            <button v-for="size in [2, 8, 20, 40]" :key="size" type="button" :class="{ active: doodleSize === size }" @click="doodleSize = size; setDoodleBrush()">
              <i :style="{ width: `${Math.min(22, 2 + size / 3)}px`, height: `${Math.min(22, 2 + size / 3)}px` }" />
            </button>
          </div>
          <div class="tool-confirm-actions">
            <UiButton size="sm" @click="confirmTool">保存</UiButton>
            <UiButton size="sm" variant="secondary" @click="cancelTool">取消</UiButton>
          </div>
        </section>

        <!-- 马赛克面板 -->
        <section v-if="activeTool === 'mosaic'" class="inspector-section">
          <header><h2>马赛克</h2></header>
          <label class="control-row">
            <span>马赛克大小</span><strong>{{ mosaicSize }}px</strong>
          </label>
          <el-slider v-model="mosaicSize" :min="4" :max="40" />
          <p style="color: var(--text-tertiary); font-size: 11px; margin: var(--space-2) 0 0">在图片上涂抹添加马赛克效果</p>
          <div class="tool-confirm-actions">
            <UiButton size="sm" @click="confirmTool">保存</UiButton>
            <UiButton size="sm" variant="secondary" @click="cancelTool">取消</UiButton>
          </div>
        </section>

        <!-- 通用：当前工具提示 -->
        <section v-if="!activeTool" class="inspector-section">
          <header><h2>选择工具</h2></header>
          <p style="color: var(--text-tertiary); font-size: 11px">从左侧选择一个编辑工具开始。</p>
        </section>
      </aside>
    </div>

    <UiModal v-model="imagePickerVisible" title="选择已有作品" max-width="760px">
      <div v-if="loadingImages" class="picker-loading"><el-skeleton animated :rows="4" /></div>
      <div v-else-if="myImages.length" class="image-picker-grid">
        <button v-for="image in myImages" :key="image.id" type="button" @click="loadImageUrl(image.url, image.title || image.original_name)">
          <img :src="image.thumbnail_url || image.url" :alt="image.title || image.original_name">
          <span>{{ image.title || image.original_name || '未命名作品' }}</span>
        </button>
      </div>
      <EmptyState v-else title="还没有作品" description="先从本地导入一张图片吧。" />
      <template #footer>
        <UiButton variant="secondary" @click="imagePickerVisible = false">取消</UiButton>
        <UiButton @click="localInput?.click()">从本地导入</UiButton>
      </template>
    </UiModal>

    <UiModal v-model="exportVisible" title="导出作品" max-width="440px">
      <div class="export-form">
        <label><span>文件格式</span><el-radio-group v-model="exportFormat"><el-radio-button value="png">PNG</el-radio-button><el-radio-button value="jpeg">JPG</el-radio-button><el-radio-button value="webp">WebP</el-radio-button></el-radio-group></label>
        <label v-if="exportFormat !== 'png'"><span>图片质量 {{ exportQuality }}%</span><el-slider v-model="exportQuality" :min="50" :max="100" /></label>
      </div>
      <template #footer>
        <UiButton variant="secondary" :loading="saving" @click="saveToProfile">保存到个人中心</UiButton>
        <UiButton @click="download"><template #icon><el-icon><Download /></el-icon></template>下载</UiButton>
      </template>
    </UiModal>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, shallowRef } from 'vue'
import { useRoute } from 'vue-router'
import { Canvas, FabricImage, PencilBrush, Point, filters } from 'fabric'
import {
  Crop,
  Download,
  EditPen,
  FolderOpened,
  FullScreen,
  Grid,
  MagicStick,
  Picture,
  RefreshLeft,
  RefreshRight,
  Remove,
  Sunny,
  Upload,
  ZoomIn,
  ZoomOut
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import EmptyState from '@/components/common/EmptyState.vue'
import UiButton from '@/components/ui/UiButton.vue'
import UiCard from '@/components/ui/UiCard.vue'
import UiModal from '@/components/ui/UiModal.vue'
import { getImageDetail } from '@/api/community'
import { getUserImages, uploadImage } from '@/api/image'

const route = useRoute()
const bgCanvasRef = ref(null)
const canvasRef = ref(null)
const stageRef = ref(null)
const localInput = ref(null)
const bgCanvas = shallowRef(null)   // 底层 canvas：固定底图
const fabricCanvas = shallowRef(null) // 上层 canvas：涂鸦/文字/马赛克
const baseImage = shallowRef(null)
const hasDocument = ref(false)
const activeTool = ref(null)
const zoom = ref(1)
const history = ref([])
const historyIndex = ref(-1)
const restoring = ref(false)
const imagePickerVisible = ref(false)
const loadingImages = ref(false)
const myImages = ref([])
const exportVisible = ref(false)
const exportFormat = ref('png')
const exportQuality = ref(92)
const saving = ref(false)
let resizeObserver

// 工具状态快照
let toolSnapshot = null
let cropRect = null

// 调色
const adjustments = reactive({ brightness: 0, contrast: 0, saturation: 0, temperature: 0 })

// 滤镜
const activeFilter = ref('original')
const filterPresets = [
  { label: '原图', value: 'original' },
  { label: '清晰', value: 'clear' },
  { label: '暖色', value: 'warm' },
  { label: '冷色', value: 'cool' },
  { label: '黑白', value: 'mono' },
  { label: '复古', value: 'vintage' },
  { label: '胶片', value: 'film' },
  { label: '日系', value: 'japanese' },
  { label: '美食', value: 'food' },
  { label: '油画', value: 'painting' }
]
const filterPresetsMap = {
  original: [],
  clear: [new filters.Contrast({ contrast: 0.12 }), new filters.Saturation({ saturation: 0.08 })],
  warm: [new filters.BlendColor({ color: '#ef9b55', mode: 'overlay', alpha: 0.16 })],
  cool: [new filters.BlendColor({ color: '#5599ef', mode: 'overlay', alpha: 0.14 })],
  mono: [new filters.Grayscale()],
  vintage: [new filters.Sepia(), new filters.Contrast({ contrast: 0.08 })],
  film: [new filters.Contrast({ contrast: 0.15 }), new filters.Saturation({ saturation: -0.2 }), new filters.Brightness({ brightness: -0.05 })],
  japanese: [new filters.Brightness({ brightness: 0.06 }), new filters.Saturation({ saturation: -0.15 }), new filters.Contrast({ contrast: -0.05 })],
  food: [new filters.Saturation({ saturation: 0.2 }), new filters.Brightness({ brightness: 0.05 }), new filters.Contrast({ contrast: 0.08 })],
  painting: [new filters.Contrast({ contrast: 0.25 }), new filters.Saturation({ saturation: 0.15 })]
}

// 裁剪
const cropRatio = ref('free')
const cropRatios = [
  { label: '自由', value: 'free' },
  { label: '1:1', value: '1:1' },
  { label: '4:3', value: '4:3' },
  { label: '3:2', value: '3:2' },
  { label: '16:9', value: '16:9' },
  { label: '9:16', value: '9:16' }
]

// 文字
const textContent = ref('在此输入文字')
const textSize = ref(36)
const textColor = ref('#ffffff')
const textFont = ref('Arial')
const fontOptions = [
  { label: 'Arial', value: 'Arial' },
  { label: 'SimHei (黑体)', value: 'SimHei, sans-serif' },
  { label: 'SimSun (宋体)', value: 'SimSun, serif' },
  { label: 'KaiTi (楷体)', value: 'KaiTi, serif' },
  { label: 'Microsoft YaHei', value: 'Microsoft YaHei, sans-serif' }
]

// 涂鸦笔
const doodleMode = ref('draw')
const doodleColor = ref('#1ead78')
const doodleSize = ref(8)

// 马赛克
const mosaicSize = ref(12)

// 无右键菜单，改用文字面板中的删除按钮

const tools = [
  { label: '裁剪', value: 'crop', icon: Crop, shortcut: 'C' },
  { label: '调色', value: 'adjust', icon: Sunny, shortcut: 'A' },
  { label: '滤镜', value: 'filter', icon: MagicStick, shortcut: 'F' },
  { label: '文字', value: 'text', icon: EditPen, shortcut: 'T' },
  { label: '涂鸦', value: 'doodle', icon: EditPen, shortcut: 'D' },
  { label: '马赛克', value: 'mosaic', icon: Grid, shortcut: 'M' }
]

const canUndo = computed(() => historyIndex.value > 0)
const canRedo = computed(() => historyIndex.value < history.value.length - 1)

// ─── 双层 Canvas 初始化 ────────────────────────────────

const createCanvases = () => {
  if (bgCanvas.value || !bgCanvasRef.value || !canvasRef.value) return

  // 底层：放底图，不可交互
  bgCanvas.value = new Canvas(bgCanvasRef.value, {
    backgroundColor: 'transparent',
    selection: false,
    renderOnAddRemove: true
  })

  // 上层：放涂鸦/文字/马赛克
  fabricCanvas.value = new Canvas(canvasRef.value, {
    backgroundColor: 'transparent',
    preserveObjectStacking: true,
    selection: false
  })

  resizeCanvas()

  const top = fabricCanvas.value
  top.on('object:modified', saveHistory)
  top.on('path:created', () => saveHistory())

  resizeObserver = new ResizeObserver(resizeCanvas)
  resizeObserver.observe(stageRef.value)
}

const resizeCanvas = () => {
  const stage = stageRef.value
  if (!stage) return
  const width = stage.clientWidth
  const height = stage.clientHeight
  if (bgCanvas.value) {
    bgCanvas.value.setDimensions({ width, height })
    bgCanvas.value.requestRenderAll()
  }
  if (fabricCanvas.value) {
    fabricCanvas.value.setDimensions({ width, height })
    fabricCanvas.value.requestRenderAll()
  }
}

// ─── 图片加载 ──────────────────────────────────────────

const onLocalFile = event => {
  const file = event.target.files?.[0]
  if (!file) return
  const url = URL.createObjectURL(file)
  loadImageUrl(url, file.name).finally(() => URL.revokeObjectURL(url))
  event.target.value = ''
}

const loadImageUrl = async (url, name = '编辑图片') => {
  const bg = bgCanvas.value
  const top = fabricCanvas.value
  if (!bg || !top) return
  try {
    const image = await FabricImage.fromURL(url, { crossOrigin: url.startsWith('data:') || url.startsWith('blob:') ? null : 'anonymous' })

    // 清空两个 canvas
    bg.clear()
    bg.backgroundColor = 'transparent'
    top.clear()

    // 底图铺满底层 canvas（小图也会放大至填满画布）
    const scale = Math.min(bg.width / image.width, bg.height / image.height)
    image.set({
      name,
      left: bg.width / 2,
      top: bg.height / 2,
      originX: 'center',
      originY: 'center',
      selectable: false,
      hasControls: false,
      evented: false,
      lockMovementX: true,
      lockMovementY: true,
      lockScalingX: true,
      lockScalingY: true,
      lockRotation: true
    })
    image.scale(scale)
    bg.add(image)
    bg.requestRenderAll()
    baseImage.value = image

    hasDocument.value = true
    activeTool.value = null
    toolSnapshot = null
    activeFilter.value = 'original'
    Object.assign(adjustments, { brightness: 0, contrast: 0, saturation: 0, temperature: 0 })
    zoom.value = 1
    resetHistory()
    imagePickerVisible.value = false
  } catch (error) {
    console.error('加载图片失败:', error)
    ElMessage.error('图片加载失败，请换一张图片重试')
  }
}

const openImagePicker = async () => {
  imagePickerVisible.value = true
  if (myImages.value.length) return
  loadingImages.value = true
  try {
    const data = await getUserImages({ page: 1, pageSize: 30 })
    myImages.value = data?.list || []
  } catch {
    myImages.value = []
  } finally {
    loadingImages.value = false
  }
}

// ─── 工具保存/取消机制 ──────────────────────────────────

const takeSnapshot = () => {
  if (!fabricCanvas.value) return null
  return JSON.stringify(fabricCanvas.value.toJSON(['name']))
}

const restoreSnapshot = async () => {
  if (!toolSnapshot || !fabricCanvas.value) return
  restoring.value = true
  await fabricCanvas.value.loadFromJSON(toolSnapshot)
  fabricCanvas.value.requestRenderAll()
  restoring.value = false
  toolSnapshot = null
}

const confirmTool = () => {
  if (activeTool.value === 'crop' && cropRect) {
    applyCrop()
    toolSnapshot = null
    activeTool.value = null
    return
  }
  toolSnapshot = null
  activeTool.value = null
  const top = fabricCanvas.value
  if (top) {
    top.isDrawingMode = false
    setTextSelectable(false)
  }
  saveHistory()
  ElMessage.success('已保存')
}

const cancelTool = async () => {
  const isCropCancel = activeTool.value === 'crop'
  await restoreSnapshot()
  activeTool.value = null

  // 裁剪取消时恢复底图（mergeToTopForCrop 会清空底层）
  if (isCropCancel && baseImage.value) {
    const bg = bgCanvas.value
    if (bg) {
      bg.clear()
      bg.backgroundColor = 'transparent'
      bg.add(baseImage.value)
      bg.requestRenderAll()
    }
  }

  const top = fabricCanvas.value
  if (top) {
    top.isDrawingMode = false
    top.selection = false
    setTextSelectable(false)
    top.discardActiveObject()
    top.requestRenderAll()
  }
  Object.assign(adjustments, { brightness: 0, contrast: 0, saturation: 0, temperature: 0 })
  activeFilter.value = 'original'
  ElMessage.info('已取消修改')
}

// ─── 工具切换 ──────────────────────────────────────────

const setTool = async value => {
  const top = fabricCanvas.value
  if (!top) return

  // 如果正在使用其他工具，先 await 取消
  if (activeTool.value && activeTool.value !== value) {
    await cancelTool()
  }

  // 点击同一个工具 = 关闭
  if (activeTool.value === value) {
    await cancelTool()
    return
  }

  // 进入新工具：保存快照
  toolSnapshot = takeSnapshot()
  activeTool.value = value

  if (value === 'crop') {
    top.selection = false
    setTextSelectable(false)
    initCrop()
  } else if (value === 'adjust') {
    top.selection = false
    setTextSelectable(false)
  } else if (value === 'filter') {
    top.selection = false
    setTextSelectable(false)
  } else if (value === 'text') {
    top.selection = true
    setTextSelectable(true)
  } else if (value === 'doodle') {
    top.selection = false
    setTextSelectable(false)
    top.isDrawingMode = true
    setDoodleBrush()
  } else if (value === 'mosaic') {
    top.selection = false
    setTextSelectable(false)
    top.isDrawingMode = false
    setupMosaicEvents()
  } else {
    top.selection = false
    setTextSelectable(false)
    top.isDrawingMode = false
  }

  top.discardActiveObject()
  top.requestRenderAll()
}

// ─── 裁剪 ──────────────────────────────────────────────

/** 合并底图+上层到一张临时图片，替换上层内容，清空底层 */
const mergeToTopForCrop = async () => {
  const bg = bgCanvas.value
  const top = fabricCanvas.value
  if (!bg || !top) return

  // 导出前重置 viewport，确保导出图片与画布逻辑坐标一致
  const bgVpt = bg.viewportTransform.slice()
  const topVpt = top.viewportTransform.slice()
  bg.setViewportTransform([1, 0, 0, 1, 0, 0])
  top.setViewportTransform([1, 0, 0, 1, 0, 0])

  const bgData = bg.toDataURL({ format: 'png' })
  const topData = top.toDataURL({ format: 'png' })

  return new Promise(resolve => {
    const offCanvas = document.createElement('canvas')
    // 使用 canvas 元素的实际像素尺寸，确保离屏 canvas 与导出数据一致
    offCanvas.width = bgCanvasRef.value.width
    offCanvas.height = bgCanvasRef.value.height
    const offCtx = offCanvas.getContext('2d')

    const bgImg = new Image()
    bgImg.onload = () => {
      offCtx.drawImage(bgImg, 0, 0)
      const fgImg = new Image()
      fgImg.onload = () => {
        offCtx.drawImage(fgImg, 0, 0)
        const merged = offCanvas.toDataURL('image/png')
        FabricImage.fromURL(merged).then(img => {
          bg.clear()
          top.clear()
          img.set({
            left: bg.width / 2,
            top: bg.height / 2,
            originX: 'center',
            originY: 'center',
            selectable: false,
            evented: false,
            name: '__crop_base'
          })
          top.add(img)
          // 恢复 viewport
          bg.setViewportTransform(bgVpt)
          top.setViewportTransform(topVpt)
          top.requestRenderAll()
          resolve()
        })
      }
      fgImg.src = topData
    }
    bgImg.src = bgData
  })
}

const initCrop = async () => {
  const top = fabricCanvas.value
  if (!top) return

  // 合并两层到上层
  await mergeToTopForCrop()

  // 找到合并后的底图
  const merged = top.getObjects().find(o => o.name === '__crop_base')
  if (!merged) return

  const left = merged.left - (merged.width * merged.scaleX) / 2
  const top_ = merged.top - (merged.height * merged.scaleY) / 2
  const width = merged.width * merged.scaleX
  const height = merged.height * merged.scaleY

  const { Rect } = await import('fabric')
  cropRect = new Rect({
    left, top: top_, width, height,
    fill: 'rgba(0,0,0,0.3)',
    stroke: '#fff',
    strokeWidth: 2,
    strokeDashArray: [6, 4],
    cornerColor: '#19c37d',
    borderColor: '#19c37d',
    transparentCorners: false,
    cornerSize: 10,
    lockRotation: true,
    selectable: true,
    hasControls: true,
    name: '__crop'
  })
  top.add(cropRect)
  top.setActiveObject(cropRect)
  top.requestRenderAll()
}

const setCropRatio = value => {
  cropRatio.value = value
  if (!cropRect || value === 'free') return
  const [w, h] = value.split(':').map(Number)
  cropRect.set('scaleX', 1)
  cropRect.set('scaleY', 1)
  cropRect.set('height', cropRect.width / (w / h))
  cropRect.setCoords()
  fabricCanvas.value.requestRenderAll()
}

const applyCrop = () => {
  const bg = bgCanvas.value
  const top = fabricCanvas.value
  if (!bg || !top || !cropRect) return

  // 保存裁剪框的 canvas 逻辑坐标
  const cropLeft = cropRect.left
  const cropTop = cropRect.top
  const cropW = cropRect.width * cropRect.scaleX
  const cropH = cropRect.height * cropRect.scaleY

  top.remove(cropRect)
  cropRect = null

  // 重置 viewport 后导出，确保导出像素与 canvas 逻辑坐标 1:1 对应
  const topVpt = top.viewportTransform.slice()
  top.setViewportTransform([1, 0, 0, 1, 0, 0])

  const mergedData = top.toDataURL({ format: 'png' })

  // 恢复 viewport
  top.setViewportTransform(topVpt)

  const img = new Image()
  img.onload = () => {
    const cropOff = document.createElement('canvas')
    cropOff.width = Math.round(cropW)
    cropOff.height = Math.round(cropH)
    const cropCtx = cropOff.getContext('2d')
    cropCtx.drawImage(img, cropLeft, cropTop, cropW, cropH, 0, 0, cropW, cropH)

    const dataUrl = cropOff.toDataURL('image/png')
    FabricImage.fromURL(dataUrl).then(newImg => {
      bg.clear()
      top.clear()
      newImg.set({
        name: '编辑图片',
        left: bg.width / 2,
        top: bg.height / 2,
        originX: 'center',
        originY: 'center',
        selectable: false,
        hasControls: false,
        evented: false,
        lockMovementX: true,
        lockMovementY: true,
        lockScalingX: true,
        lockScalingY: true,
        lockRotation: true
      })
      const s = Math.min(bg.width / newImg.width, bg.height / newImg.height, 1)
      newImg.scale(s)
      bg.add(newImg)
      bg.requestRenderAll()
      baseImage.value = newImg
      ElMessage.success('裁剪完成')
    })
  }
  img.src = mergedData
}

// ─── 调色（实时预览） ──────────────────────────────────

const buildFilters = () => {
  const f = []
  if (adjustments.brightness) f.push(new filters.Brightness({ brightness: adjustments.brightness / 200 }))
  if (adjustments.contrast) f.push(new filters.Contrast({ contrast: adjustments.contrast / 100 }))
  if (adjustments.saturation) f.push(new filters.Saturation({ saturation: adjustments.saturation / 100 }))
  if (adjustments.temperature) {
    const t = adjustments.temperature / 200
    f.push(new filters.BlendColor({ color: t > 0 ? '#ff9500' : '#0066ff', mode: 'overlay', alpha: Math.abs(t) }))
  }
  return f
}

const previewAdjustments = () => {
  if (!baseImage.value) return
  baseImage.value.filters = buildFilters()
  baseImage.value.applyFilters()
  bgCanvas.value.requestRenderAll()
}

// ─── 滤镜（实时预览） ──────────────────────────────────

const previewFilter = value => {
  activeFilter.value = value
  if (!baseImage.value) return
  baseImage.value.filters = [...(filterPresetsMap[value] || []), ...buildFilters()]
  baseImage.value.applyFilters()
  bgCanvas.value.requestRenderAll()
}

// ─── 文字 ──────────────────────────────────────────────

const setTextSelectable = selectable => {
  const top = fabricCanvas.value
  if (!top) return
  top.getObjects().forEach(obj => {
    if (obj.name === '文字' || obj.type === 'textbox' || obj.type === 'i-text') {
      obj.set({ selectable, evented: selectable, hasControls: selectable, editable: selectable })
    }
  })
  top.requestRenderAll()
}

const addText = async () => {
  const top = fabricCanvas.value
  if (!top || !textContent.value.trim()) return
  const { FabricText: FT } = await import('fabric')
  const text = new FT(textContent.value, {
    left: top.width / 2,
    top: top.height / 2,
    originX: 'center',
    originY: 'center',
    fontSize: textSize.value,
    fill: textColor.value,
    fontFamily: textFont.value,
    editable: true,
    selectable: true,
    hasControls: true,
    cornerColor: '#19c37d',
    borderColor: '#19c37d',
    transparentCorners: false,
    name: '文字'
  })
  top.add(text)
  top.setActiveObject(text)
  top.requestRenderAll()
}

const deleteTextTarget = target => {
  const top = fabricCanvas.value
  if (!top || !target) return
  top.remove(target)
  top.requestRenderAll()
  saveHistory()
}

const hasSelectedText = computed(() => {
  const top = fabricCanvas.value
  if (!top) return false
  const active = top.getActiveObject()
  return active && (active.name === '文字' || active.type === 'textbox' || active.type === 'i-text')
})

const deleteSelectedText = () => {
  const top = fabricCanvas.value
  if (!top) return
  const active = top.getActiveObject()
  if (active && (active.name === '文字' || active.type === 'textbox' || active.type === 'i-text')) {
    top.remove(active)
    top.discardActiveObject()
    top.requestRenderAll()
    saveHistory()
  }
}

// ─── 涂鸦笔 ────────────────────────────────────────────

const setDoodleBrush = () => {
  const top = fabricCanvas.value
  if (!top || activeTool.value !== 'doodle') return
  top.isDrawingMode = true

  // 移除旧的橡皮擦事件监听
  teardownEraserEvents()

  if (doodleMode.value === 'eraser') {
    class EraserBrush extends PencilBrush {
      _setBrushStyles(ctx) {
        super._setBrushStyles(ctx)
        ctx.globalCompositeOperation = 'destination-out'
      }
      createPath(pathData) {
        const path = super.createPath(pathData)
        path.globalCompositeOperation = 'destination-out'
        path.name = '橡皮'
        return path
      }
    }
    const brush = new EraserBrush(top)
    brush.width = doodleSize.value
    top.freeDrawingBrush = brush

    // 橡皮擦模式：擦过时删除非底图对象
    setupEraserEvents()
  } else {
    const brush = new PencilBrush(top)
    brush.width = doodleSize.value
    brush.color = doodleColor.value
    top.freeDrawingBrush = brush
  }
}

// 橡皮擦事件：检测并删除擦过的对象
const setupEraserEvents = () => {
  const top = fabricCanvas.value
  if (!top) return
  top.on('mouse:down', eraserCheckObject)
  top.on('mouse:move', eraserCheckObject)
}

const teardownEraserEvents = () => {
  const top = fabricCanvas.value
  if (!top) return
  top.off('mouse:down', eraserCheckObject)
  top.off('mouse:move', eraserCheckObject)
}

const eraserCheckObject = opt => {
  if (activeTool.value !== 'doodle' || doodleMode.value !== 'eraser') return
  const top = fabricCanvas.value
  if (!top) return
  const target = top.findTarget(opt.e, false)
  if (target && target.name !== '__crop_base' && target.name !== '__crop') {
    top.remove(target)
    top.requestRenderAll()
  }
}

// ─── 马赛克 ────────────────────────────────────────────

let mosaicDrawing = false

const setupMosaicEvents = () => {
  const top = fabricCanvas.value
  if (!top) return
  mosaicDrawing = false
  top.on('mouse:down', mosaicMouseDown)
  top.on('mouse:move', mosaicMouseMove)
  top.on('mouse:up', mosaicMouseUp)
}

const teardownMosaicEvents = () => {
  const top = fabricCanvas.value
  if (!top) return
  top.off('mouse:down', mosaicMouseDown)
  top.off('mouse:move', mosaicMouseMove)
  top.off('mouse:up', mosaicMouseUp)
}

const mosaicMouseDown = opt => {
  if (activeTool.value !== 'mosaic') return
  opt.e.preventDefault()
  opt.e.stopPropagation()
  mosaicDrawing = true
}

const mosaicMouseMove = opt => {
  if (!mosaicDrawing || activeTool.value !== 'mosaic') return
  opt.e.preventDefault()
  const pointer = fabricCanvas.value.getScenePoint(opt.e)
  const size = mosaicSize.value

  import('fabric').then(({ Rect }) => {
    const block = new Rect({
      left: pointer.x - size / 2,
      top: pointer.y - size / 2,
      width: size,
      height: size,
      fill: `hsl(${Math.random() * 360}, 10%, ${40 + Math.random() * 30}%)`,
      selectable: false,
      evented: false,
      name: '马赛克'
    })
    fabricCanvas.value.add(block)
    fabricCanvas.value.requestRenderAll()
  })
}

const mosaicMouseUp = () => {
  mosaicDrawing = false
}

// ─── History（只记录上层 canvas） ───────────────────────

const serialize = () => JSON.stringify(fabricCanvas.value.toJSON(['name']))
const resetHistory = () => {
  history.value = [serialize()]
  historyIndex.value = 0
}
const saveHistory = () => {
  if (restoring.value || !hasDocument.value) return
  const state = serialize()
  if (history.value[historyIndex.value] === state) return
  history.value = history.value.slice(0, historyIndex.value + 1)
  history.value.push(state)
  if (history.value.length > 30) history.value.shift()
  historyIndex.value = history.value.length - 1
}

const restoreState = async state => {
  restoring.value = true
  await fabricCanvas.value.loadFromJSON(state)
  setTextSelectable(activeTool.value === 'text')
  fabricCanvas.value.requestRenderAll()
  restoring.value = false
}

const undo = async () => {
  if (!canUndo.value) return
  historyIndex.value--
  await restoreState(history.value[historyIndex.value])
}

const redo = async () => {
  if (!canRedo.value) return
  historyIndex.value++
  await restoreState(history.value[historyIndex.value])
}

// ─── Zoom（同时缩放两层） ──────────────────────────────

const changeZoom = delta => {
  zoom.value = Math.min(2, Math.max(0.4, Number((zoom.value + delta).toFixed(1))))
  const pt = new Point((bgCanvas.value?.width || 0) / 2, (bgCanvas.value?.height || 0) / 2)
  bgCanvas.value?.zoomToPoint(pt, zoom.value)
  fabricCanvas.value?.zoomToPoint(pt, zoom.value)
}

const fitImage = () => {
  zoom.value = 1
  bgCanvas.value?.setViewportTransform([1, 0, 0, 1, 0, 0])
  fabricCanvas.value?.setViewportTransform([1, 0, 0, 1, 0, 0])
}

// ─── Export（合并两层） ─────────────────────────────────

const canvasDataUrl = () => {
  const format = exportFormat.value
  const bg = bgCanvas.value
  const top = fabricCanvas.value

  // 合并两层
  const offCanvas = document.createElement('canvas')
  offCanvas.width = bg.width
  offCanvas.height = bg.height
  const offCtx = offCanvas.getContext('2d')

  // 导出前重置 viewport，确保导出图片与画布逻辑坐标一致
  const bgVpt = bg.viewportTransform.slice()
  const topVpt = top.viewportTransform.slice()
  bg.setViewportTransform([1, 0, 0, 1, 0, 0])
  top.setViewportTransform([1, 0, 0, 1, 0, 0])

  const bgData = bg.toDataURL({ format: 'png' })
  const topData = top.toDataURL({ format: 'png' })

  // 恢复 viewport
  bg.setViewportTransform(bgVpt)
  top.setViewportTransform(topVpt)

  return new Promise(resolve => {
    const bgImg = new Image()
    bgImg.onload = () => {
      offCtx.drawImage(bgImg, 0, 0)
      const fgImg = new Image()
      fgImg.onload = () => {
        offCtx.drawImage(fgImg, 0, 0)
        resolve(offCanvas.toDataURL(format, format === 'png' ? 1 : exportQuality.value / 100))
      }
      fgImg.src = topData
    }
    bgImg.src = bgData
  })
}

const download = async () => {
  const extension = exportFormat.value === 'jpeg' ? 'jpg' : exportFormat.value
  const dataUrl = await canvasDataUrl()
  const link = document.createElement('a')
  link.download = `pixel-lab-${Date.now()}.${extension}`
  link.href = dataUrl
  link.click()
  exportVisible.value = false
  ElMessage.success('作品已导出')
}

const saveToProfile = async () => {
  saving.value = true
  try {
    const dataUrl = await canvasDataUrl()
    const blob = await fetch(dataUrl).then(response => response.blob())
    await uploadImage(new File([blob], `studio-${Date.now()}.png`, { type: blob.type || 'image/png' }))
    ElMessage.success('已保存到个人中心')
    exportVisible.value = false
  } finally {
    saving.value = false
  }
}

// ─── Keyboard ───────────────────────────────────────────

const onKeydown = event => {
  if (event.target instanceof HTMLInputElement || event.target instanceof HTMLTextAreaElement) return
  const key = event.key.toLowerCase()
  if ((event.ctrlKey || event.metaKey) && key === 'z') {
    event.preventDefault()
    event.shiftKey ? redo() : undo()
  } else if ((event.ctrlKey || event.metaKey) && key === 'y') {
    event.preventDefault()
    redo()
  } else if ((event.ctrlKey || event.metaKey) && key === 'e' && hasDocument.value) {
    event.preventDefault()
    exportVisible.value = true
  } else if (event.key === 'Escape') {
    if (activeTool.value) {
      cancelTool()
    }
  } else if (key === 'c' && hasDocument.value && !event.ctrlKey && !event.metaKey) setTool('crop')
  else if (key === 'a' && hasDocument.value && !event.ctrlKey && !event.metaKey) setTool('adjust')
  else if (key === 'f' && hasDocument.value) setTool('filter')
  else if (key === 't' && hasDocument.value) setTool('text')
  else if (key === 'd' && hasDocument.value) setTool('doodle')
  else if (key === 'm' && hasDocument.value) setTool('mosaic')
}

// ─── Lifecycle ──────────────────────────────────────────

onMounted(async () => {
  createCanvases()
  window.addEventListener('keydown', onKeydown)
  await nextTick()
  const pendingImage = localStorage.getItem('pixel_lab_workbench_image')
  if (pendingImage) {
    localStorage.removeItem('pixel_lab_workbench_image')
    await loadImageUrl(pendingImage, '导入图片')
  } else if (route.params.id) {
    try {
      const image = await getImageDetail(route.params.id)
      await loadImageUrl(image.url, image.title || image.original_name)
    } catch {
      ElMessage.error('无法加载指定作品')
    }
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
  teardownMosaicEvents()
  teardownEraserEvents()
  resizeObserver?.disconnect()
  bgCanvas.value?.dispose()
  fabricCanvas.value?.dispose()
})
</script>

<style scoped>
.studio-page { min-height: calc(100vh - 88px); background: var(--bg); }

.studio-header {
  height: 64px;
  display: flex;
  align-items: center;
  gap: var(--space-4);
  position: relative;
  z-index: 20;
  padding: 0 var(--space-6);
  border-bottom: 1px solid var(--border);
  background: var(--surface-glass);
  backdrop-filter: blur(18px);
}

.studio-brand { display: flex; align-items: center; gap: var(--space-2); color: var(--text-primary); text-decoration: none; font-size: 16px; }
.mini-mark { width: 24px; height: 24px; display: grid; grid-template-columns: 10px 10px; grid-template-rows: 10px 10px; gap: 2px; }
.mini-mark i { border-radius: 2px; background: var(--primary); }
.mini-mark i:nth-child(2) { opacity: 0.25; }
.studio-divider { width: 1px; height: 24px; background: var(--border-strong); }
.studio-header h1 { margin: 0; font-size: 17px; }
.history-actions { display: flex; gap: var(--space-1); position: absolute; left: 50%; transform: translateX(-50%); }
.document-actions { display: flex; gap: var(--space-2); margin-left: auto; }

.studio-workspace {
  height: calc(100vh - 64px - 88px);
  min-height: 560px;
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr) 300px;
}

.tool-rail {
  display: flex;
  align-items: stretch;
  flex-direction: column;
  gap: var(--space-2);
  border-right: 1px solid var(--border);
  padding: var(--space-4) var(--space-2);
  background: var(--card);
}

.tool-rail button {
  min-height: 58px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: var(--space-1);
  border: 1px solid transparent;
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  background: transparent;
  font-size: 11px;
  cursor: pointer;
  transition: transform var(--transition-fast), color var(--transition-fast), background var(--transition-fast), border-color var(--transition-fast);
}

.tool-rail button .el-icon { font-size: 21px; }
.tool-rail button:hover:not(:disabled) { transform: translateY(-2px); color: var(--primary-active); background: var(--primary-soft); }
.tool-rail button:active:not(:disabled) { transform: scale(0.98); }
.tool-rail button.active { border-color: var(--primary-border); color: var(--primary-active); background: var(--primary-soft); }
.tool-rail button:disabled { opacity: 0.35; cursor: not-allowed; }

.canvas-stage {
  min-width: 0;
  min-height: 0;
  position: relative;
  overflow: hidden;
  background-color: #f0f0f0;
}

.canvas-layer {
  position: absolute;
  inset: 0;
}
.canvas-layer :deep(canvas) { width: 100% !important; height: 100% !important; }

.empty-document { width: min(460px, calc(100% - 48px)); display: grid; justify-items: center; gap: var(--space-3); text-align: center; position: absolute; left: 50%; top: 50%; transform: translate(-50%, -50%); z-index: 10; }
.empty-document h2 { margin: 0; font-size: 22px; }
.empty-document p { margin: 0; color: var(--text-secondary); }
.empty-document > div { display: flex; gap: var(--space-2); margin-top: var(--space-2); }
.empty-icon { width: 58px; height: 58px; display: grid; place-items: center; border-radius: var(--radius-lg); color: var(--primary); background: var(--primary-soft); font-size: 26px; }

.zoom-controls {
  display: flex;
  align-items: center;
  position: absolute;
  left: var(--space-6);
  bottom: var(--space-4);
  z-index: 10;
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--surface-glass);
  box-shadow: var(--shadow-sm);
  backdrop-filter: blur(12px);
}
.zoom-controls button,
.zoom-controls span { width: 38px; height: 34px; display: grid; place-items: center; border: 0; color: var(--text-secondary); background: transparent; font-size: 11px; }
.zoom-controls button { cursor: pointer; }
.zoom-controls button:hover:not(:disabled) { color: var(--primary); background: var(--primary-soft); }
.zoom-controls button:disabled { opacity: .35; }

.inspector { min-width: 0; overflow: auto; border-left: 1px solid var(--border); background: var(--card); }
.inspector-section { padding: var(--space-4); border-bottom: 1px solid var(--border); }
.inspector-section header,
.control-row,
.adjustments label { display: flex; align-items: center; justify-content: space-between; gap: var(--space-3); }
.inspector-section h2 { margin: 0; font-size: 13px; }
.inspector-section header > span,
.inspector-section header > strong,
.control-row,
.adjustments label { color: var(--text-secondary); font-size: 11px; }
.inspector-section :deep(.el-slider) { --el-slider-main-bg-color: var(--primary); --el-slider-runway-bg-color: var(--surface-muted); }

.color-control { display: grid; grid-template-columns: 46px 1fr; gap: var(--space-2); }
.color-control input { height: 40px; border: 1px solid var(--border); border-radius: var(--radius-md); color: var(--text-primary); background: var(--card); }
.color-control input[type='color'] { width: 46px; padding: 4px; cursor: pointer; }
.color-control input[type='text'] { padding: 0 var(--space-3); font: 12px var(--font-mono); }

.adjustments { margin-top: var(--space-3); }
.adjustments label { margin-bottom: -4px; }

.tool-confirm-actions { display: flex; gap: var(--space-2); margin-top: var(--space-4); padding-top: var(--space-3); border-top: 1px solid var(--border); }

.filter-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: var(--space-2); margin-top: var(--space-3); }
.filter-item { display: grid; justify-items: center; gap: 4px; border: 1px solid var(--border); border-radius: var(--radius-md); padding: var(--space-2); background: var(--card); cursor: pointer; transition: border-color var(--transition-fast); }
.filter-item:hover { border-color: var(--primary-border); }
.filter-item.active { border-color: var(--primary); background: var(--primary-soft); }
.filter-item small { font-size: 10px; color: var(--text-secondary); }
.filter-thumb { width: 100%; aspect-ratio: 4/3; display: block; border-radius: var(--radius-sm); background: linear-gradient(135deg, #8bd3f7, #f6fbff 47%, #58a96d); }
.filter-preview-clear { filter: contrast(1.15) saturate(1.1); }
.filter-preview-warm { filter: sepia(.35) saturate(1.2); }
.filter-preview-cool { filter: sepia(.1) saturate(1.1) hue-rotate(20deg); }
.filter-preview-mono { filter: grayscale(1); }
.filter-preview-vintage { filter: sepia(.8) contrast(1.1); }
.filter-preview-film { filter: contrast(1.15) saturate(.8) brightness(.95); }
.filter-preview-japanese { filter: brightness(1.06) saturate(.85) contrast(.95); }
.filter-preview-food { filter: saturate(1.2) brightness(1.05) contrast(1.08); }
.filter-preview-painting { filter: contrast(1.25) saturate(1.15); }

.crop-ratio-list { display: grid; grid-template-columns: repeat(3, 1fr); gap: var(--space-2); }
.crop-ratio-list button { height: 34px; border: 1px solid var(--border); border-radius: var(--radius-md); background: var(--card); color: var(--text-secondary); font-size: 12px; cursor: pointer; }
.crop-ratio-list button.active { border-color: var(--primary); color: var(--primary-active); background: var(--primary-soft); }

.doodle-modes { display: grid; grid-template-columns: 1fr 1fr; gap: var(--space-2); }
.doodle-modes button { display: flex; align-items: center; justify-content: center; gap: var(--space-1); height: 38px; border: 1px solid var(--border); border-radius: var(--radius-md); background: var(--card); color: var(--text-secondary); font-size: 12px; cursor: pointer; }
.doodle-modes button.active { border-color: var(--primary); color: var(--primary-active); background: var(--primary-soft); }

.brush-presets { display: grid; grid-template-columns: repeat(4, 1fr); gap: var(--space-2); margin-top: var(--space-3); }
.brush-presets button { height: 38px; display: grid; place-items: center; border: 1px solid var(--border); border-radius: var(--radius-md); background: var(--card); cursor: pointer; }
.brush-presets button.active { border-color: var(--primary); background: var(--primary-soft); }
.brush-presets i { display: block; border-radius: 50%; background: var(--text-primary); }

.image-picker-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: var(--space-3); }
.image-picker-grid button { overflow: hidden; border: 1px solid var(--border); border-radius: var(--radius-md); padding: 0; background: var(--card); cursor: pointer; text-align: left; transition: transform var(--transition-fast), box-shadow var(--transition-fast); }
.image-picker-grid button:hover { transform: translateY(-2px); box-shadow: var(--shadow-md); }
.image-picker-grid img { width: 100%; aspect-ratio: 4 / 3; display: block; object-fit: cover; }
.image-picker-grid span { display: block; overflow: hidden; padding: var(--space-2); color: var(--text-primary); font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }
.export-form { display: grid; gap: var(--space-6); }
.export-form label { display: grid; gap: var(--space-2); }
.export-form label > span { font-size: 12px; font-weight: 680; }

@media (max-width: 1000px) {
  .studio-workspace { grid-template-columns: 74px minmax(0, 1fr); }
  .inspector { display: none; }
  .history-actions { position: static; transform: none; margin-left: auto; }
  .document-actions { margin-left: 0; }
}

@media (max-width: 680px) {
  .studio-header { padding-inline: var(--space-3); }
  .studio-brand strong,
  .studio-divider,
  .studio-header h1,
  .history-actions span { display: none; }
  .studio-workspace { height: calc(100vh - 64px - 82px); grid-template-columns: 1fr; grid-template-rows: 1fr 64px; }
  .tool-rail { grid-row: 2; flex-direction: row; border: 0; border-top: 1px solid var(--border); padding: var(--space-1) var(--space-2); }
  .tool-rail button { min-height: 52px; flex: 1; }
  .canvas-stage { grid-row: 1; }
  .zoom-controls { left: var(--space-3); bottom: var(--space-3); }
}
</style>
