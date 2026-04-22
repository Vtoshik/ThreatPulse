<template>
  <div class="empty-state">
    <div class="empty-state__icon">{{ icon }}</div>
    <div class="empty-state__title">{{ title }}</div>
    <div v-if="desc" class="empty-state__desc">{{ desc }}</div>
    <slot />
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  icon?: string
  title: string
  desc?: string
}>(), {
  icon: '○',
})
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 64px 24px;
  gap: 12px;
  text-align: center;
}

.empty-state__icon {
  font-size: 32px;
  color: var(--tp-dimmer);
  margin-bottom: 4px;
}

.empty-state__title {
  font-size: 14px;
  font-weight: 500;
  color: var(--tp-sec);
  letter-spacing: -0.2px;
}

.empty-state__desc {
  font-size: 12px;
  color: var(--tp-muted);
  max-width: 280px;
  line-height: 1.6;
}
</style>
