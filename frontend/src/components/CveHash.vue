<template>
  <div class="cve-hash">
    <span v-if="id" class="cve-hash__id">{{ id }}</span>
    <span class="cve-hash__cve">{{ cve }}</span>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  cve: string
  id?: string
}>()
</script>

<style scoped>
.cve-hash {
  display: flex;
  align-items: center;
  gap: 6px;
}

.cve-hash__id {
  font-family: var(--tp-mono);
  font-size: 10px;
  color: var(--tp-sec);
  letter-spacing: 0.3px;
}

.cve-hash__cve {
  font-family: var(--tp-mono);
  font-size: 10px;
  color: var(--tp-muted);
  letter-spacing: 0.3px;
}
</style>
