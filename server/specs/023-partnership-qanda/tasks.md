# Tasks: Partnership Q&A Game

**Input**: Design documents from `/specs/023-partnership-qanda/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Included — contract tests and integration tests per constitution requirement (≥80% coverage).

**Organization**: Tasks grouped by user story to enable independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Database migration, new tables, entities, and shared domain models needed by all user stories.

- [X] T001 Create database migration `CreateQandaTablesMigration` that adds `qanda_enabled`, `qanda_max_questions`, `qanda_max_answers` columns to `events` table, creates `qanda_questions` table, and creates `qanda_answers` table in `application/src/main/kotlin/fr/devlille/partners/connect/internal/infrastructure/migrations/CreateQandaTablesMigration.kt`
- [X] T002 Register `CreateQandaTablesMigration` in `application/src/main/kotlin/fr/devlille/partners/connect/internal/infrastructure/migrations/MigrationRegistry.kt`
- [X] T003 [P] Create `QandaQuestionsTable` in `application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/db/QandaQuestionsTable.kt`
- [X] T004 [P] Create `QandaAnswersTable` in `application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/db/QandaAnswersTable.kt`
- [X] T005 [P] Create `QandaQuestionEntity` in `application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/db/QandaQuestionEntity.kt`
- [X] T006 [P] Create `QandaAnswerEntity` in `application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/db/QandaAnswerEntity.kt`
- [X] T007 [P] Create `QandaQuestion` domain model in `application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/QandaQuestion.kt`
- [X] T008 [P] Create `QandaAnswer` domain model in `application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/QandaAnswer.kt`
- [X] T009 [P] Create `QandaQuestionRequest` and `QandaAnswerInput` DTOs in `application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/QandaQuestionRequest.kt`
- [X] T010 [P] Create `PartnershipQandaSummary` domain model in `application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/PartnershipQandaSummary.kt`
- [X] T011 [P] Create `QandaConfig` value object in `application/src/main/kotlin/fr/devlille/partners/connect/events/domain/QandaConfig.kt`
- [X] T012 [P] Create entity-to-domain mappers in `application/src/main/kotlin/fr/devlille/partners/connect/partnership/application/mappers/QandaEntity.ext.kt`
- [X] T013 [P] Create JSON schema `qanda_question_request.schema.json` in `application/src/main/resources/schemas/qanda_question_request.schema.json`
- [X] T014 [P] Create `questionId` StringValues extension in `application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/StringValues.ext.kt`

**Checkpoint**: All shared infrastructure ready. User stories can begin.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Repository interface, implementation, Koin binding, and test factories needed before any route or test can be written.

- [X] T015 Create `QandaRepository` interface in `application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/QandaRepository.kt`
- [X] T016 Implement `QandaRepositoryExposed` with all CRUD operations and validation logic in `application/src/main/kotlin/fr/devlille/partners/connect/partnership/application/QandaRepositoryExposed.kt`
- [X] T017 Register `QandaRepository` binding in `application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/bindings/PartnershipModule.kt`
- [X] T018 [P] Create `insertMockedQandaQuestion()` factory in `application/src/test/kotlin/fr/devlille/partners/connect/partnership/factories/QandaQuestion.factory.kt`
- [X] T019 [P] Create `insertMockedQandaAnswer()` factory in `application/src/test/kotlin/fr/devlille/partners/connect/partnership/factories/QandaAnswer.factory.kt`

**Checkpoint**: Foundation ready — route implementation and tests can begin.

---

## Phase 3: User Story 1 — Organiser enables Q&A game for an event (Priority: P1) 🎯 MVP

**Goal**: Organisers can enable/disable Q&A and configure max questions/answers limits via the existing event update endpoint. Q&A config is visible in event display.

**Independent Test**: Update an event with Q&A config, then GET the event and verify `qanda_config` is present.

### Implementation for User Story 1

- [X] T020 [US1] Add `qandaEnabled`, `qandaMaxQuestions`, `qandaMaxAnswers` columns to `EventsTable` in `application/src/main/kotlin/fr/devlille/partners/connect/events/infrastructure/db/EventsTable.kt`
- [X] T021 [US1] Add `qandaEnabled`, `qandaMaxQuestions`, `qandaMaxAnswers` properties to `EventEntity` in `application/src/main/kotlin/fr/devlille/partners/connect/events/infrastructure/db/EventEntity.kt`
- [X] T022 [US1] Add `qandaEnabled`, `qandaMaxQuestions`, `qandaMaxAnswers` fields to `Event` DTO in `application/src/main/kotlin/fr/devlille/partners/connect/events/domain/Event.kt`
- [X] T023 [US1] Add `qandaConfig` field to `EventDisplay` DTO in `application/src/main/kotlin/fr/devlille/partners/connect/events/domain/EventDisplay.kt`
- [X] T024 [US1] Update event create/update repository logic to persist Q&A config fields and validate constraints (max_questions ≥ 1, max_answers ≥ 2 when enabled) in `application/src/main/kotlin/fr/devlille/partners/connect/events/application/EventRepositoryExposed.kt`
- [X] T025 [US1] Update event-to-domain mapper to populate `qandaConfig` in `EventDisplay` (null when disabled) in event mapper file
- [X] T026 [US1] Update `create_event.schema.json` to add optional `qanda_enabled`, `qanda_max_questions`, `qanda_max_answers` fields in `application/src/main/resources/schemas/create_event.schema.json`

### Tests for User Story 1

- [X] T027 [P] [US1] Contract test for event update with Q&A config (PUT returns 200 with Q&A fields, 400 for invalid config) in `application/src/test/kotlin/fr/devlille/partners/connect/events/infrastructure/api/EventQandaConfigRoutePutTest.kt`
- [X] T028 [P] [US1] Contract test for event display with Q&A config — covers both public GET `/events/{eventSlug}` and org GET `/orgs/{orgSlug}/events/{eventSlug}` (returns `qanda_config` when enabled, null when disabled) in `application/src/test/kotlin/fr/devlille/partners/connect/events/infrastructure/api/EventQandaConfigRouteGetTest.kt`

**Checkpoint**: Event Q&A configuration works end-to-end. MVP delivers organiser value.

---

## Phase 4: User Story 2 — Partner submits questions and answers (Priority: P2)

**Goal**: Partners can create, edit, and delete questions with answers on their partnership space. All validation rules enforced.

**Independent Test**: POST a question to a partnership on a Q&A-enabled event, verify 201; GET the questions list, verify question appears; PUT to update, verify 200; DELETE to remove, verify 204.

### Implementation for User Story 2

- [X] T029 [US2] Create `QandaRoutes.kt` with public partnership CRUD routes (GET list, POST create, PUT update, DELETE) in `application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/QandaRoutes.kt`
- [X] T030 [US2] Register Q&A routes in `partnershipRoutes()` in `application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/PartnershipRoutes.kt`
- [X] T031 [US2] Register Q&A routes mount point in `application/src/main/kotlin/fr/devlille/partners/connect/App.kt` (if not already mounted via partnershipRoutes)

### Tests for User Story 2

- [X] T032 [P] [US2] Contract test for POST question (201 created, 400 validation errors, 403 Q&A disabled, 409 limit reached) in `application/src/test/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/QandaQuestionRoutePostTest.kt`
- [X] T033 [P] [US2] Contract test for PUT question (200 updated, 400 validation, 404 not found) in `application/src/test/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/QandaQuestionRoutePutTest.kt`
- [X] T034 [P] [US2] Contract test for DELETE question (204 success, 404 not found) in `application/src/test/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/QandaQuestionRouteDeleteTest.kt`
- [X] T035 [P] [US2] Contract test for GET partnership questions (200 with list) in `application/src/test/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/QandaQuestionRouteGetTest.kt`
- [X] T036 [US2] Integration test for full Q&A CRUD lifecycle (create → list → update → delete) in `application/src/test/kotlin/fr/devlille/partners/connect/partnership/QandaRoutesTest.kt`

**Checkpoint**: Partners can fully manage their Q&A questions. Core content creation flow works.

---

## Phase 5: User Story 3 — Retrieve all questions for an event (Priority: P3)

**Goal**: External services can fetch all Q&A questions for an event, grouped by partnership, via a public endpoint.

**Independent Test**: Create questions for multiple partnerships, then GET `/events/{eventSlug}/qanda/questions` and verify all questions returned grouped by partnership.

### Implementation for User Story 3

- [X] T037 [US3] Add public event-level Q&A route (GET `/events/{eventSlug}/qanda/questions`) in `application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/QandaRoutes.kt`

### Tests for User Story 3

- [X] T038 [P] [US3] Contract test for GET event questions (200 grouped by partnership, 403 Q&A disabled, 200 empty list when no questions) in `application/src/test/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/QandaEventQuestionsRouteGetTest.kt`
- [X] T039 [US3] Integration test for event questions listing with multiple partnerships in `application/src/test/kotlin/fr/devlille/partners/connect/partnership/QandaRoutesTest.kt` (extend existing)

**Checkpoint**: External game services can consume Q&A data. Read-side complete.

---

## Phase 6: User Story 4 — Q&A data included in webhook payload (Priority: P4)

**Goal**: Webhook payloads include partnership Q&A data alongside existing fields.

**Independent Test**: Create questions for a partnership, trigger a webhook, verify the payload includes the `questions` array.

### Implementation for User Story 4

- [X] T040 [US4] Add `questions` field to `WebhookPayload` in `application/src/main/kotlin/fr/devlille/partners/connect/webhooks/domain/WebhookPayload.kt`
- [X] T041 [US4] Fetch Q&A questions by partnership ID and include in webhook payload in `application/src/main/kotlin/fr/devlille/partners/connect/webhooks/infrastructure/gateways/HttpWebhookGateway.kt`

### Tests for User Story 4

- [X] T042 [US4] Integration test verifying webhook payload includes Q&A data (extend existing webhook test or add new) in `application/src/test/kotlin/fr/devlille/partners/connect/webhooks/`

**Checkpoint**: Webhook consumers receive Q&A data automatically. Full feature complete.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: OpenAPI documentation, code quality, and final validation.

- [X] T043 Add all Q&A endpoints (partnership CRUD, public event questions, Q&A config fields in event schema) to OpenAPI spec in `application/src/main/resources/openapi/openapi.yaml`
- [X] T044 Run `./gradlew ktlintFormat --no-daemon` and fix any formatting issues
- [X] T045 Run `./gradlew detekt --no-daemon` and fix any static analysis issues
- [X] T046 Run `npm run validate` to validate OpenAPI schema
- [X] T047 Run `./gradlew check --no-daemon` for full validation
- [X] T048 Run quickstart.md validation steps manually or via script

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — start immediately
- **Phase 2 (Foundational)**: Depends on Phase 1 (tables/entities must exist for repository)
- **Phase 3 (US1)**: Depends on Phase 1 (event table columns) — can start in parallel with Phase 2
- **Phase 4 (US2)**: Depends on Phase 2 (repository) + Phase 3 (Q&A enabled on event)
- **Phase 5 (US3)**: Depends on Phase 2 (repository) — can start in parallel with Phase 4
- **Phase 6 (US4)**: Depends on Phase 2 (repository) — can start in parallel with Phase 4/5
- **Phase 7 (Polish)**: Depends on all user stories being complete

### User Story Dependencies

- **US1 (P1)**: Independent — only needs Phase 1 (event table changes)
- **US2 (P2)**: Needs US1 complete (Q&A must be enabled to validate question creation)
- **US3 (P3)**: Needs Phase 2 complete — can run alongside US2
- **US4 (P4)**: Needs Phase 2 complete — can run alongside US2/US3

### Within Each User Story

- Models/tables before repository implementation
- Repository before routes
- Routes before tests (contract tests validate HTTP behavior)
- Integration tests after route implementation

### Parallel Opportunities

- T003–T014 (all Setup tasks marked [P]) can run in parallel
- T018–T019 (test factories) can run in parallel
- T027–T028 (US1 contract tests) can run in parallel
- T032–T035 (US2 contract tests) can run in parallel
- US3 and US4 can run in parallel after Phase 2

---

## Parallel Example: Phase 1 Setup

```
# Launch all infrastructure tasks together:
T003: Create QandaQuestionsTable
T004: Create QandaAnswersTable
T005: Create QandaQuestionEntity
T006: Create QandaAnswerEntity
T007: Create QandaQuestion domain model
T008: Create QandaAnswer domain model
T009: Create QandaQuestionRequest DTO
T010: Create PartnershipQandaSummary model
T011: Create QandaConfig value object
T012: Create entity-to-domain mappers
T013: Create JSON schema
T014: Create questionId StringValues extension
```

## Parallel Example: User Story 2 Tests

```
# Launch all contract tests together:
T032: Contract test POST question
T033: Contract test PUT question
T034: Contract test DELETE question
T035: Contract test GET partnership questions
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (tables, entities, models)
2. Complete Phase 3: US1 (event Q&A config)
3. **STOP and VALIDATE**: Organisers can configure Q&A on events
4. Deploy/demo if ready — delivers organiser value immediately

### Incremental Delivery

1. Phase 1 + Phase 2 → Foundation ready
2. Phase 3 (US1) → Organisers configure Q&A → Deploy/Demo (MVP!)
3. Phase 4 (US2) → Partners create questions → Deploy/Demo
4. Phase 5 (US3) → External services consume questions → Deploy/Demo
5. Phase 6 (US4) → Webhook integration → Deploy/Demo
6. Phase 7 → Polish, OpenAPI, quality gates → Final release

### Single Developer Strategy

1. Phase 1 → Phase 2 → Phase 3 → Phase 4 → Phase 5 → Phase 6 → Phase 7
2. Each phase builds on the previous, commit after each checkpoint

---

## Notes

- [P] tasks = different files, no dependencies
- [US*] label maps task to specific user story for traceability
- Total: 48 tasks across 7 phases
- Estimated contract tests: 6 test classes
- Estimated integration tests: 1 test class (extended across US2/US3)
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
