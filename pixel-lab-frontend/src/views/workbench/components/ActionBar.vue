<template>
  <div class="action-bar">
    <div class="tool-actions" aria-label="编辑工具栏">
      <button type="button" @click="$emit('back')">
        <el-icon><ArrowLeft /></el-icon>
        <span>返回</span>
      </button>
      <button
        type="button"
        :disabled="!canUndo"
        @click="$emit('undo')"
      >
        <el-icon><Back /></el-icon>
        <span>撤销</span>
      </button>
      <button
        type="button"
        :disabled="!canRedo"
        @click="$emit('redo')"
      >
        <el-icon><Right /></el-icon>
        <span>重做</span>
      </button>
      <button type="button" @click="$emit('reset')">
        <el-icon><Refresh /></el-icon>
        <span>重置</span>
      </button>
      <button type="button" @click="$emit('fit')">
        <el-icon><FullScreen /></el-icon>
        <span>适应窗口</span>
      </button>
      <button type="button" @click="$emit('change-image')">
        <el-icon><FolderOpened /></el-icon>
        <span>换一张</span>
      </button>
      <button type="button" @click="$emit('draft')">
        <el-icon><Document /></el-icon>
        <span>保存草稿</span>
      </button>
      <button type="button" @click="$emit('save')">
        <el-icon><Upload /></el-icon>
        <span>保存到个人中心</span>
      </button>
    </div>

    <div class="primary-actions">
      <button class="publish-action" type="button" @click="$emit('publish')">
        <el-icon><Promotion /></el-icon>
        <span>发布到社区</span>
      </button>
      <button class="download-action" type="button" @click="$emit('download')">
        <el-icon><Download /></el-icon>
        <span>下载</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import {
  ArrowLeft,
  Back,
  Document,
  Download,
  FolderOpened,
  FullScreen,
  Promotion,
  Refresh,
  Right,
  Upload
} from '@element-plus/icons-vue'

defineProps({
  canUndo: { type: Boolean, default: false },
  canRedo: { type: Boolean, default: false }
})

defineEmits(['back', 'undo', 'redo', 'reset', 'fit', 'change-image', 'draft', 'save', 'publish', 'download'])
</script>

<style scoped>
.action-bar {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  min-height: 78px;
  margin-top: 12px;
  padding: 12px 16px;
  border: 1px solid rgba(220, 226, 232, 0.82);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 18px 44px rgba(17, 24, 39, 0.08);
}

.tool-actions,
.primary-actions {
  display: flex;
  align-items: center;
  min-width: 0;
}

.tool-actions {
  flex: 1 1 auto;
  gap: 2px;
  justify-content: space-between;
}

.primary-actions {
  flex: 0 0 auto;
  gap: 10px;
}

button {
  min-width: 72px;
  min-height: 52px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 5px;
  border: 0;
  border-radius: 15px;
  background: transparent;
  color: #4b5563;
  font-size: 12px;
  font-weight: 750;
  cursor: pointer;
  transition:
    background 0.18s ease,
    color 0.18s ease,
    opacity 0.18s ease,
    transform 0.18s ease,
    box-shadow 0.18s ease;
}

button:hover:not(:disabled) {
  background: #f3fbf7;
  color: #08a965;
  transform: translateY(-1px);
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.42;
}

button .el-icon {
  font-size: 18px;
}

.publish-action,
.download-action {
  min-width: 142px;
  flex-direction: row;
  border: 1px solid rgba(18, 199, 127, 0.5);
  border-radius: 999px;
  color: #08a965;
  background: #fff;
}

.publish-action:hover {
  box-shadow: 0 12px 26px rgba(18, 199, 127, 0.16);
}

.download-action {
  border-color: transparent;
  color: #fff;
  background: linear-gradient(135deg, #13c77f, #08aa68);
  box-shadow: 0 14px 26px rgba(18, 199, 127, 0.24);
}

.download-action:hover {
  color: #fff !important;
  background: linear-gradient(135deg, #11bd78, #079b5f) !important;
}

@media (max-width: 1366px) {
  .action-bar {
    gap: 10px;
    padding: 10px 12px;
  }

  button {
    min-width: 58px;
    font-size: 11px;
  }

  .publish-action,
  .download-action {
    min-width: 122px;
  }
}

@media (max-width: 1080px) {
  .action-bar {
    align-items: stretch;
    flex-direction: column;
  }

  .tool-actions {
    flex-wrap: wrap;
    justify-content: center;
  }

  .primary-actions {
    justify-content: center;
  }
}

@media (max-width: 640px) {
  .primary-actions {
    flex-direction: column;
  }

  .publish-action,
  .download-action {
    width: 100%;
  }
}
</style>
