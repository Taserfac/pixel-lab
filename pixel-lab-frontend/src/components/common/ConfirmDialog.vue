<!--
  【文件路径】src/components/common/ConfirmDialog.vue
  【文件功能说明】通用确认弹窗组件
  - 封装 Element Plus 的 Dialog
  - 支持自定义标题、内容、按钮文字
  - 支持确认/取消回调
-->

<template>
  <el-dialog
    v-model="visible"
    :title="title"
    width="400px"
    :close-on-click-modal="false"
    :show-close="showClose"
    @close="handleCancel"
  >
    <div class="confirm-content">
      <el-icon v-if="type" :size="48" :class="`type-${type}`">
        <Warning v-if="type === 'warning'" />
        <CircleCheck v-else-if="type === 'success'" />
        <CircleClose v-else-if="type === 'error'" />
        <InfoFilled v-else />
      </el-icon>
      <p class="message">{{ message }}</p>
    </div>
    
    <template #footer>
      <el-button @click="handleCancel" v-if="showCancel">{{ cancelText }}</el-button>
      <el-button type="primary" @click="handleConfirm" :loading="loading">
        {{ confirmText }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { Warning, CircleCheck, CircleClose, InfoFilled } from '@element-plus/icons-vue'

const props = defineProps({
  // 弹窗标题
  title: {
    type: String,
    default: '确认'
  },
  // 提示类型：warning | success | error | info
  type: {
    type: String,
    default: 'warning'
  },
  // 提示消息
  message: {
    type: String,
    default: ''
  },
  // 确认按钮文字
  confirmText: {
    type: String,
    default: '确定'
  },
  // 取消按钮文字
  cancelText: {
    type: String,
    default: '取消'
  },
  // 是否显示取消按钮
  showCancel: {
    type: Boolean,
    default: true
  },
  // 是否显示关闭按钮
  showClose: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['confirm', 'cancel'])

// 弹窗显示状态
const visible = ref(false)
// 加载状态
const loading = ref(false)

// 打开弹窗
const open = () => {
  visible.value = true
}

// 关闭弹窗
const close = () => {
  visible.value = false
  loading.value = false
}

// 确认
const handleConfirm = async () => {
  loading.value = true
  try {
    await emit('confirm')
    close()
  } catch (error) {
    loading.value = false
  }
}

// 取消
const handleCancel = () => {
  emit('cancel')
  close()
}

// 暴露方法
defineExpose({
  open,
  close
})
</script>

<style scoped>
.confirm-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--space-6) 0;
}

.message {
  margin-top: var(--space-4);
  font-size: 14px;
  color: var(--foreground);
  text-align: center;
}

.type-warning {
  color: var(--warning);
}

.type-success {
  color: var(--success);
}

.type-error {
  color: var(--error);
}

.type-info {
  color: var(--info);
}
</style>
