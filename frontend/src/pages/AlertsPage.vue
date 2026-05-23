<template>
  <div class="page-pad tp-fade-in">
    <div class="alerts-header">
      <PageTitle
        title="Alerts"
        :sub="`${activeCount} active rules · ${history.length} sent today`"
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
            <ToggleSwitch :model-value="r.active" @update:model-value="toggleRule(r)" />

            <div class="rule-info">
              <div class="rule-name" :class="{ 'rule-name--muted': !r.active }">
                {{ r.name || (r.minSeverity.charAt(0) + r.minSeverity.slice(1).toLowerCase() + ' and above') }}
              </div>
              <div class="rule-tags">
                <SeverityBadge :severity="r.minSeverity" />
                <template v-if="r.technologiesFilter.length > 0">
                  <AppTag
                    v-for="t in r.technologiesFilter"
                    :key="t"
                    :label="t"
                    color="var(--tp-muted)"
                  />
                </template>
                <AppTag v-else label="All technologies" color="var(--tp-muted)" />
              </div>
            </div>

            <AppButton
              variant="ghost"
              size="sm"
              @click="$router.push(`/app/alerts/${r.id}/edit`)"
            >
              Edit
            </AppButton>
            <AppButton
              variant="ghost"
              size="sm"
              style="color:var(--tp-dimmer)"
              @click="deleteRule(r.id)"
            >
              Delete
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
        <EmptyState
          v-if="history.length === 0"
          icon="📭"
          title="No alerts sent yet"
          desc="Alerts will appear here once a rule matches a new threat."
          style="padding:24px"
        />
        <div
          v-for="a in history"
          :key="a.id"
          class="history-row"
        >
          <div>
            <div class="history-threat">{{ a.threatTitle }}</div>
          </div>
          <span class="history-mono">{{ a.channel.toLowerCase() }}</span>
          <span class="history-mono">{{ formatDate(a.sentAt) }}</span>
        </div>
      </AppCard>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { alertsService, type AlertRuleDto, type AlertHistoryDto } from 'src/services/alerts.service'
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
void sevColor // used in style bindings elsewhere; suppress unused warning

const TABS = [
  { key: 'rules',   label: 'Alert Rules'  },
  { key: 'history', label: 'Send History' },
]
const HISTORY_HEADERS = ['Threat', 'Channel', 'Sent at']

const activeTab   = ref('rules')
const rules       = ref<AlertRuleDto[]>([])
const history     = ref<AlertHistoryDto[]>([])
const loading     = ref(false)
const activeCount = computed(() => rules.value.filter(r => r.active).length)

function formatDate(iso: string) {
  return new Date(iso).toLocaleString()
}

async function loadRules() {
  loading.value = true
  try {
    rules.value = await alertsService.getRules()
  } finally {
    loading.value = false
  }
}

async function loadHistory() {
  history.value = await alertsService.getHistory()
}

async function toggleRule(rule: AlertRuleDto) {
  const updated = await alertsService.toggleRule(rule)
  const idx = rules.value.findIndex(r => r.id === rule.id)
  if (idx !== -1) rules.value[idx] = updated
}

async function deleteRule(id: number) {
  await alertsService.deleteRule(id)
  rules.value = rules.value.filter(r => r.id !== id)
}

onMounted(() => {
  void loadRules()
  void loadHistory()
})
</script>

<style scoped>
.page-pad { padding: 24px 28px; }

@media (max-width: 768px) {
  .page-pad { padding: 16px; }
  .alerts-header { flex-direction: column; gap: 12px; align-items: flex-start; }
  .history-header { grid-template-columns: 1fr 80px; }
  .history-row { grid-template-columns: 1fr 80px; }
  .history-row > :nth-child(3) { display: none; }
}

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

.history-header {
  padding: 9px 18px;
  box-shadow: inset 0 -1px 0 var(--tp-border);
  display: grid;
  grid-template-columns: 1fr 80px 140px;
  gap: 12px;
  align-items: center;
}

.history-row {
  padding: 11px 18px;
  box-shadow: inset 0 -1px 0 var(--tp-border);
  display: grid;
  grid-template-columns: 1fr 80px 140px;
  gap: 12px;
  align-items: center;
}

.history-threat {
  font-size: 12px;
  margin-bottom: 2px;
}

.history-mono {
  font-size: 11px;
  color: var(--tp-muted);
  font-family: var(--tp-mono);
}
</style>
