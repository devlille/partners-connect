# Tasks: Synchronize Pack Options

**Feature**: 012-sync-pack-options  
**Branch**: `012-sync-pack-options`  
**Input**: Design documents from `/specs/012-sync-pack-options/`  
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Contract tests are REQUIRED per project constitution (TDD approach)

**Organization**: Tasks grouped by user story priority to enable independent implementation

---

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- File paths use absolute paths from repository root

---

## Phase 1: Setup (Prerequisite Verification)

**Purpose**: Verify development environment and documentation are ready

- [X] T001 Verify on branch `012-sync-pack-options` via `git branch --show-current`
- [X] T002 Review spec.md to understand user stories and requirements
- [X] T003 Review quickstart.md for implementation guide and patterns
- [X] T004 Review contracts/sync_pack_options_contract.md for test scenarios

**Checkpoint**: Documentation reviewed - ready to begin test-driven development

---

## Phase 2: User Story 1 - Complete Pack Configuration Update (Priority: P1) 🎯 MVP

**Goal**: Enable organizers to synchronize pack options in a single request, removing old options and adding new ones atomically

**Independent Test**: Submit a complete options configuration and verify pack contains exactly those options (no orphaned options remain)

### Contract Tests for User Story 1 (TDD - MUST WRITE FIRST)

> **CRITICAL**: Write these tests FIRST, verify they FAIL, then implement

- [X] T005 [P] [US1] Update existing test that expects 409 error to expect 201 (idempotent behavior) in `server/application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/SponsoringPackRoutesTest.kt`
- [X] T006 [P] [US1] Write contract test: Replace all options scenario (Given: pack with A,B → Sync to: C,D → Then: pack has C,D only) in `server/application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/SponsoringPackRoutesTest.kt`
- [X] T007 [P] [US1] Write contract test: Partial overlap scenario (Given: pack with A,B,C → Sync to: B,D → Then: pack has B,D only) in `server/application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/SponsoringPackRoutesTest.kt`
- [X] T008 [P] [US1] Write contract test: Empty configuration scenario (Given: pack with options → Sync to: empty → Then: pack has no options) in `server/application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/SponsoringPackRoutesTest.kt`
- [X] T009 [US1] Run tests with `./gradlew test --no-daemon --tests "SponsoringPackRoutesTest"` and verify they FAIL (TDD confirmation)

### Implementation for User Story 1

- [X] T010 [US1] Implement synchronization logic in `attachOptionsToPack()` method: Add delete operation for removed options in `server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/application/OptionRepositoryExposed.kt`
- [X] T011 [US1] Implement synchronization logic in `attachOptionsToPack()` method: Query currently attached options to determine add vs update in `server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/application/OptionRepositoryExposed.kt`
- [X] T012 [US1] Implement synchronization logic in `attachOptionsToPack()` method: Process required options (insert new, update existing) in `server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/application/OptionRepositoryExposed.kt`
- [X] T013 [US1] Implement synchronization logic in `attachOptionsToPack()` method: Process optional options (insert new, update existing) in `server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/application/OptionRepositoryExposed.kt`
- [X] T014 [US1] Remove "already attached" validation check (now idempotent) in `server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/application/OptionRepositoryExposed.kt`
- [X] T015 [US1] Add KDoc documentation to modified `attachOptionsToPack()` method explaining synchronization behavior in `server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/application/OptionRepositoryExposed.kt`
- [X] T016 [US1] Run tests with `./gradlew test --no-daemon --tests "SponsoringPackRoutesTest"` and verify they PASS

### Validation for User Story 1

- [X] T017 [US1] Verify database state after sync operations matches submitted configuration (query PackOptionsTable directly in test)
- [X] T018 [US1] Verify SC-001: Single API request updates complete configuration
- [X] T019 [US1] Verify SC-002: Pack configurations reflect exactly submitted state with 100% accuracy

**Checkpoint**: User Story 1 complete - core synchronization functionality works independently

---

## Phase 3: User Story 2 - Option Requirement Status Update (Priority: P2)

**Goal**: Enable organizers to change option status (required ↔ optional) by simply including it in the appropriate list

