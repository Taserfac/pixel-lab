<template>
  <div class="draw-page">
    <header class="editor-topbar">
      <div class="document-area">
        <el-tooltip content="返回首页" placement="bottom">
          <button class="icon-button back-button" type="button" @click="goBack">
            <el-icon><ArrowLeft /></el-icon>
          </button>
        </el-tooltip>
        <el-input v-model="documentName" class="document-name" maxlength="40" aria-label="文件名" />
      </div>

      <div class="topbar-actions">
        <section class="action-group view-group" aria-label="视图操作">
          <span class="group-label">视图</span>
          <div class="zoom-controls">
            <el-tooltip content="缩小 (Ctrl + -)" placement="bottom">
              <button class="zoom-btn" type="button" :disabled="zoomLevel <= ZOOM_MIN" @click="zoomOut">
                <el-icon><ZoomOut /></el-icon>
              </button>
            </el-tooltip>
            <button class="zoom-value" type="button" @click="resetZoom">{{ zoomLevel }}%</button>
            <el-tooltip content="放大 (Ctrl + +)" placement="bottom">
              <button class="zoom-btn" type="button" :disabled="zoomLevel >= ZOOM_MAX" @click="zoomIn">
                <el-icon><ZoomIn /></el-icon>
              </button>
            </el-tooltip>
            <el-tooltip content="全屏" placement="bottom">
              <button class="zoom-btn" :class="{ active: isFullscreen }" type="button" @click="toggleFullscreen">
                <el-icon><FullScreen /></el-icon>
              </button>
            </el-tooltip>
          </div>
        </section>

        <section class="action-group canvas-actions" aria-label="画布操作">
          <span class="group-label">画布</span>
          <el-select v-model="selectedPreset" class="preset-select" size="small" @change="applyPreset">
            <el-option v-for="preset in canvasPresets" :key="preset.value" :label="preset.label" :value="preset.value" />
          </el-select>
          <el-input-number v-model="draftWidth" class="dimension-input" :min="128" :max="2400" :step="64" size="small" controls-position="right" aria-label="画布宽度" />
          <span class="dimension-separator">×</span>
          <el-input-number v-model="draftHeight" class="dimension-input" :min="128" :max="2400" :step="64" size="small" controls-position="right" aria-label="画布高度" />
          <el-button size="small" type="primary" @click="resizeCanvas">应用尺寸</el-button>
          <el-select v-model="backgroundMode" class="background-select" size="small" @change="applyBackground">
            <el-option v-for="option in backgroundOptions" :key="option.value" :label="option.label" :value="option.value" />
          </el-select>
          <el-color-picker v-if="backgroundMode === 'color'" v-model="backgroundColor" size="small" @change="applyBackground" />
          <label class="grid-toggle"><span>网格</span><el-switch v-model="showGrid" size="small" /></label>
        </section>

        <section class="action-group compact-group" aria-label="AI 操作">
          <span class="group-label">AI</span>
          <el-button size="small" @click="openAiDialog"><el-icon><MagicStick /></el-icon>AI 创作</el-button>
        </section>

        <section class="action-group compact-group file-actions" aria-label="文件操作">
          <span class="group-label">文件</span>
          <el-popconfirm title="确定清空当前画布吗？" confirm-button-text="清空" cancel-button-text="取消" @confirm="clearCanvas">
            <template #reference>
              <el-button size="small"><el-icon><RefreshRight /></el-icon>清空</el-button>
            </template>
          </el-popconfirm>
          <el-button size="small" type="primary" @click="saveToGallery"><el-icon><Upload /></el-icon>保存</el-button>
          <el-dropdown trigger="click" @command="downloadImage">
            <el-button size="small"><el-icon><Download /></el-icon>导出<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="transparent">PNG 透明背景</el-dropdown-item>
                <el-dropdown-item command="background">PNG 当前背景</el-dropdown-item>
                <el-dropdown-item command="jpeg">JPG 当前背景</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </section>
      </div>
    </header>

    <aside class="tool-rail" aria-label="工具选择">
      <div class="rail-title">工具选择</div>
      <div class="rail-group primary-tools">
        <el-tooltip content="画笔 (B)" placement="right">
          <button class="rail-btn" :class="{ active: currentTool === 'brush' && !isShapeToolActive }" type="button" @click="selectTool('brush')">
            <el-icon :size="21"><EditPen /></el-icon><span>画笔</span><kbd>B</kbd>
          </button>
        </el-tooltip>
        <el-tooltip content="橡皮 (E)" placement="right">
          <button class="rail-btn" :class="{ active: currentTool === 'eraser' }" type="button" @click="selectTool('eraser')">
            <el-icon :size="21"><Delete /></el-icon><span>橡皮</span><kbd>E</kbd>
          </button>
        </el-tooltip>
        <el-tooltip content="移动画布 (V)" placement="right">
          <button class="rail-btn" :class="{ active: currentTool === 'pan' }" type="button" @click="selectTool('pan')">
            <el-icon :size="21"><Rank /></el-icon><span>移动</span><kbd>V</kbd>
          </button>
        </el-tooltip>
      </div>

      <div class="rail-group shape-tools">
        <el-popover
          v-model:visible="shapeMenuVisible"
          placement="right-start"
          trigger="click"
          :width="160"
          popper-class="shape-menu-popper"
        >
          <template #reference>
            <button
              class="rail-btn shape-tool"
              :class="{ active: currentTool === 'brush' && shapeMenuBrushTypes.has(brushType) }"
              type="button"
            >
              <span class="tool-glyph">◇</span>
              <span>形状</span>
            </button>
          </template>
          <div class="shape-menu-options">
            <button
              v-for="shape in shapeMenuTools"
              :key="shape.value"
              class="shape-menu-option"
              :class="{ active: brushType === shape.value }"
              type="button"
              @click="selectDrawingTool(shape.value)"
            >
              <span class="tool-glyph">{{ shape.glyph }}</span>
              <span>{{ shape.label }}</span>
            </button>
          </div>
        </el-popover>

        <el-tooltip v-for="tool in directDrawingTools" :key="tool.value" :content="tool.label" placement="right">
          <button
            class="rail-btn shape-tool"
            :class="{ active: currentTool === 'brush' && brushType === tool.value }"
            type="button"
            @click="selectDrawingTool(tool.value)"
          >
            <span class="tool-glyph">{{ tool.glyph }}</span>
            <span>{{ tool.shortLabel }}</span>
          </button>
        </el-tooltip>
      </div>

      <div class="rail-group history-tools">
        <el-tooltip content="撤销 (Ctrl+Z)" placement="right">
          <button class="rail-btn history-tool" :disabled="!canUndo" type="button" @click="undo">
            <el-icon :size="20"><Back /></el-icon><span>撤销</span>
          </button>
        </el-tooltip>
        <el-tooltip content="重做 (Ctrl+Y)" placement="right">
          <button class="rail-btn history-tool" :disabled="!canRedo" type="button" @click="redo">
            <el-icon :size="20"><Right /></el-icon><span>重做</span>
          </button>
        </el-tooltip>
      </div>
    </aside>

    <section class="studio">
      <div
        ref="canvasWrapperRef"
        class="canvas-stage"
        :class="{ 'show-grid': showGrid, 'pan-tool': currentTool === 'pan', 'is-panning': isCanvasPanning }"
        @wheel="handleCanvasWheel"
        @pointerdown.capture="startCanvasPan"
        @pointermove="moveCanvasPan"
        @pointerup="endCanvasPan"
        @pointercancel="endCanvasPan"
      >
        <div class="shortcut-strip" aria-label="快捷操作">
          <span><kbd>Ctrl</kbd> + <kbd>Z</kbd> 撤销</span>
          <span><kbd>Ctrl</kbd> + <kbd>S</kbd> 保存</span>
          <span>滚轮缩放 · 中键拖动画布</span>
        </div>
        <div class="canvas-card" :style="canvasCardStyle">
          <div class="canvas-viewport" :style="canvasViewportStyle">
            <div class="canvas-shell" :class="{ transparent: backgroundMode === 'transparent' }" :style="canvasShellStyle">
              <canvas ref="canvasRef" />
              <div v-show="showGrid" class="canvas-grid-overlay" />
            </div>
          </div>
        </div>
      </div>

      <footer class="status-bar">
        <span>画布 {{ canvasWidth }} × {{ canvasHeight }}px</span>
        <span>缩放 {{ zoomLevel }}%</span>
        <span>工具 {{ currentToolLabel }}</span>
        <span v-if="pointerPosition">光标 {{ pointerPosition.x }}, {{ pointerPosition.y }}</span>
        <span class="status-history">历史 {{ historyIndex + 1 }}/{{ historyCount }}</span>
      </footer>
    </section>

    <aside class="inspector">
      <div class="inspector-heading">
        <div><span>属性面板</span><strong>{{ inspectorTitle }}</strong></div>
        <span v-if="currentToolShortcut" class="tool-shortcut">{{ currentToolShortcut }}</span>
      </div>

      <template v-if="currentTool === 'brush'">
        <section v-if="brushType === 'text'" class="inspector-section selection-controls">
          <div class="section-title">文字操作</div>
          <el-button class="delete-selection" type="danger" plain :disabled="!selectedText" @click="deleteSelectedText">
            <el-icon><Delete /></el-icon>
            删除选中文字
          </el-button>
          <p class="selection-hint">单击文字后拖动；按 Delete 或 Backspace 也可删除</p>
        </section>

        <section v-if="!isShapeToolActive" class="inspector-section">
          <div class="section-title">笔刷类型</div>
          <div class="type-button-grid">
            <button
              v-for="brush in brushTypes"
              :key="brush.value"
              class="type-choice"
              :class="{ active: brushType === brush.value }"
              type="button"
              @click="selectBrushType(brush.value)"
            >
              {{ brush.label }}
            </button>
          </div>
        </section>

        <section class="inspector-section">
          <div class="section-title">{{ isShapeToolActive ? '线条设置' : '画笔设置' }}</div>
          <label class="field"><span>大小 <em>{{ brushSize }}px</em></span><el-slider v-model="brushSize" :min="1" :max="96" :show-tooltip="false" @change="commitSelectedTextStyle" /></label>
        </section>

        <section class="inspector-section">
          <div class="section-title">颜色</div>
          <div class="field"><span>当前颜色</span><div class="color-control"><el-color-picker v-model="brushColor" :predefine="presetColors" @change="commitSelectedTextStyle" /><code>{{ brushColor }}</code></div></div>
          <div class="field"><span>色板</span><div class="quick-colors"><button v-for="color in quickColors" :key="color" class="color-dot" :class="{ active: brushColor === color }" :style="{ backgroundColor: color }" type="button" :aria-label="color" @click="selectBrushColor(color)" /></div></div>
          <div class="field"><span>最近颜色</span><div class="quick-colors recent-colors"><button v-for="color in recentColors" :key="color" class="color-dot" :style="{ backgroundColor: color }" type="button" :aria-label="color" @click="selectBrushColor(color)" /></div></div>
        </section>

        <section class="inspector-section">
          <div class="section-title">透明度</div>
          <label class="field"><span>透明度 <em>{{ brushOpacity }}%</em></span><el-slider v-model="brushOpacity" :min="10" :max="100" :step="5" :show-tooltip="false" @change="commitSelectedTextStyle" /></label>
        </section>
      </template>

      <template v-else-if="currentTool === 'eraser'">
        <section class="inspector-section">
          <div class="section-title">橡皮类型</div>
          <div class="type-button-grid">
            <button
              v-for="eraser in eraserTypes"
              :key="eraser.value"
              class="type-choice"
              :class="{ active: eraserType === eraser.value }"
              type="button"
              @click="selectEraserType(eraser.value)"
            >
              {{ eraser.label }}
            </button>
          </div>
        </section>
        <section class="inspector-section">
          <div class="section-title">橡皮设置</div>
          <label class="field"><span>大小 <em>{{ eraserSize }}px</em></span><el-slider v-model="eraserSize" :min="1" :max="96" :show-tooltip="false" /></label>
        </section>
      </template>

      <div v-else-if="currentTool === 'pan'" class="tool-help">
        <el-icon><Rank /></el-icon><strong>移动画布</strong><p>按住并拖动画布，可调整画布与外框在工作区中的位置。</p>
      </div>
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

    <el-dialog
      v-model="saveDialogVisible"
      title="保存到个人中心"
      width="min(520px, 92vw)"
      :close-on-click-modal="!savingToGallery"
      :close-on-press-escape="!savingToGallery"
    >
      <el-form label-position="top" class="save-form">
        <el-form-item label="标题">
          <el-input v-model="saveForm.title" maxlength="100" show-word-limit placeholder="给作品起一个标题" />
        </el-form-item>
        <el-form-item label="标签">
          <el-select
            v-model="saveForm.tags"
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
            v-model="saveForm.description"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="写下作品的创作想法或说明"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="savingToGallery" @click="saveDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="savingToGallery"
          :disabled="!saveForm.title.trim()"
          @click="confirmSaveToGallery"
        >
          确认保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRouter } from 'vue-router'
