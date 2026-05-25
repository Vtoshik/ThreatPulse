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

    <span class="threat-row__severity"><SeverityBadge :severity="threat.severity" /></span>

    <span class="threat-row__mono threat-row__tech">{{ threat.tech }}</span>

    <ScoreBar class="threat-row__scorebar" :score="threat.score" :severity="threat.severity" />

    <span class="threat-row__mono threat-row__age">{{ threat.age }}</span>

    <button
      class="threat-row__bookmark"
      :class="{ 'threat-row__bookmark--active': isBookmarked }"
      @click.stop="$emit('bookmark', threat)"
      :title="isBookmarked ? 'Remove from watchlist' : 'Add to watchlist'"
    >{{ isBookmarked ? '★' : '☆' }}</button>
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
  isBookmarked?: boolean
}>(), {
  selected: false,
  first: false,
  isBookmarked: false,
})

defineEmits<{ click: []; bookmark: [threat: Threat] }>()

const hovered = ref(false)
const { sevColor } = useSeverity()

const selectedStyle = computed(() => {
  if (props.selected) {
    const c = sevColor(props.threat.severity)
    return { background: `${c}1a` }
  }
  return { background: hovered.value ? 'var(--tp-surf2)' : 'transparent' }
})
</script>

<style scoped>
.threat-row {
  padding: 12px 20px;
  display: grid;
  grid-template-columns: 1fr 92px 110px 120px 44px 28px;
  gap: 12px;
  align-items: center;
  box-shadow: inset 0 -1px 0 var(--tp-border);
  cursor: pointer;
  transition: background 0.1s;
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

.threat-row__bookmark {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 13px;
  color: var(--tp-dimmer);
  padding: 0;
  line-height: 1;
  opacity: 0;
  transition: opacity 0.1s, color 0.1s;
}

.threat-row:hover .threat-row__bookmark,
.threat-row__bookmark--active {
  opacity: 1 !important;
}

.threat-row__bookmark--active {
  color: var(--tp-sec);
}

.threat-row__bookmark:hover {
  color: var(--tp-text);
}

@media (max-width: 600px) {
  .threat-row {
    display: grid;
    grid-template-columns: auto 1fr auto auto;
    grid-template-rows: auto auto;
    gap: 4px 8px;
    padding: 10px 14px;
  }

  .threat-row__severity  { grid-area: 1 / 1; align-self: start; padding-top: 1px; }
  .threat-row__title-col { grid-area: 1 / 2; }
  .threat-row__age       { grid-area: 1 / 3; align-self: start; font-size: 10px; }
  .threat-row__bookmark  { grid-area: 1 / 4; align-self: start; opacity: 1; }
  .threat-row__tech      { grid-area: 2 / 2 / 3 / 5; color: var(--tp-dimmer); font-size: 10px; }
  .threat-row__scorebar  { display: none; }
}
</style>
