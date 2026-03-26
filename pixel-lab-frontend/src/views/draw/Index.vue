<template>
  <div class="draw-page">
    <!-- 左侧工具栏 -->
    <div class="toolbar">
      <div class="tool-icons">
        <el-tooltip content="画笔 (B)" placement="right">
          <div
            class="tool-btn"
            :class="{ active: currentTool === 'brush' }"
            @click="selectTool('brush')"
          >
            <el-icon :size="22"><EditPen /></el-icon>
          </div>
        </el-tooltip>
        <el-tooltip content="橡皮擦 (E)" placement="right">
          <div
            class="tool-btn"
            :class="{ active: currentTool === 'eraser' }"
            @click="selectTool('eraser')"
          >
            <el-icon :size="22"><Delete /></el-icon>
          </div>
        </el-tooltip>
        <div class="tool-divider" />
        <el-tooltip content="撤销 (Ctrl+Z)" placement="right">
          <div class="tool-btn" :class="{ disabled: !canUndo }" @click="undo">
            <el-icon :size="22"><Back /></el-icon>
          </div>
        </el-tooltip>
        <el-tooltip content="重做 (Ctrl+Y)" placement="right">
          <div class="tool-btn" :class="{ disabled: !canRedo }" @click="redo">
            <el-icon :size="22"><Right /></el-icon>
          </div>
        </el-tooltip>
        <div class="tool-divider" />
        <el-tooltip content="清空画布" placement="right">
          <div class="tool-btn" @click="clearCanvas">
            <el-icon :size="22"><RefreshRight /></el-icon>
          </div>
        </el-tooltip>
      </div>
    </div>

    <!-- 主区域 -->
    <div class="main-area">
      <!-- 顶部控制条 -->
      <div class="top-bar">
        <div class="brush-controls">
          <!-- 画笔大小 -->
          <div class="control-group">
            <span class="label">大小</span>
            <el-slider
              v-model="brushSize"
              :min="1"
              :max="100"
              :show-tooltip="true"
              style="width: 120px"
            />
            <span class="value">{{ brushSize }}</span>
          </div>

          <!-- 画笔颜色 -->
          <div class="control-group" v-show="currentTool === 'brush'">
            <span class="label">颜色</span>
            <el-color-picker v-model="brushColor" :predefine="presetColors" size="small" />
            <div class="quick-colors">
              <div
                v-for="color in quickColors"
                :key="color"
                class="color-dot"
                :class="{ active: brushColor === color }"
                :style="{ backgroundColor: color }"
                @click="brushColor = color"
              />
            </div>
          </div>

          <!-- 橡皮擦提示 -->
          <div class="control-group" v-show="currentTool === 'eraser'">
            <span class="label">橡皮擦</span>
            <span class="hint">擦除绘制内容</span>
          </div>
        </div>

        <div class="action-buttons">
          <el-dropdown trigger="click" @command="downloadImage">
            <el-button>
              <el-icon><Download /></el-icon> 下载
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="transparent">
                  <span>PNG (透明背景)</span>
                </el-dropdown-item>
                <el-dropdown-item command="white">
                  <span>PNG (白色背景)</span>
                </el-dropdown-item>
                <el-dropdown-item command="jpeg">
                  <span>JPG (白色背景)</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button type="primary" @click="saveToGallery">
            <el-icon><Upload /></el-icon> 保存
          </el-button>
        </div>
      </div>

      <!-- 画布区域 -->
      <div class="canvas-area" ref="canvasWrapperRef">
        <canvas ref="canvasRef" />
      </div>

      <!-- 底部状态栏 -->
      <div class="status-bar">
        <span>{{ canvasWidth }} × {{ canvasHeight }}</span>
        <span v-if="historyCount > 0">历史: {{ historyCount }} 步</span>
        <span class="hint">透明画布 · 导出时可选择背景</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed, watch, nextTick } from 'vue'
import { EditPen, Delete, Back, Right, Download, Upload, RefreshRight, ArrowDown } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { Canvas as FabricCanvas, PencilBrush } from 'fabric'
import { uploadImage } from '@/api/image'

// ============ Refs ============
const canvasRef = ref(null)
const canvasWrapperRef = ref(null)
let fabricCanvas = null

// ============ 状态 ============
const currentTool = ref('brush')
const brushSize = ref(5)
const brushColor = ref('#000000')
const canvasWidth = ref(800)
const canvasHeight = ref(600)

// ============ 颜色预设 ============
const presetColors = [
  '#000000', '#ffffff', '#ff0000', '#ff6b00', '#ffd700',
  '#00ff00', '#00ffff', '#0066ff', '#9900ff', '#ff00ff',
  '#666666', '#999999'
]

const quickColors = ['#000000', '#ff0000', '#0066ff', '#00ff00', '#ffd700', '#ffffff']

// ============ 历史记录 ============
const historyList = ref([])
const historyIndex = ref(-1)
const maxHistory = 50

const canUndo = computed(() => historyIndex.value > 0)
const canRedo = computed(() => historyIndex.value < historyList.value.length - 1)
const historyCount = computed(() => historyList.value.length)

