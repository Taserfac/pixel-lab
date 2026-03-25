import { ref, computed } from 'vue'

export function useHistory(maxHistory = 20) {
  const historyStack = ref([])
  const historyIndex = ref(-1)

  const canUndo = computed(() => historyIndex.value > 0)
  const canRedo = computed(() => historyIndex.value < historyStack.value.length - 1)

  const saveHistory = (state) => {
    historyStack.value = historyStack.value.slice(0, historyIndex.value + 1)
    historyStack.value.push(state)
    
    if (historyStack.value.length > maxHistory) {
      historyStack.value.shift()
    }
    
    historyIndex.value = historyStack.value.length - 1
  }

  const undo = () => {
    if (!canUndo.value) return null
    historyIndex.value--
    return historyStack.value[historyIndex.value]
  }

  const redo = () => {
    if (!canRedo.value) return null
    historyIndex.value++
    return historyStack.value[historyIndex.value]
  }

  const getCurrentState = () => {
    return historyStack.value[historyIndex.value]
  }

  const clearHistory = () => {
    historyStack.value = []
    historyIndex.value = -1
  }

  return {
    historyStack,
    historyIndex,
    canUndo,
    canRedo,
    saveHistory,
    undo,
    redo,
    getCurrentState,
    clearHistory
  }
}