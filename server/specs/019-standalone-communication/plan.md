# Implementation Plan: Schedule Standalone Communication

**Branch**: `019-standalone-communication` | **Date**: 2026-03-16 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/019-standalone-communication/spec.md`

## Summary

Introduce a dedicated `communication_plans` table as the single source of truth for all scheduled communications in an event's planning view. Standalone entries have no `partnershipId`; migrated/partnership-linked entries retain a nullable `partnershipId` FK. A data migration moves all current `communicationPublicationDate`/`communicationSupportUrl` values from `PartnershipsTable` into the new table. Three new CRUD endpoints (`POST`/`PUT`/`DELETE` at `/orgs/{orgSlug}/events/{eventSlug}/communication-plan[/{id}]`) manage entries. The existing `GET /communication` planning view is re-sourced from the new table. Existing `PUT .../communication/publication` and `PUT .../communication/support` routes are updated to write to the new table.

## Technical Context

**Language/Version**: Kotlin 1.9.x, JVM 21 (Amazon Corretto)  
**Primary Dependencies**: Ktor 2.x, Exposed 0.41+, kotlinx.serialization, Koin  
**Storage**: PostgreSQL (production), H2 in-memory (tests)  
**Testing**: JUnit5 + Ktor `testApplication`, `moduleSharedDb` pattern, H2 in-memory DB  
**Target Platform**: Linux server (Docker container)  
**Project Type**: Single Ktor backend — source under `application/src/main/kotlin/`  
**Performance Goals**: Planning view loads in a single query (same as current)  
**Constraints**: Backwards-compatible response shape on `GET /communication`; migration must not lose data; table schema extensible for future nullable `integrationId` FK  
**Scale/Scope**: Per-event entries; existing dataset is small (one row per partnership that has a communication date set)

## Constitution Check

| Gate | Status | Notes |
|------|--------|-------|
| ktlint zero violations | PASS | All generated code must be formatted before commit |
| detekt zero violations | PASS | No cross-repository dependencies; repository pattern followed |
| Tests ≥ 80% coverage | PASS | Contract + integration tests required for all new endpoints |
| Repository isolation | PASS | `CommunicationPlanRepositoryExposed` has no repository dependencies |
| Notifications in route | N/A | No Slack/email notifications triggered by this feature |
| `UUIDTable` / `datetime()` | PASS | New table follows existing DB standards |
| `moduleSharedDb` in tests | PASS | All tests use shared DB pattern |
| Schema extensible for `integrationId` | PASS | Column designed as nullable FK; can be added as a future `ALTER TABLE` migration |

## Project Structure

### Documentation (this feature)

```text
specs/019-standalone-communication/
├── plan.md              ← this file
├── research.md          ← Phase 0 output
├── data-model.md        ← Phase 1 output
├── quickstart.md        ← Phase 1 output
├── contracts/           ← Phase 1 output
└── tasks.md             ← Phase 2 output (/speckit.tasks — NOT created here)
```

### Source Code (new files to create)

```text
application/src/main/kotlin/fr/devlille/partners/connect/
└── partnership/
    ├── domain/
    │   ├── CommunicationPlan.kt                      ← add CommunicationPlanEntry domain model (alongside existing CommunicationPlan/CommunicationItem)
    │   └── CommunicationPlanRepository.kt            ← NEW interface
    ├── application/
    │   └── CommunicationPlanRepositoryExposed.kt     ← NEW Exposed implementation
    └── infrastructure/
        ├── api/
        │   └── CommunicationPlanRoutes.kt            ← NEW CRUD routes
        ├── bindings/
        │   └── PartnershipModule.kt                  ← ADD single<CommunicationPlanRepository> binding
        └── db/
            ├── CommunicationPlansTable.kt            ← NEW table
            └── CommunicationPlanEntity.kt            ← NEW entity

application/src/main/kotlin/fr/devlille/partners/connect/internal/infrastructure/migrations/versions/
    ├── CreateCommunicationPlansTableMigration.kt     ← NEW (DDL: create table)
    └── MigratePartnershipCommunicationsMigration.kt  ← NEW (DML: copy existing rows)

application/src/main/resources/schemas/
    └── communication_plan_request.schema.json       ← NEW JSON schema for POST/PUT body

application/src/main/resources/openapi.yaml          ← ADD 3 new operations + CommunicationPlanEntry schema
```

### Source Code (files to modify)

```text
partnership/infrastructure/api/PartnershipRoutes.kt
    └── ADD orgsEventCommunicationPlanRoutes() call

partnership/infrastructure/api/PartnershipCommunicationRoutes.kt
    └── UPDATE GET /communication data source (re-source listCommunicationPlan from new table)
    └── UPDATE PUT .../publication to write to CommunicationPlansTable
    └── UPDATE PUT .../support to write to CommunicationPlansTable

partnership/domain/PartnershipCommunicationRepository.kt
    └── UPDATE listCommunicationPlan signature to use CommunicationPlanEntry items

partnership/application/PartnershipCommunicationRepositoryExposed.kt
    └── UPDATE listCommunicationPlan, updateCommunicationPublicationDate, updateCommunicationSupportUrl

internal/infrastructure/migrations/MigrationRegistry.kt
    └── ADD CreateCommunicationPlansTableMigration, MigratePartnershipCommunicationsMigration

application/src/test/kotlin/...
    ├── EventCommunicationPlanRouteGetTest.kt         ← UPDATE test setup (new table, new factory)
    ├── CommunicationPlanRoutePostTest.kt             ← NEW contract test
    ├── CommunicationPlanRoutePutTest.kt              ← NEW contract test
    ├── CommunicationPlanRouteDeleteTest.kt           ← NEW contract test
    └── CommunicationPlanRoutesTest.kt                ← NEW integration test

partnership/factories/CommunicationPlan.factory.kt    ← NEW factory
```

## Complexity Tracking

No constitution violations. Architecture is additive: new table + new repository + new routes. Migration is data-only (safe, reversible at application level). All existing API contracts preserved.