import { ArrowDown, ArrowLeft, Back, Delete, Download, EditPen, FullScreen, MagicStick, Rank, RefreshRight, Right, Upload, UploadFilled, ZoomIn, ZoomOut } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Canvas as FabricCanvas, Circle, CircleBrush, Control, FabricImage, IText, Line, Path, PencilBrush, Rect, Shadow, SprayBrush } from 'fabric'
import { updateImageMetadata, uploadImage } from '@/api/image'
import { refineDrawing } from '@/api/ai'

const router = useRouter()
const canvasRef = ref(null)
const canvasWrapperRef = ref(null)
let fabricCanvas = null
let isRestoring = false
let shapeDraft = null
const AI_PROMPT_LIMIT = 1500
const ZOOM_MIN = 25
const ZOOM_MAX = 400
const ZOOM_STEP = 25
const DRAFT_STORAGE_KEY = 'pixel-lab:creative-canvas-draft:v1'

const documentName = ref('未命名作品')
const currentTool = ref('brush')
const brushType = ref('hard')
const eraserType = ref('hard')
const drawingToolStyles = reactive({
  brush: { size: 8, opacity: 100, color: '#111827' },
  shape: { size: 8, opacity: 100, color: '#111827' },
  line: { size: 8, opacity: 100, color: '#111827' },
  arrow: { size: 8, opacity: 100, color: '#111827' },
  text: { size: 8, opacity: 100, color: '#111827' }
})
const activeDrawingStyleKey = computed(() => {
  if (['rect', 'rect-fill', 'circle', 'circle-fill'].includes(brushType.value)) return 'shape'
  if (['line', 'arrow', 'text'].includes(brushType.value)) return brushType.value
  return 'brush'
})
const activeDrawingStyle = computed(() => drawingToolStyles[activeDrawingStyleKey.value])
const brushSize = computed({
  get: () => activeDrawingStyle.value.size,
  set: value => { activeDrawingStyle.value.size = value }
})
const brushOpacity = computed({
  get: () => activeDrawingStyle.value.opacity,
  set: value => { activeDrawingStyle.value.opacity = value }
})
const brushColor = computed({
  get: () => activeDrawingStyle.value.color,
  set: value => { activeDrawingStyle.value.color = value }
})
const eraserSize = ref(24)
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
const isCanvasPanning = ref(false)
const shapeMenuVisible = ref(false)
const aiDialogVisible = ref(false)
const aiMode = ref('refine')
const aiPrompt = ref('')
const aiModel = ref('qwen-plus')
const aiFileList = ref([])
const aiPreviewUrl = ref('')
const aiResultUrl = ref('')
const aiResultText = ref('')
const aiResultNotice = ref('')
const aiLoading = ref(false)
const isFullscreen = ref(false)
const hasUnsavedChanges = ref(false)
const recentColors = ref(['#111827', '#ffffff', '#3b82f6', '#ef4444'])
const selectedText = ref(null)
const saveDialogVisible = ref(false)
const savingToGallery = ref(false)
const saveForm = reactive({
  title: '',
  tags: [],
  description: ''
})