**Independent Test**: Submit configuration where a previously required option is now optional, verify status changes correctly

### Contract Tests for User Story 2 (TDD - MUST WRITE FIRST)

- [X] T020 [P] [US2] Write contract test: Required to optional transition (Given: pack with option A as required → Sync with A in optional → Then: A is optional) in `server/application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/SponsoringPackRoutesTest.kt`
- [X] T021 [P] [US2] Write contract test: Optional to required transition (Given: pack with option B as optional → Sync with B in required → Then: B is required) in `server/application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/SponsoringPackRoutesTest.kt`
- [X] T022 [US2] Run tests and verify they FAIL before implementation

### Implementation for User Story 2

- [X] T023 [US2] Verify implementation from US1 already handles requirement status updates (update logic in steps 5-6 of algorithm)
- [X] T024 [US2] Run tests with `./gradlew test --no-daemon --tests "SponsoringPackRoutesTest"` and verify requirement status update tests PASS

### Validation for User Story 2

- [X] T025 [US2] Verify option status changes are persisted correctly in database
- [X] T026 [US2] Verify status changes work for both directions (required ↔ optional)

**Checkpoint**: User Story 2 complete - requirement status updates work independently

---

## Phase 4: User Story 3 - Add New Options Without Manual Cleanup (Priority: P2)

**Goal**: Enable complete replacement of pack configuration without prior deletion requests

**Independent Test**: Add new options to pack with existing options, verify final state matches submitted configuration without separate delete calls

### Contract Tests for User Story 3 (TDD - MUST WRITE FIRST)

- [X] T027 [P] [US3] Write contract test: Complete replacement scenario (Given: pack with A,B → Sync to: C → Then: pack has C only, no separate delete needed) in `server/application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/SponsoringPackRoutesTest.kt`
- [X] T028 [P] [US3] Write contract test: Idempotency scenario (Given: pack with A,B → Sync to: A,B → Then: 201 success, pack unchanged) in `server/application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/SponsoringPackRoutesTest.kt`
- [X] T029 [US3] Run tests and verify they FAIL before implementation

### Implementation for User Story 3

- [X] T030 [US3] Verify implementation from US1 already provides idempotent behavior (no duplicate check, processes all configurations)
- [X] T031 [US3] Run tests with `./gradlew test --no-daemon --tests "SponsoringPackRoutesTest"` and verify idempotency tests PASS

### Validation for User Story 3

- [X] T032 [US3] Verify multiple sync requests with same configuration produce same result
- [X] T033 [US3] Verify SC-001: Single request replaces entire configuration

**Checkpoint**: User Story 3 complete - all user stories are independently functional

---

## Phase 5: Error Handling & Validation (Cross-Story Concerns)

**Goal**: Ensure proper error responses for all edge cases across all user stories

### Contract Tests for Error Scenarios

- [X] T034 [P] Write contract test: 409 Conflict when option in both required and optional lists in `server/application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/SponsoringPackRoutesTest.kt`
- [X] T035 [P] Write contract test: 403 Forbidden when options don't belong to event in `server/application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/SponsoringPackRoutesTest.kt`
- [X] T036 [P] Write contract test: 404 Not Found when pack doesn't exist in `server/application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/SponsoringPackRoutesTest.kt`
- [X] T037 [P] Write contract test: 404 Not Found when option doesn't exist in `server/application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/SponsoringPackRoutesTest.kt`
- [X] T038 Run error scenario tests and verify existing validation logic handles all cases correctly

### Integration Tests for Atomicity

- [X] T039 Write integration test: Verify transaction rollback on validation failure (pack state unchanged after 404 error) in `server/application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/SponsoringPackRoutesTest.kt`
- [X] T040 Write integration test: Verify no partial states occur (all changes applied or none) in `server/application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/SponsoringPackRoutesTest.kt`

### Validation

- [X] T041 Verify SC-004: Validation errors identify specific problem option IDs
- [X] T042 Verify SC-005: Zero partial states occur (atomic operations)
- [X] T043 Verify FR-006 through FR-011: All error handling requirements met

**Checkpoint**: Error handling complete - all edge cases handled correctly

---

## Phase 6: Documentation & API Contract Updates

