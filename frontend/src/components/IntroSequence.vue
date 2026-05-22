<template>
  <div
    v-if="visible"
    class="intro"
    :class="{ 'intro--exit': exiting, 'intro--reduced': reduced }"
    role="presentation"
    @click="skip"
  >
    <div class="intro__corner intro__corner--tl">THREATPULSE // SIGNAL ACQUISITION</div>
    <div class="intro__corner intro__corner--tr">
      <span class="intro__beacon" />{{ exiting ? 'FEED LOCKED' : 'ACQUIRING' }}
    </div>

    <div class="intro__stage">
      <!-- Oscilloscope trace -->
      <div class="intro__scope">
        <svg class="intro__svg" viewBox="0 0 760 140" aria-hidden="true">
          <line class="intro__baseline" x1="0" y1="88" x2="760" y2="88" />
          <path
            class="intro__trace"
            vector-effect="non-scaling-stroke"
            d="M 0 88 L 150 88 L 168 88 L 178 64 L 188 88 L 330 88 L 348 88 L 360 30 L 372 88 L 470 88 L 486 88 L 496 88 L 503 14 L 510 120 L 517 88 L 760 88"
          />
        </svg>

        <template v-for="s in SPIKES" :key="s.cve">
          <span
            class="intro__ping"
            :style="{ left: pct(s.cx, 760), top: pct(s.cy, 140), background: s.color, '--d': s.delay + 's' }"
          />
          <span
            class="intro__dot"
            :style="{ left: pct(s.cx, 760), top: pct(s.cy, 140), background: s.color, '--d': s.delay + 's' }"
          />
          <span
            class="intro__tag"
            :style="{ left: pct(s.cx, 760), top: pct(88, 140), '--d': s.delay + 's' }"
          >
            <span class="intro__tag-sev" :style="{ color: s.color }">{{ s.label }}</span>
            {{ s.cve }}
          </span>
        </template>
      </div>

      <!-- Feed connection log -->
      <div class="intro__log">
        <div
          v-for="l in LOG"
          :key="l.src"
          class="intro__log-row"
          :style="{ '--d': l.delay + 's' }"
        >
          <span class="intro__log-src">{{ l.src }}</span>
          <span class="intro__log-meta">{{ l.meta }}</span>
          <span class="intro__log-status">
            <span class="intro__log-wait" :style="{ '--d': l.delay + 0.4 + 's' }">connecting</span>
            <span class="intro__log-ok" :style="{ '--d': l.delay + 0.45 + 's' }">online</span>
          </span>
        </div>
      </div>

      <!-- Brand lockup -->
      <div class="intro__lockup">
        <span class="intro__mark" aria-hidden="true">
          <svg width="13" height="13" viewBox="0 0 13 13">
            <polygon points="6.5,1.6 12,11.4 1,11.4" fill="var(--tp-bg)" />
          </svg>
        </span>
        <span class="intro__word">ThreatPulse</span>
      </div>
      <div class="intro__tagline">Security intelligence, filtered to your stack</div>
    </div>

    <button class="intro__skip" type="button" @click.stop="skip">Skip intro</button>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'

const emit = defineEmits<{ done: [] }>()

const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches

const visible = ref(true)
const exiting = ref(false)

// Spike apex coordinates in the 760x140 viewBox; delay = moment the trace draws past it
const SPIKES = [
  { cve: 'CVE-2026-2841', label: 'MEDIUM',   color: 'var(--tp-med)',  cx: 178, cy: 64, delay: 0.53 },
  { cve: 'CVE-2026-3107', label: 'HIGH',     color: 'var(--tp-high)', cx: 360, cy: 30, delay: 0.86 },
  { cve: 'CVE-2026-3399', label: 'CRITICAL', color: 'var(--tp-crit)', cx: 503, cy: 14, delay: 1.13 },
]

const LOG = [
  { src: 'NVD / NIST', meta: 'cve database',   delay: 0.45 },
  { src: 'RSS x6',     meta: 'security blogs', delay: 0.78 },
  { src: 'ANALYZER',   meta: 'llm pipeline',   delay: 1.10 },
]

const pct = (value: number, total: number): string => `${(value / total) * 100}%`

let exitTimer: number | undefined
let doneTimer: number | undefined

