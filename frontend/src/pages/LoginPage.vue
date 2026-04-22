<template>
  <div class="auth-shell">
    <!-- Logo -->
    <div class="auth-shell__logo">
      <div class="auth-shell__logo-mark">
        <svg width="13" height="13" viewBox="0 0 13 13">
          <polygon points="6.5,1 12,12 1,12" fill="var(--tp-bg)" />
        </svg>
      </div>
      <span class="auth-shell__brand">ThreatPulse</span>
    </div>

    <div class="auth-shell__box-wrap">
      <div class="auth-shell__heading">
        <h1 class="auth-shell__title">Sign in</h1>
        <p class="auth-shell__sub">Enter your credentials to continue</p>
      </div>

      <div class="auth-shell__box">
        <div class="auth-form">
          <div class="auth-form__field">
            <MonoLabel style="display:block; margin-bottom:6px">Email</MonoLabel>
            <AppInput v-model="email" placeholder="you@example.com" type="email" />
          </div>

          <div class="auth-form__field">
            <MonoLabel style="display:block; margin-bottom:6px">Password</MonoLabel>
            <AppInput v-model="password" placeholder="••••••••" type="password" />
          </div>

          <AppButton
            variant="primary"
            size="md"
            style="width:100%; margin-top:4px"
            :disabled="loading"
            @click="handleLogin"
          >
            {{ loading ? 'Signing in...' : 'Sign in →' }}
          </AppButton>

          <div class="auth-form__divider" />

          <div class="auth-form__forgot">
            <span class="auth-form__link">Forgot password?</span>
          </div>
        </div>
      </div>

      <div class="auth-shell__footer">
        No account?&nbsp;
        <span class="auth-shell__switch" @click="$router.push('/register')">Create one</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import MonoLabel from 'src/components/MonoLabel.vue'
import AppInput from 'src/components/AppInput.vue'
import AppButton from 'src/components/AppButton.vue'

const router  = useRouter()
const email   = ref('viktor@example.com')
const password = ref('')
const loading = ref(false)

async function handleLogin() {
  loading.value = true
  await new Promise(r => setTimeout(r, 800))
  loading.value = false
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

.auth-form__forgot {
  text-align: right;
  font-size: 11px;
}

.auth-form__link {
  color: var(--tp-muted);
  cursor: pointer;
  text-decoration: underline;
  text-underline-offset: 3px;
}
</style>
