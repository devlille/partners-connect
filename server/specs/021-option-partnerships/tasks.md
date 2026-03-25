# Tasks: Option Partnerships

**Input**: Design documents from `/specs/021-option-partnerships/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

**Organization**: Tasks are grouped by user story. US1 and US2 are both P1 and inseparable — US1 is the core feature, US2 is data format consistency (guaranteed by reusing the existing `PartnershipItem` model and `toDomain()` mapper).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2)
- Exact file paths included in all descriptions

---

## Phase 1: Setup

**Purpose**: No project initialization needed — this feature modifies an existing codebase. Skip to Phase 2.

---

## Phase 2: Foundational (Schema & Domain Model)

**Purpose**: Create the new JSON schema and domain model that the route and repository depend on. MUST complete before user story implementation.

- [x] T001 [P] Create JSON schema file in `application/src/main/resources/schemas/sponsoring_option_with_partnerships.schema.json` referencing `sponsoring_option_with_translations.schema.json` and `partnership_item.schema.json`
- [x] T002 [P] Create `SponsoringOptionDetailWithPartners` data class in `application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/domain/SponsoringOptionDetailWithPartners.kt`
- [x] T003 [P] Add GET operation for `/orgs/{orgSlug}/events/{eventSlug}/options/{optionId}` in `application/src/main/resources/openapi/openapi.yaml` with `SponsoringOptionDetailWithPartners` response schema

**Checkpoint**: New schema, domain model, and OpenAPI spec ready. Run `npm run validate` to verify OpenAPI.

---

## Phase 3: User Story 1 — View Validated Partnerships per Option (Priority: P1) 🎯 MVP

**Goal**: Enrich the single option detail endpoint to return validated partnerships alongside option data.

**Independent Test**: Call `GET /orgs/{orgSlug}/events/{eventSlug}/options/{optionId}` and verify the response includes an `option` object and a `partnerships` array with `PartnershipItem` entries.

### Implementation for User Story 1

- [x] T004 [US1] Add `getOptionByIdWithPartners(eventSlug: String, optionId: UUID): SponsoringOptionDetailWithPartners` method signature to `OptionRepository` interface in `application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/domain/OptionRepository.kt`
- [x] T005 [US1] Implement `getOptionByIdWithPartners` in `application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/application/OptionRepositoryExposed.kt` — query `PackOptionsTable` for pack IDs containing the option, filter event partnerships by `validatedPack()` membership, map to `PartnershipItem` via `toDomain(emails)`
- [x] T006 [US1] Update `get("/{optionId}")` route in `application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/infrastructure/api/SponsoringRoutes.kt` to call `getOptionByIdWithPartners` instead of `getOptionByIdWithAllTranslations` and respond with `SponsoringOptionDetailWithPartners`

### Contract Test for User Story 1

- [x] T007 [US1] Create contract test `SponsoringOptionRouteGetTest` in `application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/infrastructure/api/SponsoringOptionRouteGetTest.kt` covering: 200 with validated partnerships, 200 with empty partnerships (no validated partnerships for option), 404 for non-existent option, 401 for unauthorized access

**Checkpoint**: Option detail endpoint returns validated partnerships. Run `./gradlew test --tests "*SponsoringOptionRouteGetTest*" --no-daemon` to verify.

---

## Phase 4: Polish & Cross-Cutting Concerns

**Purpose**: Quality gates and validation

- [x] T008 Run `./gradlew ktlintFormat --no-daemon` to auto-fix formatting
- [x] T009 Run `./gradlew check --no-daemon` to verify all quality gates pass (ktlint, detekt, tests)
- [x] T010 Run quickstart.md validation — verify the option detail endpoint returns expected structure per quickstart.md

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 2)**: No dependencies — can start immediately
- **User Story 1 (Phase 3)**: Depends on T001 (schema), T002 (domain model) — T003 (OpenAPI) can proceed in parallel
- **Polish (Phase 4)**: Depends on all Phase 3 tasks being complete

### User Story Dependencies

- **User Story 1 (P1)**: Core feature — T004 → T005 → T006 (sequential: interface → implementation → route)
- **User Story 2 (P1)**: No separate tasks — guaranteed by reusing `PartnershipItem` and `toDomain()` mapper in T005

### Within User Story 1

- T004 (interface) must complete before T005 (implementation)
- T005 (implementation) must complete before T006 (route update)
- T007 (contract test) can start after T006 (route update)

### Parallel Opportunities

- T001, T002, T003 can all run in parallel (different files, no dependencies)
- T007 can be written alongside T006 if the test file structure is set up first

---

## Parallel Example: Phase 2

```
# Launch all foundational tasks together:
Task T001: "Create JSON schema in schemas/sponsoring_option_with_partnerships.schema.json"
Task T002: "Create SponsoringOptionDetailWithPartners data class"
Task T003: "Add GET operation to openapi.yaml"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 2: Foundational (T001-T003 in parallel)
2. Complete Phase 3: User Story 1 (T004 → T005 → T006 → T007)
3. **STOP and VALIDATE**: Run contract tests + quality gates
4. Complete Phase 4: Polish (T008-T010)

### Incremental Delivery

- **After Phase 2**: Schema and model are in place, no behavioral changes yet
- **After T006**: Feature is functionally complete — endpoint returns partnerships
- **After T007**: Feature is tested — contract test validates the response
- **After Phase 4**: All quality gates pass — ready for PR

### Total Tasks: 10

| Phase | Tasks | Parallelizable |
|-------|-------|---------------|
| Phase 2: Foundational | 3 | 3 (all parallel) |
| Phase 3: User Story 1 | 4 | 0 (sequential chain) |
| Phase 4: Polish | 3 | 0 (sequential) |