function finish(): void {
  if (exiting.value) return
  exiting.value = true
  doneTimer = window.setTimeout(() => {
    visible.value = false
    emit('done')
  }, reduced ? 320 : 640)
}

function skip(): void {
  if (exitTimer) clearTimeout(exitTimer)
  finish()
}

function onKey(e: KeyboardEvent): void {
  if (e.key === 'Escape' || e.key === 'Enter' || e.key === ' ') skip()
}

onMounted(() => {
  exitTimer = window.setTimeout(finish, reduced ? 1100 : 2700)
  window.addEventListener('keydown', onKey)
})

onBeforeUnmount(() => {
  if (exitTimer) clearTimeout(exitTimer)
  if (doneTimer) clearTimeout(doneTimer)
  window.removeEventListener('keydown', onKey)
})
</script>

<style scoped>
.intro {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(115% 80% at 50% 42%, var(--tp-surface), var(--tp-bg) 72%);
  color: var(--tp-text);
  font-family: var(--tp-font);
  cursor: pointer;
  will-change: transform;
}

.intro--exit {
  transform: translateY(-101%);
  transition: transform 0.62s var(--tp-ease-expo);
}

/* ── Corner status ── */
.intro__corner {
  position: fixed;
  top: 22px;
  font-family: var(--tp-mono);
  font-size: 10px;
  letter-spacing: 0.6px;
  color: var(--tp-muted);
  animation: introFade 0.5s ease 0.1s both;
}

.intro__corner--tl { left: 24px; }
.intro__corner--tr {
  right: 24px;
  display: flex;
  align-items: center;
  gap: 7px;
}

.intro__beacon {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--tp-crit);
  animation: introBeacon 1.4s ease-in-out infinite;
}

/* ── Stage ── */
.intro__stage {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: min(600px, 84vw);
}

/* ── Oscilloscope ── */
.intro__scope {
  position: relative;
  width: 100%;
  aspect-ratio: 760 / 140;
}

.intro__svg {
  width: 100%;
  height: 100%;
  display: block;
  overflow: visible;
}

.intro__baseline {
  stroke: var(--tp-border-bright);
  stroke-width: 1;
  vector-effect: non-scaling-stroke;
  stroke-dasharray: 2 4;
  opacity: 0;
  animation: introFade 0.5s var(--tp-ease) 0.05s both;
}

.intro__trace {
  fill: none;
  stroke: var(--tp-sec);
  stroke-width: 2;
  stroke-linejoin: round;
  stroke-linecap: round;
  stroke-dasharray: 1100;
  stroke-dashoffset: 1100;
  animation:
    introDraw 1.4s var(--tp-ease) 0.2s both,
    introTracePulse 0.7s ease 1.65s;
}

.intro__dot {
  position: absolute;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  transform: translate(-50%, -50%) scale(0);
  animation: introDot 0.4s var(--tp-ease) var(--d) both;
}

.intro__ping {
  position: absolute;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  opacity: 0;
  transform: translate(-50%, -50%) scale(0);
  animation: introPing 0.9s var(--tp-ease-expo) var(--d) both;
}

.intro__tag {
  position: absolute;
  transform: translate(-50%, 9px);
  white-space: nowrap;
  font-family: var(--tp-mono);
  font-size: 10px;
  letter-spacing: 0.3px;
  color: var(--tp-muted);
  opacity: 0;
  animation: introTag 0.5s var(--tp-ease) calc(var(--d) + 0.1s) both;
}

.intro__tag-sev {
  font-weight: 500;
  margin-right: 5px;
}

