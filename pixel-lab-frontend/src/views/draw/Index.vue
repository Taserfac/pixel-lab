<template>
  <div class="draw-page">
    <aside class="tool-rail">
      <div class="rail-group primary-tools">
        <el-tooltip content="画笔 (B)" placement="right">
          <button
            class="rail-btn primary-tool"
            :class="{ active: currentTool === 'brush' }"
            type="button"
            @click="selectTool('brush')"
          >
            <span class="btn-icon"><el-icon :size="22"><EditPen /></el-icon></span>
            <span class="btn-key">B</span>
          </button>
        </el-tooltip>

        <el-tooltip content="橡皮擦 (E)" placement="right">
          <button
            class="rail-btn primary-tool"
            :class="{ active: currentTool === 'eraser' }"
            type="button"
            @click="selectTool('eraser')"
          >
            <span class="btn-icon"><el-icon :size="22"><Delete /></el-icon></span>
            <span class="btn-key">E</span>
          </button>
        </el-tooltip>

        <el-tooltip content="移动画布 (H)" placement="right">
          <button
            class="rail-btn primary-tool"
            :class="{ active: currentTool === 'pan' }"
            type="button"
            @click="selectTool('pan')"
          >
            <span class="btn-icon"><el-icon :size="22"><Rank /></el-icon></span>
            <span class="btn-key">H</span>
          </button>
        </el-tooltip>
      </div>

      <div class="rail-group history-tools">
        <el-tooltip content="上一步 (Ctrl+Z)" placement="right">
          <button
            class="rail-btn history-tool"
            :class="{ disabled: !canUndo }"
            type="button"
            @click="undo"
          >
            <span class="btn-icon"><el-icon :size="21"><Back /></el-icon></span>
          </button>
        </el-tooltip>

        <el-tooltip content="下一步 (Ctrl+Y)" placement="right">
          <button
            class="rail-btn history-tool"
            :class="{ disabled: !canRedo }"
            type="button"
            @click="redo"
          >
            <span class="btn-icon"><el-icon :size="21"><Right /></el-icon></span>
          </button>
        </el-tooltip>
      </div>

      <div class="rail-group danger-tools">
        <el-popconfirm
          title="确定清空当前画布吗？"
          confirm-button-text="清空"
          cancel-button-text="取消"
          @confirm="clearCanvas"
        >
          <template #reference>
            <button class="rail-btn danger-tool" type="button">
              <span class="btn-icon"><el-icon :size="21"><RefreshRight /></el-icon></span>
              <span class="btn-key">清</span>
            </button>
          </template>
        </el-popconfirm>
      </div>
    </aside>

    <section class="studio">
      <header class="studio-header">
        <div class="studio-title">
          <span class="eyebrow">Pixel Lab</span>
          <h1>创意画布</h1>
        </div>

        <div class="header-actions">
          <div class="zoom-controls" aria-label="画布缩放">
            <el-tooltip content="缩小 (Ctrl + -)" placement="bottom">
              <button
                class="zoom-btn"
                type="button"
                :disabled="zoomLevel <= ZOOM_MIN"
                @click="zoomOut"
              >
                <el-icon><ZoomOut /></el-icon>
              </button>
            </el-tooltip>
            <button class="zoom-value" type="button" @click="resetZoom">
              {{ zoomLevel }}%
            </button>
            <el-tooltip content="放大 (Ctrl + +)" placement="bottom">
              <button
                class="zoom-btn"
                type="button"
                :disabled="zoomLevel >= ZOOM_MAX"
                @click="zoomIn"
              >
                <el-icon><ZoomIn /></el-icon>
              </button>
            </el-tooltip>
            <el-tooltip content="适配窗口" placement="bottom">
              <button class="zoom-btn" type="button" @click="fitCanvasToView">
                <el-icon><FullScreen /></el-icon>
              </button>
            </el-tooltip>
          </div>

          <el-button @click="openAiDialog">
            <el-icon><MagicStick /></el-icon>
            AI 创作
          </el-button>

          <el-dropdown trigger="click" @command="downloadImage">
            <el-button>
              <el-icon><Download /></el-icon>
              导出
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="transparent">PNG 透明背景</el-dropdown-item>
                <el-dropdown-item command="background">PNG 当前背景</el-dropdown-item>
                <el-dropdown-item command="jpeg">JPG 当前背景</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <el-button type="primary" @click="saveToGallery">
            <el-icon><Upload /></el-icon>
            保存
          </el-button>
        </div>
      </header>

      <div
        ref="canvasWrapperRef"
        class="canvas-stage"
        :class="{
          'show-grid': showGrid,
          'pan-tool': currentTool === 'pan',
          'space-pan': isSpacePressed,
          'is-panning': isPanning
        }"
        @wheel="handleCanvasWheel"
        @pointerdown.capture="handleCanvasPanStart"
        @pointermove="handleCanvasPanMove"
        @pointerup="stopCanvasPanning"
        @pointerleave="stopCanvasPanning"
      >
        <div class="canvas-viewport" :style="canvasViewportStyle">
          <div
            class="canvas-shell"
            :class="{ transparent: backgroundMode === 'transparent' }"
            :style="canvasShellStyle"
          >
            <canvas ref="canvasRef" />
            <div v-show="showGrid" class="canvas-grid-overlay" />
          </div>
        </div>
      </div>

      <footer class="status-bar">
        <span>{{ canvasWidth }} x {{ canvasHeight }}</span>
        <span>缩放 {{ zoomLevel }}%</span>
        <span>{{ currentToolLabel }}</span>
        <span>历史 {{ historyIndex + 1 }}/{{ historyCount }}</span>
        <span v-if="pointerPosition">x {{ pointerPosition.x }}, y {{ pointerPosition.y }}</span>
        <span class="hint">B 画笔 · E 橡皮 · H 移动 · Ctrl+滚轮 缩放</span>
      </footer>
    </section>

    <aside class="inspector">
      <section class="panel-section">
        <h2>工具</h2>
        <el-segmented
          v-model="currentTool"
          :options="toolOptions"
          @change="updateBrush"
        />
      </section>

      <section class="panel-section">
        <div class="section-row">
          <h2>画笔</h2>
          <span class="meter">{{ brushSize }}px</span>
        </div>
        <div v-show="currentTool === 'pan'" class="tool-note">
          左键拖动画布视图，不会修改画布内容。
        </div>
        <label v-show="currentTool === 'brush'" class="field">
          <span>类型</span>
          <el-select
            v-model="brushType"
            size="small"
            @change="updateBrush"
          >
            <el-option
              v-for="brush in brushTypes"
              :key="brush.value"
              :label="brush.label"
              :value="brush.value"
            />
          </el-select>
        </label>
        <label v-show="currentTool === 'eraser'" class="field">
          <span>类型</span>
          <el-select
            v-model="eraserType"
            size="small"
            @change="updateBrush"
          >
            <el-option
              v-for="eraser in eraserTypes"
              :key="eraser.value"
              :label="eraser.label"
              :value="eraser.value"
            />
          </el-select>
        </label>
        <label class="field">
          <span>大小</span>
          <el-slider
            v-model="brushSize"
            :min="1"
            :max="96"
            :disabled="currentTool === 'pan'"
            :show-tooltip="false"
          />
        </label>
        <label class="field">
          <span>透明度</span>
          <el-slider
            v-model="brushOpacity"
            :min="10"
            :max="100"
            :step="5"
            :disabled="currentTool === 'eraser' || currentTool === 'pan'"
            :show-tooltip="false"
          />
        </label>
        <div v-show="currentTool === 'brush'" class="field">
          <span>颜色</span>
          <div class="color-row">
            <el-color-picker
              v-model="brushColor"
              :predefine="presetColors"
              size="small"
            />
            <div class="quick-colors">
              <button
                v-for="color in quickColors"
                :key="color"
                class="color-dot"
                :class="{ active: brushColor === color }"
                :style="{ backgroundColor: color }"
                type="button"
                @click="brushColor = color"
              />
            </div>
          </div>
        </div>
      </section>

      <section class="panel-section">
        <h2>画布</h2>
        <label class="field">
          <span>预设</span>
          <el-select
            v-model="selectedPreset"
            size="small"
            @change="applyPreset"
          >
            <el-option
              v-for="preset in canvasPresets"
              :key="preset.value"
              :label="preset.label"
              :value="preset.value"
            />
          </el-select>
        </label>
        <div class="size-grid">
          <label class="field">
            <span>宽度</span>
            <el-input-number
              v-model="draftWidth"
              :min="128"
              :max="2400"
              :step="64"
              size="small"
              controls-position="right"
            />
          </label>
          <label class="field">
            <span>高度</span>
            <el-input-number
              v-model="draftHeight"
              :min="128"
              :max="2400"
              :step="64"
              size="small"
              controls-position="right"
            />
          </label>
        </div>
        <el-button class="full-action" @click="resizeCanvas">应用尺寸</el-button>
      </section>

      <section class="panel-section">
        <h2>背景</h2>
        <el-segmented
          v-model="backgroundMode"
          :options="backgroundOptions"
          @change="applyBackground"
        />
        <div v-if="backgroundMode === 'color'" class="field inline">
          <span>颜色</span>
          <el-color-picker
            v-model="backgroundColor"
            size="small"
            @change="applyBackground"
          />
        </div>
        <div class="field inline">
          <span>网格</span>
          <el-switch v-model="showGrid" />
        </div>
      </section>
    </aside>

    <el-dialog
      v-model="aiDialogVisible"
      title="AI 创作"
      width="min(1180px, calc(100vw - 40px))"
      class="ai-dialog"
      destroy-on-close
    >
      <div class="ai-refine">
        <section class="ai-form ai-panel">
          <label class="field">
            <span>任务</span>
            <el-segmented
              v-model="aiMode"
              :options="aiModeOptions"
              @change="handleAiModeChange"
            />
          </label>

          <label class="field">
            <span>{{ aiPromptLabel }}</span>
            <el-input
              v-model="aiPrompt"
              type="textarea"
              :rows="5"
              :maxlength="AI_PROMPT_LIMIT"
              show-word-limit
              :placeholder="aiPromptPlaceholder"
            />
          </label>

          <label class="field">
            <span>模型</span>
            <el-select v-model="aiModel">
              <el-option
                v-for="model in activeAiModels"
                :key="model.value"
                :label="model.label"
                :value="model.value"
              />
            </el-select>
          </label>

          <div class="field">
            <span>参考图/文件</span>
            <el-upload
              v-model:file-list="aiFileList"
              drag
              multiple
              :limit="6"
              :auto-upload="false"
              :on-exceed="handleAiFileExceed"
              accept="image/*,.txt,.md,.json"
            >
              <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
              <div class="el-upload__text">
                拖入图片或文本文件，或点击选择
              </div>
            </el-upload>
          </div>
        </section>

        <section class="preview-card canvas-preview-card">
          <div class="preview-title-row">
            <div class="preview-title">当前画布</div>
            <span class="preview-badge">{{ canvasWidth }} × {{ canvasHeight }}</span>
          </div>
          <div class="preview-frame current-preview-frame">
            <img v-if="aiPreviewUrl" :src="aiPreviewUrl" alt="当前画布预览">
            <div v-else class="ai-empty compact">打开后生成预览</div>
          </div>
        </section>

        <section class="preview-card ai-result-card">
          <div class="preview-title-row">
            <div class="preview-title">{{ aiResultTitle }}</div>
            <span class="preview-badge">{{ aiMode === 'draw' ? 'IMAGE' : 'TEXT' }}</span>
          </div>
          <div class="preview-frame result-preview-frame">
            <div v-if="aiLoading" class="ai-loading">{{ aiLoadingText }}</div>
            <img v-else-if="aiResultUrl" :src="aiResultUrl" alt="AI 创作结果">
            <div v-else-if="aiResultText" class="ai-text-wrap">
              <div v-if="aiResultNotice" class="ai-result-notice">{{ aiResultNotice }}</div>
              <div class="ai-text-result">{{ aiResultText }}</div>
            </div>
            <div v-else class="ai-empty">
              <el-icon><MagicStick /></el-icon>
              <span>生成后在这里预览</span>
            </div>
          </div>
        </section>
      </div>

      <template #footer>
        <div class="dialog-actions">
          <el-button class="dialog-btn" @click="aiDialogVisible = false">取消</el-button>
          <el-button
            v-if="aiResultUrl"
            class="dialog-btn"
            :disabled="!aiResultUrl"
            @click="applyAiResult"
          >
            应用到画布
          </el-button>
          <el-button
            class="dialog-btn"
            :disabled="!aiResultText"
            @click="useAiTextAsPrompt"
          >
            用作要求
          </el-button>
          <el-button
            class="dialog-btn primary-ai-btn"
            type="primary"
            :loading="aiLoading"
            @click="runAiRefine"
          >
            {{ aiActionLabel }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { EditPen, Delete, Back, Right, Download, Upload, RefreshRight, ArrowDown, MagicStick, UploadFilled, ZoomIn, ZoomOut, FullScreen, Rank } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { Canvas as FabricCanvas, CircleBrush, FabricImage, PencilBrush, Shadow, SprayBrush } from 'fabric'
import { uploadImage } from '@/api/image'
import { refineDrawing } from '@/api/ai'

const canvasRef = ref(null)
const canvasWrapperRef = ref(null)
let fabricCanvas = null
let isRestoring = false
const AI_PROMPT_LIMIT = 1500
const ZOOM_MIN = 25
const ZOOM_MAX = 400
const ZOOM_STEP = 25

const currentTool = ref('brush')
const brushType = ref('hard')
const eraserType = ref('hard')
const brushSize = ref(8)
const brushOpacity = ref(100)
const brushColor = ref('#111827')
const canvasWidth = ref(800)
const canvasHeight = ref(600)
const draftWidth = ref(800)
const draftHeight = ref(600)
const selectedPreset = ref('800x600')
const backgroundMode = ref('transparent')
const backgroundColor = ref('#ffffff')
const showGrid = ref(true)
const pointerPosition = ref(null)
const zoomLevel = ref(100)
const panOffset = ref({ x: 0, y: 0 })
const isSpacePressed = ref(false)
const isPanning = ref(false)
const aiDialogVisible = ref(false)
const aiMode = ref('refine')
const aiPrompt = ref('')
const aiModel = ref('deepseek-v4-flash')
const aiFileList = ref([])
const aiPreviewUrl = ref('')
const aiResultUrl = ref('')
const aiResultText = ref('')
const aiResultNotice = ref('')
const aiLoading = ref(false)

const toolOptions = [
  { label: '画笔', value: 'brush' },
  { label: '橡皮', value: 'eraser' },
  { label: '移动', value: 'pan' }
]

const brushTypes = [
  { label: '硬圆笔', value: 'hard' },
  { label: '软圆笔', value: 'soft' },
  { label: '马克笔', value: 'marker' },
  { label: '喷枪', value: 'spray' },
  { label: '散点笔', value: 'scatter' },
  { label: '像素笔', value: 'pixel' }
]

const eraserTypes = [
  { label: '硬橡皮', value: 'hard' },
  { label: '软橡皮', value: 'soft' },
  { label: '方块橡皮', value: 'block' },
  { label: '喷点橡皮', value: 'spray' }
]

const aiModeOptions = [
  { label: '润色建议', value: 'refine' },
  { label: '二次创作', value: 'draw' }
]

const aiModelGroups = {
  refine: [
    { label: 'DeepSeek V4 Flash - 润色建议', value: 'deepseek-v4-flash' },
    { label: 'DeepSeek V4 Pro - 润色建议', value: 'deepseek-v4-pro' }
  ],
  draw: [
    { label: 'DeepSeek V4 Flash - 绘图模型', value: 'deepseek-v4-flash-draw' },
    { label: 'DeepSeek V4 Pro - 绘图模型', value: 'deepseek-v4-pro-draw' }
  ]
}

const backgroundOptions = [
  { label: '透明', value: 'transparent' },
  { label: '白色', value: 'white' },
  { label: '自定义', value: 'color' }
]

const canvasPresets = [
  { label: '横向 800x600', value: '800x600', width: 800, height: 600 },
  { label: '方形 1024', value: '1024x1024', width: 1024, height: 1024 },
  { label: '竖向 768x1024', value: '768x1024', width: 768, height: 1024 },
  { label: '头像 512', value: '512x512', width: 512, height: 512 },
  { label: '自定义', value: 'custom', width: 800, height: 600 }
]

const presetColors = [
  '#111827', '#ffffff', '#ef4444', '#f97316', '#facc15',
  '#22c55e', '#06b6d4', '#3b82f6', '#8b5cf6', '#ec4899',
  '#64748b', '#a3a3a3'
]

const quickColors = ['#111827', '#ffffff', '#ef4444', '#f97316', '#facc15', '#22c55e', '#3b82f6', '#8b5cf6']

const historyList = ref([])
const historyIndex = ref(-1)
const maxHistory = 60
let panStart = null

const canUndo = computed(() => historyIndex.value > 0)
const canRedo = computed(() => historyIndex.value < historyList.value.length - 1)
const historyCount = computed(() => historyList.value.length)
const activeBrushName = computed(() => brushTypes.find((brush) => brush.value === brushType.value)?.label || '硬圆笔')
const activeEraserName = computed(() => eraserTypes.find((eraser) => eraser.value === eraserType.value)?.label || '硬橡皮')
const currentToolLabel = computed(() => {
  if (currentTool.value === 'pan') return '移动画布'
  return currentTool.value === 'eraser'
    ? `${activeEraserName.value} ${brushSize.value}px`
    : `${activeBrushName.value} ${brushSize.value}px / ${brushOpacity.value}%`
})
const activeAiModels = computed(() => aiModelGroups[aiMode.value] || aiModelGroups.refine)
const aiPromptLabel = computed(() => aiMode.value === 'draw' ? '创作要求' : '润色要求')
const aiPromptPlaceholder = computed(() => (
  aiMode.value === 'draw'
    ? '例如：基于当前草图生成一张赛博朋克角色头像，保留大致轮廓，加入霓虹光影和金属材质。'
    : '例如：保留构图，把线条清理干净，增加柔和光影，做成像素游戏角色头像风格。'
))
const aiActionLabel = computed(() => aiMode.value === 'draw' ? '开始创作' : '开始润色')
const aiLoadingText = computed(() => aiMode.value === 'draw' ? '创作中...' : '润色中...')
const aiResultTitle = computed(() => {
  if (aiMode.value !== 'draw') return 'AI 结果'
  return aiResultUrl.value ? '创作图片' : '创作提示词'
})
const zoomScale = computed(() => zoomLevel.value / 100)
const canvasViewportStyle = computed(() => ({
  width: `${Math.round(canvasWidth.value * zoomScale.value)}px`,
  height: `${Math.round(canvasHeight.value * zoomScale.value)}px`,
  transform: `translate(${panOffset.value.x}px, ${panOffset.value.y}px)`
}))

const canvasShellStyle = computed(() => {
  const bg = backgroundMode.value === 'transparent'
    ? 'transparent'
    : backgroundMode.value === 'white'
      ? '#ffffff'
      : backgroundColor.value

  return {
    width: `${canvasWidth.value}px`,
    height: `${canvasHeight.value}px`,
    backgroundColor: bg,
    transform: `scale(${zoomScale.value})`
  }
})

const refreshCanvasOffset = () => {
  nextTick(() => {
    fabricCanvas?.calcOffset()
  })
}

const syncFabricCanvasLayout = () => {
  if (!fabricCanvas) return

  const width = `${canvasWidth.value}px`
  const height = `${canvasHeight.value}px`
  const wrapper = fabricCanvas.wrapperEl
  const canvasElements = [
    fabricCanvas.lowerCanvasEl,
    fabricCanvas.upperCanvasEl,
    canvasRef.value
  ].filter(Boolean)

  if (wrapper) {
    wrapper.style.width = width
    wrapper.style.height = height
  }

  canvasElements.forEach((element) => {
    element.style.width = width
    element.style.height = height
  })

  fabricCanvas.calcOffset()
}

const initCanvas = async () => {
  if (!canvasRef.value || fabricCanvas) return

  await nextTick()

  fabricCanvas = new FabricCanvas(canvasRef.value, {
    width: canvasWidth.value,
    height: canvasHeight.value,
    backgroundColor: '',
    isDrawingMode: true,
    preserveObjectStacking: true
  })

  fabricCanvas.on('path:created', handlePathCreated)
  fabricCanvas.on('mouse:move', updatePointerPosition)
  fabricCanvas.on('mouse:out', () => {
    pointerPosition.value = null
  })

  syncFabricCanvasLayout()
  updateBrush()
  saveHistory('初始画布')
}

const selectTool = (tool) => {
  currentTool.value = tool
  updateBrush()
}

const getBrushColor = (opacity = brushOpacity.value) => {
  if (currentTool.value === 'eraser') return 'rgba(255, 255, 255, 1)'

  const hex = brushColor.value.replace('#', '')
  const bigint = parseInt(hex.length === 3 ? hex.split('').map((char) => char + char).join('') : hex, 16)
  const red = (bigint >> 16) & 255
  const green = (bigint >> 8) & 255
  const blue = bigint & 255
  return `rgba(${red}, ${green}, ${blue}, ${opacity / 100})`
}

const createBrush = () => {
  if (!fabricCanvas) return

  if (currentTool.value === 'eraser') {
    if (eraserType.value === 'spray') {
      const brush = new SprayBrush(fabricCanvas)
      brush.width = brushSize.value
      brush.color = 'rgba(255, 255, 255, 1)'
      brush.density = 36
      brush.dotWidth = Math.max(2, Math.round(brushSize.value / 6))
      brush.dotWidthVariance = Math.max(1, Math.round(brushSize.value / 4))
      brush.randomOpacity = true
      return brush
    }

    const brush = new PencilBrush(fabricCanvas)
    brush.width = brushSize.value
    brush.color = 'rgba(255, 255, 255, 1)'

    if (eraserType.value === 'soft') {
      brush.shadow = new Shadow({
        color: 'rgba(255, 255, 255, 0.9)',
        blur: Math.max(6, Math.round(brushSize.value * 0.7)),
        offsetX: 0,
        offsetY: 0
      })
    }

    if (eraserType.value === 'block') {
      brush.strokeLineCap = 'square'
      brush.strokeLineJoin = 'miter'
    }

    return brush
  }

  const color = getBrushColor()

  if (brushType.value === 'spray') {
    const brush = new SprayBrush(fabricCanvas)
    brush.width = brushSize.value
    brush.color = color
    brush.density = 28
    brush.dotWidth = Math.max(1, Math.round(brushSize.value / 7))
    brush.dotWidthVariance = Math.max(1, Math.round(brushSize.value / 5))
    brush.randomOpacity = true
    return brush
  }

  if (brushType.value === 'soft') {
    const brush = new PencilBrush(fabricCanvas)
    brush.width = brushSize.value
    brush.color = color
    brush.shadow = new Shadow({
      color,
      blur: Math.max(6, Math.round(brushSize.value * 0.65)),
      offsetX: 0,
      offsetY: 0
    })
    return brush
  }

  if (brushType.value === 'marker') {
    const brush = new PencilBrush(fabricCanvas)
    brush.width = brushSize.value
    brush.color = getBrushColor(Math.min(brushOpacity.value, 58))
    brush.strokeLineCap = 'round'
    brush.strokeLineJoin = 'round'
    return brush
  }

  if (brushType.value === 'scatter') {
    const brush = new CircleBrush(fabricCanvas)
    brush.width = brushSize.value
    brush.color = color
    return brush
  }

  if (brushType.value === 'pixel') {
    const brush = new PencilBrush(fabricCanvas)
    brush.width = brushSize.value
    brush.color = color
    brush.strokeLineCap = 'square'
    brush.strokeLineJoin = 'miter'
    return brush
  }

  const brush = new PencilBrush(fabricCanvas)
  brush.width = brushSize.value
  brush.color = color
  return brush
}

const updateBrush = () => {
  if (!fabricCanvas) return

  if (currentTool.value === 'pan') {
    fabricCanvas.isDrawingMode = false
    fabricCanvas.defaultCursor = 'grab'
    fabricCanvas.hoverCursor = 'grab'
    return
  }

  fabricCanvas.isDrawingMode = true
  fabricCanvas.defaultCursor = 'crosshair'
  fabricCanvas.hoverCursor = 'crosshair'
  fabricCanvas.freeDrawingBrush = createBrush()
}

const handlePathCreated = (event) => {
  if (!fabricCanvas || isRestoring) return

  if (currentTool.value === 'eraser' && event.path) {
    event.path.globalCompositeOperation = 'destination-out'
    event.path.stroke = '#ffffff'
    event.path.opacity = 1

    if (eraserType.value === 'soft') {
      event.path.shadow = new Shadow({
        color: 'rgba(255, 255, 255, 0.9)',
        blur: Math.max(6, Math.round(brushSize.value * 0.7)),
        offsetX: 0,
        offsetY: 0
      })
    }

    if (eraserType.value === 'block') {
      event.path.strokeLineCap = 'square'
      event.path.strokeLineJoin = 'miter'
    }

    fabricCanvas.requestRenderAll()
  } else if (currentTool.value === 'brush' && brushType.value === 'pixel' && event.path) {
    event.path.strokeLineCap = 'square'
    event.path.strokeLineJoin = 'miter'
    fabricCanvas.requestRenderAll()
  }

  saveHistory(currentTool.value === 'eraser' ? '擦除' : '绘制')
}

const updatePointerPosition = (event) => {
  if (!fabricCanvas) return
  const pointer = fabricCanvas.getScenePoint
    ? fabricCanvas.getScenePoint(event.e)
    : fabricCanvas.getPointer(event.e)
  pointerPosition.value = {
    x: Math.round(pointer.x),
    y: Math.round(pointer.y)
  }
}

const saveHistory = (name) => {
  if (!fabricCanvas || isRestoring) return

  historyList.value = historyList.value.slice(0, historyIndex.value + 1)
  historyList.value.push({
    name,
    json: fabricCanvas.toJSON(['globalCompositeOperation'])
  })

  if (historyList.value.length > maxHistory) {
    historyList.value.shift()
  }

  historyIndex.value = historyList.value.length - 1
}

const undo = () => {
  if (canUndo.value) restoreHistory(historyIndex.value - 1)
}

const redo = () => {
  if (canRedo.value) restoreHistory(historyIndex.value + 1)
}

const restoreHistory = async (index) => {
  if (!fabricCanvas || !historyList.value[index]) return

  isRestoring = true
  historyIndex.value = index
  await fabricCanvas.loadFromJSON(historyList.value[index].json)
  fabricCanvas.requestRenderAll()
  updateBrush()
  isRestoring = false
}

const clearCanvas = () => {
  if (!fabricCanvas) return

  fabricCanvas.clear()
  fabricCanvas.backgroundColor = ''
  fabricCanvas.renderAll()
  saveHistory('清空画布')
  ElMessage.success('画布已清空')
}

const applyPreset = (value) => {
  const preset = canvasPresets.find((item) => item.value === value)
  if (!preset || preset.value === 'custom') return

  draftWidth.value = preset.width
  draftHeight.value = preset.height
  resizeCanvas()
}

const resizeCanvas = () => {
  if (!fabricCanvas) return

  canvasWidth.value = draftWidth.value
  canvasHeight.value = draftHeight.value
  fabricCanvas.setDimensions({
    width: canvasWidth.value,
    height: canvasHeight.value
  })
  syncFabricCanvasLayout()
  fabricCanvas.requestRenderAll()
  selectedPreset.value = canvasPresets.some((item) => item.width === canvasWidth.value && item.height === canvasHeight.value)
    ? `${canvasWidth.value}x${canvasHeight.value}`
    : 'custom'
  panOffset.value = { x: 0, y: 0 }
  saveHistory('调整画布尺寸')
}

const applyBackground = () => {
  if (!fabricCanvas) return
  fabricCanvas.requestRenderAll()
}

const setZoomLevel = (value) => {
  const nextZoom = Math.min(ZOOM_MAX, Math.max(ZOOM_MIN, Math.round(value)))
  zoomLevel.value = nextZoom
  refreshCanvasOffset()
}

const zoomIn = () => {
  setZoomLevel(zoomLevel.value + ZOOM_STEP)
}

const zoomOut = () => {
  setZoomLevel(zoomLevel.value - ZOOM_STEP)
}

const resetZoom = () => {
  setZoomLevel(100)
  panOffset.value = { x: 0, y: 0 }
}

const fitCanvasToView = () => {
  const stage = canvasWrapperRef.value
  if (!stage) {
    resetZoom()
    return
  }

  const availableWidth = Math.max(1, stage.clientWidth - 64)
  const availableHeight = Math.max(1, stage.clientHeight - 64)
  const fitScale = Math.min(
    availableWidth / canvasWidth.value,
    availableHeight / canvasHeight.value
  )

  setZoomLevel(fitScale * 100)
  panOffset.value = { x: 0, y: 0 }
}

const handleCanvasWheel = (event) => {
  if (!event.ctrlKey && !event.metaKey) return

  event.preventDefault()
  if (event.deltaY < 0) {
    zoomIn()
  } else {
    zoomOut()
  }
}

const shouldStartCanvasPan = (event) => {
  return event.button === 1 ||
    (event.button === 0 && (isSpacePressed.value || currentTool.value === 'pan'))
}

const handleCanvasPanStart = (event) => {
  const stage = canvasWrapperRef.value
  if (!stage || !shouldStartCanvasPan(event)) return

  event.preventDefault()
  event.stopPropagation()

  isPanning.value = true
  panStart = {
    x: event.clientX,
    y: event.clientY,
    offsetX: panOffset.value.x,
    offsetY: panOffset.value.y
  }
  stage.setPointerCapture?.(event.pointerId)

  if (fabricCanvas) {
    fabricCanvas.isDrawingMode = false
    fabricCanvas.defaultCursor = 'grabbing'
    fabricCanvas.hoverCursor = 'grabbing'
  }
}

const moveCanvasPan = (event) => {
  const stage = canvasWrapperRef.value
  if (!stage || !isPanning.value || !panStart) return

  event.preventDefault()
  event.stopPropagation()

  panOffset.value = {
    x: panStart.offsetX + event.clientX - panStart.x,
    y: panStart.offsetY + event.clientY - panStart.y
  }
}

const handleCanvasPanMove = (event) => {
  moveCanvasPan(event)
}

const stopCanvasPanning = (event) => {
  const stage = canvasWrapperRef.value
  if (!isPanning.value) return

  event?.preventDefault?.()
  event?.stopPropagation?.()
  if (event?.pointerId !== undefined) {
    stage?.releasePointerCapture?.(event.pointerId)
  }

  isPanning.value = false
  panStart = null
  updateBrush()
  refreshCanvasOffset()
}

const handleGlobalPointerMove = (event) => {
  moveCanvasPan(event)
}

const handleGlobalPointerUp = (event) => {
  stopCanvasPanning(event)
}

const drawToCanvas = async (format = 'png', includeBackground = false, multiplier = 2) => {
  if (!fabricCanvas) return ''

  const drawingUrl = fabricCanvas.toDataURL({
    format: 'png',
    quality: 1,
    multiplier
  })

  if (!includeBackground || backgroundMode.value === 'transparent') {
    return drawingUrl
  }

  const tempCanvas = document.createElement('canvas')
  tempCanvas.width = canvasWidth.value * multiplier
  tempCanvas.height = canvasHeight.value * multiplier
  const ctx = tempCanvas.getContext('2d')
  const bg = backgroundMode.value === 'white' ? '#ffffff' : backgroundColor.value

  ctx.fillStyle = bg
  ctx.fillRect(0, 0, tempCanvas.width, tempCanvas.height)

  await new Promise((resolve) => {
    const image = new Image()
    image.onload = () => {
      ctx.drawImage(image, 0, 0)
      resolve()
    }
    image.src = drawingUrl
  })

  return tempCanvas.toDataURL(format === 'jpeg' ? 'image/jpeg' : 'image/png', format === 'jpeg' ? 0.92 : 1)
}

const dataUrlToFile = async (dataUrl, filename) => {
  const response = await fetch(dataUrl)
  const blob = await response.blob()
  return new File([blob], filename, { type: blob.type || 'image/png' })
}

const downloadImage = async (mode) => {
  if (!fabricCanvas) return

  const format = mode === 'jpeg' ? 'jpeg' : 'png'
  const includeBackground = mode !== 'transparent'
  const dataURL = await drawToCanvas(format, includeBackground)
  const link = document.createElement('a')
  link.download = `drawing_${Date.now()}.${format === 'jpeg' ? 'jpg' : 'png'}`
  link.href = dataURL
  link.click()
  ElMessage.success('导出成功')
}

const openAiDialog = async () => {
  if (!fabricCanvas) return

  aiPreviewUrl.value = await drawToCanvas('png', backgroundMode.value !== 'transparent', 1)
  aiResultUrl.value = ''
  aiResultText.value = ''
  aiResultNotice.value = ''
  aiDialogVisible.value = true
}

const handleAiFileExceed = () => {
  ElMessage.warning('最多上传 6 个参考文件')
}

const ensureAiModelForMode = () => {
  if (activeAiModels.value.some((model) => model.value === aiModel.value)) {
    return
  }

  aiModel.value = activeAiModels.value[0]?.value || 'deepseek-v4-flash'
}

const handleAiModeChange = () => {
  ensureAiModelForMode()
  aiResultUrl.value = ''
  aiResultText.value = ''
  aiResultNotice.value = ''
}

const extractReusablePrompt = (text) => {
  const source = text.trim()
  const codeBlocks = [...source.matchAll(/```(?:[a-zA-Z0-9_-]+)?\s*([\s\S]*?)```/g)]
    .map((match) => match[1].trim())
    .filter(Boolean)

  if (codeBlocks.length > 0) {
    return codeBlocks[codeBlocks.length - 1]
  }

  const promptSection = source.match(
    /(?:可直接复制的(?:图片润色|绘图)?提示词|正向提示词)[:：]?\s*([\s\S]*?)(?=\n\s*(?:\d+\.|负面提示词|Negative)|$)/
  )

  if (promptSection?.[1]?.trim()) {
    return promptSection[1].trim()
  }

  return source
}

const fitAiPromptLimit = (text) => {
  const normalizedText = text.trim()

  if (normalizedText.length <= AI_PROMPT_LIMIT) {
    return {
      text: normalizedText,
      truncated: false
    }
  }

  return {
    text: normalizedText.slice(0, AI_PROMPT_LIMIT).trim(),
    truncated: true
  }
}

const runAiRefine = async () => {
  if (!aiPrompt.value.trim()) {
    ElMessage.warning('请输入润色要求')
    return
  }

  if (!fabricCanvas) return

  aiLoading.value = true
  try {
    aiResultUrl.value = ''
    aiResultText.value = ''
    aiResultNotice.value = ''
    const previewUrl = aiPreviewUrl.value || await drawToCanvas('png', backgroundMode.value !== 'transparent', 1)
    const image = await dataUrlToFile(previewUrl, `canvas_${Date.now()}.png`)
    const attachments = aiFileList.value
      .map((item) => item.raw)
      .filter(Boolean)

    const result = await refineDrawing({
      image,
      prompt: aiPrompt.value,
      mode: aiMode.value,
      model: aiModel.value,
      attachments
    })

    aiResultUrl.value = result.imageUrl || ''
    aiResultText.value = result.text || ''
    aiResultNotice.value = result.notice || ''
    ElMessage.success(aiMode.value === 'draw' ? 'AI 创作完成' : 'AI 润色完成')
  } catch (error) {
    console.error('[Draw] AI refine failed:', error)
    if (!error?.response) {
      ElMessage.error(error?.message || 'AI 处理失败')
    }
  } finally {
    aiLoading.value = false
  }
}

const useAiTextAsPrompt = () => {
  if (!aiResultText.value) return
  const extractedPrompt = extractReusablePrompt(aiResultText.value)
  const limitedPrompt = fitAiPromptLimit(extractedPrompt)

  aiPrompt.value = limitedPrompt.text
  if (aiMode.value === 'refine') {
    aiMode.value = 'draw'
    ensureAiModelForMode()
  }

  const suffix = aiMode.value === 'draw' ? '，已切换到二次创作' : ''
  ElMessage.success(limitedPrompt.truncated
    ? `已提取提示词，并自动裁剪到 ${AI_PROMPT_LIMIT} 字${suffix}`
    : `已提取提示词到要求${suffix}`
  )
}

const loadImageElement = (src) => {
  return new Promise((resolve, reject) => {
    const image = new Image()
    image.crossOrigin = 'anonymous'
    image.onload = () => resolve(image)
    image.onerror = reject
    image.src = src
  })
}

const getAiOutputBackground = () => {
  if (backgroundMode.value === 'color') return backgroundColor.value
  return '#ffffff'
}

const getVisibleImageBounds = (ctx, width, height) => {
  const { data } = ctx.getImageData(0, 0, width, height)
  let minX = width
  let minY = height
  let maxX = -1
  let maxY = -1
  let visiblePixels = 0

  for (let y = 0; y < height; y += 1) {
    for (let x = 0; x < width; x += 1) {
      const alpha = data[(y * width + x) * 4 + 3]
      if (alpha <= 8) continue

      visiblePixels += 1
      if (x < minX) minX = x
      if (y < minY) minY = y
      if (x > maxX) maxX = x
      if (y > maxY) maxY = y
    }
  }

  if (maxX < 0 || maxY < 0) {
    return null
  }

  return {
    x: minX,
    y: minY,
    width: maxX - minX + 1,
    height: maxY - minY + 1,
    visiblePixels
  }
}

const getAiSourceRect = ({ stagingCtx, sourceWidth, sourceHeight }) => {
  const bounds = getVisibleImageBounds(stagingCtx, sourceWidth, sourceHeight)
  if (!bounds) {
    return { x: 0, y: 0, width: sourceWidth, height: sourceHeight }
  }

  const totalPixels = sourceWidth * sourceHeight
  const transparentRatio = 1 - bounds.visiblePixels / totalPixels
  const boundsAreaRatio = (bounds.width * bounds.height) / totalPixels
  const hasLargeEmptyEdge = bounds.x > 2 ||
    bounds.y > 2 ||
    bounds.x + bounds.width < sourceWidth - 2 ||
    bounds.y + bounds.height < sourceHeight - 2

  if (transparentRatio < 0.12 || (!hasLargeEmptyEdge && boundsAreaRatio > 0.92)) {
    return { x: 0, y: 0, width: sourceWidth, height: sourceHeight }
  }

  const padding = Math.max(0, Math.round(Math.min(sourceWidth, sourceHeight) * 0.015))
  const x = Math.max(0, bounds.x - padding)
  const y = Math.max(0, bounds.y - padding)
  const right = Math.min(sourceWidth, bounds.x + bounds.width + padding)
  const bottom = Math.min(sourceHeight, bounds.y + bounds.height + padding)

  return {
    x,
    y,
    width: Math.max(1, right - x),
    height: Math.max(1, bottom - y)
  }
}

const drawSourceRectCover = ({ ctx, image, sourceRect }) => {
  const scale = Math.max(
    canvasWidth.value / sourceRect.width,
    canvasHeight.value / sourceRect.height
  )
  const drawWidth = Math.round(sourceRect.width * scale)
  const drawHeight = Math.round(sourceRect.height * scale)
  const drawX = Math.round((canvasWidth.value - drawWidth) / 2)
  const drawY = Math.round((canvasHeight.value - drawHeight) / 2)

  ctx.drawImage(
    image,
    sourceRect.x,
    sourceRect.y,
    sourceRect.width,
    sourceRect.height,
    drawX,
    drawY,
    drawWidth,
    drawHeight
  )
}

const normalizeAiResultToCanvas = async (src) => {
  const image = await loadImageElement(src)
  const sourceWidth = image.naturalWidth || image.width
  const sourceHeight = image.naturalHeight || image.height
  const stagingCanvas = document.createElement('canvas')
  stagingCanvas.width = sourceWidth
  stagingCanvas.height = sourceHeight
  const stagingCtx = stagingCanvas.getContext('2d', { willReadFrequently: true })
  stagingCtx.clearRect(0, 0, sourceWidth, sourceHeight)
  stagingCtx.drawImage(image, 0, 0)

  const outputCanvas = document.createElement('canvas')
  outputCanvas.width = canvasWidth.value
  outputCanvas.height = canvasHeight.value
  const ctx = outputCanvas.getContext('2d')

  ctx.clearRect(0, 0, outputCanvas.width, outputCanvas.height)
  ctx.imageSmoothingEnabled = false
  ctx.fillStyle = getAiOutputBackground()
  ctx.fillRect(0, 0, outputCanvas.width, outputCanvas.height)

  const sourceRect = getAiSourceRect({ stagingCtx, sourceWidth, sourceHeight })
  drawSourceRectCover({ ctx, image, sourceRect })
  return outputCanvas.toDataURL('image/png')
}

const applyAiResult = async () => {
  if (!fabricCanvas || !aiResultUrl.value) return

  try {
    const normalizedUrl = await normalizeAiResultToCanvas(aiResultUrl.value)
    const image = await FabricImage.fromURL(
      normalizedUrl,
      { crossOrigin: 'anonymous' },
      {
        left: 0,
        top: 0,
        selectable: false,
        evented: false,
        imageSmoothing: false
      }
    )

    fabricCanvas.clear()
    fabricCanvas.backgroundColor = ''
    image.set({
      left: 0,
      top: 0,
      originX: 'left',
      originY: 'top',
      scaleX: canvasWidth.value / (image.width || canvasWidth.value),
      scaleY: canvasHeight.value / (image.height || canvasHeight.value),
      selectable: false,
      evented: false
    })
    fabricCanvas.add(image)
    fabricCanvas.requestRenderAll()
    saveHistory('应用 AI 润色')
    aiDialogVisible.value = false
    ElMessage.success('已应用到画布')
  } catch (error) {
    console.error('[Draw] Apply AI result failed:', error)
    ElMessage.error('应用 AI 结果失败')
  }
}

const saveToGallery = async () => {
  if (!fabricCanvas) return

  try {
    const dataURL = await drawToCanvas('png', backgroundMode.value !== 'transparent')
    const res = await fetch(dataURL)
    const blob = await res.blob()
    const file = new File([blob], `drawing_${Date.now()}.png`, { type: 'image/png' })
    await uploadImage(file)
    ElMessage.success('已保存到个人中心')
  } catch (error) {
    console.error('[Draw] Save failed:', error)
    ElMessage.error('保存失败，请确认后端服务和登录状态')
  }
}

const handleKeydown = (event) => {
  const target = event.target
  const isTyping = ['INPUT', 'TEXTAREA'].includes(target?.tagName) || target?.isContentEditable
  if (isTyping) return

  if (event.code === 'Space') {
    event.preventDefault()
    isSpacePressed.value = true
    return
  }

  if (event.ctrlKey || event.metaKey) {
    if (event.key.toLowerCase() === 'z') {
      event.preventDefault()
      undo()
    }
    if (event.key.toLowerCase() === 'y') {
      event.preventDefault()
      redo()
    }
    if (event.key === '=' || event.key === '+') {
      event.preventDefault()
      zoomIn()
    }
    if (event.key === '-' || event.key === '_') {
      event.preventDefault()
      zoomOut()
    }
    if (event.key === '0') {
      event.preventDefault()
      resetZoom()
    }
    return
  }

  if (event.key.toLowerCase() === 'b') selectTool('brush')
  if (event.key.toLowerCase() === 'e') selectTool('eraser')
  if (event.key.toLowerCase() === 'h') selectTool('pan')
}

const handleKeyup = (event) => {
  if (event.code === 'Space') {
    isSpacePressed.value = false
  }
}

const handleWindowBlur = () => {
  isSpacePressed.value = false
  isPanning.value = false
  panStart = null
  updateBrush()
}

watch([brushSize, brushColor, brushOpacity, currentTool, brushType, eraserType], updateBrush)

onMounted(() => {
  initCanvas()
  window.addEventListener('keydown', handleKeydown)
  window.addEventListener('keyup', handleKeyup)
  window.addEventListener('blur', handleWindowBlur)
  window.addEventListener('pointermove', handleGlobalPointerMove)
  window.addEventListener('pointerup', handleGlobalPointerUp)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('keyup', handleKeyup)
  window.removeEventListener('blur', handleWindowBlur)
  window.removeEventListener('pointermove', handleGlobalPointerMove)
  window.removeEventListener('pointerup', handleGlobalPointerUp)
  if (fabricCanvas) {
    fabricCanvas.dispose()
    fabricCanvas = null
  }
})
</script>

<style scoped>
.draw-page {
  height: calc(100vh - 64px - 124px);
  min-height: 0;
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr) 304px;
  background: var(--background);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.tool-rail {
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-2);
  background: var(--background-soft);
  border-right: 1px solid var(--border);
  overflow-y: auto;
  overflow-x: hidden;
  overscroll-behavior: contain;
  scrollbar-width: thin;
  scrollbar-color: var(--border-hover) transparent;
}

