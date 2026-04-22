<template>
  <div
    class="threat-row"
    :class="{ 'threat-row--selected': selected, 'threat-row--first': first }"
    :style="selectedStyle"
    @click="$emit('click')"
    @mouseenter="hovered = true"
    @mouseleave="hovered = false"
  >
    <div class="threat-row__title-col">
      <div class="threat-row__title" :class="{ 'threat-row__title--bold': first || selected }">
        {{ threat.title }}
      </div>
      <CveHash :cve="threat.cve" :id="threat.id" />
    </div>

    <span><SeverityBadge :severity="threat.severity" /></span>

    <span class="threat-row__mono">{{ threat.tech }}</span>

    <ScoreBar :score="threat.score" :severity="threat.severity" />

    <span class="threat-row__mono">{{ threat.age }}</span>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { Threat } from 'src/types/threat'
import { useSeverity } from 'src/composables/useSeverity'
import SeverityBadge from './SeverityBadge.vue'
import CveHash from './CveHash.vue'
import ScoreBar from './ScoreBar.vue'

const props = withDefaults(defineProps<{
  threat: Threat
  selected?: boolean
  first?: boolean
}>(), {
  selected: false,
  first: false,
})

defineEmits<{ click: [] }>()

const hovered = ref(false)
const { sevColor } = useSeverity()

const selectedStyle = computed(() => {
  if (props.selected) {
    const c = sevColor(props.threat.severity)
    return {
      borderLeft: `3px solid ${c}`,
      background: `${c}06`,
    }
  }
  return {
    borderLeft: '3px solid transparent',
    background: hovered.value ? 'var(--tp-surf2)' : 'transparent',
  }
})
</script>

<style scoped>
.threat-row {
  padding: 12px 20px;
  display: grid;
  grid-template-columns: 1fr 92px 110px 120px 44px;
  gap: 12px;
  align-items: center;
  box-shadow: inset 0 -1px 0 var(--tp-border);
  cursor: pointer;
  transition: background 0.1s, border-color 0.1s;
}

.threat-row__title-col {
  min-width: 0;
}

.threat-row__title {
  font-size: 13px;
  font-weight: 400;
  margin-bottom: 3px;
  color: var(--tp-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.threat-row__title--bold {
  font-weight: 500;
}

.threat-row__mono {
  font-family: var(--tp-mono);
  font-size: 11px;
  color: var(--tp-muted);
}
</style>
