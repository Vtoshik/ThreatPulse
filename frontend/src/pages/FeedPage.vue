<template>
  <div class="feed-layout">
    <!-- Left: list -->
    <div class="feed-list">
      <!-- Filter bar -->
      <div class="feed-filters">
        <div class="filter-tabs">
          <button
            v-for="f in FILTERS"
            :key="f"
            class="filter-tab"
            :class="{ 'filter-tab--active': activeFilter === f }"
            @click="activeFilter = f"
          >
            {{ f === 'ALL' ? `All (${THREATS.length})` : f }}
          </button>
        </div>
      </div>

      <!-- Table header -->
      <div class="feed-table-header">
        <MonoLabel v-for="h in TABLE_HEADERS" :key="h">{{ h }}</MonoLabel>
      </div>

      <!-- Rows -->
      <div class="feed-rows">
        <EmptyState
          v-if="filtered.length === 0"
          icon="◌"
          title="No threats match this filter"
          desc="Try selecting a different severity level or checking back later."
        />
        <ThreatRow
          v-for="t in filtered"
          :key="t.id"
          :threat="t"
          :selected="selected?.id === t.id"
          @click="selected = t"
        />
      </div>
    </div>

    <!-- Right: detail panel -->
    <aside v-if="selected" class="feed-detail">
      <div class="feed-detail__header">
        <MonoLabel>Selected Threat</MonoLabel>
      </div>
      <div class="feed-detail__body">
        <SeverityBadge :severity="selected.severity" />
        <div class="feed-detail__title">{{ selected.title }}</div>
        <div class="feed-detail__cve">{{ selected.cve }}</div>

        <!-- Score -->
        <div class="feed-detail__score-row">
          <MonoLabel>CVSS</MonoLabel>
          <span class="feed-detail__score-value" :style="{ color: sevColor(selected.severity) }">
            {{ selected.score }}
          </span>
        </div>
        <div class="feed-detail__score-bar">
          <div
            class="feed-detail__score-fill"
            :style="{ width: `${(selected.score / 10) * 100}%`, background: sevColor(selected.severity) }"
          />
        </div>

        <p class="feed-detail__summary">{{ selected.summary?.slice(0, 160) }}...</p>

        <div v-if="selected.patch" class="feed-detail__patch">
          <MonoLabel style="display:block; margin-bottom:4px">Patch</MonoLabel>
          <div class="feed-detail__patch-text">{{ selected.patch.split('.')[0] }}.</div>
        </div>

        <div class="feed-detail__meta">
          <div
            v-for="[k, v] in metaRows"
            :key="k"
            class="feed-detail__meta-row"
          >
            <MonoLabel>{{ k }}</MonoLabel>
            <span class="feed-detail__meta-val">{{ v }}</span>
          </div>
        </div>

        <AppButton
          variant="ghost"
          size="sm"
          style="width:100%"
          @click="openDetail"
        >
          Full details →
        </AppButton>
      </div>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import type { Threat } from 'src/types/threat'
import { THREATS } from 'src/data/mockData'
import { useSeverity } from 'src/composables/useSeverity'
import MonoLabel from 'src/components/MonoLabel.vue'
import ThreatRow from 'src/components/ThreatRow.vue'
import SeverityBadge from 'src/components/SeverityBadge.vue'
import EmptyState from 'src/components/EmptyState.vue'
import AppButton from 'src/components/AppButton.vue'

const router = useRouter()
const { sevColor } = useSeverity()

const FILTERS = ['ALL', 'CRITICAL', 'HIGH', 'MEDIUM', 'LOW'] as const
const TABLE_HEADERS = ['Vulnerability', 'Severity', 'Technology', 'CVSS Score', 'Age']

const activeFilter = ref<string>('ALL')
const selected = ref<Threat | null>(THREATS[0] ?? null)

const filtered = computed(() =>
  activeFilter.value === 'ALL'
    ? THREATS
    : THREATS.filter(t => t.severity === activeFilter.value)
)

