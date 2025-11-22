---
description: "Task list for organiser assignment to partnerships feature"
---

# Tasks: Assign Organiser to Partnership

**Feature Branch**: `011-assign-partnership-organiser`
**Input**: Design documents from `/specs/011-assign-partnership-organiser/`
**Prerequisites**: plan.md (tech stack), spec.md (user stories), data-model.md (entities), contracts/ (API endpoints), quickstart.md (test scenarios)

**Tests**: Contract tests are REQUIRED per constitution (TDD approach). Integration tests are REQUIRED for 80% coverage.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

This is a backend-only feature extending the partnership module in a Kotlin/Ktor server:
- **Server code**: `server/application/src/main/kotlin/fr/devlille/partners/connect/`
- **Tests**: `server/application/src/test/kotlin/fr/devlille/partners/connect/`
- **Schemas**: `server/application/src/main/resources/schemas/`
- **OpenAPI**: `server/application/src/main/resources/openapi/openapi.yaml`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Prepare database schema and JSON schemas for organiser assignment

- [ ] T001 [P] Create migration file `AddPartnershipOrganiserMigration.kt` in `server/application/src/main/kotlin/fr/devlille/partners/connect/internal/infrastructure/migrations/versions/`
- [ ] T002 Register migration in `MigrationRegistry.kt` in `server/application/src/main/kotlin/fr/devlille/partners/connect/internal/infrastructure/migrations/`
- [ ] T003 [P] Create JSON schema `assign_organiser_request.schema.json` in `server/application/src/main/resources/schemas/`
- [ ] T004 [P] Create JSON schema `partnership_organiser_response.schema.json` in `server/application/src/main/resources/schemas/`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core database and entity extensions that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T005 Add `organiserId` nullable column to `PartnershipsTable` in `server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/db/PartnershipsTable.kt`
- [ ] T006 Add `organiser` property to `PartnershipEntity` in `server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/db/PartnershipEntity.kt`
- [ ] T007 Extend `toPartnership()` mapping to include organiser in `server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/db/PartnershipEntity.kt`
- [ ] T008 Add `organiser: User?` field to `Partnership` domain model in `server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/Partnership.kt`
- [ ] T009 Run migration locally and verify `partnerships.organiser_id` column created (use `docker-compose up -d postgres && ./gradlew run`)

**Checkpoint**: Foundation ready - database schema extended, entities updated, user story implementation can now begin

---

## Phase 3: User Story 1 - Assign Organiser to New Partnership (Priority: P1) 🎯 MVP

**Goal**: Enable administrators to assign a team member as the designated organiser for a partnership, providing partners with a direct contact point.

**Independent Test**: Create partnership, assign organiser from organization's team members, verify assignment stored and displayed correctly via API.

### Contract Tests for User Story 1 (REQUIRED - TDD Approach)

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T010 [P] [US1] Create contract test file `PartnershipOrganiserRoutesContractTest.kt` in `server/application/src/test/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/`
- [ ] T011 [P] [US1] Write contract test: POST assigns organiser with valid request schema in `PartnershipOrganiserRoutesContractTest.kt`
- [ ] T012 [P] [US1] Write contract test: POST rejects missing email field in `PartnershipOrganiserRoutesContractTest.kt`
- [ ] T013 [P] [US1] Write contract test: POST rejects invalid email format in `PartnershipOrganiserRoutesContractTest.kt`
- [ ] T014 [P] [US1] Write contract test: POST returns valid PartnershipOrganiserResponse schema in `PartnershipOrganiserRoutesContractTest.kt`
- [ ] T015 [P] [US1] Write contract test: DELETE returns valid PartnershipOrganiserResponse with null organiser in `PartnershipOrganiserRoutesContractTest.kt`

### DTOs and Request/Response Models for User Story 1

- [ ] T016 [P] [US1] Create `AssignOrganiserRequest.kt` DTO in `server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/`
- [ ] T017 [P] [US1] Create `PartnershipOrganiserResponse.kt` DTO in `server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/`

### Repository Methods for User Story 1

- [ ] T018 [US1] Add `assignOrganiser(partnershipId, email)` method signature to `PartnershipRepository` interface in `server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/PartnershipRepository.kt`
- [ ] T019 [US1] Add `removeOrganiser(partnershipId)` method signature to `PartnershipRepository` interface in `server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/PartnershipRepository.kt`
- [ ] T020 [US1] Implement `assignOrganiser()` with validation in `PartnershipRepositoryExposed` in `server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/application/PartnershipRepositoryExposed.kt`
- [ ] T021 [US1] Implement `removeOrganiser()` in `PartnershipRepositoryExposed` in `server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/application/PartnershipRepositoryExposed.kt`