.rail-group {
  flex: 0 0 auto;
  display: grid;
  gap: var(--space-2);
  padding: 6px;
  background: var(--background);
  border: 1px solid var(--border);
  border-radius: var(--radius);
}

.tool-rail::-webkit-scrollbar,
.inspector::-webkit-scrollbar {
  width: 8px;
}

.tool-rail::-webkit-scrollbar-track,
.inspector::-webkit-scrollbar-track {
  background: transparent;
}

.tool-rail::-webkit-scrollbar-thumb,
.inspector::-webkit-scrollbar-thumb {
  border: 2px solid transparent;
  border-radius: 999px;
  background: var(--border-hover);
  background-clip: padding-box;
}

.rail-btn {
  position: relative;
  width: 48px;
  height: 48px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--background-card);
  color: var(--foreground-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition:
    color var(--transition-fast),
    background var(--transition-fast),
    border-color var(--transition-fast),
    box-shadow var(--transition-fast),
    transform var(--transition-fast);
}

.btn-icon {
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-key {
  position: absolute;
  right: 5px;
  bottom: 4px;
  min-width: 14px;
  height: 14px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.08);
  color: var(--foreground-subtle);
  font-family: var(--font-mono);
  font-size: 9px;
  line-height: 14px;
  text-align: center;
}

