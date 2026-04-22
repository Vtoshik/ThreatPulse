<template>
  <span class="mono-label"><slot /></span>
</template>

<style scoped>
.mono-label {
  font-family: var(--tp-mono);
  font-size: 10px;
  text-transform: uppercase;
  letter-spacing: 0.8px;
  color: var(--tp-muted);
}
</style>