### API Routes for User Story 1

- [ ] T022 [US1] Add POST `/organiser` endpoint in `PartnershipRoutes.kt` in `server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/PartnershipRoutes.kt`
- [ ] T023 [US1] Add DELETE `/organiser` endpoint in `PartnershipRoutes.kt` in `server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/PartnershipRoutes.kt`

### Integration Tests for User Story 1 (REQUIRED - 80% Coverage)

- [ ] T024 [P] [US1] Create integration test file `PartnershipOrganiserIntegrationTest.kt` in `server/application/src/test/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/`
- [ ] T025 [P] [US1] Write integration test: Assign organiser successfully when user is org member with edit permission in `PartnershipOrganiserIntegrationTest.kt`
- [ ] T026 [P] [US1] Write integration test: Return 403 when user is not org member in `PartnershipOrganiserIntegrationTest.kt`
- [ ] T027 [P] [US1] Write integration test: Return 403 when user lacks edit permission in `PartnershipOrganiserIntegrationTest.kt`
- [ ] T028 [P] [US1] Write integration test: Return 404 when partnership not found in `PartnershipOrganiserIntegrationTest.kt`
- [ ] T029 [P] [US1] Write integration test: Return 404 when user not found in `PartnershipOrganiserIntegrationTest.kt`
- [ ] T030 [P] [US1] Write integration test: Return 401 when user lacks edit permission for organization in `PartnershipOrganiserIntegrationTest.kt`
- [ ] T031 [P] [US1] Write integration test: Remove organiser successfully in `PartnershipOrganiserIntegrationTest.kt`
- [ ] T032 [P] [US1] Write integration test: Organiser included in existing partnership GET endpoint in `PartnershipOrganiserIntegrationTest.kt`

### Validation for User Story 1

- [ ] T033 [US1] Run `./gradlew ktlintCheck detekt --no-daemon` from `server/` directory and fix violations
- [ ] T034 [US1] Run `./gradlew test --no-daemon` from `server/` directory and verify all tests pass
- [ ] T035 [US1] Manually test quickstart.md Scenario 1 (successful assignment) locally

**Checkpoint**: User Story 1 complete - organiser can be assigned and removed, all tests passing, quickstart validated

---

## Phase 4: User Story 2 - Change Assigned Organiser (Priority: P2)

**Goal**: Enable administrators to reassign partnerships to different organisers as team responsibilities change.

**Independent Test**: Assign organiser, change to different team member, verify update reflected and no data lost.

### Integration Tests for User Story 2 (REQUIRED)

- [ ] T036 [P] [US2] Write integration test: Update existing organiser assignment to different user in `PartnershipOrganiserIntegrationTest.kt`
- [ ] T037 [P] [US2] Write integration test: Concurrent assignment updates use last-write-wins strategy in `PartnershipOrganiserIntegrationTest.kt`
- [ ] T038 [P] [US2] Write integration test: Partnership reflects updated user details when organiser profile changes in `PartnershipOrganiserIntegrationTest.kt`

### Validation for User Story 2

- [ ] T039 [US2] Run `./gradlew test --no-daemon` from `server/` directory and verify all tests pass
- [ ] T040 [US2] Manually test quickstart.md Scenario 4 (update organiser assignment) locally
- [ ] T041 [US2] Manually test quickstart.md Scenario 5 (requires edit permission) locally

**Checkpoint**: User Story 2 complete - organiser reassignment works, concurrent updates handled, tests passing

---

## Phase 5: User Story 3 - Bulk Organiser Assignment (Priority: P3)

**Goal**: Enable administrators to assign same organiser to multiple partnerships simultaneously for efficient workload distribution.

**Independent Test**: Select multiple partnerships, assign same organiser to all in single operation, verify all assignments succeed.

**Note**: This user story is OUT OF SCOPE per spec.md (future enhancement). Included as placeholder for future implementation.

### Implementation for User Story 3 (DEFERRED)

- [ ] T042 [US3] DEFERRED: Design bulk assignment API endpoint (future enhancement)
- [ ] T043 [US3] DEFERRED: Implement bulk assignment repository method (future enhancement)
- [ ] T044 [US3] DEFERRED: Add bulk assignment route (future enhancement)