**Goal**: Update OpenAPI documentation and ensure API contract clarity

- [X] T044 Update operation summary from "Create sponsoring option" to "Synchronize sponsoring pack options" in `server/application/src/main/resources/openapi/openapi.yaml`
- [X] T045 Update operation description to document synchronization behavior (removes, adds, updates) in `server/application/src/main/resources/openapi/openapi.yaml`
- [X] T046 Add detailed description of atomic operation and idempotency in `server/application/src/main/resources/openapi/openapi.yaml`
- [X] T047 Verify all error responses (400, 401, 403, 404, 409, 500) are documented in `server/application/src/main/resources/openapi/openapi.yaml`
- [X] T048 Run `npm run validate` from repository root to verify OpenAPI spec validity

**Checkpoint**: Documentation updated - API contract clearly documented

---

## Phase 7: Code Quality & Final Validation

**Goal**: Ensure all constitutional requirements are met

### Code Quality Checks

- [X] T049 Run `./gradlew ktlintCheck --no-daemon` from server directory and verify zero violations
- [X] T050 If ktlint violations found, run `./gradlew ktlintFormat --no-daemon` to auto-fix
- [X] T051 Run `./gradlew detekt --no-daemon` from server directory and verify zero violations
- [X] T052 Verify test coverage ≥ 80% for modified code via coverage report

### Full Test Suite

- [X] T053 Run full test suite with `./gradlew test --no-daemon` and verify all tests pass
- [X] T054 Verify no test regressions in other sponsoring module tests
- [X] T055 Run `./gradlew check --no-daemon` to verify all quality gates pass

### Success Criteria Validation

- [X] T056 Verify SC-001: Organizers can update complete configuration in single API request
- [X] T057 Verify SC-002: Pack configurations reflect exactly submitted state (100% accuracy)
- [X] T058 Verify SC-003: Synchronization completes within 500ms for 50 options (measure in test)
- [X] T059 Verify SC-004: Validation errors provide clear feedback about problem option IDs
- [X] T060 Verify SC-005: Zero partial states occur (operations are atomic)

### Functional Requirements Validation

- [X] T061 Verify FR-001 through FR-011: All functional requirements implemented correctly
- [X] T062 Review quickstart.md validation checklist and verify all items checked

**Checkpoint**: All quality gates passed - ready for commit

---

## Phase 8: Commit & Documentation

**Goal**: Finalize implementation and prepare for PR

- [X] T063 Review all modified files for code quality and consistency
- [X] T064 Ensure no TODO comments without GitHub issue references
- [X] T065 Commit changes with message: "feat(sponsoring): implement pack options synchronization"
- [X] T066 Push to remote branch: `git push origin 012-sync-pack-options`
- [X] T067 Update CHANGELOG or release notes if applicable

**Checkpoint**: Implementation complete and committed

---

## Dependencies & Execution Order

### Phase Dependencies

1. **Phase 1 (Setup)**: No dependencies - start immediately
2. **Phase 2 (US1)**: Depends on Phase 1 completion
3. **Phase 3 (US2)**: Depends on Phase 2 completion (builds on US1 implementation)
4. **Phase 4 (US3)**: Depends on Phase 2 completion (verifies US1 behavior)
5. **Phase 5 (Error Handling)**: Depends on Phases 2-4 completion
6. **Phase 6 (Documentation)**: Can start after Phase 2, should complete after Phase 5
7. **Phase 7 (Quality)**: Depends on Phases 2-6 completion
8. **Phase 8 (Commit)**: Depends on Phase 7 completion

### User Story Dependencies

- **US1 (P1)**: Independent - core synchronization logic
- **US2 (P2)**: Depends on US1 - validates status update feature of US1 implementation
- **US3 (P2)**: Depends on US1 - validates idempotency feature of US1 implementation

**Note**: US2 and US3 are actually validation of US1 features, not separate implementations. The synchronization algorithm in US1 handles all three stories.

### Within Each Phase

**Phase 2 (US1) - Critical Path**:
1. Write contract tests (T005-T008) - can run in parallel
2. Run tests to verify failure (T009) - sequential
3. Implement sync logic (T010-T015) - sequential (modifying same method)
4. Run tests to verify pass (T016) - sequential
5. Validate (T017-T019) - sequential

