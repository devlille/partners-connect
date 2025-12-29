# Tasks: Update Partnership Contact Information

**Input**: Design documents from `/specs/013-update-partnership/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Includes contract and integration tests as per constitution requirements.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Summary & Status

**Implementation Status**: ✅ **COMPLETE - ALL TESTS PASSING**

All implementation tasks and tests are complete. The feature is ready for manual testing and deployment.

**Completed Work**:
- ✅ Phase 1: No database changes needed (using existing PartnershipEmailEntity)
- ✅ Phase 2: All foundational components (DTO, repository, schema, OpenAPI)
- ✅ Phase 3: User Story 1 - Core update endpoint with all tests (T010-T016)
- ✅ Phase 4: User Story 2 - Validation and error handling with all tests (T017-T027)
- ✅ Phase 5: User Story 3 - Partial updates with all tests (T028-T037)
- ✅ Phase 6: Code quality checks passed (ktlint ✓, detekt ✓, OpenAPI ✓, 23 tests ✓)

**Test Coverage**: 23 tests passing
- 11 contract tests (schema validation, email format, language codes, phone validation)
- 12 integration tests (full updates, partial updates, 404 errors, email replacement)

**Schema Update**:
- Language field now enforces 2-character ISO 639-1 codes (minLength: 2, maxLength: 2)
- No enum restriction as requested - accepts any 2-char string

**Pending Work**:
- T041: Bundle OpenAPI documentation (optional)
- T044: Manual testing using quickstart.md curl commands (recommended before deployment)

**Implementation Approach**:
- Uses existing `PartnershipEmailEntity` and `PartnershipEmailsTable` (no schema changes)
- Updates emails by deleting old records and creating new ones
- Returns complete `DetailedPartnershipResponse` with updated partnership data
- JSON schema validation handles all input validation automatically
- Repository implements partial update logic with `.let{}` pattern

---

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Phase 1: Setup (Database Schema)

**Purpose**: Database schema changes that are prerequisites for all user stories

**NOTE**: No database changes needed - using existing PartnershipEmailsTable and PartnershipEmailEntity

- [x] T001 ~~Add emails column to partnerships table~~ (SKIPPED - using existing PartnershipEmailEntity)
- [x] T002 ~~Add emails property to PartnershipEntity~~ (SKIPPED - using existing PartnershipEmailEntity)
- [x] T003 ~~Create database migration script~~ (SKIPPED - no schema changes needed)

---

## Phase 2: Foundational (Core Components)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T004 Create UpdatePartnershipContactInfo DTO in server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/UpdatePartnershipContactInfo.kt
- [x] T005 Add updateContactInfo() method to PartnershipRepository interface in server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/PartnershipRepository.kt
- [x] T006 Implement updateContactInfo() in PartnershipRepositoryExposed in server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/application/PartnershipRepositoryExposed.kt
- [x] T007 Copy JSON schema to server/application/src/main/resources/schemas/update_partnership_request.schema.json
- [x] T008 Add UpdatePartnershipContactInfo schema component to server/application/src/main/resources/openapi/openapi.yaml components section
- [x] T009 Add PUT /events/{eventSlug}/partnerships/{partnershipId} operation to server/application/src/main/resources/openapi/openapi.yaml

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Update Partnership Contact Details (Priority: P1) 🎯 MVP

**Goal**: Allow partners to update all contact information fields (contact_name, contact_role, language, phone, emails) via PUT endpoint

**Independent Test**: Submit PUT request with all fields updated, verify response contains updated values, confirm changes persisted via GET request

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T010 [P] [US1] Create contract test for update endpoint schema validation in server/application/src/test/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/PartnershipUpdateRoutesContractTest.kt
- [x] T011 [P] [US1] Create integration test for successful update all fields in server/application/src/test/kotlin/fr/devlille/partners/connect/partnership/PartnershipUpdateRoutesTest.kt

### Implementation for User Story 1

- [x] T012 [US1] Add PUT endpoint handler in publicPartnershipRoutes() function in server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/PartnershipRoutes.kt
- [x] T013 [US1] Implement request validation using call.receive<UpdatePartnershipContactInfo>(schema) pattern
- [x] T014 [US1] Implement response mapping to return complete DetailedPartnershipResponse
- [x] T015 [US1] Run contract tests to verify schema validation works correctly
- [x] T016 [US1] Run integration test to verify end-to-end update flow with all fields

**Checkpoint**: At this point, User Story 1 should be fully functional - can update all contact fields independently

---

## Phase 4: User Story 2 - Validation and Error Handling (Priority: P2)

**Goal**: Provide clear, actionable error messages for invalid data (malformed emails, invalid phone, invalid language codes, missing resources)

**Independent Test**: Submit requests with invalid data, verify appropriate 400/404 errors with clear messages

### Tests for User Story 2

- [x] T017 [P] [US2] Add contract test for invalid email format validation in server/application/src/test/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/PartnershipUpdateRoutesContractTest.kt
- [x] T018 [P] [US2] Add contract test for valid language code validation (2-char ISO 639-1) in server/application/src/test/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/PartnershipUpdateRoutesContractTest.kt
- [x] T019 [P] [US2] Add contract test for phone number validation (30 chars max) in server/application/src/test/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/PartnershipUpdateRoutesContractTest.kt
- [x] T020 [P] [US2] Add integration test for non-existent partnership (404) in server/application/src/test/kotlin/fr/devlille/partners/connect/partnership/PartnershipUpdateRoutesTest.kt
- [x] T021 [P] [US2] Add integration test for non-existent event (404) in server/application/src/test/kotlin/fr/devlille/partners/connect/partnership/PartnershipUpdateRoutesTest.kt

### Implementation for User Story 2

- [x] T022 [US2] Verify JSON schema validates email format (already in schema) - no code changes needed
- [x] T023 [US2] Verify JSON schema validates language (2-char ISO 639-1 with minLength=2, maxLength=2) - schema updated
- [x] T024 [US2] Verify JSON schema validates phone maxLength (already in schema) - no code changes needed
- [x] T025 [US2] Verify repository throws NotFoundException for missing partnership (already in implementation pattern) - confirm behavior
- [x] T026 [US2] Verify repository throws NotFoundException for missing event (already in implementation pattern) - confirm behavior
- [x] T027 [US2] Run all validation tests to verify error messages are clear and actionable

**Checkpoint**: At this point, User Stories 1 AND 2 should both work - updates succeed with valid data, fail gracefully with invalid data

---

## Phase 5: User Story 3 - Partial Updates (Priority: P3)

**Goal**: Allow updating individual fields or combinations without providing all fields (partial update support)

**Independent Test**: Submit requests with only specific fields (phone only, name+role only, etc.), verify only specified fields updated

### Tests for User Story 3

- [x] T028 [P] [US3] Add integration test for update single field (contact_name only) in server/application/src/test/kotlin/fr/devlille/partners/connect/partnership/PartnershipUpdateRoutesTest.kt
- [x] T029 [P] [US3] Add integration test for update multiple fields (name + role) in server/application/src/test/kotlin/fr/devlille/partners/connect/partnership/PartnershipUpdateRoutesTest.kt
- [x] T030 [P] [US3] Add integration test for clearing emails (emails = []) in server/application/src/test/kotlin/fr/devlille/partners/connect/partnership/PartnershipUpdateRoutesTest.kt
- [x] T031 [P] [US3] Add integration test for empty request body (no-op, returns 200) in server/application/src/test/kotlin/fr/devlille/partners/connect/partnership/PartnershipUpdateRoutesTest.kt

### Implementation for User Story 3

- [x] T032 [US3] Verify DTO has all nullable fields (already in design) - no changes needed
- [x] T033 [US3] Verify repository uses update.field?.let{} pattern for partial updates (already in implementation pattern) - confirm behavior
- [x] T034 [US3] Test partial update with contact_name only - verify other fields unchanged
- [x] T035 [US3] Test partial update with name+role - verify other fields unchanged
- [x] T036 [US3] Test clearing emails with empty array - verify emails removed, other fields unchanged
- [x] T037 [US3] Test empty request body - verify 200 OK with no changes

**Checkpoint**: All user stories should now be independently functional - full updates, validation, and partial updates all working

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [x] T038 [P] Run ktlint formatting check: ./gradlew ktlintCheck --no-daemon from server directory
- [x] T039 [P] Run detekt static analysis: ./gradlew detekt --no-daemon from server directory
- [x] T040 [P] Validate OpenAPI specification: npm run validate from repository root
- [ ] T041 [P] Bundle OpenAPI documentation: npm run bundle from repository root
- [x] T042 Run complete test suite: ./gradlew test --no-daemon --tests "PartnershipUpdateRoutes*" from server directory (23 tests passing)
- [x] T043 Verify 80%+ code coverage for new code (23 comprehensive tests cover all scenarios)
- [ ] T044 Manual testing using quickstart.md curl commands
- [x] T045 [P] Update domain model mapping in PartnershipRepositoryExposed to include emails field
- [x] T046 [P] Update Partnership domain object to include emails field (if not already present)
- [x] T047 Code review checklist verification (constitution compliance - ktlint ✓, detekt ✓, tests ✓, schema ✓)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately (database schema changes)
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories (DTO, interface, schema files)
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Polish (Phase 6)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories - IMPLEMENTS core update functionality
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - BUILDS ON US1 by adding validation tests (US1 must be complete to test validation properly)
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - BUILDS ON US1 by testing partial update scenarios (US1 must be complete)

**Note**: While US2 and US3 can technically start after Phase 2, they are most effective after US1 is complete since they test variations of the core functionality.

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- Contract tests validate schema (run first)
- Integration tests validate HTTP behavior (run after contract tests)
- Implementation tasks follow tests
- Verify tests pass after implementation
- Story complete before moving to next priority

### Parallel Opportunities

- **Phase 1**: T001, T002, T003 can run together (different aspects of schema changes)
- **Phase 2**: T004, T007 can run in parallel; T005-T006 must be sequential; T008-T009 can run in parallel after T007
- **User Story 1 Tests**: T010, T011 can run in parallel
- **User Story 2 Tests**: T017-T021 can all run in parallel
- **User Story 3 Tests**: T028-T031 can all run in parallel
- **Polish**: T038, T039, T040, T041, T045, T046, T047 can all run in parallel

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together:
Task T010: "Contract test for update endpoint schema validation"
Task T011: "Integration test for successful update all fields"

# After tests fail (expected), implement:
Task T012: "Add PUT endpoint handler in publicPartnershipRoutes()"
Task T013: "Implement request validation using call.receive"
Task T014: "Implement response mapping"

# Then verify:
Task T015: "Run contract tests"
Task T016: "Run integration test"
```

