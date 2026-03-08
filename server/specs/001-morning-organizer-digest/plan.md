# Implementation Plan: Morning Organiser Daily Digest

**Branch**: `001-morning-organizer-digest` | **Date**: 2026-03-05 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/001-morning-organizer-digest/spec.md`

## Summary

An internal HTTP endpoint (`POST /orgs/{orgSlug}/events/{eventId}/jobs/digest`) is called daily at 08:00 UTC by an external scheduler, once per event. It queries the existing database tables for three categories of actionable partnerships — agreement-ready, quote-ready, and social-media-due today — then sends one consolidated Slack message if there is at least one actionable item. All data fields required for the readiness checks already exist in the current schema; no database migration is needed. The feature introduces a new `digest/` domain module and registers a trigger route in `App.kt`.

## Technical Context

**Language/Version**: Kotlin 2.1.21, JVM 21 (Amazon Corretto)  
**Primary Dependencies**: Ktor 3.2.0, Exposed 1.0.0-beta-2, kotlinx-coroutines 1.9.0, Koin 4.1.0, Slack API client 1.45.3  
**Storage**: PostgreSQL (production), H2 in-memory (tests) — no new tables or migrations required  
**Testing**: Ktor `testApplication` + `moduleSharedDb` pattern, H2 in-memory, MockK for Slack gateway  
**Target Platform**: Linux server (JVM), Ktor Netty embedded server  
**Project Type**: Single Kotlin server project under `application/`  
**Performance Goals**: Digest completes for a single event within 5 seconds; all events under 30 seconds  
**Constraints**: No new external libraries; no new database tables; one new job-trigger HTTP endpoint (not in public OpenAPI spec)  
**Scale/Scope**: Dozens of events per organisation; up to a few hundred partnerships per event

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design.*

| Gate | Status | Notes |
|------|--------|-------|
| ktlint / detekt zero violations | ✅ PASS | Enforced by CI; applies to all new files |
| 80% test coverage for new code | ✅ PASS | Two integration test classes planned |
| Clean modular architecture — no circular dependencies | ✅ PASS | New `digest/` module has one-way deps on existing modules |
| Repository layer — no cross-repo dependencies | ✅ PASS | `DigestRepositoryExposed` queries via a single join; no other repo injected |
| Notifications in route layer, NOT repository | ✅ PASS | Route handler owns orchestration; digest repo returns plain data; notification sent in route |
| Trigger endpoint documented in OpenAPI | ✅ PASS | `POST /orgs/{orgSlug}/events/{eventId}/jobs/digest` added to `openapi.yaml` as a public endpoint with `security: - {}` (no bearer auth required); constitution §IV requires documentation for all endpoints |
| No new external service or secret | ✅ PASS | Uses existing Slack integration + `SlackNotificationGateway`; no new env vars |
| `NotificationRepository` interface unchanged | ✅ PASS | Feature adds `NotificationVariables.MorningDigest` subclass to the sealed interface; `NotificationRepository` method signatures are untouched |

**Pre-design result**: All gates pass. No violations to justify.

## Project Structure

### Documentation (this feature)

```text
specs/001-morning-organizer-digest/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
└── contracts/
    └── slack-message-format.md   # Phase 1 output
```

### Source Code

```text
application/src/main/kotlin/fr/devlille/partners/connect/
├── App.kt
│   ├── domain/
│   │   ├── DigestEntry.kt                       # NEW: data class — (companyName, partnershipLink)
│   │   ├── EventDigest.kt                       # NEW: data class — three category lists + event
│   │   └── DigestRepository.kt                  # NEW: interface — queryDigest(eventId, today): EventDigest
│   ├── application/
│   │   └── DigestRepositoryExposed.kt           # NEW: Exposed JOIN query across existing tables
│   └── infrastructure/
│       ├── api/
│       │   └── DigestRoutes.kt                  # NEW: POST /orgs/{orgSlug}/events/{eventId}/jobs/digest
│       └── bindings/
│           └── DigestModule.kt                  # NEW: Koin DI bindings — DigestRepository
└── notifications/
    └── domain/
        └── NotificationVariables.kt             # MODIFY: add MorningDigest subclass

application/src/main/resources/notifications/slack/
└── digest/
    ├── fr.md                                    # NEW: French Slack digest template
    └── en.md                                    # NEW: English Slack digest template

application/src/test/kotlin/fr/devlille/partners/connect/
└── digest/
    ├── DigestJobRoutePostTest.kt                # NEW: contract test — HTTP schema validation, 204/401 status codes
    └── DigestJobRoutesTest.kt                   # NEW: integration test — end-to-end readiness logic + Slack dispatch
```

**Structure Decision**: Single project layout. The `digest/` module follows the established domain module pattern (domain/application/infrastructure) and sits alongside the 14 existing modules. All modifications to `notifications/` are additive (no existing method signatures changed). The trigger endpoint follows the existing org/event URL structure (`/orgs/{orgSlug}/events/{eventId}/...`) but is a job action, not a user-facing resource, and is not listed in `openapi.yaml`.

## Complexity Tracking

No constitution violations.

---

## Phase 0: Research Summary

See [research.md](research.md) for full analysis.

| Decision | Choice |
|----------|--------|
| Scheduling mechanism | External HTTP trigger: `POST /orgs/{orgSlug}/events/{eventId}/jobs/digest`; called once per event; no in-process scheduler |
| Endpoint security | No authentication on the trigger endpoint; network-level restriction is the caller's responsibility |
| Digest notification architecture | `NotificationVariables.MorningDigest` added to existing sealed interface; route calls `notificationRepository.sendMessageFromMessaging(variables)`; no new repository |
| Slack message language | Language from `Accept-Language` request header (default `fr`); no DB or event-config lookup |

---

## Phase 1: Design Summary

See [data-model.md](data-model.md), [contracts/slack-message-format.md](contracts/slack-message-format.md), and [quickstart.md](quickstart.md).

### Post-design Constitution Check

| Gate | Status | Notes |
|------|--------|-------|
| Clean modular architecture | ✅ PASS | `digest/` has one-way dependency chain |
| Repository layer isolation | ✅ PASS | `DigestRepositoryExposed` is pure data access — no notification calls |
| No new tables or migrations | ✅ PASS | All required fields confirmed in existing schema |
| 80% test coverage | ✅ PASS | Two integration test classes cover repository queries and job dispatch |
| OpenAPI updated for trigger endpoint | ✅ PASS | `POST /orgs/{orgSlug}/events/{eventId}/jobs/digest` documented in `openapi.yaml` with `security: - {}` |
