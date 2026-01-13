---

description: "Task list for Partnership Email History feature implementation"
---

# Tasks: Partnership Email History

**Input**: Design documents from `/specs/016-partnership-email-history/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Contract tests and integration tests included per constitution requirements.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [X] T001 Add database schema registration to MigrationRegistry for PartnershipEmailHistoryTable and RecipientDeliveryStatusTable
- [X] T002 [P] Create EmailStatus enum in application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/EmailStatus.kt
- [X] T003 [P] Create DeliveryStatus enum in application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/DeliveryStatus.kt

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T004 [P] Create EmailDeliveryResult domain model in application/src/main/kotlin/fr/devlille/partners/connect/notifications/domain/EmailDeliveryResult.kt
- [X] T005 [P] Create RecipientResult domain model (nested in EmailDeliveryResult) in application/src/main/kotlin/fr/devlille/partners/connect/notifications/domain/RecipientResult.kt
- [X] T006 Update NotificationGateway interface to return EmailDeliveryResult instead of Boolean in application/src/main/kotlin/fr/devlille/partners/connect/notifications/domain/NotificationGateway.kt
- [X] T007 Update MailjetProvider to parse Mailjet response JSON (extract "Sent" array, map present emails to SENT status, absent emails to FAILED status) and return structured result with per-recipient status in application/src/main/kotlin/fr/devlille/partners/connect/notifications/infrastructure/providers/MailjetProvider.kt
- [X] T008 Update MailjetNotificationGateway to map MailjetProvider result to EmailDeliveryResult (NO history logging - that happens in routes) in application/src/main/kotlin/fr/devlille/partners/connect/notifications/infrastructure/gateways/MailjetNotificationGateway.kt

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 2 - Automatic Email History Logging (Priority: P1) 🎯 MVP

**Goal**: All emails sent through partnership notification system are automatically logged with full details

**Independent Test**: Trigger email through existing partnership workflows, verify history endpoint returns the email

### Database & Domain Models for User Story 2

- [X] T009 [P] [US2] Create PartnershipEmailHistoryTable in application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/db/PartnershipEmailHistoryTable.kt
- [X] T010 [P] [US2] Create RecipientDeliveryStatusTable in application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/db/RecipientDeliveryStatusTable.kt
- [X] T011 [P] [US2] Create PartnershipEmailHistoryEntity in application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/db/PartnershipEmailHistoryEntity.kt
- [X] T012 [P] [US2] Create RecipientDeliveryStatusEntity in application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/db/RecipientDeliveryStatusEntity.kt
- [X] T013 [P] [US2] Create PartnershipEmailHistory domain model in application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/PartnershipEmailHistory.kt

### Repository Implementation for User Story 2

- [X] T014 [US2] Create PartnershipEmailHistoryRepository interface in application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/PartnershipEmailHistoryRepository.kt
- [X] T015 [US2] Implement PartnershipEmailHistoryRepositoryExposed with create() method in application/src/main/kotlin/fr/devlille/partners/connect/partnership/application/PartnershipEmailHistoryRepositoryExposed.kt
- [X] T016 [US2] Add entity-to-domain mapping (toDomain()) methods for PartnershipEmailHistoryEntity and RecipientDeliveryStatusEntity in application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/db/PartnershipEmailHistoryEntity.kt

### Integration with Routes for User Story 2

- [X] T017 [US2] Register PartnershipEmailHistoryRepository in Koin DI partnership module in application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/bindings/PartnershipModule.kt
- [X] T018 [US2] Update PartnershipDecisionRoutes (approve/reject endpoints) to log email history after calling notificationRepository.sendMessage() in application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/PartnershipDecisionRoutes.kt
- [X] T019 [US2] Update PartnershipAgreementRoutes to log email history after calling notificationRepository.sendMessage() in application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/PartnershipAgreementRoutes.kt
- [X] T019a [US2] Update PartnershipEmailRoutes to log email history after calling notificationRepository.sendMessage() in application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/PartnershipEmailRoutes.kt
- [X] T019b [US2] Update PartnershipBillingRoutes to log email history after calling notificationRepository.sendMessage() in application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/PartnershipBillingRoutes.kt
- [X] T019c [US2] Update PartnershipJobOfferRoutes to log email history after calling notificationRepository.sendMessage() in application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/PartnershipJobOfferRoutes.kt
- [X] T019d [US2] Update PartnershipSuggestionRoutes to log email history after calling notificationRepository.sendMessage() in application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/PartnershipSuggestionRoutes.kt
- [X] T019e [US2] Update PartnershipRoutes to log email history after calling notificationRepository.sendMessage() in application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/PartnershipRoutes.kt

**Checkpoint**: At this point, emails are automatically logged to database when sent

---

## Phase 4: User Story 1 - View Email History for Partnership (Priority: P1)

**Goal**: Organisers can view complete email history for partnerships they manage

**Independent Test**: Create test partnership, send test emails, retrieve via GET endpoint and verify all emails listed

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T020 [P] [US1] Create contract test PartnershipEmailHistoryRouteGetTest in application/src/test/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/PartnershipEmailHistoryRouteGetTest.kt
- [X] T021 [P] [US1] Create integration test PartnershipEmailHistoryRoutesTest in application/src/test/kotlin/fr/devlille/partners/connect/partnership/PartnershipEmailHistoryRoutesTest.kt
- [X] T022 [P] [US1] Create factory function insertMockedPartnershipEmailHistory() in application/src/test/kotlin/fr/devlille/partners/connect/partnership/factories/PartnershipEmailHistory.factory.kt

### Implementation for User Story 1

- [X] T023 [US1] Add findByPartnershipId() method with pagination to PartnershipEmailHistoryRepositoryExposed in application/src/main/kotlin/fr/devlille/partners/connect/partnership/application/PartnershipEmailHistoryRepositoryExposed.kt
- [X] T024 [US1] Add countByPartnershipId() method to PartnershipEmailHistoryRepositoryExposed in application/src/main/kotlin/fr/devlille/partners/connect/partnership/application/PartnershipEmailHistoryRepositoryExposed.kt
- [X] T025 [P] [US1] Create PartnershipEmailHistoryResponse DTO in application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/PartnershipEmailHistoryResponse.kt
- [X] T026 [P] [US1] Create RecipientStatusResponse DTO in application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/RecipientStatusResponse.kt
- [X] T027 [US1] Create GET /email-history route in PartnershipEmailHistoryRoutes.kt in application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/PartnershipEmailHistoryRoutes.kt
- [X] T027a [US1] Add partnership existence validation (throw NotFoundException if partnership doesn't exist) in PartnershipEmailHistoryRoutes.kt
- [X] T028 [US1] Install AuthorizedOrganisationPlugin on email history route in application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/PartnershipEmailHistoryRoutes.kt
- [X] T029 [US1] Implement query parameter validation (page >= 0, page_size 1-100) in application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/PartnershipEmailHistoryRoutes.kt
- [X] T030 [US1] Implement pagination logic using PaginatedResponse<PartnershipEmailHistoryResponse> in application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/PartnershipEmailHistoryRoutes.kt
- [X] T031 [US1] Register email history route in App.kt module configuration in application/src/main/kotlin/fr/devlille/partners/connect/App.kt

**Checkpoint**: At this point, User Stories 1 AND 2 should both work - emails logged automatically (in routes after notification) and retrievable via API

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T032 [P] Create partnership_email_history_response.schema.json in application/src/main/resources/schemas/partnership_email_history_response.schema.json
- [ ] T033 [P] Create recipient_status_response.schema.json in application/src/main/resources/schemas/recipient_status_response.schema.json
- [ ] T034 [P] Update OpenAPI specification with GET /orgs/{orgSlug}/events/{eventSlug}/partnerships/{id}/email-history endpoint in application/src/main/resources/openapi.yaml
- [ ] T035 [P] Add KDoc documentation to PartnershipEmailHistory domain model in application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/PartnershipEmailHistory.kt
- [ ] T036 [P] Add KDoc documentation to PartnershipEmailHistoryRepository interface in application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/PartnershipEmailHistoryRepository.kt
- [ ] T037 Run ktlintFormat and detekt to ensure code quality standards
- [ ] T038 Run full test suite (./gradlew test --no-daemon) and verify 80%+ coverage
- [ ] T039 Validate quickstart.md examples against running application
- [ ] T040 Run npm run validate to verify OpenAPI schema validity

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Story 2 (Phase 3)**: Depends on Foundational phase completion - Automatic logging must work before retrieval makes sense
- **User Story 1 (Phase 4)**: Depends on Foundational and User Story 2 completion - Cannot retrieve history until logging works
- **Polish (Phase 5)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 2 (P1)**: Must complete FIRST - Automatic logging is foundation for entire feature
- **User Story 1 (P1)**: Must complete AFTER User Story 2 - Retrieval requires logged data to exist

**Rationale for US2 First**: Although both are P1, automatic logging (US2) must be implemented before retrieval (US1) because:
1. US1 requires data to exist in database (created by US2)
2. Testing US1 requires ability to send emails and have them logged (US2 functionality)
3. US2 is foundational infrastructure, US1 is API consumption of that infrastructure

### Within Each User Story

**User Story 2 (Automatic Logging)**:
1. Database tables and entities (T009-T012) - Can run in parallel
2. Domain model (T013) - Can run in parallel with database work
3. Repository interface (T014) - After domain model complete
4. Repository implementation (T015) - After repository interface
5. Entity mapping (T016) - After entities and domain model
6. DI integration (T017) - After repository implementation
7. Route logging integration (T018-T019e) - After DI integration - All route updates can run in parallel

**User Story 1 (View History)**:
1. Tests (T020-T022) - Can run in parallel, MUST FAIL before implementation
2. Repository methods (T023-T024) - Can run sequentially
3. DTOs (T025-T026) - Can run in parallel
4. Route implementation (T027, T027a, T028-T030) - Sequential after repository and DTOs
5. Route registration (T031) - After route implementation

### Parallel Opportunities

- **Setup (Phase 1)**: T002 and T003 can run in parallel (different enum files)
- **Foundational (Phase 2)**: T004 and T005 can run in parallel (different domain models)
- **User Story 2 Database**: T009, T010, T011, T012, T013 can all run in parallel (different files)
- **User Story 2 Routes**: T018, T019, T019a-T019e can all run in parallel (different route files)
- **User Story 1 Tests**: T020, T021, T022 can run in parallel (different test files)
- **User Story 1 DTOs**: T025 and T026 can run in parallel (different DTO files)
- **Polish**: T032, T033, T034, T035, T036 can all run in parallel (different documentation files)

---

## Parallel Example: User Story 2 Database Setup

```bash
# All these tasks can run simultaneously (different files):
git checkout -b us2-database-models

