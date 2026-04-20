# ThreatPulse

**AI-powered cybersecurity intelligence platform** that monitors public vulnerability databases and security news, analyzes threats using large language models, and delivers personalized alerts based on your technology stack.

---

## The Problem

Security teams and developers struggle to keep up with the constant flow of CVEs, vulnerability disclosures, and security news. Relevant threats get buried in noise, and by the time a team learns about a critical vulnerability in a library they use — it may be too late.

## What ThreatPulse Does

ThreatPulse continuously collects threat data from multiple public sources, uses AI to extract structured insights (severity, affected technologies, recommended actions), and matches them against each user's watched tech stack — sending alerts only for what actually matters to them.

**Key capabilities:**
- Automatic collection from NVD/NIST CVE database, security RSS feeds, and news sources
- AI-generated summaries and severity classification using Llama 3.3 70B (via Groq)
- Semantic search across threat history using vector embeddings
- Real-time dashboard updates via WebSocket
- Personalized email alerts filtered by technology stack and severity threshold

---

## Architecture

Event-driven modular monolith built on Spring Boot 3 and Apache Kafka.

```
[Data Sources]
NVD/NIST API ─────┐
Security RSS ──────┼──► Collector ──► Kafka ──► Analyzer ──► PostgreSQL
NewsAPI.org ───────┘                     │
                                         ├──► Feed API  ──► Vue Dashboard
                                         ├──► Alerts    ──► Email (Resend)
                                         └──► WebSocket ──► Real-time UI
```

Collectors run on a schedule, publish raw data to Kafka, and the analyzer processes them asynchronously — keeping collection and AI inference fully decoupled.

---

## Tech Stack

**Backend:** Java 21, Spring Boot 3.3, Spring Kafka, Spring Security (JWT), Flyway
**AI:** Groq API (Llama 3.3 70B), Hugging Face Inference API (embeddings)
**Data:** PostgreSQL 16 + pgvector, Apache Kafka, Redis
**Frontend:** Vue 3, TypeScript, Quasar, Pinia, WebSocket/STOMP
**DevOps:** Docker Compose, GitHub Actions, Railway.app, Grafana Cloud

---

## Local Development

```bash
# Start infrastructure
docker compose up -d

# Backend
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Frontend
cd frontend && npm install && npm run dev
```

See environment variable documentation in `backend/src/main/resources/application-dev.yml.example` after cloning.
