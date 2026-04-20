# ThreatPulse — developer convenience targets
# Usage: make <target>
# Requires: Docker running (for integration tests), Java 21, Maven wrapper

BACKEND_DIR = backend

# ── Tests ────────────────────────────────────────────────────────────────────

# Run only unit tests (fast, no Docker needed)
test-unit:
	cd $(BACKEND_DIR) && ./mvnw test -pl . \
		-Dtest="JwtServiceTest,AuthServiceTest" \
		-DfailIfNoTests=false

# Run only integration tests (needs Docker for Testcontainers)
test-integration:
	cd $(BACKEND_DIR) && ./mvnw test -pl . \
		-Dtest="AuthControllerIntegrationTest" \
		-DfailIfNoTests=false

# Run all tests
test:
	cd $(BACKEND_DIR) && ./mvnw test

# Run tests and generate JaCoCo coverage report (opens in browser after)
test-coverage:
	cd $(BACKEND_DIR) && ./mvnw verify
	@echo "Coverage report: backend/target/site/jacoco/index.html"

# ── Build ─────────────────────────────────────────────────────────────────────

# Compile only (no tests)
build:
	cd $(BACKEND_DIR) && ./mvnw compile

# Package into JAR (runs tests too)
package:
	cd $(BACKEND_DIR) && ./mvnw package

# ── Local dev ─────────────────────────────────────────────────────────────────

# Start infrastructure (PostgreSQL, Kafka, Redis, Kafka UI)
infra-up:
	docker compose up -d

# Stop infrastructure
infra-down:
	docker compose down

# Start backend in dev mode (needs infra running first)
dev:
	cd $(BACKEND_DIR) && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

.PHONY: test-unit test-integration test test-coverage build package infra-up infra-down dev