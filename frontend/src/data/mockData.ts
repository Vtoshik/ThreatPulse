import type { Threat } from 'src/types/threat'
import type { AlertRule, AlertHistoryItem } from 'src/types/alert'

export const THREATS: Threat[] = [
  {
    id: 'a3f9c12', cve: 'CVE-2025-1234',
    title: 'Spring Boot RCE via deserialization',
    severity: 'CRITICAL', tech: 'spring-boot', age: '2m', score: 9.8,
    published: 'Apr 20, 2025',
    summary: 'A critical remote code execution vulnerability exists in Spring Boot versions prior to 3.3.5. The vulnerability stems from unsafe Java deserialization of untrusted data via the actuator endpoint when certain conditions are met. An unauthenticated remote attacker can exploit this to execute arbitrary code on the host.',
    affected: ['spring-boot < 3.3.5', 'spring-core < 6.1.14', 'tomcat < 10.1.20'],
    patch: 'Upgrade to spring-boot 3.3.5. If immediate upgrade is not possible, disable actuator endpoints via management.endpoints.enabled-by-default=false',
    vector: 'NETWORK', complexity: 'LOW', auth: 'NONE', source: 'NVD/NIST',
    timeline: [
      { t: '09:12', e: 'CVE published to NVD' },
      { t: '09:13', e: 'Collected by ThreatPulse' },
      { t: '09:14', e: 'AI analysis complete' },
      { t: '09:14', e: 'Email alert sent' },
    ],
  },
  {
    id: 'b7e2d45', cve: 'CVE-2025-0987',
    title: 'PostgreSQL privilege escalation in pg_dump',
    severity: 'HIGH', tech: 'postgresql', age: '18m', score: 8.1,
    published: 'Apr 20, 2025',
    summary: "PostgreSQL's pg_dump utility allows a low-privileged user to escalate to superuser privileges through a crafted dump file. Affects PostgreSQL 14–16 when pg_dump is run with --superuser flag.",
    affected: ['postgresql 14.x < 14.12', 'postgresql 15.x < 15.7', 'postgresql 16.x < 16.3'],
    patch: 'Upgrade to PostgreSQL 14.12, 15.7, or 16.3. Restrict pg_dump access to trusted users only.',
    vector: 'LOCAL', complexity: 'LOW', auth: 'LOW', source: 'NVD/NIST',
    timeline: [
      { t: '07:44', e: 'CVE published' },
      { t: '07:45', e: 'Collected' },
      { t: '07:46', e: 'AI analysis' },
      { t: '07:46', e: 'Alert sent' },
    ],
  },
  {
    id: 'c1a8f67', cve: 'CVE-2025-0771',
    title: 'Apache Kafka SASL authentication bypass',
    severity: 'HIGH', tech: 'kafka', age: '1h', score: 7.6,
    published: 'Apr 20, 2025',
    summary: "A flaw in Apache Kafka's SASL/PLAIN authentication mechanism allows an attacker to bypass authentication by sending a malformed SASL handshake packet, gaining broker-level access.",
    affected: ['kafka 3.0–3.6 < 3.6.2', 'kafka 3.7.x < 3.7.1'],
    patch: 'Upgrade to Kafka 3.6.2 or 3.7.1. As a workaround, switch to SASL/SCRAM-SHA-512.',
    vector: 'NETWORK', complexity: 'LOW', auth: 'NONE', source: 'NVD/NIST',
    timeline: [
      { t: '08:55', e: 'CVE published' },
      { t: '08:56', e: 'Collected' },
      { t: '08:57', e: 'AI analysis' },
      { t: '08:57', e: 'Alert sent' },
    ],
  },
  {
    id: 'd4b3e89', cve: 'CVE-2025-0502',
    title: 'Redis SSRF via CONFIG SET directive',
    severity: 'MEDIUM', tech: 'redis', age: '3h', score: 5.9,
    published: 'Apr 19, 2025',
    summary: "Redis's CONFIG SET command can be abused to trigger Server-Side Request Forgery (SSRF) by redirecting the replication endpoint to an internal service. Requires authenticated access.",
    affected: ['redis < 7.2.4', 'redis 7.0.x < 7.0.15'],
    patch: 'Upgrade to Redis 7.2.4. Disable CONFIG command for untrusted clients: rename-command CONFIG ""',
    vector: 'NETWORK', complexity: 'HIGH', auth: 'LOW', source: 'NVD/NIST',
    timeline: [
      { t: '06:10', e: 'CVE published' },
      { t: '06:12', e: 'Collected' },
      { t: '06:13', e: 'AI analysis' },
      { t: '06:13', e: 'Alert queued' },
    ],
  },
  {
    id: 'e9c6a01', cve: 'CVE-2025-0341',
    title: 'Docker container escape via overlay2 filesystem',
    severity: 'CRITICAL', tech: 'docker', age: '5h', score: 9.1,
    published: 'Apr 19, 2025',
    summary: "A container escape vulnerability in Docker's overlay2 storage driver allows a container process to write to arbitrary paths on the host filesystem. Exploitable from inside a container with write access.",
    affected: ['docker engine < 26.1.3', 'docker desktop < 4.30.0', 'containerd < 1.7.17'],
    patch: 'Upgrade Docker Engine to 26.1.3+ and containerd to 1.7.17+. Avoid running containers as root.',
    vector: 'LOCAL', complexity: 'LOW', auth: 'LOW', source: 'NVD/NIST',
    timeline: [
      { t: '04:30', e: 'CVE published' },
      { t: '04:32', e: 'Collected' },
      { t: '04:33', e: 'AI analysis' },
      { t: '04:33', e: 'Alert sent' },
    ],
  },
]

export const ALERT_RULES: AlertRule[] = [
  { id: 1, name: 'Critical in my stack', severity: 'CRITICAL', tech: ['spring-boot', 'kafka', 'postgresql', 'redis', 'docker'], active: true,  triggered: 3  },
  { id: 2, name: 'High severity — all',  severity: 'HIGH',     tech: [],                                                         active: true,  triggered: 12 },
  { id: 3, name: 'Docker CVEs',          severity: 'MEDIUM',   tech: ['docker'],                                                 active: false, triggered: 0  },
]

export const ALERT_HISTORY: AlertHistoryItem[] = [
  { id: 1, threat: 'Spring Boot RCE via deserialization',  cve: 'CVE-2025-1234', severity: 'CRITICAL', sentAt: '09:14 today',   channel: 'EMAIL' },
  { id: 2, threat: 'PostgreSQL privilege escalation',      cve: 'CVE-2025-0987', severity: 'HIGH',     sentAt: '07:46 today',   channel: 'EMAIL' },
  { id: 3, threat: 'Apache Kafka SASL auth bypass',        cve: 'CVE-2025-0771', severity: 'HIGH',     sentAt: '08:57 today',   channel: 'EMAIL' },
  { id: 4, threat: 'Docker container escape',              cve: 'CVE-2025-0341', severity: 'CRITICAL', sentAt: '04:33 today',   channel: 'EMAIL' },
  { id: 5, threat: 'Redis SSRF via CONFIG SET',            cve: 'CVE-2024-9981', severity: 'MEDIUM',   sentAt: 'Apr 19, 17:02', channel: 'EMAIL' },
]

export const USER_STACK = ['spring-boot', 'postgresql', 'kafka', 'redis', 'docker']
