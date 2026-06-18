<template>
  <div
    class="upload-box"
    :class="{ 'is-dragging': dragging, 'has-file': previewUrl, 'has-error': error }"
    role="button"
    tabindex="0"
    @click="inputRef?.click()"
    @keydown.enter.prevent="inputRef?.click()"
    @keydown.space.prevent="inputRef?.click()"
    @dragenter.prevent="dragging = true"
    @dragover.prevent
    @dragleave.prevent="dragging = false"
    @drop.prevent="onDrop"
  >
    <input ref="inputRef" class="visually-hidden" type="file" :accept="accept" @change="onInput">
    <img v-if="previewUrl" :src="previewUrl" alt="待发布图片预览">
    <div v-else class="upload-box__empty">
      <span class="upload-box__icon"><el-icon><UploadFilled /></el-icon></span>
      <strong>拖拽图片到这里，或点击选择</strong>
      <span>支持 JPG、PNG、WebP，最大 {{ maxSize }} MB</span>
    </div>
    <div v-if="previewUrl" class="upload-box__overlay">
      <span>点击更换图片</span>
    </div>
  </div>
  <p v-if="error" class="upload-box__error">{{ error }}</p>
</template>

<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'
import { UploadFilled } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: { type: Object, default: null },
  accept: { type: String, default: 'image/jpeg,image/png,image/webp' },
  maxSize: { type: Number, default: 8 }
})

const emit = defineEmits(['update:modelValue', 'change'])
const inputRef = ref(null)
const dragging = ref(false)
const previewUrl = ref('')
const error = ref('')

const revokePreview = () => {
  if (previewUrl.value.startsWith('blob:')) URL.revokeObjectURL(previewUrl.value)
}

const setFile = file => {
  error.value = ''
  if (!file?.type?.startsWith('image/')) {
    error.value = '请选择图片文件'
    return
  }
  if (file.size > props.maxSize * 1024 * 1024) {
    error.value = `图片不能超过 ${props.maxSize} MB`
    return
  }
  revokePreview()
  previewUrl.value = URL.createObjectURL(file)
  emit('update:modelValue', file)
  emit('change', file)
}

const onInput = event => setFile(event.target.files?.[0])
const onDrop = event => {
  dragging.value = false
  setFile(event.dataTransfer.files?.[0])
}

watch(() => props.modelValue, value => {
  if (!value) {
    revokePreview()
    previewUrl.value = ''
    if (inputRef.value) inputRef.value.value = ''
  }
})

onBeforeUnmount(revokePreview)
</script>

<style scoped>
.upload-box {
  position: relative;
  min-height: 340px;
  display: grid;
  place-items: center;
  overflow: hidden;
  border: 1.5px dashed var(--border-strong);
  border-radius: var(--radius-lg);
  background: color-mix(in srgb, var(--card) 72%, var(--bg));
  cursor: pointer;
  transition: border-color var(--transition-base), background var(--transition-base), transform var(--transition-base), box-shadow var(--transition-base);
}

.upload-box:hover,
.upload-box.is-dragging {
  transform: translateY(-2px);
  border-color: var(--primary);
  background: var(--primary-soft);
  box-shadow: var(--shadow-md);
}

.upload-box:active { transform: scale(0.98); }
.upload-box.has-error { border-color: var(--danger); }

.upload-box > img {
  width: 100%;
  height: 100%;
  min-height: 340px;
  max-height: 520px;
  display: block;
  object-fit: contain;
  background: var(--surface-muted);
}

.upload-box__empty {
  display: grid;
  justify-items: center;
  gap: var(--space-2);
  padding: var(--space-8);
  text-align: center;
  color: var(--text-secondary);
}

.upload-box__empty strong { color: var(--text-primary); font-size: 15px; }
.upload-box__empty > span:last-child { font-size: 12px; }

.upload-box__icon {
  width: 54px;
  height: 54px;
  display: grid;
  place-items: center;
  margin-bottom: var(--space-2);
  border-radius: var(--radius-lg);
  color: var(--primary);
  background: var(--primary-soft);
  font-size: 24px;
}

.upload-box__overlay {
  position: absolute;
  inset: auto var(--space-4) var(--space-4);
  display: flex;
  justify-content: center;
  border-radius: var(--radius-md);
  padding: var(--space-2) var(--space-4);
  color: #fff;
  background: rgba(20, 24, 22, 0.72);
  opacity: 0;
  transition: opacity var(--transition-fast);
}

.upload-box.has-file:hover .upload-box__overlay { opacity: 1; }
.upload-box__error { margin: var(--space-2) 0 0; color: var(--danger); font-size: 12px; }
</style>
