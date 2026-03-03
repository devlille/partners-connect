# Implementation Plan: Filter Partnerships by Declined Status

**Branch**: `018-filter-declined-partnerships` | **Date**: 2025 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/018-filter-declined-partnerships/spec.md`

**IMPORTANT**: The /plan command STOPS after Phase 1. Phases 2-4 are executed by other commands:
- Phase 2: /tasks command creates tasks.md
- Phase 3-4: Implementation execution (manual or via tools)

## Summary

Add `filter[declined]` boolean query parameter to two existing endpoints:
1. **GET** `/orgs/{orgSlug}/events/{eventSlug}/partnerships` — list endpoint
2. **POST** `/orgs/{orgSlug}/events/{eventSlug}/partnerships/email` — email sending endpoint

**Default behaviour (intentional breaking change)**: when `filter[declined]` is absent, declined
partnerships are **excluded** (`WHERE declined_at IS NULL`). Pass `filter[declined]=true` to include
them. No database schema changes required — `declined_at` column already exists.

## Technical Context

**Language/Version**: Kotlin 1.9.x / JVM 21 (Amazon Corretto)
**Primary Dependencies**: Ktor 2.x, Exposed ORM 0.41+, Koin, kotlinx.serialization
**Storage**: PostgreSQL (prod) / H2 in-memory (tests) — **no schema changes required**
**Testing**: JUnit 4-style Ktor `testApplication`, H2 in-memory for integration tests
**Target Platform**: JVM backend server (Port 8080)
**Project Type**: backend-only (Kotlin/Ktor — filter extension to existing endpoints)
**Performance Goals**: No additional queries — filter is part of existing WHERE clause
**Constraints**: Zero ktlint/detekt violations, 80% test coverage minimum
**Scale/Scope**: Two endpoints, one new field across ~8 files

**Implementation Details**:
- `PartnershipFilters.declined: Boolean = false` (non-nullable unlike other boolean filters)
- `PartnershipEntity.filters()` used by both repositories; add `declined` param + `isNull()` logic
- `toBooleanStrict()` helper needed: `String.toBoolean()` silently ignores invalid values (FR-005 requires HTTP 400)
- `buildMetadata()` in `PartnershipRepositoryExposed` needs new `FilterDefinition("declined", FilterType.BOOLEAN)`
- `declinedAt` != `suggestionDeclinedAt` — filter must only touch `PartnershipsTable.declinedAt`

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Code Quality Standards**: All new code must pass ktlint and detekt with zero violations, include
KDoc documentation for public APIs, and maintain 80% test coverage minimum.
**PASS** — Small focused change. `toBooleanStrict()` will be documented. Tests planned for both endpoints.

**Testing Strategy**: Feature must include contract tests for API schema validation and integration
tests for complete HTTP route workflows using H2 in-memory database.
**PASS** — Two new test files planned: `PartnershipListDeclinedFilterRouteGetTest` (contract) +
`PartnershipDeclinedFilterRoutesTest` (integration).

**Clean Architecture**: Feature must respect domain module boundaries, avoid circular dependencies,
maintain interface segregation.
**PASS** — Change flows Domain → Infrastructure(DB) → Application → Infrastructure(API). No new
cross-module dependencies.

**API Consistency**: REST endpoints must follow established naming conventions, return consistent
error formats, include OpenAPI documentation.
**PASS** — Follows existing `filter[*]` naming. OpenAPI updated in both endpoint blocks. Errors via
existing `BadRequestException`.

**Breaking Change Note**: Default exclusion of declined partnerships is an **intentional** breaking
change per clarification Q1. Documented in `spec.md` Assumptions section and `research.md`.

## Project Structure

### Documentation (this feature)

```
specs/018-filter-declined-partnerships/
├── plan.md              # This file (/plan command output)
├── research.md          # Phase 0 output (/plan command)
├── data-model.md        # Phase 1 output (/plan command)
├── quickstart.md        # Phase 1 output (/plan command)
├── contracts/           # Phase 1 output (/plan command)
│   └── api-changes.md
└── tasks.md             # Phase 2 output (/tasks command - NOT created by /plan)
```

### Source Code (files to modify)

```
server/application/src/main/kotlin/fr/devlille/partners/connect/
├── partnership/
│   ├── domain/
│   │   └── PartnershipItem.kt                      # MODIFY: add declined: Boolean = false
│   ├── application/
│   │   ├── PartnershipRepositoryExposed.kt          # MODIFY: pass declined + FilterDefinition
│   │   └── PartnershipEmailRepositoryExposed.kt     # MODIFY: pass declined
│   └── infrastructure/
│       ├── db/
│       │   └── PartnershipEntity.kt                 # MODIFY: add declined param + isNull()
│       └── api/
│           ├── PartnershipRoutes.kt                 # MODIFY: parse filter[declined]
│           ├── PartnershipEmailRoutes.kt            # MODIFY: parse filter[declined]
│           └── StringValues.ext.kt                  # MODIFY: add toBooleanStrict()
└── resources/
    └── openapi.yaml                                 # MODIFY: 2 parameter additions

server/application/src/test/kotlin/
├── partnership/
│   └── PartnershipDeclinedFilterRoutesTest.kt       # ADD: integration tests
└── partnership/infrastructure/api/
    └── PartnershipListDeclinedFilterRouteGetTest.kt  # ADD: contract tests
