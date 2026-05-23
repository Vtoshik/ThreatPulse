<template>
  <div class="page-pad tp-fade-in">
    <PageTitle title="Settings" sub="Profile & notification preferences" />

    <div class="settings-form">
      <!-- Tech Stack -->
      <AppCard style="padding:20px">
        <MonoLabel style="display:block; margin-bottom:14px">Technology Stack</MonoLabel>

        <div class="stack-tags">
          <AppTag
            v-for="t in stack"
            :key="t"
            :label="t"
            color="var(--tp-sec)"
            :on-remove="() => removeStack(t)"
          />
        </div>

        <div class="stack-quick">
          <span
            v-for="t in remainingAll"
            :key="t"
            class="stack-pick"
            @click="addStack(t)"
          >{{ t }}</span>
        </div>

        <div class="stack-input-row">
          <AppInput
            v-model="stackInput"
            placeholder="Add custom..."
            style="font-size:12px"
            @keydown.enter="addCustomStack"
          />
          <AppButton variant="ghost" size="sm" @click="addCustomStack">Add</AppButton>
        </div>
      </AppCard>

      <!-- Notifications -->
      <AppCard style="padding:20px">
        <MonoLabel style="display:block; margin-bottom:14px">Notifications</MonoLabel>

        <div style="margin-bottom:16px">
          <MonoLabel style="display:block; margin-bottom:8px">Minimum alert severity</MonoLabel>
          <div class="sev-picker">
            <button
              v-for="s in SEVERITIES"
              :key="s"
              class="sev-btn"
              :class="{ 'sev-btn--active': minSev === s }"
              :style="minSev === s ? activeSevStyle(s) : {}"
              @click="minSev = s"
            >
              {{ s }}
            </button>
          </div>
        </div>

        <div class="notif-row">
          <div>
            <div class="notif-label">Email alerts</div>
            <div class="notif-sub">{{ authStore.user?.email ?? '' }}</div>
          </div>
          <ToggleSwitch v-model="emailOn" />
        </div>
      </AppCard>

      <!-- Actions -->
      <div class="form-actions">
        <AppButton variant="primary" size="md" :disabled="saving" @click="save">
          {{ saving ? 'Saving…' : 'Save changes' }}
        </AppButton>
        <AppButton variant="ghost" size="md" @click="reset">Cancel</AppButton>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import type { Severity } from 'src/types/threat'
import { useSeverity } from 'src/composables/useSeverity'
import { useAuthStore } from 'src/stores/auth'
import api from 'src/services/api'
import PageTitle from 'src/components/PageTitle.vue'
import AppCard from 'src/components/AppCard.vue'
import AppInput from 'src/components/AppInput.vue'
import AppButton from 'src/components/AppButton.vue'
import AppTag from 'src/components/AppTag.vue'
import MonoLabel from 'src/components/MonoLabel.vue'
import ToggleSwitch from 'src/components/ToggleSwitch.vue'

const { sevColor } = useSeverity()
const authStore = useAuthStore()

const SEVERITIES: Severity[] = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW']
const ALL_TECH = ['spring-boot', 'postgresql', 'kafka', 'redis', 'docker', 'nginx', 'node.js', 'mongodb', 'react', 'vue', 'django']

const stack      = ref<string[]>([])
const stackInput = ref('')
const minSev     = ref<Severity>('HIGH')
const emailOn    = ref(false)
const saving     = ref(false)

const remainingAll = computed(() => ALL_TECH.filter(t => !stack.value.includes(t)))

function activeSevStyle(s: Severity) {
  const c = sevColor(s)
  return { background: `${c}15`, color: c, boxShadow: `${c}40 0 0 0 1px` }
}

function addStack(t: string) {
  if (!stack.value.includes(t)) stack.value.push(t)
}

function removeStack(t: string) {
  stack.value = stack.value.filter(x => x !== t)
}

function addCustomStack() {
  const t = stackInput.value.trim()
  if (t) { addStack(t); stackInput.value = '' }
}

function reset() {
  stack.value = [...(authStore.user?.technologies ?? [])]
}

async function save() {
  saving.value = true
  try {
    await api.put('/api/user/technologies', stack.value)
    await api.put('/api/user/preferences', { minSeverity: minSev.value, emailAlertsEnabled: emailOn.value })
    if (authStore.user) {
      authStore.user.technologies = [...stack.value]
    }
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  stack.value = [...(authStore.user?.technologies ?? [])]
})
</script>

<style scoped>
.page-pad { padding: 24px 28px; }

@media (max-width: 768px) {
  .page-pad { padding: 16px; }
  .settings-form { max-width: 100%; }
}

.settings-form {
  max-width: 560px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stack-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  margin-bottom: 12px;
}

.stack-quick {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 10px;
}

.stack-pick {
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

.stack-pick:hover { color: var(--tp-sec); }

.stack-input-row {
  display: flex;
  gap: 6px;
}

.sev-picker {
  display: flex;
  gap: 5px;
}

.sev-btn {
  padding: 4px 12px;
  border-radius: 9999px;
  cursor: pointer;
  background: transparent;
  color: var(--tp-muted);
  border: none;
  box-shadow: var(--tp-sb);
  font-family: var(--tp-font);
  font-size: 10px;
  transition: all 0.15s;
}

.sev-btn:hover:not(.sev-btn--active) { color: var(--tp-sec); }

.notif-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
}

.notif-label {
  font-size: 13px;
  color: var(--tp-text);
  margin-bottom: 2px;
}

.notif-sub {
  font-size: 11px;
  color: var(--tp-muted);
  font-family: var(--tp-mono);
}

.form-actions {
  display: flex;
  gap: 8px;
}
</style>