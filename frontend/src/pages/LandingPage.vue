<template>
  <div class="landing" :class="{ 'landing--reduced': reduced }">
    <!-- Nav -->
    <nav class="landing__nav">
      <div class="landing__brand">
        <div class="landing__logo">
          <svg width="12" height="12" viewBox="0 0 13 13">
            <polygon points="6.5,1.6 12,11.4 1,11.4" fill="var(--tp-bg)" />
          </svg>
        </div>
        <span class="landing__brand-name">ThreatPulse</span>
      </div>
      <div class="landing__nav-actions">
        <AppButton variant="ghost" size="sm" @click="$router.push('/login')">Sign in</AppButton>
        <AppButton variant="primary" size="sm" @click="$router.push('/register')">Get started</AppButton>
      </div>
    </nav>

    <main class="landing__main">
      <!-- Hero -->
      <section class="hero">
        <div class="hero__pill">
          <span class="hero__pill-dot" />
          3 critical CVEs detected in the last 24h
        </div>

        <h1 class="hero__headline">
          Know about CVEs
          <span class="hero__headline-line">before they ship.</span>
        </h1>

        <p class="hero__sub">
          ThreatPulse watches NVD, RSS feeds, and security news around the clock,
          then alerts you only about vulnerabilities in the exact stack you run.
        </p>

        <div class="hero__cta">
          <AppButton variant="primary" size="lg" @click="$router.push('/register')">
            Start monitoring free
          </AppButton>
          <AppButton variant="ghost" size="lg" @click="$router.push('/login')">
            Sign in
          </AppButton>
        </div>

        <p class="hero__note">No credit card. Free tier covers a full personal stack.</p>
      </section>

      <!-- Live product preview -->
      <section class="preview" data-reveal>
        <div class="preview__chrome">
          <div class="preview__chrome-brand">
            <div class="preview__chrome-mark">
              <svg width="9" height="9" viewBox="0 0 13 13">
                <polygon points="6.5,1.6 12,11.4 1,11.4" fill="var(--tp-bg)" />
              </svg>
            </div>
            <span>threatpulse</span>
            <span class="preview__chrome-path">/ feed</span>
          </div>
          <div class="preview__chrome-live">
            <span class="preview__chrome-dot" />
            streaming
          </div>
        </div>

        <div class="preview__head">
          <span>Vulnerability</span>
          <span>Severity</span>
          <span class="preview__col-tech">Technology</span>
          <span class="preview__col-cvss">CVSS</span>
        </div>

        <TransitionGroup tag="div" name="feed" class="preview__rows">
          <div
            v-for="(row, i) in rows"
            :key="row.id"
            class="preview__row"
          >
            <div class="preview__vuln">
              <span v-if="i === 0" class="preview__new" />
              <span class="preview__cve">{{ row.cve }}</span>
              <span class="preview__title">{{ row.title }}</span>
            </div>
            <div>
              <span class="preview__sev" :style="sevStyle(row.sev)">
                {{ SEV_LABEL[row.sev] }}
              </span>
            </div>
            <span class="preview__tech preview__col-tech">{{ row.tech }}</span>
            <span
              class="preview__cvss preview__col-cvss"
              :style="{ color: SEV_COLOR[row.sev] }"
            >{{ row.cvss }}</span>
          </div>
        </TransitionGroup>

        <div class="preview__foot">
          <span>{{ rows.length }} of 1,284 tracked vulnerabilities</span>
          <span>filtered to your stack</span>
        </div>
      </section>

      <!-- Pipeline -->
      <section class="pipeline-wrap" data-reveal>
        <MonoLabel class="section-kicker">From source to inbox in under a minute</MonoLabel>
        <div class="pipeline">
          <template v-for="(node, i) in PIPELINE" :key="node">
            <span
              class="pipeline__node"
              :class="{ 'pipeline__node--end': i === PIPELINE.length - 1 }"
            >{{ node }}</span>
            <span
              v-if="i < PIPELINE.length - 1"
              class="pipeline__wire"
              :style="{ '--d': i * 0.8 + 's' }"
            />
          </template>
        </div>
      </section>

      <!-- Feature manifest -->
      <section class="features" data-reveal>
        <div
          v-for="(f, i) in FEATURES"
          :key="f.title"
          class="feature"
        >
          <span class="feature__num">{{ String(i + 1).padStart(2, '0') }}</span>
          <div class="feature__name">{{ f.title }}</div>
          <div class="feature__desc">{{ f.desc }}</div>
        </div>
      </section>

      <!-- Tech -->
      <section class="tech" data-reveal>
        <MonoLabel class="section-kicker">Monitors vulnerabilities in</MonoLabel>
        <div class="tech__tags">
          <span v-for="t in TECH_LIST" :key="t" class="tech__tag">{{ t }}</span>
        </div>
      </section>
    </main>

    <footer class="landing__footer">
      <MonoLabel>THREATPULSE · 2026</MonoLabel>
      <MonoLabel>viktor.hutsuliak · portfolio project</MonoLabel>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import AppButton from 'src/components/AppButton.vue'