**Checkpoint**: User Story 3 deferred to future release per specification scope

---

## Phase 6: OpenAPI & Documentation

**Purpose**: Update API documentation and OpenAPI specification for organiser endpoints

- [ ] T045 [P] Add `AssignOrganiserRequest` schema reference to `components/schemas` in `server/application/src/main/resources/openapi/openapi.yaml`
- [ ] T046 [P] Add `PartnershipOrganiserResponse` schema reference to `components/schemas` in `server/application/src/main/resources/openapi/openapi.yaml`
- [ ] T047 Add POST `/orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/organiser` endpoint to `paths` in `server/application/src/main/resources/openapi/openapi.yaml`
- [ ] T048 Add DELETE `/orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/organiser` endpoint to `paths` in `server/application/src/main/resources/openapi/openapi.yaml`
- [ ] T049 Update existing partnership GET endpoint response to include `organiser` field in `server/application/src/main/resources/openapi/openapi.yaml`
- [ ] T050 Run `npm run validate` from `server/` directory to verify OpenAPI schema compliance

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Final validation and quality checks across all user stories

- [ ] T051 Run `./gradlew check --no-daemon` from `server/` directory (full validation: ktlint, detekt, tests, build)
- [ ] T052 Verify test coverage ≥80% for partnership organiser feature
- [ ] T053 Run all quickstart.md scenarios (Scenarios 1-5) and verify expected outcomes
- [ ] T054 [P] Update feature documentation with any implementation notes or edge cases discovered
- [ ] T055 Verify no console warnings or errors during local testing
- [ ] T056 Check database integrity: verify foreign key constraint on `partnerships.organiser_id` references `users.id`
- [ ] T057 Code cleanup: remove any debug logging, commented code, or TODOs
- [ ] T058 Final ktlintFormat: run `./gradlew ktlintFormat --no-daemon` from `server/` directory

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup (Phase 1) - BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends on Foundational (Phase 2) completion
- **User Story 2 (Phase 4)**: Depends on User Story 1 (Phase 3) - extends existing functionality
- **User Story 3 (Phase 5)**: DEFERRED - out of scope for this release
- **OpenAPI & Documentation (Phase 6)**: Depends on User Story 1 & 2 implementation
- **Polish (Phase 7)**: Depends on all implemented user stories

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - Core assignment/removal functionality, no dependencies on other stories
- **User Story 2 (P2)**: Depends on User Story 1 - Extends assignment with update/concurrent handling logic
- **User Story 3 (P3)**: DEFERRED - bulk assignment is future enhancement per spec scope

### Within Each User Story

1. **Contract tests FIRST** (must fail initially)
2. **DTOs** (request/response models)
3. **Repository interface signatures** (declare methods)
4. **Repository implementations** (business logic with validation)
5. **API routes** (wire up endpoints)
6. **Integration tests** (end-to-end validation)
7. **Validation** (ktlint, detekt, test execution)
8. **Manual testing** (quickstart scenarios)

### Parallel Opportunities

- **Phase 1 Setup**: All tasks T001-T004 marked [P] can run in parallel (different files)
- **Phase 3 Contract Tests**: Tasks T011-T015 marked [P] can run in parallel (different test methods)
- **Phase 3 DTOs**: Tasks T016-T017 marked [P] can run in parallel (different files)
- **Phase 3 Integration Tests**: Tasks T025-T032 marked [P] can run in parallel (different test methods)
- **Phase 4 Integration Tests**: Tasks T036-T038 marked [P] can run in parallel (different test methods)
- **Phase 6 OpenAPI**: Tasks T045-T046 marked [P] can run in parallel (different schema references)

### Critical Path (Minimum for MVP)

1. Phase 1: Setup (T001-T004)
2. Phase 2: Foundational (T005-T009)
3. Phase 3: User Story 1 (T010-T035) - REQUIRED for MVP
4. Phase 6: OpenAPI (T045-T050) - REQUIRED for deployment
5. Phase 7: Polish (T051-T058) - REQUIRED before merge

User Story 2 (Phase 4) can be done in a follow-up PR if time-constrained.

---

## Parallel Example: User Story 1 Contract Tests

```bash
# Launch all contract tests for User Story 1 together:
Task T011: "Write contract test: POST assigns organiser with valid request schema"
Task T012: "Write contract test: POST rejects missing email field"
Task T013: "Write contract test: POST rejects invalid email format"
Task T014: "Write contract test: POST returns valid PartnershipOrganiserResponse schema"
Task T015: "Write contract test: DELETE returns valid PartnershipOrganiserResponse with null organiser"

# All 5 tests can be written in parallel in the same test file
# but as separate test methods
```

