<template>
  <aside class="toolbar" aria-label="编辑面板">
    <div class="toolbar-heading">
      <div>
        <h2>编辑面板</h2>
      </div>
    </div>

    <section class="tool-section">
      <div class="section-title">
        <el-icon><Operation /></el-icon>
        <h3>基础调整</h3>
      </div>
      <div class="slider-list">
        <div
          v-for="item in adjustmentItems"
          :key="item.key"
          class="adjust-row"
        >
          <div class="adjust-label">
            <label>{{ item.label }}</label>
            <span>{{ formatValue(adjustments[item.key]) }}</span>
          </div>
          <el-slider
            :model-value="adjustments[item.key]"
            :min="item.min"
            :max="item.max"
            :step="item.step"
            :show-tooltip="false"
            @input="val => updateAdjustment(item.key, val)"
          />
        </div>
      </div>
    </section>

    <section class="tool-section">
      <div class="section-title">
        <el-icon><MagicStick /></el-icon>
        <h3>滤镜效果</h3>
      </div>
      <div class="filter-grid">
        <button
          v-for="filter in filters"
          :key="filter.value"
          type="button"
          class="filter-item"
          :class="{ active: currentFilter === filter.value }"
          @click="$emit('apply-filter', filter.value)"
        >
          <span
            class="filter-preview"
            :style="getPreviewStyle(filter.value)"
          >
            <img
              v-if="thumbnailUrl"
              :src="thumbnailUrl"
              alt=""
            >
          </span>
          <span class="filter-label">{{ filter.label }}</span>
        </button>
      </div>
    </section>

    <section class="tool-section">
      <div class="section-title">
        <el-icon><Crop /></el-icon>
        <h3>裁剪变换</h3>
      </div>
      <button class="crop-action" type="button" @click="$emit('crop')">
        <el-icon><Crop /></el-icon>
        裁剪
      </button>
      <div class="transform-grid">
        <button type="button" @click="$emit('rotate', -90)">
          <el-icon><RefreshLeft /></el-icon>
          左转90°
        </button>
        <button type="button" @click="$emit('rotate', 90)">
          <el-icon><RefreshRight /></el-icon>
          右转90°
        </button>
        <button type="button" @click="$emit('flip', 'horizontal')">
          <el-icon class="flip-h"><Sort /></el-icon>
          水平翻转
        </button>
        <button type="button" @click="$emit('flip', 'vertical')">
          <el-icon><Sort /></el-icon>
          垂直翻转
        </button>
      </div>
    </section>

    <section class="tool-section">
      <div class="section-title">
        <el-icon><Grid /></el-icon>
        <h3>像素化</h3>
      </div>
      <div class="slider-list">
        <div class="adjust-row">
          <div class="adjust-label">
            <label>像素块大小</label>
            <span>{{ pixelSize }}px</span>
          </div>
          <el-slider
            :model-value="pixelSize"
            :min="2"
            :max="64"
            :step="2"
            :show-tooltip="false"
            @input="val => $emit('update:pixel-size', val)"
          />
        </div>
        <div class="adjust-row">
          <div class="adjust-label">
            <label>最多颜色</label>
            <span>{{ colorCount }}色</span>
          </div>
          <el-slider
            :model-value="colorCount"
            :min="2"
            :max="256"
            :step="1"
            :show-tooltip="false"
            @input="val => $emit('update:color-count', val)"
          />
        </div>
      </div>
      <div class="split-actions">
        <button class="primary-action" type="button" @click="$emit('apply-pixel')">应用</button>
        <button type="button" @click="$emit('reset-pixel')">重置</button>
      </div>
    </section>

    <section class="tool-section">
      <div class="section-title">
        <el-icon><EditPen /></el-icon>
        <h3>文字 / 水印</h3>
      </div>
      <div class="split-actions">
        <button type="button" @click="requestText('text')">添加文字</button>
        <button type="button" @click="requestText('watermark')">添加水印</button>
      </div>
      <div class="adjust-row compact">
        <div class="adjust-label">
          <label>透明度</label>
          <span>{{ watermarkOpacity }}%</span>
        </div>
        <el-slider
          v-model="watermarkOpacity"
          :min="10"
          :max="100"
          :step="5"
          :show-tooltip="false"
        />
      </div>
      <div class="position-grid" aria-label="水印位置">
        <button
          v-for="position in positions"
          :key="position"
          type="button"
          :class="{ active: watermarkPosition === position }"
          @click="watermarkPosition = position"
        />
      </div>
    </section>
  </aside>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import {
  Crop,
  EditPen,
  Grid,
  MagicStick,
  Operation,
  RefreshLeft,
  RefreshRight,
  Sort
} from '@element-plus/icons-vue'