import MonoLabel from 'src/components/MonoLabel.vue'

const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches

type Sev = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW'

const SEV_LABEL: Record<Sev, string> = {
  CRITICAL: 'Critical',
  HIGH: 'High',
  MEDIUM: 'Medium',
  LOW: 'Low',
}

const SEV_COLOR: Record<Sev, string> = {
  CRITICAL: 'var(--tp-crit)',
  HIGH: 'var(--tp-high)',
  MEDIUM: 'var(--tp-med)',
  LOW: 'var(--tp-low)',
}

const SEV_BG: Record<Sev, string> = {
  CRITICAL: 'var(--tp-crit-bg)',
  HIGH: 'var(--tp-high-bg)',
  MEDIUM: 'var(--tp-med-bg)',
  LOW: 'var(--tp-low-bg)',
}

const SEV_RING: Record<Sev, string> = {
  CRITICAL: 'var(--tp-crit-ring)',
  HIGH: 'var(--tp-high-ring)',
  MEDIUM: 'var(--tp-med-ring)',
  LOW: 'var(--tp-low-ring)',
}

function sevStyle(s: Sev) {
  return {
    color: SEV_COLOR[s],
    background: SEV_BG[s],
    boxShadow: `${SEV_RING[s]} 0 0 0 1px`,
  }
}

interface ThreatItem {
  cve: string
  title: string
  sev: Sev
  tech: string
  cvss: string
}

const POOL: ThreatItem[] = [
  { cve: 'CVE-2026-3399', title: 'RCE via SpEL expression injection', sev: 'CRITICAL', tech: 'spring-boot', cvss: '9.8' },
  { cve: 'CVE-2026-3601', title: 'Heap overflow in TLS session resume', sev: 'CRITICAL', tech: 'openssl', cvss: '9.4' },
  { cve: 'CVE-2026-3107', title: 'Auth bypass in OAuth2 token check', sev: 'HIGH', tech: 'spring-security', cvss: '8.1' },
  { cve: 'CVE-2026-2980', title: 'SQL injection via JSONB query path', sev: 'HIGH', tech: 'postgresql', cvss: '7.5' },
  { cve: 'CVE-2026-3512', title: 'Path traversal in static asset handler', sev: 'HIGH', tech: 'nginx', cvss: '7.2' },
  { cve: 'CVE-2026-2841', title: 'DoS on Kafka consumer rebalance', sev: 'MEDIUM', tech: 'kafka', cvss: '5.9' },
  { cve: 'CVE-2026-3260', title: 'Prototype pollution in config parser', sev: 'MEDIUM', tech: 'node.js', cvss: '6.4' },
  { cve: 'CVE-2026-3044', title: 'Cache poisoning via crafted key prefix', sev: 'LOW', tech: 'redis', cvss: '3.7' },
]

