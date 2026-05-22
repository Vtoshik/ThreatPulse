<template>
  <IntroSequence v-if="showIntro" @done="showIntro = false" />
  <router-view />
</template>

<script setup lang="ts">
import { ref } from 'vue'
import IntroSequence from 'src/components/IntroSequence.vue'

// The intro plays once per browser session, and only when the visitor lands
// on a public entry route. Deep links into the app skip it.
const SEEN_KEY = 'tp_intro_seen'

function shouldShowIntro(): boolean {
  if (sessionStorage.getItem(SEEN_KEY)) return false
  const path = window.location.hash.replace(/^#/, '') || '/'
  return path === '/' || path.startsWith('/login') || path.startsWith('/register')
}

const showIntro = ref(shouldShowIntro())
if (showIntro.value) sessionStorage.setItem(SEEN_KEY, '1')
</script>
