<template>
  <div class="page-pad tp-fade-in">
    <PageTitle title="Settings" sub="Profile & notification preferences" />

    <div class="settings-form">
      <!-- Profile -->
      <AppCard style="padding:20px">
        <MonoLabel style="display:block; margin-bottom:14px">Profile</MonoLabel>
        <div class="settings-fields">
          <div>
            <MonoLabel style="display:block; margin-bottom:6px">Name</MonoLabel>
            <AppInput v-model="name" />
          </div>
          <div>
            <MonoLabel style="display:block; margin-bottom:6px">Email</MonoLabel>
            <AppInput v-model="email" type="email" />
          </div>
        </div>
      </AppCard>

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

        <!-- Min severity -->
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

        <!-- Toggles -->
        <div
          v-for="row in notifRows"
          :key="row.label"
          class="notif-row"
        >
          <div>
            <div class="notif-label">{{ row.label }}</div>
            <div class="notif-sub">{{ row.sub }}</div>
          </div>
          <ToggleSwitch :model-value="row.value" @update:model-value="row.set($event)" />
        </div>
      </AppCard>

      <!-- Actions -->
      <div class="form-actions">
        <AppButton variant="primary" size="md">Save changes</AppButton>
        <AppButton variant="ghost" size="md">Cancel</AppButton>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { Severity } from 'src/types/threat'
import { useSeverity } from 'src/composables/useSeverity'
import { USER_STACK } from 'src/data/mockData'
import PageTitle from 'src/components/PageTitle.vue'
import AppCard from 'src/components/AppCard.vue'
import AppInput from 'src/components/AppInput.vue'
import AppButton from 'src/components/AppButton.vue'
import AppTag from 'src/components/AppTag.vue'
import MonoLabel from 'src/components/MonoLabel.vue'
import ToggleSwitch from 'src/components/ToggleSwitch.vue'

const { sevColor } = useSeverity()

const SEVERITIES: Severity[] = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW']
const ALL_TECH = ['spring-boot', 'postgresql', 'kafka', 'redis', 'docker', 'nginx', 'node.js', 'mongodb']

const name       = ref('Viktor Hutsuliak')
const email      = ref('viktor@example.com')
const stack      = ref([...USER_STACK])
const stackInput = ref('')
const minSev     = ref<Severity>('HIGH')
const emailOn    = ref(true)
const digestOn   = ref(false)

const remainingAll = computed(() => ALL_TECH.filter(t => !stack.value.includes(t)))

const notifRows = computed(() => [
  { label: 'Email alerts',  sub: 'Instant notification on new CVE match', value: emailOn.value,  set: (v: boolean) => { emailOn.value = v } },
  { label: 'Daily digest',  sub: 'Summary of all threats at 08:00',       value: digestOn.value, set: (v: boolean) => { digestOn.value = v } },
])

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
</script>

<style scoped>
.page-pad { padding: 24px 28px; }

.settings-form {
  max-width: 560px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.settings-fields {
  display: flex;
  flex-direction: column;
  gap: 12px;
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
  box-shadow: inset 0 -1px 0 rgba(255, 255, 255, 0.06);
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
