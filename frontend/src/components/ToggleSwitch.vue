<template>
  <div
    class="toggle"
    :class="{ 'toggle--on': modelValue }"
    @click="$emit('update:modelValue', !modelValue)"
  >
    <div class="toggle__thumb" />
  </div>
</template>

<script setup lang="ts">
defineProps<{ modelValue: boolean }>()
defineEmits<{ 'update:modelValue': [value: boolean] }>()
</script>

<style scoped>
.toggle {
  width: 32px;
  height: 18px;
  border-radius: 9999px;
  cursor: pointer;
  background: var(--tp-surf3);
  box-shadow: var(--tp-sb);
  position: relative;
  transition: background 0.2s;
  flex-shrink: 0;
}

.toggle--on {
  background: var(--tp-text);
  box-shadow: none;
}

.toggle__thumb {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: var(--tp-muted);
  transition: left 0.2s, background 0.2s;
}

.toggle--on .toggle__thumb {
  left: 17px;
  background: var(--tp-bg);
}
</style>
