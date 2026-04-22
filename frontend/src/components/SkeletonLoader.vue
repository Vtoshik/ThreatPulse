<template>
  <div class="skeleton" :style="{ width, height: `${height}px`, borderRadius: `${radius}px` }" />
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  width?: string
  height?: number
  radius?: number
}>(), {
  width: '100%',
  height: 14,
  radius: 4,
})
</script>

<style scoped>
.skeleton {
  background: var(--tp-surf3);
  position: relative;
  overflow: hidden;
}

.skeleton::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(
    90deg,
    transparent 0%,
    rgba(255, 255, 255, 0.04) 50%,
    transparent 100%
  );
  animation: skeletonSlide 1.5s infinite;
}
</style>
