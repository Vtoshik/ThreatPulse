<template>
  <div class="app-shell">
    <!-- Mobile backdrop -->
    <div v-if="sidebarOpen" class="sidebar-backdrop" @click="sidebarOpen = false" />

    <!-- Sidebar -->
    <aside class="sidebar" :class="{ 'sidebar--open': sidebarOpen }">
      <!-- Logo -->
      <div class="sidebar__logo">
        <div class="sidebar__logo-mark">
          <svg width="14" height="14" viewBox="0 0 14 14">
            <polygon points="7,1.5 12.5,12.5 1.5,12.5" fill="var(--tp-bg)" />
          </svg>
        </div>
        <div>
          <div class="sidebar__brand">ThreatPulse</div>
          <div class="sidebar__brand-sub">intel platform</div>
        </div>
      </div>

      <!-- Nav -->
      <nav class="sidebar__nav">
        <router-link
          v-for="item in NAV_ITEMS"
          :key="item.id"
          :to="item.to"
          class="sidebar__nav-item"
          active-class="sidebar__nav-item--active"
        >
          <div class="sidebar__nav-item-inner">
            <span class="sidebar__nav-icon">{{ item.icon }}</span>
            <span>{{ item.label }}</span>
          </div>
          <span v-if="item.badge" class="sidebar__badge">{{ item.badge }}</span>
        </router-link>
      </nav>

      <!-- Stack -->
      <div class="sidebar__stack">
        <MonoLabel style="display:block; margin-bottom:8px">Your Stack</MonoLabel>
        <div v-if="!authStore.user?.technologies?.length" class="sidebar__stack-empty">not set</div>
        <div v-for="tech in displayedStack" :key="tech" class="sidebar__stack-item">
          <span class="sidebar__stack-dot" />
          <span class="sidebar__stack-name">{{ tech }}</span>
        </div>
        <div v-if="extraStackCount > 0" class="sidebar__stack-more">+ {{ extraStackCount }} more</div>
      </div>

      <!-- User -->
      <div class="sidebar__user">
        <div class="sidebar__avatar">{{ authStore.user?.name?.charAt(0).toUpperCase() ?? '?' }}</div>
        <div class="sidebar__user-info">
          <div class="sidebar__user-name">{{ authStore.user?.name ?? '' }}</div>
          <div class="sidebar__user-email">{{ authStore.user?.email ?? '' }}</div>
        </div>
        <button class="sidebar__logout" title="Sign out" @click="handleLogout">Sign out</button>
      </div>
    </aside>

    <!-- Main -->
    <div class="app-main">
      <!-- Top bar -->
      <header class="topbar">
        <button class="topbar__hamburger" aria-label="Menu" @click="sidebarOpen = !sidebarOpen">
          <span /><span /><span />
        </button>
        <div class="topbar__title">
          <span class="topbar__title-main">{{ route.meta.title as string }}</span>
          <span v-if="route.meta.sub" class="topbar__title-sub"> {{ route.meta.sub }}</span>
        </div>
        <div class="topbar__actions">
          <slot name="topbar-actions" />
        </div>
      </header>

      <!-- Page content -->
      <div class="app-content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MonoLabel from 'src/components/MonoLabel.vue'
import { useAuthStore } from 'src/stores/auth'

const route     = useRoute()
const router    = useRouter()
const authStore = useAuthStore()

const sidebarOpen = ref(false)
watch(() => route.path, () => { sidebarOpen.value = false })

const STACK_LIMIT = 5
const displayedStack = computed(() => authStore.user?.technologies?.slice(0, STACK_LIMIT) ?? [])
const extraStackCount = computed(() => Math.max(0, (authStore.user?.technologies?.length ?? 0) - STACK_LIMIT))

const NAV_ITEMS: { id: string; label: string; icon: string; to: string; badge?: number }[] = [
  { id: 'dashboard', label: 'Dashboard',   icon: '◈', to: '/app/dashboard' },
  { id: 'feed',      label: 'Threat Feed', icon: '⊟', to: '/app/feed'      },
  { id: 'search',    label: 'Search',      icon: '⌕', to: '/app/search'    },
  { id: 'alerts',    label: 'Alerts',      icon: '⚐', to: '/app/alerts' },
  { id: 'watchlist', label: 'Watchlist',   icon: '◎', to: '/app/watchlist' },
  { id: 'settings',  label: 'Settings',    icon: '⚙', to: '/app/settings'  },
]

function handleLogout() {
  void router.push('/login')
}
</script>

<style scoped>
.app-shell {
  display: flex;
  height: 100vh;
  background: var(--tp-bg);
  font-family: var(--tp-font);
  color: var(--tp-text);
  overflow: hidden;
}

/* ── Sidebar ── */
.sidebar {
  width: 220px;
  background: var(--tp-surface);
  flex-shrink: 0;
  box-shadow: inset -1px 0 0 var(--tp-border);
  display: flex;
  flex-direction: column;
  height: 100%;
}

.sidebar__logo {
  padding: 18px 20px 16px;
  box-shadow: inset 0 -1px 0 var(--tp-border);
  display: flex;
  align-items: center;
  gap: 10px;
}

