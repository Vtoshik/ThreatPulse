import type { Severity } from 'src/types/threat'

const COLOR_MAP: Record<Severity, string> = {
  CRITICAL: 'var(--tp-crit)',
  HIGH:     'var(--tp-high)',
  MEDIUM:   'var(--tp-med)',
  LOW:      'var(--tp-low)',
  INFO:     'var(--tp-info)',
}

const BG_MAP: Record<Severity, string> = {
  CRITICAL: 'var(--tp-crit-bg)',
  HIGH:     'var(--tp-high-bg)',
  MEDIUM:   'var(--tp-med-bg)',
  LOW:      'var(--tp-low-bg)',
  INFO:     'var(--tp-info-bg)',
}

const RING_MAP: Record<Severity, string> = {
  CRITICAL: 'var(--tp-crit-ring)',
  HIGH:     'var(--tp-high-ring)',
  MEDIUM:   'var(--tp-med-ring)',
  LOW:      'var(--tp-low-ring)',
  INFO:     'var(--tp-info-ring)',
}

export function useSeverity() {
  const sevColor = (s: string): string => COLOR_MAP[s as Severity] ?? COLOR_MAP.INFO
  const sevBg    = (s: string): string => BG_MAP[s as Severity]    ?? BG_MAP.INFO
  const sevRing  = (s: string): string => RING_MAP[s as Severity]  ?? RING_MAP.INFO

  const sevCardShadow = (s: string): string =>
    `${sevColor(s)} 0 0 0 1px, rgba(0,0,0,0.4) 0 4px 16px`

  return { sevColor, sevBg, sevRing, sevCardShadow }
}
