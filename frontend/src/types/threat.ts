export type Severity = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW' | 'INFO'
export type ThreatCategory = 'RCE' | 'XSS' | 'SQLi' | 'DATA_BREACH' | 'OTHER'

export interface ThreatTimeline {
  t: string
  e: string
}

export interface Threat {
  id: string
  cve: string
  title: string
  severity: Severity
  category?: ThreatCategory
  tech: string
  age: string
  score: number
  published: string
  summary?: string
  affected?: string[]
  patch?: string
  vector?: string
  complexity?: string
  auth?: string
  source?: string
  sourceUrl?: string
  sourceName?: string
  publishedAt?: string
  timeline?: ThreatTimeline[]
}

export interface ApiThreat {
  id: number
  externalId: string
  title: string
  aiSummary?: string | null
  severity: Severity
  threatCategory?: ThreatCategory | null
  sourceName: string
  sourceUrl: string
  publishedAt: string
  affectedTechnologies: string[]
}

export interface ApiThreatPage {
  threats: ApiThreat[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}
