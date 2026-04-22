<template>
  <div class="page-pad tp-fade-in">
    <!-- Back -->
    <div class="detail-back">
      <span class="detail-back__link" @click="$router.push('/app/feed')">← Back to feed</span>
      <span class="detail-back__dot">·</span>
      <span class="detail-back__cve">{{ threat.cve }}</span>
    </div>

    <!-- Title row -->
    <div class="detail-title-row">
      <div class="detail-title-left">
        <div class="detail-title-meta">
          <SeverityBadge :severity="threat.severity" />
          <MonoLabel>{{ threat.source }}</MonoLabel>
        </div>
        <h1 class="detail-h1">{{ threat.title }}</h1>
        <CveHash :cve="threat.cve" :id="threat.id" />
      </div>

      <div class="detail-score-card" :style="{ boxShadow: `${sevColor(threat.severity)}40 0 0 0 1px, rgba(0,0,0,.4) 0 4px 16px` }">
        <MonoLabel style="display:block; margin-bottom:4px">CVSS</MonoLabel>
        <div class="detail-score-value" :style="{ color: sevColor(threat.severity) }">{{ threat.score }}</div>
        <div class="detail-score-sub">/ 10.0</div>
      </div>
    </div>

    <!-- Two-column grid -->
    <div class="detail-grid">
      <!-- Left column -->
      <div class="detail-left">
        <!-- AI Summary -->
        <AppCard style="padding:20px">
          <div class="detail-section-header">
            <MonoLabel>AI Summary</MonoLabel>
            <span class="detail-model-tag">Llama 3.3 70B</span>
          </div>
          <p class="detail-summary">{{ threat.summary }}</p>
        </AppCard>

        <!-- Affected versions -->
        <AppCard style="padding:20px">
          <MonoLabel style="display:block; margin-bottom:14px">Affected Versions</MonoLabel>
          <div
            v-for="v in (threat.affected ?? [])"
            :key="v"
            class="affected-row"
          >
            <span class="affected-dot" />
            <span class="affected-label">{{ v }}</span>
          </div>
        </AppCard>

        <!-- Patch -->
        <AppCard style="padding:20px">
          <MonoLabel style="display:block; margin-bottom:14px">Recommended Fix</MonoLabel>
          <div class="patch-box">
            <p class="patch-text">{{ threat.patch }}</p>
          </div>
        </AppCard>
      </div>

      <!-- Right column -->
      <div class="detail-right">
        <!-- Metadata -->
        <AppCard style="padding:16px">
          <MonoLabel style="display:block; margin-bottom:12px">Details</MonoLabel>
          <div
            v-for="[k, v] in metaRows"
            :key="k"
            class="meta-row"
          >
            <MonoLabel>{{ k }}</MonoLabel>
            <span class="meta-val">{{ v }}</span>
          </div>
        </AppCard>

        <!-- Timeline -->
        <AppCard style="padding:16px">
          <MonoLabel style="display:block; margin-bottom:12px">Timeline</MonoLabel>
          <div
            v-for="(e, i) in (threat.timeline ?? [])"
            :key="i"
            class="timeline-item"
          >
            <div class="timeline-track">
              <div
                class="timeline-dot"
                :style="{ background: i === 0 ? sevColor(threat.severity) : 'var(--tp-dimmer)' }"
              />
              <div v-if="i < (threat.timeline?.length ?? 0) - 1" class="timeline-line" />
            </div>
            <div class="timeline-content">
              <div class="timeline-time">{{ e.t }}</div>
              <div class="timeline-event">{{ e.e }}</div>
            </div>
          </div>
        </AppCard>

        <AppButton variant="ghost" size="sm" style="width:100%">
          ↗ View on NVD
        </AppButton>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { THREATS } from 'src/data/mockData'
import { useSeverity } from 'src/composables/useSeverity'
import SeverityBadge from 'src/components/SeverityBadge.vue'
import MonoLabel from 'src/components/MonoLabel.vue'
import CveHash from 'src/components/CveHash.vue'
import AppCard from 'src/components/AppCard.vue'
import AppButton from 'src/components/AppButton.vue'

const route = useRoute()
const { sevColor } = useSeverity()

const threat = computed(() =>
  THREATS.find(t => t.id === route.params['id']) ?? THREATS[0]!
)

const metaRows = computed(() => [
  ['Technology',    threat.value.tech],
  ['Attack Vector', threat.value.vector    ?? '—'],
  ['Complexity',    threat.value.complexity ?? '—'],
  ['Auth required', threat.value.auth       ?? '—'],
  ['Published',     threat.value.published],
])
</script>

<style scoped>
.page-pad {
  padding: 24px 28px;
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

.affected-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 0;
  box-shadow: inset 0 -1px 0 rgba(255, 255, 255, 0.06);
}

.affected-dot {
  width: 4px;
  height: 4px;
  background: var(--tp-crit);
  border-radius: 1px;
  flex-shrink: 0;
  transform: rotate(45deg);
}

.affected-label {
  font-family: var(--tp-mono);
  font-size: 12px;
  color: var(--tp-text);
}

.patch-box {
  padding: 12px 14px;
  background: var(--tp-surf2);
  border-radius: 6px;
  box-shadow: var(--tp-sb);
}

.patch-text {
  font-size: 12px;
  color: var(--tp-sec);
  line-height: 1.7;
  margin: 0;
  font-family: var(--tp-mono);
}

.meta-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 7px 0;
  box-shadow: inset 0 -1px 0 rgba(255, 255, 255, 0.06);
}

.meta-val {
  font-size: 11px;
  color: var(--tp-sec);
  font-family: var(--tp-mono);
  text-align: right;
  max-width: 120px;
}

.timeline-item {
  display: flex;
  gap: 10px;
  margin-bottom: 10px;
  align-items: flex-start;
}

.timeline-track {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.timeline-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-top: 4px;
  flex-shrink: 0;
}

.timeline-line {
  width: 1px;
  height: 16px;
  background: var(--tp-surf3);
}

.timeline-time {
  font-family: var(--tp-mono);
  font-size: 10px;
  color: var(--tp-muted);
  margin-bottom: 1px;
}

.timeline-event {
  font-size: 11px;
  color: var(--tp-sec);
}
</style>