const brushTypes = [
  { label: '硬圆笔', value: 'hard' },
  { label: '软圆笔', value: 'soft' },
  { label: '马克笔', value: 'marker' },
  { label: '喷枪', value: 'spray' },
  { label: '散点笔', value: 'scatter' },
  { label: '像素笔', value: 'pixel' }
]

const shapeMenuTools = [
  { label: '空心矩形', value: 'rect', glyph: '□' },
  { label: '实心矩形', value: 'rect-fill', glyph: '■' },
  { label: '空心圆形', value: 'circle', glyph: '○' },
  { label: '实心圆形', value: 'circle-fill', glyph: '●' }
]

const directDrawingTools = [
  { label: '直线', shortLabel: '直线', value: 'line', glyph: '╱' },
  { label: '箭头', shortLabel: '箭头', value: 'arrow', glyph: '↗' },
  { label: '文字', shortLabel: '文字', value: 'text', glyph: 'T' }
]

const shapeTools = [...shapeMenuTools, ...directDrawingTools]
const shapeBrushTypes = new Set(shapeTools.map((shape) => shape.value))
const shapeMenuBrushTypes = new Set(shapeMenuTools.map((shape) => shape.value))

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
    { label: 'Qwen Plus - 润色建议', value: 'qwen-plus' },
    { label: 'Qwen Max - 润色建议', value: 'qwen-max' }
  ],
  draw: [
    { label: 'Qwen Image Edit - 结果图', value: 'qwen-image-edit' }
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

const canUndo = computed(() => historyIndex.value > 0)
const canRedo = computed(() => historyIndex.value < historyList.value.length - 1)
const historyCount = computed(() => historyList.value.length)
const isShapeToolActive = computed(() => currentTool.value === 'brush' && shapeBrushTypes.has(brushType.value))
const activeBrushName = computed(() => (
  brushTypes.find((brush) => brush.value === brushType.value)?.label ||
  shapeTools.find((shape) => shape.value === brushType.value)?.label ||
  '硬圆笔'
))
const activeEraserName = computed(() => eraserTypes.find((eraser) => eraser.value === eraserType.value)?.label || '硬橡皮')
const currentToolLabel = computed(() => {
  if (currentTool.value === 'pan') return '移动画布'
  if (currentTool.value === 'eraser') return activeEraserName.value + ' ' + eraserSize.value + 'px'
  return activeBrushName.value + ' ' + brushSize.value + 'px / ' + brushOpacity.value + '%'
})
const inspectorTitle = computed(() => {
  if (isShapeToolActive.value) return activeBrushName.value
  return ({ brush: '画笔', eraser: '橡皮', pan: '移动' }[currentTool.value] || '工具')
})
const currentToolShortcut = computed(() => (
  isShapeToolActive.value ? '' : ({ brush: 'B', eraser: 'E', pan: 'V' }[currentTool.value] || '')
))
const effectiveBrushWidth = computed(() => brushSize.value)
const effectiveEraserWidth = computed(() => eraserSize.value)
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
  height: `${Math.round(canvasHeight.value * zoomScale.value)}px`
}))

const canvasCardStyle = computed(() => ({
  transform: 'translate(' + panOffset.value.x + 'px, ' + panOffset.value.y + 'px)'
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
    preserveObjectStacking: true,
    perPixelTargetFind: true,
    targetFindTolerance: 0
  })

  fabricCanvas.on('path:created', handlePathCreated)
  fabricCanvas.on('mouse:down', handleEraserPointerDown)
  fabricCanvas.on('mouse:move', handleEraserPointerMove)
  fabricCanvas.on('mouse:up', handleEraserPointerUp)
  fabricCanvas.on('mouse:down', handleShapePointerDown)
  fabricCanvas.on('mouse:move', handleShapePointerMove)
  fabricCanvas.on('mouse:up', handleShapePointerUp)
  fabricCanvas.on('mouse:move', updatePointerPosition)
  fabricCanvas.on('mouse:out', () => {
    pointerPosition.value = null
  })
  fabricCanvas.on('selection:created', syncSelectedText)
  fabricCanvas.on('selection:updated', syncSelectedText)
  fabricCanvas.on('selection:cleared', () => { selectedText.value = null })
  fabricCanvas.on('object:modified', handleTextModified)

  syncFabricCanvasLayout()
  updateBrush()
  saveHistory('初始画布')
}

const goBack = () => {
  router.push('/dashboard')
}

const selectBrushColor = (color) => {
  brushColor.value = color
  nextTick(commitSelectedTextStyle)
}

const toggleFullscreen = async () => {
  if (!document.fullscreenElement) {
    await document.documentElement.requestFullscreen?.()
  } else {
    await document.exitFullscreen?.()
  }
}

const syncFullscreenState = () => {
  isFullscreen.value = Boolean(document.fullscreenElement)
}
const selectTool = (tool) => {
  if (tool === 'brush' && shapeBrushTypes.has(brushType.value)) {
    brushType.value = 'hard'
  }
  currentTool.value = tool
  updateBrush()
  nextTick(refreshCanvasOffset)
}

const selectDrawingTool = (type) => {
  brushType.value = type
  currentTool.value = 'brush'
  shapeMenuVisible.value = false
  updateBrush()
}

const selectBrushType = (type) => {
  brushType.value = type
  updateBrush()
}

const selectEraserType = (type) => {
  eraserType.value = type
  updateBrush()
}

let panStart = null
const startCanvasPan = (event) => {
  const isMiddleButton = event.button === 1
  const isMoveToolDrag = currentTool.value === 'pan' && event.button === 0
  if (!isMiddleButton && !isMoveToolDrag) return

  event.preventDefault()
  event.stopPropagation()
  event.currentTarget.setPointerCapture?.(event.pointerId)
  panStart = {
    pointerX: event.clientX,
    pointerY: event.clientY,
    offsetX: panOffset.value.x,
    offsetY: panOffset.value.y
  }
  isCanvasPanning.value = true
}

const moveCanvasPan = (event) => {
  if (!isCanvasPanning.value || !panStart) return
  event.preventDefault()
  panOffset.value = {
    x: panStart.offsetX + event.clientX - panStart.pointerX,
    y: panStart.offsetY + event.clientY - panStart.pointerY
  }
}

const endCanvasPan = (event) => {
  if (!isCanvasPanning.value) return
  event.currentTarget.releasePointerCapture?.(event.pointerId)
  isCanvasPanning.value = false
  panStart = null
  refreshCanvasOffset()
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

  const color = getBrushColor()

  if (brushType.value === 'spray') {
    const brush = new SprayBrush(fabricCanvas)
    brush.width = effectiveBrushWidth.value
    brush.color = color
    brush.density = 28
    brush.dotWidth = Math.max(1, Math.round(brushSize.value / 7))
    brush.dotWidthVariance = Math.max(1, Math.round(brushSize.value / 5))
    brush.randomOpacity = true
    return brush
  }

  if (brushType.value === 'soft') {
    const brush = new PencilBrush(fabricCanvas)
    brush.width = effectiveBrushWidth.value
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
    brush.width = effectiveBrushWidth.value
    brush.color = getBrushColor(Math.min(brushOpacity.value, 58))
    brush.strokeLineCap = 'round'
    brush.strokeLineJoin = 'round'
    return brush
  }

  if (brushType.value === 'scatter') {
    const brush = new CircleBrush(fabricCanvas)
    brush.width = effectiveBrushWidth.value
    brush.color = color
    return brush
  }

  if (brushType.value === 'pixel') {
    const brush = new PencilBrush(fabricCanvas)
    brush.width = effectiveBrushWidth.value
    brush.color = color
    brush.strokeLineCap = 'square'
    brush.strokeLineJoin = 'miter'
    return brush
  }

  const brush = new PencilBrush(fabricCanvas)
  brush.width = effectiveBrushWidth.value
  brush.color = color
  return brush
}

