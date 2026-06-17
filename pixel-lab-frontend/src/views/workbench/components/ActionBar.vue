<template>
  <div class="action-bar">
    <div class="left-actions">
      <el-button
        :disabled="!canUndo"
        @click="$emit('undo')"
      >
        <el-icon><Back /></el-icon>
        撤销
      </el-button>
      <el-button
        :disabled="!canRedo"
        @click="$emit('redo')"
      >
        <el-icon><Right /></el-icon>
        重做
      </el-button>
      <el-button @click="$emit('reset')">
        <el-icon><Refresh /></el-icon>
        重置
      </el-button>
    </div>
    <div class="right-actions">
      <el-button @click="$emit('templates')">
        <el-icon><Grid /></el-icon>
        模板
      </el-button>
      <el-button @click="$emit('change-image')">
        <el-icon><FolderOpened /></el-icon>
        换一张
      </el-button>
      <el-button
        type="primary"
        @click="$emit('save')"
      >
        <el-icon><Upload /></el-icon>
        保存到个人中心
      </el-button>
      <el-button
        type="success"
        @click="$emit('download')"
      >
        <el-icon><Download /></el-icon>
        下载
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { Back, Right, Refresh, FolderOpened, Upload, Download, Grid } from '@element-plus/icons-vue'

defineProps({
  canUndo: { type: Boolean, default: false },
  canRedo: { type: Boolean, default: false }
})

defineEmits(['undo', 'redo', 'reset', 'templates', 'change-image', 'save', 'download'])
</script>

<style scoped>
.action-bar {
  display: flex;
  justify-content: space-between;
  padding: var(--space-5);
  background: var(--background-soft);
  border: 4px solid var(--border);
  box-shadow: 6px 6px 0px 0px var(--border);
  margin-top: var(--space-5);
  flex-shrink: 0;
}

.left-actions,
.right-actions {
  display: flex;
  gap: var(--space-3);
}
</style>