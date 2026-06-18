<template>
  <component
    :is="as"
    class="ui-card"
    :class="[`ui-card--${padding}`, { 'is-interactive': interactive }]"
  >
    <slot />
  </component>
</template>

<script setup>
defineProps({
  as: { type: String, default: 'section' },
  padding: {
    type: String,
    default: 'md',
    validator: value => ['none', 'sm', 'md', 'lg'].includes(value)
  },
  interactive: Boolean
})
</script>

<style scoped>
.ui-card {
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  background: var(--card);
  box-shadow: var(--shadow-sm);
}

.ui-card--sm { padding: var(--space-3); }
.ui-card--md { padding: var(--space-4); }
.ui-card--lg { padding: var(--space-6); }

.is-interactive {
  transition: transform var(--transition-base), border-color var(--transition-base), box-shadow var(--transition-base);
}

.is-interactive:hover {
  transform: translateY(-2px);
  border-color: var(--primary-border);
  box-shadow: var(--shadow-md);
}

.is-interactive:active {
  transform: scale(0.98);
}
</style>