.rail-btn:hover {
  color: var(--primary);
  border-color: var(--primary);
  background: var(--primary-muted);
  transform: translateY(-1px);
}

.rail-btn.active {
  color: var(--background);
  background: var(--primary);
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-muted), 0 0 18px var(--primary-glow);
}

.rail-btn.active .btn-key {
  background: rgba(10, 10, 10, 0.2);
  color: var(--background);
}

.primary-tool {
  height: 52px;
}

.history-tool {
  height: 42px;
  background: transparent;
}

.danger-tool {
  width: 48px;
  height: 44px;
  color: var(--foreground-muted);
  background: rgba(255, 71, 87, 0.08);
  border-color: rgba(255, 71, 87, 0.22);
}

.danger-tool:hover {
  color: var(--error);
  background: rgba(255, 71, 87, 0.14);
  border-color: var(--error);
  box-shadow: 0 0 18px rgba(255, 71, 87, 0.16);
}

.rail-btn.disabled {
  opacity: 0.42;
  cursor: not-allowed;
}

.rail-btn.disabled:hover {
  color: var(--foreground-muted);
  border-color: var(--border);
  background: transparent;
  transform: none;
  box-shadow: none;
}

.studio {
  min-width: 0;
  min-height: 0;
  display: grid;
  grid-template-rows: 64px minmax(0, 1fr) 32px;
  background: var(--background);
}

