# Tasks: Company Test Codebase Refactoring

**Input**: Design documents from `/specs/009-refactor-company-tests/`
**Prerequisites**: plan.md ✓, spec.md ✓, research.md ✓, data-model.md ✓, contracts/ ✓

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and directory structure preparation

- [x] T001 Create contract test directory structure at server/application/src/test/kotlin/fr/devlille/partners/connect/companies/infrastructure/api/
- [x] T002 [P] Analyze existing test files in server/application/src/test/kotlin/fr/devlille/partners/connect/companies/ and identify contract vs integration concerns
- [x] T003 [P] Validate existing factories directory structure at server/application/src/test/kotlin/fr/devlille/partners/connect/companies/factories/

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core analysis and baseline establishment that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T004 Run baseline test execution to measure current performance and identify all test scenarios
- [x] T005 [P] Document current test coverage mapping from existing test files to preserve during refactoring
- [x] T006 [P] Validate existing factory functions are compatible with both contract and integration test patterns
- [x] T007 Create test execution performance benchmark for 2-second requirement validation using gradle test task timing

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Contract Test Reorganization (Priority: P1) 🎯 MVP

**Goal**: Separate contract tests from integration tests with clear API schema validation focus

**Independent Test**: Run contract tests in isolation and verify they only validate request/response schemas without business logic side effects

### Implementation for User Story 1

- [x] T008 [P] [US1] Create CompanyCreateContractTest.kt in server/application/src/test/kotlin/fr/devlille/partners/connect/companies/infrastructure/api/CompanyCreateContractTest.kt
- [x] T009 [P] [US1] Create CompanyGetContractTest.kt in server/application/src/test/kotlin/fr/devlille/partners/connect/companies/infrastructure/api/CompanyGetContractTest.kt
- [x] T010 [P] [US1] Create CompanyListContractTest.kt in server/application/src/test/kotlin/fr/devlille/partners/connect/companies/infrastructure/api/CompanyListContractTest.kt
- [x] T011 [P] [US1] Create CompanyUpdateContractTest.kt in server/application/src/test/kotlin/fr/devlille/partners/connect/companies/infrastructure/api/CompanyUpdateContractTest.kt
- [x] T012 [P] [US1] Create CompanyLogoUploadContractTest.kt in server/application/src/test/kotlin/fr/devlille/partners/connect/companies/infrastructure/api/CompanyLogoUploadContractTest.kt
- [x] T013 [P] [US1] Create CompanyJobOfferContractTest.kt in server/application/src/test/kotlin/fr/devlille/partners/connect/companies/infrastructure/api/CompanyJobOfferContractTest.kt
- [x] T014 [US1] Create JSON schema files in server/application/src/main/resources/schemas/ for company API endpoints (create_company_request.schema.json, company_response.schema.json, etc.)
- [x] T015 [US1] Extract contract test scenarios from existing CompanyRoutesTest.kt and duplicate mixed-concern tests, implementing `call.receive<T>(schema)` pattern
- [x] T016 [US1] Extract contract test scenarios from existing CompanyJobOfferRoutes*Test.kt files, creating corresponding schema files
- [x] T017 [US1] Validate all contract tests execute in under 2 seconds and focus on schema validation only using `call.receive<T>(schema)` pattern
- [x] T018 [US1] Ensure contract tests use shared factory functions from factories/ folder for minimal setup

**Checkpoint**: At this point, User Story 1 should be fully functional - contract tests clearly separated and independently executable

---

## Phase 4: User Story 2 - Integration Test Restructuring (Priority: P2)

**Goal**: Organize integration tests in domain root directory for end-to-end business workflow validation

**Independent Test**: Run integration tests and verify they test complete business scenarios from HTTP request to database persistence

### Implementation for User Story 2

- [x] T019 [P] [US2] Create CompanyLifecycleIntegrationTest.kt in server/application/src/test/kotlin/fr/devlille/partners/connect/companies/CompanyLifecycleIntegrationTest.kt
- [x] T020 [P] [US2] Create CompanyJobOfferManagementIntegrationTest.kt in server/application/src/test/kotlin/fr/devlille/partners/connect/companies/CompanyJobOfferManagementIntegrationTest.kt
- [x] T021 [P] [US2] Create CompanyPartnershipWorkflowIntegrationTest.kt in server/application/src/test/kotlin/fr/devlille/partners/connect/companies/CompanyPartnershipWorkflowIntegrationTest.kt
- [x] T022 [P] [US2] Refactor existing CompanyUpdateIntegrationTest.kt to follow new integration test patterns
- [x] T023 [P] [US2] Refactor existing CompanySoftDeleteIntegrationTest.kt to follow new integration test patterns
- [x] T024 [P] [US2] Refactor existing CompanyStatusFilterIntegrationTest.kt to follow new integration test patterns
- [x] T025 [US2] Extract integration test scenarios from mixed-concern tests and ensure business workflow coverage
- [x] T026 [US2] Validate all integration tests execute in under 2 seconds and cover end-to-end business logic
- [x] T027 [US2] Ensure integration tests use shared factory functions for complex business setup
- [x] T028 [US2] Verify integration tests default ambiguous categorization to integration category for safety

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently - clear separation achieved