const getEventPoint = (event) => {
  if (!fabricCanvas) return null
  return fabricCanvas.getScenePoint ? fabricCanvas.getScenePoint(event.e) : fabricCanvas.getPointer(event.e)
}

const getShapeStyles = (filled = false) => ({
  fill: filled ? getBrushColor() : 'transparent',
  stroke: getBrushColor(),
  strokeWidth: effectiveBrushWidth.value,
  strokeLineCap: 'round',
  strokeLineJoin: 'round',
  strokeUniform: true,
  globalCompositeOperation: 'source-over',
  selectable: false,
  evented: false,
  perPixelTargetFind: true,
  padding: 0
})

const createShapeObject = (start, end) => {
  const width = Math.abs(end.x - start.x)
  const height = Math.abs(end.y - start.y)

  if (brushType.value === 'rect' || brushType.value === 'rect-fill') {
    return new Rect({
      left: start.x,
      top: start.y,
      originX: 'center',
      originY: 'center',
      width: width * 2,
      height: height * 2,
      ...getShapeStyles(brushType.value === 'rect-fill')
    })
  }

  if (brushType.value === 'circle' || brushType.value === 'circle-fill') {
    const radius = Math.hypot(end.x - start.x, end.y - start.y)
    return new Circle({
      left: start.x,
      top: start.y,
      originX: 'center',
      originY: 'center',
      radius,
      ...getShapeStyles(brushType.value === 'circle-fill')
    })
  }

  if (brushType.value === 'line') {
    return new Line([start.x, start.y, end.x, end.y], getShapeStyles())
  }

  if (brushType.value === 'arrow') {
    const angle = Math.atan2(end.y - start.y, end.x - start.x)
    const arrowSize = Math.max(10, effectiveBrushWidth.value * 4)
    const wingA = {
      x: end.x - arrowSize * Math.cos(angle - Math.PI / 6),
      y: end.y - arrowSize * Math.sin(angle - Math.PI / 6)
    }
    const wingB = {
      x: end.x - arrowSize * Math.cos(angle + Math.PI / 6),
      y: end.y - arrowSize * Math.sin(angle + Math.PI / 6)
    }
    const path = 'M ' + start.x + ' ' + start.y +
      ' L ' + end.x + ' ' + end.y +
      ' M ' + end.x + ' ' + end.y +
      ' L ' + wingA.x + ' ' + wingA.y +
      ' M ' + end.x + ' ' + end.y +
      ' L ' + wingB.x + ' ' + wingB.y
    return new Path(path, getShapeStyles())
  }

  return null
}

const renderDeleteTextControl = (ctx, left, top, _styleOverride, object) => {
  ctx.save()
  ctx.translate(left, top)
  ctx.rotate((object.angle || 0) * Math.PI / 180)
  ctx.beginPath()
  ctx.arc(0, 0, 11, 0, Math.PI * 2)
  ctx.fillStyle = '#ef4444'
  ctx.fill()
  ctx.strokeStyle = '#ffffff'
  ctx.lineWidth = 2
  ctx.lineCap = 'round'
  ctx.beginPath()
  ctx.moveTo(-4, -4)
  ctx.lineTo(4, 4)
  ctx.moveTo(4, -4)
  ctx.lineTo(-4, 4)
  ctx.stroke()
  ctx.restore()
}

const getSelectedTextObject = () => {
  const activeObject = fabricCanvas?.getActiveObject()
  if (activeObject instanceof IText) return activeObject
  return selectedText.value instanceof IText ? selectedText.value : null
}

const removeTextObject = (text) => {
  if (!fabricCanvas || !(text instanceof IText)) return false
  fabricCanvas.remove(text)
  fabricCanvas.discardActiveObject()
  selectedText.value = null
  fabricCanvas.requestRenderAll()
  saveHistory('删除文字')
  return true
}

const configureTextObject = (text) => {
  if (!(text instanceof IText)) return
  text.set({
    hasControls: true,
    borderColor: '#13c77f',
    cornerColor: '#ffffff',
    cornerStrokeColor: '#13c77f',
    cornerStyle: 'circle',
    transparentCorners: false,
    cornerSize: 10
  })
  text.controls = { ...text.controls, mtr: new Control({
    x: 0.5,
    y: -0.5,
    offsetX: 12,
    offsetY: -12,
    cursorStyle: 'pointer',
    cornerSize: 24,
    mouseUpHandler: (_eventData, transform) => removeTextObject(transform.target),
    render: renderDeleteTextControl
  }) }
  text.setCoords()
}

const applySelectedTextStyle = () => {
  if (currentTool.value !== 'brush' || brushType.value !== 'text') return
  const text = getSelectedTextObject()
  if (!text) return
  text.set({
    fill: getBrushColor(),
    fontSize: Math.max(12, effectiveBrushWidth.value * 2)
  })
  text.setCoords()
  selectedText.value = text
  fabricCanvas?.requestRenderAll()
}

const commitSelectedTextStyle = () => {
  if (getSelectedTextObject() && currentTool.value === 'brush' && brushType.value === 'text') {
    saveHistory('调整文字属性')
  }
}

const addEditableText = (point) => {
  if (!fabricCanvas) return

  const text = new IText('输入文字', {
    left: point.x,
    top: point.y,
    fill: getBrushColor(),
    fontFamily: 'Microsoft YaHei, sans-serif',
    fontSize: Math.max(12, effectiveBrushWidth.value * 2),
    globalCompositeOperation: 'source-over',
    selectable: true,
    evented: true
  })

  configureTextObject(text)

  let historySaved = false
  text.once('editing:exited', () => {
    if (!fabricCanvas || historySaved) return
    historySaved = true
    if (!text.text?.trim()) fabricCanvas.remove(text)
    selectedText.value = text.text?.trim() ? text : null
    fabricCanvas.requestRenderAll()
    saveHistory('添加文字')
  })

  fabricCanvas.add(text)
  fabricCanvas.setActiveObject(text)
  text.enterEditing()
  text.selectAll()
  fabricCanvas.requestRenderAll()
}

let eraserSession = null

const drawRoundEraser = (ctx, start, end, width, alpha = 1) => {
  ctx.globalAlpha = alpha
  ctx.lineWidth = width
  ctx.lineCap = 'round'
  ctx.lineJoin = 'round'
  ctx.beginPath()
  ctx.moveTo(start.x, start.y)
  ctx.lineTo(end.x, end.y)
  ctx.stroke()
  ctx.beginPath()
  ctx.arc(end.x, end.y, width / 2, 0, Math.PI * 2)
  ctx.fill()
}