/* ── Feed log ── */
.intro__log {
  width: min(380px, 84vw);
  margin-top: 26px;
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.intro__log-row {
  display: grid;
  grid-template-columns: 88px 1fr auto;
  gap: 14px;
  align-items: baseline;
  font-family: var(--tp-mono);
  font-size: 11px;
  opacity: 0;
  animation: introLogRow 0.45s var(--tp-ease) var(--d) both;
}

.intro__log-src { color: var(--tp-sec); }
.intro__log-meta { color: var(--tp-muted); }

.intro__log-status {
  display: grid;
}

.intro__log-wait,
.intro__log-ok {
  grid-area: 1 / 1;
  text-align: right;
}

.intro__log-wait {
  color: var(--tp-muted);
  animation: introFadeOut 0.2s ease var(--d) forwards;
}

.intro__log-ok {
  color: var(--tp-text);
  opacity: 0;
  animation: introFadeIn 0.25s ease var(--d) forwards;
}

/* ── Brand lockup ── */
.intro__lockup {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 44px;
}

.intro__mark {
  width: 26px;
  height: 26px;
  border-radius: 6px;
  background: var(--tp-text);
  display: flex;
  align-items: center;
  justify-content: center;
  transform: scale(0.7);
  opacity: 0;
  animation: introMark 0.5s var(--tp-ease) 1.7s both;
}

.intro__word {
  font-size: 30px;
  font-weight: 600;
  letter-spacing: -1.4px;
  clip-path: inset(0 100% 0 0);
  animation: introWord 0.6s var(--tp-ease-expo) 1.78s both;
}

.intro__tagline {
  margin-top: 12px;
  font-family: var(--tp-mono);
  font-size: 11px;
  letter-spacing: 0.3px;
  color: var(--tp-muted);
  opacity: 0;
  animation: introTagline 0.5s var(--tp-ease) 2.15s both;
}

/* ── Skip ── */
.intro__skip {
  position: fixed;
  bottom: 22px;
  right: 24px;
  background: transparent;
  border: none;
  cursor: pointer;
  font-family: var(--tp-mono);
  font-size: 11px;
  letter-spacing: 0.4px;
  color: var(--tp-muted);
  padding: 6px 4px;
  animation: introFade 0.5s ease 0.6s both;
  transition: color 0.15s;
}

.intro__skip:hover { color: var(--tp-text); }

/* ── Keyframes ── */
@keyframes introDraw {
  to { stroke-dashoffset: 0; }
}

@keyframes introTracePulse {
  0%   { stroke: var(--tp-sec); }
  45%  { stroke: var(--tp-text); }
  100% { stroke: var(--tp-sec); }
}

@keyframes introDot {
  from { transform: translate(-50%, -50%) scale(0); }
  to   { transform: translate(-50%, -50%) scale(1); }
}

@keyframes introPing {
  0%   { transform: translate(-50%, -50%) scale(0.4); opacity: 0.55; }
  100% { transform: translate(-50%, -50%) scale(3); opacity: 0; }
}

@keyframes introTag {
  from { opacity: 0; transform: translate(-50%, 15px); }
  to   { opacity: 1; transform: translate(-50%, 9px); }
}

@keyframes introLogRow {
  from { opacity: 0; transform: translateY(6px); }
  to   { opacity: 1; transform: translateY(0); }
}

@keyframes introMark {
  from { opacity: 0; transform: scale(0.7); }
  to   { opacity: 1; transform: scale(1); }
}

@keyframes introWord {
  from { clip-path: inset(0 100% 0 0); }
  to   { clip-path: inset(0 0 0 0); }
}

@keyframes introTagline {
  from { opacity: 0; transform: translateY(5px); }
  to   { opacity: 1; transform: translateY(0); }
}

@keyframes introFade {
  from { opacity: 0; }
  to   { opacity: 1; }
}

@keyframes introFadeOut {
  to { opacity: 0; }
}

@keyframes introFadeIn {
  to { opacity: 1; }
}

@keyframes introBeacon {
  0%, 100% { opacity: 1; }
  50%      { opacity: 0.25; }
}

/* ── Reduced motion: skip choreography, hold the final frame ── */
.intro--reduced .intro__trace { animation: none; stroke-dashoffset: 0; }
.intro--reduced .intro__baseline,
.intro--reduced .intro__dot,
.intro--reduced .intro__tag,
.intro--reduced .intro__log-row,
.intro--reduced .intro__log-ok,
.intro--reduced .intro__mark,
.intro--reduced .intro__tagline,
.intro--reduced .intro__corner,
.intro--reduced .intro__skip { animation: none; opacity: 1; }
.intro--reduced .intro__ping,
.intro--reduced .intro__log-wait,
.intro--reduced .intro__beacon { display: none; }
.intro--reduced .intro__dot { transform: translate(-50%, -50%) scale(1); }
.intro--reduced .intro__tag { transform: translate(-50%, 9px); }
.intro--reduced .intro__word { animation: none; clip-path: inset(0 0 0 0); }
.intro--reduced .intro__mark { transform: scale(1); }
.intro--reduced.intro--exit {
  transform: none;
  opacity: 0;
  transition: opacity 0.3s ease;
}
</style>