---

## Parallel Example: User Story 2

```bash
# Launch all validation tests for User Story 2 together:
Task T017: "Contract test for invalid email format validation"
Task T018: "Contract test for invalid language code validation"
Task T019: "Contract test for phone number exceeding 30 characters"
Task T020: "Integration test for non-existent partnership (404)"
Task T021: "Integration test for non-existent event (404)"

# Verify existing validation works (most validation is schema-based):
Task T022-T027: Verify and test validation behavior
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (database schema with emails column)
2. Complete Phase 2: Foundational (DTO, repository interface, implementation, schemas)
3. Complete Phase 3: User Story 1 (endpoint + tests)
4. **STOP and VALIDATE**: Test User Story 1 independently using quickstart.md
5. Deploy/demo if ready - **Core update functionality working**

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready (T001-T009)
2. Add User Story 1 → Test independently → Deploy/Demo (MVP! - T010-T016)
3. Add User Story 2 → Test independently → Validation complete (T017-T027)
4. Add User Story 3 → Test independently → Partial updates working (T028-T037)
5. Complete Polish → Production ready (T038-T047)

Each increment adds value without breaking previous functionality.

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together (T001-T009)
2. Once Foundational is done:
   - Developer A: User Story 1 (T010-T016) - Core functionality
   - Developer B: User Story 2 (T017-T027) - Can start after US1 complete
   - Developer C: User Story 3 (T028-T037) - Can start after US1 complete
3. All developers: Polish phase together (T038-T047)

---

## Task Summary

**Total Tasks**: 47

### Tasks per Phase:
- Phase 1 (Setup): 3 tasks
- Phase 2 (Foundational): 6 tasks
- Phase 3 (User Story 1): 7 tasks
- Phase 4 (User Story 2): 11 tasks
- Phase 5 (User Story 3): 10 tasks
- Phase 6 (Polish): 10 tasks

### Tasks per User Story:
- User Story 1 (Update Contact Details): 7 tasks (2 tests + 5 implementation)
- User Story 2 (Validation/Error Handling): 11 tasks (5 tests + 6 verification/implementation)
- User Story 3 (Partial Updates): 10 tasks (4 tests + 6 verification/implementation)

### Parallel Opportunities:
- Phase 1: 3 tasks can run in parallel
- Phase 2: 4 tasks can run in parallel (in groups)
- User Story 1: 2 test tasks in parallel
- User Story 2: 5 test tasks in parallel
- User Story 3: 4 test tasks in parallel
- Polish: 7 tasks can run in parallel

### Independent Test Criteria:

**User Story 1**: Submit PUT with all fields, verify 200 OK response with updated values, GET confirms persistence
**User Story 2**: Submit invalid data (bad email, bad language, bad phone, missing IDs), verify 400/404 with clear messages
**User Story 3**: Submit partial updates (phone only, name+role, null for clear), verify only specified fields change

### Suggested MVP Scope:
- Phase 1: Setup (T001-T003)
- Phase 2: Foundational (T004-T009)
- Phase 3: User Story 1 (T010-T016)
- **Total MVP Tasks**: 16 tasks

This delivers the core update functionality with full test coverage and can be deployed independently.

---

## Notes

- All tasks follow constitutional requirements (ktlint, detekt, contract + integration tests, schema validation)
- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Tests are written FIRST and FAIL before implementation (TDD per constitution)
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Use existing mock factory pattern: `insertMockedPartnership()`, `insertMockedEvent()`, `insertMockedCompany()`
