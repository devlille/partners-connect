# server Development Guidelines

Auto-generated from all feature plans. Last updated: 2026-01-10

## Active Technologies
- Kotlin 1.9.x, JVM 21 (Amazon Corretto) + Ktor 2.x, Exposed 0.41+, kotlinx.serialization, Koin (017-override-partnership-price)
- PostgreSQL (prod) / H2 in-memory (tests) via Exposed ORM (017-override-partnership-price)
- Kotlin 1.9.x / JVM 21 (Amazon Corretto) + Ktor 2.x, Exposed ORM 0.41+, Koin, kotlinx.serialization (018-filter-declined-partnerships)
- PostgreSQL (prod) / H2 in-memory (tests) — **no schema changes required** (018-filter-declined-partnerships)
- Kotlin 2.1.21, JVM 21 (Amazon Corretto) + Ktor 3.2.0, Exposed 1.0.0-beta-2, kotlinx-coroutines 1.9.0, Koin 4.1.0, Slack API client 1.45.3 (001-morning-organizer-digest)
- PostgreSQL (production), H2 in-memory (tests) — no new tables or migrations required (001-morning-organizer-digest)

- Kotlin 1.9.x with JVM 21 (Amazon Corretto) + Ktor 2.x, Exposed 0.41+, kotlinx.serialization, Koin DI (016-partnership-email-history)

## Project Structure

```text
src/
tests/
```

## Commands

# Add commands for Kotlin 1.9.x with JVM 21 (Amazon Corretto)

## Code Style

Kotlin 1.9.x with JVM 21 (Amazon Corretto): Follow standard conventions

## Recent Changes
- 019-standalone-communication: Added Kotlin 1.9.x, JVM 21 (Amazon Corretto) + Ktor 2.x, Exposed 0.41+, kotlinx.serialization, Koin
- 001-morning-organizer-digest: Added Kotlin 2.1.21, JVM 21 (Amazon Corretto) + Ktor 3.2.0, Exposed 1.0.0-beta-2, kotlinx-coroutines 1.9.0, Koin 4.1.0, Slack API client 1.45.3
- 018-filter-declined-partnerships: Added Kotlin 1.9.x / JVM 21 (Amazon Corretto) + Ktor 2.x, Exposed ORM 0.41+, Koin, kotlinx.serialization


<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->
