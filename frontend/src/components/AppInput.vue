<template>
  <input
    class="app-input"
    :class="{ 'app-input--focused': focused }"
    :type="type"
    :placeholder="placeholder"
    :value="modelValue"
    @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
    @focus="focused = true"
    @blur="focused = false"
  />
</template>

<script setup lang="ts">
import { ref } from 'vue'

withDefaults(defineProps<{
  modelValue?: string
  placeholder?: string
  type?: string
}>(), {
  modelValue: '',
  type: 'text',
})

defineEmits<{ 'update:modelValue': [value: string] }>()

const focused = ref(false)
</script>

<style scoped>
.app-input {
  width: 100%;
  padding: 9px 12px;
  background: var(--tp-surf2);
  color: var(--tp-text);
  font-family: var(--tp-font);
  font-size: 13px;
  border: none;
  border-radius: 6px;
  outline: none;
  box-shadow: var(--tp-sb);
  transition: box-shadow 0.15s;
}

.app-input::placeholder {
  color: var(--tp-muted);
}

.app-input--focused,
.app-input:focus {
  box-shadow: rgba(255, 255, 255, 0.2) 0 0 0 1px;
}
</style>
