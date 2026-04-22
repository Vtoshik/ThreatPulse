<template>
  <div class="page-title">
    <div class="page-title__row">
      <span class="page-title__h1">{{ title }}</span>
      <span v-if="sub" class="page-title__sub">{{ sub }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  sub?: string
}>()
</script>

<style scoped>
.page-title {
  margin-bottom: 24px;
}

.page-title__row {
  display: flex;
  align-items: baseline;
  gap: 12px;
  flex-wrap: wrap;
}

.page-title__h1 {
  font-size: 22px;
  font-weight: 600;
  letter-spacing: -1px;
  color: var(--tp-text);
  line-height: 1.2;
}

.page-title__sub {
  font-size: 15px;
  font-weight: 400;
  color: var(--tp-muted);
  letter-spacing: -0.3px;
}
</style>