const props = defineProps({
  filters: { type: Array, required: true },
  currentFilter: { type: String, default: 'none' },
  adjustments: { type: Object, required: true },
  pixelSize: { type: Number, default: 8 },
  colorCount: { type: Number, default: 32 },
  thumbnailUrl: { type: String, default: '' }
})

const emit = defineEmits([
  'apply-filter',
  'update-adjustment',
  'rotate',
  'flip',
  'crop',
  'apply-pixel',
  'reset-pixel',
  'add-text',
  'add-watermark',
  'update:pixel-size',
  'update:color-count'
])

const adjustmentItems = [
  { key: 'brightness', label: '亮度', min: -100, max: 100, step: 1 },
  { key: 'contrast', label: '对比度', min: -100, max: 100, step: 1 },
  { key: 'saturate', label: '饱和度', min: -100, max: 100, step: 1 },
  { key: 'sharpen', label: '锐化', min: 0, max: 100, step: 1 }
]
const positions = ['tl', 'tc', 'tr', 'ml', 'mc', 'mr', 'bl', 'bc', 'br']
const watermarkOpacity = ref(60)
const watermarkPosition = ref('mc')

const updateAdjustment = (key, value) => {
  emit('update-adjustment', { key, value })
}

const formatValue = (value = 0) => value > 0 ? `+${Math.round(value)}` : Math.round(value)

const requestText = async (type) => {
  const isWatermark = type === 'watermark'
  try {
    const { value } = await ElMessageBox.prompt(
      isWatermark ? '请输入水印内容' : '请输入要添加的文字',
      isWatermark ? '添加水印' : '添加文字',
      {
        confirmButtonText: '添加',
        cancelButtonText: '取消',
        inputPlaceholder: isWatermark ? '例如：Pixel Lab' : '输入文字内容',
        inputValidator: value => value?.trim() ? true : '内容不能为空',
        inputErrorMessage: '内容不能为空'
      }
    )

    const text = value.trim()
    if (isWatermark) {
      emit('add-watermark', {
        text,
        opacity: watermarkOpacity.value,
        position: watermarkPosition.value
      })
    } else {
      emit('add-text', { text })
    }
  } catch {
    // 用户取消输入
  }
}

const getPreviewStyle = (filterValue) => {
  let filter = ''

  switch (filterValue) {
  case 'grayscale':
    filter = 'grayscale(100%)'
    break
  case 'sepia':
    filter = 'sepia(100%)'
    break
  case 'invert':
    filter = 'invert(100%)'
    break
  case 'sharpen':
    filter = 'contrast(118%) saturate(112%)'
    break
  case 'warm':
    filter = 'sepia(30%) saturate(140%)'
    break
  case 'cool':
    filter = 'saturate(80%) hue-rotate(20deg)'
    break
  case 'film':
    filter = 'contrast(108%) saturate(92%) sepia(18%)'
    break
  }

  const brightness = 100 + (props.adjustments.brightness || 0)
  const contrast = 100 + (props.adjustments.contrast || 0) + (props.adjustments.sharpen || 0) * 0.25
  const saturate = 100 + (props.adjustments.saturate || 0)

  filter += ` brightness(${brightness}%) contrast(${contrast}%) saturate(${saturate}%)`

  return { filter }
}
</script>

<style scoped>
.toolbar {
  width: clamp(280px, 20vw, 340px);
  flex: 0 0 clamp(280px, 20vw, 340px);
  max-height: 100%;
  padding: 14px;
  overflow-y: auto;
  border: 1px solid rgba(220, 226, 232, 0.8);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 18px 44px rgba(17, 24, 39, 0.08);
}

.toolbar-heading {
  position: sticky;
  top: -14px;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: -14px -14px 12px;
  padding: 18px 16px 14px;
  border-bottom: 1px solid rgba(229, 233, 238, 0.92);
  background: rgba(255, 255, 255, 0.96);
}

.toolbar-heading h2,
.section-title h3 {
  margin: 0;
  color: #151b23;
  letter-spacing: 0;
}

.toolbar-heading h2 {
  font-size: 17px;
  font-weight: 800;
}