.studio-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  padding: 0 var(--space-5);
  background: var(--background-soft);
  border-bottom: 1px solid var(--border);
}

.studio-title {
  min-width: 0;
}

.eyebrow {
  display: block;
  color: var(--foreground-subtle);
  font-size: 11px;
  line-height: 1;
}

.studio-title h1 {
  margin: 4px 0 0;
  color: var(--foreground);
  font-size: 18px;
  font-weight: 700;
  line-height: 1.1;
}

.header-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.zoom-controls {
  height: 38px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--background);
}

.zoom-btn,
.zoom-value {
  height: 28px;
  border: 0;
  border-radius: 6px;
  color: var(--foreground-muted);
  background: transparent;
  cursor: pointer;
  transition:
    color var(--transition-fast),
    background var(--transition-fast),
    box-shadow var(--transition-fast);
}

.zoom-btn {
  width: 30px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.zoom-value {
  min-width: 58px;
  padding: 0 8px;
  color: var(--foreground);
  font-family: var(--font-mono);
  font-size: 12px;
  font-weight: 800;
}

.zoom-btn:hover,
.zoom-value:hover {
  color: var(--primary);
  background: var(--primary-muted);
}

.zoom-btn:disabled {
  opacity: 0.38;
  cursor: not-allowed;
}

.zoom-btn:disabled:hover {
  color: var(--foreground-muted);
  background: transparent;
}

.canvas-stage {
  min-height: 0;
  display: block;
  padding: var(--space-6);
  overflow: hidden;
  overscroll-behavior: contain;
  touch-action: none;
  background: radial-gradient(circle at 50% 28%, rgba(0, 255, 136, 0.06), transparent 30%), var(--background-muted);
}

.canvas-stage.pan-tool,
.canvas-stage.pan-tool .canvas-shell canvas,
.canvas-stage.pan-tool .canvas-shell :deep(canvas),
.canvas-stage.space-pan,
.canvas-stage.space-pan .canvas-shell canvas,
.canvas-stage.space-pan .canvas-shell :deep(canvas) {
  cursor: grab;
}

.canvas-stage.is-panning,
.canvas-stage.is-panning .canvas-shell canvas,
.canvas-stage.is-panning .canvas-shell :deep(canvas) {
  cursor: grabbing;
  user-select: none;
}

.canvas-viewport {
  position: relative;
  flex: 0 0 auto;
  margin: auto;
  transition: transform 0.08s linear;
  will-change: transform;
}

.canvas-stage.is-panning .canvas-viewport {
  transition: none;
}

.canvas-shell {
  position: absolute;
  left: 0;
  top: 0;
  isolation: isolate;
  border: 1px solid var(--border-hover);
  border-radius: var(--radius-sm);
  box-shadow: var(--shadow-md);
  overflow: hidden;
  transform-origin: left top;
}

.canvas-shell.transparent {
  background-image: repeating-conic-gradient(#d7d7d7 0% 25%, #f1f1f1 0% 50%);
  background-size: 18px 18px;
}

.canvas-shell canvas {
  position: relative;
  z-index: 1;
  display: block;
  cursor: crosshair;
}

.canvas-shell :deep(.canvas-container) {
  position: relative !important;
  width: 100% !important;
  height: 100% !important;
  z-index: 2;
}

.canvas-shell :deep(.lower-canvas),
.canvas-shell :deep(.upper-canvas) {
  left: 0 !important;
  top: 0 !important;
  width: 100% !important;
  height: 100% !important;
}

.canvas-shell :deep(.lower-canvas) {
  z-index: 2;
}

.canvas-shell :deep(.upper-canvas) {
  z-index: 3;
  cursor: crosshair;
  touch-action: none;
}

.canvas-grid-overlay {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  background-image:
    linear-gradient(rgba(0, 0, 0, 0.11) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 0, 0, 0.11) 1px, transparent 1px),
    linear-gradient(rgba(0, 255, 136, 0.2) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 255, 136, 0.2) 1px, transparent 1px);
  background-size: 16px 16px, 16px 16px, 64px 64px, 64px 64px;
}

