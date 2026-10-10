import api from './api'
import type {
  ApiSemanticSearchResponse,
  ApiThreatStats,
  ApiThreat,
  ApiThreatPage,
  Severity,
  Threat,
} from 'src/types/threat'

const SEVERITY_SCORE: Record<Severity, number> = {
  CRITICAL: 9.8,
  HIGH: 8.1,
  MEDIUM: 5.9,
  LOW: 3.7,
  INFO: 1.0,
}

function formatPublishedAt(value: string): string {
  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return 'Unknown date'
  }

  return date.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  })
}

function formatAge(value: string): string {
  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return '—'
  }

  const diffMs = Date.now() - date.getTime()
  const diffMinutes = Math.max(0, Math.floor(diffMs / 60000))

  if (diffMinutes < 1) {
    return 'just now'
  }
  if (diffMinutes < 60) {
    return `${diffMinutes}m`
  }

  const diffHours = Math.floor(diffMinutes / 60)
  if (diffHours < 24) {
    return `${diffHours}h`
  }

  const diffDays = Math.floor(diffHours / 24)
  return `${diffDays}d`
}

function formatTechs(techs: string[]): string {
  if (!techs.length) {
    return '—'
  }

  if (techs.length <= 2) {
    return techs.join(', ')
  }

  return `${techs.slice(0, 2).join(', ')}, +${techs.length - 2}`
}

function threatScore(severity: Severity): number {
  return SEVERITY_SCORE[severity] ?? SEVERITY_SCORE.INFO
}

export function mapThreat(apiThreat: ApiThreat): Threat {
  const affectedTechnologies = apiThreat.affectedTechnologies ?? []

  return {
    id: String(apiThreat.id),
    cve: apiThreat.externalId,
    title: apiThreat.title,
    severity: apiThreat.severity,
    ...(apiThreat.threatCategory != null ? { category: apiThreat.threatCategory } : {}),
    tech: formatTechs(affectedTechnologies),
    age: formatAge(apiThreat.publishedAt),
    score: threatScore(apiThreat.severity),
    published: formatPublishedAt(apiThreat.publishedAt),
    ...(apiThreat.aiSummary != null ? { summary: apiThreat.aiSummary } : {}),
    ...(apiThreat.description != null ? { description: apiThreat.description } : {}),
    affected: affectedTechnologies,
    source: apiThreat.sourceName,
    sourceName: apiThreat.sourceName,
    sourceUrl: apiThreat.sourceUrl,
    publishedAt: apiThreat.publishedAt,
  }
}

function mapPage(apiPage: ApiThreatPage) {
  return {
    threats: apiPage.threats.map(mapThreat),
    page: apiPage.page,
    size: apiPage.size,
    totalElements: apiPage.totalElements,
    totalPages: apiPage.totalPages,
  }
}

export const threatService = {
  async getStats() {
    // The database counts, so the numbers stay right however many threats are stored
    const response = await api.get<ApiThreatStats>('/api/threats/stats')
    const stats = response.data

    return {
      critical: stats.critical,
      high: stats.high,
      medium: stats.medium,
      low: stats.low,
      todayCount: stats.last24h,
    }
  },

  async getRecentThreats(size = 5) {
    const response = await api.get<ApiThreatPage>('/api/threats', {
      params: { page: 0, size, sort: 'publishedAt,desc' },
    })

    return mapPage(response.data).threats
  },

  async getThreats(params: {
    page?: number
    size?: number
    severity?: string
    q?: string
    sort?: string
  }) {
    const cleanParams = {
      ...params,
      severity: params.severity === 'ALL' ? undefined : params.severity,
    }

    const response = await api.get<ApiThreatPage>('/api/threats', {
      params: cleanParams,
    })

    return mapPage(response.data)
  },

  async getThreatById(id: string | number) {
    const response = await api.get<ApiThreat>(`/api/threats/${id}`)
    return mapThreat(response.data)
  },

  async searchThreats(query: string, params?: { limit?: number; severity?: string }) {
    const response = await api.get<ApiSemanticSearchResponse>('/api/threats/search', {
      params: {
        q: query,
        limit: params?.limit ?? 20,
        severity: params?.severity && params.severity !== 'ALL' ? params.severity : undefined,
      },
    })

    return { threats: response.data.threats.map(mapThreat) }
  },
}
