<template>
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-if="modelValue" class="ui-modal" role="presentation" @mousedown.self="closeOnBackdrop && close()">
        <section class="ui-modal__panel" role="dialog" aria-modal="true" :aria-labelledby="titleId" :style="{ maxWidth }">
          <header class="ui-modal__header">
            <h2 :id="titleId">{{ title }}</h2>
            <button type="button" aria-label="关闭" @click="close">×</button>
          </header>
          <div class="ui-modal__body"><slot /></div>
          <footer v-if="$slots.footer" class="ui-modal__footer"><slot name="footer" /></footer>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { onBeforeUnmount, watch } from 'vue'

const props = defineProps({
  modelValue: Boolean,
  title: { type: String, required: true },
  maxWidth: { type: String, default: '560px' },
  closeOnBackdrop: { type: Boolean, default: true }
})

const emit = defineEmits(['update:modelValue'])
const titleId = `ui-modal-${Math.random().toString(36).slice(2)}`
const close = () => emit('update:modelValue', false)
const onKeydown = event => {
  if (event.key === 'Escape' && props.modelValue) close()
}

watch(() => props.modelValue, value => {
  document.body.style.overflow = value ? 'hidden' : ''
}, { immediate: true })

window.addEventListener('keydown', onKeydown)
onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
  document.body.style.overflow = ''
})
</script>

<style scoped>
.ui-modal {
  position: fixed;
  inset: 0;
  z-index: 3000;
  display: grid;
  place-items: center;
  padding: var(--space-6);
  background: rgba(18, 22, 20, 0.46);
  backdrop-filter: blur(5px);
}

.ui-modal__panel {
  width: 100%;
  max-height: min(86vh, 900px);
  overflow: auto;
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  background: var(--card);
  box-shadow: var(--shadow-lg);
}

.ui-modal__header,
.ui-modal__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-4) var(--space-6);
}

.ui-modal__header { border-bottom: 1px solid var(--border); }
.ui-modal__footer { justify-content: flex-end; border-top: 1px solid var(--border); }

.ui-modal__header h2 {
  margin: 0;
  font-size: 18px;
}

.ui-modal__header button {
  width: 32px;
  height: 32px;
  border: 0;
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  background: transparent;
  cursor: pointer;
}

.ui-modal__header button:hover { background: var(--surface-muted); }
.ui-modal__body { padding: var(--space-6); }

.modal-fade-enter-active,
.modal-fade-leave-active { transition: opacity var(--transition-base); }
.modal-fade-enter-active .ui-modal__panel,
.modal-fade-leave-active .ui-modal__panel { transition: transform var(--transition-base); }
.modal-fade-enter-from,
.modal-fade-leave-to { opacity: 0; }
.modal-fade-enter-from .ui-modal__panel,
.modal-fade-leave-to .ui-modal__panel { transform: translateY(12px) scale(0.98); }
</style>
