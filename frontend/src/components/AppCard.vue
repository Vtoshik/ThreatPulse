<template>
  <div
    class="app-card"
    :class="{ 'app-card--clickable': !!onClick }"
    :style="highlightStyle"
    @click="onClick?.()"
  >
    <slot />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useSeverity } from 'src/composables/useSeverity'

const props = withDefaults(defineProps<{
  highlight?: string
  onClick?: () => void
}>(), {})

const { sevColor } = useSeverity()

const highlightStyle = computed(() => {
  if (!props.highlight) return {}
  return {
    boxShadow: `${sevColor(props.highlight)} 0 0 0 1px, rgba(0,0,0,0.5) 0 4px 16px`,
  }
})
</script>

<style scoped>
.app-card {
  background: var(--tp-surface);
  border-radius: 8px;
  box-shadow: var(--tp-card);
  transition: opacity 0.15s;
}

.app-card--clickable {
  cursor: pointer;
}

.app-card--clickable:hover {
  opacity: 0.92;
}
</style>