.toolbar-heading span {
  display: block;
  margin-top: 4px;
  color: #8a94a3;
  font-size: 11px;
  font-weight: 700;
}

.icon-button,
.filter-item,
.crop-action,
.transform-grid button,
.split-actions button,
.position-grid button {
  border: 1px solid #e5eaf0;
  background: #fff;
  color: #323b47;
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    color 0.18s ease,
    background 0.18s ease,
    transform 0.18s ease;
}

.icon-button {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border-radius: 50%;
}

.icon-button:hover,
.crop-action:hover,
.transform-grid button:hover,
.split-actions button:hover {
  border-color: #13bd79;
  color: #0ba86a;
  box-shadow: 0 10px 24px rgba(22, 199, 132, 0.14);
  transform: translateY(-1px);
}

.tool-section {
  padding: 14px 0;
  border-bottom: 1px solid rgba(229, 233, 238, 0.9);
}

.tool-section:last-child {
  border-bottom: 0;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.section-title .el-icon {
  color: #111827;
  font-size: 16px;
}

.section-title h3 {
  font-size: 14px;
  font-weight: 800;
}

.slider-list {
  display: grid;
  gap: 8px;
}

.adjust-row {
  min-width: 0;
}

.adjust-row.compact {
  margin-top: 12px;
}

.adjust-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 22px;
}

.adjust-label label,
.adjust-label span {
  color: #4b5563;
  font-size: 12px;
  font-weight: 700;
}

.adjust-label span {
  color: #151b23;
  font-variant-numeric: tabular-nums;
}

.toolbar :deep(.el-slider__runway) {
  height: 4px;
  margin: 8px 0 4px;
  background-color: #e7ebf0;
}

.toolbar :deep(.el-slider__bar) {
  height: 4px;
  background: linear-gradient(90deg, #12c77f, #08a965);
}

.toolbar :deep(.el-slider__button) {
  width: 13px;
  height: 13px;
  border: 2px solid #12b978;
  box-shadow: 0 4px 10px rgba(10, 132, 85, 0.22);
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.filter-item {
  min-width: 0;
  padding: 5px;
  border-radius: 10px;
  text-align: center;
}

.filter-item:hover,
.filter-item.active {
  border-color: #12c77f;
  background: #f0fff8;
}

.filter-item.active {
  box-shadow: inset 0 0 0 1px rgba(18, 199, 127, 0.22), 0 10px 22px rgba(18, 199, 127, 0.14);
}

.filter-preview {
  display: block;
  width: 100%;
  aspect-ratio: 4 / 3;
  overflow: hidden;
  border-radius: 8px;
  background:
    linear-gradient(45deg, #eef1f4 25%, transparent 25%),
    linear-gradient(-45deg, #eef1f4 25%, transparent 25%),
    linear-gradient(45deg, transparent 75%, #eef1f4 75%),
    linear-gradient(-45deg, transparent 75%, #eef1f4 75%);
  background-color: #fff;
  background-position: 0 0, 0 8px, 8px -8px, -8px 0;
  background-size: 16px 16px;
}

.filter-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.filter-label {
  display: block;
  margin-top: 5px;
  overflow: hidden;
  color: #3b4652;
  font-size: 12px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.filter-item.active .filter-label {
  color: #09a768;
}

.transform-grid,
.split-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.crop-action {
  width: 100%;
  min-height: 38px;
  margin-bottom: 8px;
}

.crop-action,
.transform-grid button,
.split-actions button {
  min-height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  padding: 0 8px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 750;
}

.primary-action {
  border-color: transparent !important;
  background: linear-gradient(135deg, #12c77f, #08aa68) !important;
  color: #fff !important;
}

.flip-h {
  transform: rotate(90deg);
}

.position-grid {
  display: grid;
  width: 92px;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin: 10px auto 0;
}

.position-grid button {
  width: 10px;
  height: 10px;
  justify-self: center;
  padding: 0;
  border-radius: 50%;
  background: #f5f7f9;
}

.position-grid button.active {
  border-color: #12c77f;
  background: #12c77f;
  box-shadow: 0 0 0 4px rgba(18, 199, 127, 0.13);
}

@media (max-width: 1180px) {
  .toolbar {
    width: 300px;
    flex-basis: 300px;
  }

  .filter-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

}

@media (max-width: 1024px) {
  .toolbar {
    width: 100%;
    flex-basis: auto;
    max-height: none;
  }

  .filter-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .filter-grid,
  .transform-grid,
  .split-actions {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
