import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('pages/LandingPage.vue'),
  },
  {
    path: '/login',
    component: () => import('pages/LoginPage.vue'),
  },
  {
    path: '/register',
    component: () => import('pages/RegisterPage.vue'),
  },
  {
    path: '/app',
    component: () => import('layouts/AppLayout.vue'),
    redirect: '/app/dashboard',
    children: [
      {
        path: 'dashboard',
        component: () => import('pages/DashboardPage.vue'),
        meta: { title: 'Dashboard' },
      },
      {
        path: 'feed',
        component: () => import('pages/FeedPage.vue'),
        meta: { title: 'Threat Feed' },
      },
      {
        path: 'threats/:id',
        component: () => import('pages/ThreatDetailPage.vue'),
        meta: { title: 'Threat Detail' },
      },
      {
        path: 'search',
        component: () => import('pages/SearchPage.vue'),
        meta: { title: 'Semantic Search' },
      },
      {
        path: 'alerts',
        component: () => import('pages/AlertsPage.vue'),
        meta: { title: 'Alerts' },
      },
      {
        path: 'alerts/new',
        component: () => import('pages/AlertRulePage.vue'),
        meta: { title: 'New Alert Rule' },
      },
      {
        path: 'alerts/:id/edit',
        component: () => import('pages/AlertRulePage.vue'),
        meta: { title: 'Edit Alert Rule' },
      },
      {
        path: 'watchlist',
        component: () => import('pages/WatchlistPage.vue'),
        meta: { title: 'Watchlist' },
      },
      {
        path: 'settings',
        component: () => import('pages/SettingsPage.vue'),
        meta: { title: 'Settings' },
      },
    ],
  },
  {
    path: '/:catchAll(.*)*',
    component: () => import('pages/ErrorNotFound.vue'),
  },
]

export default routes
