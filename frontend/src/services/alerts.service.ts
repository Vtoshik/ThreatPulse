import api from 'src/services/api'
import type { Severity } from 'src/types/threat'

export interface AlertRuleDto {
  id: number
  minSeverity: Severity
  technologiesFilter: string[]
  active: boolean
}

export interface AlertHistoryDto {
  id: number
  threatId: number
  threatTitle: string
  channel: string
  sentAt: string
}

export const alertsService = {
  getRules(): Promise<AlertRuleDto[]> {
    return api.get<AlertRuleDto[]>('/api/alerts/rules').then(r => r.data)
  },

  createRule(minSeverity: Severity, technologiesFilter: string[]): Promise<AlertRuleDto> {
    return api.post<AlertRuleDto>('/api/alerts/rules', { minSeverity, technologiesFilter }).then(r => r.data)
  },

  updateRule(id: number, minSeverity: Severity, technologiesFilter: string[], active?: boolean): Promise<AlertRuleDto> {
    return api.put<AlertRuleDto>(`/api/alerts/rules/${id}`, { minSeverity, technologiesFilter, active }).then(r => r.data)
  },

  toggleRule(rule: AlertRuleDto): Promise<AlertRuleDto> {
    return api.put<AlertRuleDto>(`/api/alerts/rules/${rule.id}`, {
      minSeverity: rule.minSeverity,
      technologiesFilter: rule.technologiesFilter,
      active: !rule.active,
    }).then(r => r.data)
  },

  deleteRule(id: number): Promise<boolean> {
    return api.delete<boolean>(`/api/alerts/rules/${id}`).then(r => r.data)
  },

  getHistory(): Promise<AlertHistoryDto[]> {
    return api.get<AlertHistoryDto[]>('/api/alerts/history').then(r => r.data)
  },
}