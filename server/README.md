# Server

A robust backend for managing developer event partnerships, sponsorships, billing, and integrations. Built with Kotlin, 
Ktor, and PostgreSQL, it provides a secure, modular REST API for partner and event management.

## Table of Contents

- [Technical Stack](#technical-stack)
- [Architecture Overview](#architecture-overview)
- [Domain Modules](#domain-modules)
- [Installation & Setup](#installation--setup)
- [Build & Test](#build--test)
- [Environment Variables & Secrets](#environment-variables--secrets)
- [Deployment](#deployment)
- [Coding Conventions](#coding-conventions)

## Technical Stack

- **Language:** Kotlin (JVM 21)
- **Framework:** Ktor (REST API)
- **ORM:** Exposed (SQL DSL)
- **Database:** PostgreSQL (H2 for tests)
- **Build Tool:** Gradle 8.13+
- **Testing:** JUnit, Ktor Test, H2 in-memory DB
- **Linting:** ktlint, detekt
- **Integrations:** Google Cloud Storage, Slack, Mailjet, Qonto, BilletWeb

## Architecture Overview

The server is organized as a modular, layered Kotlin application:

- **Entry Point:** `App.kt` (configures Ktor, DI, routing)
- **Domain Modules:** Each business domain (auth, billing, companies, etc.) is a separate package under `application/src/main/kotlin/fr/devlille/partners/connect/`.
- **Database:** Exposed ORM, schema in `*Table.kt` files, migrations at startup.
- **Configuration:** Environment variables, `docker-compose.yml`, and Gradle properties.

## Domain Modules

- **auth/**: Google OAuth authentication and user session management.
- **billing/**: Invoice and quote generation, Qonto integration.
- **companies/**: Company registration and management.
- **events/**: Event creation, management, and ticketing.
- **integrations/**: External service connectors (Slack, Mailjet, BilletWeb, Google Cloud Storage).
- **internal/**: Shared infrastructure (logging, error handling, config).
- **notifications/**: Slack notifications and event triggers.
- **organisations/**: Organisation and team management.
- **partnership/**: Core partnership workflows and agreements.
- **sponsoring/**: Sponsorship package management.
- **tickets/**: Ticket generation and BilletWeb integration.
- **users/**: User permissions and roles.

## Installation & Setup

### Prerequisites

- Java 21
- Gradle 8.13+ (use included wrapper)
- PostgreSQL (for full functionality)
- (Optional) Docker & Docker Compose

### Local Setup

1. **Clone the repository:**
   ```bash
   git clone https://github.com/your-org/partners-connect.git
   cd partners-connect/server
   ```

2. **Configure environment variables:**
   - Copy `.env.example` to `.env` and fill in required secrets (see [Environment Variables & Secrets](#environment-variables--secrets)).

3. **Start PostgreSQL (if not using Docker):**
   - Ensure a local PostgreSQL instance is running and matches your `.env` config.

4. **Install dependencies and build:**
   ```bash
   ./gradlew build --no-daemon
   ```

## Build & Test

- **Run all checks (lint, test, build):**
  ```bash
  ./gradlew check --no-daemon
  ```

- **Run tests only:**
  ```bash
  ./gradlew test --no-daemon
  ```

- **Lint and static analysis:**
  ```bash
  ./gradlew ktlintCheck detekt --no-daemon
  ```

- **Auto-format code:**
  ```bash
  ./gradlew ktlintFormat --no-daemon
  ```

- **Run the application locally:**
  ```bash
  ./gradlew run --no-daemon
  ```

- **Build artifacts:** Output in `application/build/libs/`

---

## Environment Variables & Secrets

Set these in your `.env` file or as environment variables:

| Variable                | Purpose                                 | Example/Notes                        |
|-------------------------|-----------------------------------------|--------------------------------------|
| `EXPOSED_DB_URL`        | JDBC URL for PostgreSQL                 | `jdbc:postgresql://localhost/db`     |
| `EXPOSED_DB_USER`       | DB username                             |                                      |
| `EXPOSED_DB_PASSWORD`   | DB password                             |                                      |
| `GOOGLE_CLIENT_ID`      | Google OAuth client ID                  | For user authentication              |
| `GOOGLE_CLIENT_SECRET`  | Google OAuth client secret              |                                      |
| `CRYPTO_SECRET`         | Encryption key for sensitive data       | 32+ chars, base64 recommended        |
| `SLACK_WEBHOOK_URL`     | Slack notifications                     | Optional, for event alerts           |
| `MAILJET_API_KEY`       | Mailjet integration                     | Optional, for transactional emails   |
| `MAILJET_SECRET_KEY`    | Mailjet secret                          |                                      |
| `QONTO_API_KEY`         | Qonto integration                       | Optional, for billing                |
| `BILLETWEB_API_KEY`     | BilletWeb ticketing integration         | Optional, for event tickets          |
| `GCS_BUCKET`            | Google Cloud Storage bucket name        | For file uploads                     |
| `GCS_CREDENTIALS_JSON`  | GCS service account credentials (JSON)  |                                      |

> **Note:** Not all integrations are required for local development. The app will run with minimal config, but some features will be disabled.

## Deployment

- **Docker Compose:** Use `docker-compose.yml` for local stack (app + PostgreSQL).
- **Production:** Use the provided `Dockerfile` for multi-stage builds.
- **Database migrations:** Handled automatically at startup via Exposed.

## Coding Conventions

- **Formatting:** Enforced by ktlint (`./gradlew ktlintCheck`).
- **Static Analysis:** Run detekt (`./gradlew detekt`).
- **Testing:** All new features require tests in `application/src/test/kotlin/`.
- **Commit Hygiene:** Run all checks before pushing.
- **Secrets:** Never commit secrets to the repository.

## Contributing

1. Fork and clone the repo.
2. Create a feature branch.
3. Follow all coding conventions and run all checks.
4. Submit a pull request with a clear description.
