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
  timeline?: ThreatTimeline[]
}
