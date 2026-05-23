<template>
  <div class="feed-layout tp-fade-in">
    <div class="feed-list">
      <div class="feed-filters">
        <div class="filter-tabs">
          <button
            v-for="f in FILTERS"
            :key="f"
            class="filter-tab"
            :class="{ 'filter-tab--active': activeFilter === f }"
            @click="activeFilter = f"
          >
            {{ f === 'ALL' ? `All (${filteredCount})` : `${f} (${severityCounts[f]})` }}
          </button>
        </div>

      </div>

      <div class="feed-table-header">
        <MonoLabel v-for="h in TABLE_HEADERS" :key="h">{{ h }}</MonoLabel>
      </div>

      <div class="feed-rows">
        <div v-if="isLoading" class="feed-loading">
          <MonoLabel>Loading threats from backend...</MonoLabel>
        </div>

        <EmptyState
          v-else-if="loadError"
          icon="!"
          title="Could not load threats"
          :desc="loadError"
        />

        <EmptyState
          v-else-if="filteredThreats.length === 0"
          icon="◌"
          title="No threats match this filter"
          desc="Try selecting a different severity level or check back later."
        />

        <ThreatRow
          v-for="(t, index) in filteredThreats"
          v-else
          :key="t.id"
          :threat="t"
          :selected="selected?.id === t.id"
          :first="index === 0"
          :is-bookmarked="bookmarksStore.isBookmarked(t.id)"
          @click="selected = t"
          @bookmark="bookmarksStore.toggle($event)"
        />
      </div>
    </div>

    <aside v-if="selected" class="feed-detail">
      <div class="feed-detail__header">
        <MonoLabel>Selected Threat</MonoLabel>
      </div>

      <div class="feed-detail__body">
        <SeverityBadge :severity="selected.severity" />
        <div class="feed-detail__title">{{ selected.title }}</div>
        <div class="feed-detail__cve">{{ selected.cve }}</div>

        <p class="feed-detail__summary">
          {{ selected.summary || 'No AI summary is available for this threat yet.' }}
        </p>

        <div class="feed-detail__meta">
          <div v-for="[label, value] in metaRows" :key="label" class="feed-detail__meta-row">
            <MonoLabel>{{ label }}</MonoLabel>
            <span class="feed-detail__meta-val">{{ value }}</span>
          </div>
        </div>

        <div class="feed-detail__actions">
          <AppButton variant="ghost" size="sm" style="flex:1" @click="openDetail">
            Full details
          </AppButton>
          <AppButton
            v-if="selected.sourceUrl"
            variant="ghost"
            size="sm"
            style="flex:1"
            @click="openSource"
          >
            Open source
          </AppButton>
        </div>
      </div>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import type { Severity, Threat } from 'src/types/threat'
import { threatService } from 'src/services/threats.service'
import { useBookmarksStore } from 'src/stores/bookmarks'
import MonoLabel from 'src/components/MonoLabel.vue'
import ThreatRow from 'src/components/ThreatRow.vue'
import SeverityBadge from 'src/components/SeverityBadge.vue'
import EmptyState from 'src/components/EmptyState.vue'
import AppButton from 'src/components/AppButton.vue'

const router = useRouter()
const bookmarksStore = useBookmarksStore()

const FILTERS = ['ALL', 'CRITICAL', 'HIGH', 'MEDIUM', 'LOW'] as const
const TABLE_HEADERS = ['Vulnerability', 'Severity', 'Technology', 'Risk', 'Age', '']

const isLoading = ref(false)
const loadError = ref('')
const selected = ref<Threat | null>(null)
const activeFilter = ref<(typeof FILTERS)[number]>('ALL')
const threats = ref<Threat[]>([])

const filteredThreats = computed(() =>
  activeFilter.value === 'ALL'
    ? threats.value
    : threats.value.filter((threat) => threat.severity === activeFilter.value)
)

const filteredCount = computed(() => filteredThreats.value.length)

const severityCounts = computed<Record<Severity, number>>(() => {
  const counts = {
    CRITICAL: 0,
    HIGH: 0,
    MEDIUM: 0,
    LOW: 0,
    INFO: 0,
  }

  for (const threat of threats.value) {
    if (threat.severity in counts) {
      counts[threat.severity] += 1
    }
  }

  return counts
})

const metaRows = computed(() => {
  if (!selected.value) return []

  return [
    ['Source', selected.value.sourceName || selected.value.source || '—'],
    ['Category', selected.value.category ?? '—'],
    ['Published', selected.value.published],
    ['Technologies', selected.value.affected?.length ? selected.value.affected.join(', ') : '—'],
  ]
})

watch(filteredThreats, (items) => {
  if (items.length === 0) {
    selected.value = null
    return
  }

  const stillVisible = selected.value && items.some((item) => item.id === selected.value?.id)
  if (!stillVisible) {
    selected.value = items[0] ?? null
  }
}, { immediate: true })

onMounted(() => {
  void fetchThreats()
  void bookmarksStore.load()
})

async function fetchThreats() {
  isLoading.value = true
  loadError.value = ''

  try {
    const data = await threatService.getThreats({
      page: 0,
      size: 100,
      sort: 'publishedAt,desc',
    })

    threats.value = data.threats
  } catch (error) {
    console.error(error)
    loadError.value = 'Unable to load threats. Please try again later.'
  } finally {
    isLoading.value = false
  }
}

function openDetail() {
  if (selected.value) {
    void router.push(`/app/threats/${selected.value.id}`)
  }
}

function openSource() {
  if (selected.value?.sourceUrl) {
    window.open(selected.value.sourceUrl, '_blank', 'noopener,noreferrer')
  }
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
  justify-content: space-between;
  gap: 12px;
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
  grid-template-columns: 1fr 92px 110px 120px 44px 28px;
  gap: 12px;
  align-items: center;
}

.feed-rows {
  flex: 1;
  overflow: auto;
}

.feed-loading {
  padding: 20px;
}

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
  margin-bottom: 14px;
  letter-spacing: 0.3px;
}

.feed-detail__summary {
  font-size: 12px;
  color: var(--tp-sec);
  line-height: 1.7;
  margin: 0 0 16px;
}

.feed-detail__meta {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 18px;
}

.feed-detail__meta-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.feed-detail__actions {
  display: flex;
  gap: 6px;
}

.feed-detail__meta-val {
  font-size: 11px;
  color: var(--tp-sec);
  text-align: right;
  line-height: 1.5;
}
</style>
