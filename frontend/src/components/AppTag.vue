<template>
  <span class="app-tag" :style="tagStyle">
    {{ label }}
    <span v-if="onRemove" class="app-tag__remove" @click.stop="onRemove()">×</span>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  label: string
  color?: string
  onRemove?: () => void
}>(), {
  color: 'var(--tp-muted)',
})

const tagStyle = computed(() => ({
  background: `${props.color}15`,
  color: props.color,
  boxShadow: `${props.color}30 0 0 0 1px`,
}))
</script>

<style scoped>
.app-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 9999px;
  font-size: 11px;
  font-family: var(--tp-font);
}

.app-tag__remove {
  cursor: pointer;
  opacity: 0.6;
  font-size: 12px;
  line-height: 1;
}

.app-tag__remove:hover {
  opacity: 1;
}
</style>