.status-bar {
  display: flex;
  align-items: center;
  gap: var(--space-5);
  padding: 0 var(--space-5);
  background: var(--background-soft);
  border-top: 1px solid var(--border);
  color: var(--foreground-muted);
  font-family: var(--font-mono);
  font-size: 11px;
  overflow: hidden;
}

.status-bar .hint {
  margin-left: auto;
  white-space: nowrap;
  opacity: 0.75;
}

.inspector {
  min-width: 0;
  min-height: 0;
  padding: var(--space-4);
  background: var(--background-soft);
  border-left: 1px solid var(--border);
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-width: thin;
  scrollbar-color: var(--border-hover) transparent;
}

.panel-section {
  padding: var(--space-4) 0;
  border-bottom: 1px solid var(--border);
}

.panel-section:first-child {
  padding-top: 0;
}

.panel-section:last-child {
  border-bottom: 0;
}

.panel-section h2,
.section-row h2 {
  margin: 0 0 var(--space-3);
  color: var(--foreground);
  font-size: 13px;
  font-weight: 700;
}

.section-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}

.meter {
  color: var(--foreground-muted);
  font-family: var(--font-mono);
  font-size: 11px;
}

.field {
  display: grid;
  gap: var(--space-2);
  margin-top: var(--space-3);
  color: var(--foreground-muted);
  font-size: 12px;
  font-weight: 600;
}

