# Implementation Plan: Option Partnerships

**Branch**: `021-option-partnerships` | **Date**: 2026-03-24 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/021-option-partnerships/spec.md`

## Summary

Enrich the existing single option detail endpoint (`GET /orgs/{orgSlug}/events/{eventSlug}/options/{optionId}`) to include a list of validated partnerships (as `PartnershipItem` objects) for that option. Partnership-option association is derived from the partnership's validated pack (via `validatedPack()`) containing the option (via `PackOptionsTable`). No new database tables or entities are needed.

## Technical Context

**Language/Version**: Kotlin 1.9.x / JVM 21 (Amazon Corretto)
**Primary Dependencies**: Ktor 2.x, Exposed ORM, kotlinx.serialization, Koin
**Storage**: PostgreSQL (H2 in-memory for tests)
**Testing**: JUnit 5 via Ktor `testApplication`, H2 shared database
**Target Platform**: Linux server (Docker)
**Project Type**: Single Ktor server application
**Performance Goals**: N/A — read-only endpoint enrichment, no new write paths
**Constraints**: Response must remain backward-compatible; only additive change
**Scale/Scope**: Single endpoint modification, ~5 files touched

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality Standards | PASS | ktlint/detekt compliance required; no new patterns introduced |
| II. Comprehensive Testing | PASS | Contract test for modified endpoint; factory reuse |
| III. Clean Modular Architecture | PASS | New domain model in sponsoring domain; repository stays decoupled |
| IV. API Consistency & UX | PASS | Reuses existing `PartnershipItem` format; additive response change |
| V. Performance & Observability | PASS | No new write paths; read query joins existing indexed tables |

No violations. Gate passes.

## Project Structure

### Documentation (this feature)

```text
specs/021-option-partnerships/
├── plan.md                # This file
├── research.md            # Phase 0 output
├── data-model.md          # Phase 1 output
├── quickstart.md          # Phase 1 output
├── contracts/             # Phase 1 output
│   └── option-detail-response.md
└── tasks.md               # Phase 2 output (/speckit.tasks command)
```

### Source Code (files to modify/create)

```text
application/src/main/kotlin/fr/devlille/partners/connect/
├── sponsoring/
│   ├── domain/
│   │   ├── OptionRepository.kt                          # Add new method signature
│   │   └── SponsoringOptionDetailWithPartners.kt           # New response wrapper
│   ├── application/
│   │   └── OptionRepositoryExposed.kt                   # Implement new method
│   └── infrastructure/
│       └── api/SponsoringRoutes.kt                      # Update route to use new method
├── partnership/
│   └── (no changes — reuse existing PartnershipItem + toDomain mapper)
└── application/src/main/resources/
    ├── schemas/
    │   └── sponsoring_option_with_partnerships.schema.json  # New schema
    └── openapi/openapi.yaml                              # Add GET operation + schema ref

application/src/test/kotlin/fr/devlille/partners/connect/
└── sponsoring/
    └── infrastructure/api/
        └── SponsoringOptionRouteGetTest.kt               # Contract test
```

**Structure Decision**: Modify existing sponsoring domain module. No new modules needed. The partnership domain is only consumed via its existing `PartnershipItem` domain model and `toDomain()` mapper — no cross-repository dependency.

## Complexity Tracking

No constitution violations to justify.
