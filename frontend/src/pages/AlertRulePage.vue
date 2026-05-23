<template>
  <div class="page-pad tp-fade-in">
    <div class="back-link" @click="$router.push('/app/alerts')">Back to alerts</div>

    <PageTitle
      :title="isEdit ? 'Edit rule' : 'New alert rule'"
      sub="Get notified when matching CVEs are detected"
    />

    <div class="rule-form">
      <!-- Name -->
      <AppCard style="padding:20px">
        <MonoLabel style="display:block; margin-bottom:8px">Rule name</MonoLabel>
        <AppInput v-model="name" placeholder='e.g. "Critical in my stack"' />
      </AppCard>

      <!-- Severity -->
      <AppCard style="padding:20px">
        <MonoLabel style="display:block; margin-bottom:12px">Minimum severity</MonoLabel>
        <div class="sev-picker">
          <button
            v-for="s in SEVERITIES"
            :key="s"
            class="sev-btn"
            :class="{ 'sev-btn--active': severity === s }"
            :style="severity === s ? activeSevStyle(s) : {}"
            @click="severity = s"
          >
            {{ s }}
          </button>
        </div>
        <div class="sev-hint">
          Will alert on: {{ willAlertOn.join(', ') }}
        </div>
      </AppCard>

      <!-- Technologies -->
      <AppCard style="padding:20px">
        <MonoLabel style="display:block; margin-bottom:12px">
          Technology filter
          <span style="color:var(--tp-dimmer)">(empty = all)</span>
        </MonoLabel>

        <div v-if="tech.length > 0" class="tech-tags">
          <AppTag
            v-for="t in tech"
            :key="t"
            :label="t"
            color="var(--tp-sec)"
            :on-remove="() => removeTech(t)"
          />
        </div>

        <div class="tech-input-row">
          <AppInput
            v-model="techInput"
            placeholder="spring-boot, kafka, ..."
            style="font-size:12px"
            @keydown.enter="addCustomTech"
          />
          <AppButton variant="ghost" size="sm" @click="addCustomTech">Add</AppButton>
        </div>

        <div class="tech-quick">
          <span
            v-for="t in quickTech"
            :key="t"
            class="tech-quick-item"
            @click="addTech(t)"
          >{{ t }}</span>
        </div>
      </AppCard>

      <!-- Actions -->
      <div class="form-actions">
        <AppButton variant="primary" size="md" style="flex:1" :disabled="saving" @click="save">
          {{ saving ? 'Saving…' : isEdit ? 'Save changes' : 'Create rule' }}
        </AppButton>
        <AppButton variant="ghost" size="md" @click="$router.push('/app/alerts')">
          Cancel
        </AppButton>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { Severity } from 'src/types/threat'
import { alertsService } from 'src/services/alerts.service'
import { useSeverity } from 'src/composables/useSeverity'
import PageTitle from 'src/components/PageTitle.vue'
import AppCard from 'src/components/AppCard.vue'
import AppInput from 'src/components/AppInput.vue'
import AppButton from 'src/components/AppButton.vue'
import AppTag from 'src/components/AppTag.vue'
import MonoLabel from 'src/components/MonoLabel.vue'

const route     = useRoute()
const router    = useRouter()
const { sevColor } = useSeverity()

const SEVERITIES: Severity[] = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO']
const ALL_STACK = ['spring-boot', 'postgresql', 'kafka', 'redis', 'docker']

const isEdit  = computed(() => !!route.params['id'])
const ruleId  = computed(() => Number(route.params['id']))

const name         = ref('')
const severity     = ref<Severity>('HIGH')
const tech         = ref<string[]>([])
const techInput    = ref('')
const saving       = ref(false)

const quickTech   = computed(() => ALL_STACK.filter(t => !tech.value.includes(t)))

const willAlertOn = computed(() =>
  SEVERITIES.slice(0, SEVERITIES.indexOf(severity.value) + 1)
)

function activeSevStyle(s: Severity) {
  const c = sevColor(s)
  return {
    background: `${c}15`,
    color: c,
    boxShadow: `${c}40 0 0 0 1px`,
  }
}

function addTech(t: string) {
  if (!tech.value.includes(t)) tech.value.push(t)
}

function removeTech(t: string) {
  tech.value = tech.value.filter(x => x !== t)
}

function addCustomTech() {
  const t = techInput.value.trim()
  if (t) { addTech(t); techInput.value = '' }
}

async function save() {
  saving.value = true
  try {
    if (isEdit.value) {
      await alertsService.updateRule(ruleId.value, name.value, severity.value, tech.value)
    } else {
      await alertsService.createRule(name.value, severity.value, tech.value)
    }
    void router.push('/app/alerts')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  if (!isEdit.value) return
  const rules = await alertsService.getRules()
  const existing = rules.find(r => r.id === ruleId.value)
  if (existing) {
    name.value = existing.name ?? ''
    severity.value = existing.minSeverity
    tech.value = [...existing.technologiesFilter]
  }
})
</script>

<style scoped>
.page-pad { padding: 24px 28px; }

@media (max-width: 768px) {
  .page-pad { padding: 16px; }
  .rule-form { max-width: 100%; }
}

.back-link {
  font-size: 12px;
  color: var(--tp-muted);
  cursor: pointer;
  margin-bottom: 24px;
  display: inline-block;
}

.back-link:hover { color: var(--tp-sec); }

.rule-form {
  max-width: 520px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.sev-picker {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.sev-btn {
  padding: 5px 14px;
  border-radius: 9999px;
  cursor: pointer;
  background: transparent;
  color: var(--tp-muted);
  border: none;
  box-shadow: var(--tp-sb);
  font-family: var(--tp-font);
  font-size: 11px;
  font-weight: 400;
  transition: all 0.15s;
}

.sev-btn:hover:not(.sev-btn--active) {
  color: var(--tp-sec);
}

.sev-hint {
  font-size: 11px;
  color: var(--tp-muted);
  margin-top: 10px;
  font-family: var(--tp-mono);
}

.tech-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  margin-bottom: 10px;
}

.tech-input-row {
  display: flex;
  gap: 6px;
  margin-bottom: 10px;
}

.tech-quick {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.tech-quick-item {
  padding: 2px 8px;
  border-radius: 9999px;
  font-size: 10px;
  cursor: pointer;
  font-family: var(--tp-mono);
  color: var(--tp-muted);
  background: var(--tp-surf2);
  box-shadow: var(--tp-sb);
  transition: color 0.1s;
}

.tech-quick-item:hover { color: var(--tp-sec); }

.channel-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
}

.channel-label {
  font-size: 13px;
  color: var(--tp-text);
  margin-bottom: 2px;
}

.channel-sub {
  font-size: 11px;
  color: var(--tp-muted);
  font-family: var(--tp-mono);
}

.form-actions {
  display: flex;
  gap: 8px;
}
</style>