# Developer A: Tables
touch application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/db/PartnershipEmailHistoryTable.kt
# ... implement T009

# Developer B: Entities  
touch application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/db/PartnershipEmailHistoryEntity.kt
# ... implement T011

# Developer C: Domain Model
touch application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/PartnershipEmailHistory.kt
# ... implement T013

# All merge once complete - no conflicts (different files)
```

---

## Implementation Strategy

### MVP Scope (User Story 2 Only)

For fastest value delivery, implement **User Story 2 ONLY** first:
- Emails automatically logged when sent
- Data persists in database
- No API endpoint yet (Phase 4)

**Value**: Immediately captures historical data, even before retrieval UI is built. Data preservation begins on day 1.

**Estimated Effort**: 24 tasks (T001-T019e)

### Full Feature (User Stories 1 + 2)

Complete implementation with retrieval API:
- User Story 2: Automatic logging ✅
- User Story 1: GET endpoint for history retrieval ✅
- Polish: Documentation and schemas ✅

**Value**: Complete audit trail + organiser UI access

**Estimated Effort**: 40 tasks (T001-T040)

---

## Task Count Summary

- **Phase 1 (Setup)**: 3 tasks
- **Phase 2 (Foundational)**: 5 tasks
- **Phase 3 (User Story 2 - Automatic Logging)**: 16 tasks (T009-T019e)
- **Phase 4 (User Story 1 - View History)**: 13 tasks (T020-T031)
- **Phase 5 (Polish)**: 9 tasks (T032-T040)

**Total**: 46 tasks

**MVP (US2 only)**: 24 tasks (Phase 1 + Phase 2 + Phase 3)
**Full Feature**: 46 tasks (all phases)

---

## Validation Checklist

After implementation, verify:

- [ ] All 46 tasks completed and checked off
- [ ] Contract tests pass: PartnershipEmailHistoryRouteGetTest
- [ ] Integration tests pass: PartnershipEmailHistoryRoutesTest
- [ ] Emails automatically logged when sent through any partnership workflow
- [ ] GET /email-history endpoint returns paginated results
- [ ] Authorization enforced (organisers only)
- [ ] Per-recipient delivery status captured correctly
- [ ] Pagination works (page, page_size query params)
- [ ] OpenAPI spec validates (npm run validate)
- [ ] ktlint passes (./gradlew ktlintCheck --no-daemon)
- [ ] detekt passes (./gradlew detekt --no-daemon)
- [ ] Test coverage ≥ 80% (./gradlew test --no-daemon)
- [ ] quickstart.md examples work against running application
- [ ] Email history survives partnership deletion (NO_ACTION foreign key)
- [ ] TEXT column stores unlimited email body content

---

## Notes

**Tests**: All test tasks (T020-T022) must be written FIRST and FAIL before implementation begins. This ensures TDD approach per constitution.

**Parallelization**: 22 tasks can run in parallel within their phases (7 route updates in US2, 5 database models in US2, 3 tests in US1, 2 DTOs in US1, 5 polish tasks), enabling faster completion if multiple developers are available.

**MVP Focus**: User Story 2 (automatic logging) is the MVP. Even without retrieval API (US1), system begins capturing valuable audit trail data immediately.

**Constitution Compliance**: All tasks follow Kotlin/Ktor/Exposed patterns per AGENTS.md, including UUIDTable, datetime(), HTTP route testing, AuthorizedOrganisationPlugin, factory functions with UUID-based defaults.