const drawEraserSegment = (start, end) => {
  if (!fabricCanvas) return
  const ctx = fabricCanvas.contextContainer
  ctx.save()
  ctx.globalCompositeOperation = 'destination-out'
  ctx.fillStyle = '#000000'
  ctx.strokeStyle = '#000000'

  if (eraserType.value === 'spray') {
    const radius = effectiveEraserWidth.value / 2
    for (let index = 0; index < 32; index += 1) {
      const angle = Math.random() * Math.PI * 2
      const distance = Math.sqrt(Math.random()) * radius
      const size = Math.max(1, effectiveEraserWidth.value / 10)
      ctx.globalAlpha = 0.45 + Math.random() * 0.55
      ctx.fillRect(
        end.x + Math.cos(angle) * distance - size / 2,
        end.y + Math.sin(angle) * distance - size / 2,
        size,
        size
      )
    }
  } else if (eraserType.value === 'soft') {
    drawRoundEraser(ctx, start, end, effectiveEraserWidth.value * 1.45, 0.16)
    drawRoundEraser(ctx, start, end, effectiveEraserWidth.value, 0.32)
    drawRoundEraser(ctx, start, end, effectiveEraserWidth.value * 0.55, 0.62)
  } else if (eraserType.value === 'block') {
    ctx.globalAlpha = 1
    ctx.lineWidth = effectiveEraserWidth.value
    ctx.lineCap = 'square'
    ctx.lineJoin = 'miter'
    ctx.beginPath()
    ctx.moveTo(start.x, start.y)
    ctx.lineTo(end.x, end.y)
    ctx.stroke()
    const half = effectiveEraserWidth.value / 2
    ctx.fillRect(end.x - half, end.y - half, effectiveEraserWidth.value, effectiveEraserWidth.value)
  } else {
    drawRoundEraser(ctx, start, end, effectiveEraserWidth.value)
  }

  ctx.restore()
}

const handleEraserPointerDown = (event) => {
  if (!fabricCanvas || currentTool.value !== 'eraser') return
  const point = getEventPoint(event)
  if (!point) return
  fabricCanvas.renderAll()
  eraserSession = { lastPoint: point }
  drawEraserSegment(point, point)
}

const handleEraserPointerMove = (event) => {
  if (!eraserSession || currentTool.value !== 'eraser') return
  const point = getEventPoint(event)
  if (!point) return
  drawEraserSegment(eraserSession.lastPoint, point)
  eraserSession.lastPoint = point
}

const handleEraserPointerUp = async () => {
  if (!fabricCanvas || !eraserSession) return
  eraserSession = null

  try {
    const erasedUrl = fabricCanvas.lowerCanvasEl.toDataURL('image/png')
    const image = await FabricImage.fromURL(erasedUrl)
    image.set({
      left: 0,
      top: 0,
      originX: 'left',
      originY: 'top',
      scaleX: canvasWidth.value / (image.width || canvasWidth.value),
      scaleY: canvasHeight.value / (image.height || canvasHeight.value),
      selectable: false,
      evented: false,
      imageSmoothing: true
    })

    isRestoring = true
    fabricCanvas.clear()
    fabricCanvas.add(image)
    fabricCanvas.requestRenderAll()
    isRestoring = false
    saveHistory('擦除')
  } catch (error) {
    isRestoring = false
    fabricCanvas.requestRenderAll()
    console.error('[Draw] Eraser commit failed:', error)
    ElMessage.error('擦除结果保存失败')
  }
}

const handleShapePointerDown = (event) => {
  if (!fabricCanvas || currentTool.value !== 'brush' || !shapeBrushTypes.has(brushType.value)) return
  const point = getEventPoint(event)
  if (!point) return
  if (brushType.value === 'text') {
    if (event.target instanceof IText) {
      if (event.target.isEditing) event.target.exitEditing()
      configureTextObject(event.target)
      fabricCanvas.setActiveObject(event.target)
      selectedText.value = event.target
      fabricCanvas.requestRenderAll()
      return
    }
    const activeObject = fabricCanvas.getActiveObject()
    if (activeObject?.isEditing) {
      activeObject.exitEditing()
      fabricCanvas.discardActiveObject()
      fabricCanvas.requestRenderAll()
      return
    }
    addEditableText(point)
    return
  }
  shapeDraft = { start: point, object: null }
}

const handleShapePointerMove = (event) => {
  if (!fabricCanvas || !shapeDraft || currentTool.value !== 'brush') return
  const point = getEventPoint(event)
  if (!point) return
  if (shapeDraft.object) fabricCanvas.remove(shapeDraft.object)
  shapeDraft.object = createShapeObject(shapeDraft.start, point)
  if (shapeDraft.object) fabricCanvas.add(shapeDraft.object)
  fabricCanvas.requestRenderAll()
}

const handleShapePointerUp = (event) => {
  if (!fabricCanvas || !shapeDraft) return
  const point = getEventPoint(event)
  const distance = point ? Math.hypot(point.x - shapeDraft.start.x, point.y - shapeDraft.start.y) : 0
  if (!shapeDraft.object || distance < 2) {
    if (shapeDraft.object) fabricCanvas.remove(shapeDraft.object)
    shapeDraft = null
    fabricCanvas.requestRenderAll()
    return
  }
  shapeDraft.object.setCoords()
  shapeDraft = null
  fabricCanvas.requestRenderAll()
  saveHistory('绘制图形')
}

const updateBrush = () => {
  if (!fabricCanvas) return

  const isMoveTool = currentTool.value === 'pan'
  const isEraserTool = currentTool.value === 'eraser'
  const isShapeTool = currentTool.value === 'brush' && shapeBrushTypes.has(brushType.value)
  const isTextTool = currentTool.value === 'brush' && brushType.value === 'text'

  fabricCanvas.selection = false
  fabricCanvas.perPixelTargetFind = !isTextTool
  fabricCanvas.targetFindTolerance = isTextTool ? 6 : 0
  fabricCanvas.forEachObject((object) => {
    const isEditableText = isTextTool && object instanceof IText
    if (object instanceof IText) configureTextObject(object)
    object.set({
      selectable: isEditableText,
      evented: isEditableText,
      perPixelTargetFind: false,
      padding: 0,
      hoverCursor: isEditableText ? 'move' : 'default'
    })
  })

  const activeObject = fabricCanvas.getActiveObject()
  if (!(isTextTool && activeObject instanceof IText)) {
    fabricCanvas.discardActiveObject()
    selectedText.value = null
  }

  if (isMoveTool || isEraserTool) {
    fabricCanvas.isDrawingMode = false
    fabricCanvas.defaultCursor = isMoveTool ? 'grab' : 'crosshair'
    fabricCanvas.hoverCursor = fabricCanvas.defaultCursor
    fabricCanvas.requestRenderAll()
    return
  }

  fabricCanvas.isDrawingMode = !isShapeTool
  fabricCanvas.defaultCursor = 'crosshair'
  fabricCanvas.hoverCursor = 'crosshair'
  if (!isShapeTool) fabricCanvas.freeDrawingBrush = createBrush()
  fabricCanvas.requestRenderAll()
}

const colorChannelToHex = value => Math.round(Number(value)).toString(16).padStart(2, '0')

const syncTextStyleControls = (text) => {
  if (!(text instanceof IText)) return
  const fill = String(text.fill || '#111827')
  const rgba = fill.match(/^rgba?\(\s*([\d.]+)\s*,\s*([\d.]+)\s*,\s*([\d.]+)(?:\s*,\s*([\d.]+))?\s*\)$/i)
  if (rgba) {
    brushColor.value = `#${colorChannelToHex(rgba[1])}${colorChannelToHex(rgba[2])}${colorChannelToHex(rgba[3])}`
    brushOpacity.value = Math.round((rgba[4] === undefined ? 1 : Number(rgba[4])) * 100)
  } else if (/^#[0-9a-f]{6}$/i.test(fill)) {
    brushColor.value = fill
    brushOpacity.value = Math.round((text.opacity ?? 1) * 100)
  }
  brushSize.value = Math.max(1, Math.min(96, Math.round((text.fontSize || 16) / 2)))
}

