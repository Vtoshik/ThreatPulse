<template>
  <div class="auth-shell">
    <div class="auth-shell__logo">
      <div class="auth-shell__logo-mark">
        <svg width="13" height="13" viewBox="0 0 13 13">
          <polygon points="6.5,1 12,12 1,12" fill="var(--tp-bg)" />
        </svg>
      </div>
      <span class="auth-shell__brand">ThreatPulse</span>
    </div>

    <div class="auth-shell__box-wrap">
      <!-- Step 1: Credentials -->
      <template v-if="step === 1">
        <div class="auth-shell__heading">
          <h1 class="auth-shell__title">Create account</h1>
          <p class="auth-shell__sub">Free · no credit card required</p>
        </div>

        <div class="auth-shell__box">
          <div class="auth-form">
            <div class="auth-form__field">
              <MonoLabel style="display:block; margin-bottom:6px">Name</MonoLabel>
              <AppInput v-model="form.name" placeholder="Viktor Hutsuliak" />
            </div>
            <div class="auth-form__field">
              <MonoLabel style="display:block; margin-bottom:6px">Email</MonoLabel>
              <AppInput v-model="form.email" placeholder="you@example.com" type="email" />
            </div>
            <div class="auth-form__field">
              <MonoLabel style="display:block; margin-bottom:6px">Password</MonoLabel>
              <AppInput v-model="form.password" placeholder="min 8 characters" type="password" />
            </div>
            <AppButton variant="primary" size="md" style="width:100%; margin-top:4px" @click="step = 2">
              Continue → Set up stack
            </AppButton>
          </div>
        </div>

        <div class="auth-shell__footer">
          Have an account?&nbsp;
          <span class="auth-shell__switch" @click="$router.push('/login')">Sign in</span>
        </div>
      </template>

      <!-- Step 2: Tech stack -->
      <template v-else>
        <div class="auth-shell__heading">
          <h1 class="auth-shell__title">Your tech stack</h1>
          <p class="auth-shell__sub">We'll alert you only about relevant CVEs</p>
        </div>

        <div class="auth-shell__box">
          <div class="auth-form">
            <!-- Selected tags -->
            <div class="stack-tags">
              <AppTag
                v-for="t in stack"
                :key="t"
                :label="t"
                color="var(--tp-sec)"
                :on-remove="() => removeStack(t)"
              />
            </div>

            <div class="auth-form__divider" />

            <!-- Quick picks -->
            <div>
              <MonoLabel style="display:block; margin-bottom:8px">Common technologies</MonoLabel>
              <div class="stack-picks">
                <span
                  v-for="t in remainingTech"
                  :key="t"
                  class="stack-pick"
                  @click="addStack(t)"
                >{{ t }}</span>
              </div>
            </div>

            <!-- Custom input -->
            <div class="stack-input-row">
              <AppInput v-model="stackInput" placeholder="add custom..." style="font-size:12px" />
              <AppButton variant="ghost" size="sm" @click="addCustom">Add</AppButton>
            </div>

            <AppButton variant="primary" size="md" style="width:100%; margin-top:4px" @click="handleRegister">
              Start monitoring →
            </AppButton>
          </div>
        </div>

        <div class="auth-shell__footer">
          <span class="auth-shell__switch" @click="step = 1">← Back</span>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import MonoLabel from 'src/components/MonoLabel.vue'
import AppInput from 'src/components/AppInput.vue'
import AppButton from 'src/components/AppButton.vue'
import AppTag from 'src/components/AppTag.vue'

const router = useRouter()

const step = ref(1)
const form = ref({ name: '', email: '', password: '' })
const stack = ref(['spring-boot', 'postgresql'])
const stackInput = ref('')

const ALL_TECH = ['spring-boot', 'postgresql', 'kafka', 'redis', 'docker', 'nginx', 'node.js', 'mongodb', 'react', 'vue', 'django']
const remainingTech = computed(() => ALL_TECH.filter(t => !stack.value.includes(t)))

function addStack(t: string) {
  if (!stack.value.includes(t)) stack.value.push(t)
}

function removeStack(t: string) {
  stack.value = stack.value.filter(x => x !== t)
}

function addCustom() {
  const t = stackInput.value.trim()
  if (t) { addStack(t); stackInput.value = '' }
}

async function handleRegister() {
  await new Promise(r => setTimeout(r, 600))
  void router.push('/app/dashboard')
}
</script>

<style scoped>
.auth-shell {
  min-height: 100vh;
  background: var(--tp-bg);
  color: var(--tp-text);
  font-family: var(--tp-font);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.auth-shell__logo {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 40px;
}

.auth-shell__logo-mark {
  width: 24px;
  height: 24px;
  background: var(--tp-text);
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.auth-shell__brand {
  font-size: 14px;
  font-weight: 600;
  letter-spacing: -0.4px;
}

.auth-shell__box-wrap {
  width: 100%;
  max-width: 360px;
}

.auth-shell__heading {
  margin-bottom: 24px;
  text-align: center;
}

.auth-shell__title {
  font-size: 22px;
  font-weight: 600;
  letter-spacing: -1px;
  margin: 0 0 6px;
}

.auth-shell__sub {
  font-size: 13px;
  color: var(--tp-muted);
  margin: 0;
}

.auth-shell__box {
  background: var(--tp-surface);
  border-radius: 8px;
  padding: 24px;
  box-shadow: var(--tp-card);
}

.auth-shell__footer {
  text-align: center;
  margin-top: 16px;
  font-size: 12px;
  color: var(--tp-muted);
}

.auth-shell__switch {
  color: var(--tp-text);
  cursor: pointer;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.auth-form__field {
  display: flex;
  flex-direction: column;
}

.auth-form__divider {
  height: 1px;
  background: var(--tp-surf3);
}

.stack-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  min-height: 36px;
}

.stack-picks {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.stack-pick {
  padding: 2px 9px;
  border-radius: 9999px;
  font-size: 11px;
  cursor: pointer;
  font-family: var(--tp-mono);
  color: var(--tp-muted);
  background: var(--tp-surf2);
  box-shadow: var(--tp-sb);
  transition: color 0.1s;
}

.stack-pick:hover {
  color: var(--tp-sec);
}

.stack-input-row {
  display: flex;
  gap: 6px;
}
</style>
