import type { Severity } from './threat'

export type AlertChannel = 'EMAIL' | 'PUSH'

export interface AlertRule {
  id: number
  name: string
  severity: Severity
  tech: string[]
  active: boolean
  triggered: number
}

export interface AlertHistoryItem {
  id: number
  threat: string
  cve: string
  severity: Severity
  sentAt: string
  channel: AlertChannel
}
