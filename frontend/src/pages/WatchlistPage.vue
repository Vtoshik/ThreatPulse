<template>
  <div class="page-pad tp-fade-in">
    <PageTitle title="Watchlist" :sub="`${list.length} saved threats`" />

    <EmptyState
      v-if="list.length === 0"
      icon="◎"
      title="Watchlist is empty"
      desc="Save threats from the feed to track them here."
    >
      <AppButton variant="ghost" size="sm" @click="$router.push('/app/feed')">
        Browse feed
      </AppButton>
    </EmptyState>

    <div v-else class="watchlist-grid">
      <AppCard
        v-for="t in list"
        :key="t.id"
        style="padding:18px"
        :on-click="() => openDetail(t.id)"
      >
        <div class="card-top">
          <SeverityBadge :severity="t.severity" />
          <span class="card-remove" @click.stop="remove(t.id)">✕</span>
        </div>
        <div class="card-title">{{ t.title }}</div>
        <CveHash :cve="t.cve" :id="t.id" />
        <div style="margin-top:12px">
          <ScoreBar :score="t.score" :severity="t.severity" />
        </div>
        <div class="card-footer">
          <span class="card-mono">{{ t.tech }}</span>
          <span class="card-mono">{{ t.published }}</span>
        </div>
      </AppCard>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { THREATS } from 'src/data/mockData'
import PageTitle from 'src/components/PageTitle.vue'
import AppCard from 'src/components/AppCard.vue'
import AppButton from 'src/components/AppButton.vue'
import SeverityBadge from 'src/components/SeverityBadge.vue'
import CveHash from 'src/components/CveHash.vue'
import ScoreBar from 'src/components/ScoreBar.vue'
import EmptyState from 'src/components/EmptyState.vue'

const router = useRouter()
const list = ref(THREATS.filter((_, i) => [0, 2, 4].includes(i)))

function remove(id: string) {
  list.value = list.value.filter(t => t.id !== id)
}

function openDetail(id: string) {
  void router.push(`/app/threats/${id}`)
}
</script>

<style scoped>
.page-pad { padding: 24px 28px; }

.watchlist-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.card-top {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
  align-items: center;
  justify-content: space-between;
}

.card-remove {
  font-size: 11px;
  color: var(--tp-muted);
  cursor: pointer;
  opacity: 0.6;
  padding: 2px 6px;
}

.card-remove:hover { opacity: 1; }

.card-title {
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 6px;
  letter-spacing: -0.2px;
  line-height: 1.35;
}

.card-footer {
  margin-top: 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-mono {
  font-family: var(--tp-mono);
  font-size: 10px;
  color: var(--tp-muted);
}
</style>
