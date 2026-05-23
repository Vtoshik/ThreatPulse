<template>
  <IntroSequence v-if="showIntro" @done="showIntro = false" />
  <router-view />
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import IntroSequence from 'src/components/IntroSequence.vue'
import { useAuthStore } from 'src/stores/auth'

const authStore = useAuthStore()

const SEEN_KEY = 'tp_intro_seen'

function shouldShowIntro(): boolean {
  if (sessionStorage.getItem(SEEN_KEY)) return false
  const path = window.location.hash.replace(/^#/, '') || '/'
  return path === '/' || path.startsWith('/login') || path.startsWith('/register')
}

const showIntro = ref(shouldShowIntro())
if (showIntro.value) sessionStorage.setItem(SEEN_KEY, '1')

onMounted(() => { void authStore.restoreSession() })
</script>