interface FeedRow extends ThreatItem {
  id: number
}

let seq = 5
const rows = ref<FeedRow[]>(
  POOL.slice(0, 5).map((t, i) => ({ ...t, id: i })),
)

let timer: number | undefined

function rotate(): void {
  const next = POOL[seq % POOL.length]!
  rows.value = [{ ...next, id: seq }, ...rows.value.slice(0, 4)]
  seq += 1
}

const PIPELINE = ['NVD feed', 'Kafka', 'AI analyzer', 'Your inbox']

const FEATURES = [
  {
    title: 'Stack-aware alerts',
    desc: 'Define your stack once. Get notified only about CVEs that touch spring-boot, kafka, or whatever you actually run.',
  },
  {
    title: 'AI-triaged summaries',
    desc: 'Every threat arrives with an LLM triage: severity, affected versions, the recommended patch, and a clear next action.',
  },
  {
    title: 'Real-time pipeline',
    desc: 'NVD to Kafka to analyzer to email in under 60 seconds. No polling, no manual checks, no missed vulnerabilities.',
  },
]

const TECH_LIST = [
  'spring-boot', 'postgresql', 'kafka', 'redis', 'docker',
  'nginx', 'node.js', 'react', 'django', 'rails', 'mongodb', 'elasticsearch',
]

onMounted(() => {
  const els = document.querySelectorAll('[data-reveal]')

  if (reduced) {
    els.forEach((el) => el.classList.add('is-in'))
    return
  }

  const io = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add('is-in')
          io.unobserve(entry.target)
        }
      })
    },
    { threshold: 0.15 },
  )
  els.forEach((el) => io.observe(el))

  timer = window.setInterval(rotate, 3600)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
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

/* ── Nav ── */
.landing__nav {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: inset 0 -1px 0 var(--tp-border);
  position: sticky;
  top: 0;
  background: color-mix(in oklab, var(--tp-bg) 86%, transparent);
  backdrop-filter: blur(8px);
  z-index: 10;
}

.landing__brand {
  display: flex;
  align-items: center;
  gap: 8px;
}

.landing__logo {
  width: 22px;
  height: 22px;
  background: var(--tp-text);
  border-radius: 5px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.landing__brand-name {
  font-size: 13px;
  font-weight: 600;
  letter-spacing: -0.3px;
}

.landing__nav-actions {
  display: flex;
  gap: 8px;
}

/* ── Main ── */
.landing__main {
  flex: 1;
  width: 100%;
  max-width: 760px;
  margin: 0 auto;
  padding: 0 24px;
}

/* ── Hero ── */
.hero {
  padding: 96px 0 72px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.hero__pill {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 4px 13px;
  border-radius: 9999px;
  background: var(--tp-surf2);
  box-shadow: var(--tp-sb);
  font-size: 11px;
  font-family: var(--tp-mono);
  letter-spacing: 0.2px;
  color: var(--tp-sec);
  margin-bottom: 30px;
  animation: heroIn 0.6s var(--tp-ease) 0.05s both;
}

.hero__pill-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--tp-crit);
  animation: pillPulse 2.4s ease-in-out infinite;
}

.hero__headline {
  font-size: clamp(38px, 6.2vw, 60px);
  font-weight: 600;
  line-height: 1.05;
  letter-spacing: -2.4px;
  margin: 0 0 22px;
  animation: heroIn 0.6s var(--tp-ease) 0.12s both;
}

.hero__headline-line {
  display: block;
  color: var(--tp-muted);
}

.hero__sub {
  font-size: 16px;
  line-height: 1.7;
  color: var(--tp-sec);
  max-width: 52ch;
  margin: 0 0 32px;
  animation: heroIn 0.6s var(--tp-ease) 0.2s both;
}

.hero__cta {
  display: flex;
  gap: 10px;
  animation: heroIn 0.6s var(--tp-ease) 0.28s both;
}

