<template>
  <div class="landing">
    <!-- Nav -->
    <nav class="landing__nav">
      <div class="landing__nav-logo">
        <div class="logo-mark">
          <svg width="12" height="12" viewBox="0 0 12 12">
            <polygon points="6,1 11,11 1,11" fill="var(--tp-bg)" />
          </svg>
        </div>
        <span class="landing__nav-brand">ThreatPulse</span>
      </div>
      <div class="landing__nav-actions">
        <AppButton variant="ghost" size="sm" @click="$router.push('/login')">Sign in</AppButton>
        <AppButton variant="primary" size="sm" @click="$router.push('/register')">Get started</AppButton>
      </div>
    </nav>

    <!-- Hero -->
    <main class="landing__hero">
      <div class="landing__pill">
        <span class="landing__pill-dot" />
        3 CRITICAL CVEs DETECTED TODAY
      </div>

      <h1 class="landing__headline">
        Know about CVEs<br />
        <span class="landing__headline-muted">before they hit you.</span>
      </h1>

      <p class="landing__sub">
        ThreatPulse monitors NVD, RSS feeds, and security news in real-time.
        Get instant alerts for vulnerabilities in your exact tech stack.
      </p>

      <div class="landing__cta">
        <AppButton variant="primary" size="lg" @click="$router.push('/register')">
          Start monitoring free →
        </AppButton>
        <AppButton variant="ghost" size="lg" @click="$router.push('/login')">
          Sign in
        </AppButton>
      </div>

      <!-- Feature grid -->
      <div class="landing__features">
        <div v-for="f in FEATURES" :key="f.title" class="landing__feature-card">
          <div class="landing__feature-title">{{ f.title }}</div>
          <div class="landing__feature-desc">{{ f.desc }}</div>
        </div>
      </div>

      <!-- Tech tags -->
      <div class="landing__tech">
        <MonoLabel style="display:block; margin-bottom:12px; text-align:center">
          Monitors vulnerabilities in
        </MonoLabel>
        <div class="landing__tech-tags">
          <span
            v-for="t in TECH_LIST"
            :key="t"
            class="landing__tech-tag"
          >{{ t }}</span>
        </div>
      </div>
    </main>

    <!-- Footer -->
    <footer class="landing__footer">
      <MonoLabel>THREATPULSE · 2026</MonoLabel>
      <MonoLabel>viktor.hutsuliak — portfolio project</MonoLabel>
    </footer>
  </div>
</template>

<script setup lang="ts">
import AppButton from 'src/components/AppButton.vue'
import MonoLabel from 'src/components/MonoLabel.vue'

const FEATURES = [
  {
    title: 'Stack-aware alerts',
    desc: 'Define your tech stack once. Get notified only about CVEs that affect spring-boot, kafka, or whatever you run.',
  },
  {
    title: 'AI-powered summaries',
    desc: 'Every threat gets an LLM-generated triage: severity, affected versions, recommended patch, and an action plan.',
  },
  {
    title: 'Real-time pipeline',
    desc: 'NVD → Kafka → analyzer → email in under 60 seconds. No polling, no manual checks, no missed vulnerabilities.',
  },
]

const TECH_LIST = [
  'spring-boot', 'postgresql', 'kafka', 'redis', 'docker',
  'nginx', 'node.js', 'react', 'django', 'rails', 'mongodb', 'elasticsearch',
]
</script>

<style scoped>
.landing {
  min-height: 100vh;
  background: var(--tp-bg);
  color: var(--tp-text);
  display: flex;
  flex-direction: column;
  font-family: var(--tp-font);
}

.landing__nav {
  height: 56px;
  display: flex;
  align-items: center;
  padding: 0 48px;
  box-shadow: inset 0 -1px 0 var(--tp-border);
  position: sticky;
  top: 0;
  background: var(--tp-bg);
  z-index: 10;
}

.landing__nav-logo {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-right: auto;
}

.logo-mark {
  width: 22px;
  height: 22px;
  background: var(--tp-text);
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.landing__nav-brand {
  font-size: 13px;
  font-weight: 600;
  letter-spacing: -0.3px;
}

.landing__nav-actions {
  display: flex;
  gap: 8px;
}

.landing__hero {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80px 24px 64px;
  text-align: center;
}

.landing__pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 12px;
  border-radius: 9999px;
  margin-bottom: 32px;
  background: var(--tp-surf2);
  box-shadow: var(--tp-sb);
  font-size: 11px;
  color: var(--tp-sec);
  font-family: var(--tp-mono);
  letter-spacing: 0.5px;
}

.landing__pill-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--tp-crit);
  display: inline-block;
}

.landing__headline {
  font-size: 56px;
  font-weight: 600;
  line-height: 1.05;
  letter-spacing: -3px;
  max-width: 720px;
  margin: 0 0 20px;
  color: var(--tp-text);
}

.landing__headline-muted {
  color: var(--tp-muted);
}

.landing__sub {
  font-size: 16px;
  color: var(--tp-sec);
  line-height: 1.7;
  max-width: 480px;
  margin: 0 0 36px;
}

.landing__cta {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 64px;
}

.landing__features {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  max-width: 780px;
  width: 100%;
  margin-bottom: 48px;
}

.landing__feature-card {
  background: var(--tp-surface);
  border-radius: 8px;
  padding: 20px;
  box-shadow: var(--tp-card);
  text-align: left;
}

.landing__feature-title {
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 8px;
  letter-spacing: -0.2px;
}

.landing__feature-desc {
  font-size: 12px;
  color: var(--tp-muted);
  line-height: 1.7;
}

.landing__tech {
  width: 100%;
  max-width: 640px;
}

.landing__tech-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  justify-content: center;
}

.landing__tech-tag {
  padding: 3px 10px;
  border-radius: 9999px;
  font-size: 11px;
  font-family: var(--tp-mono);
  color: var(--tp-sec);
  background: var(--tp-surf2);
  box-shadow: var(--tp-sb);
}

.landing__footer {
  padding: 16px 48px;
  box-shadow: inset 0 1px 0 var(--tp-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