.sidebar__logo-mark {
  width: 26px;
  height: 26px;
  background: var(--tp-text);
  border-radius: 5px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.sidebar__brand {
  font-size: 13px;
  font-weight: 600;
  letter-spacing: -0.4px;
  color: var(--tp-text);
  line-height: 1.1;
}

.sidebar__brand-sub {
  font-size: 10px;
  color: var(--tp-muted);
  font-family: var(--tp-mono);
  letter-spacing: 0.3px;
}

.sidebar__nav {
  flex: 1;
  padding: 8px;
}

.sidebar__nav-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 7px 12px;
  border-radius: 6px;
  cursor: pointer;
  margin-bottom: 1px;
  color: var(--tp-muted);
  font-size: 13px;
  font-weight: 400;
  text-decoration: none;
  transition: background 0.1s, color 0.1s;
}

.sidebar__nav-item:hover {
  color: var(--tp-sec);
  background: rgba(255, 255, 255, 0.03);
}

.sidebar__nav-item--active {
  background: var(--tp-surf2) !important;
  color: var(--tp-text) !important;
  font-weight: 500;
  box-shadow: var(--tp-sb);
}

.sidebar__nav-item-inner {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sidebar__nav-icon {
  font-size: 13px;
  opacity: 0.7;
}

.sidebar__badge {
  background: var(--tp-crit-bg);
  color: var(--tp-crit);
  font-size: 10px;
  padding: 1px 7px;
  border-radius: 9999px;
  font-weight: 500;
  box-shadow: var(--tp-crit-ring) 0 0 0 1px;
}

.sidebar__stack {
  padding: 12px 20px;
  box-shadow: inset 0 1px 0 var(--tp-border);
}

.sidebar__stack-empty {
  font-family: var(--tp-mono);
  font-size: 11px;
  color: var(--tp-dimmer);
}

.sidebar__stack-more {
  font-family: var(--tp-mono);
  font-size: 10px;
  color: var(--tp-dimmer);
  padding-top: 2px;
}

.sidebar__stack-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 3px 0;
  font-size: 12px;
  color: var(--tp-sec);
}

.sidebar__stack-dot {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: var(--tp-dimmer);
  display: inline-block;
  flex-shrink: 0;
}

.sidebar__stack-name {
  font-family: var(--tp-mono);
  font-size: 11px;
  letter-spacing: 0.2px;
}

.sidebar__user {
  padding: 10px 16px;
  box-shadow: inset 0 1px 0 var(--tp-border);
  display: flex;
  align-items: center;
  gap: 8px;
}

.sidebar__avatar {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: var(--tp-surf3);
  box-shadow: var(--tp-sb);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  color: var(--tp-sec);
  font-weight: 500;
  flex-shrink: 0;
}

.sidebar__user-info {
  flex: 1;
  min-width: 0;
}

.sidebar__user-name {
  font-size: 12px;
  color: var(--tp-text);
  letter-spacing: -0.1px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sidebar__user-email {
  font-size: 10px;
  color: var(--tp-muted);
  font-family: var(--tp-mono);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sidebar__logout {
  cursor: pointer;
  color: var(--tp-muted);
  font-size: 10px;
  font-family: var(--tp-mono);
  background: var(--tp-surf2);
  border: none;
  border-radius: 4px;
  padding: 3px 7px;
  flex-shrink: 0;
  transition: color 0.1s, background 0.1s;
}

.sidebar__logout:hover {
  color: var(--tp-text);
  background: var(--tp-surf3);
}

/* ── Main ── */
.app-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

.topbar {
  height: 52px;
  flex-shrink: 0;
  padding: 0 28px;
  box-shadow: inset 0 -1px 0 var(--tp-border);
  display: flex;
  align-items: center;
  gap: 16px;
  background: var(--tp-bg);
}

.topbar__title {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.topbar__title-main {
  font-size: 17px;
  font-weight: 600;
  letter-spacing: -0.8px;
  color: var(--tp-text);
}

.topbar__title-sub {
  font-size: 17px;
  font-weight: 400;
  letter-spacing: -0.8px;
  color: var(--tp-muted);
}

.topbar__actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.app-content {
  flex: 1;
  overflow: auto;
}

/* ── Hamburger (hidden on desktop) ── */
.topbar__hamburger {
  display: none;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
  width: 32px;
  height: 32px;
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 4px;
  flex-shrink: 0;
}

.topbar__hamburger span {
  display: block;
  height: 1.5px;
  background: var(--tp-muted);
  border-radius: 2px;
  transition: background 0.1s;
}

.topbar__hamburger:hover span { background: var(--tp-text); }

/* ── Mobile ── */
@media (max-width: 768px) {
  .topbar__hamburger { display: flex; }

  .sidebar {
    position: fixed;
    left: 0;
    top: 0;
    height: 100%;
    z-index: 200;
    transform: translateX(-100%);
    transition: transform 0.22s ease;
    box-shadow: none;
  }

  .sidebar--open {
    transform: translateX(0);
    box-shadow: 4px 0 24px rgba(0, 0, 0, 0.4);
  }

  .sidebar-backdrop {
    position: fixed;
    inset: 0;
    z-index: 199;
    background: rgba(0, 0, 0, 0.5);
  }

  .topbar {
    padding: 0 16px;
  }

  .topbar__title-sub { display: none; }
}
</style>