// ============ 初始化画布 ============
const initCanvas = async () => {
  if (!canvasRef.value || !canvasWrapperRef.value) return

  await nextTick()

  const wrapper = canvasWrapperRef.value
  const maxWidth = wrapper.clientWidth - 40
  const maxHeight = wrapper.clientHeight - 40

  canvasWidth.value = Math.min(800, maxWidth)
  canvasHeight.value = Math.min(600, maxHeight)

  fabricCanvas = new FabricCanvas(canvasRef.value, {
    width: canvasWidth.value,
    height: canvasHeight.value,
    backgroundColor: '', // 透明背景
    isDrawingMode: true
  })

  fabricCanvas.renderAll()
  updateBrush()

  fabricCanvas.on('path:created', handlePathCreated)

  saveHistory('初始画布')
}

// ============ 工具选择 ============
const selectTool = (tool) => {
  currentTool.value = tool
  updateBrush()
}

// ============ 更新画笔 ============
const updateBrush = () => {
  if (!fabricCanvas) return

  fabricCanvas.isDrawingMode = true

  const brush = new PencilBrush(fabricCanvas)
  brush.width = brushSize.value

  if (currentTool.value === 'eraser') {
    // 橡皮擦：半透明灰色轨迹，能看到擦除范围
    brush.color = 'rgba(128, 128, 128, 0.4)'
  } else {
    brush.color = brushColor.value
  }

  fabricCanvas.freeDrawingBrush = brush
}

// ============ 路径创建后处理（橡皮擦） ============
const handlePathCreated = (e) => {
  if (currentTool.value === 'eraser' && e.path) {
    // 移除灰色预览路径
    const previewPath = e.path
    fabricCanvas.remove(previewPath)
    
    // 创建真正的擦除路径
    previewPath.globalCompositeOperation = 'destination-out'
    previewPath.stroke = '#ffffff'
    previewPath.opacity = 1
    
    // 重新添加并渲染
    fabricCanvas.add(previewPath)
    fabricCanvas.requestRenderAll()
  }
  saveHistory('绘制')
}

// ============ 历史记录 ============
const saveHistory = (name) => {
  if (!fabricCanvas) return
  historyList.value = historyList.value.slice(0, historyIndex.value + 1)
  historyList.value.push({ name, json: fabricCanvas.toJSON() })
  if (historyList.value.length > maxHistory) historyList.value.shift()
  historyIndex.value = historyList.value.length - 1
}

const undo = () => {
  if (historyIndex.value > 0) restoreHistory(historyIndex.value - 1)
}

const redo = () => {
  if (historyIndex.value < historyList.value.length - 1) restoreHistory(historyIndex.value + 1)
}

const restoreHistory = (index) => {
  if (!fabricCanvas || !historyList.value[index]) return
  historyIndex.value = index
  fabricCanvas.loadFromJSON(historyList.value[index].json, () => {
    fabricCanvas.renderAll()
    updateBrush()
  })
}

// ============ 清空画布 ============
const clearCanvas = () => {
  if (!fabricCanvas) return
  fabricCanvas.clear()
  fabricCanvas.backgroundColor = ''
  fabricCanvas.renderAll()
  saveHistory('清空画布')
  ElMessage.success('画布已清空')
}

// ============ 下载图片 ============
const downloadImage = (bgType) => {
  if (!fabricCanvas) return

  let dataURL
  const multiplier = 2

  if (bgType === 'transparent') {
    // 透明背景 PNG
    dataURL = fabricCanvas.toDataURL({
      format: 'png',
      quality: 1,
      multiplier
    })
  } else if (bgType === 'white') {
    // 白色背景 PNG
    const tempCanvas = document.createElement('canvas')
    tempCanvas.width = fabricCanvas.width * multiplier
    tempCanvas.height = fabricCanvas.height * multiplier
    const ctx = tempCanvas.getContext('2d')
    
    // 填充白色背景
    ctx.fillStyle = '#ffffff'
    ctx.fillRect(0, 0, tempCanvas.width, tempCanvas.height)
    
    // 绘制画布内容
    const img = new Image()
    img.onload = () => {
      ctx.drawImage(img, 0, 0)
      const link = document.createElement('a')
      link.download = `drawing_${Date.now()}.png`
      link.href = tempCanvas.toDataURL('image/png')
      link.click()
      ElMessage.success('下载成功')
    }
    img.src = fabricCanvas.toDataURL({ format: 'png', quality: 1, multiplier })
    return
  } else if (bgType === 'jpeg') {
    // 白色背景 JPG
    const tempCanvas = document.createElement('canvas')
    tempCanvas.width = fabricCanvas.width * multiplier
    tempCanvas.height = fabricCanvas.height * multiplier
    const ctx = tempCanvas.getContext('2d')
    
    ctx.fillStyle = '#ffffff'
    ctx.fillRect(0, 0, tempCanvas.width, tempCanvas.height)
    
    const img = new Image()
    img.onload = () => {
      ctx.drawImage(img, 0, 0)
      const link = document.createElement('a')
      link.download = `drawing_${Date.now()}.jpg`
      link.href = tempCanvas.toDataURL('image/jpeg', 0.9)
      link.click()
      ElMessage.success('下载成功')
    }
    img.src = fabricCanvas.toDataURL({ format: 'png', quality: 1, multiplier })
    return
  }

  const link = document.createElement('a')
  link.download = `drawing_${Date.now()}.png`
  link.href = dataURL
  link.click()
  ElMessage.success('下载成功')
}

