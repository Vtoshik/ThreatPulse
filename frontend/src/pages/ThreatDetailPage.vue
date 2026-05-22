<template>
  <div class="page-pad tp-fade-in">
    <div v-if="isLoading" class="detail-state">
      <MonoLabel>Loading threat details...</MonoLabel>
    </div>

    <EmptyState
      v-else-if="loadError"
      icon="!"
      title="Could not load threat"
      :desc="loadError"
    />

    <template v-else-if="threat">
      <div class="detail-back">
        <span class="detail-back__link" @click="$router.push('/app/feed')">Back to feed</span>
        <span class="detail-back__dot">·</span>
        <span class="detail-back__cve detail-back__cve--copy" @click="copyCve" :title="`Copy ${threat.cve}`">
          {{ cveCopied ? 'Copied' : threat.cve }}
        </span>
      </div>

      <div class="detail-title-row">
        <div class="detail-title-left">
          <div class="detail-title-meta">
            <SeverityBadge :severity="threat.severity" />
            <MonoLabel>{{ threat.sourceName || threat.source || 'API' }}</MonoLabel>
          </div>
          <h1 class="detail-h1">{{ threat.title }}</h1>
          <CveHash :cve="threat.cve" :id="threat.id" />
        </div>

        <div class="detail-score-card" :style="{ boxShadow: `${sevColor(threat.severity)}40 0 0 0 1px, rgba(0,0,0,.4) 0 4px 16px` }">
          <MonoLabel style="display:block; margin-bottom:4px">Risk</MonoLabel>
          <div class="detail-score-value" :style="{ color: sevColor(threat.severity) }">{{ threat.score }}</div>
          <div class="detail-score-sub">/ 10.0</div>
        </div>
      </div>

      <div class="detail-grid">
        <div class="detail-left">
          <AppCard style="padding:20px">
            <div class="detail-section-header">
              <MonoLabel>AI Summary</MonoLabel>
              <span class="detail-model-tag">Groq · Llama 3.3</span>
            </div>
            <p class="detail-summary">
              {{ threat.summary || 'No AI summary is available for this threat yet.' }}
            </p>
          </AppCard>

          <AppCard style="padding:20px">
            <MonoLabel style="display:block; margin-bottom:14px">Affected Technologies</MonoLabel>
            <div v-if="affectedTechnologies.length" class="affected-list">
              <div v-for="tech in affectedTechnologies" :key="tech" class="affected-row">
                <span class="affected-dot" />
                <span class="affected-label">{{ tech }}</span>
              </div>
            </div>
            <div v-else class="detail-empty-inline">
              <MonoLabel>No affected technologies returned by the API</MonoLabel>
            </div>
          </AppCard>
        </div>

        <div class="detail-right">
          <AppCard style="padding:16px">
            <MonoLabel style="display:block; margin-bottom:12px">Details</MonoLabel>
            <div v-for="[k, v] in metaRows" :key="k" class="meta-row">
              <MonoLabel>{{ k }}</MonoLabel>
              <span class="meta-val">{{ v }}</span>
            </div>
          </AppCard>

          <AppCard style="padding:16px">
            <MonoLabel style="display:block; margin-bottom:12px">Source</MonoLabel>
            <div class="source-box">
              <div class="source-name">{{ threat.sourceName }}</div>
              <div class="source-url">{{ threat.sourceUrl }}</div>
            </div>
          </AppCard>

          <AppButton variant="ghost" size="sm" style="width:100%" @click="openSource">
            Open source
          </AppButton>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import type { Threat } from 'src/types/threat'
import { threatService } from 'src/services/threats.service'
import { useSeverity } from 'src/composables/useSeverity'
import SeverityBadge from 'src/components/SeverityBadge.vue'
import MonoLabel from 'src/components/MonoLabel.vue'
import CveHash from 'src/components/CveHash.vue'
import AppCard from 'src/components/AppCard.vue'
import AppButton from 'src/components/AppButton.vue'
import EmptyState from 'src/components/EmptyState.vue'

const route = useRoute()
const { sevColor } = useSeverity()

const threat = ref<Threat | null>(null)
const isLoading = ref(false)
const loadError = ref('')
const cveCopied = ref(false)