.hero__note {
  margin: 18px 0 0;
  font-size: 12px;
  color: var(--tp-muted);
  animation: heroIn 0.6s var(--tp-ease) 0.36s both;
}

/* ── Reveal ── */
[data-reveal] {
  opacity: 0;
  transform: translateY(20px);
  transition:
    opacity 0.6s var(--tp-ease),
    transform 0.6s var(--tp-ease);
}

[data-reveal].is-in {
  opacity: 1;
  transform: none;
}

.landing--reduced [data-reveal] {
  transition: none;
}

/* ── Preview panel ── */
.preview {
  border-radius: 10px;
  background: var(--tp-surface);
  box-shadow: var(--tp-card);
  overflow: hidden;
  margin-bottom: 64px;
}

.preview__chrome {
  height: 38px;
  padding: 0 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: inset 0 -1px 0 var(--tp-border);
}

.preview__chrome-brand {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 12px;
  font-weight: 500;
  color: var(--tp-sec);
}

.preview__chrome-mark {
  width: 16px;
  height: 16px;
  border-radius: 4px;
  background: var(--tp-text);
  display: flex;
  align-items: center;
  justify-content: center;
}

.preview__chrome-path {
  color: var(--tp-muted);
  font-family: var(--tp-mono);
  font-size: 11px;
}

.preview__chrome-live {
  display: flex;
  align-items: center;
  gap: 6px;
  font-family: var(--tp-mono);
  font-size: 10px;
  letter-spacing: 0.6px;
  text-transform: uppercase;
  color: var(--tp-muted);
}

.preview__chrome-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--tp-low);
  animation: pillPulse 2s ease-in-out infinite;
}

.preview__head,
.preview__row {
  display: grid;
  grid-template-columns: 1fr 84px 116px 48px;
  gap: 14px;
  align-items: center;
  padding: 0 16px;
}

.preview__head {
  height: 32px;
  box-shadow: inset 0 -1px 0 var(--tp-border);
  font-family: var(--tp-mono);
  font-size: 10px;
  letter-spacing: 0.6px;
  text-transform: uppercase;
  color: var(--tp-muted);
}

.preview__rows {
  position: relative;
}

.preview__row {
  height: 52px;
  box-shadow: inset 0 -1px 0 var(--tp-border);
}

.preview__vuln {
  display: flex;
  align-items: baseline;
  gap: 9px;
  min-width: 0;
}

.preview__new {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--tp-low);
  flex-shrink: 0;
  align-self: center;
  animation: pillPulse 2s ease-in-out infinite;
}

.preview__cve {
  font-family: var(--tp-mono);
  font-size: 11px;
  color: var(--tp-sec);
  flex-shrink: 0;
}