// ============ 保存到个人中心 ============
const saveToGallery = async () => {
  if (!fabricCanvas) return
  try {
    const dataURL = fabricCanvas.toDataURL({ format: 'png', quality: 1, multiplier: 2 })
    const res = await fetch(dataURL)
    const blob = await res.blob()
    const file = new File([blob], `drawing_${Date.now()}.png`, { type: 'image/png' })
    await uploadImage(file)
    ElMessage.success('已保存到个人中心')
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

// ============ 快捷键 ============
const handleKeydown = (e) => {
  if (e.ctrlKey || e.metaKey) {
    if (e.key === 'z') { e.preventDefault(); undo() }
    if (e.key === 'y') { e.preventDefault(); redo() }
  }
  if (e.key === 'b' || e.key === 'B') selectTool('brush')
  if (e.key === 'e' || e.key === 'E') selectTool('eraser')
}

// ============ 监听画笔变化 ============
watch([brushSize, brushColor], updateBrush)

// ============ 生命周期 ============
onMounted(() => {
  initCanvas()
  window.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  if (fabricCanvas) fabricCanvas.dispose()
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.draw-page {
  height: calc(100vh - 72px - 80px);
  display: flex;
  background: var(--background);
}

/* 左侧工具栏 */
.toolbar {
  width: 56px;
  background: var(--background-soft);
  border-right: 3px solid var(--border);
  display: flex;
  flex-direction: column;
  padding: var(--space-2);
}

.tool-icons {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.tool-btn {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 3px solid var(--border);
  background: var(--background);
  cursor: pointer;
  transition: all 0.15s;
}

.tool-btn:hover {
  background: var(--accent);
  transform: translate(-2px, -2px);
  box-shadow: 4px 4px 0 var(--border);
}

.tool-btn.active {
  background: var(--primary);
  border-color: var(--primary);
  color: white;
}

.tool-btn.disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.tool-btn.disabled:hover {
  transform: none;
  box-shadow: none;
  background: var(--background);
}

.tool-divider {
  height: 2px;
  background: var(--border);
  margin: var(--space-2) 4px;
}

/* 主区域 */
.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 顶部控制条 */
.top-bar {
  height: 56px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 var(--space-4);
  background: var(--background-soft);
  border-bottom: 3px solid var(--border);
  flex-shrink: 0;
}

.brush-controls {
  display: flex;
  gap: var(--space-6);
  align-items: center;
}

.control-group {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.control-group .label {
  font-size: 12px;
  font-weight: 600;
  color: var(--foreground-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.control-group .value {
  font-size: 12px;
  font-weight: 600;
  color: var(--foreground);
  min-width: 28px;
}

.control-group .hint {
  font-size: 12px;
  color: var(--foreground-muted);
}

.quick-colors {
  display: flex;
  gap: 4px;
}

.color-dot {
  width: 20px;
  height: 20px;
  border: 2px solid var(--border);
  cursor: pointer;
  transition: transform 0.15s;
}

.color-dot:hover {
  transform: scale(1.15);
}

.color-dot.active {
  border-color: var(--primary);
  box-shadow: 0 0 0 2px var(--primary);
}

.action-buttons {
  display: flex;
  gap: var(--space-2);
}

/* 画布区域 */
.canvas-area {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-6);
  background: var(--background-muted);
  overflow: hidden;
}

.canvas-area canvas {
  flex-shrink: 0;
  max-width: 100%;
  max-height: 100%;
  cursor: crosshair;
  border: 2px dashed var(--border);
  /* 棋盘格背景只显示在画布内 */
  background: repeating-conic-gradient(#e0e0e0 0% 25%, #f5f5f5 0% 50%) 50% / 16px 16px;
}

/* 底部状态栏 */
.status-bar {
  height: 28px;
  display: flex;
  gap: var(--space-6);
  padding: 0 var(--space-4);
  background: var(--background-soft);
  border-top: 2px solid var(--border);
  font-size: 11px;
  color: var(--foreground-muted);
  align-items: center;
  font-family: monospace;
}

.status-bar .hint {
  margin-left: auto;
  color: var(--foreground-muted);
  opacity: 0.7;
}

/* 深色主题 */
@media (prefers-color-scheme: dark) {
  .canvas-area {
    background: var(--background-muted);
  }
  .canvas-area canvas {
    background: repeating-conic-gradient(#3a3a3a 0% 25%, #2a2a2a 0% 50%) 50% / 16px 16px;
  }
}
</style>