```

## Phase 0: Research

**Status**: complete — see [research.md](./research.md)

Key decisions resolved:
1. `Boolean = false` (not `Boolean?`) — default exclusion is explicit in the domain model
2. `toBooleanStrict()` helper is necessary — `String.toBoolean()` is silent about invalid input
3. No schema migration needed — `declined_at` already exists
4. `declinedAt IS NULL` is the correct declined check (not `suggestionDeclinedAt`)
5. Breaking change is intentional and documented
6. FR-010 (HTTP 204 vs 404 on email empty) — out of scope, preserve existing behaviour

## Phase 1: Design & Contracts

**Status**: complete

| Artifact | File | Status |
|----------|------|--------|
| Data model | [data-model.md](./data-model.md) | done |
| API contracts | [contracts/api-changes.md](./contracts/api-changes.md) | done |
| Quickstart | [quickstart.md](./quickstart.md) | done |

### Data Model Changes (summary)

No new database tables or columns. Query logic only:

```kotlin
// PartnershipFilters — PartnershipItem.kt
data class PartnershipFilters(
    // existing 7 fields unchanged
    val declined: Boolean = false   // NEW: non-nullable, default = exclude declined
)

// PartnershipEntity.filters() — PartnershipEntity.kt
fun filters(
    // existing params
    declined: Boolean = false       // NEW
): SizedIterable<PartnershipEntity> {
    // existing logic
    if (!declined) {
        op = op and (PartnershipsTable.declinedAt.isNull())
    }
    return find { op }
}
```

### Validation Helper (summary)

```kotlin
// StringValues.ext.kt — new function
fun String?.toBooleanStrict(paramName: String, default: Boolean): Boolean {
    if (this == null) return default
    return when (this.lowercase()) {
        "true" -> true
        "false" -> false
        else -> throw BadRequestException("Invalid value for '$paramName': expected 'true' or 'false'")
    }
}
```

### OpenAPI Changes (summary)

Add `filter[declined]` boolean parameter after `filter[organiser]` in:
- GET `/orgs/{orgSlug}/events/{eventSlug}/partnerships` (~line 2937)
- POST `/orgs/{orgSlug}/events/{eventSlug}/partnerships/email` (~line 4184)

See [contracts/api-changes.md](./contracts/api-changes.md) for exact YAML snippets.

## Phase 2: Task Planning Approach

*This section describes what the /tasks command will do — DO NOT execute during /plan*

**Task Generation Strategy**: Load `.specify/templates/tasks-template.md` as base, generate tasks
from Phase 1 design docs (contracts, data model, quickstart).

**Ordered task groups**:

| # | Task | Notes |
|---|------|-------|
| 1 | Add `toBooleanStrict()` to `StringValues.ext.kt` | Foundation for validation |
| 2 | Add `declined: Boolean = false` to `PartnershipFilters` | No side effects |
| 3 | Add `declined` param + `isNull()` logic to `PartnershipEntity.filters()` | Depends on 2 |
| 4 | Pass `declined` in `PartnershipRepositoryExposed.listByEvent()` | Depends on 3 |
| 5 | Add `FilterDefinition("declined", FilterType.BOOLEAN)` in `buildMetadata()` | Depends on 4 |
| 6 | Pass `declined` in `PartnershipEmailRepositoryExposed` | Depends on 3 |
| 7 | Parse `filter[declined]` in `PartnershipRoutes.kt` GET handler | Depends on 1, 2 |
| 8 | Parse `filter[declined]` in `PartnershipEmailRoutes.kt` POST handler | Depends on 1, 2 |
| 9 | Update `openapi.yaml` (2 param additions) | Independent |
| 10 | Create `PartnershipListDeclinedFilterRouteGetTest.kt` (contract) | Depends on 7 |
| 11 | Create `PartnershipDeclinedFilterRoutesTest.kt` (integration) | Depends on 4–8 |
| 12 | Run quality gates: `ktlintFormat`, `detekt`, `test`, `npm run validate` | Final |

**Parallel candidates**: Tasks 2 and 9 can run in parallel. Tasks 4 and 6 can run in parallel.

**Estimated Output**: 12-14 numbered tasks in tasks.md

**IMPORTANT**: This phase is executed by the /tasks command, NOT by /plan

## Complexity Tracking

*No architectural violations — feature is a focused filter extension.*

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|--------------------------------------|
| Breaking default change | Intentional per Q1 clarification | Null default would require callers to always pass `filter[declined]=false` |

## Progress Tracking

**Phase Status**:
- [x] Phase 0: Research complete (/plan command)
- [x] Phase 1: Design complete (/plan command)
- [x] Phase 2: Task planning complete (/plan command — approach only)
- [ ] Phase 3: Tasks generated (/tasks command)
- [ ] Phase 4: Implementation complete
- [ ] Phase 5: Validation passed

**Gate Status**:
- [x] Initial Constitution Check: PASS
- [x] Post-Design Constitution Check: PASS
- [x] All NEEDS CLARIFICATION resolved (3 clarifications applied)
- [x] Breaking change documented in spec.md + research.md

---
*Based on Constitution v2.1.1 - See `/memory/constitution.md`*
