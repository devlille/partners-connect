# Implementation Plan: Partnership Q&A Game

**Branch**: `023-partnership-qanda` | **Date**: 2026-04-02 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/023-partnership-qanda/spec.md`

## Summary

Add a Q&A game feature to events: organisers configure Q&A limits (max questions per partnership, max answers per question) via the existing event update endpoint; partners create/edit/delete questions with answers on their partnership space; a public endpoint exposes all event questions grouped by partnership for external game services; webhook payloads include Q&A data. Following the booth activities CRUD pattern (same domain module, same routing structure, similar table design).

## Technical Context

**Language/Version**: Kotlin 1.9.x / JVM 21 (Amazon Corretto)  
**Primary Dependencies**: Ktor 2.x, Exposed ORM 0.41+, kotlinx.serialization, Koin  
**Storage**: PostgreSQL (H2 in-memory for tests)  
**Testing**: JUnit 5 via Ktor `testApplication`, H2 shared DB (`moduleSharedDb`)  
**Target Platform**: Linux server (Docker)  
**Project Type**: Single Kotlin/Ktor application  
**Performance Goals**: <2s response time for standard operations (per constitution)  
**Constraints**: Schema-validated requests, zero ktlint/detekt violations, ≥80% test coverage  
**Scale/Scope**: Adds 2 new tables, extends 1 existing table, 1 new domain model area, ~6 new endpoints

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality Standards | PASS | ktlint + detekt enforced, no exceptions needed |
| II. Comprehensive Testing | PASS | Contract + integration tests planned for all endpoints |
| III. Clean Modular Architecture | PASS | Q&A lives within existing `partnership` domain module (follows booth activities pattern). No new modules. No cross-repository dependencies. |
| IV. API Consistency | PASS | REST naming, JSON schema validation, OpenAPI docs, slug-based event refs |
| V. Performance & Observability | PASS | No performance testing in implementation phase; standard DB queries with indexes |

**Post-Phase 1 Re-check**: All gates still PASS. No violations to justify.

## Project Structure

### Documentation (this feature)

```text
specs/023-partnership-qanda/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output (OpenAPI schemas)
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
application/src/main/kotlin/fr/devlille/partners/connect/
├── events/
│   ├── domain/
│   │   ├── Event.kt                          # MODIFY: add Q&A config fields
│   │   ├── EventDisplay.kt                   # MODIFY: add Q&A config in response
│   │   └── QandaConfig.kt                    # NEW: Q&A configuration value object
│   └── infrastructure/
│       └── db/
│           └── EventsTable.kt                # MODIFY: add qanda columns
│           └── EventEntity.kt                # MODIFY: add qanda properties
├── partnership/
│   ├── domain/
│   │   ├── QandaQuestion.kt                  # NEW: question domain model
│   │   ├── QandaQuestionRequest.kt           # NEW: create/update request DTO
│   │   ├── QandaAnswer.kt                    # NEW: answer domain model (nested in question)
│   │   └── QandaRepository.kt                # NEW: repository interface
│   ├── application/
│   │   ├── QandaRepositoryExposed.kt         # NEW: Exposed implementation
│   │   └── mappers/
│   │       └── QandaEntity.ext.kt            # NEW: entity → domain mappers
│   └── infrastructure/
│       ├── api/
│       │   ├── QandaRoutes.kt                # NEW: Q&A route handlers
│       │   └── StringValues.ext.kt           # MODIFY: add questionId extension
│       ├── db/
│       │   ├── QandaQuestionsTable.kt        # NEW: questions table
│       │   ├── QandaQuestionEntity.kt        # NEW: question entity
│       │   ├── QandaAnswersTable.kt          # NEW: answers table
│       │   └── QandaAnswerEntity.kt          # NEW: answer entity
│       └── bindings/
│           └── PartnershipModule.kt          # MODIFY: register QandaRepository
├── webhooks/
│   ├── domain/
│   │   └── WebhookPayload.kt                # MODIFY: add questions field
│   └── infrastructure/gateways/
│       └── HttpWebhookGateway.kt             # MODIFY: fetch Q&A data for payload
└── internal/infrastructure/migrations/
    └── MigrationRegistry.kt                  # MODIFY: register new migration

application/src/main/resources/
├── schemas/
│   └── qanda_question_request.schema.json    # NEW: JSON schema for question create/update
└── openapi/
    └── openapi.yaml                           # MODIFY: add Q&A endpoints

application/src/test/kotlin/fr/devlille/partners/connect/partnership/
├── factories/
│   ├── QandaQuestion.factory.kt              # NEW: insertMockedQandaQuestion()
│   └── QandaAnswer.factory.kt                # NEW: insertMockedQandaAnswer()
├── infrastructure/api/
│   ├── QandaQuestionRoutePostTest.kt         # NEW: contract test
│   ├── QandaQuestionRoutePutTest.kt          # NEW: contract test
│   ├── QandaQuestionRouteDeleteTest.kt       # NEW: contract test
│   ├── QandaQuestionRouteGetTest.kt          # NEW: contract test
│   └── QandaEventQuestionsRouteGetTest.kt    # NEW: contract test (public endpoint)
└── QandaRoutesTest.kt                        # NEW: integration test
```

**Structure Decision**: Q&A is a partnership sub-resource (same as booth activities). It lives in the `partnership` domain module. Event-level configuration fields are added to the `events` module. No new top-level modules created.
