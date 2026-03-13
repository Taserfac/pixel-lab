<!--
  【文件路径】src/components/common/UploadFile.vue
  【文件功能说明】通用文件上传组件
  - 支持拖拽上传
  - 支持格式校验、大小限制
  - 显示上传进度
-->

<template>
  <el-upload
    class="upload-file"
    :action="action"
    :headers="headers"
    :data="data"
    :name="name"
    :accept="accept"
    :multiple="multiple"
    :limit="limit"
    :file-list="fileList"
    :before-upload="beforeUpload"
    :on-success="handleSuccess"
    :on-error="handleError"
    :on-progress="handleProgress"
    :on-exceed="handleExceed"
    :on-remove="handleRemove"
    :drag="drag"
    :auto-upload="autoUpload"
    :show-file-list="showFileList"
  >
    <!-- 拖拽上传区域 -->
    <template v-if="drag" #default>
      <el-icon :size="48" class="upload-icon">
        <Upload />
      </el-icon>
      <div class="el-upload__text">
        拖拽文件到此处，或<em>点击上传</em>
      </div>
      <div class="upload-tip" v-if="tip">{{ tip }}</div>
    </template>
    
    <!-- 按钮上传 -->
    <template v-else #default>
      <el-button type="primary" :icon="Upload">
        {{ buttonText }}
      </el-button>
      <div class="upload-tip" v-if="tip">{{ tip }}</div>
    </template>
  </el-upload>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'

const props = defineProps({
  // 上传地址
  action: {
    type: String,
    required: true
  },
  // 请求头
  headers: {
    type: Object,
    default: () => ({})
  },
  // 附加数据
  data: {
    type: Object,
    default: () => ({})
  },
  // 文件字段名
  name: {
    type: String,
    default: 'file'
  },
  // 接受文件类型
  accept: {
    type: String,
    default: ''
  },
  // 是否多选
  multiple: {
    type: Boolean,
    default: false
  },
  // 最大上传数量
  limit: {
    type: Number,
    default: 1
  },
  // 最大文件大小（MB）
  maxSize: {
    type: Number,
    default: 10
  },
  // 是否拖拽上传
  drag: {
    type: Boolean,
    default: false
  },
  // 是否自动上传
  autoUpload: {
    type: Boolean,
    default: true
  },
  // 是否显示文件列表
  showFileList: {
    type: Boolean,
    default: true
  },
  // 按钮文字
  buttonText: {
    type: String,
    default: '点击上传'
  },
  // 提示文字
  tip: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['success', 'error', 'progress', 'exceed', 'remove'])

// 文件列表
const fileList = ref([])

// 上传前校验
const beforeUpload = (file) => {
  // 校验文件大小
  const isLtSize = file.size / 1024 / 1024 < props.maxSize
  if (!isLtSize) {
    ElMessage.error(`文件大小不能超过 ${props.maxSize}MB`)
    return false
  }
  
  return true
}

// 上传成功
const handleSuccess = (response, file, fileList) => {
  ElMessage.success('上传成功')
  emit('success', { response, file, fileList })
}

// 上传失败
const handleError = (error, file, fileList) => {
  ElMessage.error('上传失败')
  emit('error', { error, file, fileList })
}

// 上传进度
const handleProgress = (event, file, fileList) => {
  emit('progress', { event, file, fileList })
}

// 超出限制
const handleExceed = () => {
  ElMessage.warning(`最多只能上传 ${props.limit} 个文件`)
  emit('exceed')
}

// 移除文件
const handleRemove = (file, fileList) => {
  emit('remove', { file, fileList })
}
</script>

<style scoped>
.upload-file {
  width: 100%;
}

.upload-icon {
  color: var(--primary);
  margin-bottom: var(--space-2);
}

.upload-tip {
  margin-top: var(--space-2);
  font-size: 12px;
  color: var(--foreground-muted);
}

:deep(.el-upload-dragger) {
  background-color: var(--background-soft);
  border-color: var(--border);
}

:deep(.el-upload-dragger:hover) {
  border-color: var(--primary);
}
</style>