**Phase 3 (US2) & Phase 4 (US3)**:
- Tests can be written in parallel with Phase 2 if desired
- Implementation verification is quick (confirms US1 handles these cases)

**Phase 5 (Error Handling)**:
- All contract tests (T034-T037) can be written in parallel
- Integration tests (T039-T040) can be written in parallel

### Parallel Opportunities

**Maximum Parallelization** (if multiple developers):
- Developer A: Focus on US1 core implementation (T005-T019)
- Developer B: Write US2 and US3 tests (T020-T021, T027-T028) in parallel
- Developer C: Write error handling tests (T034-T037) in parallel
- All converge for final validation and quality checks

**Single Developer** (recommended flow):
1. Complete Phase 1 (setup)
2. Complete Phase 2 (US1) fully - this is the critical path
3. Complete Phase 3 (US2) - quick validation
4. Complete Phase 4 (US3) - quick validation  
5. Complete Phase 5 (error handling)
6. Complete Phase 6-8 (documentation, quality, commit)

---

## Implementation Strategy

### Recommended Approach: MVP First (US1 Only)

This feature is unique - **US1 implementation handles all three user stories**:

1. **Complete Phase 1**: Setup and documentation review
2. **Complete Phase 2**: Implement US1 synchronization logic
3. **STOP and VALIDATE**: US1 is the MVP - test independently
4. **Complete Phases 3-4**: Verify US2 and US3 scenarios work (they should - same implementation)
5. **Complete Phases 5-8**: Error handling, documentation, quality checks

**Why this works**: The synchronization algorithm is designed to:
- Replace all options (US1)
- Update requirement status (US2) 
- Be idempotent without manual cleanup (US3)

All three stories are satisfied by the same implementation.

### Estimated Time

- **Phase 1**: 30 minutes (setup and review)
- **Phase 2 (US1)**: 2-3 hours (write tests, implement, validate)
- **Phase 3 (US2)**: 30 minutes (write tests, verify)
- **Phase 4 (US3)**: 30 minutes (write tests, verify)
- **Phase 5**: 1 hour (error handling tests)
- **Phase 6**: 30 minutes (documentation updates)
- **Phase 7**: 45 minutes (quality checks)
- **Phase 8**: 15 minutes (commit)

**Total**: 5-6 hours

### Incremental Delivery

1. **After Phase 2**: Deploy US1 (MVP) - core synchronization works
2. **After Phase 4**: Deploy all user stories - complete feature
3. **After Phase 7**: Production-ready with full quality validation

---

## Task Summary

**Total Tasks**: 67

**By Phase**:
- Phase 1 (Setup): 4 tasks
- Phase 2 (US1): 15 tasks
- Phase 3 (US2): 7 tasks
- Phase 4 (US3): 7 tasks
- Phase 5 (Error Handling): 10 tasks
- Phase 6 (Documentation): 5 tasks
- Phase 7 (Quality): 14 tasks
- Phase 8 (Commit): 5 tasks

**By User Story**:
- US1: 15 tasks (core implementation)
- US2: 7 tasks (validation of US1 feature)
- US3: 7 tasks (validation of US1 feature)
- Cross-cutting: 38 tasks (setup, errors, docs, quality)

**Parallel Opportunities**: 
- 12 tasks marked [P] can run in parallel within their phase
- Phases 3 and 4 can overlap with Phase 2 test writing
- Error handling tests can be written during Phases 2-4

**Format Validation**: ✅ All tasks follow checklist format with ID, optional [P] marker, [Story] label (where applicable), and file paths

---

## Notes

- This is a **modification of existing code**, not new development
- Only 2 files require changes: `OptionRepositoryExposed.kt` and `SponsoringPackRoutesTest.kt`
- OpenAPI documentation file also needs minor updates
- No schema migrations or new files needed
- TDD approach is critical - tests must fail before implementation
- The constitution requires contract tests and integration tests
- All three user stories are satisfied by the same synchronization implementation
- Focus on Phase 2 (US1) - it's the critical path that enables everything else
