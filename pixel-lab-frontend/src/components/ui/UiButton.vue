<template>
  <button
    class="ui-button"
    :class="[`ui-button--${variant}`, `ui-button--${size}`, { 'is-block': block, 'is-loading': loading }]"
    :type="type"
    :disabled="disabled || loading"
  >
    <span v-if="loading" class="ui-button__spinner" aria-hidden="true" />
    <span v-if="$slots.icon" class="ui-button__icon"><slot name="icon" /></span>
    <span><slot /></span>
  </button>
</template>

<script setup>
defineProps({
  variant: {
    type: String,
    default: 'primary',
    validator: value => ['primary', 'secondary', 'ghost', 'danger'].includes(value)
  },
  size: {
    type: String,
    default: 'md',
    validator: value => ['sm', 'md', 'lg'].includes(value)
  },
  type: {
    type: String,
    default: 'button'
  },
  block: Boolean,
  loading: Boolean,
  disabled: Boolean
})
</script>

<style scoped>
.ui-button {
  min-height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  border: 1px solid transparent;
  border-radius: var(--radius-md);
  padding: 0 var(--space-4);
  font-weight: 680;
  line-height: 1;
  cursor: pointer;
  transition: transform var(--transition-fast), background var(--transition-fast), border-color var(--transition-fast), box-shadow var(--transition-fast), color var(--transition-fast);
}

.ui-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.ui-button:active:not(:disabled) {
  transform: scale(0.98);
}

.ui-button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.ui-button--primary {
  color: #fff;
  background: var(--primary);
}

.ui-button--primary:hover:not(:disabled) {
  background: var(--primary-hover);
}

.ui-button--secondary {
  color: var(--text-primary);
  border-color: var(--border);
  background: var(--card);
}

.ui-button--secondary:hover:not(:disabled) {
  border-color: var(--primary-border);
  color: var(--primary-active);
}

.ui-button--ghost {
  color: var(--text-secondary);
  background: transparent;
}

.ui-button--ghost:hover:not(:disabled) {
  color: var(--primary-active);
  background: var(--primary-soft);
  box-shadow: none;
}

.ui-button--danger {
  color: #fff;
  background: var(--danger);
}

.ui-button--sm {
  min-height: 32px;
  padding-inline: var(--space-3);
  font-size: 12px;
}

.ui-button--lg {
  min-height: 48px;
  padding-inline: var(--space-6);
  font-size: 15px;
}

.is-block {
  width: 100%;
}

.ui-button__icon {
  display: inline-flex;
  font-size: 1.1em;
}

.ui-button__spinner {
  width: 14px;
  height: 14px;
  border: 2px solid currentColor;
  border-right-color: transparent;
  border-radius: 50%;
  animation: ui-spin 0.7s linear infinite;
}

@keyframes ui-spin {
  to { transform: rotate(360deg); }
}
</style>