const syncSelectedText = () => {
  const activeObject = fabricCanvas?.getActiveObject()
  selectedText.value = activeObject instanceof IText ? activeObject : null
  if (selectedText.value) {
    configureTextObject(selectedText.value)
    syncTextStyleControls(selectedText.value)
  }
}

const handleTextModified = (event) => {
  if (isRestoring || !(event.target instanceof IText)) return
  selectedText.value = event.target
  saveHistory('移动文字')
}

const deleteSelectedText = () => {
  removeTextObject(getSelectedTextObject())
}

const handlePathCreated = (event) => {
  if (!fabricCanvas || isRestoring || currentTool.value !== 'brush' || !event.path) return

  event.path.globalCompositeOperation = 'source-over'
  event.path.set({ selectable: false, evented: false, perPixelTargetFind: false, padding: 0 })

  if (brushType.value === 'pixel') {
    event.path.strokeLineCap = 'square'
    event.path.strokeLineJoin = 'miter'
  }

  fabricCanvas.requestRenderAll()
  saveHistory('绘制')
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
    json: fabricCanvas.toJSON(['globalCompositeOperation', 'isEraserStroke'])
  })

  if (historyList.value.length > maxHistory) {
    historyList.value.shift()
  }

  historyIndex.value = historyList.value.length - 1
  if (name !== '初始画布') hasUnsavedChanges.value = true
}

const createDraftPayload = () => ({
  version: 1,
  savedAt: Date.now(),
  documentName: documentName.value,
  canvasWidth: canvasWidth.value,
  canvasHeight: canvasHeight.value,
  backgroundMode: backgroundMode.value,
  backgroundColor: backgroundColor.value,
  showGrid: showGrid.value,
  drawingToolStyles: Object.fromEntries(
    Object.entries(drawingToolStyles).map(([key, style]) => [key, { ...style }])
  ),
  canvasJson: fabricCanvas?.toJSON(['globalCompositeOperation', 'isEraserStroke'])
})

const clearStoredDraft = () => {
  localStorage.removeItem(DRAFT_STORAGE_KEY)
}

const saveDraftLocally = (markSaved = true) => {
  if (!fabricCanvas) return false

  try {
    localStorage.setItem(DRAFT_STORAGE_KEY, JSON.stringify(createDraftPayload()))
    if (markSaved) hasUnsavedChanges.value = false
    return true
  } catch (error) {
    console.error('[Draw] Save draft failed:', error)
    return false
  }
}

const readStoredDraft = () => {
  try {
    const rawDraft = localStorage.getItem(DRAFT_STORAGE_KEY)
    if (!rawDraft) return null
    const draft = JSON.parse(rawDraft)
    if (draft?.version !== 1 || !draft.canvasJson) throw new Error('Invalid draft data')
    return draft
  } catch (error) {
    console.error('[Draw] Read draft failed:', error)
    clearStoredDraft()
    return null
  }
}

const restoreStoredDraft = async (draft) => {
  if (!fabricCanvas) return

  isRestoring = true
  try {
    documentName.value = draft.documentName || '未命名作品'
    canvasWidth.value = Number(draft.canvasWidth) || 800
    canvasHeight.value = Number(draft.canvasHeight) || 600
    draftWidth.value = canvasWidth.value
    draftHeight.value = canvasHeight.value
    backgroundMode.value = draft.backgroundMode || 'transparent'
    backgroundColor.value = draft.backgroundColor || '#ffffff'
    showGrid.value = draft.showGrid !== false
    if (draft.drawingToolStyles) {
      Object.entries(drawingToolStyles).forEach(([key, style]) => {
        if (draft.drawingToolStyles[key]) Object.assign(style, draft.drawingToolStyles[key])
      })
    }
    selectedPreset.value = canvasPresets.some((item) => item.width === canvasWidth.value && item.height === canvasHeight.value)
      ? `${canvasWidth.value}x${canvasHeight.value}`
      : 'custom'

    fabricCanvas.setDimensions({ width: canvasWidth.value, height: canvasHeight.value })
    syncFabricCanvasLayout()
    await fabricCanvas.loadFromJSON(draft.canvasJson)
    fabricCanvas.requestRenderAll()
    updateBrush()
    historyList.value = [{
      name: '恢复草稿',
      json: fabricCanvas.toJSON(['globalCompositeOperation', 'isEraserStroke'])
    }]
    historyIndex.value = 0
    hasUnsavedChanges.value = false
  } finally {
    isRestoring = false
  }
}

