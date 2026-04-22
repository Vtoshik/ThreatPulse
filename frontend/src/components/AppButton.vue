<template>
  <button
    class="app-btn"
    :class="[`app-btn--${variant}`, `app-btn--${size}`]"
    :disabled="disabled"
    @click="$emit('click')"
  >
    <slot />
  </button>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  variant?: 'primary' | 'ghost' | 'danger'
  size?: 'sm' | 'md' | 'lg'
  disabled?: boolean
}>(), {
  variant: 'primary',
  size: 'md',
  disabled: false,
})

defineEmits<{ click: [] }>()
</script>

<style scoped>
.app-btn {
  border: none;
  cursor: pointer;
  font-family: var(--tp-font);
  border-radius: 6px;
  font-weight: 500;
  transition: opacity 0.15s, box-shadow 0.15s;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  letter-spacing: -0.1px;
  white-space: nowrap;
}

.app-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* Sizes */
.app-btn--sm { padding: 5px 12px; font-size: 12px; }
.app-btn--md { padding: 8px 16px; font-size: 13px; }
.app-btn--lg { padding: 10px 20px; font-size: 14px; }

/* Variants */
.app-btn--primary {
  background: var(--tp-text);
  color: var(--tp-bg);
}
.app-btn--primary:hover:not(:disabled) {
  opacity: 0.9;
}

.app-btn--ghost {
  background: transparent;
  color: var(--tp-text);
  box-shadow: var(--tp-sb);
}
.app-btn--ghost:hover:not(:disabled) {
  box-shadow: var(--tp-sb2);
}

.app-btn--danger {
  background: var(--tp-crit-bg);
  color: var(--tp-crit);
  box-shadow: var(--tp-crit-ring) 0 0 0 1px;
}
.app-btn--danger:hover:not(:disabled) {
  opacity: 0.85;
}
</style>
