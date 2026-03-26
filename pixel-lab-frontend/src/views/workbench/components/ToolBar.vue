<template>
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
            @click="$emit('apply-filter', filter.value)"
          >
            <div
              class="filter-preview"
              :style="getPreviewStyle(filter.value)"
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
            :model-value="adjustments.brightness"
            :min="-100"
            :max="100"
            :format-tooltip="val => val > 0 ? `+${val}` : val"
            @input="val => updateAdjustment('brightness', val)"
          />
        </div>
        <div class="adjust-item">
          <label>对比度</label>
          <el-slider
            :model-value="adjustments.contrast"
            :min="-100"
            :max="100"
            :format-tooltip="val => val > 0 ? `+${val}` : val"
            @input="val => updateAdjustment('contrast', val)"
          />
        </div>
        <div class="adjust-item">
          <label>饱和度</label>
          <el-slider
            :model-value="adjustments.saturate"
            :min="-100"
            :max="100"
            :format-tooltip="val => val > 0 ? `+${val}` : val"
            @input="val => updateAdjustment('saturate', val)"
          />
        </div>
      </el-collapse-item>

      <!-- 裁剪 -->
      <el-collapse-item
        title="裁剪"
        name="crop"
      >
        <div class="crop-buttons">
          <el-button
            type="primary"
            @click="$emit('crop')"
          >
            <el-icon><Crop /></el-icon>
            裁剪图片
          </el-button>
          <p class="crop-hint">
            支持自由裁剪和固定比例裁剪
          </p>
        </div>
      </el-collapse-item>

      <!-- 几何变换 -->
      <el-collapse-item
        title="几何变换"
        name="transform"
      >
        <div class="transform-buttons">
          <el-button @click="$emit('rotate', 90)">
            <el-icon><RefreshRight /></el-icon>
            旋转90°
          </el-button>
          <el-button @click="$emit('flip', 'horizontal')">
            <el-icon class="flip-h">
              <Sort />
            </el-icon>
            水平翻转
          </el-button>
          <el-button @click="$emit('flip', 'vertical')">
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
              :model-value="pixelSize"
              :min="2"
              :max="64"
              :step="2"
              @input="val => $emit('update:pixelSize', val)"
            />
          </div>
          <div class="adjust-item">
            <label>颜色数量: {{ colorCount }}色</label>
            <el-slider
              :model-value="colorCount"
              :min="2"
              :max="256"
              :step="1"
              @input="val => $emit('update:colorCount', val)"
            />
          </div>
          <div class="pixel-buttons">
            <el-button
              type="primary"
              size="small"
              @click="$emit('apply-pixel')"
            >
              应用
            </el-button>
            <el-button
              size="small"
              @click="$emit('reset-pixel')"
            >
              重置
            </el-button>
          </div>
        </div>
      </el-collapse-item>
    </el-collapse>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { RefreshRight, Sort, Crop } from '@element-plus/icons-vue'

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
  'update:pixelSize',
  'update:colorCount'
])

const activeCollapse = ref(['filters', 'adjust', 'crop', 'transform', 'pixel'])

const updateAdjustment = (key, value) => {
  emit('update-adjustment', { key, value })
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
  case 'blur':
    filter = 'blur(2px)'
    break
  case 'warm':
    filter = 'sepia(30%) saturate(140%)'
    break
  case 'cool':
    filter = 'saturate(80%) hue-rotate(20deg)'
    break
  }

  const brightness = 100 + props.adjustments.brightness
  const contrast = 100 + props.adjustments.contrast
  const saturate = 100 + props.adjustments.saturate

  filter += ` brightness(${brightness}%) contrast(${contrast}%) saturate(${saturate}%)`

  return { filter }
}
</script>

<style scoped>
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

.crop-buttons {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.crop-buttons .el-button {
  width: 100% !important;
  padding: 14px 16px !important;
  font-size: 14px !important;
}

.crop-hint {
  font-size: 12px;
  color: var(--foreground-muted);
  text-align: center;
  margin: 0;
}

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
</style>