---

## Phase 5: User Story 3 - Acceptance Test Preservation (Priority: P3)

**Goal**: Preserve all existing test scenarios during refactoring with no regression in test coverage

**Independent Test**: Compare test coverage reports before and after refactoring to ensure equivalent coverage is maintained

### Implementation for User Story 3

- [x] T029 [P] [US3] Create test scenario preservation matrix mapping all original tests to new contract or integration categories
- [x] T030 [P] [US3] Validate 100% of existing test scenarios are covered in either contract or integration tests
- [x] T031 [US3] Run complete refactored test suite and compare coverage metrics against baseline
- [x] T032 [US3] Identify and document any test scenarios that were categorized as integration by default
- [x] T033 [US3] Verify all original test files can be safely removed after scenario preservation validation
- [x] T034 [US3] Update test documentation to reflect new structure and categorization approach
- [x] T035 [US3] Validate refactored test structure maintains or improves execution performance
- [x] T036 [US3] Create developer guide explaining contract vs integration test organization for future reference

**Checkpoint**: All user stories should now be independently functional with full test scenario preservation

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Quality improvements and final validation

- [x] T037 [P] Run ktlint and detekt validation on all new test files
- [x] T038 [P] Validate naming conventions are consistent across all contract and integration tests
- [x] T039 [P] Ensure shared factory functions are properly documented and accessible
- [x] T040 Perform final performance validation ensuring all test categories execute in 2 seconds or less using gradle test task timing
- [x] T041 Run full quickstart.md validation to verify refactoring approach
- [x] T042 [P] Update any project documentation referencing the old test structure
- [x] T043 Remove original test files after confirming complete scenario preservation

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - User stories can proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Polish (Final Phase)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - Independently testable but benefits from US1 completion
- **User Story 3 (P3)**: Should start after US1 and US2 for complete validation but can run independently

### Within Each User Story

- Contract test file creation can be done in parallel ([P] marked tasks)
- Integration test file creation can be done in parallel ([P] marked tasks)
- Scenario extraction depends on file creation but not on other stories
- Performance validation should be done after core implementation
- Factory function usage validation should be continuous throughout

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- Contract test file creation tasks (T008-T013) can all run in parallel
- Integration test file creation tasks (T018-T023) can all run in parallel
- Different user stories can be worked on in parallel by different team members
- Polish tasks can be distributed across multiple developers

---

## Parallel Example: User Story 1

```bash
# Launch all contract test file creation together:
Task: "Create CompanyCreateContractTest.kt"
Task: "Create CompanyGetContractTest.kt" 
Task: "Create CompanyListContractTest.kt"
Task: "Create CompanyUpdateContractTest.kt"
Task: "Create CompanyLogoUploadContractTest.kt"
Task: "Create CompanyJobOfferContractTest.kt"

# Then sequential extraction and validation:
Task: "Extract contract test scenarios from existing tests"
Task: "Validate performance and schema focus"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - establishes baseline)
3. Complete Phase 3: User Story 1 (Contract test separation)
4. **STOP and VALIDATE**: Test contract tests independently, verify <2 second execution
5. Demo clear API schema validation separation

### Incremental Delivery

1. Complete Setup + Foundational → Analysis and baseline ready
2. Add User Story 1 → Test independently → Validate contract test separation
3. Add User Story 2 → Test independently → Validate integration test organization  
4. Add User Story 3 → Test independently → Validate complete preservation
5. Each story adds organizational value without breaking existing functionality

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together  
2. Once Foundational is done:
   - Developer A: User Story 1 (Contract tests)
   - Developer B: User Story 2 (Integration tests)
   - Developer C: User Story 3 (Preservation validation)
3. Stories complete and validate independently

---

## Validation Checklist

**After Phase 3 (User Story 1)**:
- [ ] Contract tests execute in <2 seconds
- [ ] Contract tests focus only on API schema validation
- [ ] Contract tests are in infrastructure/api/ directory
- [ ] Factory functions are properly accessible

**After Phase 4 (User Story 2)**:
- [ ] Integration tests execute in <2 seconds  
- [ ] Integration tests validate complete business workflows
- [ ] Integration tests are in domain root directory
- [ ] Ambiguous tests defaulted to integration category

**After Phase 5 (User Story 3)**:
- [ ] 100% of original test scenarios preserved
- [ ] Coverage metrics maintained or improved
- [ ] All original tests can be safely removed
- [ ] Documentation updated for new structure

**After Phase 6 (Polish)**:
- [ ] All tests pass ktlint/detekt validation
- [ ] Naming conventions are consistent
- [ ] Performance requirements met across all test categories
- [ ] Quickstart validation successful

---

## Notes

- [P] tasks = different files, no dependencies on completion order
- [Story] label maps task to specific user story for traceability  
- Each user story should be independently completable and testable
- Focus on schema validation vs business logic separation throughout
- Preserve existing factory patterns and H2 database approach
- Ensure constitutional compliance with testing strategy requirements
- Validate performance continuously to meet <2 second execution requirements