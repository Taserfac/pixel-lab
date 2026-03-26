<template>
  <div class="canvas-area">
    <div
      ref="wrapperRef"
      class="canvas-wrapper"
    >
      <canvas
        ref="canvasRef"
        class="editor-canvas"
        :style="canvasStyle"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  canvasStyle: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['canvas-ready', 'resize'])

const wrapperRef = ref(null)
const canvasRef = ref(null)

// 暴露 refs 给父组件
defineExpose({
  canvas: canvasRef,
  wrapper: wrapperRef
})

// 监听窗口大小变化
onMounted(() => {
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})

const handleResize = () => {
  emit('resize')
}
</script>

<style scoped>
.canvas-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--background-soft);
  border: 4px solid var(--border);
  box-shadow: 8px 8px 0px 0px var(--border);
  min-width: 0;
  overflow: hidden;
}

.canvas-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-6);
}

.editor-canvas {
  max-width: 100%;
  max-height: 100%;
  border: 4px solid var(--border);
  box-shadow: 6px 6px 0px 0px var(--border);
}
</style>