.field.inline {
  grid-template-columns: 1fr auto;
  align-items: center;
}

.tool-note {
  margin-top: var(--space-3);
  padding: var(--space-3);
  border: 1px solid var(--border);
  border-radius: 8px;
  color: var(--foreground-muted);
  background: var(--background);
  font-size: 12px;
  font-weight: 700;
  line-height: 1.6;
}

.size-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-3);
}

.color-row,
.quick-colors {
  display: flex;
  align-items: center;
}

.color-row {
  gap: var(--space-2);
}

.quick-colors {
  flex-wrap: wrap;
  gap: 5px;
}

.color-dot {
  width: 20px;
  height: 20px;
  border: 1px solid var(--border-hover);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: transform var(--transition-fast), border-color var(--transition-fast), box-shadow var(--transition-fast);
}

.color-dot:hover {
  transform: translateY(-1px);
}

.color-dot.active {
  border-color: var(--primary);
  box-shadow: 0 0 0 2px var(--primary-muted);
}

.full-action {
  width: 100%;
  margin-top: var(--space-3);
}

.inspector :deep(.el-segmented) {
  width: 100%;
}

.inspector :deep(.el-select),
.inspector :deep(.el-input-number) {
  width: 100%;
}

:deep(.ai-dialog) {
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--background-soft);
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.46);
  overflow: hidden;
}