const metaRows = computed(() => selected.value
  ? [
      ['Vector',     selected.value.vector     ?? '—'],
      ['Complexity', selected.value.complexity  ?? '—'],
      ['Auth',       selected.value.auth        ?? '—'],
    ]
  : []
)

function openDetail() {
  if (selected.value) void router.push(`/app/threats/${selected.value.id}`)
}
</script>

<style scoped>
.feed-layout {
  display: flex;
  height: 100%;
  overflow: hidden;
}

.feed-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.feed-filters {
  padding: 10px 28px;
  box-shadow: inset 0 -1px 0 var(--tp-border);
  display: flex;
  align-items: center;
}

.filter-tabs {
  display: flex;
  background: var(--tp-surface);
  border-radius: 6px;
  box-shadow: var(--tp-sb);
  overflow: hidden;
}

.filter-tab {
  padding: 5px 14px;
  background: transparent;
  color: var(--tp-muted);
  border: none;
  box-shadow: inset -1px 0 0 var(--tp-border);
  cursor: pointer;
  font-family: var(--tp-font);
  font-size: 11px;
  font-weight: 400;
  transition: background 0.1s, color 0.1s;
}

.filter-tab:last-child {
  box-shadow: none;
}

.filter-tab--active {
  background: var(--tp-surf3);
  color: var(--tp-text);
  font-weight: 500;
}

.feed-table-header {
  padding: 9px 20px;
  box-shadow: inset 0 -1px 0 var(--tp-border);
  display: grid;
  grid-template-columns: 1fr 92px 110px 120px 44px;
  gap: 12px;
  align-items: center;
}

.feed-rows {
  flex: 1;
  overflow: auto;
}

/* Right panel */
.feed-detail {
  width: 280px;
  background: var(--tp-surface);
  box-shadow: inset 1px 0 0 var(--tp-border);
  display: flex;
  flex-direction: column;
  overflow: auto;
  flex-shrink: 0;
}

.feed-detail__header {
  padding: 14px 16px;
  box-shadow: inset 0 -1px 0 var(--tp-border);
}

.feed-detail__body {
  padding: 16px;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0;
}

.feed-detail__title {
  font-size: 13px;
  font-weight: 600;
  margin-top: 10px;
  margin-bottom: 4px;
  letter-spacing: -0.3px;
  line-height: 1.35;
}

.feed-detail__cve {
  font-family: var(--tp-mono);
  font-size: 10px;
  color: var(--tp-muted);
  margin-bottom: 16px;
  letter-spacing: 0.3px;
}

.feed-detail__score-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 6px;
}

.feed-detail__score-value {
  font-size: 26px;
  font-weight: 600;
  letter-spacing: -1.2px;
}

.feed-detail__score-bar {
  height: 2px;
  background: var(--tp-surf3);
  border-radius: 1px;
  margin-bottom: 16px;
}

.feed-detail__score-fill {
  height: 100%;
  border-radius: 1px;
}

.feed-detail__summary {
  font-size: 12px;
  color: var(--tp-sec);
  line-height: 1.65;
  margin-bottom: 14px;
}

.feed-detail__patch {
  padding: 10px 12px;
  background: var(--tp-surf2);
  border-radius: 6px;
  box-shadow: var(--tp-sb);
  margin-bottom: 14px;
}

.feed-detail__patch-text {
  font-family: var(--tp-mono);
  font-size: 11px;
  color: var(--tp-text);
  line-height: 1.5;
}

.feed-detail__meta {
  margin-bottom: 14px;
}

.feed-detail__meta-row {
  display: flex;
  justify-content: space-between;
  padding: 5px 0;
  box-shadow: inset 0 -1px 0 rgba(255, 255, 255, 0.06);
}

.feed-detail__meta-val {
  font-size: 11px;
  color: var(--tp-sec);
  font-family: var(--tp-mono);
}
</style>