const affectedTechnologies = computed(() => threat.value?.affected ?? [])

const metaRows = computed(() => {
  if (!threat.value) return []

  return [
    ['Source', threat.value.sourceName || threat.value.source || '—'],
    ['Category', threat.value.category ?? '—'],
    ['Published', threat.value.published],
    ['Age', threat.value.age],
    ['Technologies', affectedTechnologies.value.length ? affectedTechnologies.value.join(', ') : '—'],
  ]
})

watch(
  () => route.params.id,
  async (id) => {
    if (!id) return
    await fetchThreat(String(id))
  },
  { immediate: true }
)

async function fetchThreat(id: string) {
  isLoading.value = true
  loadError.value = ''

  try {
    threat.value = await threatService.getThreatById(id)
  } catch (error) {
    console.error(error)
    loadError.value = 'Unable to load this threat. It may have been removed or the service is temporarily unavailable.'
    threat.value = null
  } finally {
    isLoading.value = false
  }
}

function copyCve() {
  if (threat.value?.cve) {
    void navigator.clipboard.writeText(threat.value.cve)
    cveCopied.value = true
    setTimeout(() => { cveCopied.value = false }, 2000)
  }
}

function openSource() {
  if (threat.value?.sourceUrl) {
    window.open(threat.value.sourceUrl, '_blank', 'noopener,noreferrer')
  }
}
</script>

<style scoped>
.page-pad {
  padding: 24px 28px;
}

.detail-state {
  padding: 32px 0;
}

.detail-back {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-back__link {
  font-size: 12px;
  color: var(--tp-muted);
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
}

.detail-back__link:hover {
  color: var(--tp-sec);
}

.detail-back__dot {
  color: var(--tp-dimmer);
}

.detail-back__cve {
  font-family: var(--tp-mono);
  font-size: 11px;
  color: var(--tp-muted);
  letter-spacing: 0.3px;
}

.detail-back__cve--copy {
  cursor: pointer;
  transition: color 0.1s;
}

.detail-back__cve--copy:hover {
  color: var(--tp-sec);
}

.detail-title-row {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 28px;
}

.detail-title-left {
  flex: 1;
}

.detail-title-meta {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 10px;
}

.detail-h1 {
  font-size: 24px;
  font-weight: 600;
  letter-spacing: -1px;
  margin: 0 0 6px;
  line-height: 1.25;
  color: var(--tp-text);
}

.detail-score-card {
  text-align: center;
  padding: 16px 24px;
  background: var(--tp-surface);
  border-radius: 8px;
  flex-shrink: 0;
}

.detail-score-value {
  font-size: 40px;
  font-weight: 600;
  letter-spacing: -2px;
  line-height: 1;
}

.detail-score-sub {
  font-size: 10px;
  color: var(--tp-muted);
  margin-top: 4px;
  font-family: var(--tp-mono);
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 280px;
  gap: 20px;
}

.detail-left,
.detail-right {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
}

.detail-model-tag {
  font-size: 10px;
  color: var(--tp-muted);
  padding: 1px 7px;
  border-radius: 9999px;
  background: var(--tp-surf2);
  box-shadow: var(--tp-sb);
  font-family: var(--tp-mono);
}

.detail-summary {
  font-size: 13px;
  color: var(--tp-sec);
  line-height: 1.75;
  margin: 0;
}

.affected-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.affected-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.affected-dot {
  width: 6px;
  height: 6px;
  border-radius: 9999px;
  background: var(--tp-dimmer);
  flex-shrink: 0;
}

.affected-label {
  font-size: 12px;
  color: var(--tp-sec);
  line-height: 1.5;
}

.detail-empty-inline {
  padding: 4px 0;
}

.meta-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 7px 0;
}

.meta-row + .meta-row {
  box-shadow: inset 0 1px 0 var(--tp-border);
}

.meta-val {
  font-size: 11px;
  color: var(--tp-sec);
  text-align: right;
  line-height: 1.5;
}

.source-box {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.source-name {
  font-size: 12px;
  color: var(--tp-text);
  font-weight: 500;
}

.source-url {
  font-size: 11px;
  color: var(--tp-muted);
  line-height: 1.5;
  word-break: break-word;
}
</style>