:deep(.ai-dialog .el-dialog__header) {
  margin: 0;
  padding: 28px 36px 24px;
  border-bottom: 1px solid var(--border);
}

:deep(.ai-dialog .el-dialog__title) {
  color: var(--foreground);
  font-size: 22px;
  font-weight: 800;
  line-height: 1;
}

:deep(.ai-dialog .el-dialog__headerbtn) {
  top: 20px;
  right: 22px;
  width: 34px;
  height: 34px;
  border-radius: 8px;
  transition: background var(--transition-fast);
}

:deep(.ai-dialog .el-dialog__headerbtn:hover) {
  background: var(--background-muted);
}

:deep(.ai-dialog .el-dialog__body) {
  max-height: calc(100vh - 188px);
  padding: 28px 36px 24px;
  overflow: auto;
}

:deep(.ai-dialog .el-dialog__footer) {
  padding: 18px 36px 28px;
  border-top: 1px solid var(--border);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.015), transparent);
}

.ai-refine {
  display: grid;
  grid-template-columns: minmax(360px, 1fr) minmax(240px, 0.62fr) minmax(310px, 0.82fr);
  gap: 18px;
  align-items: stretch;
}

.ai-panel,
.preview-card {
  min-width: 0;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: color-mix(in srgb, var(--background-muted) 72%, transparent);
}

