<template>
  <div class="page-pad tp-fade-in">
    <PageTitle title="Dashboard" :sub="todaySub" />

    <!-- Stat cards -->

    <div v-if="isLoadingStats" class="stat-grid">
      <AppCard v-for="i in 4" :key="i" style="padding: 18px 20px; opacity: 0.5">
        <MonoLabel>Loading...</MonoLabel>
      </AppCard>
    </div>

    <div v-else class="stat-grid">
      <AppCard v-for="s in stats" :key="s.label" style="padding: 18px 20px">
        <MonoLabel style="display:block; margin-bottom:10px">{{ s.label }}</MonoLabel>
        <div class="stat-value" :style="{ color: sevColor(s.severity) }">
          {{ s.value }}
        </div>
      </AppCard>
    </div>

    <!-- Recent threats header -->
    <div class="section-header">
      <MonoLabel>Recent threats</MonoLabel>
      <div class="section-header__line" />
      <router-link to="/app/feed" class="section-header__link">View all</router-link>
    </div>

    <!-- Threats table -->
    <AppCard class="table-card">
      <div v-if="isLoadingThreats" style="padding: 20px; opacity: 0.6">
        <MonoLabel>Loading threats...</MonoLabel>
      </div>
      <template v-else>
        <div v-if="recentThreats.length === 0" style="padding: 20px">
          <MonoLabel>No threats found</MonoLabel>
        </div>

        <template v-else>
          <div class="table-header">
            <MonoLabel v-for="h in TABLE_HEADERS" :key="h">{{ h }}</MonoLabel>
          </div>

          <ThreatRow
            v-for="(threat, i) in recentThreats"
            :key="threat.id"
            :threat="threat"
            :first="i === 0"
            :is-bookmarked="bookmarksStore.isBookmarked(threat.id)"
            @click="openDetail(threat.id)"
            @bookmark="bookmarksStore.toggle($event)"
          />
        </template>
      </template>
    </AppCard>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { threatService } from 'src/services/threats.service'
import { useBookmarksStore } from 'src/stores/bookmarks'
import { useRouter } from 'vue-router'
import { useSeverity } from 'src/composables/useSeverity'
import type { Severity, Threat } from 'src/types/threat'
import PageTitle from 'src/components/PageTitle.vue'
import AppCard from 'src/components/AppCard.vue'
import MonoLabel from 'src/components/MonoLabel.vue'
import ThreatRow from 'src/components/ThreatRow.vue'
import { useWebSocket } from 'src/composables/useWebSocket'

const router = useRouter()
const { sevColor } = useSeverity()
const bookmarksStore = useBookmarksStore()

const recentThreats = ref<Threat[]>([])
const stats = ref<Array<{ label: string; value: number; severity: Severity }>>([])
const isLoadingStats = ref(false)
const isLoadingThreats = ref(false)
const TABLE_HEADERS = ['Vulnerability', 'Severity', 'Technology', 'CVSS', 'Age', '']
const { threats: wsThreats, connect} = useWebSocket()

const todayCount = ref(0)
const todaySub = computed(() => {
  const d = new Date().toLocaleDateString('en-US', {
    weekday: 'long',
    month: 'long',
    day: 'numeric',
  })

  return `${d} · ${todayCount.value} threats today`
})

watch(wsThreats, (newThreats) => {
  const threat = newThreats[0]
  if (threat) {
    recentThreats.value.unshift(threat)
  }
})

onMounted(async () => {
  try {
    isLoadingStats.value = true
    isLoadingThreats.value = true

    const [threatsData, statsData] = await Promise.all([
      threatService.getRecentThreats(),
      threatService.getStats()
    ])

    recentThreats.value = threatsData

    stats.value = [
      { label: 'Critical', value: statsData.critical, severity: 'CRITICAL' },
      { label: 'High', value: statsData.high, severity: 'HIGH' },
      { label: 'Medium', value: statsData.medium, severity: 'MEDIUM' },
      { label: 'Low', value: statsData.low, severity: 'LOW' },
    ]

    todayCount.value = statsData.todayCount ?? 0


  } catch (err) {
    console.error(err)
  } finally {
    isLoadingStats.value = false
    isLoadingThreats.value = false
  }
  connect()
  void bookmarksStore.load()
})

function openDetail(id: string) {
  void router.push(`/app/threats/${id}`)
}
</script>

<style scoped>
.page-pad {
  padding: 24px 28px;
}

@media (max-width: 768px) {
  .page-pad { padding: 16px; }
  .stat-grid { grid-template-columns: repeat(2, 1fr); }
  .table-card { overflow-x: auto; }
  .table-header { display: none; }
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
  grid-template-columns: 1fr 92px 110px 120px 44px 28px;
  gap: 12px;
  align-items: center;
}
</style>