---

## Parallel Example: User Story 1 Integration Tests

```bash
# Launch all integration tests for User Story 1 together:
Task T025: "Assign organiser successfully when user is org member with edit permission"
Task T026: "Return 403 when user is not org member"
Task T027: "Return 403 when user lacks edit permission"
Task T028: "Return 404 when partnership not found"
Task T029: "Return 404 when user not found"
Task T030: "Return 401 when user lacks edit permission for organization"
Task T031: "Remove organiser successfully"
Task T032: "Organiser included in existing partnership GET endpoint"

# All 8 tests can be written in parallel as separate test methods
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001-T004) - 4 tasks
2. Complete Phase 2: Foundational (T005-T009) - 5 tasks
3. Complete Phase 3: User Story 1 (T010-T035) - 26 tasks
4. Complete Phase 6: OpenAPI (T045-T050) - 6 tasks
5. Complete Phase 7: Polish (T051-T058) - 8 tasks
6. **STOP and VALIDATE**: Test User Story 1 independently, run quickstart scenarios
7. **TOTAL MVP: 49 tasks**

### Incremental Delivery

1. **Iteration 1 (MVP)**: Setup + Foundational + User Story 1 + OpenAPI + Polish
   - Delivers: Basic assign/remove organiser functionality
   - **Deploy/Demo**: Core value proposition ready
   
2. **Iteration 2**: Add User Story 2 (T036-T041)
   - Delivers: Reassignment and concurrent update handling
   - **Deploy/Demo**: Enhanced flexibility for team changes
   
3. **Future**: User Story 3 (bulk assignment) - deferred per scope

### Validation Checkpoints

- **After T009**: Verify database migration worked locally
- **After T015**: Verify all contract tests fail (TDD)
- **After T023**: Verify contract tests now pass
- **After T032**: Verify all integration tests pass
- **After T035**: Verify quickstart Scenario 1 works end-to-end
- **After T041**: Verify quickstart Scenarios 4-5 work
- **After T050**: Verify OpenAPI validation passes
- **After T058**: Verify full build passes with no violations

---

## Task Summary

- **Total Tasks**: 58 (excluding deferred US3)
- **MVP Tasks**: 49 (Setup + Foundational + US1 + OpenAPI + Polish)
- **Setup**: 4 tasks
- **Foundational**: 5 tasks (CRITICAL - blocks all stories)
- **User Story 1**: 26 tasks (contract tests, DTOs, repository, routes, integration tests, validation)
- **User Story 2**: 6 tasks (integration tests, validation)
- **User Story 3**: 3 tasks (DEFERRED - out of scope)
- **OpenAPI**: 6 tasks
- **Polish**: 8 tasks
- **Parallelizable Tasks**: 24 tasks marked [P]

---

## Notes

- **[P] tasks** = different files, no dependencies, can run in parallel
- **[Story] label** maps task to specific user story for traceability
- **Contract tests MUST be written FIRST** and fail before implementation (TDD)
- **Integration tests REQUIRED** for 80% coverage per constitution
- **All tasks include exact file paths** per project structure
- **ktlint/detekt MUST pass** with zero violations before merge
- **OpenAPI validation MUST pass** with `npm run validate`
- **Quickstart scenarios** provide manual validation checklist
- **User Story 3 is OUT OF SCOPE** - deferred to future release
- **No frontend changes** required per plan.md - backend-only feature

---

## Constitutional Compliance Checklist

- ✅ Contract tests required (Tasks T010-T015)
- ✅ 80% test coverage (Tasks T024-T032 integration tests)
- ✅ TDD approach (tests before implementation)
- ✅ Mock factories exist (verified in research.md)
- ✅ No repository dependencies (assignOrganiser uses direct entity access)
- ✅ JSON schema validation (Tasks T003-T004, schemas created)
- ✅ AuthorizedOrganisationPlugin (used in routes Task T022-T023)
- ✅ UUIDTable pattern (Task T005 extends existing table)
- ✅ datetime() not used (no timestamps per FR-015)
- ✅ Entity delegation pattern (Task T006 optionalReferencedOn)
- ✅ OpenAPI validation (Task T050 npm run validate)
- ✅ ktlint/detekt compliance (Tasks T033, T051, T058)