.ai-panel {
  padding: 18px;
}

.ai-form .field:first-child {
  margin-top: 0;
}

.ai-form :deep(.el-segmented) {
  height: 44px;
  padding: 3px;
  border-radius: 8px;
  background: var(--background);
}

.ai-form :deep(.el-segmented__item) {
  min-height: 36px;
  border-radius: 6px;
  font-size: 15px;
  font-weight: 800;
}

.ai-form :deep(.el-textarea__inner),
.ai-form :deep(.el-select__wrapper) {
  border-radius: 8px;
  background: var(--background);
  box-shadow: 0 0 0 1px var(--border) inset;
  color: var(--foreground);
}

.ai-form :deep(.el-textarea__inner) {
  min-height: 150px !important;
  padding: 14px 16px 28px;
  resize: vertical;
  font-size: 14px;
  line-height: 1.65;
}

.ai-form :deep(.el-input__count) {
  right: 12px;
  bottom: 8px;
  background: transparent;
  color: var(--foreground-subtle);
  font-family: var(--font-mono);
  font-weight: 700;
}

.ai-form :deep(.el-upload),
.ai-form :deep(.el-upload-dragger) {
  width: 100%;
}

.ai-form :deep(.el-upload-dragger) {
  height: 158px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 18px;
  border: 1px dashed var(--border-hover);
  border-radius: 8px;
  background: var(--background);
}

.ai-form :deep(.el-upload-dragger:hover) {
  border-color: var(--primary);
  background: var(--primary-muted);
}

.ai-form :deep(.el-icon--upload) {
  margin: 0 0 8px;
  color: var(--foreground-subtle);
  font-size: 42px;
}

.ai-form :deep(.el-upload__text) {
  max-width: 260px;
  color: var(--foreground-muted);
  font-size: 13px;
  font-weight: 700;
  line-height: 1.5;
}

.preview-card {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 12px;
  padding: 16px;
}

.preview-title-row {
  min-height: 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
}

.preview-title {
  color: var(--foreground);
  font-size: 13px;
  font-weight: 800;
}

.preview-badge {
  flex: 0 0 auto;
  padding: 3px 7px;
  border: 1px solid var(--border);
  border-radius: 999px;
  color: var(--foreground-subtle);
  background: var(--background);
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 800;
  line-height: 1.2;
}

.preview-frame {
  min-height: 470px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--background);
}

.current-preview-frame,
.preview-frame > img {
  background: repeating-conic-gradient(#d7d7d7 0% 25%, #f1f1f1 0% 50%) 50% / 18px 18px;
}

.preview-frame > img {
  width: 100%;
  height: 100%;
  min-height: 470px;
  object-fit: contain;
}

.ai-empty,
.ai-loading {
  width: calc(100% - 20px);
  height: calc(100% - 20px);
  min-height: 220px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  border: 1px dashed var(--border-hover);
  border-radius: 8px;
  color: var(--foreground-muted);
  background: color-mix(in srgb, var(--background-muted) 70%, transparent);
  font-size: 14px;
  font-weight: 700;
}

.ai-empty.compact {
  width: 100%;
  height: 100%;
  border: 0;
  background: transparent;
}

.ai-empty .el-icon {
  color: var(--primary);
  font-size: 34px;
  filter: drop-shadow(0 0 12px var(--primary-glow));
}

.ai-text-wrap {
  width: 100%;
  height: 100%;
  min-height: 0;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: var(--space-2);
}

.ai-result-notice {
  padding: var(--space-2) var(--space-3);
  border: 1px solid rgba(250, 204, 21, 0.25);
  border-radius: 8px;
  color: #facc15;
  background: rgba(250, 204, 21, 0.08);
  font-size: 12px;
  line-height: 1.5;
}

.ai-text-result {
  min-height: 0;
  height: 100%;
  overflow: auto;
  padding: 14px;
  border: 0;
  border-radius: 8px;
  color: var(--foreground);
  background: var(--background);
  font-size: 13px;
  line-height: 1.75;
  white-space: pre-wrap;
}

.dialog-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.dialog-btn {
  min-width: 104px;
  height: 42px;
  border-radius: 8px;
  font-weight: 800;
}

.primary-ai-btn {
  min-width: 128px;
  box-shadow: 0 0 18px var(--primary-glow);
}

@media (max-width: 1180px) {
  .draw-page {
    grid-template-columns: 72px minmax(0, 1fr);
    grid-template-rows: minmax(0, 1fr) auto;
  }

  .tool-rail {
    grid-row: 1 / 3;
  }

  .inspector {
    grid-column: 2;
    display: grid;
    grid-template-columns: repeat(4, minmax(160px, 1fr));
    gap: var(--space-4);
    padding: var(--space-3);
    border-left: 0;
    border-top: 1px solid var(--border);
  }

  .panel-section {
    padding: 0;
    border-bottom: 0;
  }
}

@media (max-width: 1080px) {
  :deep(.ai-dialog .el-dialog__body) {
    padding: 24px;
  }

  :deep(.ai-dialog .el-dialog__header),
  :deep(.ai-dialog .el-dialog__footer) {
    padding-left: 24px;
    padding-right: 24px;
  }

  .ai-refine {
    grid-template-columns: minmax(320px, 1fr) minmax(260px, 0.78fr);
  }

  .ai-result-card {
    grid-column: 1 / -1;
  }

  .result-preview-frame {
    min-height: 340px;
  }

  .result-preview-frame > img {
    min-height: 340px;
  }
}

@media (max-width: 920px) {
  .ai-refine {
    grid-template-columns: 1fr;
  }

  .preview-frame,
  .preview-frame > img {
    min-height: 360px;
  }
}

@media (max-width: 760px) {
  .draw-page {
    height: calc(100vh - 64px - 96px);
    min-height: 0;
    grid-template-columns: 56px minmax(0, 1fr);
  }

  .tool-rail {
    padding: var(--space-2) 4px;
  }

  .rail-group {
    padding: 4px;
  }

  .rail-btn {
    width: 40px;
    height: 40px;
  }

  .danger-tool {
    width: 40px;
  }

  .btn-key {
    display: none;
  }

  .studio {
    grid-template-rows: auto minmax(0, 1fr) 32px;
  }

  .studio-header {
    align-items: flex-start;
    flex-direction: column;
    padding: var(--space-3);
  }

  .header-actions {
    width: 100%;
    justify-content: flex-end;
  }

  .canvas-stage {
    align-items: flex-start;
    justify-content: flex-start;
    padding: var(--space-3);
  }

  .inspector {
    grid-template-columns: 1fr;
    max-height: 260px;
  }

  .status-bar {
    gap: var(--space-2);
    overflow-x: auto;
  }

  .status-bar .hint {
    margin-left: 0;
  }
}
</style>
