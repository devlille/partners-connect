# Implementation Plan: Option Usage Count

**Branch**: `022-option-usage-count` | **Date**: 2026-03-25 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/022-option-usage-count/spec.md`

## Summary

Add a `partnership_count` field to each option in the options list endpoint by wrapping each option in a new object (`SponsoringOptionWithCount`). The count represents the number of validated partnerships whose validated pack contains the option. Uses the same `validatedPack()` → `PackOptionsTable` association logic from feature 021, computed efficiently in a single pass for all options.

## Technical Context

**Language/Version**: Kotlin 1.9.x / JVM 21 (Amazon Corretto)
**Primary Dependencies**: Ktor 2.x, Exposed ORM, kotlinx.serialization, Koin
**Storage**: PostgreSQL (H2 in-memory for tests)
**Testing**: Ktor `testApplication` with H2 shared DB, contract tests + integration tests
**Target Platform**: Linux server (Docker container)
**Project Type**: Single Kotlin/Ktor server project
**Performance Goals**: <2s response time for standard operations (constitution)
**Constraints**: `validatedPack()` is an in-memory function — cannot be expressed as pure SQL; must load all event partnerships
**Scale/Scope**: Organizer-facing endpoint, moderate traffic per event

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality Standards | PASS | Will run ktlint/detekt, zero violations |
| II. Comprehensive Testing Strategy | PASS | Contract tests for response shape and count accuracy via HTTP route tests |
| III. Clean Modular Architecture | PASS | New wrapper in `sponsoring/domain/`, repository method in existing `OptionRepository` |
| IV. API Consistency & User Experience | PASS | Wrapper approach, OpenAPI updated, JSON schema added |
| V. Performance & Observability | PASS | Single-pass counting for all options; no N+1 queries |

No violations. All gates pass.

## Project Structure

### Documentation (this feature)

```text
specs/022-option-usage-count/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/
├── domain/
│   ├── SponsoringOptionWithCount.kt          # NEW: wrapper data class
│   ├── OptionRepository.kt                   # MODIFIED: new method signature
│   └── SponsoringOptionWithTranslations.kt   # UNCHANGED
├── application/
│   └── OptionRepositoryExposed.kt            # MODIFIED: new method implementation
└── infrastructure/
    └── api/
        └── SponsoringRoutes.kt               # MODIFIED: list route uses new method

application/src/main/resources/
├── schemas/
│   └── sponsoring_option_with_count.schema.json  # NEW: JSON schema for wrapper
└── openapi/
    └── openapi.yaml                              # MODIFIED: list endpoint response schema

application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/
└── infrastructure/api/
    └── SponsoringListOptionRouteGetTest.kt       # MODIFIED: updated contract tests
```

**Structure Decision**: Follows existing domain module structure. New wrapper class in `sponsoring/domain/`, new JSON schema in `schemas/`, modifications to existing repository interface/implementation and route handler.

## Complexity Tracking

No constitution violations to justify.
