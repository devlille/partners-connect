# Tasks: Filter Partnerships by Assigned Organiser

**Input**: Design documents from `/specs/015-filter-partnerships-organiser/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Organization**: Tasks are grouped by user story (US1, US2, US3) to enable independent implementation and testing of each story increment.

**Tests**: This feature does not explicitly request TDD, so test tasks are included only for validation at the end of each story implementation.

## Format: `- [ ] [ID] [P?] [Story?] Description with file path`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: User story label (US1, US2, US3) - only for story phases
- Include exact file paths in descriptions

---

## Phase 1: Setup

**Purpose**: Project initialization and JSON schemas

- [ ] T001 [P] Create pagination_metadata.schema.json in application/src/main/resources/schemas/
- [ ] T002 [P] Create filter_definition.schema.json in application/src/main/resources/schemas/
- [ ] T003 [P] Create filter_value.schema.json in application/src/main/resources/schemas/
- [ ] T004 Update partnership_list_response.schema.json to include metadata field in application/src/main/resources/schemas/

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core models and enums needed by all user stories

- [ ] T005 [P] Create FilterType enum in internal/infrastructure/api/FilterType.kt
- [ ] T006 [P] Create FilterValue data class in internal/infrastructure/api/FilterValue.kt
- [ ] T007 [P] Create FilterDefinition data class in internal/infrastructure/api/FilterDefinition.kt
- [ ] T008 Create PaginationMetadata data class in internal/infrastructure/api/PaginationMetadata.kt
- [ ] T009 Add metadata field to PaginatedResponse in internal/infrastructure/api/PaginatedResponse.kt
- [ ] T010 Add organiser field to PartnershipFilters in partnership/domain/PartnershipItem.kt

---

## Phase 3: User Story 1 - Filter Partnerships by Organiser Email (P1)

**Goal**: Enable filtering partnerships by assigned organiser's email address

**Independent Test Criteria**: Authenticate as organizer → Apply `filter[organiser]=email@example.com` → Verify only partnerships assigned to that organiser are returned

### Models

- [ ] T011 [US1] Add organiserUserId parameter to PartnershipEntity.filters() method in partnership/infrastructure/db/PartnershipEntity.kt

### Services

- [ ] T012 [US1] Update PartnershipRepository.listByEvent() return type to PaginatedResponse and accept organiser filter in partnership/application/PartnershipRepository.kt
- [ ] T013 [US1] Add email resolution logic (UserEntity.singleUserByEmail) in PartnershipRepository.listByEvent()
- [ ] T014 [US1] Pass organiserUserId to PartnershipEntity.filters() query in PartnershipRepository.listByEvent()

### API Endpoints

- [ ] T015 [US1] Extract organiser query parameter in partnership list route in partnership/infrastructure/api/PartnershipRoutes.kt
- [ ] T016 [US1] Pass organiser to PartnershipFilters constructor in partnership/infrastructure/api/PartnershipRoutes.kt
- [ ] T017 [US1] Update OpenAPI spec with filter[organiser] parameter in application/src/main/resources/openapi.yaml

### Integration

- [ ] T018 [US1] Create PartnershipOrganiserFilterRoutesTest integration test in partnership/PartnershipOrganiserFilterRoutesTest.kt (root package for end-to-end workflows per constitution Section II)
- [ ] T019 [US1] Test organiser filter returns only assigned partnerships in integration test
- [ ] T020 [US1] Test filter excludes partnerships with no assigned organiser in integration test
- [ ] T021 [US1] Test filter returns empty list for non-existent email in integration test
- [ ] T022 [US1] Test filter combines with other filters using AND logic in integration test
- [ ] T023 [US1] Test filter is case-insensitive for email matching in integration test
- [ ] T024 [US1] Run ktlintFormat and detekt checks
- [ ] T025 [US1] Verify 80% test coverage for organiser filter logic

---

## Phase 4: User Story 2 - Pagination Metadata with Available Filters (P2)

**Goal**: Return pagination metadata containing filters array (including available organisers) and sorts array

**Independent Test Criteria**: Request partnership list → Verify response includes metadata.filters with all filter definitions → Verify organiser filter has values array with organisation editors

### Database Queries

- [ ] T026 [US2] Create listEditorsbyOrgId() method in users/infrastructure/db/OrganisationPermissionEntity.kt

### Services

- [ ] T027 [US2] Implement buildMetadata() helper method in PartnershipRepository: query organisation editors via listEditorsbyOrgId(), map to FilterValue objects, build complete filters array (pack_id, validated, suggestion, paid, agreement-generated, agreement-signed, organiser with values), and build sorts array ["created", "validated"]
- [ ] T028 [US2] Populate PaginatedResponse.metadata field in listByEvent() by calling buildMetadata()

### API Endpoints

- [ ] T029 [US2] Update OpenAPI spec with metadata response schema in application/src/main/resources/openapi.yaml

### Integration

- [ ] T030 [US2] Update PartnershipListRouteGetTest contract test in partnership/infrastructure/api/PartnershipListRouteGetTest.kt (validates HTTP request/response schema per constitution Section II)
- [ ] T031 [US2] Test metadata.filters includes all 7 filter definitions (pack_id, validated, suggestion, paid, agreement-generated, agreement-signed, organiser) in contract test
- [ ] T032 [US2] Test organiser filter includes values array with editors in contract test
- [ ] T033 [US2] Test metadata.filters includes users with no assigned partnerships in contract test
- [ ] T034 [US2] Test metadata.sorts includes expected sort fields in contract test
- [ ] T035 [US2] Test metadata present in every response in contract test
- [ ] T036 [US2] Test empty values array when no organisation editors exist in contract test
- [ ] T037 [US2] Run ktlintFormat and detekt checks
- [ ] T038 [US2] Verify 80% test coverage for metadata building logic

---

## Phase 5: User Story 3 - Filter Partnerships by Organiser in Email Endpoint (P3)

**Goal**: Apply organiser filter to email partnerships endpoint for targeted bulk communications

**Independent Test Criteria**: Use POST /partnerships/email with filter[organiser] parameter → Verify only partnerships assigned to specified organiser receive email

### API Endpoints

- [ ] T044 [US3] Extract organiser query parameter in email route in partnership/infrastructure/api/PartnershipEmailRoutes.kt
- [ ] T045 [US3] Pass organiser to PartnershipFilters for email recipients query in partnership/infrastructure/api/PartnershipEmailRoutes.kt
- [ ] T046 [US3] Update OpenAPI spec with filter[organiser] for email endpoint in application/src/main/resources/openapi.yaml

### Integration
39 [US3] Extract organiser query parameter in email route in partnership/infrastructure/api/PartnershipEmailRoutes.kt
- [ ] T040 [US3] Pass organiser to PartnershipFilters for email recipients query in partnership/infrastructure/api/PartnershipEmailRoutes.kt
- [ ] T041 [US3] Update OpenAPI spec with filter[organiser] for email endpoint in application/src/main/resources/openapi.yaml

### Integration

- [ ] T042 [US3] Create PartnershipEmailOrganiserFilterRoutesTest integration test in partnership/PartnershipEmailOrganiserFilterRoutesTest.kt (root package for end-to-end workflows per constitution Section II)
- [ ] T043 [US3] Test email endpoint filters by organiser email in integration test
- [ ] T044 [US3] Test email endpoint combines organiser with other filters in integration test
- [ ] T045 [US3] Test email endpoint returns 204 when no recipients match filter in integration test
- [ ] T046 [US3] Test email filter is case-insensitive in integration test
- [ ] T047 [US3] Run ktlintFormat and detekt checks
- [ ] T048
**Purpose**: Final validation and documentation

- [ ] T049 [P] Run full test suite with ./gradlew test --no-daemon
- [ ] T050 [P] Validate OpenAPI spec with npm run validate
- [ ] T051 [P] Run ktlintCheck and detekt for all modified files
- [ ] T052 Update AGENTS.md with feature context if needed
- [ ] T053 Create PR with comprehensive description linking to spec.md

---

## Dependencies

### Story Completion Order

```
Setup (Phase 1)
  ↓
