<template>
  <div class="score-bar">
    <div class="score-bar__track">
      <div class="score-bar__fill" :style="fillStyle" />
    </div>
    <span class="score-bar__value" :style="{ color: color }">{{ score }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useSeverity } from 'src/composables/useSeverity'

const props = defineProps<{
  score: number
  severity?: string
}>()

const { sevColor } = useSeverity()

const color = computed(() => {
  if (props.severity) return sevColor(props.severity)
  if (props.score >= 9) return sevColor('CRITICAL')
  if (props.score >= 7) return sevColor('HIGH')
  if (props.score >= 4) return sevColor('MEDIUM')
  return sevColor('LOW')
})

const fillStyle = computed(() => ({
  width: `${(props.score / 10) * 100}%`,
  background: color.value,
}))
</script>

<style scoped>
.score-bar {
  display: flex;
  align-items: center;
  gap: 8px;
}

.score-bar__track {
  flex: 1;
  height: 3px;
  background: var(--tp-surf3);
  border-radius: 1px;
}

.score-bar__fill {
  height: 100%;
  border-radius: 1px;
  transition: width 0.3s ease;
}

.score-bar__value {
  font-size: 12px;
  font-weight: 600;
  font-family: var(--tp-mono);
  min-width: 28px;
  text-align: right;
  letter-spacing: -0.3px;
}
</style>
