<template>
  <div class="page-pad tp-fade-in">
    <PageTitle title="Semantic" sub="search" />

    <!-- Search bar -->
    <div class="search-bar">
      <AppInput
        v-model="query"
        placeholder='e.g. "kafka authentication" or "RCE spring"'
        style="font-size:14px; padding:10px 14px"
        @keydown.enter="doSearch"
      />
      <AppButton variant="primary" size="md" @click="doSearch">Search</AppButton>
    </div>
    <div class="search-hint">Powered by pgvector semantic search · HuggingFace embeddings</div>

    <!-- Recent searches (before first search) -->
    <template v-if="!searched">
      <MonoLabel style="display:block; margin-bottom:12px">Recent searches</MonoLabel>
      <div
        v-for="q in RECENT_QUERIES"
        :key="q"
        class="recent-item"
        @click="query = q"
        @mouseenter="(e) => ((e.currentTarget as HTMLElement).style.background = 'var(--tp-surf2)')"
        @mouseleave="(e) => ((e.currentTarget as HTMLElement).style.background = 'transparent')"
      >
        <span style="font-size:11px; opacity:0.5">⟳</span>
        {{ q }}
      </div>
    </template>

    <!-- No results -->
    <EmptyState
      v-else-if="results.length === 0"
      icon="⌕"
      title="No results found"
      :desc="`No threats matched &quot;${query}&quot;. Try different keywords or check spelling.`"
    />

    <!-- Results -->
    <template v-else>
      <div class="results-header">
        <MonoLabel>{{ results.length }} result{{ results.length !== 1 ? 's' : '' }} for "{{ query }}"</MonoLabel>
      </div>
      <AppCard
        v-for="t in results"
        :key="t.id"
        style="padding:14px 18px; margin-bottom:8px"
        :on-click="() => openDetail(t.id)"
      >
        <div class="result-row">
          <div class="result-left">
            <div class="result-meta">
              <SeverityBadge :severity="t.severity" />
              <CveHash :cve="t.cve" :id="t.id" />
            </div>
            <div class="result-title">{{ t.title }}</div>
            <div class="result-summary">{{ t.summary?.slice(0, 120) }}...</div>
          </div>
          <div class="result-score">
            <div class="result-score-value" :style="{ color: sevColor(t.severity) }">{{ t.score }}</div>
            <div class="result-score-label">CVSS</div>
          </div>
        </div>
      </AppCard>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import type { Threat } from 'src/types/threat'
import { THREATS } from 'src/data/mockData'
import { useSeverity } from 'src/composables/useSeverity'
import PageTitle from 'src/components/PageTitle.vue'
import AppInput from 'src/components/AppInput.vue'
import AppButton from 'src/components/AppButton.vue'
import AppCard from 'src/components/AppCard.vue'
import MonoLabel from 'src/components/MonoLabel.vue'
import SeverityBadge from 'src/components/SeverityBadge.vue'
import CveHash from 'src/components/CveHash.vue'
import EmptyState from 'src/components/EmptyState.vue'

const router = useRouter()
const { sevColor } = useSeverity()

const RECENT_QUERIES = ['spring boot deserialization', 'kafka auth bypass', 'docker escape 2025']

const query   = ref('')
const results = ref<Threat[]>([])
const searched = ref(false)

function doSearch() {
  searched.value = true
  const q = query.value.toLowerCase()
  results.value = THREATS.filter(t =>
    t.title.toLowerCase().includes(q) ||
    t.cve.toLowerCase().includes(q) ||
    t.tech.toLowerCase().includes(q) ||
    (t.summary ?? '').toLowerCase().includes(q)
  )
}

function openDetail(id: string) {
  void router.push(`/app/threats/${id}`)
}
</script>

<style scoped>
.page-pad {
  padding: 24px 28px;
}

.search-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  max-width: 600px;
}

.search-hint {
  font-size: 11px;
  color: var(--tp-muted);
  margin-bottom: 28px;
  font-family: var(--tp-mono);
}

.recent-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 6px;
  cursor: pointer;
  margin-bottom: 4px;
  font-size: 13px;
  color: var(--tp-muted);
  font-family: var(--tp-mono);
  transition: background 0.1s;
}

.results-header {
  margin-bottom: 14px;
}

.result-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.result-left {
  flex: 1;
}

.result-meta {
  display: flex;
  gap: 8px;
  margin-bottom: 6px;
  align-items: center;
}

.result-title {
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 6px;
  letter-spacing: -0.2px;
}

.result-summary {
  font-size: 12px;
  color: var(--tp-muted);
  line-height: 1.6;
}

.result-score {
  text-align: right;
  flex-shrink: 0;
}

.result-score-value {
  font-size: 20px;
  font-weight: 600;
  letter-spacing: -1px;
}

.result-score-label {
  font-size: 10px;
  color: var(--tp-muted);
  font-family: var(--tp-mono);
}
</style>
