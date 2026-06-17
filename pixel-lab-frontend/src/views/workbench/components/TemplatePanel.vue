<template>
  <el-dialog
    title="选择画布模板"
    :model-value="modelValue"
    width="720px"
    :close-on-click-modal="false"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <div class="template-grid">
      <div
        v-for="tpl in templates"
        :key="tpl.name"
        class="template-card"
        :class="{ 'template-card--active': selected === tpl.name }"
        @click="handleSelect(tpl)"
      >
        <div class="template-card__preview">
          <div
            class="template-card__shape"
            :style="getShapeStyle(tpl)"
          ></div>
        </div>
        <div class="template-card__info">
          <span class="template-card__name">{{ tpl.name }}</span>
          <span class="template-card__size">{{ tpl.width }} × {{ tpl.height }}</span>
        </div>
      </div>

      <!-- Custom template card -->
      <div
        class="template-card template-card--custom"
        :class="{ 'template-card--active': selected === 'custom' }"
        @click="selected = 'custom'"
      >
        <div class="template-card__preview">
          <div
            class="template-card__shape template-card__shape--custom"
            :style="customShapeStyle"
          ></div>
        </div>
        <div class="template-card__info">
          <span class="template-card__name">自定义尺寸</span>
          <div class="template-card__custom-inputs" @click.stop>
            <el-input-number
              v-model="customWidth"
              :min="1"
              :max="10000"
              size="small"
              controls-position="right"
              placeholder="宽"
            />
            <span class="template-card__x">&times;</span>
            <el-input-number
              v-model="customHeight"
              :min="1"
              :max="10000"
              size="small"
              controls-position="right"
              placeholder="高"
            />
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="$emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :disabled="!canConfirm" @click="confirmSelection">
        使用此模板
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'select'])

const templates = [
  { name: 'Instagram Post',  width: 1080, height: 1080 },
  { name: 'Instagram Story', width: 1080, height: 1920 },
  { name: 'Facebook Cover',  width: 820,  height: 312  },
  { name: 'Twitter Post',    width: 1200, height: 675  },
  { name: 'YouTube 缩略图',  width: 1280, height: 720  },
  { name: 'Poster A4',       width: 2480, height: 3508 },
  { name: '名片',            width: 1050, height: 600  },
  { name: '桌面壁纸',        width: 1920, height: 1080 },
  { name: '手机壁纸',        width: 1080, height: 1920 },
]

const selected = ref(null)
const customWidth = ref(1920)
const customHeight = ref(1080)

const MAX_PREVIEW_HEIGHT = 120
const MAX_PREVIEW_WIDTH = 80

const customShapeStyle = computed(() => {
  const w = customWidth.value || 1
  const h = customHeight.value || 1
  return calcShapeStyle(w, h)
})

const canConfirm = computed(() => {
  if (selected.value === 'custom') {
    return customWidth.value > 0 && customHeight.value > 0
  }
  return selected.value !== null
})

function getShapeStyle(tpl) {
  return calcShapeStyle(tpl.width, tpl.height)
}

function calcShapeStyle(w, h) {
  const aspect = w / h
  let width, height

  if (aspect >= 1) {
    width = Math.min(MAX_PREVIEW_WIDTH, MAX_PREVIEW_HEIGHT * aspect)
    height = width / aspect
  } else {
    height = Math.min(MAX_PREVIEW_HEIGHT, MAX_PREVIEW_WIDTH / aspect)
    width = height * aspect
  }

  return {
    width: `${Math.round(width)}px`,
    height: `${Math.round(height)}px`,
  }
}

function handleSelect(tpl) {
  selected.value = tpl.name
  emit('select', { name: tpl.name, width: tpl.width, height: tpl.height })
  emit('update:modelValue', false)
}

function confirmSelection() {
  if (selected.value === 'custom') {
    emit('select', {
      name: '自定义尺寸',
      width: customWidth.value,
      height: customHeight.value,
    })
    emit('update:modelValue', false)
  }
}

watch(() => props.modelValue, (val) => {
  if (val) {
    selected.value = null
  }
})
</script>

<style scoped>
.template-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-4);
}

.template-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-4);
  border-radius: var(--radius);
  border: 4px solid transparent;
  background: var(--background-muted);
  cursor: pointer;
  transition: all var(--transition-fast);
  box-shadow: var(--shadow-sm);
}

.template-card:hover {
  border-color: var(--border-hover);
  background: var(--background-elevated);
  box-shadow: var(--shadow);
  transform: translateY(-2px);
}

.template-card--active {
  border-color: var(--primary);
  background: var(--primary-muted);
  box-shadow: var(--glow-sm);
}

.template-card--active:hover {
  border-color: var(--primary-hover);
  box-shadow: var(--glow);
}

.template-card__preview {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 130px;
}

.template-card__shape {
  border-radius: var(--radius-sm);
  background: linear-gradient(135deg, var(--primary-muted) 0%, var(--background-elevated) 100%);
  border: 2px dashed var(--border);
  transition: all var(--transition-fast);
}

.template-card--active .template-card__shape {
  border-color: var(--primary);
  background: linear-gradient(135deg, var(--primary-glow) 0%, var(--primary-muted) 100%);
}

.template-card__shape--custom {
  border-style: dotted;
  background: var(--background-elevated);
}

.template-card__info {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-1);
  width: 100%;
}

.template-card__name {
  font-family: var(--font-mono);
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--foreground);
  text-align: center;
}

.template-card__size {
  font-size: 11px;
  color: var(--foreground-muted);
  font-family: var(--font-mono);
}

.template-card__custom-inputs {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-top: var(--space-1);
}

.template-card__x {
  color: var(--foreground-subtle);
  font-size: 12px;
}

/* Element Plus overrides scoped to this component */
:deep(.el-dialog) {
  border: 4px solid var(--border);
}

:deep(.el-input-number) {
  width: 100px;
}

:deep(.el-input-number .el-input__wrapper) {
  border-radius: var(--radius-sm);
}
</style>
