<template>
  <label class="ui-field">
    <span v-if="label" class="ui-field__label">{{ label }}</span>
    <span class="ui-input-wrap" :class="{ 'has-error': error }">
      <span v-if="$slots.prefix" class="ui-input__prefix"><slot name="prefix" /></span>
      <input
        class="ui-input"
        :value="modelValue"
        :type="type"
        :placeholder="placeholder"
        :disabled="disabled"
        :maxlength="maxlength"
        @input="$emit('update:modelValue', $event.target.value)"
      >
      <button
        v-if="clearable && modelValue"
        class="ui-input__clear"
        type="button"
        aria-label="清空"
        @click="$emit('update:modelValue', '')"
      >×</button>
    </span>
    <span v-if="error" class="ui-field__error">{{ error }}</span>
    <span v-else-if="hint" class="ui-field__hint">{{ hint }}</span>
  </label>
</template>

<script setup>
defineProps({
  modelValue: { type: [String, Number], default: '' },
  label: { type: String, default: '' },
  type: { type: String, default: 'text' },
  placeholder: { type: String, default: '' },
  hint: { type: String, default: '' },
  error: { type: String, default: '' },
  maxlength: { type: [String, Number], default: undefined },
  disabled: Boolean,
  clearable: Boolean
})

defineEmits(['update:modelValue'])
</script>

<style scoped>
.ui-field {
  display: grid;
  gap: var(--space-2);
}

.ui-field__label {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 680;
}

.ui-input-wrap {
  min-height: 44px;
  display: flex;
  align-items: center;
  gap: var(--space-2);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 0 var(--space-3);
  background: var(--card);
  transition: border-color var(--transition-fast), box-shadow var(--transition-fast);
}

.ui-input-wrap:focus-within {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-soft);
}

.ui-input-wrap.has-error {
  border-color: var(--danger);
}

.ui-input {
  width: 100%;
  min-width: 0;
  border: 0;
  outline: 0;
  color: var(--text-primary);
  background: transparent;
}

.ui-input::placeholder {
  color: var(--text-tertiary);
}

.ui-input__prefix {
  display: inline-flex;
  color: var(--text-tertiary);
}

.ui-input__clear {
  border: 0;
  color: var(--text-tertiary);
  background: transparent;
  cursor: pointer;
}

.ui-field__hint,
.ui-field__error {
  font-size: 12px;
}

.ui-field__hint { color: var(--text-tertiary); }
.ui-field__error { color: var(--danger); }
</style>
