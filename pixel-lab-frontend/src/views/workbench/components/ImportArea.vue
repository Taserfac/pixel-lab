<template>
  <div class="import-area">
    <div
      class="import-box"
      @click="$emit('select')"
    >
      <div class="import-icon">
        <el-icon :size="34">
          <Upload />
        </el-icon>
      </div>
      <p class="import-kicker">Image Workshop</p>
      <h3>导入图片开始编辑</h3>
      <p class="import-copy">从个人中心继续处理旧作品，或直接上传本地图片进行裁剪、滤镜、像素化与导出。</p>
      <div class="import-buttons">
        <el-button
          type="primary"
          @click.stop="$emit('select')"
        >
          从个人中心选择
        </el-button>
        <el-button @click.stop="triggerUpload">
          本地上传
        </el-button>
      </div>
      <div class="import-tips">
        <span>支持 JPG / PNG / WebP</span>
        <span>最大 10MB</span>
        <span>可导出 PNG / JPG / WebP</span>
      </div>
      <input
        ref="fileInput"
        type="file"
        accept="image/*"
        style="display: none"
        @click.stop
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
  min-height: 0;
}

.import-box {
  width: min(720px, 100%);
  text-align: center;
  padding: var(--space-12);
  background: var(--background-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-md);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.import-box:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-lg);
}

.import-icon {
  width: 72px;
  height: 72px;
  display: grid;
  place-items: center;
  margin: 0 auto var(--space-5);
  border-radius: var(--radius-lg);
  background: var(--primary-muted);
  color: var(--primary);
  box-shadow: var(--glow-sm);
}

.import-kicker {
  margin: 0 0 var(--space-2);
  color: var(--foreground-muted);
  font-family: var(--font-mono);
  font-size: 12px;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0;
}

.import-box h3 {
  margin: 0;
  color: var(--foreground);
  font-size: 32px;
  line-height: 1.2;
  font-weight: 800;
}

.import-copy {
  max-width: 520px;
  color: var(--foreground-muted);
  margin: var(--space-4) auto var(--space-6);
  font-size: 16px;
  line-height: 1.8;
}

.import-buttons {
  display: flex;
  gap: var(--space-4);
  justify-content: center;
  flex-wrap: wrap;
}

.import-tips {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-top: var(--space-6);
}

.import-tips span {
  padding: 6px 10px;
  border-radius: var(--radius-full);
  background: var(--background-muted);
  color: var(--foreground-muted);
  font-size: 12px;
  font-weight: 650;
}

@media (max-width: 640px) {
  .import-box {
    padding: var(--space-8) var(--space-5);
  }

  .import-box h3 {
    font-size: 26px;
  }

  .import-buttons .el-button {
    width: 100%;
  }
}
</style>