const offerDraftRestore = async () => {
  const draft = readStoredDraft()
  if (!draft) return

  try {
    await ElMessageBox.confirm(
      `发现 ${new Date(draft.savedAt).toLocaleString()} 保存的草稿，是否恢复？`,
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
  hasUnsavedChanges.value = true
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
}

const handleCanvasWheel = (event) => {
  event.preventDefault()
  if (event.deltaY < 0) {
    zoomIn()
  } else {
    zoomOut()
  }
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

const safeDocumentName = () => documentName.value.trim().replace(/[\\/:*?"<>|]/g, '-') || '未命名作品'
const downloadImage = async (mode) => {
  if (!fabricCanvas) return

  const format = mode === 'jpeg' ? 'jpeg' : 'png'
  const includeBackground = mode !== 'transparent'
  const dataURL = await drawToCanvas(format, includeBackground)
  const link = document.createElement('a')
  link.download = `${safeDocumentName()}_${Date.now()}.${format === 'jpeg' ? 'jpg' : 'png'}`
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

  aiModel.value = activeAiModels.value[0]?.value || 'qwen-plus'
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

const getUploadedImageId = (res) => res?.id || res?.data?.id

const saveToGallery = () => {
  if (!fabricCanvas) return
  saveForm.title = documentName.value.trim() || '未命名作品'
  saveForm.tags = []
  saveForm.description = ''
  saveDialogVisible.value = true
}

const confirmSaveToGallery = async () => {
  if (!fabricCanvas || !saveForm.title.trim()) return

  savingToGallery.value = true
  try {
    const dataURL = await drawToCanvas('png', backgroundMode.value !== 'transparent')
    const res = await fetch(dataURL)
    const blob = await res.blob()
    const file = new File([blob], `${safeDocumentName()}_${Date.now()}.png`, { type: 'image/png' })
    const uploadResult = await uploadImage(file)
    const imageId = getUploadedImageId(uploadResult)
    if (!imageId) {
      ElMessage.warning('图片已上传，但未拿到作品ID，无法保存作品信息')
      return
    }

    const tags = [...new Set(saveForm.tags.map(tag => tag.trim()).filter(Boolean))]
    await updateImageMetadata(imageId, {
      title: saveForm.title.trim(),
      tags: tags.join(','),
      description: saveForm.description.trim()
    })
    clearStoredDraft()
    hasUnsavedChanges.value = false
    saveDialogVisible.value = false
    ElMessage.success('已保存到个人中心')
  } catch (error) {
    console.error('[Draw] Save failed:', error)
    ElMessage.error('保存失败，请确认后端服务和登录状态')
  } finally {
    savingToGallery.value = false
  }
}

const handleKeydown = (event) => {
  const target = event.target
  const isTyping = ['INPUT', 'TEXTAREA'].includes(target?.tagName) || target?.isContentEditable
  if (isTyping) return

  if ((event.key === 'Delete' || event.key === 'Backspace') && getSelectedTextObject()) {
    event.preventDefault()
    deleteSelectedText()
    return
  }

  if (event.ctrlKey || event.metaKey) {
    if (event.key.toLowerCase() === 's') {
      event.preventDefault()
      saveToGallery()
    }
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
  if (event.key.toLowerCase() === 'v') selectTool('pan')
}

const handleWindowBlur = () => {
  isCanvasPanning.value = false
  panStart = null
  updateBrush()
}

watch([brushSize, brushColor, brushOpacity], () => {
  applySelectedTextStyle()
  updateBrush()
})
watch([eraserSize, currentTool, brushType, eraserType], updateBrush)
watch([documentName, backgroundMode, backgroundColor, showGrid], () => {
  if (fabricCanvas && !isRestoring) hasUnsavedChanges.value = true
})
watch(brushColor, (color) => {
  recentColors.value = [color, ...recentColors.value.filter(item => item !== color)].slice(0, 8)
})

onBeforeRouteLeave(async () => {
  if (!hasUnsavedChanges.value) return true

  try {
    await ElMessageBox.confirm('当前画布尚未保存，是否保存为草稿后退出？', '保存草稿', {
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
    if (action === 'cancel') {
      clearStoredDraft()
      return true
    }
    return false
  }
})

onMounted(async () => {
  await initCanvas()
  await offerDraftRestore()
  window.addEventListener('keydown', handleKeydown)
  window.addEventListener('blur', handleWindowBlur)
  window.addEventListener('beforeunload', handleBeforeUnload)
  document.addEventListener('fullscreenchange', syncFullscreenState)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('blur', handleWindowBlur)
  window.removeEventListener('beforeunload', handleBeforeUnload)
  document.removeEventListener('fullscreenchange', syncFullscreenState)
  if (fabricCanvas) {
    fabricCanvas.dispose()
    fabricCanvas = null
  }
})
</script>

<style scoped>
.draw-page {
  --editor-accent: #16a36a;
  --editor-accent-soft: #e7f7ef;
  --editor-ink: #17211d;
  --editor-muted: #6d7973;
  width: 100%;
  height: 100vh;
  min-width: 980px;
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr) 300px;
  grid-template-rows: 80px minmax(0, 1fr);
  color: var(--editor-ink);
  background: #d9dddc;
  overflow: hidden;
}

.editor-topbar {
  grid-column: 1 / -1;
  z-index: 20;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 10px 16px;
  background: rgba(255, 255, 255, 0.96);
  border-bottom: 1px solid #dce2df;
  box-shadow: 0 2px 14px rgba(24, 34, 30, 0.06);
}

.document-area,
.topbar-actions,
.action-group,
.zoom-controls,
.grid-toggle {
  display: flex;
  align-items: center;
}

.document-area {
  flex: 0 0 248px;
  gap: 8px;
}

.document-name {
  width: 196px;
}

.document-name :deep(.el-input__wrapper) {
  box-shadow: none;
  background: #f4f6f5;
}

.icon-button,
.zoom-btn,
.zoom-value {
  border: 0;
  background: transparent;
  color: #53605a;
  cursor: pointer;
}

.icon-button {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  border-radius: 9px;
}

.icon-button:hover,
.zoom-btn:hover,
.zoom-btn.active,
.zoom-value:hover {
  color: var(--editor-accent);
  background: var(--editor-accent-soft);
}

.topbar-actions {
  min-width: 0;
  justify-content: flex-end;
  gap: 8px;
  overflow-x: auto;
  scrollbar-width: none;
}

.topbar-actions::-webkit-scrollbar { display: none; }

.action-group {
  flex: 0 0 auto;
  min-height: 54px;
  gap: 6px;
  padding: 5px 9px;
  border: 1px solid #e1e6e3;
  border-radius: 10px;
  background: #fafbfa;
}

.group-label {
  align-self: flex-start;
  margin: 1px 3px 0 0;
  color: #8a948f;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: .08em;
}

.zoom-controls { gap: 2px; }

.zoom-btn,
.zoom-value {
  height: 28px;
  border-radius: 7px;
}

.zoom-btn {
  width: 28px;
  display: inline-grid;
  place-items: center;
}

.zoom-value {
  min-width: 48px;
  padding: 0 5px;
  color: #26322c;
  font-size: 12px;
  font-weight: 700;
}

.zoom-btn:disabled {
  opacity: .35;
  cursor: not-allowed;
}

.preset-select { width: 100px; }
.dimension-input { width: 82px; }
.background-select { width: 88px; }

.dimension-separator {
  color: #9aa39f;
  font-size: 12px;
}

.grid-toggle {
  gap: 5px;
  margin-left: 3px;
  color: #626e68;
  font-size: 12px;
}

.tool-rail {
  grid-column: 1;
  grid-row: 2;
  z-index: 10;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 14px 9px 12px;
  background: #202925;
  border-right: 1px solid #38413d;
  box-shadow: 5px 0 18px rgba(22, 30, 27, .12);
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: #4b5751 transparent;
}

.rail-title {
  margin: 0 0 11px;
  color: #88938e;
  font-size: 10px;
  font-weight: 700;
  text-align: center;
  letter-spacing: .08em;
}

.rail-group {
  display: grid;
  gap: 8px;
}

.shape-tools {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #3b4540;
}

.shape-tool {
  min-height: 48px;
}

.tool-glyph {
  font: 700 22px/1 ui-monospace, monospace;
}

.shape-menu-options {
  display: grid;
  gap: 6px;
}

.shape-menu-option {
  width: 100%;
  min-height: 38px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 7px 10px;
  border: 0;
  border-radius: 7px;
  color: #58645e;
  background: transparent;
  font: inherit;
  font-size: 12px;
  cursor: pointer;
}

.shape-menu-option:hover {
  color: var(--editor-accent);
  background: var(--editor-accent-soft);
}

.shape-menu-option.active {
  color: #fff;
  background: var(--editor-accent);
}

:global(.shape-menu-popper.el-popper) {
  --editor-accent: #16a36a;
  --editor-accent-soft: #e7f7ef;
  padding: 8px;
  border-radius: 10px;
}

.history-tools {
  margin-top: auto;
  padding-top: 12px;
  border-top: 1px solid #3b4540;
}

.rail-btn {
  position: relative;
  min-height: 60px;
  display: grid;
  place-items: center;
  gap: 3px;
  padding: 7px 4px;
  border: 1px solid transparent;
  border-radius: 10px;
  background: transparent;
  color: #aab3af;
  font: inherit;
  font-size: 11px;
  cursor: pointer;
  transition: .16s ease;
}

.rail-btn:hover {
  color: #fff;
  background: #303a35;
}

.rail-btn.active {
  color: #fff;
  border-color: rgba(59, 210, 142, .34);
  background: #147d54;
  box-shadow: 0 5px 15px rgba(0, 0, 0, .22);
}

.rail-btn kbd {
  position: absolute;
  top: 5px;
  right: 7px;
  color: #75807b;
  font: 9px/1 ui-monospace, monospace;
}

.rail-btn.active kbd { color: #bcebd4; }

.rail-btn:disabled {
  opacity: .3;
  cursor: not-allowed;
}

.history-tool { min-height: 48px; }

.studio {
  grid-column: 2;
  grid-row: 2;
  min-width: 0;
  min-height: 0;
  display: grid;
  grid-template-rows: minmax(0, 1fr) 34px;
  background: #c9cecc;
}

.canvas-stage {
  position: relative;
  min-width: 0;
  min-height: 0;
  display: grid;
  place-items: center;
  padding: 76px 42px 44px;
  overflow: hidden;
  overscroll-behavior: contain;
  touch-action: none;
  background: #c9cecc;
}

.shortcut-strip {
  position: absolute;
  z-index: 8;
  top: 18px;
  left: 50%;
  display: flex;
  gap: 20px;
  padding: 8px 14px;
  transform: translateX(-50%);
  border: 1px solid rgba(255,255,255,.72);
  border-radius: 9px;
  background: rgba(246,248,247,.88);
  color: #68736e;
  box-shadow: 0 5px 18px rgba(26, 34, 31, .09);
  font-size: 11px;
  white-space: nowrap;
  backdrop-filter: blur(8px);
}

.shortcut-strip kbd {
  color: #2c3933;
  font: 600 10px/1 ui-monospace, monospace;
}

.canvas-card {
  min-width: 220px;
  min-height: 180px;
  display: grid;
  place-items: center;
  padding: 22px;
  border: 1px solid rgba(255,255,255,.48);
  border-radius: 8px;
  background: rgba(236,239,238,.42);
  box-shadow: 0 20px 45px rgba(31, 41, 37, .18), inset 0 1px rgba(255,255,255,.55);
  transition: transform .08s linear;
  will-change: transform;
}

.canvas-viewport {
  position: relative;
  flex: 0 0 auto;
}

.canvas-stage.is-panning .canvas-card { transition: none; }

.canvas-shell {
  position: absolute;
  left: 0;
  top: 0;
  isolation: isolate;
  overflow: hidden;
  transform-origin: left top;
  border: 1px solid rgba(42, 53, 48, .24);
  background: #fff;
  box-shadow: 0 16px 38px rgba(20, 28, 25, .30), 0 2px 7px rgba(20, 28, 25, .18);
}

.canvas-shell.transparent {
  background-color: #fff;
  background-image:
    linear-gradient(45deg, #d9dddb 25%, transparent 25%),
    linear-gradient(-45deg, #d9dddb 25%, transparent 25%),
    linear-gradient(45deg, transparent 75%, #d9dddb 75%),
    linear-gradient(-45deg, transparent 75%, #d9dddb 75%);
  background-size: 18px 18px;
  background-position: 0 0, 0 9px, 9px -9px, -9px 0;
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

.canvas-shell :deep(.upper-canvas) {
  z-index: 3;
  cursor: crosshair;
  touch-action: none;
}

.canvas-grid-overlay {
  position: absolute;
  inset: 0;
  z-index: 4;
  pointer-events: none;
  background-image:
    linear-gradient(rgba(22, 89, 62, .10) 1px, transparent 1px),
    linear-gradient(90deg, rgba(22, 89, 62, .10) 1px, transparent 1px),
    linear-gradient(rgba(22, 89, 62, .22) 1px, transparent 1px),
    linear-gradient(90deg, rgba(22, 89, 62, .22) 1px, transparent 1px);
  background-size: 16px 16px, 16px 16px, 64px 64px, 64px 64px;
}

.canvas-stage.pan-tool,
.canvas-stage.pan-tool :deep(canvas),
.canvas-stage.space-pan,
.canvas-stage.space-pan :deep(canvas) { cursor: grab; }

.canvas-stage.is-panning,
.canvas-stage.is-panning :deep(canvas) {
  cursor: grabbing;
  user-select: none;
}

.status-bar {
  display: flex;
  align-items: center;
  gap: 22px;
  padding: 0 16px;
  overflow: hidden;
  border-top: 1px solid #bac1be;
  background: #e9eceb;
  color: #5e6a64;
  font: 11px/1 ui-monospace, monospace;
  white-space: nowrap;
}

.status-history { margin-left: auto; }

.inspector {
  grid-column: 3;
  grid-row: 2;
  z-index: 10;
  min-width: 0;
  min-height: 0;
  padding: 0 18px 20px;
  overflow-y: auto;
  border-left: 1px solid #d9dfdc;
  background: #f8faf9;
  box-shadow: -5px 0 18px rgba(22, 30, 27, .08);
  scrollbar-width: thin;
  scrollbar-color: #bec7c2 transparent;
}

.inspector-heading {
  position: sticky;
  z-index: 4;
  top: 0;
  min-height: 72px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 0 -18px 5px;
  padding: 12px 18px;
  border-bottom: 1px solid #e1e6e3;
  background: rgba(248,250,249,.96);
  backdrop-filter: blur(8px);
}

.inspector-heading div { display: grid; gap: 2px; }

.inspector-heading span {
  color: #929b97;
  font-size: 10px;
  letter-spacing: .08em;
}

.inspector-heading strong {
  font-size: 16px;
  font-weight: 700;
}

.tool-shortcut {
  min-width: 28px;
  padding: 6px;
  border: 1px solid #d9dfdc;
  border-radius: 6px;
  background: #fff;
  color: #4f5c56 !important;
  text-align: center;
  font: 700 11px/1 ui-monospace, monospace !important;
}

.inspector-section {
  padding: 16px 2px 18px;
  border-bottom: 1px solid #e5e9e7;
}

.section-title {
  margin-bottom: 12px;
  color: #34413b;
  font-size: 13px;
  font-weight: 700;
}

.type-button-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.type-choice {
  min-height: 38px;
  padding: 7px 8px;
  border: 1px solid #d9dfdc;
  border-radius: 8px;
  background: #fff;
  color: #58645e;
  font: inherit;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: .16s ease;
}

.type-choice:hover {
  border-color: #8ecdb0;
  color: var(--editor-accent);
}

.type-choice.active {
  border-color: var(--editor-accent);
  color: #fff;
  background: var(--editor-accent);
  box-shadow: 0 4px 12px rgba(22, 163, 106, .18);
}

.inspector :deep(.el-collapse) {
  border: 0;
}

.inspector :deep(.el-collapse-item__header) {
  height: 49px;
  border-bottom-color: #e5e9e7;
  background: transparent;
  color: #34413b;
  font-size: 13px;
  font-weight: 700;
}

.inspector :deep(.el-collapse-item__wrap) {
  border-bottom-color: #e5e9e7;
  background: transparent;
}

.inspector :deep(.el-collapse-item__content) {
  padding: 2px 2px 18px;
}

.field {
  display: grid;
  gap: 8px;
  margin-top: 13px;
  color: #626f69;
  font-size: 12px;
  font-weight: 600;
}

.field > span {
  display: flex;
  justify-content: space-between;
}

.field em {
  color: var(--editor-accent);
  font-style: normal;
  font-family: ui-monospace, monospace;
}

.inspector :deep(.el-select) { width: 100%; }

.color-control {
  display: flex;
  align-items: center;
  gap: 10px;
}

.color-control code {
  color: #65716b;
  font-size: 11px;
}

.quick-colors {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.color-dot {
  width: 27px;
  height: 27px;
  padding: 0;
  border: 2px solid #fff;
  border-radius: 7px;
  outline: 1px solid #d6dcd9;
  cursor: pointer;
  transition: transform .15s ease, outline-color .15s ease;
}

.color-dot:hover { transform: translateY(-2px); }

.color-dot.active {
  outline: 2px solid var(--editor-accent);
}

.recent-colors .color-dot {
  width: 22px;
  height: 22px;
}

.tool-help {
  display: grid;
  justify-items: center;
  gap: 10px;
  margin-top: 28px;
  padding: 28px 18px;
  border: 1px solid #e1e6e3;
  border-radius: 10px;
  background: #fff;
  color: #65716b;
  text-align: center;
}

.tool-help .el-icon {
  color: var(--editor-accent);
  font-size: 28px;
}

.tool-help strong { color: #29352f; }

.tool-help p {
  margin: 0;
  font-size: 12px;
  line-height: 1.75;
}

.selection-controls {
  display: grid;
  gap: 18px;
  padding: 4px 2px 12px;
}

.delete-selection {
  width: 100%;
  margin-top: 4px;
}

.selection-hint {
  margin: -8px 0 0;
  color: #8a948f;
  font-size: 11px;
  text-align: center;
}

.tool-help.compact {
  margin-top: 8px;
  padding: 22px 14px;
}
@media (max-width: 1280px) {
  .draw-page {
    min-width: 920px;
    grid-template-columns: 78px minmax(0, 1fr) 270px;
  }

  .document-area { flex-basis: 210px; }
  .document-name { width: 160px; }
  .action-group { padding-inline: 6px; }
  .group-label { display: none; }
  .canvas-stage { padding-inline: 28px; }
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


</style>