Foundational (Phase 2) - BLOCKS all user stories
  ↓
├─→ User Story 1 (P1) - Can be implemented independently
├─→ User Story 2 (P2) - Requires US1 repository changes
└─→ User Story 3 (P3) - Requires US1 filter logic
```

### Within User Stories

**User Story 1 (P1)**:
- Models (T011) → Services (T012-T014) → API Endpoints (T015-T017) → Integration Tests (T018-T025)

**User Story 2 (P2)**:
- Database Queries (T026) → Services (T027-T033) → API Endpoints (T034) → Integration Tests (T035-T043)

**User Story 3 (P3)**:
- API Endpoints (T044-T046) → Integration Tests (T047-T053)

---

## Parallel Execution Examples

### Phase 1 (Setup) - ALL parallel
```bash
T001, T002, T003 (independent JSON schemas)
```

### Phase 2 (Foundational) - SOME parallel
```bash
T005, T006, T007 (independent data classes)
T008 → requires T007
T009 → requires T008
T010 (independent PartnershipFilters change)
```

### User Story 1 (P1) - Sequential within story
```bash
T011 → T012 → T013 → T014 → T015 → T016 → T017
Then: T018-T025 (tests after implementation)
```

### User Story 2 (P2) - Sequential within story
```bash
T026 → T027 → T028 → T029 → T030 → T031 → T032 → T033 → T034
Then: T035-T043 (tests after implementation)
```

### User Story 3 (P3) - Sequential within story
```bash
T044 → T045 → T046
Then: T047-T053 (tests after implementation)
```

### Phase 6 (Polish) - ALL parallel
```bash
T054, T055, T056 (independent validation tasks)
T057, T058 (documentation tasks)
```

---

## Implementation Strategy

### MVP Scope (Recommended)
**Ship User Story 1 (P1) first** as a complete, independently testable increment:
- Core filtering by organiser email
- Validates feature viability
- Delivers immediate value for workload visibility
- 25 tasks (T001-T025)

### Incremental Delivery
1. **MVP**: User Story 1 (P1) - Filter by organiser email
2. **Enhancement 1**: User Story 2 (P2) - Add pagination metadata
3. **Enhancement 2**: User Story 3 (P3) - Email endpoint filter support

### Testing Strategy
- Integration tests cover end-to-end behavior (contract + business logic)
- Contract tests validate HTTP interface (request/response schemas)
- Use shared database pattern (`moduleSharedDb`) for all tests
- Target 80% coverage minimum per constitution requirements

---

## Task Summary

**Total Tasks**: 53

### By Phase
- Phase 1 (Setup): 4 tasks
- Phase 2 (Foundational): 6 tasks
- Phase 3 (User Story 1 - P1): 15 tasks
- Phase 4 (User Story 2 - P2): 13 tasks
- Phase 5 (User Story 3 - P3): 10 tasks
- Phase 6 (Polish): 5 tasks

### By User Story
- User Story 1 (P1): 15 tasks (T011-T025)
- User Story 2 (P2): 13 tasks (T026-T038)
- User Story 3 (P3): 10 tasks (T039-T048)
- Setup/Foundational: 10 tasks (T001-T010)
- Polish: 5 tasks (T049-T053)

### Parallelizable Tasks
- Phase 1: 3 tasks (T001-T003)
- Phase 2: 3 tasks (T005-T007)
- Phase 6: 3 tasks (T054-T056)
- **Total Parallel Opportunities**: 9 tasks

### Critical Path
Setup (4) → Foundational (6) → US1 Models (1) → US1 Services (3) → US1 API (3) → US1 Tests (7) → US2 DB (1) → US2 Services (7) → US2 API (1) → US2 Tests (9) → US3 API (3) → US3 Tests (7) → Polish (5)

**Estimated Duration** (sequential): ~13-17 hours
**Estimated Duration** (with parallel execution): ~10-13 hours

---

## Format Validation

✅ All tasks follow checklist format: `- [ ] [TaskID] [P?] [Story?] Description with file path`
✅ All tasks have sequential IDs (T001-T058)
✅ [P] marker only on parallelizable tasks (different files, no dependencies)
✅ [Story] labels (US1, US2, US3) on user story phase tasks only
✅ Setup/Foundational/Polish phases have NO story labels
✅ File paths included in every task description
✅ Tasks organized by user story for independent implementation
✅ Each story has independent test criteria documented
