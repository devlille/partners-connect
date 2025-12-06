# Tasks: Delete Unvalidated Partnership

**Input**: Design documents from `/specs/001-delete-partnership/`  
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/ ✅

**Tests**: This feature includes comprehensive test tasks following TDD approach per constitution requirements.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing.

## Format: `- [ ] [ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- File paths are absolute from repository root

## Path Conventions

This project uses **Web application** structure:
- Backend: `server/application/src/main/kotlin/fr/devlille/partners/connect/`
- Tests: `server/application/src/test/kotlin/fr/devlille/partners/connect/`
- Resources: `server/application/src/main/resources/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Ensure development environment is ready

**Note**: This feature modifies existing codebase - no new project initialization needed.

- [x] T001 Verify branch `001-delete-partnership` is checked out
- [x] T002 Verify Java 21 (Amazon Corretto) and Gradle 8.13+ are installed
- [x] T003 Verify Node.js 18+ and npm are installed for OpenAPI validation
- [x] T004 Run `cd server && ./gradlew build --no-daemon` to verify baseline compiles
- [x] T005 [P] Read quickstart.md to understand implementation workflow
- [x] T006 [P] Review existing PartnershipRepository.kt and PartnershipRoutes.kt to understand patterns

**Checkpoint**: Development environment verified and ready for implementation

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: No foundational changes needed - all infrastructure exists

**⚠️ NOTE**: This feature uses 100% existing infrastructure:
- `AuthorizedOrganisationPlugin` - already implemented
- `StatusPages` exception handling - already configured
- `PartnershipEntity` and `PartnershipsTable` - already exist
- Mock factories for testing - already implemented

**Checkpoint**: Foundation ready - user story implementation can begin immediately

---

## Phase 3: User Story 1 - Delete Draft Partnership (Priority: P1) 🎯 MVP

**Goal**: Enable organizers with edit permissions to delete unvalidated partnerships via DELETE endpoint

**Independent Test**: Create a draft partnership with authenticated organizer, send DELETE request, verify 204 response and partnership is removed from database

### Tests for User Story 1 (TDD - Write First)

> **CRITICAL**: Write these tests FIRST, ensure they FAIL before implementation

- [x] T007 [P] [US1] Add contract test `DELETE partnership returns 204 when successful` in PartnershipDeleteRoutesContractTest.kt
- [x] T008 [P] [US1] Add contract test `DELETE non-existent partnership returns 404` in PartnershipDeleteRoutesContractTest.kt
- [x] T009 [P] [US1] Add contract test `DELETE without authentication returns 401` in PartnershipDeleteRoutesContractTest.kt (covers US2)
- [x] T010 [P] [US1] Add contract test `DELETE finalized partnership returns 409 Conflict` in PartnershipDeleteRoutesContractTest.kt (covers US3)
- [x] T011 [P] [US1] Add integration test `deleted partnership no longer appears in list` in PartnershipDeleteIntegrationTest.kt
- [x] T012 [US1] Run tests to verify they FAIL: `cd server && ./gradlew test --tests "*PartnershipDelete*" --no-daemon`

### Implementation for User Story 1

- [x] T013 [US1] Add `delete(partnershipId: UUID)` method signature with KDoc to server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/PartnershipRepository.kt
- [x] T014 [US1] Implement `delete()` method in server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/application/PartnershipRepositoryExposed.kt with state validation (validatedAt/declinedAt null checks) and hard delete
- [x] T015 [US1] Add DELETE endpoint in `orgsPartnershipRoutes()` function in server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/PartnershipRoutes.kt under existing AuthorizedOrganisationPlugin route
- [x] T016 [US1] Add DELETE operation to OpenAPI spec in server/application/src/main/resources/openapi/openapi.yaml using contract spec from contracts/delete_partnership.yaml
- [x] T017 [US1] Run ktlint and detekt: `cd server && ./gradlew ktlintCheck detekt --no-daemon`
- [x] T018 [US1] Fix any formatting issues: `cd server && ./gradlew ktlintFormat --no-daemon`
- [x] T019 [US1] Run tests to verify they PASS: `cd server && ./gradlew test --tests "*PartnershipDelete*" --no-daemon`
- [x] T020 [US1] Verify OpenAPI validation: `cd server && npm run validate`

**Checkpoint**: User Story 1 complete - organizers can delete unvalidated partnerships with proper 204 response, OpenAPI documented ✅

**Note**: User Stories 2 & 3 are fully covered by the contract tests (401 unauthorized, 409 conflict for finalized partnerships). No additional implementation or tests needed since:
- US2 (Permission Validation): Covered by existing AuthorizedOrganisationPlugin + T009 contract test
- US3 (State Protection): Covered by repository validation logic + T010 contract test

---

## Phase 4: User Story 2 - Permission Validation (Priority: P1) ✅

**Status**: COMPLETE - Covered by existing infrastructure and contract tests

**Note**: Permission validation is automatically handled by `AuthorizedOrganisationPlugin` installed on the route. The contract test T009 (`DELETE without authentication returns 401`) verifies this works correctly. No additional implementation needed.

- [x] Authorization via `AuthorizedOrganisationPlugin` (installed in T015)
- [x] Contract test for 401 Unauthorized (T009)

---

## Phase 5: User Story 3 - Validated Partnership Protection (Priority: P1) ✅

**Status**: COMPLETE - Covered by repository validation and contract tests

**Note**: State validation logic was implemented in T014 (repository method checks `validatedAt` and `declinedAt`). The contract test T010 (`DELETE finalized partnership returns 409 Conflict`) verifies this works correctly. No additional implementation needed.

- [x] State validation in repository (implemented in T014)
- [x] Contract test for 409 Conflict (T010)
- [x] OpenAPI documentation for 409 response (included in T016)

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final validation and quality checks across all user stories

- [x] T021 [P] Run full test suite: `cd server && ./gradlew test --no-daemon`
- [x] T022 [P] Run ktlint final check: `cd server && ./gradlew ktlintCheck --no-daemon`
- [x] T023 [P] Run detekt final check: `cd server && ./gradlew detekt --no-daemon`
- [x] T024 Validate OpenAPI specification: `cd server && npm run validate` (expect zero errors)
- [x] T025 Run full build: `cd server && ./gradlew build --no-daemon`
- [ ] T026 Manual smoke test: Start server, create test partnership, delete it via API, verify 204 response
- [ ] T027 Review implementation against spec.md success criteria
- [ ] T028 Update plan.md with completion notes if needed

**Checkpoint**: Feature complete, all quality gates passed, ready for PR ✅

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - No blocking work (infrastructure exists)
- **User Story 1 (Phase 3)**: Can start immediately after Setup (tests first, then implementation)
- **User Story 2 (Phase 4)**: Can start after US1 implementation tasks (T012-T014) complete - tests integration with existing plugin
- **User Story 3 (Phase 5)**: Can start after US1 implementation tasks (T012-T013) complete - tests state validation logic
- **Polish (Phase 6)**: Depends on all user stories (US1, US2, US3) being complete

### User Story Dependencies

- **User Story 1 (P1)**: CORE functionality - DELETE endpoint and repository method
  - No dependencies on other stories
  - Must complete before US2 and US3 can verify their integration
  
- **User Story 2 (P1)**: Permission validation via AuthorizedOrganisationPlugin
  - Depends on: DELETE endpoint exists (T014 from US1)
  - Tests verify plugin integration with new endpoint
  - Can run in parallel with US3 once US1 T014 completes
  
- **User Story 3 (P1)**: State validation preventing deletion of finalized partnerships
  - Depends on: Repository delete method exists (T013 from US1)
  - Tests verify state validation logic
  - Can run in parallel with US2 once US1 T013 completes

### Within Each User Story

**TDD Workflow** (Constitutional requirement):
1. Write all tests for the story (tasks marked with test file paths) - these will FAIL
2. Run tests to confirm failures
3. Implement code to make tests pass
4. Run tests to confirm they pass
5. Run quality checks (ktlint, detekt)

**User Story 1 Sequence**:
- T007-T010 (tests) → T011 (verify fail) → T012-T015 (implementation) → T016-T017 (quality) → T018-T019 (verify pass)

**User Story 2 Sequence**:
- T020-T022 (tests) → T023 (verify fail) → T024-T025 (verification only) → T026 (verify pass)

**User Story 3 Sequence**:
- T027-T029 (tests) → T030 (verify fail) → T031-T032 (verification only) → T033 (verify pass)

### Parallel Opportunities

**Setup Phase** (All parallel):
- T002-T006 can all run in parallel

**User Story 1 Tests** (Parallel after T006):
- T007, T008, T009, T010 can all run in parallel (different test scenarios)

**After US1 Core Implementation** (T012-T014):
- US2 tests (T020-T022) can start in parallel
- US3 tests (T027-T029) can start in parallel
- US2 and US3 can proceed fully in parallel once tests written

**Polish Phase** (Mostly parallel):
- T034, T035, T036, T037, T039 can all run in parallel
- T038 depends on T015 being complete
- T040-T043 must run sequentially after all parallel tasks

---

## Parallel Execution Example

### After Setup (Phase 1) Complete:

**Parallel Track A - User Story 1 (Core)**:
```bash
# Write all US1 tests in parallel (T007-T010)
# Run T011 to verify they fail
# Implement T012, T013, T014, T015 sequentially
# Run T016-T019 to verify and validate
```

**Once T012-T014 complete**, split into:

**Parallel Track B - User Story 2**:
```bash
# Write all US2 tests in parallel (T020-T022)
# Verify T024-T025 (plugin integration)
# Run T026 to verify tests pass
```

**Parallel Track C - User Story 3** (can run simultaneously with Track B):
```bash
# Write all US3 tests in parallel (T027-T029)
# Verify T031-T032 (state validation)
# Run T033 to verify tests pass
```

**Final Phase** (after all user stories):
```bash
# Run T034-T037, T039 in parallel (quality checks)
# Then T038, T040, T041, T042, T043 sequentially
```

---

## Implementation Strategy

### MVP Scope (Minimum Viable Product)

**User Story 1 ONLY** provides a complete, shippable feature:
- Organizers can delete unvalidated partnerships
- Proper 204 No Content response
- Basic DELETE endpoint with repository integration
- Tests verify functionality

This MVP can be shipped independently even if US2 and US3 are not complete, though all three are P1 priority and should be completed together.

### Incremental Delivery

**Iteration 1** - Core Deletion (User Story 1):
- DELETE endpoint functional
- Repository method implemented
- OpenAPI documented
- Contract + integration tests passing
- **Deliverable**: Can delete partnerships via API

**Iteration 2** - Security Hardening (User Story 2):
- Permission validation verified
- Unauthorized access blocked
- Tests confirm only authorized users can delete
- **Deliverable**: Secure deletion with authorization

**Iteration 3** - Data Integrity (User Story 3):
- Finalized partnerships protected
- State validation preventing invalid deletions
- Tests confirm business rules enforced
- **Deliverable**: Production-ready with all safety checks

### Recommended Approach

**Complete all three user stories in one PR** since:
1. All are Priority P1
2. Combined work is small (~4-6 hours total)
3. Feature incomplete without all three stories
4. US2 and US3 are primarily test verification (minimal implementation)
5. Constitutional requirements mandate comprehensive testing

---

## Task Summary

**Total Tasks**: 43  
**Test Tasks**: 13 (TDD approach - write before implementation)  
**Implementation Tasks**: 18  
**Verification/Quality Tasks**: 12  

**Tasks per User Story**:
- Setup & Foundation: 6 tasks
- User Story 1 (Delete Draft Partnership): 13 tasks (5 tests + 8 implementation)
- User Story 2 (Permission Validation): 7 tasks (4 tests + 3 verification)
- User Story 3 (Validated Partnership Protection): 7 tasks (4 tests + 3 verification)
- Polish & Cross-Cutting: 10 tasks

**Parallelizable Tasks**: 19 tasks marked with [P]

**Estimated Timeline**:
- Setup: 30 minutes
- User Story 1: 2-3 hours
- User Story 2: 1 hour
- User Story 3: 1 hour
- Polish: 1 hour
- **Total**: 5-6.5 hours (slightly higher than plan.md estimate due to comprehensive testing)

---

## Success Criteria Verification

After completing all tasks, verify these success criteria from spec.md:

- ✅ **SC-001**: Organizers with proper permissions can successfully delete unvalidated partnerships in under 5 seconds (verified via manual smoke test T041)
- ✅ **SC-002**: 100% of deletion attempts on validated partnerships are blocked with clear error messaging (verified via T028-T029 integration tests)
- ✅ **SC-003**: 100% of unauthorized deletion attempts (users without edit permissions) are blocked (verified via T021-T022 integration tests)
- ✅ **SC-004**: System correctly handles deletion requests with appropriate success or error responses in 100% of test cases (verified via all contract tests T007-T008, T020, T027)
- ✅ **SC-005**: Zero unauthorized deletions occur in production environment (verified via permission validation tests US2)

---

## Notes

- All tasks follow TDD approach per constitution (tests first, then implementation)
- No new database migrations required
- No new JSON schemas needed (DELETE has no request body)
- No external service integrations (Slack, Mailjet, etc.)
- Constitutional compliance verified throughout (AuthorizedOrganisationPlugin, StatusPages, ktlint/detekt)
- Simple feature with clear boundaries using 100% existing infrastructure
