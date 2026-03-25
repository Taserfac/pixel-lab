<template>
  <div class="import-area">
    <div class="import-box" @click="$emit('select')">
      <el-icon :size="64">
        <Upload />
      </el-icon>
      <h3>导入图片开始编辑</h3>
      <p>从个人中心选择图片，或上传新图片</p>
      <div class="import-buttons">
        <el-button type="primary" @click.stop="$emit('select')">
          从个人中心选择
        </el-button>
        <el-button @click.stop="triggerUpload">
          本地上传
        </el-button>
      </div>
      <input
        ref="fileInput"
        type="file"
        accept="image/*"
        style="display: none"
        @change="handleUpload"
      >
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Upload } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const emit = defineEmits(['select', 'upload'])

const fileInput = ref(null)

const triggerUpload = () => {
  fileInput.value?.click()
}

const handleUpload = (e) => {
  const file = e.target.files[0]
  if (!file) return

  if (!file.type.startsWith('image/')) {
    ElMessage.error('请选择图片文件')
    return
  }

  // 文件大小限制 10MB
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 10MB')
    return
  }

  const url = URL.createObjectURL(file)
  emit('upload', { url, name: file.name, file })
  e.target.value = ''
}
</script>

<style scoped>
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
</style>