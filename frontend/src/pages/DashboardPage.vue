<template>
  <div class="page-pad tp-fade-in">
    <PageTitle title="Dashboard" :sub="todaySub" />

    <!-- Stat cards -->
    <div class="stat-grid">
      <AppCard v-for="s in STATS" :key="s.label" style="padding: 18px 20px">
        <MonoLabel style="display:block; margin-bottom:10px">{{ s.label }}</MonoLabel>
        <div class="stat-value" :style="{ color: sevColor(s.severity) }">{{ s.value }}</div>
      </AppCard>
    </div>

    <!-- Recent threats header -->
    <div class="section-header">
      <MonoLabel>Recent threats</MonoLabel>
      <div class="section-header__line" />
      <router-link to="/app/feed" class="section-header__link">View all</router-link>
    </div>

    <!-- Threats table -->
    <AppCard style="overflow:hidden">
      <div class="table-header">
        <MonoLabel v-for="h in TABLE_HEADERS" :key="h">{{ h }}</MonoLabel>
      </div>
      <ThreatRow
        v-for="(threat, i) in recentThreats"
        :key="threat.id"
        :threat="threat"
        :first="i === 0"
        @click="openDetail(threat.id)"
      />
    </AppCard>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useSeverity } from 'src/composables/useSeverity'
import { THREATS } from 'src/data/mockData'
import PageTitle from 'src/components/PageTitle.vue'
import AppCard from 'src/components/AppCard.vue'
import MonoLabel from 'src/components/MonoLabel.vue'
import ThreatRow from 'src/components/ThreatRow.vue'

const router = useRouter()
const { sevColor } = useSeverity()

const STATS = [
  { label: 'Critical',    value: 3,  severity: 'CRITICAL' },
  { label: 'High',        value: 12, severity: 'HIGH'     },
  { label: 'Medium',      value: 28, severity: 'MEDIUM'   },
  { label: 'Watching',    value: 5,  severity: 'LOW'      },
]

const TABLE_HEADERS = ['Vulnerability', 'Severity', 'Technology', 'CVSS', 'Age']

const todaySub = computed(() => {
  const d = new Date().toLocaleDateString('en-US', { weekday: 'long', month: 'long', day: 'numeric' })
  return `${d} · 47 threats today`
})

const recentThreats = computed(() => THREATS.slice(0, 3))

function openDetail(id: string) {
  void router.push(`/app/threats/${id}`)
}
</script>

<style scoped>
.page-pad {
  padding: 24px 28px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 28px;
}

.stat-value {
  font-size: 32px;
  font-weight: 600;
  letter-spacing: -1.5px;
  line-height: 1;
}

.section-header {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.section-header__line {
  flex: 1;
  height: 1px;
  background: var(--tp-surf3);
}

.section-header__link {
  font-size: 11px;
  color: var(--tp-muted);
  cursor: pointer;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.section-header__link:hover {
  color: var(--tp-sec);
}

.table-header {
  padding: 9px 20px;
  box-shadow: inset 0 -1px 0 var(--tp-border);
  display: grid;
  grid-template-columns: 1fr 92px 110px 120px 44px;
  gap: 12px;
  align-items: center;
}
</style>