.preview__title {
  font-size: 13px;
  color: var(--tp-text);
  letter-spacing: -0.1px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview__sev {
  padding: 2px 9px;
  border-radius: 9999px;
  font-size: 10px;
  font-weight: 500;
}

.preview__tech {
  font-family: var(--tp-mono);
  font-size: 11px;
  color: var(--tp-sec);
}

.preview__cvss {
  font-family: var(--tp-mono);
  font-size: 13px;
  font-weight: 500;
  text-align: right;
}

.preview__foot {
  height: 36px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-family: var(--tp-mono);
  font-size: 10px;
  letter-spacing: 0.4px;
  color: var(--tp-muted);
}

/* Feed stream transitions */
.feed-move {
  transition: transform 0.55s var(--tp-ease);
}

.feed-enter-active {
  transition:
    opacity 0.5s ease,
    transform 0.5s var(--tp-ease),
    background-color 0.7s ease;
}

.feed-leave-active {
  transition:
    opacity 0.3s ease,
    transform 0.3s ease;
  position: absolute;
  width: 100%;
}

.feed-enter-from {
  opacity: 0;
  transform: translateY(-14px);
  background-color: rgba(255, 255, 255, 0.05);
}

.feed-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

.landing--reduced .feed-move,
.landing--reduced .feed-enter-active,
.landing--reduced .feed-leave-active {
  transition: none;
}

/* ── Pipeline ── */
.pipeline-wrap {
  margin-bottom: 64px;
}

.section-kicker {
  display: block;
  text-align: center;
  margin-bottom: 16px;
}

.pipeline {
  display: flex;
  align-items: center;
}

.pipeline__node {
  font-family: var(--tp-mono);
  font-size: 11px;
  color: var(--tp-sec);
  padding: 6px 12px;
  border-radius: 6px;
  background: var(--tp-surf2);
  box-shadow: var(--tp-sb);
  white-space: nowrap;
  flex-shrink: 0;
}

.pipeline__node--end {
  color: var(--tp-text);
  box-shadow: var(--tp-sb2);
}

.pipeline__wire {
  flex: 1;
  height: 1px;
  background:
    linear-gradient(90deg, transparent 42%, var(--tp-text) 50%, transparent 58%),
    var(--tp-dimmer);
  background-size: 240% 1px, 100% 1px;
  background-position: 180% center, 0 center;
  background-repeat: no-repeat;
  animation: wireGlint 2.4s linear var(--d) infinite;
}

.landing--reduced .pipeline__wire {
  animation: none;
  background: var(--tp-dimmer);
}

/* ── Features ── */
.features {
  margin-bottom: 64px;
}

.feature {
  display: grid;
  grid-template-columns: 30px 1fr 1.6fr;
  gap: 0 30px;
  align-items: baseline;
  padding: 22px 0;
  box-shadow: inset 0 1px 0 var(--tp-border);
}

.feature:last-child {
  box-shadow: inset 0 1px 0 var(--tp-border), inset 0 -1px 0 var(--tp-border);
}

.feature__num {
  font-family: var(--tp-mono);
  font-size: 10px;
  color: var(--tp-dimmer);
}

.feature__name {
  font-size: 14px;
  font-weight: 500;
  letter-spacing: -0.2px;
}

.feature__desc {
  font-size: 13px;
  line-height: 1.7;
  color: var(--tp-muted);
}

/* ── Tech ── */
.tech {
  margin-bottom: 72px;
}

.tech__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  justify-content: center;
}

.tech__tag {
  padding: 3px 11px;
  border-radius: 9999px;
  font-family: var(--tp-mono);
  font-size: 11px;
  color: var(--tp-sec);
  background: var(--tp-surf2);
  box-shadow: var(--tp-sb);
}

/* ── Footer ── */
.landing__footer {
  padding: 18px 24px;
  box-shadow: inset 0 1px 0 var(--tp-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* ── Keyframes ── */
@keyframes heroIn {
  from { opacity: 0; transform: translateY(14px); }
  to   { opacity: 1; transform: translateY(0); }
}

@keyframes pillPulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50%      { opacity: 0.4; transform: scale(0.82); }
}

@keyframes wireGlint {
  from { background-position: 180% center, 0 center; }
  to   { background-position: -80% center, 0 center; }
}

.landing--reduced .hero__pill,
.landing--reduced .hero__headline,
.landing--reduced .hero__sub,
.landing--reduced .hero__cta,
.landing--reduced .hero__note { animation: none; }

.landing--reduced .hero__pill-dot,
.landing--reduced .preview__chrome-dot,
.landing--reduced .preview__new { animation: none; }

/* ── Responsive ── */
@media (max-width: 560px) {
  .preview__head,
  .preview__row {
    grid-template-columns: 1fr 78px;
  }

  .preview__col-tech,
  .preview__col-cvss {
    display: none;
  }

  .feature {
    grid-template-columns: 30px 1fr;
    gap: 4px 30px;
  }

  .feature__desc {
    grid-column: 2;
  }

  .landing__footer {
    flex-direction: column;
    gap: 6px;
  }
}
</style>
