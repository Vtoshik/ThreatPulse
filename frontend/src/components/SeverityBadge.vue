<template>
  <span class="sev-badge" :class="`sev-badge--${severity.toLowerCase()}`">
    {{ LABELS[severity] ?? severity }}
  </span>
</template>

<script setup lang="ts">
import type { Severity } from 'src/types/threat'

defineProps<{ severity: Severity }>()

const LABELS: Record<Severity, string> = {
  CRITICAL: 'Critical',
  HIGH:     'High',
  MEDIUM:   'Medium',
  LOW:      'Low',
  INFO:     'Info',
}
</script>

<style scoped>
.sev-badge {
  padding: 2px 9px;
  border-radius: 9999px;
  font-size: 10px;
  font-weight: 500;
  font-family: var(--tp-font);
  display: inline-block;
  line-height: 1.6;
}

.sev-badge--critical {
  color: var(--tp-crit);
  background: var(--tp-crit-bg);
  box-shadow: var(--tp-crit-ring) 0 0 0 1px;
}
.sev-badge--high {
  color: var(--tp-high);
  background: var(--tp-high-bg);
  box-shadow: var(--tp-high-ring) 0 0 0 1px;
}
.sev-badge--medium {
  color: var(--tp-med);
  background: var(--tp-med-bg);
  box-shadow: var(--tp-med-ring) 0 0 0 1px;
}
.sev-badge--low {
  color: var(--tp-low);
  background: var(--tp-low-bg);
  box-shadow: var(--tp-low-ring) 0 0 0 1px;
}
.sev-badge--info {
  color: var(--tp-info);
  background: var(--tp-info-bg);
  box-shadow: var(--tp-info-ring) 0 0 0 1px;
}
</style>
