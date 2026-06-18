<template>
  <component
    :is="clickable ? 'button' : 'span'"
    class="ui-tag"
    :class="{ 'is-active': active, 'is-clickable': clickable }"
    :type="clickable ? 'button' : undefined"
  >
    <slot />
    <button v-if="removable" class="ui-tag__remove" type="button" aria-label="移除标签" @click.stop="$emit('remove')">×</button>
  </component>
</template>

<script setup>
defineProps({ active: Boolean, clickable: Boolean, removable: Boolean })
defineEmits(['remove'])
</script>

<style scoped>
.ui-tag {
  min-height: 30px;
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  border: 1px solid var(--border);
  border-radius: var(--radius-full);
  padding: 4px 12px;
  color: var(--text-secondary);
  background: var(--card);
  font-size: 12px;
  font-weight: 620;
}

.is-clickable {
  cursor: pointer;
  transition: transform var(--transition-fast), color var(--transition-fast), background var(--transition-fast), border-color var(--transition-fast);
}

.is-clickable:hover,
.is-active {
  border-color: var(--primary-border);
  color: var(--primary-active);
  background: var(--primary-soft);
}

.is-clickable:active { transform: scale(0.98); }

.ui-tag__remove {
  border: 0;
  padding: 0;
  color: currentColor;
  background: transparent;
  cursor: pointer;
}
</style>
