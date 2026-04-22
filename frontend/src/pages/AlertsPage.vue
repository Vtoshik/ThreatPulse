<template>
  <div class="page-pad tp-fade-in">
    <div class="alerts-header">
      <PageTitle
        title="Alerts"
        :sub="`${activeCount} active rules · ${ALERT_HISTORY.length} sent today`"
      />
      <AppButton variant="primary" size="sm" @click="$router.push('/app/alerts/new')">
        + New rule
      </AppButton>
    </div>

    <!-- Tabs -->
    <div class="tabs">
      <button
        v-for="(t, i) in TABS"
        :key="t.key"
        class="tab"
        :class="{ 'tab--active': activeTab === t.key, 'tab--bordered': i === 0 }"
        @click="activeTab = t.key"
      >
        {{ t.label }}
      </button>
    </div>

    <!-- Rules tab -->
    <template v-if="activeTab === 'rules'">
      <EmptyState
        v-if="rules.length === 0"
        icon="⚐"
        title="No alert rules yet"
        desc="Create a rule to get notified when new CVEs match your tech stack."
      >
        <AppButton variant="ghost" size="sm" @click="$router.push('/app/alerts/new')">
          Create first rule
        </AppButton>
      </EmptyState>

      <div v-else class="rules-list">
        <AppCard v-for="r in rules" :key="r.id" style="padding:14px 18px">
          <div class="rule-row">
            <ToggleSwitch :model-value="r.active" @update:model-value="toggleRule(r.id)" />

            <div class="rule-info">
              <div class="rule-name" :class="{ 'rule-name--muted': !r.active }">{{ r.name }}</div>
              <div class="rule-tags">
                <SeverityBadge :severity="r.severity" />
                <template v-if="r.tech.length > 0">
                  <AppTag
                    v-for="t in r.tech"
                    :key="t"
                    :label="t"
                    color="var(--tp-muted)"
                  />
                </template>
                <AppTag v-else label="All technologies" color="var(--tp-muted)" />
              </div>
            </div>

            <div class="rule-stat">
              <div
                class="rule-count"
                :style="{ color: r.triggered > 0 ? sevColor(r.severity) : 'var(--tp-dimmer)' }"
              >{{ r.triggered }}</div>
              <MonoLabel>triggered</MonoLabel>
            </div>

            <AppButton
              variant="ghost"
              size="sm"
              @click="$router.push(`/app/alerts/${r.id}/edit`)"
            >
              Edit
            </AppButton>
          </div>
        </AppCard>
      </div>
    </template>

    <!-- History tab -->
    <template v-else>
      <AppCard style="overflow:hidden">
        <div class="history-header">
          <MonoLabel v-for="h in HISTORY_HEADERS" :key="h">{{ h }}</MonoLabel>
        </div>
        <div
          v-for="a in ALERT_HISTORY"
          :key="a.id"
          class="history-row"
        >
          <div>
            <div class="history-threat">{{ a.threat }}</div>
            <span class="history-cve">{{ a.cve }}</span>
          </div>
          <SeverityBadge :severity="a.severity" />
          <span class="history-mono">{{ a.channel.toLowerCase() }}</span>
          <span class="history-mono">{{ a.sentAt }}</span>
        </div>
      </AppCard>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { AlertRule } from 'src/types/alert'
import { ALERT_RULES, ALERT_HISTORY } from 'src/data/mockData'
import { useSeverity } from 'src/composables/useSeverity'
import PageTitle from 'src/components/PageTitle.vue'
import AppButton from 'src/components/AppButton.vue'
import AppCard from 'src/components/AppCard.vue'
import MonoLabel from 'src/components/MonoLabel.vue'
import SeverityBadge from 'src/components/SeverityBadge.vue'
import AppTag from 'src/components/AppTag.vue'
import ToggleSwitch from 'src/components/ToggleSwitch.vue'
import EmptyState from 'src/components/EmptyState.vue'

const { sevColor } = useSeverity()

const TABS = [
  { key: 'rules',   label: 'Alert Rules'  },
  { key: 'history', label: 'Send History' },
]
const HISTORY_HEADERS = ['Threat', 'Severity', 'Channel', 'Sent at']

const activeTab  = ref('rules')
const rules      = ref<AlertRule[]>([...ALERT_RULES])
const activeCount = computed(() => rules.value.filter(r => r.active).length)

function toggleRule(id: number) {
  const r = rules.value.find(r => r.id === id)
  if (r) r.active = !r.active
}
</script>

<style scoped>
.page-pad { padding: 24px 28px; }

.alerts-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.tabs {
  display: flex;
  gap: 0;
  margin-bottom: 20px;
  background: var(--tp-surface);
  border-radius: 6px;
  box-shadow: var(--tp-sb);
  overflow: hidden;
  width: fit-content;
}

.tab {
  padding: 6px 18px;
  background: transparent;
  color: var(--tp-muted);
  border: none;
  cursor: pointer;
  font-family: var(--tp-font);
  font-size: 12px;
  font-weight: 400;
  transition: background 0.1s, color 0.1s;
}

.tab--bordered {
  box-shadow: inset -1px 0 0 var(--tp-border);
}

.tab--active {
  background: var(--tp-surf3);
  color: var(--tp-text);
  font-weight: 500;
}

.rules-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.rule-row {
  display: flex;
  align-items: center;
  gap: 14px;
}

.rule-info {
  flex: 1;
}

.rule-name {
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 4px;
  letter-spacing: -0.2px;
  color: var(--tp-text);
  transition: color 0.15s;
}

.rule-name--muted {
  color: var(--tp-muted);
}

.rule-tags {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.rule-stat {
  text-align: right;
  flex-shrink: 0;
}

.rule-count {
  font-size: 18px;
  font-weight: 600;
  letter-spacing: -0.8px;
}

.history-header {
  padding: 9px 18px;
  box-shadow: inset 0 -1px 0 var(--tp-border);
  display: grid;
  grid-template-columns: 1fr 90px 80px 120px;
  gap: 12px;
  align-items: center;
}

.history-row {
  padding: 11px 18px;
  box-shadow: inset 0 -1px 0 var(--tp-border);
  display: grid;
  grid-template-columns: 1fr 90px 80px 120px;
  gap: 12px;
  align-items: center;
}

.history-threat {
  font-size: 12px;
  margin-bottom: 2px;
}

.history-cve {
  font-family: var(--tp-mono);
  font-size: 10px;
  color: var(--tp-muted);
  letter-spacing: 0.3px;
}

.history-mono {
  font-size: 11px;
  color: var(--tp-muted);
  font-family: var(--tp-mono);
}
</style>
